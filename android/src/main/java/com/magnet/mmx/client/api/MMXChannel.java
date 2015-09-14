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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
import com.magnet.mmx.protocol.MMXStatus;
import com.magnet.mmx.protocol.MMXTopic;
import com.magnet.mmx.protocol.MMXTopicOptions;
import com.magnet.mmx.protocol.SearchAction;
import com.magnet.mmx.protocol.TopicAction;
import com.magnet.mmx.protocol.TopicSummary;
import com.magnet.mmx.protocol.UserInfo;

/**
 * The MMXChannel class representing the Topic/Feed/PubSub model.
 */
public class MMXChannel {
  private static final String TAG = MMXChannel.class.getSimpleName();

  /**
   * Failure codes for the MMXChannel class.
   */
  public static class FailureCode extends MMX.FailureCode {
    public static final FailureCode CHANNEL_EXISTS = new FailureCode(409, "CHANNEL_EXISTS");
    public static final FailureCode CONTENT_TOO_LARGE = new FailureCode(413, "CONTENT_TOO_LARGE");
    public static final FailureCode CHANNEL_FORBIDDEN = new FailureCode(403, "CHANNEL_FORBIDDEN");
    public static final FailureCode CHANNEL_NOT_FOUND = new FailureCode(404, "CHANNEL_NOT_FOUND");
    public static final FailureCode CHANNEL_NOT_AUTHORIZED = new FailureCode(401, "CHANNEL_NOT_AUTHROIZED");
    public static final FailureCode SUBSCRIPTION_NOT_FOUND = new FailureCode(404, "SUBSCRIPTION_NOT_FOUND");
    public static final FailureCode SUBSCRIPTION_INVALID_ID = new FailureCode(406, "SUBSCRIPTION_INVALID_ID");
    public static final FailureCode INVALID_INVITEE = new FailureCode(403, "INVALID_INVITEE");
    
    FailureCode(int value, String description) {
      super(value, description);
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
   */
  public static final class Builder {
    private MMXChannel mChannel;

    public Builder() {
      mChannel = new MMXChannel();
    }

    /**
     * Set the name of this channel
     *
     * @param name the name
     * @return this Builder object
     */
    public Builder name(String name) {
      mChannel.name(name);
      return this;
    }

    /**
     * Set the summary of this channel
     *
     * @param summary the summary
     * @return this Builder object
     */
    public Builder summary(String summary) {
      mChannel.summary(summary);
      return this;
    }

    /**
     * Set the owner's username for this channel
     *
     * @param ownerUsername the owner username
     * @return this Builder object
     */
    Builder ownerUsername(String ownerUsername) {
      mChannel.ownerUsername(ownerUsername);
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
    Builder lastTimeActive(Date lastTimeActive) {
      mChannel.lastTimeActive(lastTimeActive);
      return this;
    }

    /**
     * Set the subscribed flag for this channel
     *
     * @param subscribed the subscribed flag
     * @return this Builder object
     */
    Builder subscribed(Boolean subscribed) {
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
  private String mOwnerUsername;
  private Integer mNumberOfMessages;
  private Date mLastTimeActive;
  private boolean mPublic;
  private Boolean mSubscribed;

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
   * @param ownerUsername the owner username
   * @return this MMXChannel object
   */
  MMXChannel ownerUsername(String ownerUsername) {
    mOwnerUsername = ownerUsername;
    return this;
  }

  /**
   * The owner for this channel
   *
   * @return the owner, null if not yet retrieved
   */
  public String getOwnerUsername() {
    return mOwnerUsername;
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
   * @return the ast active time, null if not yet retrieved
   */
  public Date getLastTimeActive() {
    return mLastTimeActive;
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
        MMXPubSubManager psm = mmxClient.getPubSubManager();
        TopicAction.TopicTags topicTags = psm.getAllTags(getMMXTopic());
        return topicTags.getTags() != null ?
                new HashSet<String>(topicTags.getTags()) : null;
      }

      @Override
      public void onException(Throwable exception) {
        listener.onFailure(FailureCode.fromMMXFailureCode(FailureCode.DEVICE_ERROR, exception), exception);
      }

      @Override
      public void onResult(HashSet<String> result) {
        listener.onSuccess(result);
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
      public void onException(Throwable exception) {
        listener.onFailure(FailureCode.fromMMXFailureCode(FailureCode.DEVICE_ERROR, exception), exception);
      }

      @Override
      public void onResult(MMXStatus result) {
        if (result.getCode() == MMXStatus.SUCCESS) {
          listener.onSuccess(null);
        } else {
          Log.e(TAG, "setTags(): received bad status from server: " + result);
          listener.onFailure(FailureCode.fromMMXFailureCode(FailureCode.SERVER_ERROR, null), null);
        }

      }
    };
    task.execute();
  }

  MMXTopic getMMXTopic() {
    if (!isPublic()) {
      if (getOwnerUsername() == null) {
        return new MMXPersonalTopic(getName());
      } else {
        return new MMXUserTopic(getOwnerUsername(), getName());
      }
    }
    return new MMXGlobalTopic(getName());
  }

  /**
   * Retrieve all of the messages for this channel.  Possible failure codes are:
   * {@link FailureCode#CHANNEL_NOT_FOUND} for no such channel,
   * {@link FailureCode#CHANNEL_FORBIDDEN} for insufficient rights.
   *
   * @param startDate filter based on start date, or null for no filter
   * @param endDate filter based on end date, or null for no filter
   * @param limit the maximum number of messages to return
   * @param ascending the chronological sort order of the results
   * @param listener the listener for the results of this operation
   */
  public void getItems(final Date startDate, final Date endDate, final int limit, final boolean ascending,
                       final OnFinishedListener<ListResult<MMXMessage>> listener) {
    final MMXTopic topic = getMMXTopic();
    MMXTask<ListResult<com.magnet.mmx.client.common.MMXMessage>> task =
            new MMXTask<ListResult<com.magnet.mmx.client.common.MMXMessage>> (MMX.getMMXClient(), MMX.getHandler()) {
      @Override
      public ListResult<com.magnet.mmx.client.common.MMXMessage> doRun(MMXClient mmxClient) throws Throwable {
        MMXPubSubManager psm = mmxClient.getPubSubManager();
        List<com.magnet.mmx.client.common.MMXMessage> messages =
                psm.getItems(topic, new TopicAction.FetchOptions()
                .setSince(startDate)
                .setUntil(endDate)
                .setMaxItems(limit)
                .setAscending(ascending));
        ArrayList<MMXTopic> topicList = new ArrayList<MMXTopic>();
        topicList.add(getMMXTopic());
        List<TopicSummary> summaries = psm.getTopicSummary(topicList, startDate, endDate);
        TopicSummary summary = summaries.get(0);
        return new ListResult<com.magnet.mmx.client.common.MMXMessage>(summary.getCount(), messages);
      }

      @Override
      public void onException(Throwable exception) {
        if (listener != null) {
          listener.onFailure(FailureCode.fromMMXFailureCode(FailureCode.DEVICE_ERROR, exception), exception);
        }
      }

      @Override
      public void onResult(ListResult<com.magnet.mmx.client.common.MMXMessage> result) {
        if (listener == null) {
          return;
        }
        ArrayList<MMXMessage> resultList = new ArrayList<MMXMessage>();
        if (result != null && result.items.size() > 0) {
          //convert MMXMessages
          for (com.magnet.mmx.client.common.MMXMessage message : result.items) {
            resultList.add(MMXMessage.fromMMXMessage(getMMXTopic(), message));
          }
        }
        listener.onSuccess(new ListResult<MMXMessage>(result.totalCount, resultList));
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
  public void getItemsByIds(final Set<String> ids, final OnFinishedListener<Map<String, MMXMessage>> listener) {
    final MMXTopic topic = getMMXTopic();
    MMXTask<Map<String, com.magnet.mmx.client.common.MMXMessage>> task =
            new MMXTask<Map<String, com.magnet.mmx.client.common.MMXMessage>> (MMX.getMMXClient(), MMX.getHandler()) {
      @Override
      public Map<String, com.magnet.mmx.client.common.MMXMessage> doRun(MMXClient mmxClient) throws Throwable {
        MMXPubSubManager psm = mmxClient.getPubSubManager();
        List<String> idList = Arrays.asList(ids.toArray(new String[ids.size()]));
        Map<String, com.magnet.mmx.client.common.MMXMessage> messages =
                psm.getItemsByIds(topic, idList);
        return messages;
      }

      @Override
      public void onException(Throwable exception) {
        if (listener != null) {
          listener.onFailure(FailureCode.fromMMXFailureCode(FailureCode.DEVICE_ERROR, exception), exception);
        }
      }

      @Override
      public void onResult(Map<String, com.magnet.mmx.client.common.MMXMessage> result) {
        if (listener == null) {
          return;
        }
        Map<String, MMXMessage> resultMap = new HashMap<String, MMXMessage>();
        if (result != null && result.size() > 0) {
          //convert MMXMessages
          for (Map.Entry<String, com.magnet.mmx.client.common.MMXMessage> entry : result.entrySet()) {
            resultMap.put(entry.getKey(), MMXMessage.fromMMXMessage(getMMXTopic(), entry.getValue()));
          }
        }
        listener.onSuccess(resultMap);
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
  public void deleteItemsByIds(final Set<String> ids,
                              final OnFinishedListener<Map<String, Integer>> listener) {
    final MMXTopic topic = getMMXTopic();
    MMXTask<Map<String, Integer>> task =
            new MMXTask<Map<String, Integer>> (MMX.getMMXClient(), MMX.getHandler()) {
      @Override
      public Map<String, Integer> doRun(MMXClient mmxClient) throws Throwable {
        MMXPubSubManager psm = mmxClient.getPubSubManager();
        List<String> idList = Arrays.asList(ids.toArray(new String[ids.size()]));
        Map<String, Integer> results = psm.deleteItemsByIds(topic, idList);
        return results;
      }

      @Override
      public void onException(Throwable exception) {
        if (listener != null) {
          listener.onFailure(FailureCode.fromMMXFailureCode(
              FailureCode.DEVICE_ERROR, exception), exception);
        }
      }

      @Override
      public void onResult(Map<String, Integer> result) {
        if (listener != null) {
          listener.onSuccess(result);
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

  /**
   * Create the channel.  The default behavior is to create a private channel.
   * Possible failure codes are: {@link FailureCode#BAD_REQUEST} for invalid
   * channel name, {@value FailureCode#CHANNEL_EXISTS} for existing channel.
   *
   * @param listener the listner for the newly created channel
   * @see Builder#setPublic(boolean)
   */
  public void create(final OnFinishedListener<MMXChannel> listener) {
    MMXTask<MMXTopic> task = new MMXTask<MMXTopic>(MMX.getMMXClient(), MMX.getHandler()) {
      @Override
      public MMXTopic doRun(MMXClient mmxClient) throws Throwable {
        MMXPubSubManager psm = mmxClient.getPubSubManager();
        MMXTopicOptions options = new MMXTopicOptions()
                .setDescription(mSummary).setSubscribeOnCreate(true);
        MMXTopic topic = getMMXTopic();
        return psm.createTopic(topic, options);
      }

      @Override
      public void onException(Throwable exception) {
        if (listener != null) {
          listener.onFailure(FailureCode.fromMMXFailureCode(FailureCode.DEVICE_ERROR, exception), exception);
        }
      }

      @Override
      public void onResult(final MMXTopic createResult) {
        if (listener == null) {
          return;
        }
        String currentUsername = MMX.getCurrentUser().getUsername();
        MMXChannel.this.ownerUsername(currentUsername);
        listener.onSuccess(MMXChannel.fromMMXTopic(createResult).ownerUsername(currentUsername));
      }
    };
    task.execute();
  }

  /**
   * Delete the topic.  Possible failure codes are: {@link FailureCode#CHANNEL_NOT_FOUND}
   * for no such channel, {@link FailureCode#CHANNEL_FORBIDDEN} for
   * insufficient rights.
   *
   * @param listener the listener for success or failure
   */
  public void delete(final OnFinishedListener<Void> listener) {
    MMXTask<MMXStatus> task = new MMXTask<MMXStatus> (MMX.getMMXClient(), MMX.getHandler()) {
      @Override
      public MMXStatus doRun(MMXClient mmxClient) throws Throwable {
        MMXPubSubManager psm = mmxClient.getPubSubManager();
        return psm.deleteTopic(getMMXTopic());
      }

      @Override
      public void onResult(MMXStatus result) {
        if (listener == null) {
          return;
        }
        if (result.getCode() == MMXStatus.SUCCESS) {
          listener.onSuccess(null);
        } else {
          listener.onFailure(FailureCode.fromMMXFailureCode(FailureCode.SERVER_ERROR, null), null);
        }
      }

      @Override
      public void onException(Throwable exception) {
        if (listener != null) {
          listener.onFailure(FailureCode.fromMMXFailureCode(FailureCode.DEVICE_ERROR, exception), exception);
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
    MMXTask<String> task = new MMXTask<String>(MMX.getMMXClient(), MMX.getHandler()) {
      @Override
      public String doRun(MMXClient mmxClient) throws Throwable {
        MMXPubSubManager psm = mmxClient.getPubSubManager();
        return psm.subscribe(getMMXTopic(), false);
      }

      @Override
      public void onException(Throwable exception) {
        if (listener != null) {
          listener.onFailure(FailureCode.fromMMXFailureCode(FailureCode.DEVICE_ERROR, exception), exception);
        }
      }

      @Override
      public void onResult(String result) {
        if (listener != null) {
          listener.onSuccess(result);
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
      public void onException(Throwable exception) {
        if (listener != null) {
          listener.onFailure(FailureCode.fromMMXFailureCode(FailureCode.DEVICE_ERROR, exception), exception);
        }
      }

      @Override
      public void onResult(Boolean result) {
        if (listener != null) {
          listener.onSuccess(result);
        }
      }
    };
    task.execute();
  }

  /**
   * Publishes a message to this channel.
   *
   * @param messageContent the message content to publish
   * @param listener the listener for the message id
   * @return the message id for this published message
   * @deprecated Use {@link #publish(Map, OnFinishedListener)}
   */
  public String publish(Map<String, String> messageContent,
                      final MMXMessage.OnFinishedListener<String> listener) {
    MMXMessage message = new MMXMessage.Builder()
            .channel(this)
            .content(messageContent)
            .build();
    return message.send(listener);
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
    MMXMessage message = new MMXMessage.Builder()
            .channel(this)
            .content(messageContent)
            .build();
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
  public void inviteUser(final MMXUser invitee, final String invitationText,
                         final OnFinishedListener<MMXInvite> listener) {
    MMXInviteInfo inviteInfo = new MMXInviteInfo(invitee, MMX.getCurrentUser(), this, invitationText);
    MMXInvite invite = new MMXInvite(inviteInfo, false);
    invite.send(listener);
  }

  /**
   * A convenient method to send an invitation to multiple users for this channel.
   * The listener will be invoked one per invitee.  If the invitee is invalid,
   * the failure code will be {@link FailureCode#INVALID_INVITEE}.
   * @param invitees A set of invitees
   * @param invitationText the text to include in the invite
   * @param listener the listener for success/failure of this operation
   */
  public void inviteUsers(final Set<MMXUser> invitees, final String invitationText,
                          final OnFinishedListener<MMXInvite> listener) {
    for (MMXUser invitee : invitees) {
      MMXInviteInfo inviteInfo = new MMXInviteInfo(invitee, MMX.getCurrentUser(), this, invitationText);
      MMXInvite invite = new MMXInvite(inviteInfo, false);
      invite.send(listener);
    }
  }

  /**
   * Retrieves all the subscribers for this channel.  Possible failure codes are:
   * {@link FailureCode#CHANNEL_NOT_FOUND} for no such channel,
   * {@link FailureCode#CHANNEL_FORBIDDEN} for insufficient rights.
   *
   * @param limit the maximum number of subscribers to return
   * @param listener the listener for the subscribers
   */
  public void getAllSubscribers(final int limit, final OnFinishedListener<ListResult<MMXUser>> listener) {
    MMXTask<MMXResult<List<UserInfo>>> task =
            new MMXTask<MMXResult<List<UserInfo>>> (MMX.getMMXClient(), MMX.getHandler()) {
      @Override
      public MMXResult<List<UserInfo>> doRun(MMXClient mmxClient) throws Throwable {
        MMXPubSubManager psm = mmxClient.getPubSubManager();
        return psm.getSubscribers(getMMXTopic(), limit);
      }

      @Override
      public void onException(Throwable exception) {
        if (listener != null) {
          listener.onFailure(FailureCode.fromMMXFailureCode(FailureCode.DEVICE_ERROR, exception), exception);
        }
      }

      @Override
      public void onResult(MMXResult<List<UserInfo>> result) {
        if (listener == null) {
          return;
        }
        ArrayList<MMXUser> users = new ArrayList<MMXUser>();
        for (UserInfo userInfo : result.getResult()) {
          users.add(MMXUser.fromUserInfo(userInfo));
        }
        listener.onSuccess(new ListResult<MMXUser>(result.getTotal(), users));
      }
    };
    task.execute();
  }

  /**
   * Find the channel that starts with the specified text.  If there are no
   * matching names, {@link OnFinishedListener#onSuccess(Object)} with an
   * empty list will be invoked.
   *
   * @param startsWith the search string
   * @param limit the maximum number of results to return
   * @param listener the listener for the query results
   */
  public static void findByName(final String startsWith, final int limit,
                                final OnFinishedListener<ListResult<MMXChannel>> listener) {
    MMXTask<ListResult<MMXChannel>> task = new MMXTask<ListResult<MMXChannel>>(
            MMX.getMMXClient(), MMX.getHandler()) {
      @Override
      public ListResult<MMXChannel> doRun(MMXClient mmxClient) throws Throwable {
        MMXPubSubManager psm = mmxClient.getPubSubManager();
        TopicAction.TopicSearch search = new TopicAction.TopicSearch()
                .setTopicName(startsWith, SearchAction.Match.PREFIX);
        MMXTopicSearchResult searchResult = psm.searchBy(SearchAction.Operator.AND, search, limit);
        List<MMXChannel> channels = fromTopicInfos(searchResult.getResults(), null);
        return new ListResult<MMXChannel>(searchResult.getTotal(), channels);
      }

      @Override
      public void onResult(ListResult<MMXChannel> result) {
        //build the query result
        if (listener != null) {
          listener.onSuccess(result);
        }
      }

      @Override
      public void onException(Throwable exception) {
        if (listener != null) {
          listener.onFailure(FailureCode.fromMMXFailureCode(FailureCode.DEVICE_ERROR, exception), exception);
        }
      }
    };
    task.execute();
  }

  /**
   * Query for the specified tags (inclusive.)  If there are no matching tags
   *
   * @param tags the tags to match
   * @param listener the listener for the query results
   */
  public static void findByTags(final Set<String> tags, final int limit,
                                final OnFinishedListener<ListResult<MMXChannel>> listener) {
    MMXTask<ListResult<MMXChannel>> task = new MMXTask<ListResult<MMXChannel>>(
            MMX.getMMXClient(), MMX.getHandler()) {
      @Override
      public ListResult<MMXChannel> doRun(MMXClient mmxClient) throws Throwable {
        MMXPubSubManager psm = mmxClient.getPubSubManager();
        TopicAction.TopicSearch search = new TopicAction.TopicSearch()
                .setTags(new ArrayList<String>(tags));
        MMXTopicSearchResult searchResult = psm.searchBy(SearchAction.Operator.AND, search, limit);
        List<MMXChannel> channels =fromTopicInfos(searchResult.getResults(), null);
        return new ListResult<MMXChannel>(searchResult.getTotal(), channels);
      }

      @Override
      public void onResult(ListResult<MMXChannel> result) {
        //build the query result
        if (listener != null) {
          listener.onSuccess(result);
        }
      }

      @Override
      public void onException(Throwable exception) {
        if (listener != null) {
          listener.onFailure(FailureCode.fromMMXFailureCode(FailureCode.DEVICE_ERROR, exception), exception);
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
        MMXPubSubManager psm = mmxClient.getPubSubManager();
        return fromSubscriptions(psm.listAllSubscriptions());
      }

      @Override
      public void onResult(List<MMXChannel> result) {
        if (listener != null) {
          listener.onSuccess(result);
        }
      }

      @Override
      public void onException(Throwable exception) {
        if (listener != null) {
          listener.onFailure(FailureCode.fromMMXFailureCode(MMX.FailureCode.DEVICE_ERROR, exception), exception);
        }
      }
    };
    task.execute();
  }

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
            .ownerUsername(isUserTopic ? topic.getUserId() : null)
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
      if (info != null) {
        channels.add(new MMXChannel.Builder()
                      .lastTimeActive(summary.getLastPubTime())
                      .name(topic.getName())
                      .numberOfMessages(summary.getCount())
                      .ownerUsername(info.getCreator().getUserId())
                      .subscribed(subMap.containsKey(topic))
                      .summary(info.getDescription())
                      .setPublic(!topic.isUserTopic())
                      .build());
      }
    }
    return channels;
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
    private static final String KEY_CHANNEL_CREATOR_USERNAME = "channelCreatorUsername";
    private MMXChannel mChannel;
    private MMXUser mInvitee;
    private MMXUser mInviter;
    private String mComment;

    private MMXInviteInfo(MMXUser invitee, MMXUser inviter, MMXChannel channel, String comment) {
      mInvitee = invitee;
      mInviter = inviter;
      mChannel = channel;
      mComment = comment;
    }

    /**
     * The user who is being invited.
     *
     * @return the user who is being invited
     */
    public MMXUser getInvitee() {
      return mInvitee;
    }

    /**
     * The user who is inviting someone else to subscribe to this channel.
     *
     * @return the user who is inviting someone else to subscribe
     */
    public MMXUser getInviter() {
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
      content.put(KEY_CHANNEL_SUMMARY, mChannel.getSummary());
      content.put(KEY_CHANNEL_IS_PUBLIC, String.valueOf(mChannel.isPublic()));
      content.put(KEY_CHANNEL_CREATOR_USERNAME, mChannel.getOwnerUsername());
      return content;
    }

    static MMXInviteInfo fromMMXMessage(MMXMessage message) {
      Map<String,String> content = message.getContent();
      String text = content.get(KEY_TEXT);
      String channelName = content.get(KEY_CHANNEL_NAME);
      String channelSummary = content.get(KEY_CHANNEL_SUMMARY);
      String channelIsPublic = content.get(KEY_CHANNEL_IS_PUBLIC);
      String channelOwnerUsername = content.get(KEY_CHANNEL_CREATOR_USERNAME);
      MMXChannel channel = new MMXChannel.Builder()
              .name(channelName)
              .summary(channelSummary)
              .setPublic(Boolean.parseBoolean(channelIsPublic))
              .ownerUsername(channelOwnerUsername)
              .build();
      return new MMXInviteInfo(MMX.getCurrentUser(), message.getSender(), channel, text);
    }
  }

  /**
   * The MMXInvite class is used when sending invites for channels.
   *
   * @see #inviteUser(MMXUser, String, OnFinishedListener)
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
        throw new RuntimeException("Cannot call send on an incoming invitation.");
      }
      HashSet<MMXUser> recipients = new HashSet<MMXUser>();
      recipients.add(mInviteInfo.getInvitee());
      MMXMessage message = new MMXMessage.Builder()
              .recipients(recipients)
              .content(mInviteInfo.buildMessageContent())
              .type(TYPE)
              .build();
      message.send(new MMXMessage.OnFinishedListener<String>() {
        public void onSuccess(String result) {
          if (listener != null) {
            listener.onSuccess(MMXInvite.this);
          }
        }

        public void onFailure(MMXMessage.FailureCode code, Throwable ex) {
          if (listener != null) {
            listener.onFailure(FailureCode.fromMMXFailureCode(FailureCode.DEVICE_ERROR, ex), ex);
          }
        }
      });
    }

    /**
     * Accept this invitation.  This will subscribe to the specified topic and notify the inviter.
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
                listener.onSuccess(MMXInvite.this);
              }
            }

            @Override
            public void onFailure(MMXMessage.FailureCode code, Throwable ex) {
              if (listener != null) {
                listener.onFailure(FailureCode.fromMMXFailureCode(FailureCode.DEVICE_ERROR, ex), ex);
              }
            }
          });
        }

        @Override
        public void onFailure(FailureCode code, Throwable ex) {
          if (listener != null) {
            listener.onFailure(FailureCode.fromMMXFailureCode(FailureCode.DEVICE_ERROR, ex), ex);
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
            listener.onSuccess(MMXInvite.this);
          }
        }

        @Override
        public void onFailure(MMXMessage.FailureCode code, Throwable ex) {
          if (listener != null) {
            listener.onFailure(FailureCode.fromMMXFailureCode(FailureCode.DEVICE_ERROR, ex), ex);
          }
        }
      });
    }

    private MMXMessage buildResponse(boolean accepted, String responseText) {
      HashSet<MMXUser> recipients = new HashSet<MMXUser>();
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
}
