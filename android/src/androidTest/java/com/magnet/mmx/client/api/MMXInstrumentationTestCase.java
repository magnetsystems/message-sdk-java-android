package com.magnet.mmx.client.api;

import android.content.Context;
import android.test.InstrumentationTestCase;

import com.google.gson.Gson;
import com.magnet.android.ApiCallback;
import com.magnet.android.ApiError;
import com.magnet.android.User;
import com.magnet.android.auth.model.UserRegistrationInfo;
import com.magnet.mmx.client.ClientTestConfigImpl;
import com.magnet.mmx.client.common.Log;


abstract public class MMXInstrumentationTestCase extends InstrumentationTestCase {
  private static final String TAG = MMXInstrumentationTestCase.class.getSimpleName();
  private Context mContext;
  protected static final String USERNAME_PREFIX = "mmxusertest";
  protected static final byte[] PASSWORD = "test".getBytes();
  protected static final String DISPLAY_NAME_PREFIX = "MMX TestUser";
  protected static final String NO_SUCH_USERNAME_PREFIX = "nosuchuser";
  protected static final String WRONG_USERNAME_PREFIX = "wronguser";
  protected static final String INVALID_USER_JSON =
          "{\"clientId\":\"USERNAME-client-id\",\"firstName\":\"USERNAME-first-name\",\"roles\":[\"USER\"],\"userAccountData\":{},\"userIdentifier\":\"USERNAME-user-id\",\"userName\":\"USERNAME\",\"userStatus\":\"ACTIVE\"}";

  protected User getInvalidUser(String username) {
    Gson gson = new Gson();
    return gson.fromJson(INVALID_USER_JSON.replace("USERNAME", username), User.class);
  }

  private final MMX.OnFinishedListener<Void> mLoginLogoutListener =
          new MMX.OnFinishedListener<Void>() {
            public void onSuccess(Void result) {
              synchronized (this) {
                this.notify();
              }
            }

            public void onFailure(MMX.FailureCode code, Throwable ex) {
              synchronized (this) {
                this.notify();
              }
            }
          };

  @Override
  protected final void setUp() throws Exception {
    Log.setLoggable(null, Log.VERBOSE);
    mContext = this.getInstrumentation().getTargetContext();
    MMX.init(mContext, new ClientTestConfigImpl(mContext));
    postSetUp();
  }

  /**
   * Override this to do any additional setup.  This is
   * called at the end of the setUp() method.
   */
  protected void postSetUp() {

  }

  protected MMX.OnFinishedListener<Void> getLoginLogoutListener() {
    return mLoginLogoutListener;
  }

  protected Context getContext() {
    return mContext;
  }

  protected void registerUser(String username, String displayName, byte[] password) {
    helpRegisterUser(username, displayName, password, ExecMonitor.Status.INVOKED, null);
  }
  
  protected void helpRegisterUser(String username, String displayName,
                                  byte[] password, ExecMonitor.Status status, ApiError apiError) {
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
        userReg.invoked(user);
      }

      @Override
      public void failure(ApiError apiError) {
        Log.e(TAG, "onFailure(): code=" + apiError, apiError.getCause());
        userReg.failed(apiError);
      }
    };

    //Register the user.  This may fail
    User.register(userInfo, listener);
    ExecMonitor.Status actual = userReg.waitFor(10000);
    assertEquals(status, actual);
    if (actual == ExecMonitor.Status.INVOKED) {
      assertEquals(username.toLowerCase(), userReg.getReturnValue().getUserName());
    } else if (actual == ExecMonitor.Status.FAILED) {
      assertEquals(apiError.getKind(), userReg.getFailedValue().getKind());
      fail("NEED TO IMPLEMENT THIS: apiError cause=" + userReg.getFailedValue().getCause().getClass().getName());
    }
  }
}
