/**
 * Copyright (c) 2012-2015 Magnet Systems. All rights reserved.
 */
package com.magnet.mmx.client.api;

import android.os.Parcel;
import android.os.Parcelable;
import com.magnet.max.android.UserProfile;
import java.util.List;

/**
 * The details of @see MMXChannel
 */
public class ChannelDetail implements Parcelable {
  private MMXChannel channel;
  private List<UserProfile> subscribers;
  private List<MMXMessage> messages;
  private int totalSubscribers;
  private int totalMessages;
  private UserProfile owner;

  /**
   * The channel
   * @return
   */
  public MMXChannel getChannel() {
    return channel;
  }

  /**
   * Subscribers of the channel requested in @see ChannelDetailOptions#getNumOfSubcribers
   * @return
   */
  public List<UserProfile> getSubscribers() {
    return subscribers;
  }

  /**
   * Messages of the channel requested in @see ChannelDetailOptions#getNumOfMessages
   * @return
   */
  public List<MMXMessage> getMessages() {
    return messages;
  }

  public UserProfile getOwner() {
    return owner;
  }

  /**
   * Total number of subscribers
   * @return
   */
  public int getTotalSubscribers() {
    return totalSubscribers;
  }

  /**
   * Total number of messages
   * @return
   */
  public int getTotalMessages() {
    return totalMessages;
  }

  /**
   * Builder for @see ChannelDetail
   */
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

    public Builder owner(UserProfile owner) {
      channelDetail.owner = owner;
      return this;
    }

    public ChannelDetail build() {
      return channelDetail;
    }
  }

  @Override public int describeContents() {
    return 0;
  }

  @Override public void writeToParcel(Parcel dest, int flags) {
    dest.writeParcelable(this.channel, 0);
    dest.writeTypedList(subscribers);
    dest.writeTypedList(messages);
    dest.writeInt(this.totalSubscribers);
    dest.writeInt(this.totalMessages);
    dest.writeParcelable(this.owner, flags);
  }

  public ChannelDetail() {
  }

  protected ChannelDetail(Parcel in) {
    this.channel = in.readParcelable(MMXChannel.class.getClassLoader());
    this.subscribers = in.createTypedArrayList(UserProfile.CREATOR);
    this.messages = in.createTypedArrayList(MMXMessage.CREATOR);
    this.totalSubscribers = in.readInt();
    this.totalMessages = in.readInt();
    this.owner = in.readParcelable(UserProfile.class.getClassLoader());
  }

  public static final Parcelable.Creator<ChannelDetail> CREATOR =
      new Parcelable.Creator<ChannelDetail>() {
        public ChannelDetail createFromParcel(Parcel source) {
          return new ChannelDetail(source);
        }

        public ChannelDetail[] newArray(int size) {
          return new ChannelDetail[size];
        }
      };
}
