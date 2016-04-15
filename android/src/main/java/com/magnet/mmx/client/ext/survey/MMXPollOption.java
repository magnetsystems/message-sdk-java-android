/**
 * Copyright (c) 2012-2016 Magnet Systems. All rights reserved.
 */
package com.magnet.mmx.client.ext.survey;

import com.magnet.mmx.client.internal.survey.model.SurveyChoiceResult;
import com.magnet.mmx.client.internal.survey.model.SurveyOption;

public class MMXPollOption extends SurveyQuestionOption<Long> {
  public static final String TYPE = "MMXPollOption2";

  public MMXPollOption(String text) {
    super(text);
  }

  public static MMXPollOption fromSurveyOption(String pollId, SurveyOption surveyOption, SurveyChoiceResult surveyChoiceResult) {
    MMXPollOption pollOption = new MMXPollOption(surveyOption.getValue());
    pollOption.setOptionId(surveyOption.getOptionId());
    if(null != surveyChoiceResult) {
      pollOption.setAnswer(surveyChoiceResult.getCount());
    }
    pollOption.setPollId(pollId);

    return pollOption;
  }
}
