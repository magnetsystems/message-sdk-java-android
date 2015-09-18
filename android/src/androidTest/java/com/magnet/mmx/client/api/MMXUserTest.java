package com.magnet.mmx.client.api;

import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import android.util.Log;

import com.magnet.mmx.client.api.MMXUser.FailureCode;

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
    
    // test change display name
    MMXUser user = MMX.getCurrentUser();
    assertNotNull(user);
    String oldDisplayName = user.getDisplayName();
    String newDisplayName = "New User Name";
    helpChangeDisplayName(user, newDisplayName, null);
    assertEquals(newDisplayName, user.getDisplayName());
    helpChangeDisplayName(user, oldDisplayName, null);
    assertEquals(oldDisplayName, user.getDisplayName());

    // test get all users
    final ExecMonitor<ListResult<MMXUser>, MMXUser.FailureCode> findRes = new ExecMonitor<ListResult<MMXUser>, MMXUser.FailureCode>();
    MMXUser.OnFinishedListener<ListResult<MMXUser>> listener = new
        MMXUser.OnFinishedListener<ListResult<MMXUser>>() {
      public void onSuccess(ListResult<MMXUser> result) {
        findRes.invoked(result);
      }
      
      public void onFailure(MMXUser.FailureCode code, Throwable ex) {
        Log.e(TAG, "failed on find users: code="+code, ex);
        findRes.failed(code);
      }
    };
    MMXUser.getAllUsers(0, 10, listener);
    assertEquals(ExecMonitor.Status.INVOKED, findRes.waitFor(10000));
    int numUsers = findRes.getReturnValue().items.size();
    assertTrue(numUsers > 0 && numUsers <= 10);
    
    // Test if empty search string is disallowed
    findRes.reset(null, null);
    MMXUser.findByDisplayName("", 0, 10, listener);
    assertEquals(ExecMonitor.Status.FAILED, findRes.waitFor(10000));
    assertEquals(MMXUser.FailureCode.BAD_REQUEST, findRes.getFailedValue());
    
    //test findByDisplayName()
    final AtomicInteger totalCount = new AtomicInteger(0);
    final StringBuffer displayNameBuffer = new StringBuffer();
    MMXUser.findByDisplayName(displayName, 0, 10, new MMXUser.OnFinishedListener<ListResult<MMXUser>>() {
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
    MMXUser.getUsers(names, new MMXUser.OnFinishedListener<Map<String, MMXUser>>() {
      public void onSuccess(Map<String, MMXUser> result) {
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

  
  private void helpChangeDisplayName(MMXUser user, final String newDisplayName,
                                      FailureCode expectedFailureCode) {
    final ExecMonitor<Void, FailureCode> changeRes = new ExecMonitor<Void, FailureCode>();
    user.changeDisplayName(newDisplayName, new MMXUser.OnFinishedListener<Void>() {
      @Override
      public void onSuccess(Void result) {
        changeRes.invoked(null);
      }

      @Override
      public void onFailure(FailureCode code, Throwable throwable) {
        changeRes.failed(code);
      }
    });
    ExecMonitor.Status status = changeRes.waitFor(10000);
    if (expectedFailureCode == null) {
      assertEquals(ExecMonitor.Status.INVOKED, status);
    } else {
      assertEquals(ExecMonitor.Status.FAILED, status);
      assertEquals(expectedFailureCode, changeRes.getFailedValue());
    }
  }
}
