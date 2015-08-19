package com.magnet.mmx.client.api;

import java.util.concurrent.atomic.AtomicInteger;

public class MMXUserTest extends MMXInstrumentationTestCase {
  private static final String TAG = MMXUserTest.class.getSimpleName();

  public void testFindUser() {
    final MMX.OnFinishedListener<Void> loginLogoutListener = getLoginLogoutListener();
    String suffix = String.valueOf(System.currentTimeMillis());
    String username = USERNAME_PREFIX + suffix;
    String displayName = DISPLAY_NAME_PREFIX + suffix;
    registerUser(username, displayName, suffix, PASSWORD);

    MMX.login(username, PASSWORD, loginLogoutListener);
    synchronized (loginLogoutListener) {
      try {
        loginLogoutListener.wait(10000);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
    assertTrue(MMX.getMMXClient().isConnected());
    final AtomicInteger totalCount = new AtomicInteger(0);
    MMX.OnFinishedListener<MMXUser.FindResult> listener =
            new MMX.OnFinishedListener<MMXUser.FindResult>() {
      @Override
      public void onSuccess(MMXUser.FindResult result) {
        totalCount.set(result.totalCount);
      }

      @Override
      public void onFailure(MMX.FailureCode code, Throwable ex) {

      }
    };
    MMXUser.findByName(displayName, 10, listener);
    synchronized (listener) {
      try {
        listener.wait(10000);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
    assertTrue(totalCount.get() == 1);

    MMX.logout(loginLogoutListener);
    synchronized (loginLogoutListener) {
      try {
        loginLogoutListener.wait(10000);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
    assertFalse(MMX.getMMXClient().isConnected());
  }

}
