package com.magnet.mmx.client.api;

import android.util.Log;

import com.magnet.mmx.client.common.MMXid;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

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
            .build();
    helpCreate(channel);
    helpFind(channelName, 1);
    helpSubscribe(channel);
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
            .setPrivate(true)
            .build();
    helpCreate(channel);
    helpFind(channelName, 0); // 0 because private channels should not show up on search
    helpSubscribe(channel);
    helpPublish(channel);
    helpChannelSummary(channelName, 0, 0); // 0 and 0 because this method will not be able to find private channels
    helpUnsubscribe(channel);
    helpDelete(channel);
  }

  //**************
  //HELPER METHODS
  //**************
  private void helpCreate(MMXChannel channel) {
    final AtomicBoolean createResult = new AtomicBoolean(false);
    channel.create(new MMX.OnFinishedListener<MMXChannel>() {
      public void onSuccess(MMXChannel result) {
        createResult.set(true);
        synchronized (createResult) {
          createResult.notify();
        }
      }

      public void onFailure(MMX.FailureCode code, Throwable ex) {
        Log.e(TAG, "Exception caught: " + code, ex);
        createResult.set(false);
        synchronized (createResult) {
          createResult.notify();
        }
      }
    });
    synchronized (createResult) {
      try {
        createResult.wait(10000);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
    assertTrue(createResult.get());
  }

  private void helpFind(String channelName, int expectedCount) {
    //find
    final AtomicInteger findResult = new AtomicInteger(0);
    MMXChannel.findByName(channelName, 10, new MMX.OnFinishedListener<MMXChannel.FindResult>() {
      public void onSuccess(MMXChannel.FindResult result) {
        findResult.set(result.totalCount);
        synchronized (findResult) {
          findResult.notify();
        }
      }

      @Override
      public void onFailure(MMX.FailureCode code, Throwable ex) {
        Log.e(TAG, "Exception caught: " + code, ex);
        synchronized (findResult) {
          findResult.notify();
        }
      }
    });
    synchronized (findResult) {
      try {
        findResult.wait(10000);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
    assertEquals(expectedCount, findResult.intValue());
  }

  private void helpSubscribe(MMXChannel channel) {
    //subscribe
    final AtomicBoolean subResult = new AtomicBoolean(false);
    channel.subscribe(new MMX.OnFinishedListener<String>() {
      public void onSuccess(String result) {
        subResult.set(true);
        synchronized (subResult) {
          subResult.notify();
        }
      }

      public void onFailure(MMX.FailureCode code, Throwable ex) {
        Log.e(TAG, "Exception caught: " + code, ex);
        synchronized (subResult) {
          subResult.notify();
        }

      }
    });
    synchronized (subResult) {
      try {
        subResult.wait(10000);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
    assertTrue(subResult.get());

    //FIXME:  Add MMXChannel.getSubscribers()
  }

  private void helpPublish(MMXChannel channel) {
    //setup message listener to receive published message
    final StringBuffer barBuffer = new StringBuffer();
    final MMX.EventListener messageListener = new MMX.EventListener() {
      @Override
      public boolean onMessageReceived(MMXMessage message) {
        String bar = message.getContent().get("foo");
        //FIXME:  Check the sender name/displayname
        if (bar != null) {
          barBuffer.append(bar);
        }
        synchronized (barBuffer) {
          barBuffer.notify();
        }
        return false;
      }

      @Override
      public boolean onMessageAcknowledgementReceived(MMXid from, String messageId) {
        return false;
      }
    };
    MMX.registerListener(messageListener);

    //publish
    final StringBuffer pubId = new StringBuffer();
    HashMap<String, String> content = new HashMap<String, String>();
    content.put("foo", "bar");
    String id = channel.publish(content, new MMX.OnFinishedListener<String>() {
      public void onSuccess(String result) {
        pubId.append(result);
        synchronized (pubId) {
          pubId.notify();
        }
      }

      public void onFailure(MMX.FailureCode code, Throwable ex) {
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
    MMX.unregisterListener(messageListener);
  }

  private void helpChannelSummary(String channelName, int expectedChannelCount, int expectedItemCount) {
    //get topic again
    final AtomicInteger itemCount = new AtomicInteger(0);
    final AtomicInteger channelCount = new AtomicInteger(0);
    MMXChannel.findByName(channelName, 100, new MMX.OnFinishedListener<MMXChannel.FindResult>() {
      @Override
      public void onSuccess(MMXChannel.FindResult result) {
        channelCount.set(result.totalCount);
        if (result.channels.size() > 0) {
          itemCount.set(result.channels.get(0).getNumberOfMessages());
        }
        synchronized (channelCount) {
          channelCount.notify();
        }
      }

      @Override
      public void onFailure(MMX.FailureCode code, Throwable ex) {
        Log.e(TAG, "Exception caught: " + code, ex);
        synchronized (channelCount) {
          channelCount.notify();
        }
      }
    });
    synchronized (channelCount) {
      try {
        channelCount.wait(10000);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
    assertEquals(expectedChannelCount, channelCount.intValue());
    assertEquals(expectedItemCount, itemCount.intValue());
  }

  private void helpUnsubscribe(MMXChannel channel) {
    //unsubscribe
    final AtomicBoolean unsubResult = new AtomicBoolean(false);
    channel.unsubscribe(new MMX.OnFinishedListener<Boolean>() {
      @Override
      public void onSuccess(Boolean result) {
        unsubResult.set(result);
        synchronized (unsubResult) {
          unsubResult.notify();
        }
      }

      @Override
      public void onFailure(MMX.FailureCode code, Throwable ex) {
        Log.e(TAG, "Exception caught: " + code, ex);
        synchronized (unsubResult) {
          unsubResult.notify();
        }
      }
    });
    synchronized (unsubResult) {
      try {
        unsubResult.wait(10000);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
    assertTrue(unsubResult.get());
  }

  private void helpDelete(MMXChannel channel) {
    //delete
    final AtomicBoolean deleteResult = new AtomicBoolean(false);
    channel.delete(new MMX.OnFinishedListener<Void>() {
      @Override
      public void onSuccess(Void result) {
        deleteResult.set(true);
        synchronized (deleteResult) {
          deleteResult.notify();
        }
      }

      @Override
      public void onFailure(MMX.FailureCode code, Throwable ex) {
        Log.e(TAG, "Exception caught: " + code, ex);
        synchronized (deleteResult) {
          deleteResult.notify();
        }
      }
    });
    synchronized (deleteResult) {
      try {
        deleteResult.wait(10000);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
    assertTrue(deleteResult.get());
  }

  private void helpFetch(MMXChannel channel, int expectedCount) {
    //test basic fetch
    final AtomicInteger fetchCount = new AtomicInteger(0);
    channel.getItems(null, null, 100, true, new MMX.OnFinishedListener<List<MMXMessage>>() {
      @Override
      public void onSuccess(List<MMXMessage> result) {
        fetchCount.set(result.size());
        synchronized (fetchCount) {
          fetchCount.notify();
        }
      }

      @Override
      public void onFailure(MMX.FailureCode code, Throwable ex) {
        Log.e(TAG, "Exception caught: " + code, ex);
        synchronized (fetchCount) {
          fetchCount.notify();
        }
      }
    });
    synchronized (fetchCount) {
      try {
        fetchCount.wait(10000);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
    assertEquals(expectedCount, fetchCount.get());
  }
}
