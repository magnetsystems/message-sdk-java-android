/*   Copyright (c) 2015 Magnet Systems, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.magnet.mmx.client.api;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.google.gson.Gson;
import com.magnet.android.User;
import com.magnet.mmx.client.api.MMXMessage.InvalidRecipientException;
import com.magnet.mmx.client.common.Log;

public class MMXMessageTest extends MMXInstrumentationTestCase {
  private static final String TAG = MMXMessageTest.class.getSimpleName();

  public void testFailureCodes() {
    assertEquals(MMXMessage.FailureCode.BAD_REQUEST, MMX.FailureCode.BAD_REQUEST);
    assertEquals(MMXMessage.FailureCode.BAD_REQUEST, 
        MMXMessage.FailureCode.fromMMXFailureCode(MMX.FailureCode.BAD_REQUEST, null));
  }
  
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
    MMX.start();
    final ExecMonitor<HashMap<String, Object>, Void> receivedResult = new ExecMonitor<HashMap<String, Object>, Void>();
    final StringBuffer senderBuffer = new StringBuffer();
    final ExecMonitor<String, Void> acknowledgeResult = new ExecMonitor<String, Void>();
    MMX.EventListener messageListener = new MMX.EventListener() {
      public boolean onMessageReceived(MMXMessage message) {
        Log.d(TAG, "onMessageReceived(): " + message.getId());
        senderBuffer.append(message.getSender().getFirstName());
        HashMap<String, Object> receivedContent = new HashMap<String, Object>();
        for (Map.Entry<String, String> entry : message.getContent().entrySet()) {
          receivedContent.put(entry.getKey(), entry.getValue());
        }
        receivedResult.invoked(receivedContent);
        //do the acknowledgement
        message.acknowledge(null);
        return false;
      }

      public boolean onMessageAcknowledgementReceived(User from, String messageId) {
        acknowledgeResult.invoked(messageId);
        return false;
      }
    };
    MMX.registerListener(messageListener);

    HashSet<User> recipients = new HashSet<User>();
    recipients.add(MMX.getCurrentUser());

    HashMap<String, String> content = new HashMap<String, String>();
    content.put("foo", "bar");
    MMXMessage message = new MMXMessage.Builder()
            .recipients(recipients)
            .content(content)
            .build();
    final ExecMonitor<String, Boolean> sendResult = new ExecMonitor<String, Boolean>();
    final String messageId = message.send(new MMXMessage.OnFinishedListener<String>() {
      public void onSuccess(String result) {
        Log.e(TAG, "testSendMessage(): onSuccess() msgId=" + result);
        sendResult.invoked(result);
      }

      public void onFailure(MMXMessage.FailureCode code, Throwable ex) {
        Log.e(TAG, "testSendMessage(): failureCode=" + code, ex);
        sendResult.failed(Boolean.TRUE);
      }
    });
    // Check if the send is success
    ExecMonitor.Status status = sendResult.waitFor(10000);
    assertEquals(ExecMonitor.Status.INVOKED, status);
    assertEquals(messageId, sendResult.getReturnValue());
    
    // Check if the receive is success
    status = receivedResult.waitFor(10000);
    if (status == ExecMonitor.Status.WAITING) {
      fail("testSendMessage() receive msg timed out");
    }
    assertEquals("bar", receivedResult.getReturnValue().get("foo"));
    assertEquals(MMX.getCurrentUser().getFirstName(), senderBuffer.toString());

    //check acknowledgement

    status = acknowledgeResult.waitFor(10000);
    if (status == ExecMonitor.Status.WAITING) {
      fail("testSenddMessage() receive acknowledgement timed out");
    }
    assertEquals(messageId, acknowledgeResult.getReturnValue());

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
  
  public void testSendBeforeLogin() {
    HashSet<User> recipients = new HashSet<User>();
    recipients.add(getInvalidUser("foo"));
    HashMap<String,String> content = new HashMap<String,String>();
    content.put("foo","bar");
    MMXMessage message = new MMXMessage.Builder()
            .recipients(recipients)
            .content(content)
            .build();
    final ExecMonitor<String,MMXMessage.FailureCode> failureMonitor = new ExecMonitor<String,MMXMessage.FailureCode>();
    message.send(new MMXMessage.OnFinishedListener<String>() {
      @Override
      public void onSuccess(String result) {
        failureMonitor.invoked(result);
      }

      @Override
      public void onFailure(MMXMessage.FailureCode code, Throwable throwable) {
        Log.e(TAG, "testSendBeforeLogin.onFailure", throwable);
        failureMonitor.failed(code);
      }
    });
    ExecMonitor.Status status = failureMonitor.waitFor(10000);
    if (status == ExecMonitor.Status.INVOKED) {
      fail("should have called onFailure()");
    } else if (status == ExecMonitor.Status.FAILED) {
      assertEquals(MMX.FailureCode.BAD_REQUEST, failureMonitor.getFailedValue());
    } else {
      fail("message.send() timed out");
    }

  }

  public void testPublishBeforeLogin() {
    MMXChannel channel = new MMXChannel.Builder()
            .name("foo").summary("bar").build();
    HashMap<String,String> content = new HashMap<String,String>();
    content.put("foo", "bar");
    final ExecMonitor<String,MMXChannel.FailureCode> failureMonitor = new ExecMonitor<String,MMXChannel.FailureCode>();
    channel.publish(content, new MMXChannel.OnFinishedListener<String>() {
      @Override
      public void onSuccess(String result) {
        failureMonitor.invoked(result);
      }

      @Override
      public void onFailure(MMXChannel.FailureCode code, Throwable throwable) {
        com.magnet.mmx.client.common.Log.e(TAG, "testPublishBeforeLogin.onFailure", throwable);
        failureMonitor.failed(code);
      }
    });
    ExecMonitor.Status status = failureMonitor.waitFor(10000);
    if (status == ExecMonitor.Status.INVOKED) {
      fail("should have called onFailure()");
    } else if (status == ExecMonitor.Status.FAILED) {
      assertEquals(MMX.FailureCode.BAD_REQUEST, failureMonitor.getFailedValue());
    } else {
      fail("channel.publish() timed out");
    }
  }

  public void testSendUCastMessageError() {
    MMX.OnFinishedListener<Void> loginLogoutListener = getLoginLogoutListener();
    String suffix = String.valueOf(System.currentTimeMillis());
    String username = USERNAME_PREFIX + suffix;
    String displayName = DISPLAY_NAME_PREFIX + suffix;
    String noSuchUser = NO_SUCH_USERNAME_PREFIX;
    registerUser(username, displayName, PASSWORD);
    
    final Set<String> invalidUsers = new HashSet<String>();
    
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
    MMX.start();
    final ExecMonitor<Boolean, MMXMessage.FailureCode> receivedResult =
        new ExecMonitor<Boolean, MMXMessage.FailureCode>();
    MMX.EventListener messageListener = new MMX.EventListener() {
      @Override
      public boolean onMessageReceived(MMXMessage message) {
        Log.d(TAG, "onMessageReceived(): " + message.getId());
        receivedResult.invoked(Boolean.TRUE);
        return false;
      }
      @Override
      public boolean onMessageAcknowledgementReceived(User from, String messageId) {
        receivedResult.invoked(Boolean.TRUE);
        return false;
      }
      @Override
      public boolean onMessageSendError(String messageId, 
                        MMXMessage.FailureCode code, String text) {
        Log.d(TAG, "onMessageSendError(): msgId="+messageId+", code="+code+", text="+text);
        receivedResult.failed(code);
        return false;
      }
    };
    MMX.registerListener(messageListener);

    HashSet<User> recipients = new HashSet<User>();
    recipients.add(getInvalidUser("foo"));
    
    HashMap<String, String> content = new HashMap<String, String>();
    content.put("foo", "bar");
    MMXMessage message = new MMXMessage.Builder()
            .recipients(recipients)
            .content(content)
            .build();
    final ExecMonitor<Boolean, MMXMessage.FailureCode> sendResult =
            new ExecMonitor<Boolean, MMXMessage.FailureCode>();
    final String messageId = message.send(new MMXMessage.OnFinishedListener<String>() {
      @Override
      public void onSuccess(String msgId) {
        sendResult.invoked(Boolean.TRUE);
      }
      @Override
      public void onFailure(MMXMessage.FailureCode code, Throwable ex) {
        Log.d(TAG, "Unicast Send Msg failed: code="+code+", ex="+ex);
        if (code.equals(MMXMessage.FailureCode.INVALID_RECIPIENT)) {
          InvalidRecipientException irEx = (InvalidRecipientException) ex;
          invalidUsers.addAll(irEx.getUserIds());
        }
        sendResult.failed(code);
      }
    });
    
    // Send failed because of an invalid recipient
    ExecMonitor.Status status = sendResult.waitFor(10000);
    assertEquals(ExecMonitor.Status.FAILED, status);
    assertEquals(MMXMessage.FailureCode.INVALID_RECIPIENT, sendResult.getFailedValue());
    assertEquals(1, invalidUsers.size());
    assertTrue(invalidUsers.contains("foo"));
    
    // Make sure that no error message came and no message arrived.
    status = receivedResult.waitFor(2000);
    assertEquals(ExecMonitor.Status.WAITING, status);

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
  
  public void testSendMCastMessageError() {
    MMX.OnFinishedListener<Void> loginLogoutListener = getLoginLogoutListener();
    String suffix = String.valueOf(System.currentTimeMillis());
    String username = USERNAME_PREFIX + suffix;
    String displayName = DISPLAY_NAME_PREFIX + suffix;
    String noSuchUser = NO_SUCH_USERNAME_PREFIX;
    String wrongUser = WRONG_USERNAME_PREFIX;
    registerUser(username, displayName, PASSWORD);

    final Set<String> invalidUsers = new HashSet<String>();
    
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
    MMX.start();
    final ExecMonitor<Boolean, MMXMessage.FailureCode> receivedResult =
            new ExecMonitor<Boolean, MMXMessage.FailureCode>();
    MMX.EventListener messageListener = new MMX.EventListener() {
      @Override
      public boolean onMessageReceived(MMXMessage message) {
        Log.d(TAG, "onMessageReceived(): " + message.getId());
        receivedResult.invoked(Boolean.TRUE);
        return false;
      }
      @Override
      public boolean onMessageAcknowledgementReceived(User from, String messageId) {
        receivedResult.invoked(Boolean.TRUE);
        return false;
      }
      @Override
      public boolean onMessageSendError(String messageId, 
                        MMXMessage.FailureCode code, String text) {
        Log.d(TAG, "onMessageSendError(): msgId="+messageId+", code="+code+", text="+text);
        receivedResult.failed(code);
        return false;
      }
    };
    MMX.registerListener(messageListener);

    HashSet<User> recipients = new HashSet<User>();
    User badRecipient1 = getInvalidUser(noSuchUser);
    recipients.add(badRecipient1);
    User badRecipient2 = getInvalidUser(wrongUser);
    recipients.add(badRecipient2);
    User goodRecipient = MMX.getCurrentUser();
    recipients.add(goodRecipient);

    HashMap<String, String> content = new HashMap<String, String>();
    content.put("foo", "bar");
    MMXMessage message = new MMXMessage.Builder()
            .recipients(recipients)
            .content(content)
            .build();
    final ExecMonitor<Boolean, MMXMessage.FailureCode> sendResult =
            new ExecMonitor<Boolean, MMXMessage.FailureCode>();
    final String messageId = message.send(new MMXMessage.OnFinishedListener<String>() {
      @Override
      public void onSuccess(String msgId) {
        sendResult.invoked(Boolean.TRUE);
      }
      @Override
      public void onFailure(MMXMessage.FailureCode code, Throwable ex) {
        Log.d(TAG, "Multicast onFailure() ex="+ex);
        if (code.equals(MMXMessage.FailureCode.INVALID_RECIPIENT)) {
          InvalidRecipientException irEx = (InvalidRecipientException) ex;
          invalidUsers.addAll(irEx.getUserIds());
        }
        sendResult.failed(code);
      }
    });
    
    // Send partial failure despite invalid recipients
    ExecMonitor.Status status = sendResult.waitFor(10000);
    assertEquals(ExecMonitor.Status.FAILED, status);
    assertEquals(MMXMessage.FailureCode.INVALID_RECIPIENT, sendResult.getFailedValue());
    assertEquals(2, invalidUsers.size());
    assertTrue(invalidUsers.contains(badRecipient1.getUserName()));
    assertTrue(invalidUsers.contains(badRecipient2.getUserName()));
    
    // Do a sleep; one error message per invalid recipient will be received.
    try {
      Thread.sleep(2000);
    } catch (InterruptedException e) {
      // Ignored.
    }
    // One message should arrive.
    status = receivedResult.waitFor(1000);
    assertEquals(ExecMonitor.Status.INVOKED, status);
    assertTrue(receivedResult.getReturnValue());
    
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
