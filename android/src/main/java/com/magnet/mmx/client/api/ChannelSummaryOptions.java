/**
 * Copyright (c) 2012-2015 Magnet Systems. All rights reserved.
 */
package com.magnet.mmx.client.api;

public class ChannelSummaryOptions {
  private int messageNum;
  private int subscriberNum;
  private boolean wantOwnerInfo;

  private ChannelSummaryOptions() {

  }

  public int getMessageNum() {
    return messageNum;
  }

  public int getSubscriberNum() {
    return subscriberNum;
  }

  public boolean wantOwnerInfo() {
    return wantOwnerInfo;
  }

  public static class Builder {
    private ChannelSummaryOptions options;

    public Builder() {
      options = new ChannelSummaryOptions();
    }

    public Builder messageNum(int messageNum) {
      options.messageNum = messageNum;
      return this;
    }

    public Builder subscriberNum(int subscriberNum) {
      options.subscriberNum = subscriberNum;
      return this;
    }

    public Builder wantOwnerInfo() {
      options.wantOwnerInfo = true;
      return this;
    }

    public ChannelSummaryOptions build() {
      return options;
    }
  }
}
