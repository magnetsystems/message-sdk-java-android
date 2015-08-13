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

  private final MagnetMessage.OnFinishedListener<Void> mSessionListener =
          new MagnetMessage.OnFinishedListener<Void>() {
            public void onSuccess(Void result) {
              synchronized (this) {
                this.notify();
              }
            }

            public void onFailure(MagnetMessage.FailureCode code, Exception ex) {
              synchronized (this) {
                this.notify();
              }
            }
          };

  @Override
  protected final void setUp() throws Exception {
    Log.setLoggable(null, Log.VERBOSE);
    mContext = this.getInstrumentation().getTargetContext();
    MagnetMessage.init(mContext, new ClientTestConfigImpl(mContext));
    postSetUp();
  }

  /**
   * Override this to do any additional setup.  This is
   * called at the end of the setUp() method.
   */
  protected void postSetUp() {

  }

  protected MagnetMessage.OnFinishedListener<Void> getSessionListener() {
    return mSessionListener;
  }

  protected Context getContext() {
    return mContext;
  }

  protected void registerUser(String username, String displayName, String email, byte[] password) {
    MMXUser user = new MMXUser.Builder()
            .email(email)
            .displayName(displayName)
            .username(username)
            .build();

    //setup the listener for the registration call
    final MagnetMessage.OnFinishedListener<Boolean> listener =
            new MagnetMessage.OnFinishedListener<Boolean>() {
              public void onSuccess(Boolean result) {
                Log.d(TAG, "onSuccess: result=" + result);
                synchronized (this) {
                  this.notify();
                }
              }

              public void onFailure(MagnetMessage.FailureCode code, Exception ex) {
                Log.e(TAG, "onFailure(): code=" + code, ex);
                synchronized (this) {
                  this.notify();
                }
              }
            };

    //Register the user.  This may fail
    MMXUser.register(user, password, listener);
    synchronized (listener) {
      try {
        listener.wait(10000);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }

  }
}
