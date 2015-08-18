package com.magnet.mmx.client.api;

import com.magnet.mmx.client.MMXClient;
import com.magnet.mmx.client.MMXPubSubManager;
import com.magnet.mmx.client.MMXTask;
import com.magnet.mmx.client.common.Log;
import com.magnet.mmx.client.common.MMXException;
import com.magnet.mmx.client.common.MMXGlobalTopic;
import com.magnet.mmx.client.common.MMXSubscription;
import com.magnet.mmx.client.common.MMXTopicInfo;
import com.magnet.mmx.client.common.MMXTopicSearchResult;
import com.magnet.mmx.protocol.MMXStatus;
import com.magnet.mmx.protocol.MMXTopic;
import com.magnet.mmx.protocol.MMXTopicOptions;
import com.magnet.mmx.protocol.SearchAction;
import com.magnet.mmx.protocol.TopicAction;
import com.magnet.mmx.protocol.TopicSummary;

import java.util.ArrayList;
import java.util.Collections;
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
     * Build the channel object
     *
     * @return the channel
     */
    public MMXChannel build() {
      return mChannel;
    }
  }

  public static class FindResult {
    public final int totalCount;
    public final List<MMXChannel> channels;

    private FindResult(int totalCount, List<MMXChannel> channels) {
      this.totalCount = totalCount;
      this.channels = Collections.unmodifiableList(channels);
    }
  }

  private String mName;
  private String mSummary;
  private String mOwnerUsername;
  private Integer mNumberOfMessages;
  private Date mLastTimeActive;
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

  private MMXTopic getMMXTopic() {
    //TODO: handle private topics
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
        return psm.createTopic(getMMXTopic(), options);
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
   * Find the channel that starts with the specified text.
   *
   * @param startsWith the search string
   * @param limit the maximum number of results to return
   * @param listener the listener for the query results
   */
  public static void findByName(final String startsWith, final int limit,
                                final MMX.OnFinishedListener<FindResult> listener) {
    MMXTask<FindResult> task = new MMXTask<FindResult>(
            MMX.getMMXClient(), MMX.getHandler()) {
      @Override
      public FindResult doRun(MMXClient mmxClient) throws Throwable {
        MMXPubSubManager psm = mmxClient.getPubSubManager();
        TopicAction.TopicSearch search = new TopicAction.TopicSearch()
                .setTopicName(startsWith, SearchAction.Match.PREFIX);
        MMXTopicSearchResult searchResult = psm.searchBy(SearchAction.Operator.AND, search, limit);
        List<MMXChannel> channels = fromTopicInfos(searchResult.getResults());
        return new FindResult(searchResult.getTotal(), channels);
      }

      @Override
      public void onResult(FindResult result) {
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
                                final MMX.OnFinishedListener<FindResult> listener) {
    MMXTask<FindResult> task = new MMXTask<FindResult>(
            MMX.getMMXClient(), MMX.getHandler()) {
      @Override
      public FindResult doRun(MMXClient mmxClient) throws Throwable {
        MMXPubSubManager psm = mmxClient.getPubSubManager();
        TopicAction.TopicSearch search = new TopicAction.TopicSearch()
                .setTags(new ArrayList<String>(tags));
        MMXTopicSearchResult searchResult = psm.searchBy(SearchAction.Operator.AND, search, limit);
        List<MMXChannel> channels =fromTopicInfos(searchResult.getResults());
        return new FindResult(searchResult.getTotal(), channels);
      }

      @Override
      public void onResult(FindResult result) {
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
                      .build()
      );
    }
    return channels;
  }
}
