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

import com.magnet.max.android.Attachment;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import android.util.Log;

import com.magnet.max.android.User;
import com.magnet.mmx.client.api.MMXChannel.FailureCode;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

public class MMXChannelTest extends MMXInstrumentationTestCase {
  private static final String TAG = MMXChannelTest.class.getSimpleName();

  public void postSetUp() {
    helpLogin(MMX_TEST_USER_1);
  }

  @Override
  public void tearDown() {
    Log.d(TAG, "------tearDown test " + getName());
    helpLogout();
  }

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
    assertNotNull(expectedThrowable);
    assertTrue(expectedThrowable instanceof RuntimeException);
  }

  public void testPublicChannel() {
    String channelName = "channel" + System.currentTimeMillis();
    String channelSummary = channelName + " Summary";
    MMXChannel channel = helpCreate(channelName, channelSummary, true);
    helpCreateError(channel, MMXChannel.FailureCode.CHANNEL_EXISTS);
    helpFind(channelName, 1);
    helpSubscribe(channel, 1);

    helpPublish(channel);
    helpFetch(channel, 1);

    helpChannelSummary(channelName, 1, 1);
    helpGetPublicChannel(channelName, 1);
    helpUnsubscribe(channel);
    helpDelete(channel);
  }

  public void testPrivateChannel() {
    String channelName = "private-channel" + System.currentTimeMillis();
    String channelSummary = channelName + " Summary";
    MMXChannel channel = helpCreate(channelName, channelSummary, false);
    helpCreateError(channel, MMXChannel.FailureCode.CHANNEL_EXISTS);
    helpFind(channelName, 0); // 0 because private channels should not show up on search
    helpSubscribe(channel, 1);
    helpPublish(channel);
    helpChannelSummary(channelName, 0, 0); // 0 and 0 because this method will not be able to find private channels
    helpGetPrivateChannel(channelName, 1);
    helpUnsubscribe(channel);
    helpDelete(channel);
  }

  public void testSomeonePrivateChannel() {
    helpLogout();

    helpLogin(MMX_TEST_USER_2);
    String channelName = "private-channel" + System.currentTimeMillis();
    String channelSummary = channelName + " Summary";
    MMXChannel channel = helpCreate(channelName, channelSummary, false);

    helpPublish(channel);
    helpFetch(channel, 1);

    helpLogout();
    helpLogin(MMX_TEST_USER_3);

    helpFind(channelName, 0);   // expect 0 because someone private channel cannot be searched

    helpLogout();
    helpLogin(MMX_TEST_USER_2);

    helpDelete(channel);
  }

  public void testPrivateChannelInvite() {
    helpTestChannelInvite(false);
  }

  public void testPublicChannelInvite() {
    helpTestChannelInvite(true);
  }

  public void testCreateChannelWithSubscribers() throws InterruptedException {

    String suffix = String.valueOf(System.currentTimeMillis());

    User user2 = helpRegisterUser(MMX_TEST_USER_2 + suffix, MMX_TEST_USER_2, MMX_TEST_USER_2.getBytes(), null, null, false);

    //helpLogin(MMX_TEST_USER_1);

    Set<User> subscribers = new HashSet<>();
    subscribers.add(user2);

    String channelName = "Chat_channel_" + suffix;
    String channelSummary = channelName + " Summary";
    final MMXChannel channel = helpCreate(channelName, channelSummary, false, subscribers);

    subscribers.add(User.getCurrentUser());

    List<MMXChannel> c1 = helpFindChannelBySubscribers(subscribers);
    assertThat(c1.size()).isEqualTo(1);

    List<User> l1 = helpGetSubscribers(channel);
    printCollection(subscribers, "subscribers after create");
    assertSubscribers(l1, subscribers);

    helpPublish(channel);
    helpFetch(channel, 1);

    //Add subscribers
    User user3 = helpRegisterUser(MMX_TEST_USER_3 + suffix, MMX_TEST_USER_3, MMX_TEST_USER_3.getBytes(), null, null, false);
    User user4 = helpRegisterUser(MMX_TEST_USER_4 + suffix, MMX_TEST_USER_4, MMX_TEST_USER_4.getBytes(), null, null, false);

    helpAddSubscriber(channel, new HashSet<User>(Arrays.asList(user3, user4)));
    subscribers.add(user3);
    subscribers.add(user4);

    List<MMXChannel> c2 = helpFindChannelBySubscribers(subscribers);
    assertThat(c2.size()).isEqualTo(1);

    List<User> l2 = helpGetSubscribers(channel);
    printCollection(subscribers, "subscribers after add");
    assertSubscribers(l2, subscribers);

    helpRemoveSubscriber(channel, new HashSet<User>(Arrays.asList(user2, user3)));

    subscribers.remove(user2);
    subscribers.remove(user3);
    List<User> l3 = helpGetSubscribers(channel);
    printCollection(subscribers, "subscribers after remove");
    assertSubscribers(l3, subscribers);

    List<MMXChannel> c3 = helpFindChannelBySubscribers(subscribers);
    assertThat(c3.size()).isEqualTo(1);


    helpDelete(channel);

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

    //helpLogout();
    //helpLogin(MMX_TEST_USER_2);
  }

  public void helpTestChannelInvite(boolean isPublic) {
    String channelName = (isPublic ? "public-channel" : "private-channel") + System.currentTimeMillis();
    String channelSummary = channelName + " Summary";
    MMXChannel channel = helpCreate(channelName, channelSummary, isPublic);

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
      public void onFailure(MMXChannel.FailureCode code, Throwable ex) {
        Log.e(TAG, "Exception caught: " + code, ex);
        fail("channel.inviteUser failed due to " + ex.getMessage());
      }
    });
    try {
      inviteSendLatch.await(TIMEOUT_IN_SECOND, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      fail("channel.inviteUser timeout");
    }

    try {
      inviteReceiveLatch.await(TIMEOUT_IN_SECOND, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      fail("receive invite timeout");
    }

    try {
      inviteResponseReceiveLatch.await(TIMEOUT_IN_SECOND, TimeUnit.SECONDS);
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
    //status = inviteFromCallbackSent.waitFor(TIMEOUT_IN_MILISEC);
    //assertEquals(ExecMonitor.Status.INVOKED, status);
    //assertTrue(inviteFromCallbackSent.getReturnValue());
    MMX.unregisterListener(inviteListener);
    helpDelete(channel);
  }

  public void testGetAllSubscriptions() {
    String timestamp = String.valueOf(System.currentTimeMillis());

    helpLogout();
    helpLogin(MMX_TEST_USER_PREFIX + timestamp);

    String privateChannelName = "private-channel" + timestamp;
    String privateChannelSummary = privateChannelName + " Summary";
    MMXChannel privateChannel = helpCreate(privateChannelName, privateChannelSummary, false);

    String publicChannelName = "channel" + timestamp;
    String publicChannelSummary = publicChannelName + " Summary";
    MMXChannel publicChannel = helpCreate(publicChannelName, publicChannelSummary, true);

    helpGetAllSubscriptions(2);

    helpDelete(publicChannel);
    helpDelete(privateChannel);
  }

  public void testErrorHandling() {
    String suffix = String.valueOf(System.currentTimeMillis());
    String existChannelName = "exist-channel" + suffix;
    MMXChannel existChannel = helpCreate(existChannelName, null, true);
    helpCreateError(existChannel, MMXChannel.FailureCode.CHANNEL_EXISTS);
    helpDelete(existChannel);
    
    String noSuchChannelName = "no-such-channel" + suffix;
    MMXChannel noSuchChannel = new MMXChannel.Builder()
            .name(noSuchChannelName)
            .build();
    helpFindError(noSuchChannelName, 0);
    helpChannelSummaryError(noSuchChannelName, 0);
    helpSubscribeError(noSuchChannel, MMXChannel.FailureCode.CHANNEL_NOT_FOUND);
    helpPublishError(noSuchChannel, MMXChannel.FailureCode.CHANNEL_NOT_FOUND);
    helpUnsubscribeError(noSuchChannel, MMXChannel.FailureCode.CHANNEL_NOT_FOUND);
    helpDeleteError(noSuchChannel, MMXChannel.FailureCode.CHANNEL_NOT_FOUND);
  }
  
  public void testGetSubscribedPrivateChannel() {
    final String CHANNEL_NAME = "channel1";

    helpLogout();
    
    // Register and login as user1, create and auto-subscribe a private channel.
    String suffix = String.valueOf(System.currentTimeMillis());
    helpLogin(MMX_TEST_USER_2);
    User user1 = MMX.getCurrentUser();
    MMXChannel channel = helpCreate(CHANNEL_NAME + suffix, "Private Channel 1", false);
    helpLogout();
    
    // Register and login as user2, subscribe to the private channel.  It
    // should have 2 subscribers: user1 and user2.
    final String newUser = "newSubUser1";
    helpLogin(newUser, newUser, newUser, suffix, true);
    helpSubscribe(channel, 2);
    // Get the subscribed channel information.

    List<MMXChannel> subChannels = helpGetAllSubscriptions(1);
    assertNotNull(subChannels);
    assertEquals(1, subChannels.size());
    assertEquals(user1.getUserIdentifier(), subChannels.get(0).getOwnerId());

    helpLogout();
    
    // Login as user1 again and delete the channel.
    helpLogin(MMX_TEST_USER_2);
    helpDelete(channel);
  }
  
  /**
   * Create 3 private channels and 1 public channel.  Publish 2 items to each
   * private channel.  Get all private channels and it should be 3.  Validate each private
   * channel to have 2 items.
   */
  public void testGetAllPrivateChannels() {
    String suffix = String.valueOf(System.currentTimeMillis());

    helpLogout();
    helpLogin(MMX_TEST_USER_PREFIX + suffix);

    MMXChannel pubChannel = helpCreate("public_ch4" + suffix, "Ch4" + suffix, true);   // public channel

    MMXChannel[] channels = {
        new MMXChannel.Builder().name("private_ch1" + suffix).summary("Ch1" + suffix).build(),
        new MMXChannel.Builder().name("private_ch2" + suffix).summary("Ch2" + suffix).build(),
        new MMXChannel.Builder().name("private_ch3" + suffix).summary("Ch3" + suffix).build() };

    for (int i = 0; i < 3; i++) {
      MMXChannel channel = helpCreate(channels[i].getName(), channels[i].getSummary(), false);
      helpPublish(channel);
      helpPublish(channel);
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
    ExecMonitor.Status status = channelsRes.waitFor(TIMEOUT_IN_MILISEC);
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

    helpDelete(pubChannel);
    for (int i = 0; i < 3; i++) {
      helpDelete(channels[i]);
    }
  }

  public void testFindError() {
    //find public channels 
    final ExecMonitor<Integer, FailureCode> findNullResult = new ExecMonitor<Integer, FailureCode>();
    MMXChannel.findPublicChannelsByName(null, null, null, new MMXChannel.OnFinishedListener<ListResult<MMXChannel>>() {
      public void onSuccess(ListResult<MMXChannel> result) {
        findNullResult.invoked(result.totalCount);
      }

      @Override
      public void onFailure(MMXChannel.FailureCode code, Throwable ex) {
        Log.e(TAG, "Exception caught: " + code, ex);
        findNullResult.failed(code);
      }
    });
    ExecMonitor.Status status = findNullResult.waitFor(TIMEOUT_IN_MILISEC);
    if (status == ExecMonitor.Status.INVOKED)
      fail("Find channel should have failed for findByName(null)");
    else if (status == ExecMonitor.Status.FAILED)
      assertEquals(MMX.FailureCode.BAD_REQUEST, findNullResult.getFailedValue());
    else
      fail("Find channel timed out");

    //test empty
    final ExecMonitor<Integer, FailureCode> findEmptyResult = new ExecMonitor<Integer, FailureCode>();
    MMXChannel.findPublicChannelsByName("", null, null, new MMXChannel.OnFinishedListener<ListResult<MMXChannel>>() {
      public void onSuccess(ListResult<MMXChannel> result) {
        findEmptyResult.invoked(result.totalCount);
      }

      @Override
      public void onFailure(MMXChannel.FailureCode code, Throwable ex) {
        Log.e(TAG, "Exception caught: " + code, ex);
        findEmptyResult.failed(code);
      }
    });
    status = findEmptyResult.waitFor(TIMEOUT_IN_MILISEC);
    if (status == ExecMonitor.Status.INVOKED)
      fail("Find channel should have failed for findByName(null)");
    else if (status == ExecMonitor.Status.FAILED)
      assertEquals(MMX.FailureCode.BAD_REQUEST, findNullResult.getFailedValue());
    else
      fail("Find channel timed out");
  }
  
  //**************
  //HELPER METHODS
  //**************

  private MMXChannel helpCreate(String name, String summary, boolean isPublic) {
    final ExecMonitor<MMXChannel, Void> createResult = new ExecMonitor<MMXChannel, Void>();
    MMXChannel.create(name, summary, isPublic, MMXChannel.PublishPermission.ANYONE,
        new MMXChannel.OnFinishedListener<MMXChannel>() {
          public void onSuccess(MMXChannel result) {
            Log.e(TAG, "helpCreate.onSuccess ");
            createResult.invoked(result);
          }

          public void onFailure(MMXChannel.FailureCode code, Throwable ex) {
            Log.e(TAG, "Exception caught: " + code, ex);
            createResult.invoked(null);
          }
        });
    MMXChannel result = null;
    if (createResult.waitFor(TIMEOUT_IN_MILISEC) == ExecMonitor.Status.INVOKED) {
      result = createResult.getReturnValue();
      assertNotNull(result);
      assertNotNull(result.getOwnerId());
      assertEquals(MMX.getCurrentUser().getUserIdentifier(), result.getOwnerId());
      assertEquals(summary, result.getSummary());
      assertEquals(name, result.getName());
      assertEquals(isPublic, result.isPublic());
      assertNotNull(result.getCreationDate());
    } else {
      fail("Channel creation timed out");
    }
    return result;
  }


  private MMXChannel helpCreate(String name, String summary, boolean isPublic, Set<User> subscribers) {
    final ExecMonitor<MMXChannel, Void> createResult = new ExecMonitor<MMXChannel, Void>();
    MMXChannel.create(name, summary, isPublic, MMXChannel.PublishPermission.ANYONE, userSetToIds(subscribers),
        new MMXChannel.OnFinishedListener<MMXChannel>() {
          public void onSuccess(MMXChannel result) {
            Log.e(TAG, "helpCreate.onSuccess ");
            createResult.invoked(result);
          }

          public void onFailure(MMXChannel.FailureCode code, Throwable ex) {
            Log.e(TAG, "Exception caught: " + code, ex);
            createResult.invoked(null);
          }
        });
    MMXChannel result = null;
    if (createResult.waitFor(TIMEOUT_IN_MILISEC) == ExecMonitor.Status.INVOKED) {
      result = createResult.getReturnValue();
      assertNotNull(result);
      assertNotNull(result.getOwnerId());
      assertEquals(MMX.getCurrentUser().getUserIdentifier(), result.getOwnerId());
      assertEquals(summary, result.getSummary());
      assertEquals(name, result.getName());
      assertEquals(isPublic, result.isPublic());
      assertNotNull(result.getCreationDate());
    } else {
      fail("Channel creation timed out");
    }
    return result;
  }
  
  private void helpCreateError(MMXChannel channel, final FailureCode expected) {
    final ExecMonitor<FailureCode, String> obj = new ExecMonitor<FailureCode, String>();
    MMXChannel.create(channel.getName(), channel.getSummary(), channel.isPublic(), channel.getPublishPermission(),
            new MMXChannel.OnFinishedListener<MMXChannel>() {
      @Override
      public void onSuccess(MMXChannel result) {
        obj.failed("Unexpected success on creating an existing channel");
      }

      @Override
      public void onFailure(FailureCode code, Throwable throwable) {
        obj.invoked(code);
      }
    });
    if (obj.waitFor(TIMEOUT_IN_MILISEC) == ExecMonitor.Status.FAILED)
      fail(obj.getFailedValue());
    else
      assertEquals(expected, obj.getReturnValue());
  }


  private void helpFind(String channelName, int expectedCount) {
    //find
    final ExecMonitor<Integer, FailureCode> findResult = new ExecMonitor<Integer, FailureCode>();
    MMXChannel.findPublicChannelsByName(channelName, null, null, new MMXChannel.OnFinishedListener<ListResult<MMXChannel>>() {
      public void onSuccess(ListResult<MMXChannel> result) {
        findResult.invoked(result.totalCount);
      }

      @Override
      public void onFailure(MMXChannel.FailureCode code, Throwable ex) {
        Log.e(TAG, "Exception caught: " + code, ex);
        findResult.failed(code);
      }
    });
    ExecMonitor.Status status = findResult.waitFor(TIMEOUT_IN_MILISEC);
    if (status == ExecMonitor.Status.INVOKED)
      assertEquals(expectedCount, findResult.getReturnValue().intValue());
    else if (status == ExecMonitor.Status.FAILED)
      fail("Find channel failed: " + findResult.getFailedValue());
    else
      fail("Find channel timed out");
  }
  
  private void helpFindError(String channelName, final int expected) {
    final ExecMonitor<ListResult<MMXChannel>, String> obj = new ExecMonitor<ListResult<MMXChannel>, String>();
    MMXChannel.findPublicChannelsByName(channelName, null, null, new MMXChannel.OnFinishedListener<ListResult<MMXChannel>>() {
      @Override
      public void onSuccess(ListResult<MMXChannel> result) {
        obj.invoked(result);
      }

      @Override
      public void onFailure(FailureCode code, Throwable ex) {
        obj.failed("Unexpected failure on finding a non-existing channel");
      }
    });
    ExecMonitor.Status status = obj.waitFor(TIMEOUT_IN_MILISEC);
    if (status == ExecMonitor.Status.FAILED)
      fail(obj.getFailedValue());
    else if (status == ExecMonitor.Status.INVOKED)
      assertEquals(expected, obj.getReturnValue().totalCount);
    else
      fail("Find non-existing channel timeout");
  }

  private void helpSubscribe(MMXChannel channel, final int expectedSubscriberCount) {
    //subscribe
    final CountDownLatch subScribeLatch = new CountDownLatch(1);
    final AtomicReference<Throwable> errorRef = new AtomicReference<>();
    channel.subscribe(new MMXChannel.OnFinishedListener<String>() {
      public void onSuccess(String result) {
        subScribeLatch.countDown();
      }

      public void onFailure(MMXChannel.FailureCode code, Throwable ex) {
        Log.e(TAG, "Exception caught: " + code, ex);
        errorRef.set(ex);
        //fail("channel.subscribe failed due to " + ex.getMessage());
        subScribeLatch.countDown();
      }
    });
    try {
      subScribeLatch.await(TIMEOUT_IN_SECOND, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      fail("channel.subscribe timeout");
    }
    assertThat(errorRef.get()).isNull();
    assertTrue(channel.isSubscribed());

    final CountDownLatch getSubScribesLatch = new CountDownLatch(1);
    errorRef.set(null);
    channel.getAllSubscribers(0, 100, new MMXChannel.OnFinishedListener<ListResult<User>>() {
      public void onSuccess(ListResult<User> result) {
        assertThat(result.totalCount).isEqualTo(expectedSubscriberCount);
        getSubScribesLatch.countDown();
      }

      public void onFailure(MMXChannel.FailureCode code, Throwable ex) {
        Log.e(TAG, "Exception caught: " + code, ex);
        //fail("channel.getAllSubscribers failed due to " + ex.getMessage());
        errorRef.set(ex);
        getSubScribesLatch.countDown();
      }
    });
    try {
      getSubScribesLatch.await(TIMEOUT_IN_SECOND, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      fail("channel.getAllSubscribers timeout");
    }
    assertThat(errorRef.get()).isNull();
  }
  
  private void helpSubscribeError(MMXChannel channel, final FailureCode expected) {
    final ExecMonitor<FailureCode, String> obj = new ExecMonitor<FailureCode, String>();
    channel.subscribe(new MMXChannel.OnFinishedListener<String>() {
      @Override
      public void onSuccess(String result) {
        obj.failed("Unexpected success on subscribing a non-existing channel");
      }
      @Override
      public void onFailure(FailureCode code, Throwable throwable) {
        obj.invoked(code);
      }
    });
    if (obj.waitFor(TIMEOUT_IN_MILISEC) == ExecMonitor.Status.FAILED)
      fail(obj.getFailedValue());
    else
      assertEquals(expected, obj.getReturnValue());
  }

  private void helpPublish(MMXChannel channel) {
    helpPublish(channel, null);
  }

  private void helpPublish(MMXChannel channel, Attachment attachment) {
    //setup message listener to receive published message
    final StringBuffer barBuffer = new StringBuffer();
    final StringBuffer senderBuffer = new StringBuffer();
    final CountDownLatch receiveMessageLatch = new CountDownLatch(1);
    final MMX.EventListener messageListener = new MMX.EventListener() {
      @Override
      public boolean onMessageReceived(MMXMessage message) {
        String bar = message.getContent().get("foo");
        //FIXME:  Check the sender name/displayname
        User sender = message.getSender();
        if (sender != null) {
          senderBuffer.append(sender.getFirstName());
        }
        if (bar != null) {
          barBuffer.append(bar);
        }
        receiveMessageLatch.countDown();
        return false;
      }

      @Override
      public boolean onMessageAcknowledgementReceived(User from, String messageId) {
        return false;
      }
    };
    MMX.registerListener(messageListener);

    //publish
    final CountDownLatch sendMessageLatch = new CountDownLatch(1);
    final StringBuffer pubId = new StringBuffer();
    HashMap<String, String> content = new HashMap<String, String>();
    content.put("foo", "bar");
    String id = channel.publish(content, new MMXChannel.OnFinishedListener<String>() {
      public void onSuccess(String result) {
        pubId.append(result);
        sendMessageLatch.countDown();
      }

      public void onFailure(MMXChannel.FailureCode code, Throwable ex) {
        Log.e(TAG, "Exception caught: " + code, ex);
        fail("channel.publish failed due to " + ex.getMessage());
      }
    });
    try {
      sendMessageLatch.await(TIMEOUT_IN_SECOND, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      fail("channel.publish timeout");
    }

    try {
      Thread.sleep(SLEEP_IN_MILISEC);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    try {
      receiveMessageLatch.await(TIMEOUT_IN_SECOND, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      fail("receive published message timeout");
    }
    MMX.unregisterListener(messageListener);
  }
  
  private void helpPublishError(MMXChannel channel, final FailureCode expected) {
    final ExecMonitor<FailureCode, String> obj = new ExecMonitor<FailureCode, String>();
    HashMap<String, String> content = new HashMap<String, String>();
    content.put("foo", "bar");
    String id = channel.publish(content, new MMXChannel.OnFinishedListener<String>() {
      @Override
      public void onSuccess(String result) {
        obj.failed("Unexpected success on publishing to a non-existing channel");
      }

      @Override
      public void onFailure(FailureCode code, Throwable throwable) {
        obj.invoked(code);
      }
    });
    if (obj.waitFor(TIMEOUT_IN_MILISEC) == ExecMonitor.Status.FAILED)
      fail(obj.getFailedValue());
    else
      assertEquals(expected, obj.getReturnValue());
  }

  private void helpChannelSummary(String channelName, final int expectedChannelCount, final int expectedItemCount) {
    //get topic again
    //final AtomicInteger itemCount = new AtomicInteger(0);
    final CountDownLatch latch = new CountDownLatch(1);
    final AtomicReference<ListResult<MMXChannel>> resultRef = new AtomicReference<>();
    final AtomicReference<Throwable> errorRef = new AtomicReference<>();
    MMXChannel.findPublicChannelsByName(channelName, null, null, new MMXChannel.OnFinishedListener<ListResult<MMXChannel>>() {
      @Override
      public void onSuccess(ListResult<MMXChannel> result) {
        resultRef.set(result);
        latch.countDown();
      }

      @Override
      public void onFailure(MMXChannel.FailureCode code, Throwable ex) {
        Log.e(TAG, "Exception caught in MMXChannel.findPublicChannelsByName : " + code, ex);
        errorRef.set(ex);
      }
    });

    try {
      latch.await(TIMEOUT_IN_SECOND, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      fail("MMXChannel.findPublicChannelsByName timed out");
    }

    if(null != errorRef.get()) {
      fail("MMXChannel.findPublicChannelsByName failed due to " + errorRef.get().getMessage());
    } else {
      assertThat(resultRef.get()).isNotNull();
      ListResult<MMXChannel> result = resultRef.get();
      assertThat(result.totalCount).isEqualTo(expectedChannelCount);
      if (result.items.size() > 0) {
        assertThat(result.items.get(0).getNumberOfMessages()).isEqualTo(expectedItemCount);
      }
    }
  }
  
  private void helpChannelSummaryError(String channelName, int expected) {
    final ExecMonitor<ListResult<MMXChannel>, String> obj = new ExecMonitor<ListResult<MMXChannel>, String>();
    MMXChannel.findPublicChannelsByName(channelName, null, null, new MMXChannel.OnFinishedListener<ListResult<MMXChannel>>() {
      @Override
      public void onSuccess(ListResult<MMXChannel> result) {
        obj.invoked(result);
      }

      @Override
      public void onFailure(FailureCode code, Throwable throwable) {
        obj.failed("Unexpected failure on channel summary of a non-existing channel");
      }
    });
    ExecMonitor.Status status = obj.waitFor(TIMEOUT_IN_MILISEC);
    if (status == ExecMonitor.Status.FAILED)
      fail(obj.getFailedValue());
    else if (status == ExecMonitor.Status.INVOKED)
      assertEquals(expected, obj.getReturnValue().totalCount);
    else
      fail("Getting channel summary timed out");
  }


  private void helpUnsubscribe(MMXChannel channel) {
    //unsubscribe
    final ExecMonitor<Boolean, FailureCode> unsubResult = new ExecMonitor<Boolean, FailureCode>();
    channel.unsubscribe(new MMXChannel.OnFinishedListener<Boolean>() {
      @Override
      public void onSuccess(Boolean result) {
        unsubResult.invoked(result);
      }

      @Override
      public void onFailure(MMXChannel.FailureCode code, Throwable ex) {
        Log.e(TAG, "Exception caught: " + code, ex);
        unsubResult.failed(code);
      }
    });
    ExecMonitor.Status status = unsubResult.waitFor(TIMEOUT_IN_MILISEC);
    if (status == ExecMonitor.Status.INVOKED)
      assertTrue(unsubResult.getReturnValue());
    else if (status == ExecMonitor.Status.FAILED)
      fail("Channel unsubscription failed: "+unsubResult.getFailedValue());
    else
      fail("Channel unsubscription timed out");
    //make sure the flag is set to false
    assertFalse(channel.isSubscribed());
  }

  private void helpUnsubscribeError(MMXChannel channel, final FailureCode expected) {
    final ExecMonitor<FailureCode, String> obj = new ExecMonitor<FailureCode, String>();
    channel.unsubscribe(new MMXChannel.OnFinishedListener<Boolean>() {
      @Override
      public void onSuccess(Boolean result) {
        obj.failed("Unexpected success on unsubscribing a non-existing channel");
      }

      @Override
      public void onFailure(FailureCode code, Throwable throwable) {
        obj.invoked(code);
      }
    });
    if (obj.waitFor(TIMEOUT_IN_MILISEC) == ExecMonitor.Status.FAILED)
      fail(obj.getFailedValue());
    else
      assertEquals(expected, obj.getReturnValue());
  }


  private void helpDelete(MMXChannel channel) {
    //delete
    final ExecMonitor<Boolean, Void> deleteResult = new ExecMonitor<Boolean, Void>();
    channel.delete(new MMXChannel.OnFinishedListener<Void>() {
      @Override
      public void onSuccess(Void result) {
        deleteResult.invoked(true);
      }

      @Override
      public void onFailure(MMXChannel.FailureCode code, Throwable ex) {
        Log.e(TAG, "Exception caught: " + code, ex);
        deleteResult.invoked(false);
      }
    });
    if (deleteResult.waitFor(TIMEOUT_IN_MILISEC) == ExecMonitor.Status.INVOKED)
      assertTrue(deleteResult.getReturnValue());
    else
      fail("Channel deletion timed out");
  }
  
  private void helpDeleteError(MMXChannel channel, final FailureCode expected) {
    final ExecMonitor<FailureCode, String> obj = new ExecMonitor<FailureCode, String>();
    channel.delete(new MMXChannel.OnFinishedListener<Void>() {
      @Override
      public void onSuccess(Void result) {
        obj.failed("Unexpected success on deleting a non-existing channel");
      }

      @Override
      public void onFailure(FailureCode code, Throwable throwable) {
        obj.invoked(code);
      }
    });
    if (obj.waitFor(TIMEOUT_IN_MILISEC) == ExecMonitor.Status.FAILED)
      fail(obj.getFailedValue());
    else
      assertEquals(expected, obj.getReturnValue());
  }

  private void helpFetch(MMXChannel channel, final int expectedCount) {
    try {
      Thread.sleep(SLEEP_IN_MILISEC);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    //test basic fetch
    final CountDownLatch latch = new CountDownLatch(1);
    final AtomicReference<ListResult<MMXMessage>> resultRef = new AtomicReference<>();
    final AtomicReference<Throwable> errorRef = new AtomicReference<>();
    channel.getMessages(null, null, null, null, true, new MMXChannel.OnFinishedListener<ListResult<MMXMessage>>() {
      @Override
      public void onSuccess(ListResult<MMXMessage> result) {
        resultRef.set(result);
        latch.countDown();
      }

      @Override
      public void onFailure(MMXChannel.FailureCode code, Throwable ex) {
        errorRef.set(ex);
        Log.e(TAG, "Exception caught channel.getMessages : " + code, ex);
        //fail("channel.getMessages failed due to " + ex.getMessage());
      }
    });

    try {
      latch.await(TIMEOUT_IN_SECOND, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      fail("channel.getMessages timeout");
    }

    if(null != errorRef.get()) {
      fail("channel.getMessages failed due to " + errorRef.get().getMessage());
    } else {
      if(0 != expectedCount) {
        assertThat(resultRef.get()).isNotNull();
        ListResult<MMXMessage> result = resultRef.get();
        assertThat(result.totalCount).isEqualTo(expectedCount);
      } else {
        assertThat(resultRef.get() == null || resultRef.get().totalCount == 0).isTrue();
      }
    }
  }

  private void helpGetPrivateChannel(String name, int expectedMsgs) {
    final ExecMonitor<MMXChannel, FailureCode> getRes = new ExecMonitor<MMXChannel, FailureCode>();
    MMXChannel.getPrivateChannel(name, new MMXChannel.OnFinishedListener<MMXChannel>() {
      public void onSuccess(MMXChannel result) {
        getRes.invoked(result);
      }
      
      public void onFailure(MMXChannel.FailureCode code, Throwable ex) {
        getRes.failed(code);
      }
    });
    assertEquals(ExecMonitor.Status.INVOKED, getRes.waitFor(TIMEOUT_IN_MILISEC));
    MMXChannel priChannel = getRes.getReturnValue();
    assertEquals(expectedMsgs, priChannel.getNumberOfMessages().intValue());
    assertNotNull(priChannel.getName());
    assertNotNull(priChannel.getSummary());
    assertNotNull(priChannel.getOwnerId());
    assertFalse(priChannel.isPublic());
    assertTrue(priChannel.isSubscribed());
  }
  
  private void helpGetPublicChannel(String name, int expectedMsgs) {
    final ExecMonitor<MMXChannel, FailureCode> getRes = new ExecMonitor<MMXChannel, FailureCode>();
    MMXChannel.getPublicChannel(name, new MMXChannel.OnFinishedListener<MMXChannel>() {
      public void onSuccess(MMXChannel result) {
        getRes.invoked(result);
      }
      
      public void onFailure(MMXChannel.FailureCode code, Throwable ex) {
        getRes.failed(code);
      }
    });
    assertEquals(ExecMonitor.Status.INVOKED, getRes.waitFor(TIMEOUT_IN_MILISEC));
    MMXChannel pubChannel = getRes.getReturnValue();
    assertEquals(expectedMsgs, pubChannel.getNumberOfMessages().intValue());
    assertNotNull(pubChannel.getName());
    assertNotNull(pubChannel.getSummary());
    assertNotNull(pubChannel.getOwnerId());
    assertTrue(pubChannel.isPublic());
    assertTrue(pubChannel.isSubscribed());
  }

  private void helpLogin(String userName) {
    helpLogin(userName, userName, userName, null, true);
  }
  private void helpLogin(String userName, String displayName, String password,
        String suffix, boolean regUser) {
    if(null != suffix) {
      userName = userName + suffix;
      displayName = displayName + suffix;
    }
    if(null == password) {
      password = userName;
    }
    if (regUser) {
      helpRegisterUser(userName, displayName, password.getBytes(), null, null, true);
    }

    //login with credentials
    loginMax(userName, password);

    assertTrue(MMX.getMMXClient().isConnected());
    MMX.start();
  }

  private List<User> helpGetSubscribers(final MMXChannel channel) {
    final CountDownLatch getSubLatch = new CountDownLatch(1);
    final List<User> subscribers = new ArrayList<>();
    final AtomicReference<Throwable> errorRef = new AtomicReference<>();
    channel.getAllSubscribers(10, 0, new MMXChannel.OnFinishedListener<ListResult<User>>() {
      @Override public void onSuccess(ListResult<User> result) {
        subscribers.addAll(result.items);
        //assertThat(result.totalCount).isEqualTo(userIds.size());
        //for(User u : result.items) {
        //  assertThat(userIds.contains(u.getUserIdentifier())).isTrue();
        //}
        getSubLatch.countDown();
      }

      @Override public void onFailure(FailureCode code, Throwable throwable) {
        errorRef.set(throwable);
        getSubLatch.countDown();
        //fail("Failed to get subscriber due to: " + throwable.getMessage());
        //getSubLatch.countDown();
      }
    });
    try {
      getSubLatch.await(TIMEOUT_IN_SECOND, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      fail("Failed to get subscriber : timeout");
    }

    assertThat(errorRef.get()).isNull();

    return subscribers;
  }

  private void assertSubscribers(List<User> subscribersFound, Set<User> userIdsExpected) {
    printCollection(userIdsExpected, "exected subscribers in assertSubscribers");
    printCollection(subscribersFound, "actual subscribers in assertSubscribers");
    //assertEquals(userIdsExpected.size(), subscribersFound.size());
    assertThat(subscribersFound).hasSize(userIdsExpected.size()).containsAll(userIdsExpected);
    //for(User u : subscribersFound) {
    //  assertThat(userIdsExpected.contains(u.getUserIdentifier())).isTrue();
    //}
  }

  private void helpAddSubscriber(MMXChannel channel, Set<User> users) {
    final CountDownLatch latch = new CountDownLatch(1);
    final AtomicReference<Throwable> errorRef = new AtomicReference<>();
    channel.addSubscribers(users, new MMXChannel.OnFinishedListener<List<String>>() {
      @Override public void onSuccess(List<String> result) {
        assertThat(result).isEmpty();
        latch.countDown();
      }

      @Override public void onFailure(FailureCode code, Throwable throwable) {
        errorRef.set(throwable);
        //fail("Failed to add subscriber due to: " + throwable.getMessage());
        latch.countDown();
      }
    });
    try {
      latch.await(TIMEOUT_IN_SECOND, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      fail("Failed to add subscriber : timeout");
    }

    assertThat(errorRef.get()).isNull();
  }

  private void helpRemoveSubscriber(MMXChannel channel, Set<User> users) {
    final CountDownLatch latch = new CountDownLatch(1);
    final AtomicReference<Throwable> errorRef = new AtomicReference<>();
    channel.removeSubscribers(users, new MMXChannel.OnFinishedListener<List<String>>() {
      @Override public void onSuccess(List<String> result) {
        assertThat(result).isEmpty();
        latch.countDown();
      }

      @Override public void onFailure(FailureCode code, Throwable throwable) {
        errorRef.set(throwable);
        latch.countDown();
      }
    });
    try {
      latch.await(TIMEOUT_IN_SECOND, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      fail("Failed to remove subscriber : timeout");
    }

    assertThat(errorRef.get()).isNull();
  }

  private List<MMXChannel> helpFindChannelBySubscribers(Set<User> users) {
    final List<MMXChannel> channels = new ArrayList<>();

    final CountDownLatch latch = new CountDownLatch(1);
    final AtomicReference<Throwable> errorRef = new AtomicReference<>();
    MMXChannel.findChannelsBySubscribers(users, ChannelMatchType.EXACT_MATCH, new MMXChannel.OnFinishedListener<ListResult<MMXChannel>>() {
      @Override public void onSuccess(ListResult<MMXChannel> result) {
        channels.addAll(result.items);
        latch.countDown();
      }

      @Override public void onFailure(FailureCode code, Throwable throwable) {
        errorRef.set(throwable);
        latch.countDown();
      }
    });
    try {
      latch.await(TIMEOUT_IN_SECOND, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      fail("Failed to findChannelsBySubscribers : timeout");
    }

    assertThat(errorRef.get()).isNull();

    return channels;
  }

  private List<MMXChannel> helpGetAllSubscriptions(int expectedCount) {
    try {
      Thread.sleep(SLEEP_IN_MILISEC);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    final CountDownLatch latch = new CountDownLatch(1);
    final AtomicReference<List<MMXChannel>> resultRef = new AtomicReference<>();
    final AtomicReference<Throwable> errorRef = new AtomicReference<>();
    MMXChannel.getAllSubscriptions(new MMXChannel.OnFinishedListener<List<MMXChannel>>() {
      public void onSuccess(List<MMXChannel> result) {
        resultRef.set(result);
        latch.countDown();
      }

      public void onFailure(MMXChannel.FailureCode code, Throwable ex) {
        Log.e(TAG, "Exception caught: " + code, ex);
        errorRef.set(ex);
        latch.countDown();
      }
    });
    try {
      latch.await(TIMEOUT_IN_SECOND, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      fail("MMXChannel.getAllSubscriptions : timeout");
    }

    if(null != errorRef.get()) {
      fail("MMXChannel.getAllSubscriptions failed due to " + errorRef.get().getMessage());
    } else {
      if(0 != expectedCount) {
        assertThat(resultRef.get()).isNotNull();
        List<MMXChannel> result = resultRef.get();
        assertThat(result.size()).isEqualTo(expectedCount);
        return result;
      } else {
        assertThat(resultRef.get() == null || resultRef.get().isEmpty()).isTrue();
      }
    }

    return null;
  }

  private void printCollection(Collection<?> set, String description) {
    Log.d(TAG, "------------------print collection " + description + "(" + set.size() + ")------------------");
    for(Object o : set) {
      Log.d(TAG, o.toString());
    }
  }

  private static Set<String> userSetToIds(Set<User> users) {
    if(null == users || users.isEmpty()) {
      return null;
    }

    Set<String> userIds = new HashSet<>();
    for(User u : users) {
      userIds.add(u.getUserIdentifier());
    }

    return userIds;
  }
}
