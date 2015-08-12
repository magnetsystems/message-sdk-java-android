package com.magnet.mmx.client.api;

import com.magnet.mmx.client.common.Log;

public class MMXUserTest extends MMXInstrumentationTestCase {
  private static final String TAG = MMXUserTest.class.getSimpleName();

  public void testLogin() {
    final MagnetMessage.OnFinishedListener<Void> sessionListener = getSessionListener();
    MagnetMessage.startSession(sessionListener);
    synchronized (sessionListener) {
      try {
        sessionListener.wait(10000);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
    assertTrue(MagnetMessage.getMMXClient().isConnected());
    MMXUser user = new MMXUser.Builder()
            .email("test@test.com")
            .displayName("Test User")
            .username("testuser")
            .build();
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
    MMXUser.register(user, "test".getBytes(), listener);
    synchronized (listener) {
      try {
        listener.wait(10000);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
    MagnetMessage.endSession(sessionListener);
    synchronized (sessionListener) {
      try {
        sessionListener.wait(10000);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
    assertFalse(MagnetMessage.getMMXClient().isConnected());
  }
}
