/**
 * Copyright (c) 2012-2016 Magnet Systems. All rights reserved.
 */
package com.magnet.mmx.client.utils;

import android.util.Log;
import com.magnet.max.android.Attachment;
import com.magnet.max.android.User;
import com.magnet.mmx.client.api.ChannelDetail;
import com.magnet.mmx.client.api.ChannelDetailOptions;
import com.magnet.mmx.client.api.ChannelMatchType;
import com.magnet.mmx.client.api.ListResult;
import com.magnet.mmx.client.api.MMX;
import com.magnet.mmx.client.api.MMXChannel;
import com.magnet.mmx.client.api.MMXMessage;
import com.magnet.mmx.client.internal.channel.ChannelSummaryResponse;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static junit.framework.Assert.fail;
import static org.assertj.core.api.Assertions.assertThat;

public class ChannelHelper {
  private static final String TAG = ChannelHelper.class.getSimpleName();

  public static MMXChannel create(String name, String summary, boolean isPublic, Set<User> subscribers) {
    final ExecMonitor<MMXChannel, Void> createResult = new ExecMonitor<MMXChannel, Void>();
    if(null == subscribers) {
      MMXChannel.create(name, summary, isPublic, MMXChannel.PublishPermission.ANYONE, new MMXChannel.OnFinishedListener<MMXChannel>() {
        public void onSuccess(MMXChannel result) {
          Log.e(TAG, "helpCreate.onSuccess ");
          createResult.invoked(result);
        }

        public void onFailure(MMXChannel.FailureCode code, Throwable ex) {
          Log.e(TAG, "Exception caught: " + code, ex);
          createResult.invoked(null);
        }
      });
    } else {
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
    }
    MMXChannel result = null;
    if (createResult.waitFor(TestConstants.TIMEOUT_IN_MILISEC) == ExecMonitor.Status.INVOKED) {
      result = createResult.getReturnValue();
      assertThat(result).isNotNull();
      assertThat(result.getOwnerId()).isNotNull();
      assertThat(result.getOwnerId()).isEqualTo(MMX.getCurrentUser().getUserIdentifier());
      assertThat(result.getSummary()).isEqualTo(summary);
      assertThat(result.getName()).isEqualTo(name);
      assertThat(result.isPublic()).isEqualTo(isPublic);
      assertThat(result.getCreationDate()).isNotNull();
    } else {
      fail("Channel creation timed out");
    }
    return result;
  }


  public static MMXChannel create(String name, String summary, boolean isPublic) {
    return create(name, summary, isPublic, null);
  }

  public static void createError(MMXChannel channel, final MMXChannel.FailureCode expected) {
    final ExecMonitor<MMXChannel.FailureCode, String> obj = new ExecMonitor<MMXChannel.FailureCode, String>();
    MMXChannel.create(channel.getName(), channel.getSummary(), channel.isPublic(), channel.getPublishPermission(),
        new MMXChannel.OnFinishedListener<MMXChannel>() {
          @Override
          public void onSuccess(MMXChannel result) {
            obj.failed("Unexpected success on creating an existing channel");
          }

          @Override
          public void onFailure(MMXChannel.FailureCode code, Throwable throwable) {
            obj.invoked(code);
          }
        });
    if (obj.waitFor(TestConstants.TIMEOUT_IN_MILISEC) == ExecMonitor.Status.FAILED)
      fail(obj.getFailedValue());
    else
      assertThat(obj.getReturnValue()).isEqualTo(expected);
  }


  public static void find(String channelName, int expectedCount) {
    //find
    final ExecMonitor<Integer, MMXChannel.FailureCode> findResult = new ExecMonitor<Integer, MMXChannel.FailureCode>();
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
    ExecMonitor.Status status = findResult.waitFor(TestConstants.TIMEOUT_IN_MILISEC);
    if (status == ExecMonitor.Status.INVOKED)
      assertThat(findResult.getReturnValue().intValue()).isEqualTo(expectedCount);
    else if (status == ExecMonitor.Status.FAILED)
      fail("Find channel failed: " + findResult.getFailedValue());
    else
      fail("Find channel timed out");
  }

  public static void findError(String channelName, final int expected) {
    final ExecMonitor<ListResult<MMXChannel>, String> obj = new ExecMonitor<ListResult<MMXChannel>, String>();
    MMXChannel.findPublicChannelsByName(channelName, null, null, new MMXChannel.OnFinishedListener<ListResult<MMXChannel>>() {
      @Override
      public void onSuccess(ListResult<MMXChannel> result) {
        obj.invoked(result);
      }

      @Override
      public void onFailure(MMXChannel.FailureCode code, Throwable ex) {
        obj.failed("Unexpected failure on finding a non-existing channel");
      }
    });
    ExecMonitor.Status status = obj.waitFor(TestConstants.TIMEOUT_IN_MILISEC);
    if (status == ExecMonitor.Status.FAILED)
      fail(obj.getFailedValue());
    else if (status == ExecMonitor.Status.INVOKED)
      assertThat(obj.getReturnValue().totalCount).isEqualTo(expected);
    else
      fail("Find non-existing channel timeout");
  }

  public static void subscribe(MMXChannel channel, final int expectedSubscriberCount) {
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
      subScribeLatch.await(TestConstants.TIMEOUT_IN_SECOND, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      fail("channel.subscribe timeout");
    }
    assertThat(errorRef.get()).isNull();
    assertThat(channel.isSubscribed()).isTrue();

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
      getSubScribesLatch.await(TestConstants.TIMEOUT_IN_SECOND, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      fail("channel.getAllSubscribers timeout");
    }
    assertThat(errorRef.get()).isNull();
  }

  public static void subscribeError(MMXChannel channel, final MMXChannel.FailureCode expected) {
    final ExecMonitor<MMXChannel.FailureCode, String> obj = new ExecMonitor<MMXChannel.FailureCode, String>();
    channel.subscribe(new MMXChannel.OnFinishedListener<String>() {
      @Override
      public void onSuccess(String result) {
        obj.failed("Unexpected success on subscribing a non-existing channel");
      }
      @Override
      public void onFailure(MMXChannel.FailureCode code, Throwable throwable) {
        obj.invoked(code);
      }
    });
    if (obj.waitFor(TestConstants.TIMEOUT_IN_MILISEC) == ExecMonitor.Status.FAILED)
      fail(obj.getFailedValue());
    else
      assertThat(obj.getReturnValue()).isEqualTo(expected);
  }

  public static MMXMessage publish(MMXChannel channel) {
    return publish(channel, null);
  }

  public static MMXMessage publish(MMXChannel channel, Attachment attachment) {
    //setup message listener to receive published message
    final StringBuffer barBuffer = new StringBuffer();
    final StringBuffer senderBuffer = new StringBuffer();
    final CountDownLatch receiveMessageLatch = new CountDownLatch(1);
    final MMX.EventListener messageListener = new MMX.EventListener() {
      @Override
      public boolean onMessageReceived(MMXMessage message) {
        Map<String, String> mataData = message.getContent();
        String bar = mataData.get("foo");
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
    MMXMessage.Builder messageBuilder = new MMXMessage.Builder().channel(channel).content(content);
    if(null != attachment) {
      messageBuilder.attachments(attachment);
    }
    MMXMessage message = messageBuilder.build();
    String id = channel.publish(message, new MMXChannel.OnFinishedListener<String>() {
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
      sendMessageLatch.await(TestConstants.TIMEOUT_IN_SECOND, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      fail("channel.publish timeout");
    }

    try {
      Thread.sleep(TestConstants.SLEEP_IN_MILISEC);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    try {
      receiveMessageLatch.await(TestConstants.TIMEOUT_IN_SECOND, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      fail("receive published message timeout");
    }
    MMX.unregisterListener(messageListener);

    return message;
  }

  public static void publishError(MMXChannel channel, final MMXChannel.FailureCode expected) {
    final ExecMonitor<MMXChannel.FailureCode, String> obj = new ExecMonitor<MMXChannel.FailureCode, String>();
    HashMap<String, String> content = new HashMap<String, String>();
    content.put("foo", "bar");
    String id = channel.publish(content, new MMXChannel.OnFinishedListener<String>() {
      @Override
      public void onSuccess(String result) {
        obj.failed("Unexpected success on publishing to a non-existing channel");
      }

      @Override
      public void onFailure(MMXChannel.FailureCode code, Throwable throwable) {
        obj.invoked(code);
      }
    });
    if (obj.waitFor(TestConstants.TIMEOUT_IN_MILISEC) == ExecMonitor.Status.FAILED)
      fail(obj.getFailedValue());
    else
      assertThat(obj.getReturnValue()).isEqualTo(expected);
  }

  public static void getChannelSummary(String channelName, final int expectedChannelCount, final int expectedItemCount) {
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
      latch.await(TestConstants.TIMEOUT_IN_SECOND, TimeUnit.SECONDS);
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

  public static void getChannelSummaryError(String channelName, int expected) {
    final ExecMonitor<ListResult<MMXChannel>, String> obj = new ExecMonitor<ListResult<MMXChannel>, String>();
    MMXChannel.findPublicChannelsByName(channelName, null, null, new MMXChannel.OnFinishedListener<ListResult<MMXChannel>>() {
      @Override
      public void onSuccess(ListResult<MMXChannel> result) {
        obj.invoked(result);
      }

      @Override
      public void onFailure(MMXChannel.FailureCode code, Throwable throwable) {
        obj.failed("Unexpected failure on channel summary of a non-existing channel");
      }
    });
    ExecMonitor.Status status = obj.waitFor(TestConstants.TIMEOUT_IN_MILISEC);
    if (status == ExecMonitor.Status.FAILED)
      fail(obj.getFailedValue());
    else if (status == ExecMonitor.Status.INVOKED)
      assertThat(obj.getReturnValue().totalCount).isEqualTo(expected);
    else
      fail("Getting channel summary timed out");
  }


  public static void unsubscribe(MMXChannel channel) {
    //unsubscribe
    final ExecMonitor<Boolean, MMXChannel.FailureCode> unsubResult = new ExecMonitor<Boolean, MMXChannel.FailureCode>();
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
    ExecMonitor.Status status = unsubResult.waitFor(TestConstants.TIMEOUT_IN_MILISEC);
    if (status == ExecMonitor.Status.INVOKED)
      assertThat(unsubResult.getReturnValue()).isTrue();
    else if (status == ExecMonitor.Status.FAILED)
      fail("Channel unsubscription failed: "+unsubResult.getFailedValue());
    else
      fail("Channel unsubscription timed out");
    //make sure the flag is set to false
    assertThat(channel.isSubscribed()).isFalse();
  }

  public static void unsubscribeError(MMXChannel channel, final MMXChannel.FailureCode expected) {
    final ExecMonitor<MMXChannel.FailureCode, String> obj = new ExecMonitor<MMXChannel.FailureCode, String>();
    channel.unsubscribe(new MMXChannel.OnFinishedListener<Boolean>() {
      @Override
      public void onSuccess(Boolean result) {
        obj.failed("Unexpected success on unsubscribing a non-existing channel");
      }

      @Override
      public void onFailure(MMXChannel.FailureCode code, Throwable throwable) {
        obj.invoked(code);
      }
    });
    if (obj.waitFor(TestConstants.TIMEOUT_IN_MILISEC) == ExecMonitor.Status.FAILED)
      fail(obj.getFailedValue());
    else
      assertThat(obj.getReturnValue()).isEqualTo(expected);
  }


  public static void delete(MMXChannel channel) {
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
    if (deleteResult.waitFor(TestConstants.TIMEOUT_IN_MILISEC) == ExecMonitor.Status.INVOKED)
      assertThat(deleteResult.getReturnValue()).isTrue();
    else
      fail("Channel deletion timed out");
  }

  public static void deleteError(MMXChannel channel, final MMXChannel.FailureCode expected) {
    final ExecMonitor<MMXChannel.FailureCode, String> obj = new ExecMonitor<MMXChannel.FailureCode, String>();
    channel.delete(new MMXChannel.OnFinishedListener<Void>() {
      @Override
      public void onSuccess(Void result) {
        obj.failed("Unexpected success on deleting a non-existing channel");
      }

      @Override
      public void onFailure(MMXChannel.FailureCode code, Throwable throwable) {
        obj.invoked(code);
      }
    });
    if (obj.waitFor(TestConstants.TIMEOUT_IN_MILISEC) == ExecMonitor.Status.FAILED)
      fail(obj.getFailedValue());
    else
      assertThat(obj.getReturnValue()).isEqualTo(expected);
  }

  public static void fetch(MMXChannel channel, final int expectedCount) {
    try {
      Thread.sleep(TestConstants.SLEEP_IN_MILISEC);
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
      latch.await(TestConstants.TIMEOUT_IN_SECOND, TimeUnit.SECONDS);
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

  public static void getPrivateChannel(String name, int expectedMsgs) {
    getChannel(name, false, expectedMsgs);
  }

  public static void getPublicChannel(String name, int expectedMsgs) {
    getChannel(name, true, expectedMsgs);
  }

  public static void getChannel(String name, boolean isPublic, int expectedMsgs) {
    final ExecMonitor<MMXChannel, MMXChannel.FailureCode> getRes = new ExecMonitor<MMXChannel, MMXChannel.FailureCode>();
    if(isPublic) {
      MMXChannel.getPublicChannel(name, new MMXChannel.OnFinishedListener<MMXChannel>() {
        public void onSuccess(MMXChannel result) {
          getRes.invoked(result);
        }

        public void onFailure(MMXChannel.FailureCode code, Throwable ex) {
          getRes.failed(code);
        }
      });
    } else {
      MMXChannel.getPrivateChannel(name, new MMXChannel.OnFinishedListener<MMXChannel>() {
        public void onSuccess(MMXChannel result) {
          getRes.invoked(result);
        }

        public void onFailure(MMXChannel.FailureCode code, Throwable ex) {
          getRes.failed(code);
        }
      });
    }
    assertThat(getRes.waitFor(TestConstants.TIMEOUT_IN_MILISEC)).isEqualTo(ExecMonitor.Status.INVOKED);
    MMXChannel priChannel = getRes.getReturnValue();
    assertThat(priChannel.getNumberOfMessages().intValue()).isEqualTo(expectedMsgs);
    assertThat(priChannel.getName()).isNotNull();
    assertThat(priChannel.getSummary()).isNotNull();
    assertThat(priChannel.getOwnerId()).isNotNull();
    assertThat(priChannel.isPublic()).isEqualTo(isPublic);
    assertThat(priChannel.isSubscribed()).isTrue();
  }


  public static List<User> getSubscribers(final MMXChannel channel) {
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

      @Override public void onFailure(MMXChannel.FailureCode code, Throwable throwable) {
        errorRef.set(throwable);
        getSubLatch.countDown();
        //fail("Failed to get subscriber due to: " + throwable.getMessage());
        //getSubLatch.countDown();
      }
    });
    try {
      getSubLatch.await(TestConstants.TIMEOUT_IN_SECOND, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      fail("Failed to get subscriber : timeout");
    }

    assertThat(errorRef.get()).isNull();

    return subscribers;
  }

  public static void assertSubscribers(List<User> subscribersFound, Set<User> userIdsExpected) {
    printCollection(userIdsExpected, "exected subscribers in assertSubscribers");
    printCollection(subscribersFound, "actual subscribers in assertSubscribers");
    //assertEquals(userIdsExpected.size(), subscribersFound.size());
    assertThat(subscribersFound).hasSize(userIdsExpected.size()).containsAll(userIdsExpected);
    //for(User u : subscribersFound) {
    //  assertThat(userIdsExpected.contains(u.getUserIdentifier())).isTrue();
    //}
  }

  public static void addSubscriber(MMXChannel channel, Set<User> users) {
    final CountDownLatch latch = new CountDownLatch(1);
    final AtomicReference<Throwable> errorRef = new AtomicReference<>();
    channel.addSubscribers(users, new MMXChannel.OnFinishedListener<List<String>>() {
      @Override public void onSuccess(List<String> result) {
        assertThat(result).isEmpty();
        latch.countDown();
      }

      @Override public void onFailure(MMXChannel.FailureCode code, Throwable throwable) {
        errorRef.set(throwable);
        //fail("Failed to add subscriber due to: " + throwable.getMessage());
        latch.countDown();
      }
    });
    try {
      latch.await(TestConstants.TIMEOUT_IN_SECOND, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      fail("Failed to add subscriber : timeout");
    }

    assertThat(errorRef.get()).isNull();
  }

  public static void removeSubscriber(MMXChannel channel, Set<User> users) {
    final CountDownLatch latch = new CountDownLatch(1);
    final AtomicReference<Throwable> errorRef = new AtomicReference<>();
    channel.removeSubscribers(users, new MMXChannel.OnFinishedListener<List<String>>() {
      @Override public void onSuccess(List<String> result) {
        assertThat(result).isEmpty();
        latch.countDown();
      }

      @Override public void onFailure(MMXChannel.FailureCode code, Throwable throwable) {
        errorRef.set(throwable);
        latch.countDown();
      }
    });
    try {
      latch.await(TestConstants.TIMEOUT_IN_SECOND, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      fail("Failed to remove subscriber : timeout");
    }

    assertThat(errorRef.get()).isNull();
  }

  public static List<MMXChannel> findChannelBySubscribers(Set<User> users) {
    final List<MMXChannel> channels = new ArrayList<>();

    final CountDownLatch latch = new CountDownLatch(1);
    final AtomicReference<Throwable> errorRef = new AtomicReference<>();
    MMXChannel.findChannelsBySubscribers(users, ChannelMatchType.EXACT_MATCH, new MMXChannel.OnFinishedListener<ListResult<MMXChannel>>() {
      @Override public void onSuccess(ListResult<MMXChannel> result) {
        channels.addAll(result.items);
        latch.countDown();
      }

      @Override public void onFailure(MMXChannel.FailureCode code, Throwable throwable) {
        errorRef.set(throwable);
        latch.countDown();
      }
    });
    try {
      latch.await(TestConstants.TIMEOUT_IN_SECOND, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      fail("Failed to findChannelsBySubscribers : timeout");
    }

    assertThat(errorRef.get()).isNull();

    return channels;
  }

  public static List<MMXChannel> getAllSubscriptions(int expectedCount) {
    try {
      Thread.sleep(TestConstants.SLEEP_IN_MILISEC);
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
      latch.await(TestConstants.TIMEOUT_IN_SECOND, TimeUnit.SECONDS);
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

  public static List<ChannelDetail> getChannelDetail(MMXChannel channel) {
    final CountDownLatch latch = new CountDownLatch(1);
    final AtomicReference<List<ChannelDetail>> resultRef = new AtomicReference<>();
    final AtomicReference<MMXChannel.FailureCode> errorRef = new AtomicReference<>();
    MMXChannel.getChannelDetail(Arrays.asList(channel),new ChannelDetailOptions.Builder().numOfMessages(10).numOfSubcribers(5).build(),  new MMXChannel.OnFinishedListener<List<ChannelDetail>>() {
      @Override
      public void onSuccess(List<ChannelDetail> result) {
        resultRef.set(result);
        latch.countDown();
      }

      @Override
      public void onFailure(MMXChannel.FailureCode code, Throwable ex) {
        Log.e(TAG, "Exception caught in MMXChannel.getChannelDetail : " + code, ex);
        errorRef.set(code);
      }
    });

    try {
      latch.await(TestConstants.TIMEOUT_IN_SECOND, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      fail("MMXChannel.getChannelDetail timed out");
    }

    if(null != errorRef.get()) {
      fail("MMXChannel.getChannelDetail failed due to " + errorRef.get());
    } else {
      assertThat(resultRef.get()).isNotNull();
      List<ChannelDetail> result = resultRef.get();
      return result;
      //assertThat(result.totalCount).isEqualTo(expectedChannelCount);
      //if (result.items.size() > 0) {
      //  assertThat(result.items.get(0).getNumberOfMessages()).isEqualTo(expectedItemCount);
      //}
    }

    return null;
  }

  public static  void printCollection(Collection<?> set, String description) {
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
