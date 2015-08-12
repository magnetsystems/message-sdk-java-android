package com.magnet.mmx.client.api;

import android.content.Context;
import android.test.InstrumentationTestCase;

import com.magnet.mmx.client.common.Log;


abstract public class MMXInstrumentationTestCase extends InstrumentationTestCase {
  private static final String TAG = MMXInstrumentationTestCase.class.getSimpleName();
  private Context mContext;

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
    MagnetMessage.init(mContext, com.magnet.mmx.test.R.raw.test);
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
}
