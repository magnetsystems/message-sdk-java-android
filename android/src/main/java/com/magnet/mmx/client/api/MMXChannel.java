/*   Copyright (c) 2015 Magnet Systems, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.magnet.mmx.client.api;

import android.graphics.Bitmap;
import android.os.Parcel;
import android.os.Parcelable;
import com.magnet.max.android.ApiCallback;
import com.magnet.max.android.ApiError;
import com.magnet.max.android.Attachment;
import com.magnet.max.android.MaxCore;
import com.magnet.max.android.User;
import com.magnet.max.android.UserProfile;
import com.magnet.max.android.util.EqualityUtil;
import com.magnet.max.android.util.HashCodeBuilder;
import com.magnet.max.android.util.StringUtil;
import com.magnet.mmx.client.MMXClient;
import com.magnet.mmx.client.MMXPubSubManager;
import com.magnet.mmx.client.MMXTask;
import com.magnet.mmx.client.common.Log;
import com.magnet.mmx.client.common.MMXException;
import com.magnet.mmx.client.common.MMXGlobalTopic;
import com.magnet.mmx.client.common.MMXPersonalTopic;
import com.magnet.mmx.client.common.MMXResult;
import com.magnet.mmx.client.common.MMXSubscription;
import com.magnet.mmx.client.common.MMXTopicInfo;
import com.magnet.mmx.client.common.MMXTopicSearchResult;
import com.magnet.mmx.client.common.MMXUserTopic;
import com.magnet.mmx.client.internal.channel.BasicUserInfo;
import com.magnet.mmx.client.internal.channel.ChannelService;
import com.magnet.mmx.client.internal.channel.ChannelSummaryRequest;
import com.magnet.mmx.client.internal.channel.ChannelSummaryResponse;
import com.magnet.mmx.client.internal.channel.PubSubItem;
import com.magnet.mmx.protocol.MMXStatus;
import com.magnet.mmx.protocol.MMXTopic;
import com.magnet.mmx.protocol.MMXTopicOptions;
import com.magnet.mmx.protocol.SearchAction;
import com.magnet.mmx.protocol.StatusCode;
import com.magnet.mmx.protocol.TopicAction;
import com.magnet.mmx.protocol.TopicAction.ListType;
import com.magnet.mmx.protocol.TopicSummary;
import com.magnet.mmx.protocol.UserInfo;
import com.magnet.mmx.util.TimeUtil;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import retrofit.Callback;
import retrofit.Response;

/**
 * The MMXChannel class uses the underneath pub/sub technology to provide
 * powerful communication paradigms. Caller uses this class for forum
 * discussions, private one-to-one chat, private group chat, 
 * one-to-many announcement, or many-to-one personal messaging.  A private
 * channel can also be used as a private persistent storage.  After an
 * {@link MMX.EventListener} is registered to {@link MMX}, caller usually does
 * <ol>
 *  <li>locate a channel by
 *    <ul>
 *      <li>{@link #create(String, String, boolean, PublishPermission, OnFinishedListener)}</li>
 *      <li>{@link #findPublicChannelsByName(String, Integer, Integer, OnFinishedListener)}</li>
 *      <li>{@link #findPrivateChannelsByName(String, Integer, Integer, OnFinishedListener)}</li>
 *      <li>{@link #findByTags(Set, Integer, Integer, OnFinishedListener)}</li>
 *      <li>{@link #getPublicChannel(String, OnFinishedListener)}</li>
 *      <li>{@link #getPrivateChannel(String, OnFinishedListener)}</li>
 *    </ul>
 *  <li>subscribe to a channel by {@link #subscribe(OnFinishedListener)}</li>
 *  <li>publish a message to the channel by {@link #publish(Map, OnFinishedListener)}</li>
 * </ol>
 * The channel owner may invite people to a channel via
 * <ul>
 * <li>{@link #inviteUser(User, String, OnFinishedListener)}</li>
 * <li>{@link #inviteUsers(Set, String, OnFinishedListener)}</li>
 * </ul>
 */
public class MMXChannel implements Parcelable {
  private static final String TAG = MMXChannel.class.getSimpleName();
  private static final int DEFAULT_LIMIT = 5000;  //this is enforced by the server

  /**
   * Describes the publishing permissions for this channel.
   *
   * @see #create(String, String, boolean, PublishPermission, OnFinishedListener)
   */
  public enum PublishPermission {
    /**
     * Only the owner can publish
     */
    OWNER_ONLY(TopicAction.PublisherType.owner),
    /**
     * Only the owner and subscribers can publish
     */
    SUBSCRIBER(TopicAction.PublisherType.subscribers),
    /**
     * Anyone can publish
     */
    ANYONE(TopicAction.PublisherType.anyone);

    private final TopicAction.PublisherType type;
    PublishPermission(TopicAction.PublisherType type) {
      this.type = type;
    }

    public static PublishPermission fromPublisherType(TopicAction.PublisherType type) {
      switch (type) {
        case anyone:
          return ANYONE;
        case owner:
          return OWNER_ONLY;
        case subscribers:
          return SUBSCRIBER;
      }
      Log.w(TAG, "Role.fromPublisherType():  Unknown publisher type: " + type + ".  Returning ANYONE");
      return ANYONE;
    }
  };

  /**
   * Failure codes for the MMXChannel class.
   */
  public static class FailureCode extends MMX.FailureCode {
    public static final FailureCode CHANNEL_NOT_AUTHORIZED = new FailureCode(401, "CHANNEL_NOT_AUTHROIZED");
    public static final FailureCode CHANNEL_EXISTS = new FailureCode(409, "CHANNEL_EXISTS");
    public static final FailureCode CHANNEL_FORBIDDEN = new FailureCode(403, "CHANNEL_FORBIDDEN");
    public static final FailureCode CHANNEL_NOT_FOUND = new FailureCode(404, "CHANNEL_NOT_FOUND");
    public static final FailureCode INVALID_INVITEE = new FailureCode(405, "INVALID_INVITEE");
    public static final FailureCode SUBSCRIPTION_INVALID_ID = new FailureCode(406, "SUBSCRIPTION_INVALID_ID");
    public static final FailureCode SUBSCRIPTION_NOT_FOUND = new FailureCode(407, "SUBSCRIPTION_NOT_FOUND");
    public static final FailureCode SUBSCRIPTION_ADD_FAILURE = new FailureCode(408, "SUBSCRIPTION_ADD_FAILURE");
    public static final FailureCode SUBSCRIPTION_REMOVE_FAILURE = new FailureCode(409, "SUBSCRIPTION_REMOVE_FAILURE");
    public static final FailureCode CONTENT_TOO_LARGE = new FailureCode(413, "CONTENT_TOO_LARGE");
    public static final FailureCode CONTENT_EMPTY = new FailureCode(414, "CONTENT_IS_EMPTY");
    public static final FailureCode ATTACHMENT_FAILURE = new FailureCode(430, "ATTACHMENT_FAILURE");
    public static final FailureCode GENERIC_FAILURE = new FailureCode(500, "GENERIC_FAILURE");

    FailureCode(int value, String description) {
      super(value, description);
    }

    FailureCode(int value, String description, Throwable throwable) {
      super(value, description, throwable);
    }

    FailureCode(MMX.FailureCode code) { super(code); }

    static FailureCode fromMMXFailureCode(MMX.FailureCode code, Throwable throwable) {
      if (throwable != null)
        Log.d(TAG, "fromMMXFailureCode() ex="+throwable.getClass().getName());
      else
        Log.d(TAG, "fromMMXFailureCode() ex=null");
      if (throwable instanceof MMXException) {
        return new FailureCode(((MMXException) throwable).getCode(), throwable.getMessage());
      } else {
        return new FailureCode(code);
      }
    }
  }

  /**
   * The OnFinishedListener for MMXChannel methods.
   *
   * @param <T> The type of the onSuccess result
   */
  public static abstract class OnFinishedListener<T> implements IOnFinishedListener<T, FailureCode> {
    /**
     * Called when the operation completes successfully
     *
     * @param result the result of the operation
     */
    public abstract void onSuccess(T result);

    /**
     * Called if the operation fails
     *
     * @param code the failure code
     * @param throwable the throwable associated with this failure (may be null)
     */
    public abstract void onFailure(FailureCode code, Throwable throwable);
  }

  /**
   * The builder for a MMXChannel object
   *
   */
  static public final class Builder {
    private MMXChannel mChannel;

    public Builder() {
      mChannel = new MMXChannel();
    }

    /**
     * Set the name of this channel. It is a required property.
     *
     * @param name the non-null name
     * @return this Builder object
     */
    public Builder name(String name) {
      mChannel.name(name);
      return this;
    }

    /**
     * Set the summary of this channel.  It is a required property.
     *
     * @param summary the non-null summary
     * @return this Builder object
     */
    public Builder summary(String summary) {
      mChannel.summary(summary);
      return this;
    }

    /**
     * Set the owner's user identifier for this channel
     *
     * @param ownerId the owner userId
     * @return this Builder object
     */
    public Builder ownerId(String ownerId) {
      mChannel.ownerId(ownerId);
      return this;
    }

    /**
     * Set the number of messages for this channel
     *
     * @param numberOfMessages the number of messages
     * @return this Builder object
     */
    Builder numberOfMessages(Integer numberOfMessages) {
      mChannel.numberOfMessages(numberOfMessages);
      return this;
    }

    /**
     * Set the last active time for this channel
     *
     * @param lastTimeActive the last active time
     * @return this Builder object
     */
    public Builder lastTimeActive(Date lastTimeActive) {
      mChannel.lastTimeActive(lastTimeActive);
      return this;
    }

    /**
     * Set the subscribed flag for this channel
     *
     * @param subscribed the subscribed flag
     * @return this Builder object
     */
    public Builder subscribed(Boolean subscribed) {
      mChannel.subscribed(subscribed);
      return this;
    }

    /**
     * Set the public flag for this channel
     *
     * @param isPublic the public flag
     * @return this Builder object
     */
    public Builder setPublic(boolean isPublic) {
      mChannel.setPublic(isPublic);
      return this;
    }

    public Builder publishPermission(PublishPermission publishPermission) {
      mChannel.setPublishPermission(publishPermission);
      return this;
    }

    /**
     * Set the creation date for this channel
     *
     * @param creationDate the creation date
     * @return this Builder object
     */
    public Builder creationDate(Date creationDate) {
      mChannel.creationDate(creationDate);
      return this;
    }

    /**
     * Build the channel object
     *
     * @return the channel
     */
    public MMXChannel build() {
      return mChannel;
    }
  }

  private String mName;
  private String mSummary;
  private String mOwnerId;
  private Integer mNumberOfMessages;
  private Date mLastTimeActive;
  private boolean mPublic;
  private PublishPermission mPublishPermission;
  private Boolean mSubscribed;
  private Date mCreationDate;

  /**
   * Default constructor
   */
  private MMXChannel() {

  }

  /**
   * Set the name of this channel
   *
   * @param name the name
   * @return this MMXChannel object
   */
  MMXChannel name(String name) {
    this.mName = name;
    return this;
  }

  /**
   * The name of this channel
   *
   * @return the name
   */
  public String getName() {
    return mName;
  }

  /**
   * Set the summary of this channel
   *
   * @param summary the summary
   * @return this MMXChannel object
   */
  MMXChannel summary(String summary) {
    this.mSummary = summary;
    return this;
  }

  /**
   * The summary for this channel
   *
   * @return the summary
   */
  public String getSummary() {
    return mSummary;
  }

  /**
   * Set the owner for this channel
   *
   * @param ownerId the owner userId
   * @return this MMXChannel object
   */
  MMXChannel ownerId(String ownerId) {
    mOwnerId = ownerId;
    return this;
  }

  /**
   * The owner for this channel
   *
   * @return the owner, null if not yet retrieved
   */
  public String getOwnerId() {
    return mOwnerId;
  }

  /**
   * Set the number of messages for this channel
   *
   * @param numberOfMessages the number of messages
   * @return this MMXChannel object
   */
  MMXChannel numberOfMessages(Integer numberOfMessages) {
    mNumberOfMessages = numberOfMessages;
    return this;
  }

  /**
   * The number of messages for this channel
   *
   * @return the number of messages, null if not yet retrieved
   */
  public Integer getNumberOfMessages() {
    return mNumberOfMessages;
  }

  /**
   * Set the last active time for this channel
   *
   * @param lastTimeActive the last active time
   * @return this MMXChannel object
   */
  MMXChannel lastTimeActive(Date lastTimeActive) {
    mLastTimeActive = lastTimeActive;
    return this;
  }

  /**
   * The last active time for this channel
   *
   * @return the last active time, null if not yet retrieved
   */
  public Date getLastTimeActive() {
    return mLastTimeActive;
  }

  /**
   * Set the creation date for this channel
   *
   * @param creationDate
   * @return this MMXChannel object
   */
  MMXChannel creationDate(Date creationDate) {
    mCreationDate = creationDate;
    return this;
  }

  /**
   * The creation date for this channel
   *
   * @return the creation date
   */
  public Date getCreationDate() {
    return mCreationDate;
  }

  /**
   * The tags for this channel.  Possible failure code is:
   * {@link FailureCode#CHANNEL_NOT_FOUND} for no such channel.
   *
   * @param listener the success/failure listener for this call
   */
  public void getTags(final OnFinishedListener<HashSet<String>> listener) {
    MMXTask<HashSet<String>> task = new MMXTask<HashSet<String>>(MMX.getMMXClient(), MMX.getHandler()) {
      @Override
      public HashSet<String> doRun(MMXClient mmxClient) throws Throwable {
        validateClient(mmxClient);
        MMXPubSubManager psm = mmxClient.getPubSubManager();
        TopicAction.TopicTags topicTags = psm.getAllTags(getMMXTopic());
        return topicTags.getTags() != null ?
                new HashSet<String>(topicTags.getTags()) : null;
      }

      @Override
      public void onException(final Throwable exception) {
        MMX.getCallbackHandler().post(new Runnable() {
          public void run() {
            listener.onFailure(FailureCode.fromMMXFailureCode(FailureCode.DEVICE_ERROR, exception), exception);
          }
        });
      }

      @Override
      public void onResult(final HashSet<String> result) {
        MMX.getCallbackHandler().post(new Runnable() {
          public void run() {
            listener.onSuccess(result);
          }
        });
      }
    };
    task.execute();
  }

  /**
   * Set the tags for this channel.  Possible failure codes are:
   * {@link FailureCode#CHANNEL_NOT_FOUND} for no such channel,
   * {@link FailureCode#SERVER_ERROR} for server error.
   *
   * @param tags the tags for this channel or null to remove all tags
   * @param listener the success/failure listener for this call
   */
  public void setTags(final HashSet<String> tags, final OnFinishedListener<Void> listener) {
    MMXTask<MMXStatus> task = new MMXTask<MMXStatus>(MMX.getMMXClient(), MMX.getHandler()) {
      @Override
      public MMXStatus doRun(MMXClient mmxClient) throws Throwable {
        MMXPubSubManager psm = mmxClient.getPubSubManager();
        return psm.setAllTags(getMMXTopic(), tags != null ? new ArrayList<String>(tags) : null);
      }

      @Override
      public void onException(final Throwable exception) {
        MMX.getCallbackHandler().post(new Runnable() {
          public void run() {
            listener.onFailure(FailureCode.fromMMXFailureCode(FailureCode.DEVICE_ERROR, exception), exception);
          }
        });
      }

      @Override
      public void onResult(MMXStatus result) {
        if (result.getCode() == MMXStatus.SUCCESS) {
          MMX.getCallbackHandler().post(new Runnable() {
            public void run() {
              listener.onSuccess(null);
            }
          });
        } else {
          Log.e(TAG, "setTags(): received bad status from server: " + result);
          MMX.getCallbackHandler().post(new Runnable() {
            public void run() {
              listener.onFailure(FailureCode.fromMMXFailureCode(FailureCode.SERVER_ERROR, null), null);
            }
          });
        }
      }
    };
    task.execute();
  }

  private static MMXTopic getMMXTopic(boolean isPublic, String name, String ownerUsername) {
    if (isPublic) {
      return new MMXGlobalTopic(name);
    } else if (ownerUsername == null) {
      return new MMXPersonalTopic(name);
    } else {
      return new MMXUserTopic(ownerUsername, name);
    }
  }

  MMXTopic getMMXTopic() {
    return getMMXTopic(isPublic(), getName(), getOwnerId());
  }

  /**
   * Retrieve all of the messages for this channel within date range.  Possible
   * failure codes are: {@link FailureCode#CHANNEL_NOT_FOUND} for no such channel,
   * {@link FailureCode#CHANNEL_FORBIDDEN} for insufficient rights.
   *
   * @param startDate filter based on start date, or null for no filter
   * @param endDate filter based on end date, or null for no filter
   * @param limit the maximum number of messages to return
   * @param offset the offset of messages to return
   * @param ascending the chronological sort order of the results
   * @param listener the listener for the results of this operation
   */
  public void getMessages(final Date startDate, final Date endDate, final Integer limit, final Integer offset,
                          final boolean ascending, final OnFinishedListener<ListResult<MMXMessage>> listener) {
    final MMXTopic topic = getMMXTopic();
    MMXTask<ListResult<com.magnet.mmx.client.common.MMXMessage>> task =
            new MMXTask<ListResult<com.magnet.mmx.client.common.MMXMessage>> (MMX.getMMXClient(), MMX.getHandler()) {
      @Override
      public ListResult<com.magnet.mmx.client.common.MMXMessage> doRun(MMXClient mmxClient) throws Throwable {
        validateClient(mmxClient);
        MMXPubSubManager psm = mmxClient.getPubSubManager();
        MMXResult<List<com.magnet.mmx.client.common.MMXMessage>> messages =
                psm.getItems(topic, new TopicAction.FetchOptions()
                        .setSince(startDate)
                        .setUntil(endDate)
                        .setOffset(offset != null ? offset : 0)
                        .setMaxItems(limit != null ? limit : DEFAULT_LIMIT).setAscending(ascending));
        return new ListResult<com.magnet.mmx.client.common.MMXMessage>(
            messages.getTotal(), messages.getResult());
      }

      @Override
      public void onException(final Throwable exception) {
        if (listener != null) {
          MMX.getCallbackHandler().post(new Runnable() {
            public void run() {
              listener.onFailure(FailureCode.fromMMXFailureCode(FailureCode.DEVICE_ERROR, exception), exception);
            }
          });
        }
      }

      @Override
      public void onResult(final ListResult<com.magnet.mmx.client.common.MMXMessage> result) {
        if (listener == null) {
          return;
        }
        final ArrayList<MMXMessage> resultList = new ArrayList<MMXMessage>();
        if (result != null && result.items.size() > 0) {
          //convert MMXMessages
          for (com.magnet.mmx.client.common.MMXMessage message : result.items) {
            MMXMessage mmxMessage = MMXMessage.fromMMXMessage(getMMXTopic(), message);
            if (mmxMessage != null) {
              resultList.add(mmxMessage);
            }
          }
        }
        MMX.getCallbackHandler().post(new Runnable() {
          public void run() {
            listener.onSuccess(new ListResult<MMXMessage>(result.totalCount, resultList));
          }
        });
      }
    };
    task.execute();
  }
  
  /**
   * @hide
   * Retrieve the messages from this channel by message ID.
   * @param ids A set of message ID's
   * @param listener the listener for the results of this operation
   */
  public void getMessages(final Set<String> ids, final OnFinishedListener<Map<String, MMXMessage>> listener) {
    final MMXTopic topic = getMMXTopic();
    MMXTask<Map<String, com.magnet.mmx.client.common.MMXMessage>> task =
            new MMXTask<Map<String, com.magnet.mmx.client.common.MMXMessage>> (MMX.getMMXClient(), MMX.getHandler()) {
      @Override
      public Map<String, com.magnet.mmx.client.common.MMXMessage> doRun(MMXClient mmxClient) throws Throwable {
        validateClient(mmxClient);
        MMXPubSubManager psm = mmxClient.getPubSubManager();
        List<String> idList = Arrays.asList(ids.toArray(new String[ids.size()]));
        Map<String, com.magnet.mmx.client.common.MMXMessage> messages =
                psm.getItemsByIds(topic, idList);
        return messages;
      }

      @Override
      public void onException(final Throwable exception) {
        if (listener != null) {
          MMX.getCallbackHandler().post(new Runnable() {
            public void run() {
              listener.onFailure(FailureCode.fromMMXFailureCode(FailureCode.DEVICE_ERROR, exception), exception);
            }
          });
        }
      }

      @Override
      public void onResult(Map<String, com.magnet.mmx.client.common.MMXMessage> result) {
        if (listener == null) {
          return;
        }
        final Map<String, MMXMessage> resultMap = new HashMap<>();
        if (result != null && result.size() > 0) {
          //convert MMXMessages
          for (Map.Entry<String, com.magnet.mmx.client.common.MMXMessage> entry : result.entrySet()) {
            MMXMessage mmxMessage = MMXMessage.fromMMXMessage(getMMXTopic(), entry.getValue());
            if (mmxMessage != null) {
              resultMap.put(entry.getKey(), mmxMessage);
            }
          }
        }
        MMX.getCallbackHandler().post(new Runnable() {
          public void run() {
            listener.onSuccess(resultMap);
          }
        });
      }
    };
    task.execute();
  }
  
  /**
   * @hide
   * Delete the messages from this channel by the message ID.  Only the publisher
   * of the message can delete it.  The results contain a map of message ID as
   * key and status code as value.
   * @param ids A set of message ID's
   * @param listener the listener for the results of this operation
   */
  public void deleteMessages(final Set<String> ids,
                              final OnFinishedListener<Map<String, Integer>> listener) {
    final MMXTopic topic = getMMXTopic();
    MMXTask<Map<String, Integer>> task =
            new MMXTask<Map<String, Integer>> (MMX.getMMXClient(), MMX.getHandler()) {
      @Override
      public Map<String, Integer> doRun(MMXClient mmxClient) throws Throwable {
        validateClient(mmxClient);
        MMXPubSubManager psm = mmxClient.getPubSubManager();
        List<String> idList = Arrays.asList(ids.toArray(new String[ids.size()]));
        Map<String, Integer> results = psm.deleteItemsByIds(topic, idList);
        return results;
      }

      @Override
      public void onException(final Throwable exception) {
        if (listener != null) {
          MMX.getCallbackHandler().post(new Runnable() {
            public void run() {
              listener.onFailure(FailureCode.fromMMXFailureCode(
                      FailureCode.DEVICE_ERROR, exception), exception);
            }
          });
        }
      }

      @Override
      public void onResult(final Map<String, Integer> result) {
        if (listener != null) {
          MMX.getCallbackHandler().post(new Runnable() {
            public void run() {
              listener.onSuccess(result);
            }
          });
        }
      }
    };
    task.execute();
  }

  /**
   * Set the subscribed flag for this channel
   *
   * @param subscribed the subscribed flag
   * @return this MMXChannel object
   */
  MMXChannel subscribed(Boolean subscribed) {
    mSubscribed = subscribed;
    return this;
  }

  /**
   * Whether or not the current user is subscribed
   *
   * @return Boolean.TRUE if subscribed, null if not yet retrieved
   */
  public Boolean isSubscribed() {
    return mSubscribed;
  }

  /**
   * Whether or not the current user can publish to this channel
   *
   * @return true if the current user is allowed to publish to this channel
   */
  public boolean canPublish() {
    if (mPublishPermission == null) {
      throw new IllegalStateException("MMXChannel.canPublish(): " +
              "The publish permission cannot be null.  " +
              "This channel was improperly constructed.");
    }
    switch (mPublishPermission) {
      case OWNER_ONLY:
        return getOwnerId().equalsIgnoreCase(MMX.getCurrentUser().getUserIdentifier());
      case SUBSCRIBER:
        return isSubscribed();
      case ANYONE:
        return true;
      default:
        throw new IllegalArgumentException("MMXChannel.canPublish(): " +
                "Unknown publish permission type: " + mPublishPermission);
    }
  }

  /**
   * Set the public flag for this channel
   *
   * @param isPublic the public flag
   * @return this MMXChannel object
   */
  MMXChannel setPublic(boolean isPublic) {
    mPublic = isPublic;
    return this;
  }

  /**
   * Whether or not the current channel is public
   *
   * @return true if public, false if private
   */
  public boolean isPublic() {
    return mPublic;
  }

  MMXChannel setPublishPermission(PublishPermission publishPermission) {
    mPublishPermission = publishPermission;
    return this;
  }

  /**
   * Who can publish to this channel
   *
   * @see com.magnet.mmx.client.api.MMXChannel.PublishPermission
   * @return the PublishPermission for this channel
   */
  public PublishPermission getPublishPermission() {
    return mPublishPermission;
  }

  /**
   * Create a channel.  Upon successful completion, the current user
   * automatically subscribes to the channel.
   * Possible failure codes are: {@link FailureCode#BAD_REQUEST} for invalid
   * channel name, {@value FailureCode#CHANNEL_EXISTS} for existing channel.
   *
   * @param name the name of the channel
   * @param summary the channel summary
   * @param isPublic whether or not this channel is public
   * @param publishPermission who can publish to this topic
   * @param listener the listener for the newly created channel
   */
  public static void create(final String name, final String summary,
                            final boolean isPublic, final PublishPermission publishPermission,
                            final OnFinishedListener<MMXChannel> listener) {
    MMXTask<MMXTopic> task = new MMXTask<MMXTopic>(MMX.getMMXClient(), MMX.getHandler()) {
      @Override
      public MMXTopic doRun(MMXClient mmxClient) throws Throwable {
        validateClient(mmxClient);

        MMXPubSubManager psm = mmxClient.getPubSubManager();
        MMXTopicOptions options = new MMXTopicOptions()
                .setDescription(summary).setSubscribeOnCreate(true)
                .setPublisherType(publishPermission == null ?
                        TopicAction.PublisherType.anyone : publishPermission.type);
        MMXTopic topic = getMMXTopic(isPublic, name, MMX.getCurrentUser().getUserIdentifier());
        return psm.createTopic(topic, options);
      }

      @Override
      public void onException(final Throwable exception) {
        if (listener != null) {
          MMX.getCallbackHandler().post(new Runnable() {
            public void run() {
              listener.onFailure(FailureCode.fromMMXFailureCode(
                      FailureCode.DEVICE_ERROR, exception), exception);
            }
          });
        }
      }

      @Override
      public void onResult(final MMXTopic createResult) {
        if (listener != null) {
          try {
            MMXTopicInfo topicInfo = MMX.getMMXClient().getPubSubManager().getTopic(createResult);
            ArrayList<MMXTopicInfo> topicInfos = new ArrayList<MMXTopicInfo>();
            topicInfos.add(topicInfo);
            final List<MMXChannel> channels = fromTopicInfos(topicInfos, null);
            if (channels.size() != 1) {
              throw new IllegalStateException("Invalid number of results.");
            }
            MMX.getCallbackHandler().post(new Runnable() {
              public void run() {
                listener.onSuccess(channels.get(0));
              }
            });
          } catch (Exception ex) {
            Log.w(TAG, "create(): create succeeded, but unable to retrieve hydrated channel for onSuccess(), " +
                    "falling back to less-populated channel.", ex);
            MMX.getCallbackHandler().post(new Runnable() {
              public void run() {
                listener.onSuccess(MMXChannel.fromMMXTopic(createResult)
                        .ownerId(MMX.getCurrentUser().getUserIdentifier())
                        .summary(summary));
              }
            });
          }

        }
      }
    };
    task.execute();
  }

  /**
   * Create a channel with predefined subscribers.  Upon successful completion, the current user
   * automatically subscribes to the channel.
   * Possible failure codes are: {@link FailureCode#BAD_REQUEST} for invalid
   * channel name, {@value FailureCode#CHANNEL_EXISTS} for existing channel.
   *
   * @param name the name of the channel
   * @param summary the channel summary
   * @param isPublic whether or not this channel is public
   * @param publishPermission who can publish to this topic
   * @param subscribers the user id of subscribers
   * @param listener the listener for the newly created channel
   */
  public static void create(final String name, final String summary,
      final boolean isPublic, final PublishPermission publishPermission,
      final Set<String> subscribers,
      final OnFinishedListener<MMXChannel> listener) {
    final String ownerId = User.getCurrentUserId();
    getChannelService().createChannel(
        new ChannelService.ChannelInfo(name, summary, !isPublic, publishPermission.type.name(), subscribers),
        new Callback<Void>() {
          @Override public void onResponse(Response<Void> response) {
            if(response.isSuccess()) {
              Date currentDate = new Date();
              MMXChannel channel = new MMXChannel.Builder()
                  .name(name)
                  .summary(summary)
                  .ownerId(ownerId)
                  .setPublic(isPublic)
                  .publishPermission(publishPermission)
                  .creationDate(currentDate)
                  .subscribed(true)
                  .lastTimeActive(currentDate)
                  .build();
              listener.onSuccess(channel);
            } else {
              handleError(response.code(), new Exception(response.message()));
            }
          }

          @Override public void onFailure(Throwable throwable) {
            handleError(null, throwable);
          }

          private void handleError(Integer errorCode, Throwable throwable) {
            Log.e(TAG, "Failed to create channel ", throwable);
            listener.onFailure(new FailureCode(null != errorCode ? errorCode : -1, throwable.getLocalizedMessage()), throwable);
          }
        }).executeInBackground();
  }

  ///**
  // * Create a channel with predefined subscribers.  Upon successful completion, the current user
  // * automatically subscribes to the channel.
  // * Possible failure codes are: {@link FailureCode#BAD_REQUEST} for invalid
  // * channel name, {@value FailureCode#CHANNEL_EXISTS} for existing channel.
  // *
  // * @param name the name of the channel
  // * @param summary the channel summary
  // * @param isPublic whether or not this channel is public
  // * @param publishPermission who can publish to this topic
  // * @param subscribers the user id of subscribers
  // * @param listener the listener for the newly created channel
  // */
  //public static void create(final String name, final String summary,
  //    final boolean isPublic, final PublishPermission publishPermission,
  //    final Set<User> subscribers,
  //    final OnFinishedListener<MMXChannel> listener) {
  //  Set<String> userIds = new HashSet<>();
  //  if(null == subscribers || subscribers.isEmpty()) {
  //    for(User u : subscribers) {
  //      userIds.add(u.getUserIdentifier());
  //    }
  //  }
  //  final String ownerId = User.getCurrentUserId();
  //  getChannelService().createChannel(
  //      new ChannelService.ChannelInfo(name, summary, !isPublic, publishPermission.type.name(), userIds),
  //      new Callback<Void>() {
  //        @Override public void onResponse(Response<Void> response) {
  //          if(response.isSuccess()) {
  //            Date currentDate = new Date();
  //            MMXChannel channel = new MMXChannel.Builder()
  //                .name(name)
  //                .summary(summary)
  //                .ownerId(ownerId)
  //                .setPublic(isPublic)
  //                .publishPermission(publishPermission)
  //                .creationDate(currentDate)
  //                .subscribed(true)
  //                .lastTimeActive(currentDate)
  //                .build();
  //            listener.onSuccess(channel);
  //          } else {
  //            handleError(response.code(), new Exception(response.message()));
  //          }
  //        }
  //
  //        @Override public void onFailure(Throwable throwable) {
  //          handleError(null, throwable);
  //        }
  //
  //        private void handleError(Integer errorCode, Throwable throwable) {
  //          Log.e(TAG, "Failed to create channel ", throwable);
  //          listener.onFailure(new FailureCode(null != errorCode ? errorCode : -1, throwable.getLocalizedMessage()), throwable);
  //        }
  //      }).executeInBackground();
  //}

  /**
   * Delete this channel.  Possible failure codes are: {@link FailureCode#CHANNEL_NOT_FOUND}
   * for no such channel, {@link FailureCode#CHANNEL_FORBIDDEN} for
   * insufficient rights.
   *
   * @param listener the listener for success or failure
   */
  public void delete(final OnFinishedListener<Void> listener) {
    MMXTask<MMXStatus> task = new MMXTask<MMXStatus> (MMX.getMMXClient(), MMX.getHandler()) {
      @Override
      public MMXStatus doRun(MMXClient mmxClient) throws Throwable {
        validateClient(mmxClient);
        MMXPubSubManager psm = mmxClient.getPubSubManager();
        return psm.deleteTopic(getMMXTopic());
      }

      @Override
      public void onResult(MMXStatus result) {
        if (listener == null) {
          return;
        }
        if (result.getCode() == MMXStatus.SUCCESS) {
          MMX.getCallbackHandler().post(new Runnable() {
            public void run() {
              listener.onSuccess(null);
            }
          });
        } else {
          MMX.getCallbackHandler().post(new Runnable() {
            public void run() {
              listener.onFailure(FailureCode.fromMMXFailureCode(FailureCode.SERVER_ERROR, null), null);
            }
          });
        }
      }

      @Override
      public void onException(final Throwable exception) {
        if (listener != null) {
          MMX.getCallbackHandler().post(new Runnable() {
            public void run() {
              listener.onFailure(FailureCode.fromMMXFailureCode(FailureCode.DEVICE_ERROR, exception),
                      exception);
            }
          });
        }
      }
    };
    task.execute();
  }

  /**
   * Subscribes the current user from this channel
   *
   * @param listener the listener for the subscription id
   */
  public void subscribe(final OnFinishedListener<String> listener) {
    //TODO : if already subscribed
    //if(isSubscribed()) {
    //
    //}

    MMXTask<String> task = new MMXTask<String>(MMX.getMMXClient(), MMX.getHandler()) {
      @Override
      public String doRun(MMXClient mmxClient) throws Throwable {
        MMXPubSubManager psm = mmxClient.getPubSubManager();
        return psm.subscribe(getMMXTopic(), false);
      }

      @Override
      public void onException(final Throwable exception) {
        if (listener != null) {
          MMX.getCallbackHandler().post(new Runnable() {
            public void run() {
              listener.onFailure(FailureCode.fromMMXFailureCode(FailureCode.DEVICE_ERROR, exception), exception);
            }
          });
        }
      }

      @Override
      public void onResult(final String result) {
        MMXChannel.this.subscribed(true);
        if (listener != null) {
          MMX.getCallbackHandler().post(new Runnable() {
            public void run() {
              listener.onSuccess(result);
            }
          });
        }
      }
    };
    task.execute();
  }

  /**
   * Unsubscribes the current user from this channel.
   *
   * @param listener the listener for success or failure
   */
  public void unsubscribe(final OnFinishedListener<Boolean> listener) {
    MMXTask<Boolean> task = new MMXTask<Boolean>(MMX.getMMXClient(), MMX.getHandler()) {
      @Override
      public Boolean doRun(MMXClient mmxClient) throws Throwable {
        MMXPubSubManager psm = mmxClient.getPubSubManager();
        //unsubscribe from all devices
        return psm.unsubscribe(getMMXTopic(), null);
      }

      @Override
      public void onException(final Throwable exception) {
        if (listener != null) {
          MMX.getCallbackHandler().post(new Runnable() {
            public void run() {
              listener.onFailure(FailureCode.fromMMXFailureCode(FailureCode.DEVICE_ERROR, exception), exception);
            }
          });
        }
      }

      @Override
      public void onResult(final Boolean result) {
        MMXChannel.this.subscribed(false);
        if (listener != null) {
          MMX.getCallbackHandler().post(new Runnable() {
            public void run() {
              listener.onSuccess(result);
            }
          });
        }
      }
    };
    task.execute();
  }

  /**
   * Publishes a message to this channel.  Possible failure codes are:
   * {@link FailureCode#CHANNEL_NOT_FOUND} for no such channel,
   * {@link FailureCode#CHANNEL_FORBIDDEN} for insufficient rights,
   * {@link FailureCode#CONTENT_TOO_LARGE} for content being too large.
   *
   * @param messageContent the message content to publish
   * @param listener the listener for the message id
   * @return the message id for this published message
   */
  public String publish(Map<String, String> messageContent,
      final OnFinishedListener<String> listener) {
    if(null != messageContent && !messageContent.isEmpty()) {
      MMXMessage message = new MMXMessage.Builder().channel(this).content(messageContent).build();
      return message.publish(listener);
    } else {
      if(null != listener) {
        MMX.getCallbackHandler().post(new Runnable() {
          @Override
          public void run() {
            listener.onFailure(FailureCode.CONTENT_EMPTY, null);
          }
        });
      }

      return null;
    }
  }

  /**
   * Publishes a message to this channel.  Possible failure codes are:
   * {@link FailureCode#CHANNEL_NOT_FOUND} for no such channel,
   * {@link FailureCode#CHANNEL_FORBIDDEN} for insufficient rights,
   * {@link FailureCode#CONTENT_TOO_LARGE} for content being too large.
   *
   * @param message the message to publish
   * @param listener the listener for the message id
   * @return the message id for this published message
   */
  public String publish(MMXMessage message,
      final OnFinishedListener<String> listener) {
    message.channel(this);
    return message.publish(listener);
  }

  /**
   * Sends an invitation to the specified user for this channel.  Possible
   * failure code is: {@link FailureCode#INVALID_INVITEE}
   *
   * @param invitee the invitee
   * @param invitationText the text to include in the invite
   * @param listener the listener for success/failure of this operation
   */
  public void inviteUser(final User invitee, final String invitationText,
                         final OnFinishedListener<MMXInvite> listener) {
    Set<User> invitees = new HashSet<User>(1);
    invitees.add(invitee);
    inviteUsers(invitees, invitationText, listener);
  }

  /**
   * Sends an invitation to a set of users for this channel. The listener will
   * be invoked one per invitee.  If an invitee is invalid,
   * the failure code will be {@link FailureCode#INVALID_INVITEE}.
   * @param invitees A set of invitees
   * @param invitationText the text to include in the invite
   * @param listener the listener for success/failure of this operation
   */
  public void inviteUsers(final Set<User> invitees, final String invitationText,
                          final OnFinishedListener<MMXInvite> listener) {
    MMXInviteInfo inviteInfo = new MMXInviteInfo(invitees, MMX.getCurrentUser(), this, invitationText);
    MMXInvite invite = new MMXInvite(inviteInfo, false);
    invite.send(listener);
  }

  /**
   * Add subscribers to the channel.
   * Only channel owner has the permission for private channel.
   * Channel owner and subscribers have the permission for public channel.
   * The listener returns the user ids which fail to be added to the channel
   * @param newSubscribers
   * @param listener
   */
  public void addSubscribers(final Set<User> newSubscribers, final OnFinishedListener<List<String>> listener) {
    if(checkIfAllowedToAddSubscriber(listener)) {
      Set<String> userIds = userSetToIds(newSubscribers, listener);
      if (null != userIds) {
        getChannelService().addSubscriberToChannel(mName,
            new ChannelService.ChannelSubscribeRequest(!mPublic, userIds), new Callback<ChannelService.ChannelSubscribeResponse>() {
              @Override public void onResponse(Response<ChannelService.ChannelSubscribeResponse> response) {
                if (response.isSuccess()) {
                  if (null != listener) {
                    listener.onSuccess(getInvalidSubscribes(response.body()));
                  }
                } else {
                  handleSubscribeError(FailureCode.SUBSCRIPTION_ADD_FAILURE, new Exception(response.message()), listener);
                }
              }

              @Override public void onFailure(Throwable throwable) {
                handleSubscribeError(FailureCode.SUBSCRIPTION_ADD_FAILURE, throwable, listener);
              }
            }).executeInBackground();
      }
    }
  }

  /**
   * Remove subscribers from the channel.
   * Only channel owner has the permission for this operation.
   * The listener returns the user ids which fail to be removed from the channel
   * @param subscribersToBeRemoved
   * @param listener
   */
  public void removeSubscribers(final Set<User> subscribersToBeRemoved, final OnFinishedListener<List<String>> listener) {
    if(checkIfAllowedToRemoveSubscriber(listener)){
      Set<String> userIds = userSetToIds(subscribersToBeRemoved, listener);
      if (null != userIds) {
        getChannelService().removeSubscriberFromChannel(mName,
            new ChannelService.ChannelSubscribeRequest(!mPublic, userIds), new Callback<ChannelService.ChannelSubscribeResponse>() {
              @Override public void onResponse(Response<ChannelService.ChannelSubscribeResponse> response) {
                if (response.isSuccess()) {
                  if (null != listener) {
                    listener.onSuccess(getInvalidSubscribes(response.body()));
                  }
                } else {
                  handleSubscribeError(FailureCode.SUBSCRIPTION_REMOVE_FAILURE, new Exception(response.message()), listener);
                }
              }

              @Override public void onFailure(Throwable throwable) {
                handleSubscribeError(FailureCode.SUBSCRIPTION_REMOVE_FAILURE, throwable, listener);
              }
            }).executeInBackground();
      }
    }
  }

  private static Set<String> userSetToIds(Set<User> users, final OnFinishedListener<List<String>> listener) {
    if(null == users || users.isEmpty()) {
      if(null != listener) {
        listener.onSuccess(Collections.EMPTY_LIST);
      }
      return null;
    }

    Set<String> userIds = new HashSet<>();
    for(User u : users) {
      userIds.add(u.getUserIdentifier());
    }

    return userIds;
  }

  private List<String> getInvalidSubscribes(ChannelService.ChannelSubscribeResponse subscribeResponse) {
    List<String> result = null;
    if(null != subscribeResponse && null != subscribeResponse.getSubscribeResponse() &&
        !subscribeResponse.getSubscribeResponse().isEmpty()) {
      result = new ArrayList<>(subscribeResponse.getSubscribeResponse().size());
      for(Map.Entry<String, ChannelService.ChannelSubscribeResponse.SubscribeResult> e : subscribeResponse.getSubscribeResponse().entrySet()) {
        if(!e.getValue().isSuccess()) {
          result.add(e.getKey());
        }
      }
    } else {
      result = Collections.EMPTY_LIST;
    }

    return result;
  }

  private void handleSubscribeError(FailureCode failureCode, Throwable throwable, OnFinishedListener<List<String>> listener) {
    Log.e(TAG, "Failed to manage subscriber " + failureCode.getDescription(), throwable);
    if(null != listener) {
      listener.onFailure(failureCode, throwable);
    }
  }

  private boolean checkIfAllowedToAddSubscriber(OnFinishedListener<List<String>> listener) {
    // Currently only owner is allowed
    boolean isOwner = StringUtil.isStringValueEqual(getOwnerId(), User.getCurrentUserId());
    if(mPublic && !mSubscribed) {
      String errorMessage = "Only owner and subscriber is allowed to add subscribers for public channel";
      Log.e(TAG, errorMessage);
      if(null != listener) {
        listener.onFailure(FailureCode.CHANNEL_FORBIDDEN, new IllegalStateException(errorMessage));
      }
      return false;
    } else if(!mPublic && !isOwner) {
      String errorMessage = "Only owner is allowed to add subscribers for private channel";
      Log.e(TAG, errorMessage);
      if(null != listener) {
        listener.onFailure(FailureCode.CHANNEL_FORBIDDEN, new IllegalStateException(errorMessage));
      }
      return false;
    }

    return true;
  }

  private boolean checkIfAllowedToRemoveSubscriber(OnFinishedListener<List<String>> listener) {
    // Currently only owner is allowed
    boolean isOwner = StringUtil.isStringValueEqual(getOwnerId(), User.getCurrentUserId());
    if(!isOwner) {
      String errorMessage = "Only owner is allowed to remove subscribers for channel";
      Log.e(TAG, errorMessage);
      listener.onFailure(FailureCode.CHANNEL_FORBIDDEN, new IllegalStateException(errorMessage));
      return false;
    }

    return true;
  }

  /**
   * Retrieves all the subscribers for this channel.  Possible failure codes are:
   * {@link FailureCode#CHANNEL_NOT_FOUND} for no such channel,
   * {@link FailureCode#CHANNEL_FORBIDDEN} for insufficient rights.
   * @param limit the maximum number of subscribers to return
   * @param offset the offset of subscribers to return
   * @param listener the listener for the subscribers
   */
  public void getAllSubscribers(final Integer limit, final Integer offset, final OnFinishedListener<ListResult<User>> listener) {
    MMXTask<MMXResult<List<UserInfo>>> task =
            new MMXTask<MMXResult<List<UserInfo>>> (MMX.getMMXClient(), MMX.getHandler()) {
      @Override
      public MMXResult<List<UserInfo>> doRun(MMXClient mmxClient) throws Throwable {
        validateClient(mmxClient);
        MMXPubSubManager psm = mmxClient.getPubSubManager();
        return psm.getSubscribers(getMMXTopic(), offset != null ? offset : 0, limit != null ? limit : DEFAULT_LIMIT);
      }

      @Override
      public void onException(final Throwable exception) {
        if (listener != null) {
          MMX.getCallbackHandler().post(new Runnable() {
            public void run() {
              listener.onFailure(FailureCode.fromMMXFailureCode(FailureCode.DEVICE_ERROR, exception), exception);
            }
          });
        }
      }

      @Override
      public void onResult(final MMXResult<List<UserInfo>> result) {
        if (listener == null) {
          return;
        }

        HashSet<String> usersToRetrieve = new HashSet<String>();
        for (UserInfo userInfo : result.getResult()) {
          usersToRetrieve.add(userInfo.getUserId());
        }
        //This shouldn't be the main thread so we can fillCache.
        UserCache userCache = UserCache.getInstance();
        userCache.fillCacheByUserId(usersToRetrieve, UserCache.DEFAULT_ACCEPTED_AGE);
        final ArrayList<User> users = new ArrayList<User>();
        for (String userId : usersToRetrieve) {
          User user = userCache.getByUserId(userId);
          if (user == null) {
            Log.e(TAG, "getAllSubscribers(): failing because unable to retrieve user info for subscriber: " + userId);
            MMX.getCallbackHandler().post(new Runnable() {
              public void run() {
                listener.onFailure(FailureCode.fromMMXFailureCode(FailureCode.SERVER_ERROR, null), null);
              }
            });
            return;
          }
          users.add(user);
        }
        MMX.getCallbackHandler().post(new Runnable() {
          public void run() {
            listener.onSuccess(new ListResult<User>(result.getTotal(), users));
          }
        });
      }
    };
    task.execute();
  }

  /**
   * Get a public channel with an exact name.  The channels created by anyone
   * can be retrieved.  The channel name is case insensitive.
   * @param name A public channel name.
   * @param listener the listener for getting the channel.
   */
  public static void getPublicChannel(final String name,
                            final OnFinishedListener<MMXChannel> listener) {
    getChannel(name, true, listener);
  }
  
  /**
   * Get a private channel with an exact name.  Only the channels created by
   * the current user can be retrieved.  The channel name is case insensitive.
   * @param name A private channel name.
   * @param listener the listener for getting the channel
   */
  public static void getPrivateChannel(final String name,
                            final OnFinishedListener<MMXChannel> listener) {
    getChannel(name, false, listener);
  }
  
  // Get a public or private channel by its name.
  private static void getChannel(final String name, final boolean publicOnly,
                  final OnFinishedListener<MMXChannel> listener) {
    MMXTask<MMXChannel> task = new MMXTask<MMXChannel>(MMX.getMMXClient(), MMX.getHandler()) {
      @Override
      public MMXChannel doRun(MMXClient mmxClient) throws Throwable {
        validateClient(mmxClient);
        MMXPubSubManager psm = mmxClient.getPubSubManager();
        MMXTopicInfo info = psm.getTopic(publicOnly ? 
            new MMXGlobalTopic(name) : new MMXPersonalTopic(name));
        List<MMXTopicInfo> infos = Arrays.asList(new MMXTopicInfo[] { info });
        return fromTopicInfos(infos, null).get(0);
      }

      @Override
      public void onResult(final MMXChannel result) {
        if (listener != null) {
          MMX.getCallbackHandler().post(new Runnable() {
            public void run() {
              listener.onSuccess(result);
            }
          });
        }
      }

      @Override
      public void onException(final Throwable exception) {
        if (listener != null) {
          MMX.getCallbackHandler().post(new Runnable() {
            public void run() {
              listener.onFailure(FailureCode.fromMMXFailureCode(FailureCode.DEVICE_ERROR, exception), exception);
            }
          });
        } else {
          Log.e(TAG, "Get channel by name failed", exception);
        }
      }
    };
    task.execute();
  }
  
  /**
   * Get all public channels with pagination.
   * @param limit the maximum number of results to return
   * @param offset the offset of results to return
   * @param listener the listener for the query results
   */
  public static void getAllPublicChannels(Integer limit, Integer offset,
                                          final OnFinishedListener<ListResult<MMXChannel>> listener) {
    findChannelsByName("%", limit, offset, ListType.global, listener);
  }

  /**
   * Get all private channels with pagination.  Only the private channels
   * created by the current user can be retrieved.
   * @param limit the maximum number of results to return
   * @param offset the offset of results to return
   * @param listener the listener for the query results
   */
  public static void getAllPrivateChannels(Integer limit, Integer offset,
          final OnFinishedListener<ListResult<MMXChannel>> listener) {
    findChannelsByName("%", limit, offset, ListType.personal, listener);
  }

  /**
   * Find the public channels that start with the specified text.  If there are
   * no matching names, {@link OnFinishedListener#onSuccess(Object)} will return
   * an empty list.
   *
   * @param startsWith the search string
   * @param limit the maximum number of results to return
   * @param offset the offset of results to return
   * @param listener the listener for the query results
   */
  public static void findPublicChannelsByName(final String startsWith, final Integer limit,
      final Integer offset, final OnFinishedListener<ListResult<MMXChannel>> listener) {
    findChannelsByName(startsWith, limit, offset, ListType.global, listener);
  }

  /**
   * Find private channels that start with the specified text.  If there are
   * no matching names, {@link OnFinishedListener#onSuccess(Object)} will return
   * an empty list.
   *
   * @param startsWith the search string
   * @param limit the maximum number of results to return
   * @param offset the offset of results to return
   * @param listener the listener for the query results
   */
  public static void findPrivateChannelsByName(final String startsWith, final Integer limit,
      final Integer offset, final OnFinishedListener<ListResult<MMXChannel>> listener) {
    findChannelsByName(startsWith, limit, offset, ListType.personal, listener);
  }

  // Find public or private channels whose names start with the specified text.
  private static void findChannelsByName(final String startsWith,
                                         final Integer limit, final Integer offset, final ListType listType,
      final OnFinishedListener<ListResult<MMXChannel>> listener) {
    MMXTask<ListResult<MMXChannel>> task = new MMXTask<ListResult<MMXChannel>>(
            MMX.getMMXClient(), MMX.getHandler()) {
      @Override
      public ListResult<MMXChannel> doRun(MMXClient mmxClient) throws Throwable {
        validateClient(mmxClient);
        if (startsWith == null || startsWith.isEmpty()) {
          throw new MMXException("Search string cannot be null or empty",
              FailureCode.BAD_REQUEST.getValue());
        }
        MMXPubSubManager psm = mmxClient.getPubSubManager();
        TopicAction.TopicSearch search = new TopicAction.TopicSearch()
                .setTopicName(startsWith, SearchAction.Match.PREFIX);
        MMXTopicSearchResult searchResult = psm.searchBy(SearchAction.Operator.AND,
            search, offset != null ? offset : 0, limit != null ? limit : DEFAULT_LIMIT, listType);
        List<MMXChannel> channels = fromTopicInfos(searchResult.getResults(), null);
        return new ListResult<MMXChannel>(searchResult.getTotal(), channels);
      }

      @Override
      public void onResult(final ListResult<MMXChannel> result) {
        //build the query result
        if (listener != null) {
          MMX.getCallbackHandler().post(new Runnable() {
            public void run() {
              listener.onSuccess(result);
            }
          });
        }
      }

      @Override
      public void onException(final Throwable exception) {
        if (listener != null) {
          MMX.getCallbackHandler().post(new Runnable() {
            public void run() {
              listener.onFailure(FailureCode.fromMMXFailureCode(FailureCode.DEVICE_ERROR, exception), exception);
            }
          });
        } else {
          Log.e(TAG, "Find channel by name failed", exception);
        }
      }
    };
    task.execute();
  }

  /**
   * Query for the specified tags (inclusive.)  If there are no matching tags,
   * {@link OnFinishedListener#onSuccess(Object)} will return an empty list.
   *
   * @param tags the tags to match
   * @param limit the maximum number of results to return
   * @param offset the offset of results to return
   * @param listener the listener for the query results
   */
  public static void findByTags(final Set<String> tags, final Integer limit, final Integer offset,
                                final OnFinishedListener<ListResult<MMXChannel>> listener) {
    MMXTask<ListResult<MMXChannel>> task = new MMXTask<ListResult<MMXChannel>>(
            MMX.getMMXClient(), MMX.getHandler()) {
      @Override
      public ListResult<MMXChannel> doRun(MMXClient mmxClient) throws Throwable {
        validateClient(mmxClient);
        MMXPubSubManager psm = mmxClient.getPubSubManager();
        TopicAction.TopicSearch search = new TopicAction.TopicSearch()
                .setTags(new ArrayList<String>(tags));
        MMXTopicSearchResult searchResult = psm.searchBy(SearchAction.Operator.AND, search,
            offset != null ? offset : 0, limit != null ? limit : DEFAULT_LIMIT, ListType.global);
        List<MMXChannel> channels =fromTopicInfos(searchResult.getResults(), null);
        return new ListResult<MMXChannel>(searchResult.getTotal(), channels);
      }

      @Override
      public void onResult(final ListResult<MMXChannel> result) {
        //build the query result
        if (listener != null) {
          MMX.getCallbackHandler().post(new Runnable() {
            public void run() {
              listener.onSuccess(result);
            }
          });
        }
      }

      @Override
      public void onException(final Throwable exception) {
        if (listener != null) {
          MMX.getCallbackHandler().post(new Runnable() {
            public void run() {
              listener.onFailure(FailureCode.fromMMXFailureCode(FailureCode.DEVICE_ERROR, exception), exception);
            }
          });
        }
      }
    };
    task.execute();
  }

  /**
   * Returns a list of the channels to which the current user is subscribed.
   *
   * @param listener the results listener for this operation
   */
  public static void getAllSubscriptions(final OnFinishedListener<List<MMXChannel>> listener) {
    MMXTask<List<MMXChannel>> task = new MMXTask<List<MMXChannel>>(MMX.getMMXClient(), MMX.getHandler()) {
      @Override
      public List<MMXChannel> doRun(MMXClient mmxClient) throws Throwable {
        validateClient(mmxClient);
        MMXPubSubManager psm = mmxClient.getPubSubManager();
        return fromSubscriptions(psm.listAllSubscriptions());
      }

      @Override
      public void onResult(final List<MMXChannel> result) {
        if (listener != null) {
          MMX.getCallbackHandler().post(new Runnable() {
            public void run() {
              listener.onSuccess(result);
            }
          });
        }
      }

      @Override
      public void onException(final Throwable exception) {
        if (listener != null) {
          MMX.getCallbackHandler().post(new Runnable() {
            public void run() {
              listener.onFailure(FailureCode.fromMMXFailureCode(MMX.FailureCode.DEVICE_ERROR,exception),exception);
            }
          });
        }
      }
    };
    task.execute();
  }

  /**
   * Find channels that include given subscriber(s). If there is no channel found,
   * {@link OnFinishedListener#onSuccess(Object)} will return an empty list.
   *
   * @param subscribers the subscribers
   * @param matchType
   * @param listener the listener for the query results
   */
  public static void findChannelsBySubscribers(final Set<User> subscribers, ChannelMatchType matchType,
      final OnFinishedListener<ListResult<MMXChannel>> listener) {
    Set<String> userIds = userSetToIds(subscribers, null);
    if(null != userIds) {
      getChannelService().queryChannelsBySubscribers(
          new ChannelService.ChannelBySubscriberRequest(userIds, matchType),
          new Callback<ChannelService.ChannelQueryResponse>() {
            @Override public void onResponse(Response<ChannelService.ChannelQueryResponse> response) {
              if(response.isSuccess() && response.body().isSuccess()) {
                List<MMXChannel> items = null;
                if(null != response.body().getChannels() && null != listener) {
                  items = new ArrayList<MMXChannel>(response.body().getChannels().size());
                  for(ChannelService.ChannelInfoResponse ci : response.body().getChannels()) {
                    items.add(ci.toMMXChannel());
                  }
                } else {
                  items = Collections.EMPTY_LIST;
                }

                if(null != listener) {
                  listener.onSuccess(new ListResult<MMXChannel>(items.size(), items));
                }
              } else {
                handleError(new Exception(response.message()), listener);
              }
            }

            @Override public void onFailure(Throwable throwable) {
              handleError(throwable, listener);
            }

            private void handleError(Throwable throwable, OnFinishedListener listener) {
              Log.e(TAG, "Failed to queryChannelsBySubscribers due to " + throwable.getMessage());
              if(null != listener) {
                listener.onFailure(FailureCode.GENERIC_FAILURE, throwable);
              }
            }
          }).executeInBackground();
    } else {
      throw new IllegalArgumentException("Subscribers is empty");
    }
  }

  /**
   * Get detail for channels
   * @param channels
   * @param options
   * @param listener
   */
  public static void getChannelDetail(final List<MMXChannel> channels, ChannelDetailOptions options, final OnFinishedListener<List<ChannelDetail>> listener) {
    if(null == channels || channels.isEmpty()) {
      if(null != listener) {
        listener.onSuccess(Collections.EMPTY_LIST);
      }
    } else {

      getChannelService().getChannelSummary(new ChannelSummaryRequest(channels, options),
          new Callback<List<ChannelSummaryResponse>>() {
        @Override public void onResponse(final Response<List<ChannelSummaryResponse>> response) {
          if(response.isSuccess()) {
            if(null != listener) {
              // Do the conversion in background task
              MMXTask<List<ChannelDetail>> task = new MMXTask<List<ChannelDetail>>(MMX.getMMXClient(), MMX.getHandler()) {
                @Override
                public List<ChannelDetail> doRun(MMXClient mmxClient) throws Throwable {
                  List<ChannelSummaryResponse> summaryResponses = response.body();
                  List<ChannelDetail> channelDetails = null;
                  if(null != summaryResponses) {
                    channelDetails = new ArrayList<ChannelDetail>(summaryResponses.size());

                    //Pre-populate user cache using batch api
                    Set<String> userIds = new HashSet<>();
                    for(ChannelSummaryResponse cr : summaryResponses) {
                      if(null != cr.getMessages()) {
                        for (PubSubItem item : cr.getMessages()) {
                          if (null != item.getPublisher() && StringUtil.isNotEmpty(
                              item.getPublisher().getUserId())) {
                            userIds.add(item.getPublisher().getUserId());
                          }
                        }
                      }
                      //Owner
                      if(null != cr.getUserId()) {
                        userIds.add(cr.getUserId());
                      }
                    }
                    UserCache.getInstance().fillCacheByUserId(userIds);

                    for(int i = 0; i < summaryResponses.size(); i++) {
                      ChannelSummaryResponse r = summaryResponses.get(i);

                      //convert messages
                      List<PubSubItem> pubsubItems = r.getMessages();
                      List<MMXMessage> mmxMessages = new ArrayList<MMXMessage>(pubsubItems.size());
                      if(null != pubsubItems) {
                        for(PubSubItem item : pubsubItems) {
                          MMXMessage message = MMXMessage.fromPubSubItem(item);
                          if(null != message) {
                            mmxMessages.add(message);
                          }
                        }
                      }

                      List<UserProfile> userProfiles = null;
                      if(null != r.getSubscribers()) {
                        userProfiles = new ArrayList<UserProfile>(r.getSubscribers().size());
                        for(BasicUserInfo ui : r.getSubscribers()) {
                          userProfiles.add(ui.toUserProfile());
                        }
                      }

                      //FIXME : have to lookup because the order sometimes doesn't match in request/response
                      MMXChannel forChannel = null;
                      for(MMXChannel c : channels) {
                        if((null == r.getUserId() || c.getOwnerId().equals(r.getUserId()))
                            && c.getName().equals(r.getChannelName())) {
                          forChannel = c;
                          break;
                        }
                      }
                      if(null != forChannel) {
                        channelDetails.add(new ChannelDetail.Builder().channel(forChannel)
                            .messages(mmxMessages)
                            .subscribers(userProfiles)
                            .totalMessages(r.getPublishedItemCount())
                            .totalSubscribers(r.getSubscriberCount())
                            .owner(UserCache.getInstance().getByUserId(forChannel.getOwnerId()))
                            .build());
                      } else {
                        Log.e(TAG, "Couldn't find channel detail for channel " + channels.get(i));
                      }
                    }
                  } else {
                    channelDetails = Collections.EMPTY_LIST;
                  }

                  return channelDetails;
                }

                @Override
                public void onResult(final List<ChannelDetail> result) {
                    MMX.getCallbackHandler().post(new Runnable() {
                      public void run() {
                        listener.onSuccess(result);
                      }
                    });
                }

                @Override
                public void onException(final Throwable exception) {
                  if (listener != null) {
                    MMX.getCallbackHandler().post(new Runnable() {
                      public void run() {
                        listener.onFailure(FailureCode.fromMMXFailureCode(FailureCode.GENERIC_FAILURE, exception),
                            exception);
                      }
                    });
                  }
                }
              };
              task.execute();
            }
          } else {
            handleError(new ApiError(response.message(), response.code()), listener);
          }
        }

        @Override public void onFailure(Throwable throwable) {
          handleError(throwable, listener);
        }

        private void handleError(Throwable error, OnFinishedListener listener) {
          Log.e(TAG, "Failed to getChannelDetail", error);
          if(null != listener) {
            listener.onFailure(FailureCode.GENERIC_FAILURE, error);
          }
        }
      }).executeInBackground();
    }
  }

  /**
   * Set a icon for the channel
   * @param imageBitmap
   * @param mimeType
   * @param listener
   */
  public void setIcon(Bitmap imageBitmap, String mimeType, final ApiCallback<String> listener) {
    if (null != mimeType) {
      if(StringUtil.isStringValueEqual(mOwnerId, User.getCurrentUserId())) {
        Attachment attachment = new Attachment(mimeType,StringUtil.isNotEmpty(mimeType) ? mimeType : Attachment.getMimeType(null, Attachment.MIME_TYPE_IMAGE));
        attachment.addMetaData(Attachment.META_FILE_ID, getFullChannelName());
        attachment.upload(new Attachment.UploadListener() {
          @Override public void onStart(Attachment attachment) {

          }

          @Override public void onComplete(Attachment attachment) {
            if (null != listener) {
              listener.success(getIconUrl());
            }
          }

          @Override public void onError(Attachment attachment, Throwable error) {
            if (null != listener) {
              listener.failure(new ApiError(error));
            }
          }
        });
      } else {
        if(null != listener) {
          listener.failure(new ApiError("Only channel owner can set icon"));
        }
      }
    } else {
      if(null != listener) {
        listener.failure(new ApiError("image should not be null"));
      }
    }
  }

  /**
   * Get the url of the icon
   * @return
   */
  public String getIconUrl() {
    return Attachment.createDownloadUrl(getFullChannelName(), mOwnerId);
  }

  private String getFullChannelName() {
    return mPublic ? mName : (mOwnerId + "_" + mName);
  }

  //public static void getChannelSummary(String channelId, ChannelSummaryOptions options, final OnFinishedListener<ChannelSummary> listener) {
  //  getChannelService().getChannelSummary(Arrays.asList(channelId), options, new Callback<List<ChannelSummary>>() {
  //    @Override public void onResponse(Response<List<ChannelSummary>> response) {
  //      listener.onSuccess(response.body().get(0));
  //    }
  //
  //    @Override public void onFailure(Throwable throwable) {
  //
  //    }
  //  }).executeInBackground();
  //}

  /**
   * This method only populates the topic name and the public flag in all cases.
   *
   * Owner username will be populated if it's a private topic
   *
   * @param topic the topic
   * @return an MMXChannel representation of the topic
   */
  static MMXChannel fromMMXTopic(MMXTopic topic) {
    if (topic == null) {
      return null;
    }
    boolean isUserTopic = topic.isUserTopic();
    return new MMXChannel.Builder()
            .name(topic.getName())
            .ownerId(isUserTopic ? topic.getUserId() : null)
            .setPublic(!isUserTopic)
            .build();
  }

  private static List<MMXChannel> fromSubscriptions(List<MMXSubscription> subscriptions) throws MMXException {
    MMXPubSubManager psm = MMX.getMMXClient().getPubSubManager();
    if (subscriptions == null || subscriptions.size() == 0) {
      return new ArrayList<MMXChannel>();
    }

    List<MMXTopic> topics = new ArrayList<MMXTopic>(subscriptions.size());
    for (MMXSubscription subscription : subscriptions) {
      topics.add(subscription.getTopic());
    }
    List<MMXTopicInfo> topicInfos = psm.getTopics(topics);
    return fromTopicInfos(topicInfos, subscriptions);
  }

  private static List<MMXChannel> fromTopicInfos(List<MMXTopicInfo> topicInfos,
                                                 List<MMXSubscription> subscriptions) throws MMXException {
    MMXPubSubManager psm = MMX.getMMXClient().getPubSubManager();

    if (subscriptions == null) {
      //get subscriptions
      subscriptions = psm.listAllSubscriptions();
    }
    HashMap<MMXTopic, MMXSubscription> subMap = new HashMap<MMXTopic, MMXSubscription>();
    for (MMXSubscription subscription : subscriptions) {
      subMap.put(subscription.getTopic(), subscription);
    }

    //get topic summaries
    HashMap<MMXTopic, MMXTopicInfo> topicInfoMap = new HashMap<MMXTopic, MMXTopicInfo>();
    ArrayList<MMXTopic> topics = new ArrayList<MMXTopic>(topicInfos.size());
    for (MMXTopicInfo info : topicInfos) {
      if (info != null) {
        MMXTopic topic = info.getTopic();
        topics.add(topic);
        topicInfoMap.put(topic, info);
      }
    }
    List<TopicSummary> summaries = psm.getTopicSummary(topics, null, null);

    ArrayList<MMXChannel> channels = new ArrayList<MMXChannel>();
    for (TopicSummary summary : summaries) {
      MMXTopic topic = summary.getTopicNode();
      MMXTopicInfo info = topicInfoMap.get(topic);
      String description = info.getDescription();
      if (info != null) {
        channels.add(new MMXChannel.Builder()
                .lastTimeActive(summary.getLastPubTime() != null ?
                        summary.getLastPubTime() : info.getCreationDate())
                .name(topic.getName())
                .numberOfMessages(summary.getCount())
                .ownerId(info.getCreator().getUserId())
                .subscribed(subMap.containsKey(topic))
                .summary(description.equals(" ") ? null : description) //this is because of weirdness in mysql.  If it is created with null, it returns with a SPACE.
                .setPublic(!topic.isUserTopic())
                .publishPermission(PublishPermission.fromPublisherType(info.getPublisherType()))
                .creationDate(info.getCreationDate())
                .build());
      }
    }
    return channels;
  }
  
  private static void validateClient(MMXClient mmxClient) throws MMXException {
    if (!mmxClient.isConnected())
      throw new MMXException("Current user is not logged in", StatusCode.UNAUTHORIZED);
  }

  private static ChannelService getChannelService() {
    return MaxCore.create(ChannelService.class);
  }

  // ***************************
  // CODE RELATED TO INVITATIONS
  // ***************************

  /**
   * The basic fields that are included in an invitation.
   */
  public static class MMXInviteInfo {
    private static final String KEY_TEXT = "text";
    private static final String KEY_CHANNEL_NAME = "channelName";
    private static final String KEY_CHANNEL_SUMMARY = "channelSummary";
    private static final String KEY_CHANNEL_IS_PUBLIC = "channelIsPublic";
    private static final String KEY_CHANNEL_OWNER_ID = "channelOwnerId";
    private static final String KEY_CHANNEL_CREATION_DATE = "channelCreationDate";
    private static final String KEY_CHANNEL_PUBLISH_PERMISSIONS = "channelPublishPermissions";
    private MMXChannel mChannel;
    private Set<User> mInvitees;
    private User mInviter;
    private String mComment;

    private MMXInviteInfo(Set<User> invitees, User inviter, MMXChannel channel, String comment) {
      mInvitees = invitees;
      mInviter = inviter;
      mChannel = channel;
      mComment = comment;
    }
    
    /**
     * The users who are being invited.
     *
     * @return the users who are being invited
     */
    public Set<User> getInvitees() {
      return mInvitees;
    }

    /**
     * The user who is inviting someone else to subscribe to this channel.
     *
     * @return the user who is inviting someone else to subscribe
     */
    public User getInviter() {
      return mInviter;
    }

    /**
     * The channel for this invite
     *
     * @return the channel for this invite
     */
    public MMXChannel getChannel() {
      return mChannel;
    }

    /**
     * The comment for this invite
     *
     * @return the comment for this invite
     */
    public String getComment() {
      return mComment;
    }

    protected final HashMap<String, String> buildMessageContent() {
      HashMap<String,String> content = new HashMap<String, String>();
      if (getComment() != null) {
        content.put(KEY_TEXT, getComment());
      }
      content.put(KEY_CHANNEL_NAME, mChannel.getName());
      if (mChannel.getSummary() != null) {
        content.put(KEY_CHANNEL_SUMMARY, mChannel.getSummary());
      }
      content.put(KEY_CHANNEL_IS_PUBLIC, String.valueOf(mChannel.isPublic()));
      content.put(KEY_CHANNEL_OWNER_ID, mChannel.getOwnerId());
      if (mChannel.getCreationDate() != null) {
        content.put(KEY_CHANNEL_CREATION_DATE, TimeUtil.toString(mChannel.getCreationDate()));
      }
      content.put(KEY_CHANNEL_PUBLISH_PERMISSIONS, mChannel.mPublishPermission.type.name());
      return content;
    }

    static MMXInviteInfo fromMMXMessage(MMXMessage message) {
      Map<String,String> content = message.getContent();
      String text = content.get(KEY_TEXT);
      String channelName = content.get(KEY_CHANNEL_NAME);
      String channelSummary = content.get(KEY_CHANNEL_SUMMARY);
      String channelIsPublic = content.get(KEY_CHANNEL_IS_PUBLIC);
      String channelOwnerId = content.get(KEY_CHANNEL_OWNER_ID);
      String channelCreationDate = content.get(KEY_CHANNEL_CREATION_DATE);
      String channelPublishPermissions = content.get(KEY_CHANNEL_PUBLISH_PERMISSIONS);
      MMXChannel channel = new MMXChannel.Builder()
              .name(channelName)
              .summary(channelSummary)
              .setPublic(Boolean.parseBoolean(channelIsPublic))
              .ownerId(channelOwnerId)
              .creationDate(channelCreationDate == null ? null : TimeUtil.toDate(channelCreationDate))
              .publishPermission(PublishPermission.fromPublisherType(TopicAction.PublisherType.valueOf(channelPublishPermissions)))
              .build();
      return new MMXInviteInfo(message.getRecipients(), message.getSender(), channel, text);
    }
  }

  /**
   * The MMXInvite class is used when sending invites for channels.
   *
   * @see #inviteUser(User, String, OnFinishedListener)
   */
  public static class MMXInvite {
    static final String TYPE = "invitation";
    private MMXInviteInfo mInviteInfo;
    private boolean mIncoming;

    private MMXInvite(MMXInviteInfo inviteInfo, boolean incoming) {
      mInviteInfo = inviteInfo;
      mIncoming = incoming;
    }

    /**
     * The information for this invite
     *
     * @return the information for this invite
     */
    public MMXInviteInfo getInviteInfo() {
      return mInviteInfo;
    }

    private void send(final OnFinishedListener<MMXInvite> listener) {
      if (mIncoming) {
        //coding error
        throw new RuntimeException("Cannot call send on an incoming invitation.");
      } else if (mInviteInfo.mChannel.getOwnerId() == null) {
        //coding error
        throw new RuntimeException("Cannot invite for an invalid channel.  " +
                "Likely cause is that this MMXChannel instance was created using " +
                "the MMXChannel.Builder() object instead of being returned from " +
                "a method call (i.e. findBy() or create()");
      }
      MMXMessage message = new MMXMessage.Builder()
              .recipients(mInviteInfo.getInvitees())
              .content(mInviteInfo.buildMessageContent())
              .type(TYPE)
              .build();
      message.send(new MMXMessage.OnFinishedListener<String>() {
        public void onSuccess(String result) {
          if (listener != null) {
            MMX.getCallbackHandler().post(new Runnable() {
              public void run() {
                listener.onSuccess(MMXInvite.this);
              }
            });
          }
        }

        public void onFailure(MMXMessage.FailureCode code, final Throwable ex) {
          if (listener != null) {
            MMX.getCallbackHandler().post(new Runnable() {
              public void run() {
                listener.onFailure(FailureCode.fromMMXFailureCode(FailureCode.DEVICE_ERROR, ex), ex);
              }
            });
          }
        }
      });
    }

    /**
     * Accept this invitation.  This will subscribe to the specified channel
     * and notify the inviter.
     *
     * @param comment comment to include with the response
     * @param listener the listener for success/failure of the operation (optional)
     */
    public void accept(final String comment, final OnFinishedListener<MMXInvite> listener) {
      if (!mIncoming) {
        throw new RuntimeException("Can't accept an outgoing invite");
      }

      MMXChannel channel = mInviteInfo.getChannel();
      channel.subscribe(new OnFinishedListener<String>() {
        @Override
        public void onSuccess(String result) {
          MMXMessage response = buildResponse(true, comment);
          response.send(new MMXMessage.OnFinishedListener<String>() {
            @Override
            public void onSuccess(String result) {
              if (listener != null) {
                MMX.getCallbackHandler().post(new Runnable() {
                  public void run() {
                    listener.onSuccess(MMXInvite.this);
                  }
                });
              }
            }

            @Override
            public void onFailure(MMXMessage.FailureCode code, final Throwable ex) {
              if (listener != null) {
                MMX.getCallbackHandler().post(new Runnable() {
                  public void run() {
                    listener.onFailure(FailureCode.fromMMXFailureCode(FailureCode.DEVICE_ERROR, ex), ex);
                  }
                });
              }
            }
          });
        }

        @Override
        public void onFailure(FailureCode code, final Throwable ex) {
          if (listener != null) {
            MMX.getCallbackHandler().post(new Runnable() {
              public void run() {
                listener.onFailure(FailureCode.fromMMXFailureCode(FailureCode.DEVICE_ERROR, ex), ex);
              }
            });
          }
        }
      });
    }

    /**
     * Decline this invitation.  This will notify the inviter.
     *
     * @param comment comment to include with the response
     * @param listener the listener for success/failure of the operation (optional)
     */
    public void decline(String comment, final OnFinishedListener<MMXInvite> listener) {
      if (!mIncoming) {
        throw new RuntimeException("Can't reject an outgoing invite");
      }
      MMXMessage response = buildResponse(false, comment);
      response.send(new MMXMessage.OnFinishedListener<String>() {
        @Override
        public void onSuccess(String result) {
          if (listener != null) {
            MMX.getCallbackHandler().post(new Runnable() {
              public void run() {
                listener.onSuccess(MMXInvite.this);
              }
            });
          }
        }

        @Override
        public void onFailure(MMXMessage.FailureCode code, final Throwable ex) {
          if (listener != null) {
            MMX.getCallbackHandler().post(new Runnable() {
              public void run() {
                listener.onFailure(FailureCode.fromMMXFailureCode(FailureCode.DEVICE_ERROR, ex), ex);
              }
            });
          }
        }
      });
    }

    private MMXMessage buildResponse(boolean accepted, String responseText) {
      HashSet<User> recipients = new HashSet<User>();
      recipients.add(mInviteInfo.getInviter());
      HashMap<String, String> content = mInviteInfo.buildMessageContent();
      content.put(MMXInviteResponse.KEY_IS_ACCEPTED, String.valueOf(accepted));
      if (responseText != null) {
        content.put(MMXInviteResponse.KEY_RESPONSE_TEXT, responseText);
      }
      return new MMXMessage.Builder()
              .recipients(recipients)
              .content(content)
              .type(MMXInviteResponse.TYPE)
              .build();
    }

    static MMXInvite fromMMXMessage(MMXMessage message) {
      if (message == null) {
        return null;
      }
      MMXInviteInfo info = MMXInviteInfo.fromMMXMessage(message);
      return new MMXInvite(info, true);
    }
  }

  /**
   * The invitation response that is sent when an invite is accepted or rejected
   */
  public static class MMXInviteResponse {
    static final String TYPE = "invitationResponse";
    private static final String KEY_IS_ACCEPTED = "inviteIsAccepted";
    private static final String KEY_RESPONSE_TEXT = "inviteResponseText";
    private MMXInviteInfo mInviteInfo;
    private boolean mAccepted;
    private String mResponseText;

    private MMXInviteResponse(MMXInviteInfo inviteInfo) {
      mInviteInfo = inviteInfo;
    }

    /**
     * Retrieves the invite information
     *
     * @return the invite information
     */
    public MMXInviteInfo getInviteInfo() {
      return mInviteInfo;
    }

    /**
     * Whether or not the invite was accepted
     *
     * @return true if accepted, false if not
     */
    public boolean isAccepted() {
      return mAccepted;
    }

    private void setAccepted(boolean accepted) {
      mAccepted = accepted;
    }

    /**
     * Any text included with the response
     *
     * @return text that was included with the response
     */
    public String getResponseText() {
      return mResponseText;
    }

    private void setResponseText(String responseText) {
      mResponseText = responseText;
    }

    static MMXInviteResponse fromMMXMessage(MMXMessage message) {
      if (message == null) {
        return null;
      }
      MMXInviteInfo inviteInfo = MMXInviteInfo.fromMMXMessage(message);
      MMXInviteResponse response = new MMXInviteResponse(inviteInfo);

      //additional params in the response
      Map<String, String> content = message.getContent();
      String isAccepted = content.get(KEY_IS_ACCEPTED);
      String responseText = content.get(KEY_RESPONSE_TEXT);
      response.setAccepted(Boolean.parseBoolean(isAccepted));
      response.setResponseText(responseText);
      return response;
    }
  }

  @Override
  public boolean equals(Object obj) {
    if (!EqualityUtil.quickCheck(this, obj)) {
      return false;
    }

    MMXChannel theOther = (MMXChannel) obj;

    return StringUtil.isStringValueEqual(mName, theOther.getName()) &&
        StringUtil.isStringValueEqual(mSummary, theOther.getSummary()) &&
        StringUtil.isStringValueEqual(mOwnerId, theOther.getOwnerId()) &&
        mPublic == theOther.isPublic() &&
        mPublishPermission == theOther.getPublishPermission() &&
        StringUtil.isStringValueEqual(mName, theOther.getName()) &&
        (null != mCreationDate ? mCreationDate.equals(theOther.getCreationDate()) : null == theOther.getCreationDate());
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder().hash(mName).hash(mSummary).hash(mOwnerId).hash(mPublic)
        .hash(mPublishPermission).hash(mCreationDate).hashCode();
  }

  @Override
  public String toString() {
    return new StringBuilder().append("{")
        .append("name = ").append(mName).append(", ")
        .append("ownerId = ").append(mOwnerId).append(", ")
        .append("isPublic = ").append(mPublic).append(", ")
        .append("publishPermission = ").append(mPublishPermission).append(", ")
        .append("summary = ").append(mSummary).append(", ")
        .append("isSubscribed = ").append(mSubscribed)
        .append("}")
        .toString();
  }

  //----------------Parcelable Methods----------------

  @Override public int describeContents() {
    return 0;
  }

  @Override public void writeToParcel(Parcel dest, int flags) {
    dest.writeString(this.mName);
    dest.writeString(this.mSummary);
    dest.writeString(this.mOwnerId);
    dest.writeValue(this.mNumberOfMessages);
    dest.writeLong(mLastTimeActive != null ? mLastTimeActive.getTime() : -1);
    dest.writeByte(mPublic ? (byte) 1 : (byte) 0);
    dest.writeInt(this.mPublishPermission == null ? -1 : this.mPublishPermission.ordinal());
    dest.writeValue(this.mSubscribed);
    dest.writeLong(mCreationDate != null ? mCreationDate.getTime() : -1);
  }

  protected MMXChannel(Parcel in) {
    this.mName = in.readString();
    this.mSummary = in.readString();
    this.mOwnerId = in.readString();
    this.mNumberOfMessages = (Integer) in.readValue(Integer.class.getClassLoader());
    long tmpMLastTimeActive = in.readLong();
    this.mLastTimeActive = tmpMLastTimeActive == -1 ? null : new Date(tmpMLastTimeActive);
    this.mPublic = in.readByte() != 0;
    int tmpMPublishPermission = in.readInt();
    this.mPublishPermission =
        tmpMPublishPermission == -1 ? null : PublishPermission.values()[tmpMPublishPermission];
    this.mSubscribed = (Boolean) in.readValue(Boolean.class.getClassLoader());
    long tmpMCreationDate = in.readLong();
    this.mCreationDate = tmpMCreationDate == -1 ? null : new Date(tmpMCreationDate);
  }

  public static final Parcelable.Creator<MMXChannel> CREATOR =
      new Parcelable.Creator<MMXChannel>() {
        public MMXChannel createFromParcel(Parcel source) {
          return new MMXChannel(source);
        }

        public MMXChannel[] newArray(int size) {
          return new MMXChannel[size];
        }
      };
}
