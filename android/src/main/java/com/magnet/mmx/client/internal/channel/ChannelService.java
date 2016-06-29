/**
 * Copyright (c) 2012-2015 Magnet Systems. All rights reserved.
 */
package com.magnet.mmx.client.internal.channel;

import com.google.gson.annotations.SerializedName;
import com.magnet.mmx.client.api.ChannelMatchType;
import com.magnet.mmx.client.api.MMXChannel;
import com.magnet.mmx.protocol.TopicAction;
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
  MagnetCall<ChannelCreateResponse> createChannel(@Body ChannelInfo channelInfo,
      Callback<ChannelCreateResponse> callback);

  @POST("/api/com.magnet.server/channel/summary")
  MagnetCall<List<ChannelSummaryResponse>> getChannelSummary(@Body ChannelSummaryRequest request,
      Callback<List<ChannelSummaryResponse>> callback);

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
  @POST("com.magnet.server/channel/{channelId}/subscribers/add")
  MagnetCall<ChannelSubscribeResponse> addSubscriberToChannel(
      @Path("channelId") String channelId,
      @Body ChannelSubscribeRequest body,
      retrofit.Callback<ChannelSubscribeResponse> callback
  );

  /**
   *
   * POST /com.magnet.server/channel/subscribers/remove
   * @param body style:Body optional:false
   * @param callback asynchronous callback
   */
  @POST("com.magnet.server/channel/{channelId}/subscribers/remove")
  MagnetCall<ChannelSubscribeResponse> removeSubscriberFromChannel(
      @Path("channelId") String channelId,
      @Body ChannelSubscribeRequest body,
      retrofit.Callback<ChannelSubscribeResponse> callback
  );

  @POST("/api/com.magnet.server/channel/{channelId}/push/mute")
  MagnetCall<Void> mute(@Path("channelId") String channelId,
      @Body MuteChannelPushRequest muteChannelPushRequest,
      Callback<Void> callback);

  @POST("/api/com.magnet.server/channel/{channelId}/push/unmute")
  MagnetCall<Void> unMute(@Path("channelId") String channelId,
      Callback<Void> callback);

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
    private final Map<String, SubscribeResult> subscribeResponse;

    public ChannelSubscribeResponse(int code, String message,
        Map<String, SubscribeResult> subscribeResponse) {
      super(code, message);
      this.subscribeResponse = subscribeResponse;
    }

    public Map<String, SubscribeResult> getSubscribeResponse() {
      return subscribeResponse;
    }

    public static class SubscribeResult extends BaseMMXResponse {
      private final String subId;

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
    private boolean pushMutedByUser;
    private Date pushMutedUntil;

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

    public boolean isPushMutedByUser() {
      return pushMutedByUser;
    }

    public Date getPushMutedUntil() {
      return pushMutedUntil;
    }

    public MMXChannel toMMXChannel() {
      return new MMXChannel.Builder()
          .name(name)
          .summary(description)
          .ownerId(ownerId)
          .setPublic(!privateChannel)
          .publishPermission(MMXChannel.PublishPermission.fromPublisherType(
              TopicAction.PublisherType.valueOf(publishPermission)))
          .creationDate(creationDate)
          .subscribed(true)
          .isMuted(pushMutedByUser)
          .lastTimeActive(modifiedDate)
          .build();
    }
  }

  class ChannelQueryResponse extends BaseMMXResponse {
    private final List<ChannelInfoResponse> channels;

    public ChannelQueryResponse(int code, String message, List<ChannelInfoResponse> channels) {
      super(code, message);
      this.channels = channels;
    }

    public List<ChannelInfoResponse> getChannels() {
      return channels;
    }
  }

  public static class ChannelCreateResponse extends BaseMMXResponse {
    private final String channelId;

    public ChannelCreateResponse(int code, String message, String channelId) {
      super(code, message);
      this.channelId = channelId;
    }

    public String getChannelId() {
      return channelId;
    }
  }

  class ChannelInfo extends ChannelSubscribeRequest {
    private final String channelName;

    private final String description;

    private final String pushConfigName;

    //Who can publish to the channels.
    //anyone ( default)
    //owner
    //subscribers
    private final String publishPermission;

    public ChannelInfo(String channelName, String description, boolean privateChannel,
        String publishPermission, Set<String> subscribers, String pushConfigName) {
      super(privateChannel, subscribers);
      this.channelName = channelName;
      this.description = description;
      this.publishPermission = publishPermission;
      this.pushConfigName = pushConfigName;
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

    public String getPushConfigName() {
      return pushConfigName;
    }
  }

  class ChannelBySubscriberRequest {
    private final ChannelMatchType matchFilter;
    private final Set<String> subscribers;

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
