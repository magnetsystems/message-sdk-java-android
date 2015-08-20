package com.magnet.mmx.client.api;

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

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The MMXChannel class representing the Topic/Feed/PubSub model.
 */
public class MMXChannel {
  private static final String TAG = MMXChannel.class.getSimpleName();

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
     * Set the private flag for this channel
     *
     * @param isPrivate the private flag
     * @return this Builder object
     */
    Builder setPrivate(boolean isPrivate) {
      mChannel.setPrivate(isPrivate);
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
  private boolean mPrivate;
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
  public String getOwner() {
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
   * The tags for this channel.
   *
   * @param listener the success/failure listener for this call
   */
  public void getTags(final MMX.OnFinishedListener<HashSet<String>> listener) {
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
        listener.onFailure(MMX.FailureCode.SERVER_EXCEPTION, exception);
      }

      @Override
      public void onResult(HashSet<String> result) {
        listener.onSuccess(result);
      }
    };
    task.execute();
  }

  /**
   * Set the tags for this channel.
   *
   * @param tags the tags for this channel or null to remove all tags
   * @param listener the success/failure listener for this call
   */
  public void setTags(final HashSet<String> tags, final MMX.OnFinishedListener<Void> listener) {
    MMXTask<MMXStatus> task = new MMXTask<MMXStatus>(MMX.getMMXClient(), MMX.getHandler()) {
      @Override
      public MMXStatus doRun(MMXClient mmxClient) throws Throwable {
        MMXPubSubManager psm = mmxClient.getPubSubManager();
        return psm.setAllTags(getMMXTopic(), tags != null ? new ArrayList<String>(tags) : null);
      }

      @Override
      public void onException(Throwable exception) {
        listener.onFailure(MMX.FailureCode.SERVER_EXCEPTION, exception);
      }

      @Override
      public void onResult(MMXStatus result) {
        if (result.getCode() == MMXStatus.SUCCESS) {
          listener.onSuccess(null);
        } else {
          Log.e(TAG, "setTags(): received bad status from server: " + result);
          listener.onFailure(MMX.FailureCode.SERVER_BAD_STATUS, null);
        }

      }
    };
    task.execute();
  }

  MMXTopic getMMXTopic() {
    if (isPrivate()) {
      if (getOwner() == null) {
        return new MMXPersonalTopic(getName());
      } else {
        return new MMXUserTopic(getOwner(), getName());
      }
    }
    return new MMXGlobalTopic(getName());
  }

  public void getItems(final Date startDate, final Date endDate, final int limit, final boolean ascending,
                       final MMX.OnFinishedListener<List<MMXMessage>> listener) {
    final MMXTopic topic = getMMXTopic();
    MMXTask<List<com.magnet.mmx.client.common.MMXMessage>> task =
            new MMXTask<List<com.magnet.mmx.client.common.MMXMessage>> (MMX.getMMXClient(), MMX.getHandler()) {
      @Override
      public List<com.magnet.mmx.client.common.MMXMessage> doRun(MMXClient mmxClient) throws Throwable {
        MMXPubSubManager psm = mmxClient.getPubSubManager();
        return psm.getItems(topic, new TopicAction.FetchOptions()
                .setSince(startDate)
                .setUntil(endDate)
                .setMaxItems(limit)
                .setAscending(ascending));
      }

      @Override
      public void onException(Throwable exception) {
        listener.onFailure(MMX.FailureCode.SERVER_EXCEPTION, exception);
      }

      @Override
      public void onResult(List<com.magnet.mmx.client.common.MMXMessage> result) {
        ArrayList<MMXMessage> resultList = new ArrayList<MMXMessage>();
        if (result != null && result.size() > 0) {
          //convert MMXMessages
          for (com.magnet.mmx.client.common.MMXMessage message : result) {
            resultList.add(MMXMessage.fromMMXMessage(getMMXTopic(), message));
          }
        }
        listener.onSuccess(resultList);
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
   * Set the private flag for this channel
   *
   * @param isPrivate the private flag
   * @return this MMXChannel object
   */
  MMXChannel setPrivate(boolean isPrivate) {
    mPrivate = isPrivate;
    return this;
  }

  /**
   * Whether or not the current channel is private
   *
   * @return true if private, false if public
   */
  public boolean isPrivate() {
    return mPrivate;
  }

  /**
   * Create the topic
   *
   * @param listener the listner for the newly created channel
   */
  public void create(final MMX.OnFinishedListener<MMXChannel> listener) {
    MMXTask<MMXTopic> task = new MMXTask<MMXTopic>(MMX.getMMXClient(), MMX.getHandler()) {
      @Override
      public MMXTopic doRun(MMXClient mmxClient) throws Throwable {
        MMXPubSubManager psm = mmxClient.getPubSubManager();
        MMXTopicOptions options = new MMXTopicOptions()
                .setDescription(mSummary);
        MMXTopic topic = getMMXTopic();
        return psm.createTopic(topic, options);
      }

      @Override
      public void onException(Throwable exception) {
        listener.onFailure(MMX.FailureCode.SERVER_EXCEPTION, new Exception(exception));
      }

      @Override
      public void onResult(MMXTopic result) {
        listener.onSuccess(MMXChannel.fromMMXTopic(result));
      }
    };
    task.execute();
  }

  /**
   * Delete the topic
   *
   * @param listener the listener for success or failure
   */
  public void delete(final MMX.OnFinishedListener<Void> listener) {
    MMX.MMXStatusTask task = new MMX.MMXStatusTask(listener) {
      @Override
      public MMXStatus doRun(MMXClient mmxClient) throws Throwable {
        MMXPubSubManager psm = mmxClient.getPubSubManager();
        return psm.deleteTopic(getMMXTopic());
      }
    };
    task.execute();
  }

  /**
   * Subscribes the current user from this channel
   *
   * @param listener the listener for the subscription id
   */
  public void subscribe(final MMX.OnFinishedListener<String> listener) {
    MMXTask<String> task = new MMXTask<String>(MMX.getMMXClient(), MMX.getHandler()) {
      @Override
      public String doRun(MMXClient mmxClient) throws Throwable {
        MMXPubSubManager psm = mmxClient.getPubSubManager();
        return psm.subscribe(getMMXTopic(), false);
      }

      @Override
      public void onException(Throwable exception) {
        listener.onFailure(MMX.FailureCode.SERVER_EXCEPTION, exception);
      }

      @Override
      public void onResult(String result) {
        listener.onSuccess(result);
      }
    };
    task.execute();
  }

  /**
   * Unsubscribes the current user from this channel.
   *
   * @param listener the listener for success or failure
   */
  public void unsubscribe(final MMX.OnFinishedListener<Boolean> listener) {
    MMXTask<Boolean> task = new MMXTask<Boolean>(MMX.getMMXClient(), MMX.getHandler()) {
      @Override
      public Boolean doRun(MMXClient mmxClient) throws Throwable {
        MMXPubSubManager psm = mmxClient.getPubSubManager();
        //unsubscribe from all devices
        return psm.unsubscribe(getMMXTopic(), null);
      }

      @Override
      public void onException(Throwable exception) {
        listener.onFailure(MMX.FailureCode.SERVER_EXCEPTION, exception);
      }

      @Override
      public void onResult(Boolean result) {
        listener.onSuccess(result);
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
   */
  public String publish(Map<String, String> messageContent,
                      final MMX.OnFinishedListener<String> listener) {
    MMXMessage message = new MMXMessage.Builder()
            .channel(this)
            .content(messageContent)
            .build();
    return message.send(listener);
  }

  /**
   * The basic fields that are included in an invitation.
   */
  public static class MMXInviteInfo {
    private static final String KEY_TEXT = "text";
    private static final String KEY_CHANNEL_NAME = "channelName";
    private static final String KEY_CHANNEL_SUMMARY = "channelSummary";
    private static final String KEY_CHANNEL_IS_PRIVATE = "channelIsPrivate";
    private MMXChannel mChannel;
    private MMXUser mInvitee;
    private MMXUser mInviter;
    private String mText;

    private MMXInviteInfo(MMXUser invitee, MMXUser inviter, MMXChannel channel, String text) {
      mInvitee = invitee;
      mInviter = inviter;
      mChannel = channel;
      mText = text;
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
     * The text for this invite
     *
     * @return the text for this invite
     */
    public String getText() {
      return mText;
    }

    protected final HashMap<String, String> buildMessageContent() {
      HashMap<String,String> content = new HashMap<String, String>();
      if (getText() != null) {
        content.put(KEY_TEXT, mText);
      }
      content.put(KEY_CHANNEL_NAME, mChannel.getName());
      content.put(KEY_CHANNEL_SUMMARY, mChannel.getSummary());
      content.put(KEY_CHANNEL_IS_PRIVATE, String.valueOf(mChannel.isPrivate()));
      return content;
    }

    static MMXInviteInfo fromMMXMessage(MMXMessage message) {
      Map<String,String> content = message.getContent();
      String text = content.get(KEY_TEXT);
      String channelName = content.get(KEY_CHANNEL_NAME);
      String channelSummary = content.get(KEY_CHANNEL_SUMMARY);
      String channelIsPrivate = content.get(KEY_CHANNEL_IS_PRIVATE);
      MMXChannel channel = new MMXChannel.Builder()
              .name(channelName)
              .summary(channelSummary)
              .setPrivate(Boolean.parseBoolean(channelIsPrivate))
              .build();
      return new MMXInviteInfo(MMX.getCurrentUser(), message.getSender(), channel, text);
    }
  }

  /**
   * The MMXInvite class is used when sending invites for channels.
   *
   * @see #inviteUser(MMXUser, String, MMX.OnFinishedListener)
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

    private void send(final MMX.OnFinishedListener<MMXInvite> listener) {
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
      message.send(new MMX.OnFinishedListener<String>() {
        public void onSuccess(String result) {
          if (listener != null) {
            listener.onSuccess(MMXInvite.this);
          }
        }

        public void onFailure(MMX.FailureCode code, Throwable ex) {
          if (listener != null) {
            listener.onFailure(MMX.FailureCode.SERVER_EXCEPTION, ex);
          }
        }
      });
    }

    /**
     * Accept this invitation.  This will subscribe to the specified topic and notify the inviter.
     *
     * @param text text to include with the response
     * @param listener the listener for success/failure of the operation (optional)
     */
    public void accept(final String text, final MMX.OnFinishedListener<MMXInvite> listener) {
      if (!mIncoming) {
        throw new RuntimeException("Can't accept an outgoing invite");
      }

      MMXChannel channel = mInviteInfo.getChannel();
      channel.subscribe(new MMX.OnFinishedListener<String>() {
        @Override
        public void onSuccess(String result) {
          MMXMessage response = buildResponse(true, text);
          response.send(new MMX.OnFinishedListener<String>() {
            @Override
            public void onSuccess(String result) {
              if (listener != null) {
                listener.onSuccess(MMXInvite.this);
              }
            }

            @Override
            public void onFailure(MMX.FailureCode code, Throwable ex) {
              if (listener != null) {
                listener.onFailure(MMX.FailureCode.SERVER_EXCEPTION, ex);
              }
            }
          });
        }

        @Override
        public void onFailure(MMX.FailureCode code, Throwable ex) {
          if (listener != null) {
            listener.onFailure(MMX.FailureCode.SERVER_EXCEPTION, ex);
          }
        }
      });
    }

    /**
     * Decline this invitation.  This will notify the inviter.
     *
     * @param text text to include with the response
     * @param listener the listener for success/failure of the operation (optional)
     */
    public void decline(String text, final MMX.OnFinishedListener<MMXInvite> listener) {
      if (!mIncoming) {
        throw new RuntimeException("Can't reject an outgoing invite");
      }
      MMXMessage response = buildResponse(false, text);
      response.send(new MMX.OnFinishedListener<String>() {
        @Override
        public void onSuccess(String result) {
          if (listener != null) {
            listener.onSuccess(MMXInvite.this);
          }
        }

        @Override
        public void onFailure(MMX.FailureCode code, Throwable ex) {
          if (listener != null) {
            listener.onFailure(MMX.FailureCode.SERVER_EXCEPTION, ex);
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

  /**
   * Sends an invitation to the specified user for this channel.
   *
   * @param invitee the invitee
   * @param invitationText the text to include in the invite
   * @param listener the listener for success/failure of this operation
   */
  public void inviteUser(final MMXUser invitee, final String invitationText,
                         final MMX.OnFinishedListener<MMXInvite> listener) {
    MMXInviteInfo inviteInfo = new MMXInviteInfo(invitee, MMX.getCurrentUser(), this, invitationText);
    MMXInvite invite = new MMXInvite(inviteInfo, false);
    invite.send(listener);
  }

  /**
   * Retrieves all the subscribers for this channel.
   *
   * @param limit the maximum number of subscribers to return
   * @param listener the listener for the subscribers
   */
  public void getAllSubscribers(final int limit, final MMX.OnFinishedListener<ListResult<MMXUser>> listener) {
    MMXTask<MMXResult<List<UserInfo>>> task =
            new MMXTask<MMXResult<List<UserInfo>>> (MMX.getMMXClient(), MMX.getHandler()) {
      @Override
      public MMXResult<List<UserInfo>> doRun(MMXClient mmxClient) throws Throwable {
        MMXPubSubManager psm = mmxClient.getPubSubManager();
        return psm.getSubscribers(getMMXTopic(), limit);
      }

      @Override
      public void onException(Throwable exception) {
        listener.onFailure(MMX.FailureCode.SERVER_EXCEPTION, exception);
      }

      @Override
      public void onResult(MMXResult<List<UserInfo>> result) {
        ArrayList<MMXUser> users = new ArrayList<MMXUser>();
        for (UserInfo userInfo : result.getResult()) {
          users.add(new MMXUser.Builder()
                  .displayName(userInfo.getDisplayName())
                  .username(userInfo.getUserId())
                  .build());
        }
        listener.onSuccess(new ListResult<MMXUser>(result.getTotal(), users));
      }
    };
    task.execute();
  }

  /**
   * Find the channel that starts with the specified text.
   *
   * @param startsWith the search string
   * @param limit the maximum number of results to return
   * @param listener the listener for the query results
   */
  public static void findByName(final String startsWith, final int limit,
                                final MMX.OnFinishedListener<ListResult<MMXChannel>> listener) {
    MMXTask<ListResult<MMXChannel>> task = new MMXTask<ListResult<MMXChannel>>(
            MMX.getMMXClient(), MMX.getHandler()) {
      @Override
      public ListResult<MMXChannel> doRun(MMXClient mmxClient) throws Throwable {
        MMXPubSubManager psm = mmxClient.getPubSubManager();
        TopicAction.TopicSearch search = new TopicAction.TopicSearch()
                .setTopicName(startsWith, SearchAction.Match.PREFIX);
        MMXTopicSearchResult searchResult = psm.searchBy(SearchAction.Operator.AND, search, limit);
        List<MMXChannel> channels = fromTopicInfos(searchResult.getResults());
        return new ListResult<MMXChannel>(searchResult.getTotal(), channels);
      }

      @Override
      public void onResult(ListResult<MMXChannel> result) {
        //build the query result
        listener.onSuccess(result);
      }

      @Override
      public void onException(Throwable exception) {
        listener.onFailure(MMX.FailureCode.SERVER_EXCEPTION, exception);
      }
    };
    task.execute();
  }

  /**
   * Query for the specified tags (inclusive)
   *
   * @param tags the tags to match
   * @param listener the listener for the query results
   */
  public static void findByTags(final Set<String> tags, final int limit,
                                final MMX.OnFinishedListener<ListResult> listener) {
    MMXTask<ListResult> task = new MMXTask<ListResult>(
            MMX.getMMXClient(), MMX.getHandler()) {
      @Override
      public ListResult doRun(MMXClient mmxClient) throws Throwable {
        MMXPubSubManager psm = mmxClient.getPubSubManager();
        TopicAction.TopicSearch search = new TopicAction.TopicSearch()
                .setTags(new ArrayList<String>(tags));
        MMXTopicSearchResult searchResult = psm.searchBy(SearchAction.Operator.AND, search, limit);
        List<MMXChannel> channels =fromTopicInfos(searchResult.getResults());
        return new ListResult(searchResult.getTotal(), channels);
      }

      @Override
      public void onResult(ListResult result) {
        //build the query result
        listener.onSuccess(result);
      }

      @Override
      public void onException(Throwable exception) {
        listener.onFailure(MMX.FailureCode.SERVER_EXCEPTION, exception);
      }
    };
    task.execute();
  }

  static MMXChannel fromMMXTopic(MMXTopic topic) {
    if (topic == null) {
      return null;
    }
    return new MMXChannel()
            .name(topic.getName())
            .ownerUsername(topic.getUserId());
  }


  private static List<MMXChannel> fromTopicInfos(List<MMXTopicInfo> topicInfos) throws MMXException {
    MMXPubSubManager psm = MMX.getMMXClient().getPubSubManager();

    //get subscriptions
    List<MMXSubscription> subscriptions = psm.listAllSubscriptions();
    HashMap<MMXTopic, MMXSubscription> subMap = new HashMap<MMXTopic, MMXSubscription>();
    for (MMXSubscription subscription : subscriptions) {
      subMap.put(subscription.getTopic(), subscription);
    }

    //get topic summaries
    HashMap<MMXTopic, MMXTopicInfo> topicInfoMap = new HashMap<MMXTopic, MMXTopicInfo>();
    ArrayList<MMXTopic> topics = new ArrayList<MMXTopic>(topicInfos.size());
    for (MMXTopicInfo info : topicInfos) {
      MMXTopic topic = info.getTopic();
      topics.add(topic);
      topicInfoMap.put(topic, info);
    }
    List<TopicSummary> summaries = psm.getTopicSummary(topics, null, null);

    ArrayList<MMXChannel> channels = new ArrayList<MMXChannel>();
    for (TopicSummary summary : summaries) {
      MMXTopic topic = summary.getTopicNode();
      MMXTopicInfo info = topicInfoMap.get(topic);

      channels.add(new MMXChannel.Builder()
                      .lastTimeActive(summary.getLastPubTime())
                      .name(topic.getName())
                      .numberOfMessages(summary.getCount())
                      .ownerUsername(info.getCreator().getUserId())
                      .subscribed(subMap.containsKey(topic))
                      .summary(info.getDescription())
                      .setPrivate(topic.isUserTopic())
                      .build()
      );
    }
    return channels;
  }
}
