/**
 * Copyright (c) 2012-2016 Magnet Systems. All rights reserved.
 */
package com.magnet.mmx.client.ext.survey;

import com.magnet.max.android.util.EqualityUtil;
import com.magnet.max.android.util.HashCodeBuilder;
import com.magnet.max.android.util.StringUtil;
import com.magnet.mmx.client.api.MMXTypedPayload;
import com.magnet.mmx.client.internal.survey.model.SurveyChoiceResult;
import com.magnet.mmx.client.internal.survey.model.SurveyOption;

public class SurveyQuestionOption<T> implements MMXTypedPayload {
  public static final String TYPE = "SurveyQuestionOption";

  protected String pollId;
  protected String optionId;
  protected String text;
  protected T answer;

  //private List<UserProfile> voters;

  public SurveyQuestionOption(String text) {
    this.text = text;
  }

  public static SurveyQuestionOption fromSurveyOption(String pollId, SurveyOption surveyOption, SurveyChoiceResult surveyChoiceResult) {
    SurveyQuestionOption pollOption = new SurveyQuestionOption(surveyOption.getValue());
    pollOption.setOptionId(surveyOption.getOptionId());
    if(null != surveyChoiceResult) {
      pollOption.setAnswer(surveyChoiceResult.getCount());
    }
    pollOption.setPollId(pollId);

    return pollOption;
  }

  /**
   * The id of the MMXPoll
   * @return
   */
  public String getPollId() {
    return pollId;
  }

  /* package */ void setPollId(String pollId) {
    this.pollId = pollId;
  }

  /**
   * The id of the option
   * @return
   */
  public String getOptionId() {
    return optionId;
  }

  /* package */ void setOptionId(String optionId) {
    this.optionId = optionId;
  }

  /* package */ void setAnswer(T answer) {
    this.answer = answer;
  }

  /**
   * The text of the option
   * @return
   */
  public String getText() {
    return text;
  }

  /**
   * The voted count of this option
   * @return
   */
  public T getAnswer() {
    return answer;
  }

  @Override public boolean equals(Object o) {
    boolean quickCheckResult = EqualityUtil.quickCheck(this, o);
    if(!quickCheckResult) {
      return false;
    }

    SurveyQuestionOption theOther = (SurveyQuestionOption) o;
    return StringUtil.isStringValueEqual(this.getPollId(), theOther.getPollId()) &&
        StringUtil.isStringValueEqual(this.getOptionId(), theOther.getOptionId());
  }

  @Override public int hashCode() {
    return new HashCodeBuilder().hash(pollId).hash(optionId).hashCode();
  }

  @Override public String toString() {
    return new StringBuilder("MMXPollOption { ").append("pollId = ").append(pollId)
        .append(", optionId = ").append(optionId)
        .append(", text = ").append(text)
        .append(", answer = ").append(answer)
        .append(" }")
        .toString();
  }
}
