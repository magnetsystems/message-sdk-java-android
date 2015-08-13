package com.magnet.mmx.client.api;

import com.magnet.mmx.client.common.Log;
import com.magnet.mmx.client.common.MMXid;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class MMXMessageTest extends MMXInstrumentationTestCase {
  private static final String TAG = MMXMessageTest.class.getSimpleName();

  public void testSendMessage() {
    final MagnetMessage.OnFinishedListener<Void> sessionListener = getSessionListener();
    //start the session
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

    //login with credentials
    MMXUser.login(username, PASSWORD, sessionListener);
    synchronized (sessionListener) {
      try {
        sessionListener.wait(10000);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
    assertTrue(MagnetMessage.getMMXClient().isConnected());
    final HashMap<String, Object> receivedContent = new HashMap<String, Object>();
    MagnetMessage.OnMessageReceivedListener messageListener = new MagnetMessage.OnMessageReceivedListener() {
      public boolean onMessageReceived(MMXMessage message) {
        Log.d(TAG, "onMessageReceived(): " + message.getId());
        for (Map.Entry<String, Object> entry : message.getContent().entrySet()) {
          receivedContent.put(entry.getKey(), entry.getValue());
        }
        synchronized (this) {
          this.notify();
        }
        return false;
      }
    };
    MagnetMessage.registerListener(messageListener);

    HashSet< MMXid > recipients = new HashSet<MMXid>();
    recipients.add(new MMXid(username));

    HashMap<String, Object> content = new HashMap<String, Object>();
    content.put("foo", "bar");
    MMXMessage message = new MMXMessage.Builder()
            .recipients(recipients)
            .content(content)
            .build();
    final String messageId = message.send(new MagnetMessage.OnFinishedListener<String>() {
      public void onSuccess(String result) {
        assertNotNull(result);
      }

      public void onFailure(MagnetMessage.FailureCode code, Exception ex) {
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
    assertNotNull(messageId);
    assertEquals("bar", receivedContent.get("foo"));

    MagnetMessage.unregisterListener(messageListener);
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
