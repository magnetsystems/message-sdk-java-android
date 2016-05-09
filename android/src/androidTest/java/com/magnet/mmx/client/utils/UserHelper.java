/**
 * Copyright (c) 2012-2016 Magnet Systems. All rights reserved.
 */
package com.magnet.mmx.client.utils;

import com.google.gson.Gson;
import com.magnet.max.android.ApiCallback;
import com.magnet.max.android.ApiError;
import com.magnet.max.android.User;
import com.magnet.max.android.auth.model.UserRegistrationInfo;
import com.magnet.mmx.client.common.Log;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class UserHelper {
  private static final String TAG = UserHelper.class.getSimpleName();

  protected static Map<String, User> REGISTERED_USERS = new HashMap<>();
  public static String MMX_TEST_USER_PREFIX = "MMX_TEST_USER_";
  public static String MMX_TEST_USER_1 = "MMX_TEST_USER_1";
  public static String MMX_TEST_USER_2 = "MMX_TEST_USER_2";
  public static String MMX_TEST_USER_3 = "MMX_TEST_USER_3";
  public static String MMX_TEST_USER_4 = "MMX_TEST_USER_4";
  public static String MMX_TEST_USER_5 = "MMX_TEST_USER_5";

  public static final String USERNAME_PREFIX = "mmxusertest";
  public static final String PASSWORD = "test";
  public static final String DISPLAY_NAME_PREFIX = "MMX TestUser";
  public static final String NO_SUCH_USERNAME_PREFIX = "nosuchuser";
  public static final String WRONG_USERNAME_PREFIX = "wronguser";

  protected static final String INVALID_USER_JSON =
      "{\"clientId\":\"USERNAME-client-id\",\"firstName\":\"USERNAME-first-name\",\"roles\":[\"USER\"],\"userAccountData\":{},\"userIdentifier\":\"USERNAME-user-id\",\"userName\":\"USERNAME\",\"userStatus\":\"ACTIVE\"}";

  public static User getInvalidUser(String username) {
    Gson gson = new Gson();
    return gson.fromJson(INVALID_USER_JSON.replace("USERNAME", username), User.class);
  }

  public static User registerUser(final String username, final String displayName,
      final String password, final Integer apiErrorKind,
      final boolean reuseExistingUser) {
    User result = null;
    if(reuseExistingUser && REGISTERED_USERS.containsKey(username)) {
      Log.d(TAG, "User " + username + " already registered, reuse it");
      result = REGISTERED_USERS.get(username);
    } else {
      Log.d(TAG, "Registering new User " + username);
      UserRegistrationInfo userInfo = new UserRegistrationInfo.Builder().firstName(displayName)
          .userName(username)
          .password(password)
          .build();

      final ExecMonitor<User, ApiError> userReg = new ExecMonitor<User, ApiError>("Register User");
      final AtomicBoolean userExist = new AtomicBoolean(false);
      //setup the listener for the registration call
      ApiCallback<User> listener = new ApiCallback<User>() {
        public void success(User user) {
          Log.d(TAG, "register user success: result=" + user.getUserName());
          REGISTERED_USERS.put(username, user);
          userReg.invoked(user);
        }

        @Override public void failure(ApiError apiError) {
          Log.e(TAG, "register user failure(): code=" + apiError, apiError.getCause());
          if (reuseExistingUser && apiError.getKind() == 409) {
            userExist.set(true);
            userReg.invoked(null);
          } else {
            userReg.failed(apiError);
          }
        }
      };

      //Register the user.  This may fail
      User.register(userInfo, listener);
      ExecMonitor.Status actual = userReg.waitFor(TestConstants.TIMEOUT_IN_MILISEC);

      if (actual == ExecMonitor.Status.INVOKED) {
        if (!reuseExistingUser) {
          assertEquals(username.toLowerCase(), userReg.getReturnValue().getUserName());
        }

        if(!userExist.get()) {
          result = userReg.getReturnValue();
        }
      } else if (actual == ExecMonitor.Status.FAILED) {
        if(null != apiErrorKind) {
          assertEquals(apiErrorKind.intValue(), userReg.getFailedValue().getKind());
        }
        fail("User.register: apiError cause=" + userReg.getFailedValue()
            .getCause()
            .getClass()
            .getName());
      }
    }

    return result;
  }

  public static void registerAndLogin(final String username, final String password) {
    registerAndLogin(username, password, true);
  }

  public static void registerAndLogin(final String username, final String password, boolean reuseExistingUser) {
    User user = registerUser(username, username, password, null, true);
    login(username, password);
  }

  public static void login(String userName, String password) {
    final CountDownLatch latch = new CountDownLatch(1);
    final AtomicReference<ApiError> errorRef = new AtomicReference<>();
    android.util.Log.d(TAG, "--------Logging user " + userName);
    User.login(userName, password, false, new ApiCallback<Boolean>() {
      @Override
      public void success(Boolean aBoolean) {
        if (aBoolean.booleanValue()) {

        }

        latch.countDown();
      }

      @Override
      public void failure(ApiError apiError) {
        android.util.Log.e(TAG, "Failed to login due to : " + apiError.getMessage(), apiError.getCause());
        errorRef.set(apiError);
        latch.countDown();
      }
    });

    try {
      latch.await(TestConstants.TIMEOUT_IN_SECOND, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      fail("User.login timeout");
    }

    if(null != errorRef.get()) {
      fail("User.login failed due to " + errorRef.get().getMessage());
    }
  }

  public static List<User> searchUserByNane(String userName) {
    final ExecMonitor<List<User>, ApiError> execMonitor = new ExecMonitor<>();
    android.util.Log.d(TAG, "--------Find user by name " + userName);
    User.getUsersByUserNames(Arrays.asList(userName), new ApiCallback<List<User>>() {
      @Override
      public void success(List<User> result) {
        execMonitor.invoked(result);
      }

      @Override
      public void failure(ApiError apiError) {
        android.util.Log.e(TAG, "Failed to login due to : " + apiError.getMessage(), apiError.getCause());
        //fail("Failed to login due to : " + apiError.getMessage());
        execMonitor.failed(apiError);
      }
    });

    ExecMonitor.Status status = execMonitor.waitFor(TestConstants.TIMEOUT_IN_MILISEC);
    assertEquals(ExecMonitor.Status.INVOKED, status);

    return execMonitor.getReturnValue();
  }

  public static User findOrCreateUser(final String username, final String password) {
    User result = null;
    List<User> existingUser = searchUserByNane(username);
    if(null == existingUser || existingUser.isEmpty()) {
      result = UserHelper.registerUser(username, username, password, null, true);
    } else {
      result = existingUser.get(0);
    }

    return result;
  }

  public static void logout() {
    if(null == User.getCurrentUser()) {
      android.util.Log.d(TAG, "--------No user login, no need to logout");
      return;
    }

    //Log.d(TAG, "----------calling logout from ", new Exception());

    android.util.Log.d(TAG, "--------Logout user " + User.getCurrentUser().getUserName());
    final CountDownLatch latch = new CountDownLatch(1);
    final AtomicReference<ApiError> errorRef = new AtomicReference<>();
    //logoutMMX();
    User.logout(new ApiCallback<Boolean>() {
      @Override
      public void success(Boolean aBoolean) {
        //MaxCore.deInitModule(MMX.getModule(), null);
        latch.countDown();
      }

      @Override
      public void failure(ApiError apiError) {
        errorRef.set(apiError);
        latch.countDown();
      }
    });

    try {
      latch.await(TestConstants.TIMEOUT_IN_SECOND, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      fail("User.logout timeout");
    }

    if(null != errorRef.get()) {
      fail("User.logout failed due to " + errorRef.get().getMessage());
    }
    //assertFalse(MMX.getMMXClient().isConnected());
  }
}
