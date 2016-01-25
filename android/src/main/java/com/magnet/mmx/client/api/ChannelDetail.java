/**
 * Copyright (c) 2012-2015 Magnet Systems. All rights reserved.
 */
package com.magnet.mmx.client.api;

import com.magnet.max.android.UserProfile;
import java.util.List;

public class ChannelDetail {
  private MMXChannel channel;
  private List<UserProfile> subscribers;
  private List<MMXMessage> messages;
  private int totalSubscribers;
  private int totalMessages;
  //private UserProfile owner;

  public MMXChannel getChannel() {
    return channel;
  }

  public List<UserProfile> getSubscribers() {
    return subscribers;
  }

  public List<MMXMessage> getMessages() {
    return messages;
  }

  //public UserProfile getOwner() {
  //  return owner;
  //}

  public int getTotalSubscribers() {
    return totalSubscribers;
  }

  public int getTotalMessages() {
    return totalMessages;
  }

  public static class Builder {
    private final ChannelDetail channelDetail;

    public Builder() {
      channelDetail = new ChannelDetail();
    }

    public Builder channel(MMXChannel channel) {
      channelDetail.channel = channel;
      return this;
    }

    public Builder subscribers(List<UserProfile> subscribers) {
      channelDetail.subscribers = subscribers;
      return this;
    }

    public Builder messages(List<MMXMessage> messages) {
      channelDetail.messages = messages;
      return this;
    }

    public Builder totalSubscribers(int totalSubscribers) {
      channelDetail.totalSubscribers = totalSubscribers;
      return this;
    }

    public Builder totalMessages(int totalMessages) {
      channelDetail.totalMessages = totalMessages;
      return this;
    }

    //public Builder owner(UserProfile owner) {
    //  channelDetail.owner = owner;
    //  return this;
    //}

    public ChannelDetail build() {
      return channelDetail;
    }
  }
}
