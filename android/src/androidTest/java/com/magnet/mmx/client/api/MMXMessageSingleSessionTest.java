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

import android.support.test.runner.AndroidJUnit4;
import com.magnet.max.android.Attachment;
import com.magnet.max.android.MaxCore;
import com.magnet.max.android.User;
import com.magnet.mmx.client.api.MMXMessage.InvalidRecipientException;
import com.magnet.mmx.client.common.Log;
import com.magnet.mmx.client.utils.ExecMonitor;
import com.magnet.mmx.client.utils.FailureDescription;
import com.magnet.mmx.client.utils.MaxHelper;
import com.magnet.mmx.client.utils.MessageHelper;
import com.magnet.mmx.client.utils.TestCaseTimer;
import com.magnet.mmx.client.utils.TestConstants;
import com.magnet.mmx.client.utils.UserHelper;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;

@RunWith(AndroidJUnit4.class)
public class MMXMessageSingleSessionTest {
  private static final String TAG = MMXMessageSingleSessionTest.class.getSimpleName();

  @Rule
  public TestCaseTimer testCaseTimer = new TestCaseTimer();

  @BeforeClass
  public static void setUp() {
    MaxHelper.initMax();

    UserHelper.registerAndLogin(UserHelper.MMX_TEST_USER_1, UserHelper.MMX_TEST_USER_1);
  }

  @AfterClass
  public static void tearDown() {
    UserHelper.logout();
  }

  @Test
  public void testSendMessage() {
    assertTrue(MMX.getMMXClient().isConnected());

    HashSet<User> recipients = new HashSet<User>();
    recipients.add(MMX.getCurrentUser());

    HashMap<String, String> content = new HashMap<String, String>();
    content.put("foo", "bar");
    content.put("testCase", "testSendMessage");

    MMXMessage message = new MMXMessage.Builder()
            .recipients(recipients)
            .content(content)
            .build();

    ExecMonitor<String, FailureDescription> sendResult = new ExecMonitor<>("SendMessage");
    ExecMonitor<MMXMessage, FailureDescription> receivedResult = new ExecMonitor<>("MessageReceive");
    ExecMonitor<Boolean, FailureDescription> ackSend = new ExecMonitor<>("SendACK");
    ExecMonitor<String, Void> ackResult = new ExecMonitor<>("ACKReceive");

    MessageHelper.sendMessage(message, sendResult, ExecMonitor.Status.INVOKED,
        receivedResult, ExecMonitor.Status.INVOKED, ackSend, ackResult);

    // Verify received message
    MMXMessage receivedMessage = receivedResult.getReturnValue();
    assertNotNull(receivedMessage);
    HashMap<String, Object> receivedContent = new HashMap<String, Object>();
    for (Map.Entry<String, String> entry : receivedMessage.getContent().entrySet()) {
      receivedContent.put(entry.getKey(), entry.getValue());
    }
    assertEquals("bar", receivedContent.get("foo"));
  }

  @Test
  public void testSendMessageWithAttachments() {
    assertTrue(MMX.getMMXClient().isConnected());

    HashSet<User> recipients = new HashSet<User>();
    recipients.add(MMX.getCurrentUser());

    HashMap<String, String> content = new HashMap<String, String>();
    content.put("foo", "bar");
    content.put("testCase", "testSendMessageWithAttachments");
    final Attachment attachment1 = new Attachment(MaxCore.getApplicationContext().getResources().openRawResource(
        com.magnet.mmx.test.R.raw.test_image), "image/jpeg");
    //final Attachment attachment1 = new TextAttachment(Attachment.TEXT_PLAIN, "hello world");
    //assertEquals(-1, attachment1.getLength());
    MMXMessage message = new MMXMessage.Builder()
        .recipients(recipients)
        .content(content)
        .attachments(attachment1)
        .build();

    ExecMonitor<String, FailureDescription> sendResult = new ExecMonitor<>("SendMessage");
    ExecMonitor<MMXMessage, FailureDescription> receivedResult = new ExecMonitor<>("MessageReceive");
    ExecMonitor<Boolean, FailureDescription> ackSend = new ExecMonitor<>("SendACK");
    ExecMonitor<String, Void> ackResult = new ExecMonitor<>("ACKReceive");

    MessageHelper.sendMessage(message, sendResult, ExecMonitor.Status.INVOKED,
        receivedResult, ExecMonitor.Status.INVOKED, ackSend, ackResult);

    // Verify received message
    MMXMessage receivedMessage = receivedResult.getReturnValue();
    assertTrue(attachment1.getLength() > 0);
    assertEquals("bar", receivedMessage.getContent().get("foo"));
    //Attachments
    assertNotNull(receivedMessage.getAttachments());
    assertEquals(1, receivedMessage.getAttachments().size());

    final Attachment attachmentReceived = message.getAttachments().get(0);
    assertEquals("image/jpeg", attachmentReceived.getMimeType());
    //assertEquals(Attachment.Status.INIT, attachmentReceived.getStatus());
    //assertEquals(attachmentSize.get(), attachmentReceived.getLength());
    assertNotNull(attachmentReceived.getDownloadUrl());


    // Download attachment
    final CountDownLatch downLatch = new CountDownLatch(1);
    Log.d(TAG, "-----------attachment received : " + attachmentReceived.getDownloadUrl());
    attachmentReceived.download(new Attachment.DownloadAsBytesListener() {

      @Override public void onComplete(byte[] bytes) {
        assertNotNull(bytes);
        assertTrue(bytes.length > 0);
        assertEquals(attachmentReceived.getLength(), bytes.length);
        downLatch.countDown();
      }

      @Override public void onError(Throwable throwable) {
        fail(throwable.getMessage());
      }
    });
    try {
      downLatch.await(TestConstants.TIMEOUT_IN_MILISEC, TimeUnit.MILLISECONDS);
    } catch (InterruptedException e) {
      fail(e.getMessage());
    }
    assertEquals(0, downLatch.getCount());
    assertEquals(Attachment.Status.COMPLETE, attachmentReceived.getStatus());
  }

//  public void testPublishBeforeLogin() {
//    MMXChannel channel = new MMXChannel.Builder()
//            .name("foo").summary("bar").build();
//    HashMap<String,String> content = new HashMap<String,String>();
//    content.put("foo", "bar");
//    final ExecMonitor<String,MMXChannel.FailureCode> failureMonitor = new ExecMonitor<String,MMXChannel.FailureCode>();
//    channel.publish(content, new MMXChannel.OnFinishedListener<String>() {
//      @Override
//      public void onSuccess(String result) {
//        failureMonitor.invoked(result);
//      }
//
//      @Override
//      public void onFailure(MMXChannel.FailureCode code, Throwable throwable) {
//        com.magnet.mmx.client.common.Log.e(TAG, "testPublishBeforeLogin.onFailure", throwable);
//        failureMonitor.failed(code);
//      }
//    });
//    ExecMonitor.Status status = failureMonitor.waitFor(10000);
//    if (status == ExecMonitor.Status.INVOKED) {
//      fail("should have called onFailure()");
//    } else if (status == ExecMonitor.Status.FAILED) {
//      assertEquals(MMX.FailureCode.BAD_REQUEST, failureMonitor.getFailedValue());
//    } else {
//      fail("channel.publish() timed out");
//    }
//  }

  @Test
  public void testSendUCastMessageError() {
    assertTrue(MMX.getMMXClient().isConnected());

    User fooUser = UserHelper.getInvalidUser("foo");
    HashSet<User> recipients = new HashSet<User>();
    recipients.add(fooUser);
    
    HashMap<String, String> content = new HashMap<String, String>();
    content.put("foo", "bar");
    content.put("testCase", "testSendUCastMessageError");
    MMXMessage message = new MMXMessage.Builder()
            .recipients(recipients)
            .content(content)
            .build();

    ExecMonitor<String, FailureDescription> sendResult = new ExecMonitor<>("SendMessage");
    ExecMonitor<MMXMessage, FailureDescription> receivedResult = new ExecMonitor<>("MessageReceive");

    MessageHelper.sendMessage(message, sendResult, ExecMonitor.Status.FAILED, receivedResult, ExecMonitor.Status.WAITING, null, null);
    MMXMessage.FailureCode sendFailure = sendResult.getFailedValue().getCode();
    assertEquals(MMXMessage.FailureCode.INVALID_RECIPIENT, sendFailure);

    InvalidRecipientException irEx = (InvalidRecipientException) sendResult.getFailedValue().getException();
    Set<String> invalidUsers = irEx.getUserIds();
    assertEquals(1, invalidUsers.size());
    assertTrue(invalidUsers.contains(fooUser.getUserIdentifier()));
  }

  @Test
  public void testSendMCastMessageError() {
    String noSuchUser = UserHelper.NO_SUCH_USERNAME_PREFIX;
    String wrongUser = UserHelper.WRONG_USERNAME_PREFIX;

    HashSet<User> recipients = new HashSet<User>();
    User badRecipient1 = UserHelper.getInvalidUser(noSuchUser);
    recipients.add(badRecipient1);
    User badRecipient2 = UserHelper.getInvalidUser(wrongUser);
    recipients.add(badRecipient2);
    User goodRecipient = MMX.getCurrentUser();
    recipients.add(goodRecipient);

    HashMap<String, String> content = new HashMap<String, String>();
    content.put("foo", "bar");
    content.put("testCase", "testSendMCastMessageError");
    MMXMessage message = new MMXMessage.Builder()
            .recipients(recipients)
            .content(content)
            .build();

    ExecMonitor<String, FailureDescription> sendResult = new ExecMonitor<>("SendMessage");
    ExecMonitor<MMXMessage, FailureDescription> receivedResult = new ExecMonitor<>("MessageReceive");

    MessageHelper.sendMessage(message, sendResult, ExecMonitor.Status.FAILED, receivedResult, ExecMonitor.Status.INVOKED, null, null);
    MMXMessage.FailureCode sendFailure = sendResult.getFailedValue().getCode();
    assertEquals(MMXMessage.FailureCode.INVALID_RECIPIENT, sendFailure);

    InvalidRecipientException irEx = (InvalidRecipientException) sendResult.getFailedValue().getException();
    Set<String> invalidUsers = irEx.getUserIds();
    assertEquals(2, invalidUsers.size());
    assertTrue(invalidUsers.contains(badRecipient1.getUserIdentifier()));
    assertTrue(invalidUsers.contains(badRecipient2.getUserIdentifier()));
  }
}
