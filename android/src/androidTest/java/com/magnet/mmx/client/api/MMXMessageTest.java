package com.magnet.mmx.client.api;

import com.magnet.mmx.client.common.Log;
import com.magnet.mmx.client.common.MMXid;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class MMXMessageTest extends MMXInstrumentationTestCase {
  private static final String TAG = MMXMessageTest.class.getSimpleName();

  public void testSendMessage() {
    MMX.OnFinishedListener<Void> loginLogoutListener = getLoginLogoutListener();
    String suffix = String.valueOf(System.currentTimeMillis());
    String username = USERNAME_PREFIX + suffix;
    String displayName = DISPLAY_NAME_PREFIX + suffix;
    registerUser(username, displayName, suffix, PASSWORD);

    //login with credentials
    MMX.login(username, PASSWORD, loginLogoutListener);
    synchronized (loginLogoutListener) {
      try {
        loginLogoutListener.wait(10000);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
    assertTrue(MMX.getMMXClient().isConnected());
    final HashMap<String, Object> receivedContent = new HashMap<String, Object>();
    MMX.OnMessageReceivedListener messageListener = new MMX.OnMessageReceivedListener() {
      public boolean onMessageReceived(MMXMessage message) {
        Log.d(TAG, "onMessageReceived(): " + message.getId());
        for (Map.Entry<String, String> entry : message.getContent().entrySet()) {
          receivedContent.put(entry.getKey(), entry.getValue());
        }
        synchronized (this) {
          this.notify();
        }
        return false;
      }

      public boolean onMessageAcknowledgementReceived(MMXid from, String messageId) {
        return false;
      }
    };
    MMX.registerListener(messageListener);

    HashSet<String> recipients = new HashSet<String>();
    recipients.add(username);

    HashMap<String, String> content = new HashMap<String, String>();
    content.put("foo", "bar");
    MMXMessage message = new MMXMessage.Builder()
            .recipients(recipients)
            .content(content)
            .build();
    final StringBuffer resultSb = new StringBuffer();
    final String messageId = message.send(new MMX.OnFinishedListener<String>() {
      public void onSuccess(String result) {
        resultSb.append(result);
      }

      public void onFailure(MMX.FailureCode code, Throwable ex) {
        Log.e(TAG, "testSendMessage(): failureCode=" + code, ex);
        fail();
      }
    });
    synchronized (messageListener) {
      try {
        messageListener.wait(10000);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
    assertEquals(resultSb.toString(), messageId);
    assertEquals("bar", receivedContent.get("foo"));

    MMX.unregisterListener(messageListener);
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
