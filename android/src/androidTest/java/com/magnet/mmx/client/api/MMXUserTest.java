package com.magnet.mmx.client.api;

import com.magnet.mmx.client.common.Log;

public class MMXUserTest extends MMXInstrumentationTestCase {
  private static final String TAG = MMXUserTest.class.getSimpleName();
  private static final String USERNAME = "mmxusertest";
  private static final byte[] PASSWORD = "test".getBytes();
  private static final String EMAIL = "test@test.com";
  private static final String DISPLAY_NAME = "MMX TestUser";

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
            .email(EMAIL)
            .displayName(DISPLAY_NAME)
            .username(USERNAME)
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
    MMXUser.register(user, PASSWORD, listener);
    synchronized (listener) {
      try {
        listener.wait(10000);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }

    //login as this new user
    MMXUser.login(USERNAME, PASSWORD, sessionListener);
    synchronized (sessionListener) {
      try {
        sessionListener.wait(10000);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
    MMXUser currentUser = MMXUser.getCurrentUser();
    assertEquals(DISPLAY_NAME, currentUser.getDisplayName());
    assertEquals(USERNAME, currentUser.getUsername());
    assertEquals(EMAIL, currentUser.getEmail());

    MMXUser.logout(sessionListener);
    synchronized (sessionListener) {
      try {
        sessionListener.wait(10000);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
    //still connected, but should be anonymous again
    assertNull(MMXUser.getCurrentUser());

    MagnetMessage.endSession(sessionListener);
    synchronized (sessionListener) {
      try {
        sessionListener.wait(10000);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
    //assertFalse(MagnetMessage.getMMXClient().isConnected());
  }

  public void testFindUser() {
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

    MMXUser.FindResult result = MMXUser.findByName(DISPLAY_NAME, 10);
    assertTrue(result.totalCount > 0);

    MagnetMessage.endSession(sessionListener);
    synchronized (sessionListener) {
      try {
        sessionListener.wait(10000);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
  }
}
