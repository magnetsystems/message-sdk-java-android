package com.magnet.mmx.client.api;

import android.content.Context;
import android.test.InstrumentationTestCase;

import com.magnet.mmx.client.common.Log;

public class MagnetMessageTest extends InstrumentationTestCase {
  private Context mContext;

  @Override
  protected void setUp() throws Exception {
    Log.setLoggable(null, Log.VERBOSE);
    mContext = this.getInstrumentation().getTargetContext();
  }

  public void testStartSession() {
    MagnetMessage.startSession(mContext, com.magnet.mmx.test.R.raw.test);
    synchronized (this) {
      try {
        this.wait(10000);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
  }
}
