package com.magnet.mmx.client.api;

import android.os.Handler;
import android.os.HandlerThread;

import com.magnet.android.ApiCallback;
import com.magnet.android.ApiError;
import com.magnet.android.User;
import com.magnet.mmx.client.common.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

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
  private final HashMap<String,CachedUser> mUserCache = new HashMap<String,CachedUser>();
  private final Handler mHandler;
  private static UserCache sInstance = null;

  private UserCache() {
    HandlerThread thread = new HandlerThread("UserCacheThread");
    thread.start();
    mHandler = new Handler(thread.getLooper());
  }

  static synchronized UserCache getInstance() {
    if (sInstance == null) {
      sInstance = new UserCache();
    }
    return sInstance;
  }

  /**
   * Fills the cache with the specified usernames.  This is a blocking call.
   *
   * @param usernames the usernames to lookup
   * @param acceptedAgeMillis the allowed age in milliseconds
   */
  void fillCache(Set<String> usernames, long acceptedAgeMillis) {
    final ArrayList<String> retrieveUsers = new ArrayList<String>();

    synchronized (mUserCache) {
      for (String username : usernames) {
        CachedUser cachedUser = mUserCache.get(username);
        if (cachedUser == null ||
                (System.currentTimeMillis() - cachedUser.timestamp) > acceptedAgeMillis) {
          Log.v(TAG, "fillCache(): retrieving user: " + username + ", cachedUser=" + cachedUser);
          mUserCache.remove(username);
          retrieveUsers.add(username);
        }
      }
    }

    final ApiCallback<List<User>> listener = new ApiCallback<List<User>>() {
      public void success(List<User> users) {
        Log.d(TAG, "fillCache(): retrieved users " + users.size());
        long timestamp = System.currentTimeMillis();
        synchronized (mUserCache) {
          for (User user : users) {
            mUserCache.put(user.getUserName().toLowerCase(), new CachedUser(user, timestamp));
          }
        }
        synchronized (this) {
          notify();
        }
      }

      public void failure(ApiError apiError) {
        Log.d(TAG, "fillCache(): error retrieving users: " + apiError, apiError.getCause());
        synchronized (this) {
          notify();
        }
      }
    };

    mHandler.post(new Runnable() {
      public void run() {
        User.getUsersByUserNames(retrieveUsers, listener);
      }
    });
    synchronized (listener) {
      try {
        listener.wait(10000);
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
  User get(String username) {
    synchronized (mUserCache) {
      CachedUser cachedUser = mUserCache.get(username.toLowerCase());
      return cachedUser == null ? null : cachedUser.user;
    }
  }
}
