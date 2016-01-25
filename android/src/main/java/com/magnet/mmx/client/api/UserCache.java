package com.magnet.mmx.client.api;

import android.util.LruCache;
import com.magnet.max.android.ApiCallback;
import com.magnet.max.android.ApiError;
import com.magnet.max.android.User;
import com.magnet.mmx.client.common.Log;

import java.util.ArrayList;
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
  static final long DEFAULT_ACCEPTED_AGE = 8 * 60 * 60000; //8 hours
  static final int DEFAULT_CACHE_ENTRIES = 128;
  static final int DEFAULT_USER_RETRIEVE_TIMEOUT = 10;
  private final LruCache<String, CachedUser> mUserCache;
  private static UserCache sInstance = null;

  private UserCache() {
    mUserCache = new LruCache<>(DEFAULT_CACHE_ENTRIES);
  }

  static synchronized UserCache getInstance() {
    if (sInstance == null) {
      sInstance = new UserCache();
    }
    return sInstance;
  }

  /**
   * Fills the cache with the specified userIds.  This is a blocking call and should not be
   * called from the main thread.
   *
   * @param userIds the userIds to lookup
   * @param acceptedAgeMillis the allowed age in milliseconds
   */
  void fillCacheByUserId(Set<String> userIds, long acceptedAgeMillis) {
    fillCacheHelper(userIds, acceptedAgeMillis);
  }

  void fillCacheHelper(Set<String> keys, long acceptedAgeMillis) {
    final ArrayList<String> retrieveList = new ArrayList<String>();

    synchronized (this) {
      for (String key : keys) {
        CachedUser cachedUser = mUserCache.get(key);
        if (cachedUser == null) {
          Log.v(TAG, "fillCache(): cache missed for user: " + key);
          retrieveList.add(key);
        } else if((System.currentTimeMillis() - cachedUser.timestamp) > acceptedAgeMillis) {
          Log.v(TAG, "fillCache(): cache expired for user: " + key +  ", cachedUser=" + cachedUser);
          mUserCache.remove(key);
          retrieveList.add(key);
        }
      }
    }

    if(!retrieveList.isEmpty()) {
      final CountDownLatch latch = new CountDownLatch(1);
      final ApiCallback<List<User>> listener = new ApiCallback<List<User>>() {
        public void success(List<User> users) {
          Log.d(TAG, "fillCache(): retrieved users " + users.size());
          long timestamp = System.currentTimeMillis();
          for (User user : users) {
            CachedUser cachedUser = new CachedUser(user, timestamp);
            mUserCache.put(user.getUserIdentifier().toLowerCase(), cachedUser);
          }
          latch.countDown();
        }

        public void failure(ApiError apiError) {
          Log.d(TAG, "fillCache(): error retrieving users: " + apiError, apiError.getCause());
          latch.countDown();
        }
      };

      User.getUsersByUserIds(retrieveList, listener);
      synchronized (listener) {
        try {
          latch.await(DEFAULT_USER_RETRIEVE_TIMEOUT, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
          Log.e(TAG, "fillCache(): exception", e);
        }
      }
    }
  }

  /**
   * Retrieve the user from the cache.
   *
   * @param userId the user to retrieve
   * @return the user or null if user is not in the cache
   */
  User getByUserId(String userId) {
    synchronized (this) {
      CachedUser cachedUser = mUserCache.get(userId.toLowerCase());
      return cachedUser == null ? null : cachedUser.user;
    }
  }
}
