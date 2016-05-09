/**
 * Copyright (c) 2012-2016 Magnet Systems. All rights reserved.
 */
package com.magnet.mmx.client.api;

import android.util.Log;
import com.magnet.max.android.ApiCallback;
import com.magnet.max.android.ApiError;
import com.magnet.max.android.User;
import com.magnet.mmx.client.MMXClient;
import com.magnet.mmx.client.MMXTask;
import com.magnet.mmx.client.common.PrivacyManager;
import com.magnet.mmx.protocol.MMXid;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

public class MMXUserPreferences {
  private static final String TAG = "MMXUserPreferences";

  private static ReentrantLock sBlockListLock = new ReentrantLock();


  /**
   * Block users to prevent them from sending message to current user
   * @param users
   * @param listener
   */
  public static void blockUsers(final Set<User> users, final ApiCallback<Boolean> listener) {
    if(checkUsers(users, listener) && checkStatus(listener)) {
      new UserBlockingTask(users, true, listener).execute();
    }
  }

  /**
   * Unblock users to allow them to send message to current user
   * @param users
   * @param listener
   */
  public static void unblockUsers(final Set<User> users, final ApiCallback<Boolean> listener) {
    if(checkUsers(users, listener) && checkStatus(listener)) {
      new UserBlockingTask(users, false, listener).execute();
    }
  }

  /**
   * Get the list of users blocked by current user
   * @param listener
   */
  public static void getBlockedUsers(final ApiCallback<List<User>> listener) {
    if(checkStatus(listener)) {
      new MMXTask<List<User>>(MMX.getMMXClient(), MMX.getHandler()) {
        @Override
        public List<User> doRun(MMXClient mmxClient) throws Throwable {
          List<MMXid> existingList = PrivacyManager.getInstance(MMX.getMMXClient().getMMXConnection()).getPrivacyList();
          if(null != existingList && !existingList.isEmpty()) {
            List<String> userIds = new ArrayList<String>();
            for(MMXid mid : existingList) {
              userIds.add(mid.getUserId());
            }

            final CountDownLatch latch = new CountDownLatch(1);
            final AtomicReference<List<User>> resultRef = new AtomicReference<>();
            User.getUsersByUserIds(userIds, new ApiCallback<List<User>>() {
              @Override public void success(List<User> users) {
                resultRef.set(users);
                latch.countDown();
              }

              @Override public void failure(ApiError apiError) {
                Log.e(TAG, "Failed to getUsersByUserIds in getBlockedUsers due to " + apiError.getMessage(), apiError.getCause());
                latch.countDown();
              }
            });

            latch.await(5, TimeUnit.SECONDS);

            return resultRef.get();
          } else {
            return Collections.EMPTY_LIST;
          }
        }

        @Override
        public void onException(final Throwable exception) {
          if (listener != null) {
            MMX.getCallbackHandler().post(new Runnable() {
              @Override
              public void run() {
                listener.failure(new ApiError(exception));
              }
            });
          }
        }

        @Override
        public void onResult(final List<User> result) {
          if (listener != null) {
            MMX.getCallbackHandler().post(new Runnable() {
              @Override
              public void run() {
                listener.success(result);
              }
            });
          }
        }
      }.execute();
    }
  }


  private static boolean checkStatus(final ApiCallback listener) {
    ApiError failureCode = null;
    if(null == User.getCurrentUser()) {
      failureCode = MMX.USER_NOT_LOGIN;
    } else if(!MMX.getMMXClient().isConnected()) {
      failureCode = MMX.CONNECTION_NOT_AVAILABLE;
    }

    if(null != failureCode) {
      if(null != listener) {
        final ApiError failureCodeFinal = failureCode;
        MMX.getCallbackHandler().post(new Runnable() {
          @Override
          public void run() {
            listener.failure(failureCodeFinal);
          }
        });
      }

      return false;
    }

    return true;
  }

  private static boolean checkUsers(Set<User> users, final ApiCallback<Boolean> listener) {
    if(null == users || users.isEmpty()) {
      if(null != listener) {
        MMX.getCallbackHandler().post(new Runnable() {
          @Override
          public void run() {
            listener.success(Boolean.TRUE);
          }
        });
      }

      return false;
    }

    return true;
  }

  private static List<MMXid> usersToMMXids(Set<User> users) {
    List<MMXid> mmXidList = new ArrayList<>(users.size());
    for(User u :users) {
      mmXidList.add(new MMXid(u.getUserIdentifier(), null, u.getDisplayName()));
    }

    return mmXidList;
  }

  private static class UserBlockingTask extends MMXTask<Boolean> {
    private final ApiCallback<Boolean> listener;
    private final boolean isAdding;
    private final Set<User> users;

    public UserBlockingTask(Set<User> users, boolean isAdding, ApiCallback<Boolean> listener) {
      super(MMX.getMMXClient(), MMX.getHandler());
      this.users = users;
      this.listener = listener;
      this.isAdding = isAdding;
    }

    @Override
    public Boolean doRun(MMXClient mmxClient) throws Throwable {
      if(sBlockListLock.tryLock() || sBlockListLock.tryLock(5L, TimeUnit.SECONDS)) {
        try {
          PrivacyManager privacyManager = PrivacyManager.getInstance(MMX.getMMXClient().getMMXConnection());
          List<MMXid> existingList = privacyManager.getPrivacyList();
          if (isAdding) {
            if (null == existingList) {
              existingList = new ArrayList<MMXid>();
            }
            existingList.addAll(usersToMMXids(users));
          } else {
            if (null != existingList) {
              existingList.removeAll(usersToMMXids(users));
            }
          }
          privacyManager.setPrivacyList(existingList);

          if (isAdding) {
            privacyManager.enablePrivacyList(true);
          }
        } finally {
          sBlockListLock.unlock();
        }
      } else {
        Log.d(TAG, "Failed to obtain BlockList lock");
      }

      return Boolean.TRUE;
    }

    @Override
    public void onException(final Throwable exception) {
      if (listener != null) {
        MMX.getCallbackHandler().post(new Runnable() {
          @Override
          public void run() {
            listener.failure(new ApiError(exception));
          }
        });
      }
    }

    @Override
    public void onResult(final Boolean result) {
      if (listener != null) {
        MMX.getCallbackHandler().post(new Runnable() {
          @Override
          public void run() {
            listener.success(result);
          }
        });
      }
    }
  }
}
