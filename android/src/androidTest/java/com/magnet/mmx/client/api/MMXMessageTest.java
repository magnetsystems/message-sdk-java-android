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
import com.magnet.max.android.ApiCallback;
import com.magnet.max.android.ApiError;
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
    final ExecMonitor<String, ApiError> failureMonitor = new ExecMonitor<String, ApiError>();
    message.send(new ApiCallback<String>() {
      @Override
      public void success(String result) {
        failureMonitor.invoked(result);
      }

      @Override
      public void failure(ApiError apiError) {
        Log.e(TAG, "testEmptyMessageHelper.failure", apiError);
        failureMonitor.failed(apiError);
      }
    });
    ExecMonitor.Status status = failureMonitor.waitFor(TestConstants.TIMEOUT_IN_MILISEC);
    if (status == ExecMonitor.Status.INVOKED) {
      fail("should have called failure()");
    } else if (status == ExecMonitor.Status.FAILED) {
      assertEquals(MMXMessage.CONTENT_EMPTY, failureMonitor.getFailedValue());
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
    final ExecMonitor<String,ApiError> failureMonitor = new ExecMonitor<String,ApiError>();
    message.send(new ApiCallback<String>() {
      @Override
      public void success(String result) {
        failureMonitor.invoked(result);
      }

      @Override
      public void failure(ApiError apiError) {
        Log.e(TAG, "testSendBeforeLogin.failure", apiError);
        failureMonitor.failed(apiError);
      }
    });
    ExecMonitor.Status status = failureMonitor.waitFor(TestConstants.TIMEOUT_IN_MILISEC);
    if (status == ExecMonitor.Status.INVOKED) {
      fail("should have called failure()");
    } else if (status == ExecMonitor.Status.FAILED) {
      assertEquals(MMX.BAD_REQUEST_CODE, failureMonitor.getFailedValue().getKind());
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
//      public void success(String result) {
//        failureMonitor.invoked(result);
//      }
//
//      @Override
//      public void failure(MMXChannel.FailureCode code, Throwable throwable) {
//        com.magnet.mmx.client.common.Log.e(TAG, "testPublishBeforeLogin.failure", throwable);
//        failureMonitor.failed(code);
//      }
//    });
//    ExecMonitor.Status status = failureMonitor.waitFor(10000);
//    if (status == ExecMonitor.Status.INVOKED) {
//      fail("should have called failure()");
//    } else if (status == ExecMonitor.Status.FAILED) {
//      assertEquals(MMX.FailureCode.BAD_REQUEST, failureMonitor.getFailedValue());
//    } else {
//      fail("channel.publish() timed out");
//    }
//  }
}
