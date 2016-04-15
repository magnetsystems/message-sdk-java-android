/**
 * Copyright (c) 2012-2016 Magnet Systems. All rights reserved.
 */
package com.magnet.mmx.client.ext.poll;

import com.magnet.max.android.util.EqualityUtil;
import com.magnet.max.android.util.HashCodeBuilder;
import com.magnet.max.android.util.StringUtil;
import com.magnet.mmx.client.api.MMXTypedPayload;
import com.magnet.mmx.client.internal.survey.model.SurveyChoiceResult;
import com.magnet.mmx.client.internal.survey.model.SurveyOption;
import java.util.Map;

/**
 * This class defines a option of a MMXPoll
 */
public class MMXPollOption implements MMXTypedPayload {
  public static final String TYPE = "MMXPollOption";

  private String pollId;
  private String optionId;
  private String text;
  private long count;
  private Map<String, String> metaData;

  //private List<UserProfile> voters;

  public MMXPollOption(String text) {
    this(text, null);
  }

  public MMXPollOption(String text, Map<String, String> metaData) {
    this.text = text;
    this.metaData = metaData;
  }

  public static MMXPollOption fromSurveyOption(String pollId, SurveyOption surveyOption, SurveyChoiceResult surveyChoiceResult) {
    MMXPollOption pollOption = new MMXPollOption(surveyOption.getValue());
    pollOption.setOptionId(surveyOption.getOptionId());
    if(null != surveyChoiceResult) {
      pollOption.setCount(surveyChoiceResult.getCount());
    }
    pollOption.setPollId(pollId);
    if(null != surveyOption.getMetaData() && !surveyOption.getMetaData().isEmpty()) {
      pollOption.metaData = surveyOption.getMetaData();
    }

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

  /* package */ void setCount(long count) {
    this.count = count;
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
  public long getCount() {
    return count;
  }

  /**
   * The extra meta data of the option in key-value pair
   * @return
   */
  public Map<String, String> getMetaData() {
    return metaData;
  }

  @Override public boolean equals(Object o) {
    boolean quickCheckResult = EqualityUtil.quickCheck(this, o);
    if(!quickCheckResult) {
      return false;
    }

    MMXPollOption theOther = (MMXPollOption) o;
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
        .append(", count = ").append(count)
        .append(" }")
        .toString();
  }
}
