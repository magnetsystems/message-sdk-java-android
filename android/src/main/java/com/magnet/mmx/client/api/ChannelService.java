/**
 * Copyright (c) 2012-2015 Magnet Systems. All rights reserved.
 */
package com.magnet.mmx.client.api;

import java.nio.channels.Channel;
import java.util.Set;
import retrofit.Callback;
import retrofit.MagnetCall;
import retrofit.http.Body;
import retrofit.http.POST;

public interface ChannelService {
  @POST("/api/com.magnet.server/channel/create")
  MagnetCall<Void> createChannel(@Body ChannelInfo channelInfo, Callback<Void> callback);

  /**
   *
   * POST /com.magnet.server/channel/query
   * @param body style:Body optional:false
   * @param callback asynchronous callback
   */
  @POST("com.magnet.server/channel/query")
  MagnetCall<String> queryChannels(
      @Body QueryChannelRequest body,
      retrofit.Callback<String> callback
  );

  /**
   *
   * POST /com.magnet.server/channel/subscribers/add
   * @param body style:Body optional:false
   * @param callback asynchronous callback
   */
  @POST("com.magnet.server/channel/subscribers/add")
  MagnetCall<String> addSubscriberToChannel(
      @Body ChannelRequest body,
      retrofit.Callback<String> callback
  );

  /**
   *
   * POST /com.magnet.server/channel/subscribers/remove
   * @param body style:Body optional:false
   * @param callback asynchronous callback
   */
  @POST("com.magnet.server/channel/subscribers/remove")
  MagnetCall<String> removeSubscriberFromChannel(
      @Body ChannelRequest body,
      retrofit.Callback<String> callback
  );

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

  class QueryChannelRequest {

    private String mmxAppId;

    private String userId;

    private java.util.List<String> subscribers;

    private String deviceId;

    private ChannelMatchType matchFilter;

    public String getMmxAppId() {
      return mmxAppId;
    }

    public String getUserId() {
      return userId;
    }

    public java.util.List<String> getSubscribers() {
      return subscribers;
    }

    public String getDeviceId() {
      return deviceId;
    }

    public ChannelMatchType getMatchFilter() {
      return matchFilter;
    }

    /**
     * Builder for QueryChannelRequest
     **/
    public static class QueryChannelRequestBuilder {
      private QueryChannelRequest toBuild = new QueryChannelRequest();

      public QueryChannelRequestBuilder() {
      }

      public QueryChannelRequest build() {
        return toBuild;
      }

      public QueryChannelRequestBuilder mmxAppId(String value) {
        toBuild.mmxAppId = value;
        return this;
      }

      public QueryChannelRequestBuilder userId(String value) {
        toBuild.userId = value;
        return this;
      }

      public QueryChannelRequestBuilder subscribers(java.util.List<String> value) {
        toBuild.subscribers = value;
        return this;
      }

      public QueryChannelRequestBuilder deviceId(String value) {
        toBuild.deviceId = value;
        return this;
      }

      public QueryChannelRequestBuilder matchFilter(ChannelMatchType value) {
        toBuild.matchFilter = value;
        return this;
      }
    }
  }

  public class ChannelRequest {



    private String mmxAppId;


    private Boolean privateChannel;


    private String channelName;


    private Boolean subscribeOnCreate;


    private String publishPermission;


    private String description;


    private String userId;


    private java.util.List<String> subscribers;


    private String deviceId;

    public String getMmxAppId() {
      return mmxAppId;
    }

    public Boolean getPrivateChannel() {
      return privateChannel;
    }

    public String getChannelName() {
      return channelName;
    }

    public Boolean getSubscribeOnCreate() {
      return subscribeOnCreate;
    }

    public String getPublishPermission() {
      return publishPermission;
    }

    public String getDescription() {
      return description;
    }

    public String getUserId() {
      return userId;
    }

    public java.util.List<String> getSubscribers() {
      return subscribers;
    }

    public String getDeviceId() {
      return deviceId;
    }


    /**
     * Builder for ChannelRequest
     **/
    public static class ChannelRequestBuilder {
      private ChannelRequest toBuild = new ChannelRequest();

      public ChannelRequestBuilder() {
      }

      public ChannelRequest build() {
        return toBuild;
      }

      public ChannelRequestBuilder mmxAppId(String value) {
        toBuild.mmxAppId = value;
        return this;
      }

      public ChannelRequestBuilder privateChannel(Boolean value) {
        toBuild.privateChannel = value;
        return this;
      }

      public ChannelRequestBuilder channelName(String value) {
        toBuild.channelName = value;
        return this;
      }

      public ChannelRequestBuilder subscribeOnCreate(Boolean value) {
        toBuild.subscribeOnCreate = value;
        return this;
      }

      public ChannelRequestBuilder publishPermission(String value) {
        toBuild.publishPermission = value;
        return this;
      }

      public ChannelRequestBuilder description(String value) {
        toBuild.description = value;
        return this;
      }

      public ChannelRequestBuilder userId(String value) {
        toBuild.userId = value;
        return this;
      }

      public ChannelRequestBuilder subscribers(java.util.List<String> value) {
        toBuild.subscribers = value;
        return this;
      }

      public ChannelRequestBuilder deviceId(String value) {
        toBuild.deviceId = value;
        return this;
      }
    }
  }
}
