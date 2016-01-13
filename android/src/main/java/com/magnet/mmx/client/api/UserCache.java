package com.magnet.mmx.client.api;

import com.magnet.max.android.ApiCallback;
import com.magnet.max.android.ApiError;
import com.magnet.max.android.User;
import com.magnet.mmx.client.common.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * An in-memory user cache for convenience
 */
final class UserCache {
  private class CachedUser {
    private final User user;
    private final long timestamp;

    private CachedUser(User user, long timestamp) {
      this.user = user;
      this.timestamp = timestamp;
    }
  }

  private static final String TAG = UserCache.class.getSimpleName();
  static final long DEFAULT_ACCEPTED_AGE = 5 * 60000; // five minutes
  private final HashMap<String,CachedUser> mUserNameCache = new HashMap<String,CachedUser>();
  private final HashMap<String,CachedUser> mUserIdCache = new HashMap<String,CachedUser>();
  private static UserCache sInstance = null;

  private UserCache() {
  }

  static synchronized UserCache getInstance() {
    if (sInstance == null) {
      sInstance = new UserCache();
    }
    return sInstance;
  }

  /**
   * Fills the cache with the specified usernames.  This is a blocking call and should not be
   * called from the main thread.
   *
   * @param usernames the usernames to lookup
   * @param acceptedAgeMillis the allowed age in milliseconds
   */
  void fillCacheByUsername(Set<String> usernames, long acceptedAgeMillis) {
    fillCacheHelper(usernames, false, acceptedAgeMillis);
  }

  /**
   * Fills the cache with the specified userIds.  This is a blocking call and should not be
   * called from the main thread.
   *
   * @param userIds the userIds to lookup
   * @param acceptedAgeMillis the allowed age in milliseconds
   */
  void fillCacheByUserId(Set<String> userIds, long acceptedAgeMillis) {
    fillCacheHelper(userIds, true, acceptedAgeMillis);
  }

  void fillCacheHelper(Set<String> keys, final boolean isUserIds, long acceptedAgeMillis) {
    final ArrayList<String> retrieveList = new ArrayList<String>();

    synchronized (this) {
      for (String key : keys) {
        CachedUser cachedUser = isUserIds ? mUserIdCache.get(key) : mUserNameCache.get(key);
        if (cachedUser == null ||
                (System.currentTimeMillis() - cachedUser.timestamp) > acceptedAgeMillis) {
          Log.v(TAG, "fillCache(): retrieving user: " + key + ", isUserId=" +
                  isUserIds +  ", cachedUser=" + cachedUser);
          mUserNameCache.remove(key);
          mUserIdCache.remove(key);
          retrieveList.add(key);
        }
      }
    }

    final CountDownLatch latch = new CountDownLatch(1);
    final ApiCallback<List<User>> listener = new ApiCallback<List<User>>() {
      public void success(List<User> users) {
        Log.d(TAG, "fillCache(): retrieved users " + users.size());
        long timestamp = System.currentTimeMillis();
        synchronized (UserCache.this) {
          for (User user : users) {
            CachedUser cachedUser = new CachedUser(user, timestamp);
            mUserNameCache.put(user.getUserName().toLowerCase(), cachedUser);
            mUserIdCache.put(user.getUserIdentifier().toLowerCase(), cachedUser);
          }
        }
        latch.countDown();
      }

      public void failure(ApiError apiError) {
        Log.d(TAG, "fillCache(): error retrieving users: " + apiError, apiError.getCause());
        latch.countDown();
      }
    };

    if (isUserIds) {
      User.getUsersByUserIds(retrieveList, listener);
    } else {
      User.getUsersByUserNames(retrieveList, listener);
    }
    synchronized (listener) {
      try {
        latch.await(10, TimeUnit.SECONDS);
      } catch (InterruptedException e) {
        Log.e(TAG, "fillCache(): exception", e);
      }
    }
  }

  /**
   * Retrieve the user from the cache.
   *
   * @param username the user to retrieve
   * @return the user or null if user is not in the cache
   */
  User getByUsername(String username) {
    synchronized (this) {
      CachedUser cachedUser = mUserNameCache.get(username.toLowerCase());
      return cachedUser == null ? null : cachedUser.user;
    }
  }

  User getByUserId(String userId) {
    synchronized (this) {
      CachedUser cachedUser = mUserIdCache.get(userId.toLowerCase());
      return cachedUser == null ? null : cachedUser.user;
    }
  }
}
