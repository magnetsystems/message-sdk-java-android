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
    MMX.enableIncomingMessages(true);
    final ExecMonitor<HashMap<String, Object>, Void> receivedResult = new ExecMonitor<HashMap<String, Object>, Void>();
    final StringBuffer senderBuffer = new StringBuffer();
    final ExecMonitor<String, Void> acknowledgeResult = new ExecMonitor<String, Void>();
    MMX.EventListener messageListener = new MMX.EventListener() {
      public boolean onMessageReceived(MMXMessage message) {
        Log.d(TAG, "onMessageReceived(): " + message.getId());
        senderBuffer.append(message.getSender().getDisplayName());
        HashMap<String, Object> receivedContent = new HashMap<String, Object>();
        for (Map.Entry<String, String> entry : message.getContent().entrySet()) {
          receivedContent.put(entry.getKey(), entry.getValue());
        }
        receivedResult.invoked(receivedContent);
        //do the acknowledgement
        message.acknowledge(null);
        return false;
      }

      public boolean onMessageAcknowledgementReceived(MMXUser from, String messageId) {
        acknowledgeResult.invoked(messageId);
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
    if (status == ExecMonitor.Status.WAITING) {
      fail("testSendMessage() message.send timed out");
    }
    assertEquals(messageId, sendResult.getReturnValue());
    
    // Check if the receive is success
    status = receivedResult.waitFor(10000);
    if (status == ExecMonitor.Status.WAITING) {
      fail("testSendMessage() receive msg timed out");
    }
    assertEquals("bar", receivedResult.getReturnValue().get("foo"));
    assertEquals(MMX.getCurrentUser().getDisplayName(), senderBuffer.toString());

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
  
  private Set<MMXUser> mInvalidUsers;
  private MMXMessage.FailureCode mFailureCode;

  public void testSendMessageError() {
    MMX.OnFinishedListener<Void> loginLogoutListener = getLoginLogoutListener();
    String suffix = String.valueOf(System.currentTimeMillis());
    String username = USERNAME_PREFIX + suffix;
    String displayName = DISPLAY_NAME_PREFIX + suffix;
    String noSuchUser = NO_SUCH_USERNAME_PREFIX;
    String wrongUser = WRONG_USERNAME_PREFIX;
    registerUser(username, displayName, PASSWORD);

    mInvalidUsers = null;
    mFailureCode = null;
    
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
    final ExecMonitor<Boolean, Void> receivedResult = new ExecMonitor<Boolean, Void>();
    MMX.EventListener messageListener = new MMX.EventListener() {
      @Override
      public boolean onMessageReceived(MMXMessage message) {
        Log.d(TAG, "onMessageReceived(): " + message.getId());
        receivedResult.invoked(Boolean.TRUE);
        return false;
      }
      @Override
      public boolean onMessageAcknowledgementReceived(MMXUser from, String messageId) {
        receivedResult.invoked(Boolean.TRUE);
        return false;
      }
    };
    MMX.registerListener(messageListener);

    HashSet<MMXUser> recipients = new HashSet<MMXUser>();
    MMXUser badRecipient1 = new MMXUser.Builder().username(noSuchUser).build();
    recipients.add(badRecipient1);
    MMXUser badRecipient2 = new MMXUser.Builder().username(wrongUser).build();
    recipients.add(badRecipient2);

    HashMap<String, String> content = new HashMap<String, String>();
    content.put("foo", "bar");
    MMXMessage message = new MMXMessage.Builder()
            .recipients(recipients)
            .content(content)
            .build();
    final ExecMonitor<Boolean, Boolean> sendResult = new ExecMonitor<Boolean, Boolean>();
    final String messageId = message.send(new MMXMessage.OnFinishedListener<String>() {
      public void onSuccess(String result) {
        Log.e(TAG, "@@@ unexpected send success; msgId="+result);
        sendResult.invoked(Boolean.TRUE);
      }

      public void onFailure(MMXMessage.FailureCode code, Throwable ex) {
        mFailureCode = code;
        mInvalidUsers = ((MMXUser.InvalidUserException) ex).getUsers();
        Log.d(TAG, "@@@ code="+mFailureCode+", invalid users="+mInvalidUsers);
        sendResult.failed(Boolean.TRUE);
      }
    });
    
    sendResult.waitFor(10000);

    // Send failed because an error of invalid recipient.
    assertNull(sendResult.getReturnValue());
    assertNotNull(sendResult.getFailedValue());
    assertTrue(sendResult.getFailedValue());
    assertEquals(MMXMessage.FailureCode.INVALID_RECIPIENT, mFailureCode);
    assertTrue(mInvalidUsers.contains(badRecipient1));
    assertTrue(mInvalidUsers.contains(badRecipient2));
    
    // Make sure that no msg received.
    ExecMonitor.Status status = receivedResult.waitFor(5000);
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
}
