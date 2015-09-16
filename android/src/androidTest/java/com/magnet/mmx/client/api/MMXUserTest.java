package com.magnet.mmx.client.api;

import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.atomic.AtomicInteger;

import android.util.Log;

public class MMXUserTest extends MMXInstrumentationTestCase {
  private static final String TAG = MMXUserTest.class.getSimpleName();

  public void testFindUser() {
    final MMX.OnFinishedListener<Void> loginLogoutListener = getLoginLogoutListener();
    String suffix = String.valueOf(System.currentTimeMillis());
    final String username = USERNAME_PREFIX + suffix;
    String displayName = DISPLAY_NAME_PREFIX + suffix;
    registerUser(username, displayName, PASSWORD);

    MMX.login(username, PASSWORD, loginLogoutListener);
    synchronized (loginLogoutListener) {
      try {
        loginLogoutListener.wait(10000);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
    assertTrue(MMX.getMMXClient().isConnected());

    //test findByDisplayName()
    final AtomicInteger totalCount = new AtomicInteger(0);
    final StringBuffer displayNameBuffer = new StringBuffer();
    MMXUser.findByDisplayName(displayName, 10, new MMXUser.OnFinishedListener<ListResult<MMXUser>>() {
      @Override
      public void onSuccess(ListResult<MMXUser> result) {
        totalCount.set(result.totalCount);
        MMXUser user = result.items.get(0);
        displayNameBuffer.append(user.getDisplayName());
        synchronized(totalCount) {
          totalCount.notify();
        }
      }

      @Override
      public void onFailure(MMXUser.FailureCode code, Throwable ex) {
        Log.e(TAG, "Exception caught: " + code, ex);
        synchronized(totalCount) {
          totalCount.notify();
        }
      }
    });
    synchronized (totalCount) {
      try {
        totalCount.wait(10000);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
    assertEquals(1, totalCount.get());
    assertEquals(displayName, displayNameBuffer.toString());

    //test getByUsernames()
    HashSet<String> names = new HashSet<String>();
    names.add(username);
    totalCount.set(0);
    displayNameBuffer.delete(0, displayNameBuffer.length());
    MMXUser.getByUsernames(names, new MMXUser.OnFinishedListener<HashMap<String, MMXUser>>() {
      public void onSuccess(HashMap<String, MMXUser> result) {
        totalCount.set(result.size());
        MMXUser user = result.get(username);
        displayNameBuffer.append(user.getDisplayName());
        synchronized (totalCount) {
          totalCount.notify();
        }
      }

      public void onFailure(MMXUser.FailureCode code, Throwable ex) {
        Log.e(TAG, "Exception caught: " + code, ex);
        synchronized (totalCount) {
          totalCount.notify();
        }
      }
    });
    synchronized (totalCount) {
      try {
        totalCount.wait(10000);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
    assertEquals(1, totalCount.get());
    assertEquals(displayName, displayNameBuffer.toString());

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

  public void testRegisterShortUserName() {
    // This test case is only for REST API.  Custom IQ has no such restriction.
    String userName = "siva";
    String displayName = "Siva";
    byte[] passwd = "password".getBytes();
    helpRegisterUser(userName, displayName, passwd, ExecMonitor.Status.FAILED,
        MMXUser.FailureCode.REGISTRATION_INVALID_USERNAME);
  }
}
