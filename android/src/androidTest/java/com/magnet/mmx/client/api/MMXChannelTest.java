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
package com.magnet.mmx.client.api;

import android.support.test.runner.AndroidJUnit4;
import android.util.Log;
import com.magnet.max.android.User;
import com.magnet.mmx.client.api.MMXChannel.FailureCode;
import com.magnet.mmx.client.utils.ChannelHelper;
import com.magnet.mmx.client.utils.ExecMonitor;
import com.magnet.mmx.client.utils.MaxHelper;
import com.magnet.mmx.client.utils.TestCaseTimer;
import com.magnet.mmx.client.utils.TestConstants;
import com.magnet.mmx.client.utils.UserHelper;
import java.util.List;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class MMXChannelTest {
  private static final String TAG = MMXChannelTest.class.getSimpleName();

  @Rule
  public TestCaseTimer testCaseTimer = new TestCaseTimer();

  @BeforeClass
  public static void setUp() {
    MaxHelper.initMax();
  }

  @AfterClass
  public static void tearDown() {
    UserHelper.logout();;
  }

  @Test
  public void testSomeonePrivateChannel() {
    UserHelper.logout();

    UserHelper.registerAndLogin(UserHelper.MMX_TEST_USER_2, UserHelper.MMX_TEST_USER_2);
    String channelName = "private-channel" + System.currentTimeMillis();
    String channelSummary = channelName + " Summary";
    MMXChannel channel = ChannelHelper.create(channelName, channelSummary, false);

    ChannelHelper.publish(channel);
    ChannelHelper.fetch(channel, 1);

    UserHelper.logout();
    UserHelper.registerAndLogin(UserHelper.MMX_TEST_USER_3, UserHelper.MMX_TEST_USER_3);

    ChannelHelper.find(channelName, 0);   // expect 0 because someone private channel cannot be searched

    UserHelper.logout();
    UserHelper.login(UserHelper.MMX_TEST_USER_2, UserHelper.MMX_TEST_USER_2);

    ChannelHelper.delete(channel);
  }

  @Test
  public void testGetAllSubscriptions() {
    String timestamp = String.valueOf(System.currentTimeMillis());

    UserHelper.logout();
    UserHelper.registerAndLogin(UserHelper.MMX_TEST_USER_PREFIX + timestamp, UserHelper.MMX_TEST_USER_PREFIX + timestamp);

    String privateChannelName = "private-channel" + timestamp;
    String privateChannelSummary = privateChannelName + " Summary";
    MMXChannel privateChannel = ChannelHelper.create(privateChannelName, privateChannelSummary, false);

    String publicChannelName = "channel" + timestamp;
    String publicChannelSummary = publicChannelName + " Summary";
    MMXChannel publicChannel = ChannelHelper.create(publicChannelName, publicChannelSummary, true);

    ChannelHelper.getAllSubscriptions(2);

    ChannelHelper.delete(publicChannel);
    ChannelHelper.delete(privateChannel);
  }

  @Test
  public void testGetSubscribedPrivateChannel() {
    final String CHANNEL_NAME = "channel1";

    UserHelper.logout();
    
    // Register and login as user1, create and auto-subscribe a private channel.
    String suffix = String.valueOf(System.currentTimeMillis());
    UserHelper.registerAndLogin(UserHelper.MMX_TEST_USER_2, UserHelper.MMX_TEST_USER_2);
    User user1 = MMX.getCurrentUser();
    MMXChannel channel = ChannelHelper.create(CHANNEL_NAME + suffix, "Private Channel 1", false);
    UserHelper.logout();
    
    // Register and login as user2, subscribe to the private channel.  It
    // should have 2 subscribers: user1 and user2.
    final String newUser = "newSubUser1" + suffix;
    UserHelper.registerAndLogin(newUser, newUser);
    ChannelHelper.subscribe(channel, 2);
    // Get the subscribed channel information.

    List<MMXChannel> subChannels = ChannelHelper.getAllSubscriptions(1);
    assertNotNull(subChannels);
    assertEquals(1, subChannels.size());
    assertEquals(user1.getUserIdentifier(), subChannels.get(0).getOwnerId());

    UserHelper.logout();
    
    // Login as user1 again and delete the channel.
    UserHelper.login(UserHelper.MMX_TEST_USER_2, UserHelper.MMX_TEST_USER_2);
    ChannelHelper.delete(channel);
  }
  
  /**
   * Create 3 private channels and 1 public channel.  Publish 2 items to each
   * private channel.  Get all private channels and it should be 3.  Validate each private
   * channel to have 2 items.
   */
  @Test
  public void testGetAllPrivateChannels() {
    UserHelper.logout();

    String suffix = "testGetAllPrivateChannels" + String.valueOf(System.currentTimeMillis());
    String userName = UserHelper.MMX_TEST_USER_PREFIX + suffix;
    UserHelper.registerAndLogin(userName, userName);

    MMXChannel pubChannel = ChannelHelper.create("public_ch4" + suffix, "Ch4" + suffix, true);   // public channel

    MMXChannel[] channels = {
        new MMXChannel.Builder().name("private_ch1" + suffix).summary("Ch1" + suffix).build(),
        new MMXChannel.Builder().name("private_ch2" + suffix).summary("Ch2" + suffix).build(),
        new MMXChannel.Builder().name("private_ch3" + suffix).summary("Ch3" + suffix).build() };

    for (int i = 0; i < 3; i++) {
      MMXChannel channel = ChannelHelper.create(channels[i].getName(), channels[i].getSummary(), false);
      ChannelHelper.publish(channel);
      ChannelHelper.publish(channel);
    }

    // getting all private channels
    final ExecMonitor<ListResult<MMXChannel>, FailureCode> channelsRes = new ExecMonitor<ListResult<MMXChannel>, FailureCode>();
    MMXChannel.getAllPrivateChannels(null, null, new MMXChannel.OnFinishedListener<ListResult<MMXChannel>>() {
      public void onSuccess(ListResult<MMXChannel> result) {
        channelsRes.invoked(result);
      }

      public void onFailure(MMXChannel.FailureCode code, Throwable ex) {
        Log.d(TAG, "failed to getAllPrivateChannels : " + code, ex);
        channelsRes.failed(code);
      }
    });
    ExecMonitor.Status status = channelsRes.waitFor(TestConstants.TIMEOUT_IN_MILISEC);
    assertEquals(ExecMonitor.Status.INVOKED, status);
    ListResult<MMXChannel> priChannels = channelsRes.getReturnValue();
    assertNotNull(priChannels);
    assertEquals(channels.length, priChannels.totalCount);  // 3 private channels
    for (MMXChannel priChannel : priChannels.items) {
      assertEquals(2, priChannel.getNumberOfMessages().intValue());
      assertNotNull(priChannel.getName());
      assertNotNull(priChannel.getSummary());
      assertNotNull(priChannel.getOwnerId());
      assertFalse(priChannel.isPublic());
      assertTrue(priChannel.isSubscribed());
    }

    ChannelHelper.delete(pubChannel);
    for (int i = 0; i < 3; i++) {
      ChannelHelper.delete(channels[i]);
    }
  }
}
