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
    String suffix = String.valueOf(System.currentTimeMillis());
    String username = USERNAME_PREFIX + suffix;
    String displayName = DISPLAY_NAME_PREFIX + suffix;
    registerUser(username, displayName, suffix, PASSWORD);

    //login as this new user
    MMXUser.login(username, PASSWORD, sessionListener);
    synchronized (sessionListener) {
      try {
        sessionListener.wait(10000);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
    MMXUser currentUser = MMXUser.getCurrentUser();
    assertEquals(displayName, currentUser.getDisplayName());
    assertEquals(username, currentUser.getUsername());
    assertEquals(suffix, currentUser.getEmail());

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
    String suffix = String.valueOf(System.currentTimeMillis());
    String username = USERNAME_PREFIX + suffix;
    String displayName = DISPLAY_NAME_PREFIX + suffix;
    registerUser(username, displayName, suffix, PASSWORD);

    MMXUser.FindResult result = MMXUser.findByName(displayName, 10);
    assertTrue(result.totalCount == 1);

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
