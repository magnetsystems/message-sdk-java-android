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
import com.magnet.mmx.client.ext.poll.MMXPoll;
import com.magnet.mmx.client.ext.poll.MMXPollOption;
import com.magnet.mmx.client.utils.ChannelHelper;
import com.magnet.mmx.client.utils.ExecMonitor;
import com.magnet.mmx.client.utils.MaxHelper;
import com.magnet.mmx.client.utils.PollHelper;
import com.magnet.mmx.client.utils.TestCaseTimer;
import com.magnet.mmx.client.utils.TestConstants;
import com.magnet.mmx.client.utils.UserHelper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.assertj.core.data.MapEntry;
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
public class MMXPollSingleSessionTest {
  private static final String TAG = MMXPollSingleSessionTest.class.getSimpleName();

  @Rule
  public TestCaseTimer testCaseTimer = new TestCaseTimer();

  private static PollMessageReceiver pollMessageReceiver;

  private static MMXChannel channel;

  @BeforeClass
  public static void setUp() {
    MaxHelper.initMax();

    UserHelper.registerAndLogin(UserHelper.MMX_TEST_USER_1, UserHelper.MMX_TEST_USER_1);

    pollMessageReceiver = new PollMessageReceiver();
    pollMessageReceiver.start();

    channel = createChannel();
  }

  @AfterClass
  public static void tearDown() {
    UserHelper.logout();

    pollMessageReceiver.stop();
    ChannelHelper.delete(channel);
  }

  @Test
  public void testSingleChoicePoll() {
    assertTrue(MMX.getMMXClient().isConnected());

    // Create poll
    String question = "What's your favorite color ?";
    String name = "Test Single Choice Poll for channel " + channel.getName();

    MMXPoll newPoll = createAndPublishPoll(question, name, false);

    // Received new poll identifier
    MMXPoll retrievedPoll = receivePoll(newPoll);

    ExecMonitor<MMXMessage, ApiError> newPollChosenMessageMonitor = pollMessageReceiver.getNewPollChosenMessageMonitor();
    // Choose first option
    MMXPoll pollAfterChosenFirst = chooseOptions(retrievedPoll, newPollChosenMessageMonitor, 0);

    // Choose second option
    MMXPoll pollAfterChosenSecond = chooseOptions(retrievedPoll, newPollChosenMessageMonitor, 1);
    assertThat(pollAfterChosenSecond.getOptions().get(0).getCount()).isEqualTo(0);

    // Choose all options failed
    final ExecMonitor<MMXMessage, ApiError> chooseAllResult = new ExecMonitor<>("ChooseAll");
    retrievedPoll.choose(retrievedPoll.getOptions(), new ApiCallback<MMXMessage>() {
      @Override public void success(MMXMessage result) {
        chooseAllResult.invoked(result);
      }

      @Override public void failure(ApiError apiError) {
        chooseAllResult.failed(apiError);
      }
    });
    ExecMonitor.Status chooseAllStatus = chooseAllResult.waitFor(TestConstants.TIMEOUT_IN_MILISEC);
    assertThat(chooseAllResult).isNotNull();
    assertEquals(ExecMonitor.Status.FAILED, chooseAllStatus);
    assertThat(chooseAllResult.getFailedValue().getMessage()).isEqualTo("Only one option is allowed");

    //retrievedPoll.delete(null);
  }

  @Test
  public void testMultiChoicePoll() {
    assertTrue(MMX.getMMXClient().isConnected());

    // Create poll
    String question = "What's your favorite colors ?";
    String name = "Test MultiChoices Poll for channel " + channel.getName();

    MMXPoll newPoll = createAndPublishPoll(question, name, true);

    // Received new poll identifier
    MMXPoll retrievedPoll = receivePoll(newPoll);

    ExecMonitor<MMXMessage, ApiError> newPollChosenMessageMonitor = pollMessageReceiver.getNewPollChosenMessageMonitor();
    // Choose first option
    MMXPoll pollAfterChosenFirst = chooseOptions(retrievedPoll, newPollChosenMessageMonitor, 0);

    // Choose second and third option
    MMXPoll pollAfterChosenSecond = chooseOptions(retrievedPoll, newPollChosenMessageMonitor, 1, 2);
    assertThat(pollAfterChosenSecond.getOptions().get(0).getCount()).isEqualTo(0);

    //retrievedPoll.delete(null);
  }

  private MMXPoll createAndPublishPoll(String question, String name, boolean allowMultiChoices) {
    // Create poll
    Map<String, String> optionMeta = new HashMap<>();
    optionMeta.put("imageUrl", "image1");
    MMXPoll newPoll = new MMXPoll.Builder().name(name).question(question).allowMultiChoice(allowMultiChoices)
        .hideResultsFromOthers(false)
        .option("Red")
        .option("Black")
        .option("Orange")
        .option(new MMXPollOption("Blue", optionMeta))
        .extra("key1", "value1")
        .extra("key2", "value2").build();

    final ExecMonitor<MMXMessage, ApiError> publishPollResult = new ExecMonitor<>("PublishPoll");
    newPoll.publish(channel, new ApiCallback<MMXMessage>() {
      @Override public void success(MMXMessage result) {
        publishPollResult.invoked(result);
      }

      @Override public void failure(ApiError apiError) {
        publishPollResult.failed(apiError);
      }
    });
    ExecMonitor.Status createPollStatus = publishPollResult.waitFor(TestConstants.TIMEOUT_IN_MILISEC);
    assertThat(publishPollResult.getFailedValue()).isNull();
    assertThat(publishPollResult.getReturnValue()).isNotNull();
    assertEquals(ExecMonitor.Status.INVOKED, createPollStatus);

    assertThat(newPoll.getPollId()).isNotNull();
    assertThat(newPoll.getName()).isEqualTo(name);
    assertThat(newPoll.getQuestion()).isEqualTo(question);
    assertThat(newPoll.getMyVotes()).isNull();
    assertThat(newPoll.getOwnerId()).isEqualTo(User.getCurrentUserId());

    return newPoll;
  }

  private MMXPoll receivePoll(MMXPoll pollSent) {
    // Received new poll identifier
    ExecMonitor<MMXMessage, ApiError> newPollMessageMonitor = pollMessageReceiver.getNewPollMessageMonitor();

    ExecMonitor.Status newPollMessageStatus = newPollMessageMonitor.waitFor(TestConstants.TIMEOUT_IN_MILISEC);
    assertThat(newPollMessageMonitor.getFailedValue()).isNull();
    assertEquals(ExecMonitor.Status.INVOKED, newPollMessageStatus);
    assertEquals(pollSent.getPollId(), ((MMXPoll.MMXPollIdentifier) newPollMessageMonitor.getReturnValue().getPayload()).getPollId());

    // Retrieve Poll by id
    MMXPoll retrievedPoll = PollHelper.getPollById(pollSent.getPollId());
    assertPolls(retrievedPoll, pollSent);
    assertThat(retrievedPoll.getOptions().get(0).getCount()).isEqualTo(0);
    assertThat(retrievedPoll.getMyVotes()).isNull();

    newPollMessageMonitor.reset(null, null);

    return retrievedPoll;
  }

  private void assertPolls(MMXPoll poll1, MMXPoll poll2) {
    assertThat(poll2).isEqualTo(poll1); // pollId
    assertThat(poll2.getName()).isEqualTo(poll1.getName());
    assertThat(poll2.isAllowMultiChoices()).isEqualTo(poll1.isAllowMultiChoices());
    assertThat(poll2.getOwnerId()).isEqualTo(poll1.getOwnerId());
    assertThat(poll2.getOptions()).containsExactly(poll1.getOptions().toArray(new MMXPollOption[]{}));
    for(int i = 0; i< poll2.getOptions().size(); i++) {
      if(poll2.getOptions().get(i).getText().equals("Blue")) {
        MapEntry entry = entry("imageUrl", "image1");
        assertThat(poll2.getOptions().get(i).getExtras()).containsExactly(entry);
        assertThat(poll1.getOptions().get(i).getExtras()).containsExactly(entry);
      } else {
        assertThat(poll2.getOptions().get(i).getExtras()).isNull();
        assertThat(poll1.getOptions().get(i).getExtras()).isNull();
      }
    }
    assertThat(poll2.getExtras()).containsOnly(entry("key1", "value1"), entry("key2", "value2"));
  }

  private MMXPoll chooseOptions(MMXPoll poll, ExecMonitor<MMXMessage, ApiError> newPollChosenMessageMonitor,
      int... optionIndex) {
    List<MMXPollOption> options = new ArrayList<>();
    for(int i : optionIndex) {
      options.add(poll.getOptions().get(i));
    }

    PollHelper.vote(poll, options);

    // Retrieve Poll by id again
    MMXPoll retrievedPollAfterVote = PollHelper.getPollById(poll.getPollId());
    assertPolls(retrievedPollAfterVote, poll);

    assertThat(retrievedPollAfterVote.getMyVotes()).isNotNull();

    for(int i : optionIndex) {
      assertThat(retrievedPollAfterVote.getOptions().get(i).getCount()).isEqualTo(1);
    }

    assertThat(retrievedPollAfterVote.getMyVotes()).containsExactly(options.toArray(new MMXPollOption[] {}));

    // Received new message
    ExecMonitor.Status newPollMessageStatus = newPollChosenMessageMonitor.waitFor(TestConstants.TIMEOUT_IN_MILISEC);
    assertThat(newPollChosenMessageMonitor.getFailedValue()).isNull();
    assertEquals(ExecMonitor.Status.INVOKED, newPollMessageStatus);
    assertThat(((MMXPoll.MMXPollAnswer) newPollChosenMessageMonitor.getReturnValue().getPayload()).getCurrentSelection()).containsExactly(options.toArray(new MMXPollOption[] {}));
    //assertThat(((MMXPoll.MMXPollAnswer) newPollChosenMessageMonitor.getReturnValue().getPayload()).getPreviousSelection()).isEmpty();

    newPollChosenMessageMonitor.reset(null, null);

    return retrievedPollAfterVote;
  }

  private static MMXChannel createChannel() {
    String suffix = String.valueOf(System.currentTimeMillis());

    User user2 = UserHelper.registerUser(UserHelper.MMX_TEST_USER_2 + suffix, UserHelper.MMX_TEST_USER_2, UserHelper.MMX_TEST_USER_2, null, false);

    //helpLogin(MMX_TEST_USER_1);

    Set<User> subscribers = new HashSet<>();
    subscribers.add(user2);

    String channelName = "Chat_channel_" + suffix;
    String channelSummary = channelName + " Summary";
    return ChannelHelper.create(channelName, channelSummary, false, subscribers);
  }

  private static class PollMessageReceiver {
    private final ExecMonitor<MMXMessage, ApiError> newPollMessageMonitor;
    private final ExecMonitor<MMXMessage, ApiError> newPollChosenMessageMonitor;
    private final MMX.EventListener messageListener;

    public PollMessageReceiver() {
      newPollMessageMonitor = new ExecMonitor<>("NewPollMessageMonitor");
      newPollChosenMessageMonitor = new ExecMonitor<>("NewPollChosenMessageMonitor");

      messageListener = new MMX.EventListener() {
        public boolean onMessageReceived(final MMXMessage messageReceived) {
          Log.d(TAG, "------testSingleChoicePoll()--------onMessageReceived(): \n" + messageReceived);

          if (null != messageReceived.getContentType()) {
            if (messageReceived.getContentType().endsWith(MMXPoll.MMXPollIdentifier.TYPE)) {
              newPollMessageMonitor.invoked(messageReceived);
            } else if (messageReceived.getContentType().endsWith(MMXPoll.MMXPollAnswer.TYPE)) {
              newPollChosenMessageMonitor.invoked(messageReceived);
            }
          }

          return false;
        }

        @Override
        public boolean onMessageSendError(String messageId, ApiError apiError, String text) {
          Log.d(TAG,
              "onMessageSendError(): msgId=" + messageId + ", apiError=" + apiError + ", text=" + text);
          //if(null != receiveMonitor)  receiveMonitor.failed(new ApiError(code, new Exception(text)));
          return false;
        }

        @Override public boolean onMessageAcknowledgementReceived(User from, String messageId) {
          return false;
        }
      };
    }

    public void start() {
      MMX.start();
      MMX.registerListener(messageListener);
    }

    public void stop() {
      MMX.unregisterListener(messageListener);
    }

    public ExecMonitor<MMXMessage, ApiError> getNewPollMessageMonitor() {
      return newPollMessageMonitor;
    }

    public ExecMonitor<MMXMessage, ApiError> getNewPollChosenMessageMonitor() {
      return newPollChosenMessageMonitor;
    }
  }
}
