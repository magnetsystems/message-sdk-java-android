package com.magnet.mmx.client.api;

import com.magnet.mmx.client.common.MMXid;
import com.magnet.mmx.protocol.MMXTopic;

import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The MMXChannel class representing the Topic/Feed/PubSub model.
 */
public class MMXChannel {
  public static class QueryResult {
    public final int totalCount;
    public final List<MMXChannel> channels;

    private QueryResult(int totalCount, List<MMXChannel> channels) {
      this.totalCount = totalCount;
      this.channels = Collections.unmodifiableList(channels);
    }
  }

  private String mName;
  private String mSummary;
  private MMXid mOwner;
  private Integer mNumberOfMessages;
  private Date mLastTimeActive;
  private Set<String> mTags;
  private Boolean mSubscribed;
  private Boolean isPublicChannel = false;

  /**
   * Default constructor
   */
  public MMXChannel() {

  }

  /**
   * Set the name of this channel
   *
   * @param name the name
   * @return this MMXChannel object
   */
  public MMXChannel name(String name) {
    this.mName = name;
    return this;
  }

  /**
   * The name fo this channel
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
  public MMXChannel summary(String summary) {
    this.mSummary = summary;
    return this;
  }

  /**
   * Set flag to indicate that the channel is public. All channels are private by default.
   */
  public MMXChannel publicChannel(boolean isPublicChannel) {
    this.isPublicChannel = isPublicChannel;
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
   * @param owner the owner
   * @return this MMXChannel object
   */
  MMXChannel owner(MMXid owner) {
    mOwner = owner;
    return this;
  }

  /**
   * The owner for this channel
   *
   * @return the owner, null if not yet retrieved
   */
  public MMXid getOwner() {
    return mOwner;
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
   * Set the tags for this channel
   *
   * @param tags the tags
   * @return this MMXChannel object
   */
  public MMXChannel tags(Set<String> tags) {
    mTags = tags;
    return this;
  }

  /**
   * The tags for this channel
   *
   * @return the tags, null if not yet retrieved
   */
  public Set<String> getTags() {
    return mTags;
  }

  /**
   * Adds a tag to the current set of tags
   *
   * @param tag the tag
   * @return this MMXChannel object
   */
  public synchronized MMXChannel addTag(String tag) {
    if (mTags == null) {
      mTags = new HashSet<String>();
    }
    mTags.add(tag);
    return this;
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
  public void create(MagnetMessage.OnFinishedListener<MMXChannel> listener) {
    throw new RuntimeException("Not yet implemented");
  }

  /**
   * Delete the topic
   *
   * @param listener the listener for success or failure
   */
  public void delete(MagnetMessage.OnFinishedListener<Boolean> listener) {
    throw new RuntimeException("Not yet implemented");
  }

  /**
   * Subscribes the current user from this channel
   *
   * @param listener the listener for the subscription id
   */
  public void subscribe(MagnetMessage.OnFinishedListener<String> listener) {
    throw new RuntimeException("Not yet implemented");
  }

  /**
   * Unsubscribes the current user from this channel.
   *
   * @param listener the listener for success or failure
   */
  public void unsubscribe(MagnetMessage.OnFinishedListener<Boolean> listener) {
    throw new RuntimeException("Not yet implemented");
  }

  /**
   * Publishes a message to this channel.  If this channel does not exist, it will
   * be created first.
   *
   * @param message the message to publish
   * @param listener the listener for the message id
   */
  public void publish(MMXMessage message,
                      MagnetMessage.OnFinishedListener<String> listener) {
    throw new RuntimeException("Not yet implemented");
  }

  /**
   * Query for exact match of the specified name
   *
   * @param name the name
   * @param listener the listener for the query results
   */
  public static void queryByName(String name,
                                 MagnetMessage.OnFinishedListener<QueryResult> listener) {
    throw new RuntimeException("Not yet implemented");
  }

  /**
   * Query for the specified tags (inclusive)
   *
   * @param tags the tags to match
   * @param listener the listener for the query results
   */
  public static void queryByTags(Set<String> tags,
                                 MagnetMessage.OnFinishedListener<QueryResult> listener) {
    throw new RuntimeException("Not yet implemented");

  }

  static MMXChannel fromTopic(MMXTopic topic) {
    throw new RuntimeException("Not yet implemented");
  }

}
