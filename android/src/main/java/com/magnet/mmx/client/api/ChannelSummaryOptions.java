/**
 * Copyright (c) 2012-2015 Magnet Systems. All rights reserved.
 */
package com.magnet.mmx.client.api;

public class ChannelSummaryOptions {
  public static final int MAX_MESSAGE_NUM = 25;
  public static final int MAX_SUBSCIRBER_NUM = 25;

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
      if(messageNum > MAX_MESSAGE_NUM) {
        throw new IllegalArgumentException("messageNum should less than " + MAX_MESSAGE_NUM);
      }

      options.messageNum = messageNum;
      return this;
    }

    public Builder subscriberNum(int subscriberNum) {
      if(subscriberNum > MAX_SUBSCIRBER_NUM) {
        throw new IllegalArgumentException("subscriberNum should less than " + MAX_SUBSCIRBER_NUM);
      }

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
