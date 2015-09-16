package com.magnet.mmx.client.api;

import android.content.Context;
import android.test.InstrumentationTestCase;

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
      byte[] password, ExecMonitor.Status status, MMXUser.FailureCode code) {
    MMXUser user = new MMXUser.Builder()
            .displayName(displayName)
            .username(username)
            .build();

    final ExecMonitor<Boolean, MMXUser.FailureCode> userReg = new ExecMonitor<Boolean, MMXUser.FailureCode>();
    //setup the listener for the registration call
    final MMXUser.OnFinishedListener<Void> listener =
            new MMXUser.OnFinishedListener<Void>() {
              public void onSuccess(Void result) {
                Log.d(TAG, "onSuccess: result=" + result);
                userReg.invoked(Boolean.TRUE);
              }

              public void onFailure(MMXUser.FailureCode code, Throwable ex) {
                Log.e(TAG, "onFailure(): code=" + code, ex);
                userReg.failed(code);
              }
            };

    //Register the user.  This may fail
    user.register(password, listener);
    
    ExecMonitor.Status actual = userReg.waitFor(10000);
    assertEquals(status, actual);
    if (actual == ExecMonitor.Status.INVOKED) {
      assertTrue(userReg.getReturnValue());
    } else if (actual == ExecMonitor.Status.FAILED) {
      assertEquals(code, userReg.getFailedValue());
    }
  }
}
