/**
 * Copyright (c) 2012-2015 Magnet Systems. All rights reserved.
 */
package com.magnet.mmx.client.api;

import com.magnet.max.android.User;
import java.util.List;
import java.util.Set;
import retrofit.Callback;
import retrofit.MagnetCall;
import retrofit.http.Body;
import retrofit.http.GET;
import retrofit.http.POST;

public interface ChannelService {
  @POST("/api/com.magnet.server/channel/create")
  MagnetCall<Void> createChannel(@Body ChannelInfo channelInfo, Callback<Void> callback);

  @GET("/api/com.magnet.server/channel/summary")
  MagnetCall<List<ChannelSummary>> getChannelSummary(@Body List<String> channelIds, ChannelSummaryOptions options, Callback<List<ChannelSummary>> callback);

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

    private Set<String> subscribers;

    public ChannelInfo(String channelName, String description, boolean privateChannel,
        String publishPermission, Set<String> subscribers) {
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

    public Set<String> getSubscribers() {
      return subscribers;
    }
  }
}
