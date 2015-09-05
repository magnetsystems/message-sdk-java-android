package com.magnet.mmx.client.api;

import com.magnet.mmx.client.common.Log;

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
    registerUser(username, displayName, PASSWORD);

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
    MMX.enableIncomingMessages(true);
    final HashMap<String, Object> receivedContent = new HashMap<String, Object>();
    final StringBuffer senderBuffer = new StringBuffer();
    MMX.EventListener messageListener = new MMX.EventListener() {
      public boolean onMessageReceived(MMXMessage message) {
        Log.d(TAG, "onMessageReceived(): " + message.getId());
        senderBuffer.append(message.getSender().getDisplayName());
        for (Map.Entry<String, String> entry : message.getContent().entrySet()) {
          receivedContent.put(entry.getKey(), entry.getValue());
        }
        synchronized (this) {
          this.notify();
        }
        return false;
      }

      public boolean onMessageAcknowledgementReceived(MMXUser from, String messageId) {
        return false;
      }
    };
    MMX.registerListener(messageListener);

    HashSet<MMXUser> recipients = new HashSet<MMXUser>();
    recipients.add(new MMXUser.Builder()
            .username(username)
            .displayName(displayName)
            .build());

    HashMap<String, String> content = new HashMap<String, String>();
    content.put("foo", "bar");
    MMXMessage message = new MMXMessage.Builder()
            .recipients(recipients)
            .content(content)
            .build();
    final StringBuffer resultSb = new StringBuffer();
    final String messageId = message.send(new MMXMessage.OnFinishedListener<String>() {
      public void onSuccess(String result) {
        resultSb.append(result);
      }

      public void onFailure(MMXMessage.FailureCode code, Throwable ex) {
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
    assertEquals(MMX.getCurrentUser().getDisplayName(), senderBuffer.toString());

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
  
  private String mErrorMsgId;
  private boolean mSendSuccess;
  private boolean mMsgOrAckReceived;

  public void testSendMessageError() {
    MMX.OnFinishedListener<Void> loginLogoutListener = getLoginLogoutListener();
    String suffix = String.valueOf(System.currentTimeMillis());
    String username = USERNAME_PREFIX + suffix;
    String displayName = DISPLAY_NAME_PREFIX + suffix;
    String noSuchUser = NO_SUCH_USERNAME_PREFIX;
    registerUser(username, displayName, PASSWORD);

    mErrorMsgId = null;
    mSendSuccess = false;
    mMsgOrAckReceived = false;
    
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
    MMX.enableIncomingMessages(true);
    final HashMap<String, Object> receivedContent = new HashMap<String, Object>();
    MMX.EventListener messageListener = new MMX.EventListener() {
      
      public boolean onMessageReceived(MMXMessage message) {
        Log.d(TAG, "onMessageReceived(): " + message.getId());
        mMsgOrAckReceived = true;
        return false;
      }

      public boolean onMessageAcknowledgementReceived(MMXUser from, String messageId) {
        mMsgOrAckReceived = true;
        return false;
      }
      
      public boolean onMessageSendError(String messageId, MMXMessage.FailureCode fcode, String text) {
        assertEquals(MMXMessage.FailureCode.INVALID_RECIPIENT, fcode);
        mErrorMsgId = messageId;
        return false;
      }
    };
    MMX.registerListener(messageListener);

    HashSet<MMXUser> recipients = new HashSet<MMXUser>();
    recipients.add(new MMXUser.Builder()
            .username(noSuchUser)
            .build());

    HashMap<String, String> content = new HashMap<String, String>();
    content.put("foo", "bar");
    MMXMessage message = new MMXMessage.Builder()
            .recipients(recipients)
            .content(content)
            .build();
    final String messageId = message.send(new MMXMessage.OnFinishedListener<String>() {
      public void onSuccess(String result) {
        mSendSuccess = true;
      }

      public void onFailure(MMXMessage.FailureCode code, Throwable ex) {
        Log.e(TAG, "testSendMessage(): failureCode=" + code, ex);
      }
    });
    
    synchronized (messageListener) {
      try {
        messageListener.wait(10000);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
    // Send success, not expecting received msg or ack, expect an error of invalid recipient.
    assertTrue(mSendSuccess);
    assertFalse(mMsgOrAckReceived);
    assertEquals(messageId, mErrorMsgId);
    
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
