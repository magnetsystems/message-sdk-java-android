/**
 * Copyright (c) 2012-2016 Magnet Systems. All rights reserved.
 */
package com.magnet.mmx.client.ext.survey;

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
import com.magnet.mmx.client.ext.poll.MMXPollOption;
import com.magnet.mmx.client.internal.survey.SurveyService;
import com.magnet.mmx.client.internal.survey.model.Survey;
import com.magnet.mmx.client.internal.survey.model.SurveyAnswer;
import com.magnet.mmx.client.internal.survey.model.SurveyAnswerRequest;
import com.magnet.mmx.client.internal.survey.model.SurveyChoiceResult;
import com.magnet.mmx.client.internal.survey.model.SurveyDefinition;
import com.magnet.mmx.client.internal.survey.model.SurveyOption;
import com.magnet.mmx.client.internal.survey.model.SurveyParticipantModel;
import com.magnet.mmx.client.internal.survey.model.SurveyQuestionType;
import com.magnet.mmx.client.internal.survey.model.SurveyResults;
import com.magnet.mmx.client.internal.survey.model.SurveyType;
import com.magnet.mmx.protocol.MMXChannelId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import retrofit.Callback;
import retrofit.Response;

public abstract class SurveyQuestion<Q extends SurveyQuestion, T extends SurveyQuestionOption, R> implements MMXTypedPayload {
  public static final String TYPE = "SurveyQuestion";

  private static final String TAG = "SurveyQuestion";

  protected String pollId;
  protected String name;
  protected String ownerId;
  protected String question;
  protected List<T> options;
  protected Date endDate;
  protected boolean hideResultsFromOthers;
  protected T myVote;
  protected Map<String, String> metaData;
  protected SurveyQuestionType questionType;

  //protected boolean allowComment;
  //protected String comment;

  //Internal use only
  protected String questionId;
  protected String channelIdentifier;
  protected MMXChannel channel;

  abstract String getSurveyObjectType();
  abstract void updateFromSurvey(Survey survey);
  //abstract Q fromSurvey(Survey survey, List<SurveyChoiceResult> results, List<SurveyAnswer> myAnswers);
  abstract Survey toSurvey();

  protected SurveyQuestion(String pollId, String name, String ownerId, String question,
      List<T> options, Date endDate, boolean hideResultsFromOthers, T myVote,
      Map<String, String> metaData, String questionId, String channelIdentifier,
      SurveyQuestionType questionType) {
    this.pollId = pollId;
    this.name = name;
    this.ownerId = ownerId;
    this.question = question;
    this.options = options;
    this.endDate = endDate;
    this.hideResultsFromOthers = hideResultsFromOthers;
    this.myVote = myVote;
    this.metaData = metaData;
    this.questionId = questionId;
    this.channelIdentifier = channelIdentifier;
    this.questionType = questionType;
  }

  public void publish(final MMXChannel channel, final MMX.OnFinishedListener<Void> listener) {
    if (null == channel) {
      handleParameterError("Channel is required", listener);
      return;
    }
    if (StringUtil.isEmpty(question)) {
      handleParameterError("question is required", listener);
      return;
    }
    if (null == options || options.isEmpty()) {
      handleParameterError("options is required", listener);
      return;
    }
    this.channel = channel;
    getPollService().createSurvey(toSurvey(),
        new Callback<Survey>() {
          @Override public void onResponse(Response<Survey> response) {
            Survey surveyCreated = response.body();

            updateFromSurvey(surveyCreated);

            MMXMessage
                message = new MMXMessage.Builder().channel(channel).payload(new ObjectIdentifier(getSurveyObjectType(), pollId)).build();
            channel.publish(message, new MMXChannel.OnFinishedListener<String>() {
              @Override public void onSuccess(String result) {
                if(null != listener) {
                  listener.onSuccess(null);
                }
              }

              @Override public void onFailure(MMXChannel.FailureCode code, Throwable throwable) {
                if(null != listener) {
                  listener.onFailure(code, throwable);
                }
              }
            });
          }

          @Override public void onFailure(Throwable throwable) {

          }
        }).executeInBackground();
  }

  /**
   * Delete a poll by id
   * @param pollId
   * @param listener
   */
  public static void delete(String pollId, final MMX.OnFinishedListener<Boolean> listener) {
    if(StringUtil.isEmpty(pollId)) {
      handleParameterError("pollId is required", listener);
      return;
    }
    getPollService().deleteSurvey(pollId, new Callback<Void>() {
      @Override public void onResponse(Response<Void> response) {
        if (null != listener) {
          listener.onSuccess(response.isSuccess());
        }
      }

      @Override public void onFailure(Throwable throwable) {

      }
    }).executeInBackground();
  }

  /**
   * Choose a option for a poll and publish a message to the channel
   * @param option
   * @param listener
   */
  protected void choose(final List<T> option, final MMX.OnFinishedListener<Boolean> listener) {
    if(null == option) {
      handleParameterError("option is required", listener);
      return;
    }
    getPollService().submitSurveyAnswers(this.pollId,
        new SurveyAnswerRequest.SurveyAnswerRequestBuilder().answers(Arrays.asList(new SurveyAnswer.SurveyAnswerBuilder().questionId(questionId).selectedOptionId(option.getOptionId()).build())).build(),
        new Callback<Void>() {
          @Override public void onResponse(Response<Void> response) {
            if(response.isSuccess()) {
              MMXMessage message = new MMXMessage.Builder().channel(channel).payload(option).build();
              publishChannelMessage(message, new MMXChannel.OnFinishedListener<String>() {
                @Override public void onSuccess(String result) {
                  if (null != listener) {
                    listener.onSuccess(true);
                  }
                }

                @Override public void onFailure(MMXChannel.FailureCode code, Throwable throwable) {
                  handleError(code, throwable);
                }
              });
            } else {
              Log.e(TAG, "Failed to choose option for poll due to " + response.message());
              handleError(MMXChannel.FailureCode.GENERIC_FAILURE, new Exception(response.message()));
            }
          }

          @Override public void onFailure(Throwable throwable) {
            Log.e(TAG, "Failed to choose option for poll due to " + throwable.getMessage());
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
   * Get the Poll by id
   * @param pollId
   * @param listener
   */
  protected static void getById(String pollId, final MMX.OnFinishedListener<SurveyResults> listener) {
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
                listener.onSuccess(response.body());
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
  public List<T> getOptions() {
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
   * Whether to hide answer from others
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

  /**
   * My vote
   * @return
   */
  public T getMyVote() {
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

  /** Package */ void setMyVote(T myVote) {
    this.myVote = myVote;
  }

  private String getChannelIdentifier() {
    return null != channelIdentifier ? channelIdentifier : (null != channel ? channel.getIdentifier() : null);
  }

  protected static SurveyService getPollService() {
    return MaxCore.create(SurveyService.class);
  }

  protected static void handleParameterError(String message, MMX.OnFinishedListener listener) {
    handleParameterError(new MMX.FailureCode(MMX.FailureCode.ILLEGAL_ARGUMENT_CODE, message), listener);
  }

  protected static void handleParameterError(MMX.FailureCode failureCode, MMX.OnFinishedListener listener) {
    Log.e(TAG, failureCode.toString());
    if(null != listener) {
      listener.onFailure(failureCode, new Exception(failureCode.getDescription()));
    }
  }

  protected void publishChannelMessage(final MMXMessage message, final MMXChannel.OnFinishedListener<String> listener) {
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

  protected void handleSendChannelMessageError(String errorMessage, Throwable throwable, MMXChannel.OnFinishedListener<String> listener) {
    if(null != listener) {
      listener.onFailure(MMXChannel.FailureCode.GENERIC_FAILURE, new Exception(errorMessage, throwable));
    }
    Log.e(TAG, "Failed to send message to channel due to " + errorMessage);
  }

  protected static Survey createSurvey(final MMXChannel channel, final String name, final String question, final List<MMXPollOption> options, final Date endDate,
      final boolean hideResultsFromOthers, final Map<String, String> metaData) {
    List<com.magnet.mmx.client.internal.survey.model.SurveyQuestion> surveyQuestions = new ArrayList<>(1);
    List<SurveyOption> surveyOptions = new ArrayList<>(options.size());
    for(int i = 0; i < options.size(); i++) {
      surveyOptions.add(new SurveyOption.SurveyOptionBuilder().value(options.get(i).getText()).displayOrder(i).build());
    }
    surveyQuestions.add(new com.magnet.mmx.client.internal.survey.model.SurveyQuestion.SurveyQuestionBuilder().text(question)
        .displayOrder(0)
        .type(SurveyQuestionType.SINGLE_CHOICE)
        .choices(surveyOptions).build());

    return new Survey.SurveyBuilder().name(name).owners(Arrays.asList(User.getCurrentUserId()))
        .surveyDefinition(new SurveyDefinition.SurveyDefinitionBuilder().endDate(endDate)
            .notificationChannelId(channel.getIdentifier())
            //.participantModel(SurveyParticipantModel.PRIVATE)
            .resultAccessModel(hideResultsFromOthers ? SurveyParticipantModel.PRIVATE : SurveyParticipantModel.PUBLIC)
            .questions(surveyQuestions)
            .type(SurveyType.POLL)
            .build())
        .metaData(metaData)
        .build();
  }
}
