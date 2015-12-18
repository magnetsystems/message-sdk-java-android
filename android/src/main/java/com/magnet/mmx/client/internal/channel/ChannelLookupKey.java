/**
 * File generated by Magnet Magnet Lang Tool on Dec 17, 2015 2:24:58 PM
 * @see {@link http://developer.magnet.com}
 */

package com.magnet.mmx.client.internal.channel;

import com.google.gson.annotations.SerializedName;

public class ChannelLookupKey {

  private String channelName;

  private Boolean privateChannel;

  @SerializedName("userId")
  private String ownerId;

  public ChannelLookupKey(String channelName, Boolean privateChannel, String ownerId) {
    this.channelName = channelName;
    this.privateChannel = privateChannel;
    this.ownerId = ownerId;
  }

  public String getChannelName() {
    return channelName;
  }

  public Boolean isPrivateChannel() {
    return privateChannel;
  }

  public String getOwnerId() {
    return ownerId;
  }
}
