/**
 * Copyright (c) 2012-2015 Magnet Systems. All rights reserved.
 */
package com.magnet.mmx.client.api;

import com.google.gson.annotations.SerializedName;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import retrofit.Callback;
import retrofit.MagnetCall;
import retrofit.http.Body;
import retrofit.http.GET;
import retrofit.http.POST;
import retrofit.http.Path;

public interface ChannelService {
  @POST("/api/com.magnet.server/channel/create")
  MagnetCall<Void> createChannel(@Body ChannelInfo channelInfo, Callback<Void> callback);

  @GET("/api/com.magnet.server/channel/summary")
  MagnetCall<List<ChannelSummary>> getChannelSummary(@Body List<String> channelIds, ChannelSummaryOptions options, Callback<List<ChannelSummary>> callback);

  /**
   *
   * POST /com.magnet.server/channel/query
   * @param body style:Body optional:false
   * @param callback asynchronous callback
   */
  @POST("com.magnet.server/channel/query")
  MagnetCall<ChannelQueryResponse> queryChannelsBySubscribers(
      @Body ChannelBySubscriberRequest body,
      retrofit.Callback<ChannelQueryResponse> callback
  );

  /**
   *
   * POST /com.magnet.server/channel/subscribers/add
   * @param body style:Body optional:false
   * @param callback asynchronous callback
   */
  @POST("com.magnet.server/channel/{channelName}/subscribers/add")
  MagnetCall<ChannelSubscribeResponse> addSubscriberToChannel(
      @Path("channelName") String channelName,
      @Body ChannelSubscribeRequest body,
      retrofit.Callback<ChannelSubscribeResponse> callback
  );

  /**
   *
   * POST /com.magnet.server/channel/subscribers/remove
   * @param body style:Body optional:false
   * @param callback asynchronous callback
   */
  @POST("com.magnet.server/channel/{channelName}/subscribers/remove")
  MagnetCall<ChannelSubscribeResponse> removeSubscriberFromChannel(
      @Path("channelName") String channelName,
      @Body ChannelSubscribeRequest body,
      retrofit.Callback<ChannelSubscribeResponse> callback
  );

  class ChannelSubscribeRequest {
    //Set to true to if channel is private, false to make public.
    //Default to false
    protected boolean privateChannel;
    protected Set<String> subscribers;

    public ChannelSubscribeRequest(boolean privateChannel, Set<String> subscribers) {
      this.privateChannel = privateChannel;
      this.subscribers = subscribers;
    }

    public boolean isPrivateChannel() {
      return privateChannel;
    }

    public Set<String> getSubscribers() {
      return subscribers;
    }
  }

  abstract class BaseMMXResponse {
    public static final int SUCCESS_CODE = 0;

    protected int code;
    protected String message;

    public BaseMMXResponse(int code, String message) {
      this.code = code;
      this.message = message;
    }

    public int getCode() {
      return code;
    }

    public String getMessage() {
      return message;
    }

    public boolean isSuccess() {
      return code == SUCCESS_CODE;
    }
  }

  class ChannelSubscribeResponse extends BaseMMXResponse {
    private Map<String, SubscribeResult> subscribeResponse;

    public ChannelSubscribeResponse(int code, String message,
        Map<String, SubscribeResult> subscribeResponse) {
      super(code, message);
      this.subscribeResponse = subscribeResponse;
    }

    public Map<String, SubscribeResult> getSubscribeResponse() {
      return subscribeResponse;
    }

    public static class SubscribeResult extends BaseMMXResponse {
      private String subId;

      public SubscribeResult(int code, String message, String subId) {
        super(code, message);
        this.subId = subId;
      }

      public String getSubId() {
        return subId;
      }
    }
  }

  class ChannelInfoResponse {
    private String name;
    private String description;
    private Date creationDate;
    private Date modifiedDate;
    @SerializedName("creator")
    private String ownerId;
    @SerializedName("userChannel")
    private boolean privateChannel;
    private String publishPermission;

    public String getName() {
      return name;
    }

    public String getDescription() {
      return description;
    }

    public Date getCreationDate() {
      return creationDate;
    }

    public Date getModifiedDate() {
      return modifiedDate;
    }

    public String getOwnerId() {
      return ownerId;
    }

    public boolean isPrivateChannel() {
      return privateChannel;
    }

    public String getPublishPermission() {
      return publishPermission;
    }

    public MMXChannel toMMXChannel() {
      return new MMXChannel.Builder()
          .name(name)
          .summary(description)
          .ownerId(ownerId)
          .setPublic(!privateChannel)
          .publishPermission(MMXChannel.PublishPermission.valueOf(publishPermission))
          .creationDate(creationDate)
          .subscribed(true)
          .lastTimeActive(modifiedDate)
          .build();
    }
  }

  class ChannelQueryResponse extends BaseMMXResponse {
    private List<ChannelInfoResponse> channels;

    public ChannelQueryResponse(int code, String message, List<ChannelInfoResponse> channels) {
      super(code, message);
      this.channels = channels;
    }

    public List<ChannelInfoResponse> getChannels() {
      return channels;
    }
  }

  class ChannelInfo extends ChannelSubscribeRequest {
    private String channelName;

    private String description;

    //Who can publish to the channels.
    //anyone ( default)
    //owner
    //subscribers
    private String publishPermission;

    public ChannelInfo(String channelName, String description, boolean privateChannel,
        String publishPermission, Set<String> subscribers) {
      super(privateChannel, subscribers);
      this.channelName = channelName;
      this.description = description;
      this.publishPermission = publishPermission;
    }

    public String getChannelName() {
      return channelName;
    }

    public String getDescription() {
      return description;
    }

    public String getPublishPermission() {
      return publishPermission;
    }
  }

  class ChannelBySubscriberRequest {
    private ChannelMatchType matchFilter;
    private Set<String> subscribers;

    public ChannelBySubscriberRequest(Set<String> subscribers,
        ChannelMatchType matchFilter) {
      this.subscribers = subscribers;
      this.matchFilter = matchFilter;
    }

    public ChannelMatchType getMatchFilter() {
      return matchFilter;
    }

    public Set<String> getSubscribers() {
      return subscribers;
    }
  }
}
