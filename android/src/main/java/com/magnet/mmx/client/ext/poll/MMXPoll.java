/**
 * Copyright (c) 2012-2016 Magnet Systems. All rights reserved.
 */
package com.magnet.mmx.client.ext.poll;

import com.magnet.max.android.MaxCore;
import com.magnet.max.android.User;
import com.magnet.mmx.client.api.MMX;
import com.magnet.mmx.client.api.MMXChannel;
import com.magnet.mmx.client.api.MMXMessage;
import com.magnet.mmx.client.api.MMXTypedPayload;
import com.magnet.mmx.client.common.Log;
import com.magnet.mmx.client.internal.poll.SurveyService;
import com.magnet.mmx.client.internal.poll.model.Survey;
import com.magnet.mmx.client.internal.poll.model.SurveyAnswer;
import com.magnet.mmx.client.internal.poll.model.SurveyAnswerRequest;
import com.magnet.mmx.client.internal.poll.model.SurveyChoiceResult;
import com.magnet.mmx.client.internal.poll.model.SurveyDefinition;
import com.magnet.mmx.client.internal.poll.model.SurveyOption;
import com.magnet.mmx.client.internal.poll.model.SurveyParticipantModel;
import com.magnet.mmx.client.internal.poll.model.SurveyQuestion;
import com.magnet.mmx.client.internal.poll.model.SurveyQuestionType;
import com.magnet.mmx.client.internal.poll.model.SurveyResults;
import com.magnet.mmx.client.internal.poll.model.SurveyType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import retrofit.Callback;
import retrofit.Response;

public class MMXPoll implements MMXTypedPayload {
  public static final String TYPE = "MMXPoll";

  private static final String TAG = "MMXPoll";

  private String pollId;
  private String name;
  private String question;
  private List<MMXPollOption> options;
  private Date endDate;
  private boolean hideResultsFromOthers;
  private MMXPollOption myVote;
  //Internal use only
  private String questionId;

  private MMXChannel channel;

  public static void create(final MMXChannel channel, final String name, final String question, final List<MMXPollOption> options, final Date endDate,
      final boolean hideResultsFromOthers, final MMX.OnFinishedListener<MMXPoll> listener) {
    if(null == channel) {
      handleParameterError("Channel is required", listener);
      return;
    }
    if(null == options || options.isEmpty()) {
      handleParameterError("options is required", listener);
      return;
    }

    Survey newSurvey = createSurvey(channel, name, question, options, endDate, hideResultsFromOthers);
    getPollService().createSurvey(newSurvey,
        new Callback<Survey>() {
          @Override public void onResponse(Response<Survey> response) {
            Survey surveyCreated = response.body();

            for(int i = 0; i < options.size(); i++) {
              options.get(i).setOptionId(surveyCreated.getSurveyDefinition().getQuestions().get(0).getChoices().get(i).getOptionId());
              options.get(i).setPollId(surveyCreated.getId());
            }
            final MMXPoll newPoll = new MMXPoll(surveyCreated.getId(), channel, name, question, options, endDate, hideResultsFromOthers);
            newPoll.setQuestionId(surveyCreated.getSurveyDefinition().getQuestions().get(0).getQuestionId());

            MMXMessage
                message = new MMXMessage.Builder().channel(channel).payload(new MMXPollIdentifier(newPoll.getPollId())).build();
            publishChannelMessage(channel, message, new MMXChannel.OnFinishedListener<String>() {
              @Override public void onSuccess(String result) {
                if(null != listener) {
                  listener.onSuccess(newPoll);
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

  public static void getPoll(String pollId, final MMX.OnFinishedListener<MMXPoll> listener) {
    getPollService().getResults(pollId,
        new Callback<SurveyResults>() {
          @Override public void onResponse(Response<SurveyResults> response) {
            if(response.isSuccess()) {
              if (null != listener) {
                SurveyResults surveyResults = response.body();
                listener.onSuccess(fromSurvey(surveyResults.getSurvey(), surveyResults.getSummary()));
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

  public static void deletePoll(String pollId, final MMX.OnFinishedListener<Boolean> listener) {
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

  public void choose(final MMXPollOption option, final MMX.OnFinishedListener<Boolean> listener) {
    getPollService().submitSurveyAnswers(this.pollId,
        new SurveyAnswerRequest.SurveyAnswerRequestBuilder().answers(Arrays.asList(new SurveyAnswer.SurveyAnswerBuilder().questionId(questionId).selectedOptionId(option.getOptionId()).build())).build(),
        new Callback<Void>() {
      @Override public void onResponse(Response<Void> response) {
        if(response.isSuccess()) {
          MMXMessage message = new MMXMessage.Builder().channel(channel).payload(option).build();
          publishChannelMessage(channel, message, new MMXChannel.OnFinishedListener<String>() {
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

  private MMXPoll(String id, MMXChannel channel, String name, String question, List<MMXPollOption> options, Date endDate,
      boolean hideResultsFromOthers) {
    this.pollId = id;
    this.channel = channel;
    this.name = name;
    this.question = question;
    this.options = options;
    this.endDate = endDate;
    this.hideResultsFromOthers = hideResultsFromOthers;
  }

  public String getPollId() {
    return pollId;
  }

  public String getQuestion() {
    return question;
  }

  public List<MMXPollOption> getOptions() {
    return options;
  }

  public Date getEndDate() {
    return endDate;
  }

  public boolean isHideResultsFromOthers() {
    return hideResultsFromOthers;
  }

  public MMXPollOption getMyVote() {
    return myVote;
  }

  /** Package */ String getQuestionId() {
    return questionId;
  }

  /** Package */ void setQuestionId(String questionId) {
    this.questionId = questionId;
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

  private static void publishChannelMessage(MMXChannel channel, MMXMessage message, MMXChannel.OnFinishedListener<String> listener) {
    channel.publish(message, listener);
  }

  private static MMXPoll fromSurvey(Survey survey, List<SurveyChoiceResult> results) {
    if(null == survey || null == survey.getSurveyDefinition() || null == survey.getSurveyDefinition().getQuestions()
        || survey.getSurveyDefinition().getQuestions().isEmpty()) {
      return null;
    }

    if(survey.getSurveyDefinition().getQuestions().size() > 1) {
      Log.w(TAG, "Only one question is supported");
    }

    List<MMXPollOption> pollOptions = new ArrayList<>(survey.getSurveyDefinition().getQuestions().get(0).getChoices().size());
    for(int i = 0; i < survey.getSurveyDefinition().getQuestions().get(0).getChoices().size(); i++) {
      SurveyOption so = survey.getSurveyDefinition().getQuestions().get(0).getChoices().get(i);
      MMXPollOption pollOption = new MMXPollOption(so.getValue());
      pollOption.setOptionId(so.getOptionId());
      if(null != results && null != results.get(i)) {
        pollOption.setCount(results.get(i).getCount());
      }
      pollOptions.add(pollOption);
    }

    MMXPoll newPoll = new MMXPoll(survey.getId(), null, null, survey.getSurveyDefinition().getQuestions().get(0).getText(),
        pollOptions, survey.getSurveyDefinition().getEndDate(),
        SurveyParticipantModel.PRIVATE == survey.getSurveyDefinition().getResultAccessModel() ? true : false);
    newPoll.setQuestionId(survey.getSurveyDefinition().getQuestions().get(0).getQuestionId());

    return newPoll;
  }

  private static Survey createSurvey(final MMXChannel channel, final String name, final String question, final List<MMXPollOption> options, final Date endDate,
      final boolean hideResultsFromOthers) {
    List<SurveyQuestion> surveyQuestions = new ArrayList<>(1);
    List<SurveyOption> surveyOptions = new ArrayList<>(options.size());
    for(int i = 0; i < options.size(); i++) {
      surveyOptions.add(new SurveyOption.SurveyOptionBuilder().value(options.get(i).getText()).displayOrder(i).build());
    }
    surveyQuestions.add(new SurveyQuestion.SurveyQuestionBuilder().text(question)
        .displayOrder(0)
        .type(SurveyQuestionType.SINGLE_CHOICE)
        .choices(surveyOptions).build());

    return new Survey.SurveyBuilder().owners(Arrays.asList(User.getCurrentUserId()))
        .surveyDefinition(new SurveyDefinition.SurveyDefinitionBuilder().endDate(endDate)
            .notificationChannelId(channel.getName())
            //.participantModel(SurveyParticipantModel.PRIVATE)
            .resultAccessModel(hideResultsFromOthers ? SurveyParticipantModel.PRIVATE : SurveyParticipantModel.PUBLIC)
            .questions(surveyQuestions)
            .type(SurveyType.POLL)
            .build())
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
}
