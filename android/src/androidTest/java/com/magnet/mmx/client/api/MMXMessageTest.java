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
import com.magnet.max.android.User;
import com.magnet.mmx.client.common.Log;
import com.magnet.mmx.client.utils.ExecMonitor;
import com.magnet.mmx.client.utils.MaxHelper;
import com.magnet.mmx.client.utils.TestCaseTimer;
import com.magnet.mmx.client.utils.TestConstants;
import com.magnet.mmx.client.utils.UserHelper;
import java.util.HashMap;
import java.util.HashSet;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.fail;

@RunWith(AndroidJUnit4.class)
public class MMXMessageTest {
  private static final String TAG = MMXMessageTest.class.getSimpleName();

  @Rule
  public TestCaseTimer testCaseTimer = new TestCaseTimer();

  @BeforeClass
  public static void setUp() {
    MaxHelper.initMax();
  }

  @AfterClass
  public static void tearDown() {
    UserHelper.logout();;
  }

  @Test
  public void testFailureCodes() {
    assertEquals(MMXMessage.FailureCode.BAD_REQUEST, MMX.FailureCode.BAD_REQUEST);
    assertEquals(MMXMessage.FailureCode.BAD_REQUEST, 
        MMXMessage.FailureCode.fromMMXFailureCode(MMX.FailureCode.BAD_REQUEST, null));
  }

  @Test
  public void testNullContentMessage() {
    testEmptyMessageHelper(null);
  }

  @Test
  public void testEmptyMessage() {
    testEmptyMessageHelper(new HashMap<String, String>());
  }

  private void testEmptyMessageHelper(HashMap<String,String> content) {
    HashSet<User> recipients = new HashSet<User>();
    recipients.add(UserHelper.getInvalidUser("foo"));
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
        Log.e(TAG, "testEmptyMessageHelper.onFailure", throwable);
        failureMonitor.failed(code);
      }
    });
    ExecMonitor.Status status = failureMonitor.waitFor(TestConstants.TIMEOUT_IN_MILISEC);
    if (status == ExecMonitor.Status.INVOKED) {
      fail("should have called onFailure()");
    } else if (status == ExecMonitor.Status.FAILED) {
      assertEquals(MMXMessage.FailureCode.CONTENT_EMPTY, failureMonitor.getFailedValue());
    } else {
      fail("message.send() timed out");
    }
  }

  @Test
  public void testSendBeforeLogin() {
    HashSet<User> recipients = new HashSet<User>();
    recipients.add(UserHelper.getInvalidUser("foo"));
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
    ExecMonitor.Status status = failureMonitor.waitFor(TestConstants.TIMEOUT_IN_MILISEC);
    if (status == ExecMonitor.Status.INVOKED) {
      fail("should have called onFailure()");
    } else if (status == ExecMonitor.Status.FAILED) {
      assertEquals(MMX.FailureCode.BAD_REQUEST, failureMonitor.getFailedValue());
    } else {
      fail("message.send() timed out");
    }

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
}
