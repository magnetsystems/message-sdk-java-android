package com.magnet.mmx.client.api;

import android.util.Log;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import com.magnet.mmx.client.api.MMXChannel.FailureCode;

public class MMXChannelTest extends MMXInstrumentationTestCase {
  private static final String TAG = MMXChannelTest.class.getSimpleName();

  public void postSetUp() {
    MMX.OnFinishedListener<Void> loginLogoutListener = getLoginLogoutListener();
    String suffix = String.valueOf(System.currentTimeMillis());
    String username = USERNAME_PREFIX + suffix;
    String displayName = DISPLAY_NAME_PREFIX + suffix;
    registerUser(username, displayName, PASSWORD);

    //login with credentials
    MMX.login(username, PASSWORD, loginLogoutListener);
    synchronized (loginLogoutListener) {
      try {
        loginLogoutListener.wait(10000);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
    assertTrue(MMX.getMMXClient().isConnected());
    MMX.enableIncomingMessages(true);
  }

  public void tearDown() {
    MMX.OnFinishedListener<Void> loginLogoutListener = getLoginLogoutListener();
    MMX.logout(loginLogoutListener);
    synchronized (loginLogoutListener) {
      try {
        loginLogoutListener.wait(10000);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
    assertFalse(MMX.getMMXClient().isConnected());
  }

  public void testPublicChannel() {
    String channelName = "channel" + System.currentTimeMillis();
    String channelSummary = channelName + " Summary";
    MMXChannel channel = new MMXChannel.Builder()
            .name(channelName)
            .summary(channelSummary)
            .setPublic(true)
            .build();
    helpCreate(channel);
    helpCreateError(channel, MMXChannel.FailureCode.CHANNEL_EXISTS);
    helpFind(channelName, 1);
    helpSubscribe(channel, 1);
    helpPublish(channel);
    helpChannelSummary(channelName, 1, 1);
    helpUnsubscribe(channel);
    helpDelete(channel);
  }

  public void testPrivateChannel() {
    String channelName = "private-channel" + System.currentTimeMillis();
    String channelSummary = channelName + " Summary";
    MMXChannel channel = new MMXChannel.Builder()
            .name(channelName)
            .summary(channelSummary)
            .build();
    helpCreate(channel);
    helpCreateError(channel, MMXChannel.FailureCode.CHANNEL_EXISTS);
    helpFind(channelName, 0); // 0 because private channels should not show up on search
    helpSubscribe(channel, 1);
    helpPublish(channel);
    helpChannelSummary(channelName, 0, 0); // 0 and 0 because this method will not be able to find private channels
    helpUnsubscribe(channel);
    helpDelete(channel);

  }

  public void testPrivateChannelInvite() {
    helpTestChannelInvite(false);
  }

  public void testPublicChannelInvite() {
    helpTestChannelInvite(true);
  }

  public void helpTestChannelInvite(boolean isPublic) {
    String channelName = (isPublic ? "public-channel" : "private-channel") + System.currentTimeMillis();
    String channelSummary = channelName + " Summary";
    MMXChannel channel = new MMXChannel.Builder()
            .name(channelName)
            .summary(channelSummary)
            .setPublic(isPublic)
            .build();
    MMXChannel channelFromCallback = helpCreate(channel);

    final AtomicBoolean inviteResponseValue = new AtomicBoolean(false);
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
        inviteResponseValue.set(inviteResponse.isAccepted());
        inviteResponseText.append(inviteResponse.getResponseText());
        synchronized (inviteResponseValue) {
          inviteResponseValue.notify();
        }
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
    synchronized (inviteResponseValue) {
      try {
        inviteResponseValue.wait(10000);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
    assertEquals("foobar", inviteTextBuffer.toString());
    assertTrue(inviteResponseValue.get());
    assertEquals("foobar response", inviteResponseText.toString());

    //test invite from the callback channel
    final AtomicBoolean inviteFromCallbackSent = new AtomicBoolean(false);
    channel.inviteUser(MMX.getCurrentUser(), "foobar", new MMXChannel.OnFinishedListener<MMXChannel.MMXInvite>() {
      @Override
      public void onSuccess(MMXChannel.MMXInvite result) {
        inviteFromCallbackSent.set(true);
        synchronized (inviteSent) {
          inviteFromCallbackSent.notify();
        }
      }

      @Override
      public void onFailure(MMXChannel.FailureCode code, Throwable ex) {
        Log.e(TAG, "Exception caught: " + code, ex);
        synchronized (inviteFromCallbackSent) {
          inviteFromCallbackSent.notify();
        }
      }
    });
    synchronized (inviteFromCallbackSent) {
      try {
        inviteFromCallbackSent.wait(10000);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
    assertTrue(inviteFromCallbackSent.get());

    helpDelete(channel);
  }

  public void testGetAllSubscriptions() {
    long timestamp = System.currentTimeMillis();
    String privateChannelName = "private-channel" + timestamp;
    String privateChannelSummary = privateChannelName + " Summary";
    MMXChannel privateChannel = new MMXChannel.Builder()
            .name(privateChannelName)
            .summary(privateChannelSummary)
            .build();
    helpCreate(privateChannel);

    String publicChannelName = "channel" + timestamp;
    String publicChannelSummary = publicChannelName + " Summary";
    MMXChannel publicChannel = new MMXChannel.Builder()
            .name(publicChannelName)
            .summary(publicChannelSummary)
            .setPublic(true)
            .build();
    helpCreate(publicChannel);

    //should have autosubscribed since we created them
    final AtomicInteger subCount = new AtomicInteger(0);
    MMXChannel.getAllSubscriptions(new MMXChannel.OnFinishedListener<List<MMXChannel>>() {
      public void onSuccess(List<MMXChannel> result) {
        subCount.set(result.size());
        synchronized (subCount) {
          subCount.notify();
        }
      }

      public void onFailure(MMXChannel.FailureCode code, Throwable ex) {
        Log.e(TAG, "Exception caught: " + code, ex);
        synchronized (subCount) {
          subCount.notify();
        }
      }
    });
    synchronized (subCount) {
      try {
        subCount.wait();
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
    assertEquals(2, subCount.intValue());

    helpDelete(publicChannel);
    helpDelete(privateChannel);
  }

  public void testErrorHandling() {
    String existChannelName = "exist-channel" + System.currentTimeMillis();
    MMXChannel existChannel = new MMXChannel.Builder()
            .name(existChannelName)
            .setPublic(true)
            .build();
    helpCreate(existChannel);
    helpCreateError(existChannel, MMXChannel.FailureCode.CHANNEL_EXISTS);
    helpDelete(existChannel);
    
    String noSuchChannelName = "no-such-channel";
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
  
  //**************
  //HELPER METHODS
  //**************
  private MMXChannel helpCreate(MMXChannel channel) {
    final ExecMonitor<MMXChannel, Void> createResult = new ExecMonitor<MMXChannel, Void>();
    channel.create(new MMXChannel.OnFinishedListener<MMXChannel>() {
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
    if (createResult.waitFor(10000) == ExecMonitor.Status.INVOKED) {
      result = createResult.getReturnValue();
      assertNotNull(result);
      assertNotNull(result.getOwnerUsername());
      assertEquals(channel.getSummary(), result.getSummary());
      assertEquals(channel.getName(), result.getName());
    } else {
      fail("Channel creation timed out");
    }
    return result;
  }
  
  enum Status {
    INVOKED,
    FAILED,
    TIMEDOUT,
  }
  
  static class ExecMonitor<S, F> {
    private S mRtnValue;
    private F mFailedValue;
    private Status mStatus = Status.TIMEDOUT;
    
    // Only be called if waitFor() returns EXECED
    public S getReturnValue() {
      return mRtnValue;
    }
    
    // Only be called if waitFor() returns FAILED
    public F getFailedValue() {
      return mFailedValue;
    }
    
    public synchronized void invoked(S value) {
      mStatus = Status.INVOKED;
      mRtnValue = value;
      notify();

    }
    
    public synchronized void failed(F value) {
      mStatus = Status.FAILED;
      mFailedValue = value;
      notify();
    }
    
    public synchronized Status waitFor(long timeout) {
      if (mStatus == Status.TIMEDOUT) {
        // Not executed yet, wait for result.
        try {
          wait(timeout);
        } catch (InterruptedException e) {
          // Ignored
        }
      }
      return mStatus;
    }
  }
  
  private void helpCreateError(MMXChannel channel, final FailureCode expected) {
    final ExecMonitor<FailureCode, String> obj = new ExecMonitor<FailureCode, String>();
    channel.create(new MMXChannel.OnFinishedListener<MMXChannel>() {
      @Override
      public void onSuccess(MMXChannel result) {
        obj.failed("Unexpected success on creating an existing channel");
      }

      @Override
      public void onFailure(FailureCode code, Throwable throwable) {
        obj.invoked(code);
      }
    });
    if (obj.waitFor(10000) == Status.FAILED)
      fail(obj.getFailedValue());
    else
      assertEquals(expected, obj.getReturnValue());
  }


  private void helpFind(String channelName, int expectedCount) {
    //find
    final ExecMonitor<Integer, FailureCode> findResult = new ExecMonitor<Integer, FailureCode>();
    MMXChannel.findByName(channelName, 10, new MMXChannel.OnFinishedListener<ListResult<MMXChannel>>() {
      public void onSuccess(ListResult<MMXChannel> result) {
        findResult.invoked(result.totalCount);
      }

      @Override
      public void onFailure(MMXChannel.FailureCode code, Throwable ex) {
        Log.e(TAG, "Exception caught: " + code, ex);
        findResult.failed(code);
      }
    });
    Status status = findResult.waitFor(10000);
    if (status == Status.INVOKED)
      assertEquals(expectedCount, findResult.getReturnValue().intValue());
    else if (status == Status.FAILED)
      fail("Find channel failed: "+findResult.getFailedValue());
    else
      fail("Find channel timed out");
  }
  
  private void helpFindError(String channelName, final int expected) {
    final ExecMonitor<ListResult<MMXChannel>, String> obj = new ExecMonitor<ListResult<MMXChannel>, String>();
    MMXChannel.findByName(channelName, 10, new MMXChannel.OnFinishedListener<ListResult<MMXChannel>>() {
      @Override
      public void onSuccess(ListResult<MMXChannel> result) {
        obj.invoked(result);
      }

      @Override
      public void onFailure(FailureCode code, Throwable ex) {
        obj.failed("Unexpected failure on finding a non-existing channel");
      }
    });
    Status status = obj.waitFor(10000);
    if (status == Status.FAILED)
      fail(obj.getFailedValue());
    else if (status == Status.INVOKED)
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
    Status status = subResult.waitFor(10000);
    if (status == Status.INVOKED)
      assertTrue(subResult.getReturnValue());
    else if (status == Status.FAILED)
      fail("Channel subscription failed: "+subResult.getFailedValue());
    else
      fail("Channel subscription timed out");

    final ExecMonitor<Integer, FailureCode> getSubsResult = new ExecMonitor<Integer, FailureCode>();
    channel.getAllSubscribers(100, new MMXChannel.OnFinishedListener<ListResult<MMXUser>>() {
      public void onSuccess(ListResult<MMXUser> result) {
        getSubsResult.invoked(result.totalCount);
      }

      public void onFailure(MMXChannel.FailureCode code, Throwable ex) {
        Log.e(TAG, "Exception caught: " + code, ex);
        getSubsResult.failed(code);
      }
    });
    status = getSubsResult.waitFor(10000);
    if (status == Status.INVOKED)
      assertEquals(expectedSubscriberCount, getSubsResult.getReturnValue().intValue());
    else if (status == Status.FAILED)
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
    if (obj.waitFor(10000) == Status.FAILED)
      fail(obj.getFailedValue());
    else
      assertEquals(expected, obj.getReturnValue());
  }

  private void helpPublish(MMXChannel channel) {
    //setup message listener to receive published message
    final StringBuffer barBuffer = new StringBuffer();
    final StringBuffer senderBuffer = new StringBuffer();
    final MMX.EventListener messageListener = new MMX.EventListener() {
      @Override
      public boolean onMessageReceived(MMXMessage message) {
        String bar = message.getContent().get("foo");
        //FIXME:  Check the sender name/displayname
        MMXUser sender = message.getSender();
        if (sender != null) {
          senderBuffer.append(sender.getDisplayName());
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
      public boolean onMessageAcknowledgementReceived(MMXUser from, String messageId) {
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
    assertEquals(MMX.getCurrentUser().getDisplayName(), senderBuffer.toString());
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
    if (obj.waitFor(10000) == Status.FAILED)
      fail(obj.getFailedValue());
    else
      assertEquals(expected, obj.getReturnValue());
  }

  private void helpChannelSummary(String channelName, int expectedChannelCount, int expectedItemCount) {
    //get topic again
    final AtomicInteger itemCount = new AtomicInteger(0);
    final ExecMonitor<Integer, FailureCode> channelCount = new ExecMonitor<Integer, FailureCode>();
    MMXChannel.findByName(channelName, 100, new MMXChannel.OnFinishedListener<ListResult<MMXChannel>>() {
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
    Status status = channelCount.waitFor(10000);
    if (status == Status.INVOKED) {
      assertEquals(expectedChannelCount, channelCount.getReturnValue().intValue());
      assertEquals(expectedItemCount, itemCount.intValue());
    } else if (status == Status.FAILED)
      fail("Channel summary failed: "+channelCount.getFailedValue());
    else
      fail("Channel summary timed out");
  }
  
  private void helpChannelSummaryError(String channelName, int expected) {
    final ExecMonitor<ListResult<MMXChannel>, String> obj = new ExecMonitor<ListResult<MMXChannel>, String>();
    MMXChannel.findByName(channelName, 100, new MMXChannel.OnFinishedListener<ListResult<MMXChannel>>() {
      @Override
      public void onSuccess(ListResult<MMXChannel> result) {
        obj.invoked(result);
      }

      @Override
      public void onFailure(FailureCode code, Throwable throwable) {
        obj.failed("Unexpected failure on channel summary of a non-existing channel");
      }
    });
    Status status = obj.waitFor(10000);
    if (status == Status.FAILED)
      fail(obj.getFailedValue());
    else if (status == Status.INVOKED)
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
    Status status = unsubResult.waitFor(10000);
    if (status == Status.INVOKED)
      assertTrue(unsubResult.getReturnValue());
    else if (status == Status.FAILED)
      fail("Channel unsubscription failed: "+unsubResult.getFailedValue());
    else
      fail("Channel unsubscription timed out");
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
    if (obj.waitFor(10000) == Status.FAILED)
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
    if (deleteResult.waitFor(10000) == Status.INVOKED)
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
    if (obj.waitFor(10000) == Status.FAILED)
      fail(obj.getFailedValue());
    else
      assertEquals(expected, obj.getReturnValue());
  }

  private void helpFetch(MMXChannel channel, int expectedCount) {
    //test basic fetch
    final ExecMonitor<Integer, FailureCode> fetchCount = new ExecMonitor<Integer, FailureCode>();
    channel.getItems(null, null, 100, true, new MMXChannel.OnFinishedListener<ListResult<MMXMessage>>() {
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
    Status status = fetchCount.waitFor(10000);
    if (status == Status.INVOKED)
      assertEquals(expectedCount, fetchCount.getReturnValue().intValue());
    else if (status == Status.FAILED)
      fail("Fetch from channel failed: "+fetchCount.getFailedValue());
    else
      fail("Fetch from channel timed out");
  }
}
