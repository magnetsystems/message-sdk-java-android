/**
 * File generated by Magnet Magnet Lang Tool on Dec 17, 2015 2:24:58 PM
 * @see {@link http://developer.magnet.com}
 */

package com.magnet.mmx.client.internal.channel;

public class ChannelLookupKey {

  private String channelName;

  private Boolean privateChannel;

  private String userId;

  public ChannelLookupKey(String channelName, Boolean privateChannel, String userId) {
    this.channelName = channelName;
    this.privateChannel = privateChannel;
    this.userId = userId;
  }

  public String getChannelName() {
    return channelName;
  }

  public Boolean getPrivateChannel() {
    return privateChannel;
  }

  public String getUserId() {
    return userId;
  }
}
