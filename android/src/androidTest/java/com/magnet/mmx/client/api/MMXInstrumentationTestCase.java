package com.magnet.mmx.client.api;

import android.content.Context;
import android.test.InstrumentationTestCase;

import com.magnet.mmx.client.ClientTestConfigImpl;
import com.magnet.mmx.client.common.Log;

import java.util.concurrent.atomic.AtomicBoolean;


abstract public class MMXInstrumentationTestCase extends InstrumentationTestCase {
  private static final String TAG = MMXInstrumentationTestCase.class.getSimpleName();
  private Context mContext;
  protected static final String USERNAME_PREFIX = "mmxusertest";
  protected static final byte[] PASSWORD = "test".getBytes();
  protected static final String DISPLAY_NAME_PREFIX = "MMX TestUser";
  protected static final String NO_SUCH_USERNAME_PREFIX = "nosuchuser";

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
    MMXUser user = new MMXUser.Builder()
            .displayName(displayName)
            .username(username)
            .build();

    final AtomicBoolean success = new AtomicBoolean(false);
    //setup the listener for the registration call
    final MMXUser.OnFinishedListener<Void> listener =
            new MMXUser.OnFinishedListener<Void>() {
              public void onSuccess(Void result) {
                Log.d(TAG, "onSuccess: result=" + result);
                success.set(true);
                synchronized (this) {
                  this.notify();
                }
              }

              public void onFailure(MMXUser.FailureCode code, Throwable ex) {
                Log.e(TAG, "onFailure(): code=" + code, ex);
                synchronized (this) {
                  this.notify();
                }
              }
            };

    //Register the user.  This may fail
    user.register(password, listener);
    synchronized (listener) {
      try {
        listener.wait(10000);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
      assertTrue(success.get());
    }

  }
}
