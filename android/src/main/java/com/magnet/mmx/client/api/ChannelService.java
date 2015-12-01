/**
 * Copyright (c) 2012-2015 Magnet Systems. All rights reserved.
 */
package com.magnet.mmx.client.api;

import java.util.List;
import retrofit.Callback;
import retrofit.MagnetCall;
import retrofit.http.Body;
import retrofit.http.POST;

public interface ChannelService {
  @POST("/com.magnet.server/channel/create'")
  MagnetCall<Void> createChannel(@Body ChannelInfo channelInfo, Callback<Void> callback);

  class ChannelInfo {
    private String channelName;

    //Set to true to make channel private, false to make public.
    //Default to false
    private boolean privateChannel;

    private String description;

    //Who can publish to the channels.
    //anyone ( default)
    //owner
    //subscribers
    private String publishPermission;

    private List<String> subscribers;

    public ChannelInfo(String channelName, String description, boolean privateChannel,
        String publishPermission, List<String> subscribers) {
      this.channelName = channelName;
      this.privateChannel = privateChannel;
      this.description = description;
      this.publishPermission = publishPermission;
      this.subscribers = subscribers;
    }

    public String getChannelName() {
      return channelName;
    }

    public boolean isPrivateChannel() {
      return privateChannel;
    }

    public String getDescription() {
      return description;
    }

    public String getPublishPermission() {
      return publishPermission;
    }

    public List<String> getSubscribers() {
      return subscribers;
    }
  }
}
