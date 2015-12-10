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
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import android.util.Log;

import com.magnet.max.android.ApiCallback;
import com.magnet.max.android.User;
import com.magnet.mmx.client.api.MMXChannel.FailureCode;
import java.util.concurrent.atomic.AtomicReference;

public class MMXChannelTest extends MMXInstrumentationTestCase {
  private static final String TAG = MMXChannelTest.class.getSimpleName();

  public void postSetUp() {
    helpLogin(MMX_TEST_USER_1);
  }

  public void tearDown() {
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
    helpLogout();

    String suffix = String.valueOf(System.currentTimeMillis());

    User user2 = helpRegisterUser(MMX_TEST_USER_2 + suffix, MMX_TEST_USER_2, MMX_TEST_USER_2.getBytes(), null, null, false);
    User user3 = helpRegisterUser(MMX_TEST_USER_3 + suffix, MMX_TEST_USER_3, MMX_TEST_USER_3.getBytes(), null, null, false);
    User user4 = helpRegisterUser(MMX_TEST_USER_4 + suffix, MMX_TEST_USER_4, MMX_TEST_USER_4.getBytes(), null, null, false);

    helpLogin(MMX_TEST_USER_1);

    String channelName = "Chat_channel_" + System.currentTimeMillis();
    String channelSummary = channelName + " Summary";
    final MMXChannel channel = helpCreate(channelName, channelSummary, false, new HashSet<String>(
        Arrays.asList(user2.getUserIdentifier(), user3.getUserIdentifier(), user4.getUserIdentifier())));

    final CountDownLatch getSubLatch = new CountDownLatch(1);
    final AtomicReference<Boolean> isSuccess = new AtomicReference<Boolean>(true);
    final AtomicReference<String> errorMesage = new AtomicReference<>();
    channel.getAllSubscribers(10, 0, new MMXChannel.OnFinishedListener<ListResult<User>>() {
      @Override public void onSuccess(ListResult<User> result) {
        //TODO wait for server to fix auto subsribe for owner
        //assertEquals(4, result.items.size());
        //assertTrue(result.items.size() >= 3);
        if(result.items.size() != 4) {
          isSuccess.set(false);
          errorMesage.set("expecting 4 subscribers but is " + result.items.size());
        }
        getSubLatch.countDown();
      }

      @Override public void onFailure(FailureCode code, Throwable throwable) {
        Log.d(TAG, "Failed to get subscriber : ", throwable);
        //fail("Failed to get subscriber : ");
        isSuccess.set(false);
        errorMesage.set("Failed to get subscriber");
        getSubLatch.countDown();
      }
    });
    try {
      getSubLatch.await(10, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      isSuccess.set(false);
      errorMesage.set("Failed to get subscriber : timeout");
    }

    helpPublish(channel);
    helpFetch(channel, 1);

    if(!isSuccess.get()) {
      helpDelete(channel);
      fail(errorMesage.get());
    } else {
      helpDelete(channel);
    }

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

    final ExecMonitor<Boolean, Boolean> inviteResponseValue = new ExecMonitor<Boolean, Boolean>();
    final StringBuffer inviteResponseText = new StringBuffer();
    final StringBuffer inviteTextBuffer = new StringBuffer();
    MMX.EventListener inviteListener = new MMX.EventListener() {
      @Override
      public boolean onMessageReceived(MMXMessage message) {
        return false;
      }

      public boolean onInviteReceived(MMXChannel.MMXInvite invite) {
        inviteTextBuffer.append(invite.getInviteInfo().getComment());
        invite.accept("foobar response", null);
        synchronized (inviteTextBuffer) {
          inviteTextBuffer.notify();
        }
        return true;
      }

      public boolean onInviteResponseReceived(MMXChannel.MMXInviteResponse inviteResponse) {
        inviteResponseText.append(inviteResponse.getResponseText());
        inviteResponseValue.invoked(inviteResponse.isAccepted());
        return true;
      }
    };
    MMX.registerListener(inviteListener);
    final AtomicBoolean inviteSent = new AtomicBoolean(false);
    channel.inviteUser(MMX.getCurrentUser(), "foobar", new MMXChannel.OnFinishedListener<MMXChannel.MMXInvite>() {
      @Override
      public void onSuccess(MMXChannel.MMXInvite result) {
        inviteSent.set(true);
        synchronized (inviteSent) {
          inviteSent.notify();
        }
      }

      @Override
      public void onFailure(MMXChannel.FailureCode code, Throwable ex) {
        Log.e(TAG, "Exception caught: " + code, ex);
        synchronized (inviteSent) {
          inviteSent.notify();
        }
      }
    });
    ExecMonitor.Status status = inviteResponseValue.waitFor(TIMEOUT);
    assertEquals(ExecMonitor.Status.INVOKED, status);
    assertEquals("foobar", inviteTextBuffer.toString());
    assertTrue(inviteResponseValue.getReturnValue());
    assertEquals("foobar response", inviteResponseText.toString());

    //test invite from the callback channel
    final ExecMonitor<Boolean, Boolean> inviteFromCallbackSent = new ExecMonitor<Boolean, Boolean>();
    channel.inviteUser(MMX.getCurrentUser(), "foobar", new MMXChannel.OnFinishedListener<MMXChannel.MMXInvite>() {
      @Override
      public void onSuccess(MMXChannel.MMXInvite result) {
        inviteFromCallbackSent.invoked(true);
      }

      @Override
      public void onFailure(MMXChannel.FailureCode code, Throwable ex) {
        Log.e(TAG, "Exception caught: " + code, ex);
        inviteFromCallbackSent.failed(true);
      }
    });
    status = inviteFromCallbackSent.waitFor(TIMEOUT);
    assertEquals(ExecMonitor.Status.INVOKED, status);
    assertTrue(inviteFromCallbackSent.getReturnValue());
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

    //should have autosubscribed since we created them
    final ExecMonitor<Integer, FailureCode> subCount = new ExecMonitor<Integer, FailureCode>();
    MMXChannel.getAllSubscriptions(new MMXChannel.OnFinishedListener<List<MMXChannel>>() {
      public void onSuccess(List<MMXChannel> result) {
        subCount.invoked(result.size());
      }

      public void onFailure(MMXChannel.FailureCode code, Throwable ex) {
        Log.e(TAG, "Exception caught: " + code, ex);
        subCount.failed(code);
      }
    });
    ExecMonitor.Status status = subCount.waitFor(TIMEOUT);
    if (status == ExecMonitor.Status.INVOKED) {
      assertEquals(2, subCount.getReturnValue().intValue());
    } else if (status == ExecMonitor.Status.FAILED) {
      fail("getAllSubscriptions() failed: "+subCount.getFailedValue());
    } else {
      fail("getAllSubscriptions() timed out");
    }

    helpDelete(publicChannel);
    helpDelete(privateChannel);

    helpLogout();
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
    final ExecMonitor<List<MMXChannel>, FailureCode> channels = new ExecMonitor<List<MMXChannel>, FailureCode>();
    MMXChannel.getAllSubscriptions(new MMXChannel.OnFinishedListener<List<MMXChannel>>() {
      public void onSuccess(List<MMXChannel> result) {
        channels.invoked(result);
      }

      public void onFailure(MMXChannel.FailureCode code, Throwable ex) {
        Log.e(TAG, "Exception caught: " + code, ex);
        channels.failed(code);
      }
    });
    ExecMonitor.Status status = channels.waitFor(TIMEOUT);
    if (status == ExecMonitor.Status.FAILED) {
      fail("getAllSubscriptions failed: "+channels.getFailedValue());
    } else if (status == ExecMonitor.Status.WAITING) {
      fail("getAllSubscriptions timed out");
    } else {
      List<MMXChannel> subChannels = channels.getReturnValue();
      assertNotNull(subChannels);
      assertEquals(1, subChannels.size());
      assertEquals(user1.getUserIdentifier(), subChannels.get(0).getOwnerId());
    }
    helpLogout();
    
    // Login as user1 again and delete the channel.
    helpLogin(MMX_TEST_USER_2);
    helpDelete(channel);
    helpLogout();
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
    ExecMonitor.Status status = channelsRes.waitFor(TIMEOUT);
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

    helpLogout();
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
    ExecMonitor.Status status = findNullResult.waitFor(TIMEOUT);
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
    status = findEmptyResult.waitFor(TIMEOUT);
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
    if (createResult.waitFor(TIMEOUT) == ExecMonitor.Status.INVOKED) {
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


  private MMXChannel helpCreate(String name, String summary, boolean isPublic, Set<String> subscribers) {
    final ExecMonitor<MMXChannel, Void> createResult = new ExecMonitor<MMXChannel, Void>();
    MMXChannel.create(name, summary, isPublic, MMXChannel.PublishPermission.ANYONE, subscribers,
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
    if (createResult.waitFor(TIMEOUT) == ExecMonitor.Status.INVOKED) {
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
    if (obj.waitFor(TIMEOUT) == ExecMonitor.Status.FAILED)
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
    ExecMonitor.Status status = findResult.waitFor(TIMEOUT);
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
    ExecMonitor.Status status = obj.waitFor(TIMEOUT);
    if (status == ExecMonitor.Status.FAILED)
      fail(obj.getFailedValue());
    else if (status == ExecMonitor.Status.INVOKED)
      assertEquals(expected, obj.getReturnValue().totalCount);
    else
      fail("Find non-existing channel timeout");
  }

  private void helpSubscribe(MMXChannel channel, int expectedSubscriberCount) {
    //subscribe
    final ExecMonitor<Boolean, FailureCode> subResult = new ExecMonitor<Boolean, FailureCode>();
    channel.subscribe(new MMXChannel.OnFinishedListener<String>() {
      public void onSuccess(String result) {
        subResult.invoked(true);
      }

      public void onFailure(MMXChannel.FailureCode code, Throwable ex) {
        Log.e(TAG, "Exception caught: " + code, ex);
        subResult.failed(code);
      }
    });
    ExecMonitor.Status status = subResult.waitFor(TIMEOUT);
    if (status == ExecMonitor.Status.INVOKED)
      assertTrue(subResult.getReturnValue());
    else if (status == ExecMonitor.Status.FAILED)
      fail("Channel subscription failed: "+subResult.getFailedValue());
    else
      fail("Channel subscription timed out");
    //make sure the flag is set
    assertTrue(channel.isSubscribed());

    final ExecMonitor<Integer, FailureCode> getSubsResult = new ExecMonitor<Integer, FailureCode>();
    channel.getAllSubscribers(0, 100, new MMXChannel.OnFinishedListener<ListResult<User>>() {
      public void onSuccess(ListResult<User> result) {
        getSubsResult.invoked(result.totalCount);
      }

      public void onFailure(MMXChannel.FailureCode code, Throwable ex) {
        Log.e(TAG, "Exception caught: " + code, ex);
        getSubsResult.failed(code);
      }
    });
    status = getSubsResult.waitFor(TIMEOUT);
    if (status == ExecMonitor.Status.INVOKED)
      assertEquals(expectedSubscriberCount, getSubsResult.getReturnValue().intValue());
    else if (status == ExecMonitor.Status.FAILED)
      fail("Get subscrivers failed: "+getSubsResult.getFailedValue());
    else
      fail("Get subscrivers timed out");
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
    if (obj.waitFor(TIMEOUT) == ExecMonitor.Status.FAILED)
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
        synchronized (barBuffer) {
          barBuffer.notify();
        }
        return false;
      }

      @Override
      public boolean onMessageAcknowledgementReceived(User from, String messageId) {
        return false;
      }
    };
    MMX.registerListener(messageListener);

    //publish
    final StringBuffer pubId = new StringBuffer();
    HashMap<String, String> content = new HashMap<String, String>();
    content.put("foo", "bar");
    String id = channel.publish(content, new MMXChannel.OnFinishedListener<String>() {
      public void onSuccess(String result) {
        pubId.append(result);
        synchronized (pubId) {
          pubId.notify();
        }
      }

      public void onFailure(MMXChannel.FailureCode code, Throwable ex) {
        Log.e(TAG, "Exception caught: " + code, ex);
        synchronized (pubId) {
          pubId.notify();
        }
      }
    });
    synchronized (barBuffer) {
      try {
        barBuffer.wait(10000);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
    assertEquals(id, pubId.toString());
    assertEquals("bar", barBuffer.toString());
    assertEquals(MMX.getCurrentUser().getFirstName(), senderBuffer.toString());
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
    if (obj.waitFor(TIMEOUT) == ExecMonitor.Status.FAILED)
      fail(obj.getFailedValue());
    else
      assertEquals(expected, obj.getReturnValue());
  }

  private void helpChannelSummary(String channelName, int expectedChannelCount, int expectedItemCount) {
    //get topic again
    final AtomicInteger itemCount = new AtomicInteger(0);
    final ExecMonitor<Integer, FailureCode> channelCount = new ExecMonitor<Integer, FailureCode>();
    MMXChannel.findPublicChannelsByName(channelName, null, null, new MMXChannel.OnFinishedListener<ListResult<MMXChannel>>() {
      @Override
      public void onSuccess(ListResult<MMXChannel> result) {
        if (result.items.size() > 0) {
          itemCount.set(result.items.get(0).getNumberOfMessages());
        }
        channelCount.invoked(result.totalCount);
      }

      @Override
      public void onFailure(MMXChannel.FailureCode code, Throwable ex) {
        Log.e(TAG, "Exception caught: " + code, ex);
        channelCount.failed(code);
      }
    });
    ExecMonitor.Status status = channelCount.waitFor(TIMEOUT);
    if (status == ExecMonitor.Status.INVOKED) {
      assertEquals(expectedChannelCount, channelCount.getReturnValue().intValue());
      assertEquals(expectedItemCount, itemCount.intValue());
    } else if (status == ExecMonitor.Status.FAILED)
      fail("Channel summary failed: " + channelCount.getFailedValue());
    else
      fail("Channel summary timed out");
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
    ExecMonitor.Status status = obj.waitFor(TIMEOUT);
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
    ExecMonitor.Status status = unsubResult.waitFor(TIMEOUT);
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
    if (obj.waitFor(TIMEOUT) == ExecMonitor.Status.FAILED)
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
    if (deleteResult.waitFor(TIMEOUT) == ExecMonitor.Status.INVOKED)
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
    if (obj.waitFor(TIMEOUT) == ExecMonitor.Status.FAILED)
      fail(obj.getFailedValue());
    else
      assertEquals(expected, obj.getReturnValue());
  }

  private void helpFetch(MMXChannel channel, int expectedCount) {
    //test basic fetch
    final ExecMonitor<Integer, FailureCode> fetchCount = new ExecMonitor<Integer, FailureCode>();
    channel.getMessages(null, null, null, null, true, new MMXChannel.OnFinishedListener<ListResult<MMXMessage>>() {
      @Override
      public void onSuccess(ListResult<MMXMessage> result) {
        fetchCount.invoked(result.totalCount);
      }

      @Override
      public void onFailure(MMXChannel.FailureCode code, Throwable ex) {
        Log.e(TAG, "Exception caught: " + code, ex);
        fetchCount.failed(code);
      }
    });
    ExecMonitor.Status status = fetchCount.waitFor(TIMEOUT);
    if (status == ExecMonitor.Status.INVOKED)
      assertEquals(expectedCount, fetchCount.getReturnValue().intValue());
    else if (status == ExecMonitor.Status.FAILED)
      fail("Fetch from channel failed: "+fetchCount.getFailedValue());
    else
      fail("Fetch from channel timed out");
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
    assertEquals(ExecMonitor.Status.INVOKED, getRes.waitFor(TIMEOUT));
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
    assertEquals(ExecMonitor.Status.INVOKED, getRes.waitFor(TIMEOUT));
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
    ApiCallback<Boolean> loginListener = getLoginListener();
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
    User.login(userName, new String(password), false, loginListener);
    synchronized (loginListener) {
      try {
        loginListener.wait(10000);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
    assertTrue(MMX.getMMXClient().isConnected());
    MMX.start();
  }

  private void helpLogout() {
    logoutMMX();
    ApiCallback<Boolean> logoutListener = getLogoutListener();
    User.logout(logoutListener);
    synchronized (logoutListener) {
      try {
        logoutListener.wait(10000);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
    //assertFalse(MMX.getMMXClient().isConnected());
  }
}
