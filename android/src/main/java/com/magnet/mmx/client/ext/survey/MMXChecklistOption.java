/**
 * Copyright (c) 2012-2016 Magnet Systems. All rights reserved.
 */
package com.magnet.mmx.client.ext.survey;

import com.magnet.mmx.client.internal.survey.model.SurveyChoiceResult;
import com.magnet.mmx.client.internal.survey.model.SurveyOption;

public class MMXCheckListOption extends SurveyQuestionOption<Boolean> {
  public static final String TYPE = "MMXPollOption2";

  public MMXCheckListOption(String text) {
    super(text);
  }

  public static MMXCheckListOption fromSurveyOption(String pollId, SurveyOption surveyOption, SurveyChoiceResult surveyChoiceResult) {
    MMXCheckListOption pollOption = new MMXCheckListOption(surveyOption.getValue());
    pollOption.setOptionId(surveyOption.getOptionId());
    if(null != surveyChoiceResult) {
      //pollOption.setAnswer(surveyChoiceResult.getCount());
    }
    pollOption.setPollId(pollId);

    return pollOption;
  }
}
