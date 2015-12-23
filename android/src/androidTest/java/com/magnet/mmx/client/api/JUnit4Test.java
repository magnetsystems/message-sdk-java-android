/**
 * Copyright (c) 2012-2015 Magnet Systems. All rights reserved.
 */
package com.magnet.mmx.client.api;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import com.magnet.max.android.ApiCallback;
import com.magnet.max.android.ApiError;
import com.magnet.max.android.MaxCore;
import com.magnet.max.android.User;
import com.magnet.max.android.auth.model.UserRegistrationInfo;
import com.magnet.mmx.client.common.Log;
import com.magnet.mmx.client.utils.MaxAndroidJsonConfig;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class JUnit4Test {
  private static final String TAG = JUnit4Test.class.getSimpleName();

  protected static Map<String, User> REGISTERED_USERS = new HashMap<>();
  protected static String MMX_TEST_USER_PREFIX = "MMX_TEST_USER_";
  protected static String MMX_TEST_USER_1 = "MMX_TEST_USER_1";
  protected static String MMX_TEST_USER_2 = "MMX_TEST_USER_2";
  protected static String MMX_TEST_USER_3 = "MMX_TEST_USER_3";
  protected static String MMX_TEST_USER_4 = "MMX_TEST_USER_4";
  protected static String MMX_TEST_USER_5 = "MMX_TEST_USER_5";

  public static final int TIMEOUT_IN_SECOND = 50;
  public static final int TIMEOUT_IN_MILISEC = TIMEOUT_IN_SECOND * 1000;
  public static final int SLEEP_IN_MILISEC = 3 * 1000;

  private static Context mContext;

  @BeforeClass
  public static void init() {
    mContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
    MaxCore.init(mContext, new MaxAndroidJsonConfig(mContext, com.magnet.mmx.test.R.raw.keys));

    Log.setLoggable(null, Log.VERBOSE);
    //HandlerThread callbackThread = new HandlerThread("TestCaseCallbackThread");
    //callbackThread.start();
    //MMX.setCallbackHandler(new Handler(callbackThread.getLooper()));

    String userName = MMX_TEST_USER_PREFIX + System.currentTimeMillis();
    helpRegisterUser(userName, userName, userName.getBytes(), null, null, true);
    loginMax(userName, userName);
  }

  @AfterClass
  public static void tearDown() {
    helpLogout();
    mContext = null;
  }

  @Test
  public void test1() {
    assertNotNull(mContext);
    assertTrue(true);
  }

  protected static User helpRegisterUser(final String username, String displayName,
      byte[] password, ExecMonitor.Status status, ApiError apiError,
      final boolean reuseExistingUser) {
    if(reuseExistingUser && REGISTERED_USERS.containsKey(username)) {
      Log.d(TAG, "User " + username + " already registered, reuse it");
      return REGISTERED_USERS.get(username);
    }

    UserRegistrationInfo userInfo = new UserRegistrationInfo.Builder()
        .firstName(displayName)
        .userName(username)
        .password(new String(password))
        .build();

    final ExecMonitor<User, ApiError> userReg = new ExecMonitor<User, ApiError>();
    //setup the listener for the registration call
    ApiCallback<User> listener = new ApiCallback<User>() {
      public void success(User user) {
        Log.d(TAG, "onSuccess: result=" + user.getUserName());
        REGISTERED_USERS.put(username, user);
        userReg.invoked(user);
      }

      @Override
      public void failure(ApiError apiError) {
        Log.e(TAG, "onFailure(): code=" + apiError, apiError.getCause());
        if(reuseExistingUser && apiError.getKind() == 409) {
          //final ExecMonitor<User, ApiError> findUserMonitor = new ExecMonitor<User, ApiError>();
          //User.getUsersByUserNames(Arrays.asList(username), new ApiCallback<List<User>>() {
          //  @Override public void success(List<User> users) {
          //    if(null != users && users.size() == 1) {
          //      User foundUser = users.get(0);
          //      findUserMonitor.invoked(foundUser);
          //      userReg.invoked(foundUser);
          //      REGISTERED_USERS.put(username, foundUser);
          //    }
          //  }
          //
          //  @Override public void failure(ApiError apiError) {
          //    userReg.failed(apiError);
          //  }
          //});
          //
          //findUserMonitor.waitFor(10*1000);
          userReg.invoked(null);
        } else {
          userReg.failed(apiError);
        }
      }
    };

    //Register the user.  This may fail
    User.register(userInfo, listener);
    ExecMonitor.Status actual = userReg.waitFor(10000);
    if(null != status) {
      assertEquals(status, actual);
    }
    if (actual == ExecMonitor.Status.INVOKED) {
      if(!reuseExistingUser) {
        assertEquals(username.toLowerCase(), userReg.getReturnValue().getUserName());
      }

      return userReg.getReturnValue();
    } else if (actual == ExecMonitor.Status.FAILED) {
      assertEquals(apiError.getKind(), userReg.getFailedValue().getKind());
      fail("NEED TO IMPLEMENT THIS: apiError cause=" + userReg.getFailedValue().getCause().getClass().getName());
    }

    return null;
  }

  protected static void loginMax(String userName, String password) {
    final CountDownLatch latch = new CountDownLatch(2);
    final AtomicReference<ApiError> errorRef = new AtomicReference<>();
    android.util.Log.d(TAG, "--------Logging user " + userName);
    User.login(userName, password, false, new ApiCallback<Boolean>() {
      @Override
      public void success(Boolean aBoolean) {
        if (aBoolean.booleanValue()) {
          MaxCore.initModule(MMX.getModule(), new ApiCallback<Boolean>() {
            @Override
            public void success(Boolean aBoolean) {
              latch.countDown();
            }

            @Override
            public void failure(ApiError apiError) {
              android.util.Log.e(TAG, "Failed to initModule MMX due to : " + apiError.getMessage(), apiError.getCause());
              errorRef.set(apiError);
              fail("Failed to initModule MMX  due to: " + apiError.getMessage());
              latch.countDown();
            }
          });
        }

        latch.countDown();
      }

      @Override
      public void failure(ApiError apiError) {
        android.util.Log.e(TAG, "Failed to login due to : " + apiError.getMessage(), apiError.getCause());
        fail("Failed to login due to : " + apiError.getMessage());
        errorRef.set(apiError);
        latch.countDown();
        latch.countDown();
      }
    });

    try {
      latch.await(TIMEOUT_IN_SECOND, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      fail("User.login timeout");
    }

    if(null != errorRef.get()) {
      fail("User.login failed due to " + errorRef.get().getMessage());
    }
  }

  protected static void helpLogout() {
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
        MaxCore.deInitModule(MMX.getModule(), null);
        latch.countDown();
      }

      @Override
      public void failure(ApiError apiError) {
        latch.countDown();
      }
    });

    try {
      latch.await(TIMEOUT_IN_SECOND, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      fail("User.login timeout");
    }

    if(null != errorRef.get()) {
      fail("User.logout failed due to " + errorRef.get().getMessage());
    }
    //assertFalse(MMX.getMMXClient().isConnected());
  }
}
