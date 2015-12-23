package com.magnet.mmx.client.api;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.test.InstrumentationTestCase;
import com.google.gson.Gson;
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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

abstract public class MMXInstrumentationTestCase extends InstrumentationTestCase {
  private static final String TAG = MMXInstrumentationTestCase.class.getSimpleName();

  public static final int TIMEOUT_IN_SECOND = 50;
  public static final int TIMEOUT_IN_MILISEC = TIMEOUT_IN_SECOND * 1000;
  public static final int SLEEP_IN_MILISEC = 3 * 1000;

  private static Context mContext;
  protected static final String USERNAME_PREFIX = "mmxusertest";
  protected static final byte[] PASSWORD = "test".getBytes();
  protected static final String DISPLAY_NAME_PREFIX = "MMX TestUser";
  protected static final String NO_SUCH_USERNAME_PREFIX = "nosuchuser";
  protected static final String WRONG_USERNAME_PREFIX = "wronguser";
  protected static final String INVALID_USER_JSON =
          "{\"clientId\":\"USERNAME-client-id\",\"firstName\":\"USERNAME-first-name\",\"roles\":[\"USER\"],\"userAccountData\":{},\"userIdentifier\":\"USERNAME-user-id\",\"userName\":\"USERNAME\",\"userStatus\":\"ACTIVE\"}";

  protected static Map<String, User> REGISTERED_USERS = new HashMap<>();
  protected static String MMX_TEST_USER_PREFIX = "MMX_TEST_USER_";
  protected static String MMX_TEST_USER_1 = "MMX_TEST_USER_1";
  protected static String MMX_TEST_USER_2 = "MMX_TEST_USER_2";
  protected static String MMX_TEST_USER_3 = "MMX_TEST_USER_3";
  protected static String MMX_TEST_USER_4 = "MMX_TEST_USER_4";
  protected static String MMX_TEST_USER_5 = "MMX_TEST_USER_5";

  private static boolean isMaxInited;

  protected User getInvalidUser(String username) {
    Gson gson = new Gson();
    return gson.fromJson(INVALID_USER_JSON.replace("USERNAME", username), User.class);
  }

  @Override
  protected final void setUp() throws Exception {
    if(!isMaxInited) {
      Log.setLoggable(null, Log.VERBOSE);
      mContext = this.getInstrumentation().getTargetContext();
      HandlerThread callbackThread = new HandlerThread("TestCaseCallbackThread");
      callbackThread.start();
      MMX.setCallbackHandler(new Handler(callbackThread.getLooper()));
      MaxCore.init(mContext, new MaxAndroidJsonConfig(mContext, com.magnet.mmx.test.R.raw.keys));
      MaxCore.register(MMX.getModule());
      isMaxInited = true;
    }
    postSetUp();
  }

  /**
   * Override this to do any additional setup.  This is
   * called at the end of the setUp() method.
   */
  protected void postSetUp() {

  }

  protected void logoutMMX() {
    final CountDownLatch latch = new CountDownLatch(1);
    final AtomicBoolean logoutResult = new AtomicBoolean(false);
    MMX.logout(new MMX.OnFinishedListener<Void>() {
      @Override
      public void onSuccess(Void result) {
        logoutResult.set(true);
        latch.countDown();
      }

      @Override
      public void onFailure(MMX.FailureCode code, Throwable ex) {
        latch.countDown();
      }
    });
    try {
      //Log.d(TAG, "logoutMMX(): result=" + logoutResult.get());
      assertThat(logoutResult.get()).isFalse();
      latch.await(TIMEOUT_IN_SECOND, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  protected void loginMax(String userName, String password) {
    final CountDownLatch latch = new CountDownLatch(1);
    final AtomicReference<ApiError> errorRef = new AtomicReference<>();
    android.util.Log.d(TAG, "--------Logging user " + userName);
    User.login(userName, password, false, new ApiCallback<Boolean>() {
      @Override
      public void success(Boolean aBoolean) {
        if (aBoolean.booleanValue()) {
          //MaxCore.initModule(MMX.getModule(), new ApiCallback<Boolean>() {
          //  @Override
          //  public void success(Boolean aBoolean) {
          //    latch.countDown();
          //  }
          //
          //  @Override
          //  public void failure(ApiError apiError) {
          //    android.util.Log.e(TAG, "Failed to initModule MMX due to : " + apiError.getMessage(), apiError.getCause());
          //    errorRef.set(apiError);
          //    fail("Failed to initModule MMX  due to: " + apiError.getMessage());
          //    latch.countDown();
          //  }
          //});
        }

        latch.countDown();
      }

      @Override
      public void failure(ApiError apiError) {
        android.util.Log.e(TAG, "Failed to login due to : " + apiError.getMessage(), apiError.getCause());
        fail("Failed to login due to : " + apiError.getMessage());
        errorRef.set(apiError);
        latch.countDown();
        //latch.countDown();
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

  protected void helpLogout() {
    if(null == User.getCurrentUser()) {
      android.util.Log.d(TAG, "--------No user login, no need to logout");
      return;
    }

    //Log.d(TAG, "----------calling logout from ", new Exception());

    android.util.Log.d(TAG, "--------Logout user " + User.getCurrentUser().getUserName());
    final CountDownLatch latch = new CountDownLatch(1);
    final AtomicReference<ApiError> errorRef = new AtomicReference<>();
    logoutMMX();
    User.logout(new ApiCallback<Boolean>() {
      @Override
      public void success(Boolean aBoolean) {
        //MaxCore.deInitModule(MMX.getModule(), null);
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

  protected ApiCallback<Boolean> getLogoutListener() { return new ApiCallback<Boolean>() {
    @Override
    public void success(Boolean aBoolean) {
      MaxCore.deInitModule(MMX.getModule(), null);
      synchronized (this) {
        this.notify();
      }
    }

    @Override
    public void failure(ApiError apiError) {
      synchronized (this) {
        this.notify();
      }
    }
  };
  }

  protected Context getContext() {
    return mContext;
  }

  protected void registerUser(String username, String displayName, byte[] password) {
    helpRegisterUser(username, displayName, password, ExecMonitor.Status.INVOKED, null);
  }

  protected void helpRegisterUser(String username, String displayName,
      byte[] password, ExecMonitor.Status status, ApiError apiError) {
    helpRegisterUser(username, displayName, password, status, apiError, false);
  }
  
  protected User helpRegisterUser(final String username, String displayName,
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
}
