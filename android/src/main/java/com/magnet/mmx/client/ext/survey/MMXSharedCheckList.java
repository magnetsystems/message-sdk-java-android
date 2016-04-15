/**
 * Copyright (c) 2012-2016 Magnet Systems. All rights reserved.
 */
package com.magnet.mmx.client.ext.survey;

import com.magnet.max.android.User;
import com.magnet.mmx.client.api.MMX;
import com.magnet.mmx.client.api.MMXChannel;
import com.magnet.mmx.client.common.Log;
import com.magnet.mmx.client.internal.survey.model.Survey;
import com.magnet.mmx.client.internal.survey.model.SurveyAnswer;
import com.magnet.mmx.client.internal.survey.model.SurveyChoiceResult;
import com.magnet.mmx.client.internal.survey.model.SurveyDefinition;
import com.magnet.mmx.client.internal.survey.model.SurveyOption;
import com.magnet.mmx.client.internal.survey.model.SurveyParticipantModel;
import com.magnet.mmx.client.internal.survey.model.SurveyQuestionType;
import com.magnet.mmx.client.internal.survey.model.SurveyResults;
import com.magnet.mmx.client.internal.survey.model.SurveyType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class MMXSharedCheckList extends SurveyQuestion<MMXSharedCheckList, MMXCheckListOption> {
  public static final String TYPE = "MMXCheckList";

  private static final String TAG = "MMXCheckList";

  /**
   * Get the Poll by id
   * @param id
   * @param listener
   */
  public static void get(String id, final MMX.OnFinishedListener<MMXSharedCheckList> listener) {
    getById(id, new MMX.OnFinishedListener<SurveyResults>() {
      @Override public void onSuccess(SurveyResults result) {
        if (null != listener) {
          if(null != result) {
            listener.onSuccess(
                updatePollFromSurvey(null, result.getSurvey(), result.getSummary(), result.getMyAnswers()));
          } else {
            handleError(MMXChannel.FailureCode.GENERIC_FAILURE, new Exception("Result is null"));
          }
        }
      }

      @Override public void onFailure(MMX.FailureCode code, Throwable ex) {
        handleError(MMXChannel.FailureCode.GENERIC_FAILURE, ex);
      }

      private void handleError(MMXChannel.FailureCode code, Throwable throwable) {
        if (null != listener) {
          listener.onFailure(code, throwable);
        }
      }
    });
  }

  public MMXSharedCheckList(String name, String question,
      List<MMXCheckListOption> options, Date endDate, boolean hideResultsFromOthers, Map<String, String> metaData) {
    this(null, name, User.getCurrentUserId(), question, options, endDate, hideResultsFromOthers, null,
        metaData, null, null, SurveyQuestionType.MULTI_CHOICE);
  }

  protected MMXSharedCheckList(String pollId, String name, String ownerId, String question,
      List<MMXCheckListOption> options, Date endDate, boolean hideResultsFromOthers,
      MMXCheckListOption myVote, Map<String, String> metaData,
      String questionId, String channelIdentifier, SurveyQuestionType questionType) {
    super(pollId, name, ownerId, question, options, endDate, hideResultsFromOthers, myVote,
        metaData, questionId, channelIdentifier, questionType);
  }

  @Override String getSurveyObjectType() {
    return TYPE;
  }

  @Override void updateFromSurvey(Survey survey) {
    updatePollFromSurvey(this, survey, null, null);
  }

  //@Override MMXSharedCheckList fromSurvey(Survey survey, List<SurveyChoiceResult> results, List<SurveyAnswer> myAnswers) {
  //  MMXSharedCheckList newPoll = null;
  //
  //  updatePollFromSurvey(newPoll, survey, results, myAnswers);
  //
  //  return newPoll;
  //}

  @Override Survey toSurvey() {
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

  private static MMXSharedCheckList updatePollFromSurvey(MMXSharedCheckList poll, Survey survey, List<SurveyChoiceResult> results, List<SurveyAnswer> myAnswers) {
    if(null == survey || null == survey.getSurveyDefinition() || null == survey.getSurveyDefinition().getQuestions()
        || survey.getSurveyDefinition().getQuestions().isEmpty()) {
      return poll;
    }

    if(survey.getSurveyDefinition().getQuestions().size() > 1) {
      Log.w(TAG, "Only one question is supported");
    }

    if(null == poll) {
      // Convert a existing one from server
      String myAnswerOptionId = null;
      int myAnswerOptionIndex = -1;
      if(null != myAnswers && !myAnswers.isEmpty()) {
        myAnswerOptionId = myAnswers.get(0).getSelectedOptionId();
      }

      List<MMXCheckListOption> pollOptions = new ArrayList<>(survey.getSurveyDefinition().getQuestions().get(0).getChoices().size());
      for(int i = 0; i < survey.getSurveyDefinition().getQuestions().get(0).getChoices().size(); i++) {
        pollOptions.add(MMXCheckListOption.fromSurveyOption(
            survey.getId(), survey.getSurveyDefinition().getQuestions().get(0).getChoices().get(i),
            null != results ? results.get(i) : null
        ));
        if(null != myAnswerOptionId && myAnswerOptionIndex == -1 && myAnswerOptionId.equals(pollOptions.get(i).getOptionId())) {
          myAnswerOptionIndex = i;
        }
      }

      poll = new MMXSharedCheckList(survey.getId(), survey.getName(), survey.getOwners().get(0),
          survey.getSurveyDefinition().getQuestions().get(0).getText(), pollOptions,
          survey.getSurveyDefinition().getEndDate(),
          SurveyParticipantModel.PRIVATE == survey.getSurveyDefinition().getResultAccessModel() ? true : false,
          null, survey.getMetaData(), survey.getSurveyDefinition().getQuestions().get(0).getQuestionId(),
          survey.getSurveyDefinition().getNotificationChannelId(),
          survey.getSurveyDefinition().getQuestions().get(0).getType());

      if(myAnswerOptionIndex > -1) {
        poll.setMyVote(pollOptions.get(myAnswerOptionIndex));
      }
    } else {
      // Update local for ids
      poll.pollId = survey.getId();
      for(int i = 0; i < survey.getSurveyDefinition().getQuestions().get(0).getChoices().size(); i++) {
        MMXCheckListOption option = poll.getOptions().get(i);
        option.setPollId(survey.getId());
        option.setOptionId(survey.getSurveyDefinition().getQuestions().get(0).getChoices().get(i).getOptionId());
      }
    }

    poll.questionId = survey.getSurveyDefinition().getQuestions().get(0).getQuestionId();
    poll.channelIdentifier = survey.getSurveyDefinition().getNotificationChannelId();

    return poll;
  }
}
