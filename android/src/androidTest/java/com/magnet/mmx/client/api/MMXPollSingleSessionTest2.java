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
import com.magnet.mmx.client.ext.survey.MMXPoll;
import com.magnet.mmx.client.ext.survey.MMXPollOption;
import com.magnet.mmx.client.ext.survey.ObjectIdentifier;
import com.magnet.mmx.client.utils.ChannelHelper;
import com.magnet.mmx.client.utils.ExecMonitor;
import com.magnet.mmx.client.utils.FailureDescription;
import com.magnet.mmx.client.utils.MaxHelper;
import com.magnet.mmx.client.utils.PollHelper2;
import com.magnet.mmx.client.utils.TestCaseTimer;
import com.magnet.mmx.client.utils.TestConstants;
import com.magnet.mmx.client.utils.UserHelper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

@RunWith(AndroidJUnit4.class)
public class MMXPollSingleSessionTest2 {
  private static final String TAG = MMXPollSingleSessionTest2.class.getSimpleName();

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

    final ExecMonitor<MMXMessage, FailureDescription> newPollMessageMonitor = new ExecMonitor<>("NewPollMessageMonitor");
    final ExecMonitor<MMXMessage, FailureDescription> newPollChosenMessageMonitor = new ExecMonitor<>("NewPollChosenMessageMonitor");
    MMX.EventListener messageListener = new MMX.EventListener() {
      public boolean onMessageReceived(final MMXMessage messageReceived) {
        Log.d(TAG, "------testPoll()--------onMessageReceived(): \n" + messageReceived);

        if(null != messageReceived.getContentType()) {
          if(messageReceived.getContentType().endsWith(ObjectIdentifier.TYPE)) {
            ObjectIdentifier objectIdentifier = (ObjectIdentifier) messageReceived.getPayload();
            if(MMXPoll.TYPE.equals(objectIdentifier.getObjectType())) {
              newPollMessageMonitor.invoked(messageReceived);
            }
          } else if(messageReceived.getContentType().endsWith(MMXPollOption.TYPE)) {
            newPollChosenMessageMonitor.invoked(messageReceived);
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

    // Create poll
    List<MMXPollOption> pollOptions = new ArrayList<>(4);
    pollOptions.add(new MMXPollOption("Red"));
    pollOptions.add(new MMXPollOption("Black"));
    pollOptions.add(new MMXPollOption("Orange"));
    pollOptions.add(new MMXPollOption("Blue"));
    String question = "What's your favorite color ?";
    String name = "Test Poll for channel " + channel.getName();
    Map<String, String> metaData = new HashMap<>();
    metaData.put("key1", "value1");
    metaData.put("key2", "value2");
    MMXPoll newPoll = new MMXPoll(name, question, pollOptions, null, false, metaData);

    final ExecMonitor<Void, FailureDescription> createPollResult = new ExecMonitor<>("CreatePoll");
    newPoll.publish(channel, new MMX.OnFinishedListener<Void>() {
          @Override public void onSuccess(Void result) {
            createPollResult.invoked(result);
          }

          @Override public void onFailure(MMX.FailureCode code, Throwable ex) {
            createPollResult.failed(new FailureDescription(code, ex));
          }
        });
    ExecMonitor.Status createPollStatus = createPollResult.waitFor(TestConstants.TIMEOUT_IN_MILISEC);
    assertThat(createPollResult.getFailedValue()).isNull();
    assertEquals(ExecMonitor.Status.INVOKED, createPollStatus);

    assertThat(newPoll.getPollId()).isNotNull();
    for(MMXPollOption option : newPoll.getOptions() ) {
      assertThat(option.getPollId()).isNotNull();
      assertThat(option.getOptionId()).isNotNull();
    }
    assertThat(newPoll.getName()).isEqualTo(name);
    assertThat(newPoll.getQuestion()).isEqualTo(question);
    assertThat(newPoll.getMyVote()).isNull();
    assertThat(newPoll.getOwnerId()).isEqualTo(User.getCurrentUserId());

    // Received new poll identifier
    ExecMonitor.Status newPollMessageStatus = newPollMessageMonitor.waitFor(TestConstants.TIMEOUT_IN_MILISEC);
    assertThat(newPollMessageMonitor.getFailedValue()).isNull();
    assertEquals(ExecMonitor.Status.INVOKED, newPollMessageStatus);
    assertEquals(newPoll.getPollId(), ((ObjectIdentifier) newPollMessageMonitor.getReturnValue().getPayload()).getObjectId());

    // Retrieve Poll by id
    MMXPoll retrievedPoll = PollHelper2.getPollById(newPoll.getPollId());
    assertPolls(retrievedPoll, newPoll);
    assertThat(retrievedPoll.getOptions().get(0).getAnswer()).isEqualTo(0);
    assertThat(retrievedPoll.getMyVote()).isNull();

    // Choose first option
    MMXPoll pollAfterChosenFirst = chooseOptions(retrievedPoll, 0, newPollChosenMessageMonitor);

    // Choose second option
    MMXPoll pollAfterChosenSecond = chooseOptions(retrievedPoll, 1, newPollChosenMessageMonitor);
    assertThat(pollAfterChosenSecond.getOptions().get(0).getAnswer()).isEqualTo(0);

    MMXPoll.delete(retrievedPoll.getPollId(), null);
    ChannelHelper.delete(channel);
  }

  private void assertPolls(MMXPoll poll1, MMXPoll poll2) {
    assertThat(poll2).isEqualTo(poll1); // pollId
    assertThat(poll2.getName()).isEqualTo(poll1.getName());
    assertThat(poll2.getOwnerId()).isEqualTo(poll1.getOwnerId());
    assertThat(poll2.getOptions()).containsExactly(poll1.getOptions().toArray(new MMXPollOption[]{}));
    assertThat(poll2.getMetaData()).containsOnly(entry("key1", "value1"), entry("key2", "value2"));
  }

  private MMXPoll chooseOptions(MMXPoll poll, int optionIndex, ExecMonitor<MMXMessage, FailureDescription> newPollChosenMessageMonitor) {
    newPollChosenMessageMonitor.reset(null, null);

    // Choose first option
    PollHelper2.vote(poll, optionIndex);

    // Retrieve Poll by id again
    MMXPoll retrievedPollAfterVote = PollHelper2.getPollById(poll.getPollId());
    assertPolls(retrievedPollAfterVote, poll);

    assertThat(retrievedPollAfterVote.getOptions().get(optionIndex).getAnswer()).isEqualTo(1);
    assertThat(retrievedPollAfterVote.getMyVote()).isNotNull();
    assertThat(retrievedPollAfterVote.getMyVote()).isEqualTo(retrievedPollAfterVote.getOptions().get(optionIndex));

    // Received new message
    ExecMonitor.Status newPollMessageStatus = newPollChosenMessageMonitor.waitFor(TestConstants.TIMEOUT_IN_MILISEC);
    assertThat(newPollChosenMessageMonitor.getFailedValue()).isNull();
    assertEquals(ExecMonitor.Status.INVOKED, newPollMessageStatus);
    assertEquals(poll.getOptions().get(optionIndex), newPollChosenMessageMonitor.getReturnValue().getPayload());

    return retrievedPollAfterVote;
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
