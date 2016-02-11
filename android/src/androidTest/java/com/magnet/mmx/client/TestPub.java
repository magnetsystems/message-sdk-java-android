/*   Copyright (c) 2015-2016 Magnet Systems, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.magnet.mmx.client;

import android.test.suitebuilder.annotation.SmallTest;
import android.util.Log;

import com.magnet.mmx.client.common.MMXException;
import com.magnet.mmx.client.common.MMXGlobalTopic;
import com.magnet.mmx.client.common.MMXMessage;
import com.magnet.mmx.client.common.MMXPayload;
import com.magnet.mmx.client.common.MMXTopicInfo;
import com.magnet.mmx.protocol.MMXTopic;
import com.magnet.mmx.protocol.MMXTopicOptions;
import com.magnet.mmx.protocol.MMXid;
import java.util.List;

public class TestPub extends MMXInstrumentationTestCase {

  private static final String TAG = TestPub.class.getSimpleName();
  private static final String APP_NAME = "MyPubSubApp";
  private static final String MMS_URL = "http://localhost:8443";
  private static final String USER1_SUFFIX = "pubsub_test1";
  private static final String DEVICE1_ID = "computer-2";

  private static final String topic_name_transient = "testpubtransient";
  private static final String topic_name = "testpub1";

  private boolean messageReceived = false;


  @Override
  public void setUp() {
    connect(APP_NAME, MMS_URL, USER1_SUFFIX, DEVICE1_ID, new AbstractMMXListener() {
      @Override
      public void onConnectionEvent(MMXClient client, MMXClient.ConnectionEvent event) {
        super.onConnectionEvent(client, event);
      }

      @Override
      public void handleMessageReceived(MMXClient mmxClient, MMXMessage mmxMessage, String receiptId) {
        messageReceived = true;
      }

      @Override
      public void handleMessageDelivered(MMXClient mmxClient, MMXid recipient, String messageId) {
      }

      @Override
      public void handlePubsubItemReceived(MMXClient mmxClient, MMXTopic mmxTopic, MMXMessage mmxMessage) {
      }
    });
  }

  @SmallTest
  public void testBasicCreate_PersistTopic() throws MMXException {
    // create a new node
    MMXPubSubManager pubManager = mmxClient.getPubSubManager();
    List<MMXTopicInfo> list = null;
    try {
      try {
        list = pubManager.listTopics();
        assertTrue(list.size() >= 0);
        // delete each one and create new ones
        for (MMXTopicInfo info: list) {
          if (info.getTopic().getName().endsWith(topic_name)) {
            Log.d(TAG, "deleting topic:" + info.getTopic());
            pubManager.deleteTopic(new MMXGlobalTopic(topic_name));
          }
        }
      } catch (MMXException e) {
        Log.e(TAG, "MMXException for listTopics", e);
      }

      // this should always work because they are all deleted
      MMXTopicOptions options = new MMXTopicOptions();
      options.setMaxItems(6);
      pubManager.createTopic(new MMXGlobalTopic(topic_name), options);
      assertTrue(true);
      // now list it
      list = pubManager.listTopics();
      boolean found = false;
      for (MMXTopicInfo info: list) {
        if (info.getTopic().getName().equals(topic_name)) {
          found = true;
          break;
        }
      }
      assertTrue(found);
    } catch (MMXException e) {
      Log.e(TAG, "MMXException for createTopic", e);
      fail("unexpected exception" + e);
    }
    // create it again, this time it should fail
    try {
      pubManager.createTopic(new MMXGlobalTopic(topic_name), null);
      fail("expected failure when creating duplicate topic");
    } catch (MMXException e) {
      assertTrue(true);
    }

  }

//  @SmallTest
//  public void testBasicDeviceTopic() throws MMXException {
//    // create the topic for my device
//    String deviceCollectionTopic = TopicHelper.generateDeviceTopicName(OSType.ANDROID);
//    String allTopicName = TopicHelper.generateDeviceAllLeafTopicName(OSType.ANDROID);
//    // see if the topic exists
//    MMXPubSubManager pubManager = mmxClient.getPubSubManager();
//    boolean found = false;
//
//    List<MMXTopicNode> topics = pubManager.listTopics();
//    MMXTopicNode allTopic = null;
//    for (MMXTopicNode topic: topics) {
//      Log.d(TAG, "topic=" + topic);
//      if (topic.getTopic().equals(allTopicName.substring(1))) {
//        allTopic = topic;
//
//      }
//    }
//
//    // also subscribe to the ANDROID collection node
//    MMXOsTopicHelper.subscribeOS(mmxClient, OSType.ANDROID);
//    List<PubSubManager.MMXSubscription> sub = pubManager.listSubscriptions(
//        TopicHelper.makeOSTopic(OSType.ANDROID, null));
//    assertNotNull(sub);
//    assertTrue(sub.size() > 0);
//
//    // now publish something
//    try {
//      MMXOsTopicHelper.publishOS(mmxClient, OSType.ANDROID,
//          new MMXPayload("text", "Get your latest Android devices at Verizon! "
//                        + System.currentTimeMillis()));
//    } catch (Exception e) {
//      fail("device publish exception encountered" + e);
//    }
//    assertTrue(true);
//
//  }

  @SmallTest
  public void testBasicCreate_NonpersistTopic() throws MMXException {
        // create a new node
      MMXPubSubManager pubManager = mmxClient.getPubSubManager();
      List<MMXTopicInfo> list = null;
      try {
        try {
            list = pubManager.listTopics();
            assertTrue(list.size() >= 0);
            // delete each one and create new ones
            for (MMXTopicInfo info: list) {
                if (info.getTopic().getName().endsWith(topic_name_transient)) {
                    Log.d(TAG, "deleting topic:" + info.getTopic());
                    pubManager.deleteTopic(info.getTopic());
                }
            }
        } catch (MMXException e) {
            Log.e(TAG, "MMXException for listTopics", e);
        }

        // this should always work because they are all deleted
        MMXTopicOptions options = new MMXTopicOptions();
        options.setMaxItems(-1);
        pubManager.createTopic(new MMXGlobalTopic(topic_name_transient), options);
        assertTrue(true);

        // now look it up
        boolean found = false;
        if (pubManager.getOptions(new MMXGlobalTopic(topic_name_transient)) != null) {
          found = true;
        }
        assertTrue(found);

      } catch (MMXException e) {
          Log.e(TAG, "MMXException for createTopic", e);
          fail("unexpected exception" + e);
      }
      // create it again, this time it should fail
      try {
          pubManager.createTopic(new MMXGlobalTopic(topic_name_transient), null);
          fail("expected failure when creating duplicate topic");
      } catch (MMXException e) {
          assertTrue(true);
      }
  }

  public void testBasicPublish() throws MMXException {
    MMXPubSubManager pubManager = mmxClient.getPubSubManager();
    try {
      pubManager.publish(new MMXGlobalTopic(topic_name), new MMXPayload(
          "weather today is cold at: " + System.currentTimeMillis()));
      pubManager.publish(new MMXGlobalTopic(topic_name), new MMXPayload(
          "traffic is congested as usual at: " + System.currentTimeMillis()));

    } catch (MMXException e) {
      fail("publish exception encountered" + e);
    }
    mmxClient.disconnect();
  }
}
