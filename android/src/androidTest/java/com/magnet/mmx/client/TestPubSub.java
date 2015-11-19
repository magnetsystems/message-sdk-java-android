/*
 * Copyright (c) 2015 Magnet Systems, Inc.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package com.magnet.mmx.client;

import android.test.suitebuilder.annotation.Suppress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

import android.test.InstrumentationTestCase;
import android.test.suitebuilder.annotation.SmallTest;
import android.util.Log;

import com.magnet.mmx.client.common.MMXException;
import com.magnet.mmx.client.common.MMXGlobalTopic;
import com.magnet.mmx.client.common.MMXMessage;
import com.magnet.mmx.client.common.MMXPayload;
import com.magnet.mmx.client.common.MMXResult;
import com.magnet.mmx.client.common.MMXSubscription;
import com.magnet.mmx.client.common.MMXTopicInfo;
import com.magnet.mmx.client.common.TopicExistsException;
import com.magnet.mmx.client.common.TopicNotFoundException;
import com.magnet.mmx.protocol.MMXTopic;
import com.magnet.mmx.protocol.MMXTopicOptions;
import com.magnet.mmx.protocol.MMXid;
import com.magnet.mmx.protocol.TopicAction.TopicTags;
import com.magnet.mmx.protocol.TopicSummary;

//FIXME : those test cases need rewriting for 2.0
@Suppress
public class TestPubSub extends InstrumentationTestCase {

  private static final String TAG = TestPubSub.class.getSimpleName();
  private MMXClient mmxClient;
  private boolean isConnected = false;
  private static final String TESTER = "pubsub_tester@host";

  private static final String topicName0 = "MyTopic0";
  private static final String topicName1 = "MyTopic1";
  private static final String topicName2 = "MyTopic2";
  private static final String mixCaseTopicName = "MyTestTopic";
  private static final String lowerCaseTopicName = "mytesttopic";
  private static final String mixCaseDescription = "MixCaseDescription";
  private static final String lowerCaseDescription = "lowercasedescription";

  private boolean itemReceived = false;


  @Override
  public void setUp() {
    connect(TESTER);
  }

  private void connect(String user) {
    mmxClient = MMXClient.getInstance(this.getInstrumentation().getTargetContext(),
            new ClientTestConfigImpl(this.getInstrumentation().getTargetContext()));
    isConnected = false;
    MMXClient.ConnectionOptions options = new MMXClient.ConnectionOptions().setAutoCreate(true);
    mmxClient.connectWithCredentials(user, "test".getBytes(), new AbstractMMXListener() {
      @Override
      public void onConnectionEvent(MMXClient client, MMXClient.ConnectionEvent event) {
        super.onConnectionEvent(client, event);
        if (event == MMXClient.ConnectionEvent.CONNECTED) {
          isConnected = true;
        }
      }

      @Override
      public void handleMessageReceived(MMXClient mmxClient, MMXMessage mmxMessage, String receiptId) {
      }

      @Override
      public void handleMessageDelivered(MMXClient mmxClient, MMXid recipient, String messageId) {
      }

      @Override
      public void handlePubsubItemReceived(MMXClient mmxClient, MMXTopic mmxTopic, MMXMessage mmxMessage) {
        itemReceived = true;
      }
    }, options);
    try {
      for (int i=0; i<50; i++) {
        Thread.sleep(100);
        if (isConnected) {
          break;
        }
      }

    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  @SmallTest
  public void testSummary() throws MMXException {
    MMXPubSubManager pubManager = mmxClient.getPubSubManager();
    MMXTopic[] topics = new MMXTopic[] { new MMXGlobalTopic(topicName0),
                                          new MMXGlobalTopic(topicName1),
                                          new MMXGlobalTopic(topicName2) };

    try {
      for (MMXTopic topic : topics) {
        try {
          pubManager.deleteTopic(topic);
        } catch (Throwable e) {
          // Ignored.
        }
      }

      // The topics have at most 10 persisted items.
      MMXTopicOptions options = new MMXTopicOptions().setMaxItems(10);
      for (MMXTopic topic : topics) {
        pubManager.createTopic(topic, options);
      }

      for (int i = 0; i < 5; i++) {
        String id = pubManager.publish(topics[1], new MMXPayload("This is msg"+i));
        assertNotNull(id);
      }
      // Only last 10 items are available.
      for (int i = 0; i < 15; i++) {
        String id = pubManager.publish(topics[2], new MMXPayload("This is msg"+i));
        assertNotNull(id);
      }
      
      // Since server time and this client time are not synchronized, add one
      // day to make sure that it is long enough.
      Date since = new Date(0);
      Date until = new Date(System.currentTimeMillis()+24*3600*1000);
      List<TopicSummary> list = pubManager.getTopicSummary(Arrays.asList(topics),
                        null, null);
      assertNotNull(list);
      // If failed, the pubsub buffered write is still enabled!  Make sure that
      // "xmpp.pubsub.flush.max" is set to -1.
      assertTrue(0 != list.size());
      // MOB-2461 for summaries: topic0 (0 items), topic1 (5 items), topic2 (10 items)
      assertEquals(3, list.size());

      list = pubManager.getTopicSummary(Arrays.asList(topics), since, null);
      assertNotNull(list);
      assertEquals(3, list.size());
      
      list = pubManager.getTopicSummary(Arrays.asList(topics), null, until);
      assertNotNull(list);
      assertEquals(3, list.size());
      
      list = pubManager.getTopicSummary(Arrays.asList(topics), since, until);
      assertNotNull(list);
      assertEquals(3, list.size());
      
      for (TopicSummary summary : list) {
        if (summary.getTopicNode().equals(topics[0])) {
          assertEquals(0, summary.getCount());
        } else if (summary.getTopicNode().equals(topics[1])) {
          assertEquals(5, summary.getCount());
        } else if (summary.getTopicNode().equals(topics[2])) {
          assertEquals(10, summary.getCount());
        }
      }
    } finally {
      for (MMXTopic topic : topics) {
        pubManager.deleteTopic(topic);
      }
    }
  }
  
  @SmallTest
  public void testTopicNames() throws MMXException {
    // create a new node
    MMXPubSubManager pubManager = mmxClient.getPubSubManager();
    MMXTopic mcTopic = new MMXGlobalTopic(mixCaseTopicName);
    MMXTopic lcTopic = new MMXGlobalTopic(lowerCaseTopicName);
    
    // Test equality
    assertTrue(mcTopic.equals(lcTopic));

    // Prepare 
    try {
      pubManager.deleteTopic(mcTopic);
    } catch (TopicNotFoundException e) {
      // Ignored.
    }
    try {
      pubManager.deleteTopic(lcTopic);
    } catch (TopicNotFoundException e) {
      // Ignored.
    } catch (MMXException e) {
      fail("Unexpected MMXException at delete: "+e);
    }
    Log.d(TAG, "testTopicNames() is ready...");
    
    // Test topic creation.
    MMXTopicOptions options = new MMXTopicOptions().setMaxItems(2);
    pubManager.createTopic(mcTopic, options);
    try {
      MMXTopic topic = pubManager.createTopic(lcTopic, null);
      fail("Expecting TopicExistsException, but not getting one");
    } catch (TopicExistsException e) {
      // Expected.
    } catch (MMXException e) {
//      Log.e(TAG, "Unexpected MMXException at create: code="+e.getCode(), e);
      fail("Unexpected MMXException at create: "+e);
    }
    
    // Test getting topic info
    MMXTopicInfo mcInfo = pubManager.getTopic(mcTopic);
    assertNotNull(mcInfo);
    assertEquals(TESTER, mcInfo.getCreator().getUserId());
    MMXTopicInfo lcInfo = pubManager.getTopic(lcTopic);
    assertNotNull(lcInfo);
    assertTrue(mcInfo.getTopic().equals(lcInfo.getTopic()));

    // Test topic subscription
    String lcSubId = pubManager.subscribe(lcTopic, true);
    String mcSubId = pubManager.subscribe(mcTopic, true);
    assertEquals(lcSubId, mcSubId);
    
    // Test listing subscription
    List<MMXSubscription> mcSubList = pubManager.listSubscriptions(mcTopic);
    assertNotNull(mcSubList);
    boolean found = false;
    for (MMXSubscription sub : mcSubList) {
      if (mcTopic.equals(sub.getTopic()) && sub.getId().equals(mcSubId))
          found = true;
    }
    assertTrue(found);
    found = false;
    List<MMXSubscription> lcSubList = pubManager.listSubscriptions(lcTopic);
    assertNotNull(lcSubList);
    for (MMXSubscription sub : lcSubList) {
      if (lcTopic.equals(sub.getTopic()) && sub.getId().equals(lcSubId))
        found = true;
    }
    assertTrue(found);
    
    // Test publishing item
    String mcItemId = pubManager.publish(mcTopic, new MMXPayload("This is msg1"));
    assertNotNull(mcItemId);
    String lcItemId = pubManager.publish(lcTopic, new MMXPayload("This is msg2"));
    assertNotNull(lcItemId);
    assertFalse(mcItemId.equals(lcItemId));
    
    // Test fetching items
    MMXResult<List<MMXMessage>> items = pubManager.getItems(mcTopic, null);
    assertNotNull(items);
    assertEquals(2, items.getTotal());
    assertEquals(2, items.getResult().size());
    items = pubManager.getItems(lcTopic, null);
    assertNotNull(items);
    assertEquals(2, items.getTotal());
    assertEquals(2, items.getResult().size());

    // Test get items by ids
    List<String> ids = new ArrayList<String>();
    for (MMXMessage item : items.getResult()) {
      ids.add(item.getId());
    }
    Map<String, MMXMessage> map = pubManager.getItemsByIds(lcTopic, ids);
    for (MMXMessage item : items.getResult()) {
      MMXMessage pubitem = map.get(item.getId());
      assertNotNull(pubitem);
      assertEquals(item.getPayload().getDataAsText(),
                   pubitem.getPayload().getDataAsText());
    }

    // Test get and set options
    MMXTopicOptions mcOptions = pubManager.getOptions(mcTopic);
    assertNotNull(mcOptions);
    mcOptions.setDescription(mixCaseDescription);
    pubManager.updateOptions(mcTopic, mcOptions);
    MMXTopicOptions lcOptions = pubManager.getOptions(lcTopic);
    assertNotNull(lcOptions);
    assertEquals(mixCaseDescription, lcOptions.getDescription());
    lcOptions.setDescription(lowerCaseDescription);
    pubManager.updateOptions(lcTopic, lcOptions);
    mcOptions = pubManager.getOptions(mcTopic);
    assertEquals(lowerCaseDescription, mcOptions.getDescription());
    
    // Test set and get tags
    try {
      pubManager.setAllTags(mcTopic, Arrays.asList(new String[] {
          "LongTag12345678901234567890", "LongTag01234567890123456789012345" }));
      fail("It should fail because tags are longer than 25 chars");
    } catch (MMXException e) {
      assertEquals(MMXException.BAD_REQUEST, e.getCode());
    }
    pubManager.setAllTags(mcTopic, Arrays.asList(new String[] { "Tag1", "tag2" }));
    TopicTags result = pubManager.getAllTags(lcTopic);
    assertTrue(result.getTags().contains("Tag1"));
    assertTrue(result.getTags().contains("tag2"));
    
    // Test remove tags
    pubManager.removeTags(mcTopic, Arrays.asList(new String[] { "Tag1" }));
    result = pubManager.getAllTags(mcTopic);
    assertNotNull(result.getTags());
    assertFalse(result.getTags().contains("Tag1"));
    assertTrue(result.getTags().contains("tag2"));
    pubManager.removeTags(lcTopic, Arrays.asList(new String[] { "tag2" }));
    result = pubManager.getAllTags(mcTopic);
    assertNotNull(result.getTags());
    assertTrue(result.getTags().isEmpty());
    
    // Test unsubscribing topic
    boolean mcUnsub = pubManager.unsubscribe(mcTopic, null);
    assertTrue(mcUnsub);
    boolean lcUnsub = pubManager.unsubscribe(lcTopic, null);
    assertFalse(lcUnsub);
    
    // Final clean up.
    pubManager.deleteTopic(mcTopic);
    try {
      pubManager.deleteTopic(lcTopic);
      fail("Expecting TopicNotFoundException, but not getting one");
    } catch (TopicNotFoundException e) {
      // Expected
    }
  }
}
