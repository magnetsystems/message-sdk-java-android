/**
 * Copyright (c) 2012-2015 Magnet Systems. All rights reserved.
 */
package com.magnet.mmx.client.api;

/**
 * Options to get @see ChannelDetial
 */
public class ChannelDetailOptions {
  public static final int DEFAULT_NUMBER_OF_MESSAGE = 5;
  public static final int DEFAULT_NUMBER_OF_SUBSCRIBER = 5;

  private Integer numOfMessages;
  private Integer numOfSubcribers;
  //private boolean wantOwnerInfo;

  private ChannelDetailOptions() {
  }

  /**
   * Number of messages to retrieve
   * If not specifed, default is 5
   * @return
   */
  public Integer getNumOfMessages() {
    return numOfMessages;
  }

  /**
   * Number of subsribers to retrieve
   * If not specifed, default is 5
   * @return
   */
  public Integer getNumOfSubcribers() {
    return numOfSubcribers;
  }

  //public boolean wantOwnerInfo() {
  //  return wantOwnerInfo;
  //}

  /**
   * Builder for @see ChannelDetailOptions
   */
  public static class Builder {
    private ChannelDetailOptions options;

    public Builder() {
      options = new ChannelDetailOptions();
    }

    public Builder numOfMessages(Integer messageNum) {
      if(messageNum < 0) {
        throw new IllegalArgumentException("numOfMessages should greater than 0");
      }

      options.numOfMessages = messageNum;
      return this;
    }

    public Builder numOfSubcribers(Integer subscriberNum) {
      if(subscriberNum < 0) {
        throw new IllegalArgumentException("numOfSubcribers should greater than 0");
      }

      options.numOfSubcribers = subscriberNum;
      return this;
    }

    //public Builder wantOwnerInfo() {
    //  options.wantOwnerInfo = true;
    //  return this;
    //}

    public ChannelDetailOptions build() {
      if(null == options.numOfMessages) {
        options.numOfMessages = DEFAULT_NUMBER_OF_MESSAGE;
      }
      if(null == options.numOfSubcribers) {
        options.numOfSubcribers = DEFAULT_NUMBER_OF_SUBSCRIBER;
      }
      return options;
    }
  }
}
