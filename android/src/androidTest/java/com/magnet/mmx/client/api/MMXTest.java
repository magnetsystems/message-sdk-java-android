package com.magnet.mmx.client.api;

import com.magnet.max.android.ApiCallback;
import com.magnet.max.android.User;

public class MMXTest extends MMXInstrumentationTestCase {
  private static final String TAG = MMXTest.class.getSimpleName();

  public void testLoginLogout() {
    String suffix = String.valueOf(System.currentTimeMillis());
    String username = USERNAME_PREFIX + suffix;
    String displayName = DISPLAY_NAME_PREFIX + suffix;
    registerUser(username, displayName, PASSWORD);

    //login as this new user
    loginMax(username, new String(PASSWORD));

    User currentUser = MMX.getCurrentUser();
    assertEquals(displayName, currentUser.getFirstName());
    assertEquals(username, currentUser.getUserName());

    logoutMMX();
    ApiCallback<Boolean> logoutListener = getLogoutListener();
    User.logout(logoutListener);
    synchronized (logoutListener) {
      try {
        logoutListener.wait(10000);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
//    assertNull(MMX.getCurrentUser());
//    assertFalse(MMX.getMMXClient().isConnected());
  }
}
