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
import com.magnet.mmx.client.ext.poll.MMXPoll;
import com.magnet.mmx.client.ext.poll.MMXPollOption;
import com.magnet.mmx.client.utils.ChannelHelper;
import com.magnet.mmx.client.utils.ExecMonitor;
import com.magnet.mmx.client.utils.FailureDescription;
import com.magnet.mmx.client.utils.MaxHelper;
import com.magnet.mmx.client.utils.MessageHelper;
import com.magnet.mmx.client.utils.TestCaseTimer;
import com.magnet.mmx.client.utils.TestConstants;
import com.magnet.mmx.client.utils.UserHelper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(AndroidJUnit4.class)
public class MMXPollSingleSessionTest {
  private static final String TAG = MMXPollSingleSessionTest.class.getSimpleName();

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
  public void testPoll() {
    assertTrue(MMX.getMMXClient().isConnected());

    MMX.start();
    final List<MMXMessage> messagesReceived = new ArrayList<>();
    final ExecMonitor<MMXMessage, FailureDescription> newPollMessageMonitor = new ExecMonitor<>("NewPollMessageMonitor");
    MMX.EventListener messageListener = new MMX.EventListener() {
      public boolean onMessageReceived(final MMXMessage messageReceived) {
        Log.d(TAG, "------testPoll()--------onMessageReceived(): \n" + messageReceived);

        if(null != messageReceived.getContentType()) {
          if(messageReceived.getContentType().endsWith(MMXPoll.MMXPollIdentifier.TYPE)) {
            newPollMessageMonitor.invoked(messageReceived);
          }
        }

        return false;
      }

      @Override
      public boolean onMessageSendError(String messageId,
          MMXMessage.FailureCode code, String text) {
        Log.d(TAG, "onMessageSendError(): msgId="+messageId+", code="+code+", text="+text);
        //if(null != receiveMonitor)  receiveMonitor.failed(new FailureDescription(code, new Exception(text)));
        return false;
      }

      @Override
      public boolean onMessageAcknowledgementReceived(User from, String messageId) {
        return false;
      }
    };
    MMX.registerListener(messageListener);

    MMXChannel channel = createChannel();

    List<MMXPollOption> pollOptions = new ArrayList<>(4);
    pollOptions.add(new MMXPollOption("Red"));
    pollOptions.add(new MMXPollOption("Black"));
    pollOptions.add(new MMXPollOption("Orange"));
    pollOptions.add(new MMXPollOption("Blue"));
    final ExecMonitor<MMXPoll, FailureDescription> createPollResult = new ExecMonitor<>("CreatePoll");
    String question = "What's your favorite color ?";
    MMXPoll.create(channel, "Test Poll for channel " + channel.getName(),
        question, pollOptions, null, false, new MMX.OnFinishedListener<MMXPoll>() {
          @Override public void onSuccess(MMXPoll result) {
            createPollResult.invoked(result);
          }

          @Override public void onFailure(MMX.FailureCode code, Throwable ex) {
            createPollResult.failed(new FailureDescription(code, ex));
          }
        });

    // Create poll
    ExecMonitor.Status createPollStatus = createPollResult.waitFor(TestConstants.TIMEOUT_IN_MILISEC);
    assertThat(createPollResult.getFailedValue()).isNull();
    assertEquals(ExecMonitor.Status.INVOKED, createPollStatus);
    assertEquals(question, createPollResult.getReturnValue().getQuestion());

    MMXPoll newPoll = createPollResult.getReturnValue();

    // Received new poll
    ExecMonitor.Status newPollMessageStatus = newPollMessageMonitor.waitFor(TestConstants.TIMEOUT_IN_MILISEC);
    assertThat(newPollMessageMonitor.getFailedValue()).isNull();
    assertEquals(ExecMonitor.Status.INVOKED, newPollMessageStatus);
    assertEquals(newPoll.getPollId(), ((MMXPoll.MMXPollIdentifier) newPollMessageMonitor.getReturnValue().getPayload()).getPollId());

    // Retrieve Poll by id
    final ExecMonitor<MMXPoll, FailureDescription> retrievePollResult = new ExecMonitor<>("RetrievePollResult");
    MMXPoll.getPoll(newPoll.getPollId(), new MMX.OnFinishedListener<MMXPoll>() {
      @Override public void onSuccess(MMXPoll result) {
        retrievePollResult.invoked(result);
      }

      @Override public void onFailure(MMX.FailureCode code, Throwable ex) {
        retrievePollResult.failed(new FailureDescription(code, ex));
      }
    });
    ExecMonitor.Status retrievePollStatus = retrievePollResult.waitFor(TestConstants.TIMEOUT_IN_MILISEC);
    assertThat(retrievePollResult.getFailedValue()).isNull();
    assertEquals(ExecMonitor.Status.INVOKED, retrievePollStatus);
    assertEquals(question, retrievePollResult.getReturnValue().getQuestion());

    MMXPoll retrievedPoll = retrievePollResult.getReturnValue();

    // Choose option
    final ExecMonitor<Boolean, FailureDescription> chooseOptionResult = new ExecMonitor<>("ChooseOptionResult");
    newPoll.choose(retrievedPoll.getOptions().get(0), new MMX.OnFinishedListener<Boolean>() {
      @Override public void onSuccess(Boolean result) {
        chooseOptionResult.invoked(result);
      }

      @Override public void onFailure(MMX.FailureCode code, Throwable ex) {
        chooseOptionResult.failed(new FailureDescription(code, ex));
      }
    });
    ExecMonitor.Status chooseOptionStatus = chooseOptionResult.waitFor(TestConstants.TIMEOUT_IN_MILISEC);
    assertThat(chooseOptionResult.getFailedValue()).isNull();
    assertEquals(ExecMonitor.Status.INVOKED, chooseOptionStatus);
    assertEquals(Boolean.TRUE, chooseOptionResult.getReturnValue());

    MMXPoll.deletePoll(retrievedPoll.getPollId(), null);
    ChannelHelper.delete(channel);
  }

  private MMXChannel createChannel() {
    String suffix = String.valueOf(System.currentTimeMillis());

    User user2 = UserHelper.registerUser(UserHelper.MMX_TEST_USER_2 + suffix, UserHelper.MMX_TEST_USER_2, UserHelper.MMX_TEST_USER_2, null, false);

    //helpLogin(MMX_TEST_USER_1);

    Set<User> subscribers = new HashSet<>();
    subscribers.add(user2);

    String channelName = "Chat_channel_" + suffix;
    String channelSummary = channelName + " Summary";
    return ChannelHelper.create(channelName, channelSummary, false, subscribers);
  }
}
