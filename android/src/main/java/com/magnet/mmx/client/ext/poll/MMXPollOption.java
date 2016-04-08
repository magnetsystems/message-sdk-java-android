/**
 * Copyright (c) 2012-2016 Magnet Systems. All rights reserved.
 */
package com.magnet.mmx.client.ext.poll;

import com.magnet.max.android.util.EqualityUtil;
import com.magnet.max.android.util.HashCodeBuilder;
import com.magnet.max.android.util.StringUtil;

public class MMXPollOption {
  private String pollId;
  private String optionId;
  private String text;
  private int count;

  //private List<UserProfile> voters;

  public MMXPollOption(String text) {
    this.text = text;
    //this.pollId = pollId;
  }

  public MMXPollOption(String text, String pollId) {
    this.text = text;
    this.pollId = pollId;
  }


  public String getPollId() {
    return pollId;
  }

  /* package */ void setPollId(String pollId) {
    this.pollId = pollId;
  }

  public String getOptionId() {
    return optionId;
  }

  /* package */ void setOptionId(String optionId) {
    this.optionId = optionId;
  }

  public String getText() {
    return text;
  }

  public int getCount() {
    return count;
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
}
