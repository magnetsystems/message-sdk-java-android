/**
 * Copyright (c) 2012-2016 Magnet Systems. All rights reserved.
 */
package com.magnet.mmx.client.ext.poll;

import android.os.Parcel;
import android.os.Parcelable;
import com.magnet.max.android.util.EqualityUtil;
import com.magnet.max.android.util.HashCodeBuilder;
import com.magnet.max.android.util.ParcelableHelper;
import com.magnet.max.android.util.StringUtil;
import com.magnet.mmx.client.api.MMXTypedPayload;
import com.magnet.mmx.client.internal.survey.model.SurveyChoiceResult;
import com.magnet.mmx.client.internal.survey.model.SurveyOption;
import java.util.Map;

/**
 * This class defines a option of a MMXPoll
 */
public class MMXPollOption implements MMXTypedPayload, Parcelable {
  public static final String TYPE = "MMXPollOption";

  private String pollId;
  private String optionId;
  private String text;
  private Long count;
  private Map<String, String> extras;

  //private List<UserProfile> voters;

  public MMXPollOption(String text) {
    this(text, null);
  }

  public MMXPollOption(String text, Map<String, String> extras) {
    this.text = text;
    this.extras = extras;
  }

  /** Package */ static MMXPollOption fromSurveyOption(String pollId, SurveyOption surveyOption,
      SurveyChoiceResult surveyChoiceResult) {
    MMXPollOption pollOption = new MMXPollOption(surveyOption.getValue());
    pollOption.setOptionId(surveyOption.getOptionId());
    if(null != surveyChoiceResult) {
      pollOption.setCount(surveyChoiceResult.getCount());
    }
    pollOption.setPollId(pollId);
    if(null != surveyOption.getMetaData() && !surveyOption.getMetaData().isEmpty()) {
      pollOption.extras = surveyOption.getMetaData();
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
  public Long getCount() {
    return count;
  }

  public void increaseCount(long delta) {
    if(null != this.count) {
      this.count += delta;
    }
  }

  /**
   * The extra meta data of the option in key-value pair
   * @return
   */
  public Map<String, String> getExtras() {
    return extras;
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

  @Override public int describeContents() {
    return 0;
  }

  @Override public void writeToParcel(Parcel dest, int flags) {
    dest.writeString(this.pollId);
    dest.writeString(this.optionId);
    dest.writeString(this.text);
    dest.writeLong(this.count);
    dest.writeBundle(ParcelableHelper.stringMapToBundle(this.extras));
  }

  protected MMXPollOption(Parcel in) {
    this.pollId = in.readString();
    this.optionId = in.readString();
    this.text = in.readString();
    this.count = in.readLong();
    this.extras = ParcelableHelper.stringMapfromBundle(in.readBundle(getClass().getClassLoader()));
  }

  public static final Parcelable.Creator<MMXPollOption> CREATOR =
      new Parcelable.Creator<MMXPollOption>() {
        @Override public MMXPollOption createFromParcel(Parcel source) {
          return new MMXPollOption(source);
        }

        @Override public MMXPollOption[] newArray(int size) {
          return new MMXPollOption[size];
        }
      };
}
