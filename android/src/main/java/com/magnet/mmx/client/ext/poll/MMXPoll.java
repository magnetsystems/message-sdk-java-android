/**
 * Copyright (c) 2012-2016 Magnet Systems. All rights reserved.
 */
package com.magnet.mmx.client.ext.poll;

import com.magnet.max.android.MaxCore;
import com.magnet.max.android.User;
import com.magnet.max.android.util.EqualityUtil;
import com.magnet.max.android.util.HashCodeBuilder;
import com.magnet.max.android.util.StringUtil;
import com.magnet.mmx.client.api.MMX;
import com.magnet.mmx.client.api.MMXChannel;
import com.magnet.mmx.client.api.MMXMessage;
import com.magnet.mmx.client.api.MMXTypedPayload;
import com.magnet.mmx.client.common.Log;
import com.magnet.mmx.client.internal.survey.SurveyService;
import com.magnet.mmx.client.internal.survey.model.Survey;
import com.magnet.mmx.client.internal.survey.model.SurveyAnswer;
import com.magnet.mmx.client.internal.survey.model.SurveyAnswerRequest;
import com.magnet.mmx.client.internal.survey.model.SurveyChoiceResult;
import com.magnet.mmx.client.internal.survey.model.SurveyDefinition;
import com.magnet.mmx.client.internal.survey.model.SurveyOption;
import com.magnet.mmx.client.internal.survey.model.SurveyParticipantModel;
import com.magnet.mmx.client.internal.survey.model.SurveyQuestion;
import com.magnet.mmx.client.internal.survey.model.SurveyQuestionType;
import com.magnet.mmx.client.internal.survey.model.SurveyResults;
import com.magnet.mmx.client.internal.survey.model.SurveyType;
import com.magnet.mmx.protocol.MMXChannelId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import retrofit.Callback;
import retrofit.Response;

/**
 * The class defines a poll which is published and voted in a MMXChannel
 */
public class MMXPoll implements MMXTypedPayload {
  public static final String TYPE = "MMXPoll";

  private static final String TAG = "MMXPoll";

  private String pollId;
  private String name;
  private String ownerId;
  private String question;
  private List<MMXPollOption> options;
  private Date endDate;
  private boolean hideResultsFromOthers;
  private List<MMXPollOption> myVote;
  private Map<String, String> metaData;
  private boolean allowMultiChoice;

  //Internal use only
  private String questionId;
  private String channelIdentifier;
  private MMXChannel channel;

  protected MMXPoll() {
  }

  public void publish(final MMXChannel channel, final MMX.OnFinishedListener<Void> listener) {
    if(null == channel) {
      handleParameterError("Channel is required", listener);
      return;
    }
    if(StringUtil.isEmpty(question)) {
      handleParameterError("question is required", listener);
      return;
    }
    if(null == options || options.isEmpty()) {
      handleParameterError("options is required", listener);
      return;
    }

    this.channel = channel;
    Survey newSurvey = createSurvey(this);
    getPollService().createSurvey(newSurvey,
        new Callback<Survey>() {
          @Override public void onResponse(Response<Survey> response) {
            Survey surveyCreated = response.body();

            // Update ids generated by server
            pollId = surveyCreated.getId();
            for(int i = 0; i < options.size(); i++) {
              options.get(i).setOptionId(surveyCreated.getSurveyDefinition().getQuestions().get(0).getChoices().get(i).getOptionId());
              options.get(i).setPollId(surveyCreated.getId());
            }
            questionId = surveyCreated.getSurveyDefinition().getQuestions().get(0).getQuestionId();

            MMXMessage
                message = new MMXMessage.Builder().channel(channel).payload(new MMXPollIdentifier(pollId)).build();
            publishChannelMessage(message, new MMXChannel.OnFinishedListener<String>() {
              @Override public void onSuccess(String result) {
                if(null != listener) {
                  listener.onSuccess(null);
                }
              }

              @Override public void onFailure(MMXChannel.FailureCode code, Throwable throwable) {
                Log.e(TAG, "Failed to publish poll to channel due to " + code, throwable);
                if(null != listener) {
                  listener.onFailure(code, throwable);
                }
              }
            });
          }

          @Override public void onFailure(Throwable throwable) {
            Log.e(TAG, "Failed to create survey", throwable);
            if(null != listener) {
              listener.onFailure(MMXChannel.FailureCode.GENERIC_FAILURE, throwable);
            }
          }
        }).executeInBackground();
  }

  /**
   * Get the Poll by id
   * @param pollId
   * @param listener
   */
  public static void get(String pollId, final MMX.OnFinishedListener<MMXPoll> listener) {
    if(StringUtil.isEmpty(pollId)) {
      handleParameterError("pollId is required", listener);
      return;
    }
    getPollService().getResults(pollId,
        new Callback<SurveyResults>() {
          @Override public void onResponse(Response<SurveyResults> response) {
            if(response.isSuccess()) {
              if (null != listener) {
                SurveyResults surveyResults = response.body();
                listener.onSuccess(fromSurvey(surveyResults.getSurvey(), surveyResults.getSummary(), surveyResults.getMyAnswers()));
              }
            } else {
              handleError(MMXChannel.FailureCode.GENERIC_FAILURE, new Exception(response.message()));
            }
          }

          @Override public void onFailure(Throwable throwable) {
            handleError(MMXChannel.FailureCode.GENERIC_FAILURE, throwable);
          }

          private void handleError(MMXChannel.FailureCode code, Throwable throwable) {
            if (null != listener) {
              listener.onFailure(code, throwable);
            }
          }
        }).executeInBackground();
  }

  /**
   * Delete a poll by id
   * @param pollId
   * @param listener
   */
  public static void delete(String pollId, final MMX.OnFinishedListener<Void> listener) {
    if(StringUtil.isEmpty(pollId)) {
      handleParameterError("pollId is required", listener);
      return;
    }
    getPollService().deleteSurvey(pollId, new Callback<Void>() {
      @Override public void onResponse(Response<Void> response) {
        if (null != listener) {
          listener.onSuccess(null);
        }
      }

      @Override public void onFailure(Throwable throwable) {
        handleError(MMXChannel.FailureCode.GENERIC_FAILURE, throwable, listener);
      }
    }).executeInBackground();
  }

  /**
   * Choose a option for a poll and publish a message to the channel
   * @param option
   * @param listener
   */
  public void choose(final MMXPollOption option, final MMX.OnFinishedListener<Void> listener) {
    choose(Arrays.asList(option), listener);
  }

    /**
     * Choose a option for a poll and publish a message to the channel
     * @param chosenOptions
     * @param listener
     */
  public void choose(final List<MMXPollOption> chosenOptions, final MMX.OnFinishedListener<Void> listener) {
    if(null == chosenOptions && chosenOptions.isEmpty()) {
      handleParameterError("option is required", listener);
      return;
    }

    if(!allowMultiChoice && chosenOptions.size() > 1) {
      handleParameterError("Only one option is allowed", listener);
      return;
    }

    List<SurveyAnswer> answers = new ArrayList<>(chosenOptions.size());
    for(MMXPollOption option : chosenOptions) {
      answers.add(new SurveyAnswer.SurveyAnswerBuilder().questionId(questionId).selectedOptionId(option.getOptionId()).build());
    }

    getPollService().submitSurveyAnswers(this.pollId,
        new SurveyAnswerRequest.SurveyAnswerRequestBuilder().answers(answers).build(),
        new Callback<Void>() {
      @Override public void onResponse(Response<Void> response) {
        if(response.isSuccess()) {
          MMXMessage message = new MMXMessage.Builder().channel(channel).payload(new MMXPollResult(chosenOptions)).build();
          publishChannelMessage(message, new MMXChannel.OnFinishedListener<String>() {
            @Override public void onSuccess(String result) {
              if (null != listener) {
                listener.onSuccess(null);
              }
            }

            @Override public void onFailure(MMXChannel.FailureCode code, Throwable throwable) {
              handleError(code, throwable, listener);
            }
          });
        } else {
          Log.e(TAG, "Failed to choose option for poll due to " + response.message());
          handleError(MMXChannel.FailureCode.GENERIC_FAILURE, new Exception(response.message()), listener);
        }
      }

      @Override public void onFailure(Throwable throwable) {
        Log.e(TAG, "Failed to choose option for poll due to " + throwable.getMessage());
        handleError(MMXChannel.FailureCode.GENERIC_FAILURE, throwable, listener);
      }
    }).executeInBackground();
  }

  private MMXPoll(String id, MMXChannel channel, String name, String ownerId, String question, List<MMXPollOption> options, Date endDate,
      boolean hideResultsFromOthers, Map<String, String> metaData) {
    this.pollId = id;
    this.channel = channel;
    this.name = name;
    this.ownerId = ownerId;
    this.question = question;
    this.options = options;
    this.endDate = endDate;
    this.hideResultsFromOthers = hideResultsFromOthers;
    this.metaData = metaData;
  }

  /**
   * The id of the poll
   * @return
   */
  public String getPollId() {
    return pollId;
  }

  /**
   * The user id of the owner of the poll
   * @return
   */
  public String getOwnerId() {
    return ownerId;
  }

  /**
   * The name of the poll
   * @return
   */
  public String getName() {
    return name;
  }

  /**
   * The question of the poll
   * @return
   */
  public String getQuestion() {
    return question;
  }

  /**
   * The options of the poll
   * @return
   */
  public List<MMXPollOption> getOptions() {
    return options;
  }

  /**
   * The end date of the poll
   * @return
   */
  public Date getEndDate() {
    return endDate;
  }

  /**
   * Whether to hide result from others
   * @return
   */
  public boolean isHideResultsFromOthers() {
    return hideResultsFromOthers;
  }

  /**
   * The extra meta data of the poll in key-value pair
   * @return
   */
  public Map<String, String> getMetaData() {
    return metaData;
  }

  public boolean isAllowMultiChoice() {
    return allowMultiChoice;
  }

  /**
   * My vote
   * @return
   */
  public List<MMXPollOption> getMyVote() {
    return myVote;
  }

  @Override public boolean equals(Object o) {
    boolean quickCheckResult = EqualityUtil.quickCheck(this, o);
    if(!quickCheckResult) {
      return false;
    }

    MMXPoll theOther = (MMXPoll) o;
    return StringUtil.isStringValueEqual(this.getPollId(), theOther.getPollId());
  }

  @Override public int hashCode() {
    return new HashCodeBuilder().hash(pollId).hashCode();
  }

  @Override public String toString() {
    StringBuilder sb = new StringBuilder("MMXPoll { ").append("pollId = ").append(pollId)
        .append(", name = ").append(name)
        .append(", question = ").append(question)
        .append(", questionId = ").append(questionId)
        .append(", ownerId = ").append(ownerId)
        .append(", endDate = ").append(endDate)
        .append(", hideResultsFromOthers = ").append(hideResultsFromOthers)
        .append(", channel = ").append(getChannelIdentifier())
        .append(", myVote = ").append(myVote).append("\n");

    sb.append(", options = [\n");
    if(null != options) {
      for (int i = 0; i < options.size(); i++) {
        sb.append("    ").append(i).append(" = ").append(options.get(i)).append(", \n");
      }

    }
    sb.append("\n]");

    sb.append("}");

    return sb.toString();
  }

  /** Package */ String getQuestionId() {
    return questionId;
  }

  private String getChannelIdentifier() {
    return null != channelIdentifier ? channelIdentifier : (null != channel ? channel.getIdentifier() : null);
  }

  private static SurveyService getPollService() {
    return MaxCore.create(SurveyService.class);
  }

  private static void handleParameterError(String message, MMX.OnFinishedListener listener) {
    handleParameterError(new MMX.FailureCode(MMX.FailureCode.ILLEGAL_ARGUMENT_CODE, message), listener);
  }

  private static void handleParameterError(MMX.FailureCode failureCode, MMX.OnFinishedListener listener) {
    Log.e(TAG, failureCode.toString());
    if(null != listener) {
      listener.onFailure(failureCode, new Exception(failureCode.getDescription()));
    }
  }

  private static void handleError(MMXChannel.FailureCode code, Throwable throwable, MMX.OnFinishedListener listener) {
    if (null != listener) {
      listener.onFailure(code, throwable);
    }
  }

  private void publishChannelMessage(final MMXMessage message, final MMXChannel.OnFinishedListener<String> listener) {
    if(null != channel) {
      channel.publish(message, listener);
    } else if(StringUtil.isNotEmpty(channelIdentifier)) {
      final MMXChannelId channelId = MMXChannelId.parse(channelIdentifier);
      if(null != channelId) {
        MMXChannel.getChannel(channelId.getName(), StringUtil.isEmpty(channelId.getUserId()), new MMXChannel.OnFinishedListener<MMXChannel>() {
          @Override public void onSuccess(MMXChannel result) {
            channel = result;
            channel.publish(message, listener);
          }

          @Override public void onFailure(MMXChannel.FailureCode code, Throwable throwable) {
            handleSendChannelMessageError("Couldn't find channel by identifer " + channelId, throwable, listener);
          }
        });
      }
    } else {
      handleSendChannelMessageError("Both channel and channelIdentifier are null", null, listener);
    }
  }

  private void handleSendChannelMessageError(String errorMessage, Throwable throwable, MMXChannel.OnFinishedListener<String> listener) {
    if(null != listener) {
      listener.onFailure(MMXChannel.FailureCode.GENERIC_FAILURE, new Exception(errorMessage, throwable));
    }
    Log.e(TAG, "Failed to send message to channel due to " + errorMessage);
  }

  private static MMXPoll fromSurvey(Survey survey, List<SurveyChoiceResult> results, List<SurveyAnswer> myAnswers) {
    if(null == survey || null == survey.getSurveyDefinition() || null == survey.getSurveyDefinition().getQuestions()
        || survey.getSurveyDefinition().getQuestions().isEmpty()) {
      return null;
    }

    if(survey.getSurveyDefinition().getQuestions().size() > 1) {
      Log.w(TAG, "Only one question is supported");
    }

    Set<String> myAnswerOptionIds = new HashSet<>();
    if(null != myAnswers && !myAnswers.isEmpty()) {
      for(SurveyAnswer sa : myAnswers) {
        myAnswerOptionIds.add(sa.getSelectedOptionId());
      }
    }

    List<MMXPollOption> pollOptions = new ArrayList<>(survey.getSurveyDefinition().getQuestions().get(0).getChoices().size());
    List<MMXPollOption> myAnswerOptions = new ArrayList<>(myAnswerOptionIds.size());
    for(int i = 0; i < survey.getSurveyDefinition().getQuestions().get(0).getChoices().size(); i++) {
      pollOptions.add(MMXPollOption.fromSurveyOption(
          survey.getId(), survey.getSurveyDefinition().getQuestions().get(0).getChoices().get(i),
          null != results ? results.get(i) : null));
      if(myAnswerOptionIds.contains(pollOptions.get(i).getOptionId())) {
        myAnswerOptions.add(pollOptions.get(i));
      }
    }

    MMXPoll newPoll = new MMXPoll(survey.getId(), null, survey.getName(), survey.getOwners().get(0), survey.getSurveyDefinition().getQuestions().get(0).getText(),
        pollOptions, survey.getSurveyDefinition().getEndDate(),
        SurveyParticipantModel.PRIVATE == survey.getSurveyDefinition().getResultAccessModel() ? true : false,
        survey.getMetaData());
    newPoll.allowMultiChoice = survey.getSurveyDefinition().getQuestions().get(0).getType() == SurveyQuestionType.MULTI_CHOICE;
    newPoll.questionId = survey.getSurveyDefinition().getQuestions().get(0).getQuestionId();
    newPoll.channelIdentifier = survey.getSurveyDefinition().getNotificationChannelId();
    if(!myAnswerOptions.isEmpty()) {
      newPoll.myVote = myAnswerOptions;
    }

    return newPoll;
  }

  private static Survey createSurvey(final MMXPoll poll) {
    List<SurveyQuestion> surveyQuestions = new ArrayList<>(1);
    List<SurveyOption> surveyOptions = new ArrayList<>(poll.options.size());
    for(int i = 0; i < poll.options.size(); i++) {
      surveyOptions.add(new SurveyOption.SurveyOptionBuilder().value(poll.options.get(i).getText())
                            .metaData(poll.options.get(i).getMetaData()).displayOrder(i).build());
    }
    surveyQuestions.add(new SurveyQuestion.SurveyQuestionBuilder().text(poll.question)
        .displayOrder(0)
        .type(poll.allowMultiChoice ? SurveyQuestionType.MULTI_CHOICE : SurveyQuestionType.SINGLE_CHOICE)
        .choices(surveyOptions)
        .build());

    return new Survey.SurveyBuilder().name(poll.name).owners(Arrays.asList(User.getCurrentUserId()))
        .surveyDefinition(new SurveyDefinition.SurveyDefinitionBuilder().endDate(poll.endDate)
            .notificationChannelId(poll.channel.getIdentifier())
            //.participantModel(SurveyParticipantModel.PRIVATE)
            .resultAccessModel(poll.hideResultsFromOthers ? SurveyParticipantModel.PRIVATE : SurveyParticipantModel.PUBLIC)
            .questions(surveyQuestions)
            .type(SurveyType.POLL)
            .build())
        .metaData(poll.metaData)
        .build();
  }

  public static class MMXPollIdentifier implements MMXTypedPayload {
    public static final String TYPE = "MMXPollIdentifier";

    private String pollId;

    public MMXPollIdentifier(String pollId) {
      this.pollId = pollId;
    }

    public String getPollId() {
      return pollId;
    }
  }

  public static class MMXPollResult implements MMXTypedPayload {
    public static final String TYPE = "MMXPollResult";

    private List<MMXPollOption> result;

    public MMXPollResult(List<MMXPollOption> result) {
      this.result = result;
    }

    public List<MMXPollOption> getResult() {
      return result;
    }
  }

    public static class Builder {
    private MMXPoll toBuild;

    public Builder() {
      toBuild = new MMXPoll();
    }

    public Builder name(String name) {
      toBuild.name = name;
      return this;
    }

    public Builder question(String question) {
      toBuild.question = question;
      return this;
    }

    public Builder options(List<MMXPollOption> options) {
      toBuild.options = options;
      return this;
    }

    public Builder option(MMXPollOption option) {
      if(null == toBuild.options) {
        toBuild.options = new ArrayList<>();
      }
      toBuild.options.add(option);
      return this;
    }

    public Builder option(String option) {
      if(null == toBuild.options) {
        toBuild.options = new ArrayList<>();
      }
      toBuild.options.add(new MMXPollOption(option));
      return this;
    }

    public Builder endDate(Date endDate) {
      toBuild.endDate = endDate;
      return this;
    }

    public Builder hideResultsFromOthers(boolean hideResultsFromOthers) {
      toBuild.hideResultsFromOthers = hideResultsFromOthers;
      return this;
    }

    public Builder allowMultiChoice(boolean allowMultiChoice) {
      toBuild.allowMultiChoice = allowMultiChoice;
      return this;
    }

    public Builder metaData(Map<String, String> metaData) {
      toBuild.metaData = metaData;
      return this;
    }

    public Builder metaData(String key, String value) {
      if(null == toBuild.metaData) {
        toBuild.metaData = new HashMap<>();
      }
      toBuild.metaData.put(key, value);
      return this;
    }

    public MMXPoll build() {
      toBuild.ownerId = User.getCurrentUserId();

      return toBuild;
    }
  }
}
