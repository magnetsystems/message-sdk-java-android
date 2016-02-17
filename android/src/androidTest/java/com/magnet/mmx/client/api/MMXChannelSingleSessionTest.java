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
import com.magnet.max.android.Attachment;
import com.magnet.max.android.MaxCore;
import com.magnet.max.android.User;
import com.magnet.max.android.UserProfile;
import com.magnet.mmx.client.api.MMXChannel.FailureCode;
import com.magnet.mmx.client.utils.ChannelHelper;
import com.magnet.mmx.client.utils.ExecMonitor;
import com.magnet.mmx.client.utils.MaxHelper;
import com.magnet.mmx.client.utils.TestCaseTimer;
import com.magnet.mmx.client.utils.TestConstants;
import com.magnet.mmx.client.utils.UserHelper;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static junit.framework.Assert.fail;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(AndroidJUnit4.class)
public class MMXChannelSingleSessionTest {
  private static final String TAG = MMXChannelSingleSessionTest.class.getSimpleName();

  @Rule
  public TestCaseTimer testCaseTimer = new TestCaseTimer();

  @BeforeClass
  public static void setUp() {
    MaxHelper.initMax();

    UserHelper.registerAndLogin(UserHelper.MMX_TEST_USER_1, UserHelper.MMX_TEST_USER_1);
  }

  @AfterClass
  public static void tearDown() {
    UserHelper.logout();
  }

  @Test
  public void testInviteToInavlidChannel() {
    String channelName = "channel" + System.currentTimeMillis();
    String channelSummary = channelName + " Summary";
    MMXChannel channel = new MMXChannel.Builder()
            .name(channelName)
            .summary(channelSummary)
            .setPublic(true)
            .build();
    HashSet<User> invitees = new HashSet<User>();
    invitees.add(MMX.getCurrentUser());
    Throwable expectedThrowable = null;
    try {
      channel.inviteUsers(invitees, "foo", null);
    } catch (Throwable ex) {
      expectedThrowable = ex;
    }
    assertThat(expectedThrowable).isNotNull();
    assertThat(expectedThrowable).isInstanceOf(RuntimeException.class);
  }

  @Test
  public void testPublicChannel() {
    String channelName = "channel" + System.currentTimeMillis();
    String channelSummary = channelName + " Summary";
    MMXChannel channel = ChannelHelper.create(channelName, channelSummary, true);
    ChannelHelper.createError(channel, FailureCode.CHANNEL_EXISTS);
    ChannelHelper.find(channelName, 1);
    ChannelHelper.subscribe(channel, 1);

    ChannelHelper.publish(channel);
    ChannelHelper.fetch(channel, 1);

    ChannelHelper.getChannelSummary(channelName, 1, 1);
    ChannelHelper.getPublicChannel(channelName, 1);
    ChannelHelper.unsubscribe(channel);
    ChannelHelper.delete(channel);
  }

  @Test
  public void testPrivateChannel() {
    String channelName = "private-channel" + System.currentTimeMillis();
    String channelSummary = channelName + " Summary";
    MMXChannel channel = ChannelHelper.create(channelName, channelSummary, false);
    ChannelHelper.createError(channel, FailureCode.CHANNEL_EXISTS);
    ChannelHelper.find(channelName, 0); // 0 because private channels should not show up on search
    ChannelHelper.subscribe(channel, 1);
    ChannelHelper.publish(channel);
    ChannelHelper.getChannelSummary(channelName, 0, 0); // 0 and 0 because this method will not be able to find private channels
    ChannelHelper.getPrivateChannel(channelName, 1);
    ChannelHelper.unsubscribe(channel);
    ChannelHelper.delete(channel);
  }

  @Test
  public void testPrivateChannelInvite() {
    helpTestChannelInvite(false);
  }

  @Test
  public void testPublicChannelInvite() {
    helpTestChannelInvite(true);
  }

  @Test
  public void testCreateChannelWithSubscribers() throws InterruptedException {

    String suffix = String.valueOf(System.currentTimeMillis());

    User user2 = UserHelper.registerUser(UserHelper.MMX_TEST_USER_2 + suffix, UserHelper.MMX_TEST_USER_2, UserHelper.MMX_TEST_USER_2, null, false);

    //helpLogin(MMX_TEST_USER_1);

    Set<User> subscribers = new HashSet<>();
    subscribers.add(user2);

    String channelName = "Chat_channel_" + suffix;
    String channelSummary = channelName + " Summary";
    final MMXChannel channel = ChannelHelper.create(channelName, channelSummary, false, subscribers);

    subscribers.add(User.getCurrentUser());

    List<MMXChannel> c1 = ChannelHelper.findChannelBySubscribers(subscribers);
    assertThat(c1.size()).isEqualTo(1);

    List<User> l1 = ChannelHelper.getSubscribers(channel);
    ChannelHelper.printCollection(subscribers, "subscribers after create");
    ChannelHelper.assertSubscribers(l1, subscribers);

    final Attachment
        attachment1 = new Attachment(MaxCore.getApplicationContext().getResources().openRawResource(
        com.magnet.mmx.test.R.raw.test_image), "image/jpeg");
    MMXMessage message1 = ChannelHelper.publish(channel, attachment1);
    ChannelHelper.fetch(channel, 1);

    // Channel detail
    List<ChannelDetail> channelDetails = ChannelHelper.getChannelDetail(channel);
    assertThat(channelDetails).hasSize(1);
    ChannelDetail channelDetail = channelDetails.get(0);
    assertThat(channelDetail.getChannel()).isEqualTo(channel);
    assertThat(channelDetail.getTotalMessages()).isEqualTo(1);
    assertThat(channelDetail.getTotalSubscribers()).isEqualTo(2);

    MMXMessage receivedMessage1 = channelDetail.getMessages().get(0);
    Log.d(TAG, "message received : " + receivedMessage1);
    assertThat(receivedMessage1.getId()).isEqualTo(message1.getId());
    assertThat(receivedMessage1.getAttachments()).hasSize(1);

    Attachment receivedAttachment1 = receivedMessage1.getAttachments().get(0);
    Log.d(TAG, "attchement received : " + receivedAttachment1);
    assertThat(receivedAttachment1.getAttachmentId()).isEqualTo(attachment1.getAttachmentId());

    assertThat(channelDetail.getSubscribers()).hasSize(2);
    for(UserProfile up : channelDetail.getSubscribers()) {
      Log.d(TAG, "testCreateChannelWithSubscribers, subscriber : " + up);
      if(up.getUserIdentifier().equals(User.getCurrentUserId())) {
        //assertThat(up.getFirstName()).isEqualTo(User.getCurrentUser().getFirstName());
        //assertThat(up.getLastName()).isEqualTo(User.getCurrentUser().getLastName());
        //assertThat(up.getDisplayName()).isEqualTo(User.getCurrentUser().getDisplayName());
      } else {
        assertThat(up.getDisplayName()).isEqualTo(user2.getDisplayName());
      }
    }

    //Add subscribers
    User user3 = UserHelper.registerUser(UserHelper.MMX_TEST_USER_3 + suffix, UserHelper.MMX_TEST_USER_3, UserHelper.MMX_TEST_USER_3, null, false);
    User user4 = UserHelper.registerUser(UserHelper.MMX_TEST_USER_4 + suffix, UserHelper.MMX_TEST_USER_4, UserHelper.MMX_TEST_USER_4, null, false);

    ChannelHelper.addSubscriber(channel, new HashSet<User>(Arrays.asList(user3, user4)));
    subscribers.add(user3);
    subscribers.add(user4);

    List<MMXChannel> c2 = ChannelHelper.findChannelBySubscribers(subscribers);
    assertThat(c2.size()).isEqualTo(1);

    List<User> l2 = ChannelHelper.getSubscribers(channel);
    ChannelHelper.printCollection(subscribers, "subscribers after add");
    ChannelHelper.assertSubscribers(l2, subscribers);

    ChannelHelper.removeSubscriber(channel, new HashSet<User>(Arrays.asList(user2, user3)));

    subscribers.remove(user2);
    subscribers.remove(user3);
    List<User> l3 = ChannelHelper.getSubscribers(channel);
    ChannelHelper.printCollection(subscribers, "subscribers after remove");
    ChannelHelper.assertSubscribers(l3, subscribers);

    List<MMXChannel> c3 = ChannelHelper.findChannelBySubscribers(subscribers);
    assertThat(c3.size()).isEqualTo(1);


    ChannelHelper.delete(channel);

    //final CountDownLatch unsubscribeLatch = new CountDownLatch(1);
    //channel.unsubscribe(new MMXChannel.OnFinishedListener<Boolean>() {
    //  @Override public void onSuccess(Boolean result) {
    //    unsubscribeLatch.countDown();
    //  }
    //
    //  @Override public void onFailure(FailureCode code, Throwable throwable) {
    //    fail("Failed to unsbuscribe channle " + channel.getName());
    //  }
    //});
    //unsubscribeLatch.await(10, TimeUnit.SECONDS);

    //logout();
    //helpLogin(MMX_TEST_USER_2);
  }

  protected void helpTestChannelInvite(boolean isPublic) {
    String channelName = (isPublic ? "public-channel" : "private-channel") + System.currentTimeMillis();
    String channelSummary = channelName + " Summary";
    MMXChannel channel = ChannelHelper.create(channelName, channelSummary, isPublic);

    final String INVITE_MESSAGE = "foobar";
    final String INVITE_RESPONSE_MESSAGE = INVITE_MESSAGE + " response";
    final ExecMonitor<Boolean, Boolean> inviteResponseValue = new ExecMonitor<Boolean, Boolean>();
    final StringBuffer inviteResponseText = new StringBuffer();
    final StringBuffer inviteTextBuffer = new StringBuffer();
    final CountDownLatch inviteReceiveLatch = new CountDownLatch(1);
    final CountDownLatch inviteResponseReceiveLatch = new CountDownLatch(1);
    MMX.EventListener inviteListener = new MMX.EventListener() {
      @Override
      public boolean onMessageReceived(MMXMessage message) {
        return false;
      }

      public boolean onInviteReceived(MMXChannel.MMXInvite invite) {
        assertThat(invite.getInviteInfo().getComment()).isEqualTo(INVITE_MESSAGE);
        invite.accept(INVITE_RESPONSE_MESSAGE, null);
        inviteReceiveLatch.countDown();
        return true;
      }

      public boolean onInviteResponseReceived(MMXChannel.MMXInviteResponse inviteResponse) {
        assertThat(inviteResponse.getResponseText()).isEqualTo(INVITE_RESPONSE_MESSAGE);
        inviteResponseReceiveLatch.countDown();
        return true;
      }
    };
    MMX.registerListener(inviteListener);

    final CountDownLatch inviteSendLatch = new CountDownLatch(1);
    channel.inviteUser(MMX.getCurrentUser(), INVITE_MESSAGE, new MMXChannel.OnFinishedListener<MMXChannel.MMXInvite>() {
      @Override
      public void onSuccess(MMXChannel.MMXInvite result) {
        inviteSendLatch.countDown();
      }

      @Override
      public void onFailure(FailureCode code, Throwable ex) {
        Log.e(TAG, "Exception caught: " + code, ex);
        fail("channel.inviteUser failed due to " + ex.getMessage());
      }
    });
    try {
      inviteSendLatch.await(TestConstants.TIMEOUT_IN_SECOND, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      fail("channel.inviteUser timeout");
    }

    try {
      inviteReceiveLatch.await(TestConstants.TIMEOUT_IN_SECOND, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      fail("receive invite timeout");
    }

    try {
      inviteResponseReceiveLatch.await(TestConstants.TIMEOUT_IN_SECOND, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      fail("receive invite response timeout");
    }


    ////test invite from the callback channel
    //final ExecMonitor<Boolean, Boolean> inviteFromCallbackSent = new ExecMonitor<Boolean, Boolean>();
    //channel.inviteUser(MMX.getCurrentUser(), "foobar", new MMXChannel.OnFinishedListener<MMXChannel.MMXInvite>() {
    //  @Override
    //  public void onSuccess(MMXChannel.MMXInvite result) {
    //    inviteFromCallbackSent.invoked(true);
    //  }
    //
    //  @Override
    //  public void onFailure(MMXChannel.FailureCode code, Throwable ex) {
    //    Log.e(TAG, "Exception caught: " + code, ex);
    //    inviteFromCallbackSent.failed(true);
    //  }
    //});
    //status = inviteFromCallbackSent.waitFor(TestConstants.TIMEOUT_IN_MILISEC);
    //assertEquals(ExecMonitor.Status.INVOKED, status);
    //assertTrue(inviteFromCallbackSent.getReturnValue());
    MMX.unregisterListener(inviteListener);
    ChannelHelper.delete(channel);
  }

  @Test
  public void testErrorHandling() {
    String suffix = String.valueOf(System.currentTimeMillis());
    String existChannelName = "exist-channel" + suffix;
    MMXChannel existChannel = ChannelHelper.create(existChannelName, null, true);
    ChannelHelper.createError(existChannel, FailureCode.CHANNEL_EXISTS);
    ChannelHelper.delete(existChannel);

    String noSuchChannelName = "no-such-channel" + suffix;
    MMXChannel noSuchChannel = new MMXChannel.Builder()
            .name(noSuchChannelName)
            .build();
    ChannelHelper.findError(noSuchChannelName, 0);
    ChannelHelper.getChannelSummaryError(noSuchChannelName, 0);
    ChannelHelper.subscribeError(noSuchChannel, FailureCode.CHANNEL_NOT_FOUND);
    ChannelHelper.publishError(noSuchChannel, FailureCode.CHANNEL_NOT_FOUND);
    ChannelHelper.unsubscribeError(noSuchChannel, FailureCode.CHANNEL_NOT_FOUND);
    ChannelHelper.deleteError(noSuchChannel, FailureCode.CHANNEL_NOT_FOUND);
  }


  @Test
  public void testFindError() {
    //find public channels
    final ExecMonitor<Integer, FailureCode> findNullResult = new ExecMonitor<Integer, FailureCode>();
    MMXChannel.findPublicChannelsByName(null, null, null, new MMXChannel.OnFinishedListener<ListResult<MMXChannel>>() {
      public void onSuccess(ListResult<MMXChannel> result) {
        findNullResult.invoked(result.totalCount);
      }

      @Override
      public void onFailure(FailureCode code, Throwable ex) {
        Log.e(TAG, "Exception caught: " + code, ex);
        findNullResult.failed(code);
      }
    });
    ExecMonitor.Status status = findNullResult.waitFor(TestConstants.TIMEOUT_IN_MILISEC);
    if (status == ExecMonitor.Status.INVOKED)
      fail("Find channel should have failed for findByName(null)");
    else if (status == ExecMonitor.Status.FAILED)
      assertThat(findNullResult.getFailedValue()).isEqualTo(MMX.FailureCode.BAD_REQUEST);
    else
      fail("Find channel timed out");

    //test empty
    final ExecMonitor<Integer, FailureCode> findEmptyResult = new ExecMonitor<Integer, FailureCode>();
    MMXChannel.findPublicChannelsByName("", null, null, new MMXChannel.OnFinishedListener<ListResult<MMXChannel>>() {
      public void onSuccess(ListResult<MMXChannel> result) {
        findEmptyResult.invoked(result.totalCount);
      }

      @Override
      public void onFailure(FailureCode code, Throwable ex) {
        Log.e(TAG, "Exception caught: " + code, ex);
        findEmptyResult.failed(code);
      }
    });
    status = findEmptyResult.waitFor(TestConstants.TIMEOUT_IN_MILISEC);
    if (status == ExecMonitor.Status.INVOKED)
      fail("Find channel should have failed for findByName(null)");
    else if (status == ExecMonitor.Status.FAILED)
      assertThat(findNullResult.getFailedValue()).isEqualTo(MMX.FailureCode.BAD_REQUEST);
    else
      fail("Find channel timed out");
  }
}
