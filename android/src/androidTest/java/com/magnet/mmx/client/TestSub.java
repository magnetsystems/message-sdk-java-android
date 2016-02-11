/*   Copyright (c) 2015 Magnet Systems, Inc.
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

import android.test.suitebuilder.annotation.Suppress;
import android.util.Log;
import com.magnet.mmx.client.common.MMXException;
import com.magnet.mmx.client.common.MMXGlobalTopic;
import com.magnet.mmx.client.common.MMXMessage;
import com.magnet.mmx.client.common.MMXSubscription;
import com.magnet.mmx.protocol.MMXTopic;
import com.magnet.mmx.protocol.MMXid;
import java.util.List;

//FIXME : those test cases need rewriting for 2.0
@Suppress
public class TestSub extends MMXInstrumentationTestCase {

  private static final String TAG = TestSub.class.getSimpleName();
  private static final String APP_NAME = "MyPubSubApp";
  private static final String MMS_URL = "http://localhost:8443";
  private static final String USER2_SUFFIX = "pubsub_test2";
  private static final String DEVICE2_ID = "computer-2";

  private static final String topic_name = "testpub1";
  private boolean messageReceived = false;

  @Override
  public void setUp() {
    connect(APP_NAME, MMS_URL, USER2_SUFFIX, DEVICE2_ID, new AbstractMMXListener() {
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

  public void testBasicSubscribe() throws MMXException {
    MMXPubSubManager pubManager = mmxClient.getPubSubManager();
    MMXTopic topic = new MMXGlobalTopic(topic_name);
    try {
      topic = pubManager.createTopic(topic, null);
    } catch (MMXException ex) {
      Log.i(TAG, "topic already exists: " + topic_name);
    }
    try {
      messageReceived = false;
      List<MMXSubscription> subs = pubManager.listSubscriptions(new MMXGlobalTopic(topic_name));
      for (MMXSubscription sub: subs) {
        // delete it so we don't dup it
        try {
          pubManager.unsubscribe(sub.getTopic(), sub.getId());
        } catch (MMXException e) {
          Log.e(TAG, "unexpected exception unsubscribing from topic: " + topic_name);
        }
      }
      pubManager.subscribe(topic, true);
      final boolean[] done = {false};
      // expect to get some messages now
      Thread waitThread = new Thread(new Runnable () {
        @Override
        public void run() {
          // wait for some messages to arrive
          synchronized (this) {
          int count = 0;
          while (count < 30) {
            try {
              Thread.sleep(100);
              count++;
            } catch (InterruptedException e) {
              e.printStackTrace();
            }
          }

          notify();
            done[0] = true;
        }
        }
      });
      waitThread.start();
      synchronized (waitThread) {
        while (!done[0]) {
          Log.d(TAG, "waiting for thread");
          Thread.sleep(100);
        }
      }
      mmxClient.disconnect();
    } catch (MMXException e) {
      Log.e(TAG, "MMXException for subscribeTopic", e);
      fail();
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
  }
}
