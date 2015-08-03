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
package com.magnet.mmx.client;

import android.os.Handler;

import com.magnet.mmx.client.common.Log;
import com.magnet.mmx.client.common.MMXException;
import com.magnet.mmx.client.common.MMXGlobalTopic;
import com.magnet.mmx.client.common.MMXMessage;
import com.magnet.mmx.client.common.MMXMessageStatus;
import com.magnet.mmx.client.common.MMXPayload;
import com.magnet.mmx.client.common.MMXPersonalTopic;
import com.magnet.mmx.client.common.MMXSubscription;
import com.magnet.mmx.client.common.MMXTopicInfo;
import com.magnet.mmx.client.common.MMXTopicSearchResult;
import com.magnet.mmx.client.common.MMXVisibleTopic;
import com.magnet.mmx.client.common.PubSubManager;
import com.magnet.mmx.client.common.TopicExistsException;
import com.magnet.mmx.client.common.TopicNotFoundException;
import com.magnet.mmx.client.common.TopicPermissionException;
import com.magnet.mmx.protocol.Constants;
import com.magnet.mmx.protocol.MMXStatus;
import com.magnet.mmx.protocol.MMXTopic;
import com.magnet.mmx.protocol.SearchAction;
import com.magnet.mmx.protocol.TopicAction;
import com.magnet.mmx.protocol.MMXTopicOptions;
import com.magnet.mmx.protocol.TopicSummary;
import com.magnet.mmx.util.MMXQueue;
import com.magnet.mmx.util.MMXQueue.Item;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The MMXPubSubManager allows users to create or delete topics, subscribe or
 * unsubscribe to them, and publishes items to them.
 * <p />
 * The delivery of a published item is based on event model.  That is, only the
 * latest published item will be delivered to all online subscribers.  If there
 * are multiple items published while a subscriber is offline, all previously
 * published items except the last one will be lost unless the topic is created
 * with max persisted items not equal to 0.  Currently only the last published
 * item will be delivered to the reconnected subscriber, but the subscriber can
 * retrieve the missed items for a topic via {@link #getItems(com.magnet.mmx.protocol.MMXTopic, com.magnet.mmx.protocol.TopicAction.FetchOptions)}.
 * <p />
 * To publish an item, it can be done by {@link #publish(com.magnet.mmx.protocol.MMXTopic, MMXPayload)}.
 * If this client is subscribed to a topic, published items will be received through the callback
 * {@link com.magnet.mmx.client.MMXClient.MMXListener#onPubsubItemReceived(MMXClient, com.magnet.mmx.protocol.MMXTopic, com.magnet.mmx.client.common.MMXMessage)}
 */
public final class MMXPubSubManager extends MMXManager {
  private static final String TAG = MMXPubSubManager.class.getSimpleName();
  private PubSubManager mPubSubManager = null;

  MMXPubSubManager(MMXClient mmxClient, Handler handler) {
    super(mmxClient, handler);
    onConnectionChanged();
  }

  /**
   * Subscribe to a global topic for current user or current device.  If
   * the subscription already exists, the original subscription ID will be
   * returned.  A user-based subscription (i.e. <code>thisDeviceOnly</code> is
   * false) means that a published item will be delivered to all devices
   * registered by the user.  A device-based subscription means a published item
   * will only be delivered to the specific device registered by the user.
   * @see com.magnet.mmx.client.common.MMXGlobalTopic
   *
   * @param topic the topic object
   * @param thisDeviceOnly true for current device only; false for current user
   * @return a subscription ID
   * @throws TopicNotFoundException
   * @throws MMXException
   */
  public String subscribe(MMXTopic topic, boolean thisDeviceOnly) 
      throws TopicNotFoundException, MMXException {
    checkDestroyed();
    return mPubSubManager.subscribe(topic, thisDeviceOnly);
  }

  /**
   * Unsubscribe a specific subscription or all subscriptions from global topic.
   * If the <code>subscriptionId</code> is null, all subscriptions to the topic by
   * the current user will be cancelled.
   *
   * @param topic a topic object
   * @param subscriptionId a subscription ID for a device, or null for all devices
   * @return false if topic is not subscribed; true otherwise
   * @throws TopicNotFoundException
   * @throws MMXException
   */
  public boolean unsubscribe(MMXTopic topic, String subscriptionId)
      throws TopicNotFoundException, MMXException {
    checkDestroyed();
    return mPubSubManager.unsubscribe(topic, subscriptionId);
  }

  /**
   * Add tags to a global topic.  The tags must not be null or empty, and
   * each tag cannot be longer than 25 characters; otherwise MMXException with
   * BAD_REQUEST status code will be thrown.
   *
   * @param topic a topic object
   * @param tags a list of tags to be added
   * @return the status for this request
   * @throws TopicNotFoundException
   * @throws MMXException
   */
  public MMXStatus addTags(MMXTopic topic, List<String> tags) 
      throws TopicNotFoundException, MMXException {
    checkDestroyed();
    return mPubSubManager.addTags(topic, tags);
  }

  /**
   * Remove tags from a global topic.  The tags must not be null or empty, and
   * each tag cannot be longer than 25 characters; otherwise MMXException with
   * BAD_REQUEST status code will be thrown.
   *
   * @param topic a topic object
   * @param tags a list of tags to be removed
   * @return the status for this request
   * @throws TopicNotFoundException
   * @throws MMXException
   */
  public MMXStatus removeTags(MMXTopic topic, List<String> tags) 
      throws TopicNotFoundException, MMXException {
    checkDestroyed();
    return mPubSubManager.removeTags(topic, tags);
  }

  /**
   * Create a global topic with options.  Without specifying the options, the
   * topic is searchable, publicly accessible, persisted items with 1 item,
   * anyone can publish.
   *
   * @param topic a topic
   * @param options topic creation options, or null
   * @throws TopicExistsException
   * @throws MMXException
   */
  public MMXTopic createTopic(MMXTopic topic, MMXTopicOptions options) 
      throws TopicExistsException, MMXException {
    checkDestroyed();
    return mPubSubManager.createTopic(topic, options);
  }

  /**
   * Delete a global topic which was created by current user.
   *
   * @param topic a topic
   * @throws TopicNotFoundException
   * @throws TopicPermissionException
   * @throws MMXException
   */
  public MMXStatus deleteTopic(MMXTopic topic) 
      throws TopicNotFoundException, TopicPermissionException, MMXException {
    checkDestroyed();
    return mPubSubManager.deleteTopic(topic);
  }

  /**
   * Get the tags from a global topic.

   * @param topic a topic object
   * @return a tag info object
   * @throws TopicNotFoundException
   * @throws MMXException
   */
  public TopicAction.TopicTags getAllTags(MMXTopic topic) 
          throws TopicNotFoundException, MMXException {
    checkDestroyed();
    return mPubSubManager.getAllTags(topic);
  }

  /**
   * Set tags for a global topic.  All existing tags will be replaced by the
   * new values.
   *
   * @param topic a topic object
   * @param tags a list of tags or an empty list
   * @return the status
   * @throws TopicNotFoundException
   * @throws MMXException
   */
  public MMXStatus setAllTags(MMXTopic topic, List<String> tags)
          throws TopicNotFoundException, MMXException {
    checkDestroyed();
    return mPubSubManager.setAllTags(topic, tags);
  }

  /**
   * Get the items published within a date range from a global topic.
   * The caller must be an owner or subscriber to the topic; otherwise,
   * TopicPermissionException will be thrown.
   *
   * @param topic a topic object
   * @param options (optional) fetch options, or null
   * @return alist of published items, or empty list
   * @throws TopicNotFoundException
   * @throws TopicPermissionException
   * @throws MMXException
   */
  public List<MMXMessage> getItems(MMXTopic topic, TopicAction.FetchOptions options)
        throws TopicNotFoundException, TopicPermissionException, MMXException {
    checkDestroyed();
    return mPubSubManager.getItems(topic, options);
  }

  /**
   * Get the items by the published item identifiers.
   * @param topic a topic object
   * @param itemIds A list of published item identifiers.
   * @return A Map of item identifiers and published items.
   * @throws TopicNotFoundException
   * @throws TopicPermissionException
   * @throws MMXException
   */
  public Map<String, MMXMessage> getItemsByIds(MMXTopic topic, List<String> itemIds)
      throws TopicNotFoundException, TopicPermissionException, MMXException {
    checkDestroyed();
    return mPubSubManager.getItemsByIds(topic, itemIds);
  }
  
  /**
   * @hide
   * Publish an item with a publish ID.  This is for internal use.
   *
   * @param id the identifier to use for this message
   * @param realTopic the full topic node name
   * @param topic the topic name
   * @param payload the payload for this message
   * @return the identifier for the published message
   * @throws TopicNotFoundException
   * @throws TopicPermissionException
   * @throws MMXException
   */
  String publishToTopic(String id, String realTopic, String topic,
                        MMXPayload payload) 
      throws TopicNotFoundException, TopicPermissionException, MMXException {
    checkDestroyed();
    return mPubSubManager.publishToTopic(id, realTopic, topic, payload);
  }

  /**
   * Publish a payload to a topic. The topic must be existing and be created
   * with {@link com.magnet.mmx.protocol.TopicAction.PublisherType#anyone} or 
   * {@link com.magnet.mmx.protocol.TopicAction.PublisherType#subscribers} for
   * non-owner; otherwise, TopicPermissionException will be thrown.
   *
   * If the MMXClient is not connected, the publishing of this payload will
   * be queued and published upon the next successful connection.
   *
   * @param topic a topic object
   * @param payload a non-null application specific payload
   * @return a published item identifier
   * @throws TopicNotFoundException
   * @throws TopicPermissionException
   * @throws MMXException
   */
  public String publish(MMXTopic topic, MMXPayload payload)
      throws TopicNotFoundException, TopicPermissionException, MMXException {
    checkDestroyed();
    return mPubSubManager.publish(topic, payload);
  }

  /**
   * @hide
   * Publish an item to a topic under the current user name-space with
   * auto-creation.  If the topic does not exist, it will be created with the
   * specified options.  If no option is specified, the topic will be configured
   * with owner as the sole publisher and 100 persistent items.
   *
   * @param topic a personal user topic
   * @param payload a non-null application specific payload
   * @param options null for default, or a specified topic options
   * @return a published item identifier
   * @throws MMXException
   */
  String publish(MMXPersonalTopic topic, MMXPayload payload,
                        MMXTopicOptions options) throws MMXException {
    checkDestroyed();
    return mPubSubManager.publish(topic, payload, options);
  }

  /**
   * @hide
   * Clear all published items from a global topic created by the current user
   * or from a user topic under the current user name-space.
   *
   * @param topic a global topic or personal topic
   * @throws TopicNotFoundException
   * @throws TopicPermissionException
   * @throws MMXException
   */
  boolean clearAllItems(MMXVisibleTopic topic) throws
          TopicNotFoundException, TopicPermissionException, MMXException {
    checkDestroyed();
    return mPubSubManager.clearAllItems(topic);
  }

  /**
   * Search for topics by topic attributes and/or tags.
   *
   * @param operator the AND or OR operator
   * @param attributes single or multi-values search attributes
   * @param maxRows null for the server default, or a max number of rows to be returned
   * @return the search result
   * @throws MMXException
   */
  public MMXTopicSearchResult searchBy(SearchAction.Operator operator,
                                                 TopicAction.TopicSearch attributes,
                                                 Integer maxRows) throws MMXException {
    checkDestroyed();
    return mPubSubManager.searchBy(operator, attributes, maxRows);
  }

  /**
   * Search topics by all matching tags or any matching tags.
   *
   * @param tags a list of tags
   * @param matchAll true for all matching tags, false for any matching tags
   * @return a list of TopicInfo objects for the matching topics
   * @throws MMXException
   */
  public List<MMXTopicInfo> searchByTags(List<String> tags,
                                                     boolean matchAll) throws MMXException {
    checkDestroyed();
    return mPubSubManager.searchByTags(tags, matchAll);
  }

  /**
   * Get topic detail information by topic name.
   * 
   * @param topic a topic name
   * @return the detail topic information
   * @throws TopicNotFoundException
   * @throws MMXException
   */
  public MMXTopicInfo getTopic(MMXTopic topic) 
            throws TopicNotFoundException, MMXException {
    checkDestroyed();
    return mPubSubManager.getTopic(topic);
  }
  
  /**
   * Get the configurable options of a topic.  The returned options can be 
   * updated by calling {@link #updateOptions(MMXTopic, com.magnet.mmx.protocol.MMXTopicOptions)}.
   *
   * @param topic a topic name
   * @return the options of the topic
   * @throws MMXException
   * @throws TopicNotFoundException
   * @see #updateOptions(MMXTopic, com.magnet.mmx.protocol.MMXTopicOptions)
   */
  public MMXTopicOptions getOptions(MMXTopic topic)
          throws TopicNotFoundException, MMXException {
    checkDestroyed();
    return mPubSubManager.getOptions(topic);
  }

  /**
   * Update the options for a global topic or personal user topic which
   * are owned by the current user.  If a field in <code>options</code> is null,
   * its value will remain unchanged.
   *
   * @param topic the topic to be updated
   * @param options the topic options to be updated
   * @throws TopicNotFoundException
   * @throws TopicPermissionException
   * @throws MMXException
   * @see #getOptions(MMXTopic)
   */
  public void updateOptions(MMXTopic topic, MMXTopicOptions options)
          throws TopicNotFoundException, TopicPermissionException, MMXException {
    checkDestroyed();
    mPubSubManager.updateOptions(topic, options);
  }

  /**
   * Get the summary information of a list of topics with an optional published
   * date range.  Topics do not have any published items or the items having
   * an out-of-range date will not be returned.
   *
   * @param topics a list of topic nodes to be inquired
   * @param since a published since date, or null
   * @param until a published until date, or null
   * @return a list of summaries
   * @throws MMXException
   */
  public List<TopicSummary> getTopicSummary(List<MMXTopic> topics, Date since, Date until)
          throws MMXException {
    checkDestroyed();
    return mPubSubManager.getTopicSummary(topics, since, until);
  }

  /**
   * Attempts to cancel a pending message with the specified id.  This is
   * client-only functionality and will only work for messages that have not
   * been sent.
   *
   * @param messageId The id of the message to cancel.  This is the value returned by publish()
   * @return true if canceled successfully
   */
  public synchronized boolean cancelMessage(String messageId) {
    checkDestroyed();
    if (messageId == null) {
      Log.w(TAG, "cancelMessage(): cannot cancel a null messageId, returning false.");
      return false;
    }
    return getMMXClient().cancelMessage(messageId);
  }

  /**
   * Returns all pending publishing messages.  The pending publishing messages
   * can be cancelled.  Published items are "pending" when publish is called while the
   * MMXClient is disconnected.  These items will be published upon the next successful
   * connection.
   *
   * @return a map with message id as the key and MMXTopic as the value
   * @see #cancelMessage(String)
   */ 
  public Map<String, MMXTopic> getPendingItems() {
    checkDestroyed();
    Map<String, MMXQueue.Item> items = getMMXClient().getQueue()
        .getPendingItems(Item.Type.PUBSUB, true);
    Map<String, MMXTopic> pendingItems = new HashMap<String, MMXTopic>(items.size());
    for (Map.Entry<String, MMXQueue.Item> item : items.entrySet()) {
      Item.PubSub pubsub = (Item.PubSub) item.getValue();
      pendingItems.put(item.getKey(), new MMXGlobalTopic(pubsub.getTopic()));
    }
    return pendingItems;
  }

  /**
   * @hide
   * List all global topics in this application.
   *
   * @return a list of topics or an empty list
   * @throws MMXException
   */
  List<MMXTopicInfo> listTopics() throws MMXException {
    checkDestroyed();
    return mPubSubManager.listTopics();
  }

  /**
   * List subscriptions in a topic. If <code>topic</code> is null, it is
   * equivalent to {@link #listAllSubscriptions()}

   * @param topic a topic name
   * @return a list of subscriptions, or an empty list
   * @throws MMXException
   */
  public List<MMXSubscription> listSubscriptions(MMXTopic topic)
          throws MMXException {
    checkDestroyed();
    return mPubSubManager.listSubscriptions(topic);
  }

  /**
   * List all subscriptions for the current user.
   *
   * @return a list of subscriptions, or an empty list
   * @throws MMXException
   */
  public List<MMXSubscription> listAllSubscriptions() throws MMXException {
    checkDestroyed();
    return mPubSubManager.listAllSubscriptions();
  }

  @Override
  void onConnectionChanged() {
    mPubSubManager = PubSubManager.getInstance(getMMXClient().getMMXConnection());
  }

  final PubSubManager getInternalManager() {
    return mPubSubManager;
  }
}
