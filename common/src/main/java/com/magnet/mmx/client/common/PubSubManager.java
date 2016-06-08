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

package com.magnet.mmx.client.common;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jivesoftware.smack.XMPPException.XMPPErrorException;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.PacketExtension;
import org.jivesoftware.smack.packet.XMPPError;
import org.jivesoftware.smack.packet.XMPPError.Condition;
import org.jivesoftware.smack.packet.IQ.Type;
import org.jivesoftware.smack.provider.PacketExtensionProvider;
import org.jivesoftware.smack.provider.ProviderManager;
import org.jivesoftware.smack.util.PacketParserUtils;
import org.jivesoftware.smackx.pubsub.Affiliation;
import org.jivesoftware.smackx.pubsub.ConfigureForm;
import org.jivesoftware.smackx.pubsub.FormType;
import org.jivesoftware.smackx.pubsub.LeafNode;
import org.jivesoftware.smackx.pubsub.Node;
import org.jivesoftware.smackx.pubsub.NodeExtension;
import org.jivesoftware.smackx.pubsub.PayloadItem;
import org.jivesoftware.smackx.pubsub.PubSubElementType;
import org.jivesoftware.smackx.pubsub.PublishModel;
import org.jivesoftware.smackx.pubsub.Subscription;
import org.jivesoftware.smackx.pubsub.packet.PubSub;
import org.jivesoftware.smackx.pubsub.packet.PubSubNamespace;
import org.jivesoftware.smackx.xdata.FormField;
import org.xmlpull.v1.XmlPullParser;

import com.magnet.mmx.client.common.MMXPayloadMsgHandler.MMXPacketExtension;
import com.magnet.mmx.protocol.Constants;
import com.magnet.mmx.protocol.Constants.PubSubCommand;
import com.magnet.mmx.protocol.MMXStatus;
import com.magnet.mmx.protocol.MMXTopic;
import com.magnet.mmx.protocol.MMXTopicId;
import com.magnet.mmx.protocol.MMXTopicOptions;
import com.magnet.mmx.protocol.MMXid;
import com.magnet.mmx.protocol.SearchAction;
import com.magnet.mmx.protocol.SendLastPublishedItems;
import com.magnet.mmx.protocol.StatusCode;
import com.magnet.mmx.protocol.TagSearch;
import com.magnet.mmx.protocol.TagSearch.Operator;
import com.magnet.mmx.protocol.TopicAction;
import com.magnet.mmx.protocol.TopicAction.CreateRequest;
import com.magnet.mmx.protocol.TopicAction.CreateResponse;
import com.magnet.mmx.protocol.TopicAction.DeleteRequest;
import com.magnet.mmx.protocol.TopicAction.FetchOptions;
import com.magnet.mmx.protocol.TopicAction.FetchRequest;
import com.magnet.mmx.protocol.TopicAction.FetchResponse;
import com.magnet.mmx.protocol.TopicAction.GetTopicsResponse;
import com.magnet.mmx.protocol.TopicAction.ItemsByIdsRequest;
import com.magnet.mmx.protocol.TopicAction.ListType;
import com.magnet.mmx.protocol.TopicAction.MMXPublishedItem;
import com.magnet.mmx.protocol.TopicAction.PublisherType;
import com.magnet.mmx.protocol.TopicAction.RetractAllRequest;
import com.magnet.mmx.protocol.TopicAction.RetractRequest;
import com.magnet.mmx.protocol.TopicAction.RetractResponse;
import com.magnet.mmx.protocol.TopicAction.SubscribeRequest;
import com.magnet.mmx.protocol.TopicAction.SubscribeResponse;
import com.magnet.mmx.protocol.TopicAction.SubscribersRequest;
import com.magnet.mmx.protocol.TopicAction.SubscribersResponse;
import com.magnet.mmx.protocol.TopicAction.SummaryRequest;
import com.magnet.mmx.protocol.TopicAction.SummaryResponse;
import com.magnet.mmx.protocol.TopicAction.TopicQueryResponse;
import com.magnet.mmx.protocol.TopicAction.TopicSearchRequest;
import com.magnet.mmx.protocol.TopicAction.TopicTags;
import com.magnet.mmx.protocol.TopicAction.UnsubscribeRequest;
import com.magnet.mmx.protocol.TopicInfo;
import com.magnet.mmx.protocol.TopicSummary;
import com.magnet.mmx.protocol.UserInfo;
import com.magnet.mmx.util.MMXQueue;
import com.magnet.mmx.util.MMXQueue.Item;
import com.magnet.mmx.util.TagUtil;
import com.magnet.mmx.util.TopicHelper;
import com.magnet.mmx.util.Utils;
import com.magnet.mmx.util.XIDUtil;

/**
 * The Pub/Sub Manager allows users to create or delete topics, subscribe or
 * unsubscribe to them, and publishes items to them.  The Pub/Sub Manager
 * provides two levels of hierarchy to minimize name collisions:
 * global topics and user topics.  A global topic name has a unique name within
 * the application; a user topic name has a unique name under a user ID
 * within the application.  A <i>personal</i> topic is just a user topic under
 * the current user.  A topic has a path hierarchy with '/' as the separator.
 * <p/>
 * Technically all topics are in a tree hierarchy and have a path-like syntax
 * with "/" as a separator.  The tree structure is modeled after a file system:
 * folder (collection node) and file (leaf node.) A collection node can contain
 * collection nodes and leaf nodes, but a leaf node can contain only published
 * items.  Each node in the path can be subscribed separately, but only the leaf
 * node can be published to.  Subscribing to a collection node allows the
 * subscriber to be notified when an item is published to any leaf nodes.
 * <p/>
 * The delivery of a published item is based on event model.  That is, only the
 * latest published item will be delivered to all online subscribers.  If there
 * are multiple items published while a subscriber is offline, all previously
 * published items except the last one will be lost unless the topic is created
 * with max persisted items not equal to 0.  Currently only the last published
 * item will be delivered to the reconnected subscriber, but the subscriber can
 * retrieve the missed items for a topic via {@link #getItems(MMXTopic, FetchOptions)}.
 * <p/>
 * To publish an item, it can be done by {@link #publish(MMXTopic, MMXPayload)}.
 * Every published item will be received through the callback
 * {@link MMXMessageListener#onItemReceived(MMXMessage, MMXTopic)}.
 */
public class PubSubManager {
  private final static String TAG = "PubSubManager";
  private final static String FIELD_DESCRIPTION = "pubsub#description";
  private final static String FIELD_SEND_ITEM_SUBSCRIBE = "pubsub#send_item_subscribe";
  private final static String LAST_DELIVERY_FILE = "com.magnet.pubsub-";
  private final static String USER_TOPIC_NOT_ALLOWED = "User topic is not allowed";
  private final static boolean SHOW_USER_TOPICS = false;
  private final static boolean SHOW_USER_TOPIC_SUBSCRIPTIONS = true;
  private final MMXConnection mCon;
  private MappedByteBuffer mBuffer;
  private final String mAppId;
  private org.jivesoftware.smackx.pubsub.PubSubManager mPubSubMgr;
  private final static Creator sCreator = new Creator() {
    @Override
    public Object newInstance(MMXConnection con) {
      return new PubSubManager(con);
    }
  };

  static class PubSubIQHandler<Request, Response> extends MMXIQHandler
                                          <Request, Response> {
    @Override
    public String getElementName() {
      return Constants.MMX;
    }

    @Override
    public String getNamespace() {
      return Constants.MMX_NS_PUBSUB;
    }
  }

  /**
   * @hide
   * Get the instance with a connection.
   * @param con A connection object.
   * @return The instance of PubSubManager.
   */
  public static PubSubManager getInstance(MMXConnection con) {
    return (PubSubManager) con.getManager(TAG, sCreator);
  }

  protected PubSubManager(MMXConnection con) {
    mCon = con;
    mAppId = (mCon.getAppId() == null) ? "*" : mCon.getAppId();
    PubSubIQHandler<Object, Object> iqHandler = new PubSubIQHandler<Object, Object>();
    iqHandler.registerIQProvider();
  }

  private synchronized org.jivesoftware.smackx.pubsub.PubSubManager getPubSubManager() {
    if (mPubSubMgr == null) {
      mPubSubMgr = new org.jivesoftware.smackx.pubsub.PubSubManager(mCon.getXMPPConnection());
    }
    return mPubSubMgr;
  }

  private <T extends Node> T getNode(String nodeId, String topicName)
                    throws TopicNotFoundException, MMXException {
    try {
      return (T) getPubSubManager().getNode(nodeId);
    } catch (XMPPErrorException e) {
      if (XMPPError.Condition.item_not_found.equals(e.getXMPPError().getCondition())) {
        throw new TopicNotFoundException(topicName != null ? topicName : nodeId);
      }
      throw new MMXException(e.getMessage(), e);
    } catch (Throwable e) {
      throw new MMXException(e.getMessage(), e);
    }
  }

  /**
   * Publish a payload to a topic. The topic must be existing and be created
   * with {@link PublisherType#anyone} or {@link PublisherType#subscribers} for
   * non-owner; otherwise, TopicPermissionException will be thrown.
   * @param topic A topic object with topic ID.
   * @param payload A non-null application specific payload.
   * @return A published item ID.
   * @throws TopicNotFoundException
   * @throws TopicPermissionException
   * @throws MMXException
   * @see {@link com.magnet.mmx.protocol.Headers#Headers()}
   * @see {@link MMXMessageListener#onItemReceived(MMXMessage, MMXTopicId)}
   */
  public String publish(MMXTopic topic, MMXPayload payload)
      throws TopicNotFoundException, TopicPermissionException, MMXException {
    return publish(null, topic, payload);
  }

  /**
   * Publish a payload to a topic. The topic must be existing and be created
   * with {@link PublisherType#anyone} or {@link PublisherType#subscribers} for
   * non-owner; otherwise, TopicPermissionException will be thrown.
   * @param messageId the message id for the published message
   * @param topic A topic object with topic ID.
   * @param payload A non-null application specific payload.
   * @return A published item ID.
   * @throws TopicNotFoundException
   * @throws TopicPermissionException
   * @throws MMXException
   * @see {@link com.magnet.mmx.protocol.Headers#Headers()}
   * @see {@link MMXMessageListener#onItemReceived(MMXMessage, MMXTopicId)}
   */
  public String publish(String messageId, MMXTopic topic, MMXPayload payload)
          throws TopicNotFoundException, TopicPermissionException, MMXException {
    if (topic instanceof MMXPersonalTopic) {
      ((MMXPersonalTopic) topic).setUserId(mCon.getUserId());
    }
    String nodeId = getNodeId(topic);
    return publishToTopic(messageId, nodeId, topic.getDisplayName(), payload);
  }

  /**
   * @hide
   * Publish an item to a topic under the current user name-space with
   * auto-creation.  If the topic does not exist, it will be created with the
   * specified options.  If no option is specified, the topic will be configured
   * with owner as the sole publisher and 100 persistent items.
   * @param topic A personal user topic.
   * @param payload A non-null application specific payload.
   * @param options null for default, or a specified topic options.
   * @return A published item ID.
   * @throws MMXException
   * @see {@link MMXMessageListener#onItemReceived(MMXMessage, MMXTopicId)}
   */
  public String publish(MMXPersonalTopic topic, MMXPayload payload,
      MMXTopicOptions options) throws TopicPermissionException, MMXException {
    String nodeId;
    if (topic.getId() != null) {
      nodeId = TopicHelper.toNodeId(mAppId, topic.getId());
    } else {
      try {
        topic.setUserId(mCon.getUserId());
        MMXTopicInfo topicInfo = getTopic(topic);
        nodeId = TopicHelper.toNodeId(mAppId, topicInfo.getTopic().getId());
      } catch (TopicNotFoundException e) {
        if (options == null) {
          // Use a default option if not specified.
          options = new MMXTopicOptions()
            .setPublisherType(TopicAction.PublisherType.owner)
            .setMaxItems(-1);
        }
        MMXTopic newTopic = createTopic(topic, options);
        nodeId = TopicHelper.toNodeId(mAppId, newTopic.getId());
      }
    }
    return publishToTopic(null, nodeId, topic.getDisplayName(), payload);
  }

  /**
   * @hide
   * Publish an item with a publish ID.  This is for internal use.
   * @param itemId An optional item ID.
   * @param nodeId
   * @param topic Topic display name
   * @param payload
   * @return
   * @throws TopicNotFoundException
   * @throws TopicPermissionException
   * @throws MMXException
   */
  public String publishToTopic(String itemId, String nodeId, String topic,
      MMXPayload payload) throws TopicNotFoundException,
      TopicPermissionException, MMXException {
    if (payload.getSize() > MMXPayload.getMaxSizeAllowed()) {
      throw new MMXException("Payload size exceeds "+
                              MMXPayload.getMaxSizeAllowed()+" bytes",
                              MMXException.REQUEST_TOO_LARGE);
    }

    MMXQueue queue = mCon.getQueue();
    if (itemId == null) {
      itemId = mCon.genId();
    }
    if (mCon.isConnected()) {
      try {
        // XMPP does not include publisher during delivery; MMX includes the
        // authenticated (after online) publisher to the item.
        payload.setFrom(mCon.getXID());

        // TODO: smack caches the node.  It returns the node if the node is
        // deleted using custom IQ.  It can be a memory leak.
        LeafNode node = getNode(nodeId, topic);
        node.send(new PayloadItem<MMXPayloadMsgHandler.MMXPacketExtension>(itemId,
                new MMXPayloadMsgHandler.MMXPacketExtension(payload)));
        return itemId;
      } catch (XMPPErrorException e) {
        String condition = e.getXMPPError().getCondition();
        if (XMPPError.Condition.item_not_found.equals(condition)) {
          throw new TopicNotFoundException(topic);
        }
        if (XMPPError.Condition.forbidden.equals(condition)) {
          throw new TopicPermissionException(topic);
        }
        throw new MMXException(e.getMessage(), e);
      } catch (MMXException e) {
        throw e;
      } catch (Throwable e) {
        throw new MMXException(e.getMessage(), e);
      }
    } else if (queue != null) {
      //Not connected, and queue exists, queue...
      Item.PubSub item = new Item.PubSub(itemId, nodeId, topic, payload);
      queue.addItem(item);
      return itemId;
    } else {
      throw new MMXException("Cannot publish to topic because not connected.");
    }
  }

  /**
   * Get published items by their ID's.
   * @param topic A topic object.
   * @param itemIds A list of published item ID's.
   * @return A Map of item ID's and published items.
   * @throws TopicNotFoundException
   * @throws TopicPermissionException
   * @throws MMXException
   */
  public Map<String, MMXMessage> getItemsByIds(MMXTopic topic, List<String> itemIds)
        throws TopicNotFoundException, TopicPermissionException, MMXException {
    if (topic instanceof MMXPersonalTopic) {
      ((MMXPersonalTopic) topic).setUserId(mCon.getUserId());
    }
    ItemsByIdsRequest rqt = new ItemsByIdsRequest(topic.getId(),
        Utils.escapeNode(topic.getUserId()), topic.getName(), itemIds);
    PubSubIQHandler<ItemsByIdsRequest, FetchResponse> iqHandler =
        new PubSubIQHandler<ItemsByIdsRequest, FetchResponse>();
    try {
      iqHandler.sendGetIQ(mCon, Constants.PubSubCommand.getItems.toString(),
          rqt, FetchResponse.class, iqHandler);
      FetchResponse resp = iqHandler.getResult();
      Map<String, MMXMessage> msgs = new HashMap<String, MMXMessage>(resp.getItems().size());
      XmlPullParser parser = PacketParserUtils.newXmppParser();
      for (MMXPublishedItem item : resp.getItems()) {
        MMXPacketExtension mmxExt = MMXPayloadMsgHandler.parse(parser, item.getPayloadXml());
        MMXMessage msg = new MMXMessage(item.getItemId(), item.getPublisher(),
            null, mmxExt.getPayload());
        msgs.put(item.getItemId(), msg);
      }
      return msgs;
    } catch (MMXException e) {
      if (e.getCode() == StatusCode.NOT_FOUND) {
        throw new TopicNotFoundException(e.getMessage());
      } else if (e.getCode() == StatusCode.FORBIDDEN) {
        throw new TopicPermissionException(e.getMessage());
      }
      throw e;
    } catch (Throwable e) {
      throw new MMXException(e.getMessage(), e);
    }
  }

  /**
   * @hide
   * Retract a published item from a topic.  Each retracting item has its status
   * code: 200 for success, 403 for forbidden, 404 for not found.
   * @param topic A topic object.
   * @param itemIds A list of published item ID's to be retracted.
   * @return A map of item ID and its status code.
   * @throws TopicNotFoundException
   * @throws TopicPermissionException
   * @throws MMXException
   */
  public Map<String, Integer> retract(MMXTopic topic, List<String> itemIds)
            throws TopicNotFoundException, TopicPermissionException, MMXException {
    if (topic instanceof MMXPersonalTopic) {
      ((MMXPersonalTopic) topic).setUserId(mCon.getUserId());
    }
    RetractRequest rqt = new RetractRequest(topic.getId(),
        Utils.escapeNode(topic.getUserId()), topic.getName(), itemIds);
    PubSubIQHandler<RetractRequest, RetractResponse> iqHandler =
        new PubSubIQHandler<RetractRequest, RetractResponse>();
    try {
      iqHandler.sendSetIQ(mCon, Constants.PubSubCommand.retract.toString(),
          rqt, RetractResponse.class, iqHandler);
      RetractResponse results = iqHandler.getResult();
      return results;
    } catch (MMXException e) {
      if (e.getCode() == StatusCode.NOT_FOUND) {
        throw new TopicNotFoundException(e.getMessage());
      } else if (e.getCode() == StatusCode.FORBIDDEN) {
        throw new TopicPermissionException(e.getMessage());
      }
      throw e;
    } catch (Throwable e) {
      throw new MMXException(e.getMessage(), e);
    }
  }

  /**
   * @hide
   * Clear all published items from a global topic created by the current user
   * or from a user topic under the current user name-space.
   * @param topic A global topic or personal topic.
   * @throws TopicNotFoundException
   * @throws TopicPermissionException
   * @throws MMXException
   */
  public boolean clearAllItems(MMXTopic topic) throws
              TopicNotFoundException, TopicPermissionException, MMXException {
    if (topic instanceof MMXUserTopic &&
        !mCon.getUserId().equals(topic.getUserId())) {
      throw new TopicPermissionException(USER_TOPIC_NOT_ALLOWED);
    }
    RetractAllRequest rqt = new RetractAllRequest(topic.getId(),
        topic.getName(), topic.isUserTopic());
    PubSubIQHandler<RetractAllRequest, MMXStatus> iqHandler =
        new PubSubIQHandler<RetractAllRequest, MMXStatus>();
    try {
      iqHandler.sendSetIQ(mCon, Constants.PubSubCommand.retractall.toString(),
          rqt, MMXStatus.class, iqHandler);
      MMXStatus status = iqHandler.getResult();
      return status.getCode() == StatusCode.SUCCESS;
    } catch (MMXException e) {
      if (e.getCode() == StatusCode.NOT_FOUND) {
        throw new TopicNotFoundException(e.getMessage());
      } else if (e.getCode() == StatusCode.FORBIDDEN) {
        throw new TopicPermissionException(e.getMessage());
      }
      throw e;
    } catch (Throwable e) {
      throw new MMXException(e.getMessage(), e);
    }
  }

  /**
   * Get the items published within a date range from a global or user topic.
   * The caller must be an owner or subscriber to the topic; otherwise,
   * TopicPermissionException will be thrown.
   * @param topic A topic object.
   * @param options Optional fetch options, or null.
   * @return A result set of the published items, or empty list.
   * @throws TopicNotFoundException
   * @throws TopicPermissionException
   * @throws MMXException
   */
  public MMXResult<List<MMXMessage>> getItems(MMXTopic topic, FetchOptions options)
      throws TopicNotFoundException, TopicPermissionException, MMXException {
    if (topic instanceof MMXPersonalTopic) {
      ((MMXPersonalTopic) topic).setUserId(mCon.getUserId());
    }
    FetchRequest rqt = new FetchRequest(topic.getId(),
        Utils.escapeNode(topic.getUserId()), topic.getName(), options);
    PubSubIQHandler<FetchRequest, FetchResponse> iqHandler =
        new PubSubIQHandler<FetchRequest, FetchResponse>();
    try {
      iqHandler.sendGetIQ(mCon, Constants.PubSubCommand.fetch.toString(), rqt,
          FetchResponse.class, iqHandler);
      FetchResponse resp = iqHandler.getResult();
      List<MMXMessage> msgs = new ArrayList<MMXMessage>(resp.getItems().size());
//      Log.i(TAG, "fetch items: uid="+resp.getUserId()+", topic="+resp.getTopic());

      XmlPullParser parser = PacketParserUtils.newXmppParser();
      for (MMXPublishedItem item : resp.getItems()) {
//        Log.i(TAG, "item: itemId="+item.getItemId()+", publisher="+item.getPublisher()+
//            ", date="+item.getCreationDate()+", payload="+item.getPayloadXml());
        MMXPacketExtension mmxExt = MMXPayloadMsgHandler.parse(parser, item.getPayloadXml());
        MMXMessage msg = new MMXMessage(item.getItemId(), item.getPublisher(),
            null, mmxExt.getPayload());
        msgs.add(msg);
      }
      return new MMXResult<List<MMXMessage>>(msgs, resp.getTotal());
    } catch (MMXException e) {
      if (e.getCode() == StatusCode.NOT_FOUND) {
        throw new TopicNotFoundException(e.getMessage());
      } else if (e.getCode() == StatusCode.FORBIDDEN) {
        throw new TopicPermissionException(e.getMessage());
      }
      throw e;
    } catch (Throwable e) {
      throw new MMXException(e.getMessage(), e);
    }
  }

  /**
   * Subscribe to a global or user topic for current user or current device.  If
   * the subscription already exists, the original subscription ID will be
   * returned.  A user-based subscription (i.e. <code>thisDeviceOnly</code> is
   * false) means that a published item will be delivered to the first connected
   * device of the user.  A device-based subscription means a published item
   * will only be delivered to the specific device registered by the user.
   * @param topic The topic object.
   * @param thisDeviceOnly true for current device only; false for current user.
   * @return A subscription ID.
   * @throws TopicNotFoundException
   * @throws TopicPermissionException
   * @throws MMXException
   */
  public String subscribe(MMXTopic topic, boolean thisDeviceOnly)
      throws TopicNotFoundException, TopicPermissionException, MMXException {
    if (topic instanceof MMXPersonalTopic) {
      ((MMXPersonalTopic) topic).setUserId(mCon.getUserId());
    }
    String devId = thisDeviceOnly ? mCon.getContext().getDeviceId() : null;
    SubscribeRequest rqt = new SubscribeRequest(topic.getId(),
        Utils.escapeNode(topic.getUserId()), topic.getName(), devId);
    PubSubIQHandler<SubscribeRequest, SubscribeResponse> iqHandler =
        new PubSubIQHandler<SubscribeRequest, SubscribeResponse>();
    try {
      iqHandler.sendSetIQ(mCon, Constants.PubSubCommand.subscribe.toString(),
          rqt, SubscribeResponse.class, iqHandler);
      SubscribeResponse resp = iqHandler.getResult();
      return resp.getSubId();
    } catch (MMXException e) {
      if (e.getCode() == StatusCode.NOT_FOUND) {
        throw new TopicNotFoundException(e.getMessage());
      } else if (e.getCode() == StatusCode.CONFLICT) {
        throw new SubscriptionExistException(e.getMessage());
      } else if (e.getCode() == StatusCode.FORBIDDEN) {
        throw new TopicPermissionException(e.getMessage());
      } else {
        throw e;
      }
    } catch (Throwable e) {
      throw new MMXException(e.getMessage(), e);
    }
  }

  /**
   * Cancel a subscription or all subscriptions from a topic.  If the
   * <code>subscriptionId</code> is null, all subscriptions from the topic by
   * the current user will be cancelled.
   * @param topic A topic object.
   * @param subscriptionId A subscription ID for a device, or null for all devices.
   * @return false if topic is not subscribed; true otherwise.
   * @throws TopicNotFoundException
   * @throws MMXException
   */
  public boolean unsubscribe(MMXTopic topic, String subscriptionId)
          throws TopicNotFoundException, MMXException {
    if (topic instanceof MMXPersonalTopic) {
      ((MMXPersonalTopic) topic).setUserId(mCon.getUserId());
    }
    UnsubscribeRequest rqt = new UnsubscribeRequest(topic.getId(),
        Utils.escapeNode(topic.getUserId()), topic.getName(), subscriptionId);
    PubSubIQHandler<UnsubscribeRequest, MMXStatus> iqHandler =
        new PubSubIQHandler<UnsubscribeRequest, MMXStatus>();
    try {
      iqHandler.sendSetIQ(mCon, Constants.PubSubCommand.unsubscribe.toString(),
        rqt, MMXStatus.class, iqHandler);
      MMXStatus status = iqHandler.getResult();
      return (status.getCode() == StatusCode.SUCCESS);
    } catch (MMXException e) {
      if (e.getCode() == StatusCode.NOT_FOUND) {
        // Topic not found.
        throw new TopicNotFoundException(e.getMessage());
      }
      if (e.getCode() == StatusCode.GONE) {
        // No subscriptions.
        return false;
      }
      throw e;
    } catch (Throwable e) {
      throw new MMXException(e.getMessage(), e);
    }
  }

  /**
   * List subscriptions in all global topics for the current user.
   * @return A list of subscriptions, or an empty list.
   * @throws MMXException
   */
  public List<MMXSubscription> listAllSubscriptions() throws MMXException {
    return listSubscriptions(null, SHOW_USER_TOPIC_SUBSCRIPTIONS ?
        ListType.both : ListType.global);
  }

  /**
   * List subscriptions in a global topic or all global topics. If
   * <code>topic</code> is null, it is same as {@link #listAllSubscriptions()}
   * @param topic A topic object with the topic ID.
   * @return A list of subscriptions, or an empty list.
   * @throws MMXException
   */
  public List<MMXSubscription> listSubscriptions(MMXTopic topic)
                                          throws MMXException {
    String nodeId = (topic == null) ? null : getNodeId(topic);
    return listSubscriptions(nodeId, SHOW_USER_TOPIC_SUBSCRIPTIONS ?
        ListType.both : ListType.global);
  }

  private List<MMXSubscription> listSubscriptions(String nodeId, ListType type)
      throws MMXException {
    ArrayList<MMXSubscription> list = new ArrayList<MMXSubscription>();
    try {
      List<Subscription> subList = getPubSubManager().getSubscriptions();
      for (Subscription sub : subList) {
        MMXTopic mmxTopic = TopicHelper.toTopicId(sub.getNode(), null);
        if (mmxTopic == null) {
          continue;
        }
        if ((type != ListType.both) &&
            ((type == ListType.personal) ^ mmxTopic.isUserTopic())) {
          continue;
        }
        if (nodeId == null || nodeId.equalsIgnoreCase(sub.getNode())) {
          list.add(new MMXSubscription(mmxTopic, sub.getId(),
                                        XIDUtil.getResource(sub.getJid())));
        }
      }
      return list;
    } catch (XMPPErrorException e) {
      if (XMPPError.Condition.item_not_found.equals(e.getXMPPError().getCondition())) {
        return list;
      }
      throw new MMXException(e.getMessage(), e);
    } catch (Throwable e) {
      throw new MMXException(e.getMessage(), e);
    }
  }

  /**
   * Get all subscribers to a topic.
   * @param topic A topic object with topic ID.
   * @param offset offset of the result to be returned
   * @param limit -1 for unlimited, or > 0.
   * @return A result set.
   * @throws TopicNotFoundException
   * @throws TopicPermissionException
   * @throws MMXException
   */
  public MMXResult<List<UserInfo>> getSubscribers(MMXTopic topic, int offset, int limit)
      throws TopicNotFoundException, TopicPermissionException, MMXException {
    SubscribersRequest rqt = new SubscribersRequest(topic.getId(),
        Utils.escapeNode(topic.getUserId()), topic.getName(), offset, limit);
    PubSubIQHandler<SubscribersRequest, SubscribersResponse> iqHandler =
        new PubSubIQHandler<SubscribersRequest, SubscribersResponse>();
    try {
      iqHandler.sendSetIQ(mCon, Constants.PubSubCommand.getSubscribers.toString(),
        rqt, SubscribersResponse.class, iqHandler);
      SubscribersResponse resp = iqHandler.getResult();
      return new MMXResult<List<UserInfo>>(resp.getSubscribers(),
                                                 resp.getTotal());
    } catch (MMXException e) {
      if (e.getCode() == StatusCode.NOT_FOUND) {
        throw new TopicNotFoundException(e.getMessage());
      }
      if (e.getCode() == StatusCode.FORBIDDEN) {
        throw new TopicPermissionException(e.getMessage());
      }
      throw e;
    } catch (Throwable e) {
      throw new MMXException(e.getMessage(), e);
    }
  }

  /**
   * List all global topics in this application.
   * @return A list of topics or an empty list.
   * @throws MMXException
   */
  public List<MMXTopicInfo> listTopics() throws MMXException {
    return listTopics(null,
        SHOW_USER_TOPICS ? ListType.both : ListType.global,
        SHOW_USER_TOPICS ? true : false);
  }

  /**
   * @hide
   * List all (recursively) or one level of personal and/or global
   * topics at a starting topic.  All non-personal user topics are not
   * discoverable.  The <code>type</code> is applicable only if
   * <code>startingTopicId</code> is null.
   * @param startingTopicId null for root, or a starting topic ID.
   * @param type Filtering types: global-only, personal-only, or both.
   * @param recursive true to list recursively; otherwise, false.
   * @return A list of topics or an empty list.
   * @throws MMXException
   */
  public List<MMXTopicInfo> listTopics(String startingTopicId, ListType type,
                                    boolean recursive) throws MMXException {
    try {
      TopicAction.ListRequest rqt = new TopicAction.ListRequest()
                                        .setRecursive(recursive)
                                        .setStart(startingTopicId)
                                        .setType(type);
      PubSubIQHandler<TopicAction.ListRequest, TopicAction.ListResponse> iqHandler =
          new PubSubIQHandler<TopicAction.ListRequest, TopicAction.ListResponse>();
      iqHandler.sendGetIQ(mCon, Constants.PubSubCommand.listtopics.toString(),
          rqt, TopicAction.ListResponse.class, iqHandler);
      TopicAction.ListResponse resp = iqHandler.getResult();

      List<MMXTopicInfo> list = new ArrayList<MMXTopicInfo>(resp.size());
      for (TopicInfo info : resp) {
        list.add(toTopicInfo(info));
      }
      return list;
    } catch (MMXException e) {
      throw e;
    } catch (Throwable e) {
      throw new MMXException(e.getMessage(), e);
    }
  }

//  /**
//   * @hide
//   * Show all topics is for debugging only.  It must not be exposed to public.
//   * You need a system administrator account to see all topics.
//   * @return
//   * @throws MMXException
//   */
//  List<MMXTopicId> SAT() throws MMXException {
//    ArrayList<MMXTopicId> list = new ArrayList<MMXTopicId>();
//    try {
//      ServiceDiscoveryManager discoMgr = ServiceDiscoveryManager.getInstanceFor(
//          mCon.getXMPPConnection());
//      DiscoverItems items = discoMgr.discoverItems(mCon.getPubSubService());
//      List<DiscoverItems.Item> listOfItem = items.getItems();
//      for (DiscoverItems.Item item : listOfItem) {
//        String nodeId = item.getNode();
//        // TODO: the topic is the full path now; it cannot be used.
//        MMXTopicId topic = new MMXTopicId(null, nodeId);
//        list.add(topic);
//      }
//      return list;
//    } catch (XMPPErrorException e) {
//      if (XMPPError.Condition.item_not_found.equals(e.getXMPPError().getCondition())) {
//        return list;
//      }
//      throw new MMXException(e.getMessage(), e);
//    } catch (Throwable e) {
//      throw new MMXException(e.getMessage(), e);
//    }
//  }
//
//  /**
//   * @hide
//   * Clear all topics is for debugging only.  It must not be exposed to public.
//   * You need a system administrator account to clear all topics.
//   */
//  void CAT() {
//    try {
//      List<MMXTopicId> list = SAT();
//      for (MMXTopicId topic : list) {
//        Log.v(TAG, "deleting topic "+topic.getName());
//        try {
//          getPubSubManager().deleteNode(topic.getName());
//        } catch (Throwable e) {
//          Log.e(TAG, "Delete topic '"+topic.getName()+"' failed: ", e);
//        }
//      }
//    } catch (MMXException e) {
//      e.printStackTrace();
//    }
//  }

  private ConfigureForm optionsToFormSkipNull(MMXTopicOptions options) {
    ConfigureForm form = new ConfigureForm(FormType.submit);
    if (options.getPublisherType() != null) {
      form.setPublishModel(typeToModel(options.getPublisherType()));
    }
    if (options.getMaxItems() != null) {
      form.setMaxItems(options.getMaxItems());
      form.setPersistentItems(options.getMaxItems() != 0);
    }
    if (options.getDisplayName() != null) {
      form.setTitle(options.getDisplayName());
    }
    if (options.getDescription() != null) {
      FormField field = new FormField(FIELD_DESCRIPTION);
      field.addValue(options.getDescription());
      field.setType(FormField.TYPE_TEXT_SINGLE);
      form.addField(field);
    }
    return form;
  }

  private static TopicAction.PublisherType modelToType(PublishModel model) {
    switch(model) {
    case open:        return TopicAction.PublisherType.anyone;
    case publishers : return TopicAction.PublisherType.owner;
    case subscribers: return TopicAction.PublisherType.subscribers;
    default:          return TopicAction.PublisherType.anyone;
    }
  }

  private static PublishModel typeToModel(TopicAction.PublisherType right) {
    switch(right) {
    case anyone:      return PublishModel.open;
    case owner:  return PublishModel.publishers;
    case subscribers: return PublishModel.subscribers;
    default:          return PublishModel.open;
    }
  }

  private MMXTopicOptions formToOptions(ConfigureForm form) {
    MMXTopicOptions options = new MMXTopicOptions()
      .setMaxItems(form.getMaxItems())
      .setPublisherType(modelToType(form.getPublishModel()))
      .setSubscriptionEnabled(form.isSubscribe());
    // Get the description.
    FormField field = form.getField(FIELD_DESCRIPTION);
    if (field != null) {
      List<String> values = field.getValues();
      options.setDescription(values.isEmpty() ? null : values.get(0));
    }
    return options;
  }

  /**
   * Update the configurable options for a global topic or personal topic which
   * must be owned by the current user.  If a field in <code>options</code>
   * is null, its value will remain unchanged.
   * @param topic The topic object with topic ID.
   * @param options The topic options to be updated.
   * @return MMXStatus
   * @throws TopicNotFoundException
   * @throws MMXException
   * @see #getOptions(String, boolean)
   */
  public MMXStatus updateOptions(MMXTopic topic, MMXTopicOptions options)
                    throws TopicNotFoundException, MMXException {
    if (topic instanceof MMXPersonalTopic && topic.getUserId() == null) {
      ((MMXPersonalTopic) topic).setUserId(mCon.getUserId());
    }
    String nodeId = getNodeId(topic);
    if (topic instanceof MMXUserTopic &&
        !mCon.getUserId().equals(TopicHelper.getUserId(nodeId))) {
      throw new TopicPermissionException(USER_TOPIC_NOT_ALLOWED);
    }
    try {
      LeafNode leaf = getNode(nodeId, topic.getDisplayName());
      ConfigureForm form = optionsToFormSkipNull(options);
      leaf.sendConfigurationForm(form);
      return new MMXStatus().setCode(StatusCode.SUCCESS);
    } catch (MMXException e) {
      throw e;
    } catch (Throwable e) {
      throw new MMXException(e.getMessage(), e);
    }
  }

  /**
   * Get the configurable options of a topic.  The returned options can be
   * updated by calling {@link #updateOptions(MMXTopic, MMXTopicOptions)}.
   * @param topic The topic object with topic ID.
   * @return The options of the topic.
   * @throws TopicNotFoundException
   * @throws MMXException
   */
  public MMXTopicOptions getOptions(MMXTopic topic)
      throws TopicNotFoundException, MMXException {
    if (topic instanceof MMXPersonalTopic && topic.getUserId() == null) {
      ((MMXPersonalTopic) topic).setUserId(mCon.getUserId());
    }
    String nodeId = getNodeId(topic);
    if (topic instanceof MMXUserTopic &&
        !mCon.getUserId().equals(TopicHelper.getUserId(nodeId))) {
      throw new TopicPermissionException(USER_TOPIC_NOT_ALLOWED);
    }
    try {
      LeafNode leaf = getNode(nodeId, topic.getDisplayName());
      ConfigureForm form = leaf.getNodeConfiguration();
      return formToOptions(form);
    } catch (MMXException e) {
      throw e;
    } catch (Throwable e) {
      throw new MMXException(e.getMessage(), e);
    }
  }

  /**
   * Create a topic with options.  The topic may be globally visible or hidden
   * under the user name space.  Without specifying the options, the topic is
   * can be searched by anyone, accessible by subscribers, one persisted item,
   * can be published by anyone.
   * @param topic A MMXGlobalTopic or MMXPersonalTopic.
   * @param options Topic creation options, or null.
   * @return MMXTopic object for topic with a unique ID.
   * @throws MMXException
   * @throws TopicExistsException
   * @see com.magnet.mmx.client.common.MMXGlobalTopic
   * @see com.magnet.mmx.client.manget.mmx.protocol.MMXPersonalTopic
   */
  public MMXTopic createTopic(MMXTopic topic, MMXTopicOptions options)
                            throws TopicExistsException, MMXException {
    if (topic instanceof MMXUserTopic && !mCon.getUserId().equals(topic.getUserId())) {
      throw new TopicPermissionException(USER_TOPIC_NOT_ALLOWED);
    }
    if (topic.getId() != null) {
      throw new MMXException("Cannot create topic with ID");
    }
    TopicHelper.checkPathAllowed(topic.getName());
    String topicName = TopicHelper.normalizePath(topic.getName());
    CreateRequest rqt = new CreateRequest(topicName, topic.isUserTopic(), options);
    PubSubIQHandler<CreateRequest, CreateResponse> iqHandler =
        new PubSubIQHandler<CreateRequest, CreateResponse>();
    try {
      iqHandler.sendSetIQ(mCon, Constants.PubSubCommand.createtopic.toString(),
        rqt, CreateResponse.class, iqHandler);
      CreateResponse resp = iqHandler.getResult();
      if (resp.getCode() == StatusCode.SUCCESS) {
        return resp.getId();
      }
      throw new MMXException(resp.getMessage(), resp.getCode());
    } catch (MMXException e) {
      if (e.getCode() == MMXStatus.CONFLICT) {
        throw new TopicExistsException(e.getMessage());
      }
      throw e;
    }
  }

//  // For debug purpose.
//  private void showConfigureForm(ConfigureForm form) {
//    for (FormField field : form.getFields()) {
//      System.out.print("var="+field.getVariable());
//      System.out.print('['+field.getType()+']');
//      boolean first = true;
//      for (Option option : field.getOptions()) {
//        if (first) { System.out.print("; opt: "); first = false; }
//        System.out.print(option.getValue()+",");
//      }
//      first = true;
//      for (String value : field.getValues()) {
//        if (first) { System.out.print("; val: "); first = false; }
//        System.out.print(value+",");
//      }
//      System.out.println("");
//    }
//  }

  /**
   * Delete a topic which was created by current user.
   * @param topic A topic.
   * @throws TopicNotFoundException
   * @throws TopicPermissionException
   * @throws MMXException
   */
  public MMXStatus deleteTopic(MMXTopic topic) throws MMXException,
                            TopicNotFoundException, TopicPermissionException {
    if (topic instanceof MMXUserTopic && !mCon.getUserId().equals(topic.getUserId())) {
      throw new TopicPermissionException(USER_TOPIC_NOT_ALLOWED);
    }
    DeleteRequest rqt = new DeleteRequest(topic.getId(), topic.getName(),
                                          topic.isUserTopic());
    PubSubIQHandler<DeleteRequest, MMXStatus> iqHandler =
        new PubSubIQHandler<DeleteRequest, MMXStatus>();
    try {
      iqHandler.sendSetIQ(mCon, Constants.PubSubCommand.deletetopic.toString(), rqt,
        MMXStatus.class, iqHandler);
      MMXStatus status = iqHandler.getResult();
      return status;
    } catch (MMXException e) {
      if (e.getCode() == StatusCode.NOT_FOUND) {
        throw new TopicNotFoundException(e.getMessage());
      }
      if (e.getCode() == StatusCode.FORBIDDEN) {
        throw new TopicPermissionException(e.getMessage());
      }
      throw e;
    }
  }

  /**
   * Request to send last published items for all topics with max items per
   * topic since a given time.  The published items will be delivered through
   * {@link MMXMessageListener#onItemReceived(MMXMessage, MMXTopic).
   * @param maxItems Maximum of items to be returned.
   * @param since The last published date/time.
   * @return The request result.
   * @throws MMXException
   */
  public MMXStatus requestLastPublishedItems(int maxItems, Date since)
                                    throws MMXException {
    return requestLastPublishedItems(null, maxItems, since);
  }

  /**
   * Request to send last published items for a topic with max items since a
   * given time.  The published items will be delivered through
   * {@link MMXMessageListener#onItemReceived(MMXMessage, MMXTopic).
   * @param topic The topic.
   * @param maxItems Maximum of items to be returned.
   * @param since The last published date/time.
   * @return The request result.
   * @throws MMXException
   */
  public MMXStatus requestLastPublishedItems(MMXTopic topic, int maxItems,
                                              Date since) throws MMXException {
    try {
      SendLastPublishedItems rqt = new SendLastPublishedItems((topic == null) ?
          null : new MMXTopicId(topic), maxItems, since);
      PubSubIQHandler<SendLastPublishedItems, MMXStatus> iqHandler =
          new PubSubIQHandler<SendLastPublishedItems, MMXStatus>();
      iqHandler.sendGetIQ(mCon, Constants.PubSubCommand.getlatest.toString(),
          rqt, MMXStatus.class, iqHandler);
      return iqHandler.getResult();
    } catch (MMXException e) {
      throw e;
    } catch (Throwable e) {
      throw new MMXException(e.getMessage(), e);
    }
  }

  /**
   * Search for global topics by topic attributes and/or tags.
   * @param operator The AND or OR operator.
   * @param search Single or multi-values search attributes.
   * @param offset the offset of rows to be returned.
   * @param limit The max number of rows to be returned, or null for system imposed max rows.
   * @return The search result.
   * @throws MMXException
   * @deprecated {@link #searchBy(com.magnet.mmx.protocol.SearchAction.Operator, com.magnet.mmx.protocol.TopicAction.TopicSearch, Integer, Integer, ListType)}
   */
  @Deprecated
  public MMXTopicSearchResult searchBy(SearchAction.Operator operator,
      TopicAction.TopicSearch search, Integer offset, Integer limit) throws MMXException {
    return searchBy(operator, search, offset, limit, ListType.global);
  }

  /**
   * Search for global or personal topics by topic attributes and/or tags.
   * @param operator The AND or OR operator.
   * @param search Single or multi-values search attributes.
   * @param offset the offset of rows to be returned.
   * @param limit The max number of rows to be returned, or null for system imposed max rows.
   * @param listType scope of the search: global or personal topics.
   * @return The search result.
   * @throws MMXException
   */
  public MMXTopicSearchResult searchBy(SearchAction.Operator operator,
      TopicAction.TopicSearch search, Integer offset, Integer limit, ListType listType)
          throws MMXException {
    try {
      TopicSearchRequest rqt = new TopicSearchRequest(operator, search,
          (null == offset) ? 0 : offset, (limit == null) ? -1 : limit.intValue(),
          listType);
      PubSubIQHandler<TopicSearchRequest, TopicQueryResponse> iqHandler =
          new PubSubIQHandler<TopicSearchRequest, TopicQueryResponse>();
      iqHandler.sendGetIQ(mCon, Constants.PubSubCommand.searchTopic.toString(),
          rqt, TopicQueryResponse.class, iqHandler);
      TopicQueryResponse resp = iqHandler.getResult();
      List<MMXTopicInfo> list = new ArrayList<MMXTopicInfo>(resp.getResults().size());
      for (TopicInfo info : resp.getResults()) {
        list.add(toTopicInfo(info));
      }
      return new MMXTopicSearchResult(resp.getTotal(), list);
    } catch (MMXException e) {
      throw e;
    } catch (Throwable e) {
      throw new MMXException(e.getMessage(), e);
    }
  }

  /**
   * Get the topic information by the topic name.
   * @param topic A topic object.
   * @return The topic information.
   * @throws TopicNotFoundException
   * @throws TopicPermissionException
   * @throws MMXException
   */
  public MMXTopicInfo getTopic(MMXTopic topic)
      throws TopicNotFoundException, TopicPermissionException, MMXException {
    if (topic instanceof MMXPersonalTopic) {
      ((MMXPersonalTopic) topic).setUserId(mCon.getUserId());
    }
    topic = new MMXTopicId(topic.getId(), null, topic.getUserId(), topic.getName());
    PubSubIQHandler<MMXTopic, TopicInfo> iqHandler =
        new PubSubIQHandler<MMXTopic, TopicInfo>();
    try {
      iqHandler.sendGetIQ(mCon, Constants.PubSubCommand.getTopic.toString(),
          topic, TopicInfo.class, iqHandler);
      TopicInfo info = iqHandler.getResult();
      return toTopicInfo(info);
    } catch (MMXException e) {
      if (e.getCode() == StatusCode.NOT_FOUND) {
        throw new TopicNotFoundException(e.getMessage());
      }
      if (e.getCode() == StatusCode.FORBIDDEN) {
        throw new TopicPermissionException(e.getMessage());
      }
      throw e;
    } catch (Throwable e) {
      throw new MMXException(e.getMessage(), e);
    }
  }

  /**
   * Get topic information by topic names.  If a topic in the request list
   * cannot be retrieved, a null will be set in the returning list.
   * @param topics A list of topic names.
   * @return A list of topic info.
   * @throws MMXException
   */
  public List<MMXTopicInfo> getTopics(List<MMXTopic> topics) throws MMXException {
    List<MMXTopicId> rqt = new ArrayList<MMXTopicId>(topics.size());
    for (MMXTopic topic : topics) {
      if (topic instanceof MMXPersonalTopic) {
        ((MMXPersonalTopic) topic).setUserId(mCon.getUserId());
      }
      rqt.add(new MMXTopicId(topic.getId(), null, topic.getUserId(), topic.getName()));
    }

    List<MMXTopicInfo> res = new ArrayList<MMXTopicInfo>(topics.size());
    PubSubIQHandler<List<MMXTopicId>, GetTopicsResponse> iqHandler =
        new PubSubIQHandler<List<MMXTopicId>, GetTopicsResponse>();
    try {
      iqHandler.sendGetIQ(mCon, Constants.PubSubCommand.getTopics.toString(),
          rqt, GetTopicsResponse.class, iqHandler);
      GetTopicsResponse resp = iqHandler.getResult();
      for (TopicInfo info : resp) {
        res.add((info == null) ? null : toTopicInfo(info));
      }
      return res;
    } catch (MMXException e) {
      throw e;
    } catch (Throwable e) {
      throw new MMXException(e.getMessage(), e);
    }
  }

  boolean saveLastDelivery(Date lastDeliveryTime) {
    if (mBuffer == null) {
      // Delay initializing mBuffer until mCon is connected.
      mBuffer = mapDeliveryLogFile();
    }
    mBuffer.position(0);
    mBuffer.putLong(lastDeliveryTime.getTime());
    mBuffer.force();
    //Log.d(TAG, "save last del time="+lastDeliveryTime.getTime(), null);
    return true;
  }

  /**
   * Get the last published item delivery time.
   */
  public Date getLastDelivery() {
    if (mBuffer == null) {
      // Delay initializing mBuffer until mCon is connected.
      mBuffer = mapDeliveryLogFile();
    }
    mBuffer.position(0);
    long lastDeliveryTime = mBuffer.getLong();
    //Log.d(TAG, "load last del time="+lastDeliveryTime, null);
    return new Date(lastDeliveryTime);
  }

  private MappedByteBuffer mapDeliveryLogFile() {
    RandomAccessFile file = null;
    try {
      String path = mCon.getContext().getFilePath(LAST_DELIVERY_FILE+
                                              mCon.getConnectionToken()+".bin");
      file = new RandomAccessFile(path, "rw");
      if (file.length() != 8) {
        // Initialize with 8 bytes (long)
        file.writeLong(0);
      }
      // Map file into memory so it can be GC when this pubsub manager is done.
      return file.getChannel().map(FileChannel.MapMode.READ_WRITE, 0, 8);
    } catch (IOException e) {
      Log.e(TAG, "Unable to persist pubsub delivery log", e);
      return null;
    } finally {
      if (file != null) {
        try {
          file.close();
        } catch (IOException e) {
          // Ignored
        }
      }
    }
  }

  /**
   * Get the summary information of a list of topics with an optional
   * published date range.
   * @param topics A list of topics to be inquired.
   * @param since A published since date, or null
   * @param until A published until date, or null
   * @return A list of summaries.
   * @throws MMXException
   */
  public List<TopicSummary> getTopicSummary(List<MMXTopic> topics, Date since,
                                Date until) throws MMXException {
    for (MMXTopic topic : topics) {
      if (topic instanceof MMXPersonalTopic) {
        ((MMXPersonalTopic) topic).setUserId(mCon.getUserId());
      }
    }
    List<MMXTopicId> list = new ArrayList<MMXTopicId>(topics.size());
    for (MMXTopic topic : topics) {
      list.add((MMXTopicId) topic);
    }
    SummaryRequest rqt = new SummaryRequest(list).setSince(since).setUntil(until);
    PubSubIQHandler<SummaryRequest, SummaryResponse> iqHandler =
        new PubSubIQHandler<SummaryRequest, SummaryResponse>();
    iqHandler.sendGetIQ(mCon, Constants.PubSubCommand.getSummary.toString(),
        rqt, SummaryResponse.class, iqHandler);
    SummaryResponse resp = iqHandler.getResult();
    return resp;
  }

  private static class TopicInfoList extends ArrayList<TopicInfo> {
  }

  /**
   * Search topics by all matching tags or any matching tags.
   * @param tags A list of tags.
   * @param matchAll true for all matching tags, false for any matching tags.
   * @return A list of topic information.
   * @throws MMXException
   */
  public List<MMXTopicInfo> searchByTags(List<String> tags, boolean matchAll )
                                throws MMXException {
    TagSearch rqt = new TagSearch(matchAll ? Operator.AND : Operator.OR, tags);
    PubSubIQHandler<TagSearch, TopicInfoList> iqHandler =
        new PubSubIQHandler<TagSearch, TopicInfoList>();
    iqHandler.sendGetIQ(mCon, Constants.PubSubCommand.searchByTags.toString(),
            rqt, TopicInfoList.class, iqHandler);
    List<TopicInfo> resp = iqHandler.getResult();
    List<MMXTopicInfo> list = new ArrayList<MMXTopicInfo>(resp.size());
    for (TopicInfo info : resp) {
      list.add(toTopicInfo(info));
    }
    return list;
  }

  /**
   * Get the tags from a user or global topic.
   * @param topic A topic object.
   * @return A tag info object.
   * @throws TopicNotFoundException
   * @throws MMXException
   */
  public TopicTags getAllTags(MMXTopic topic)
      throws TopicNotFoundException, MMXException {
    if (topic instanceof MMXPersonalTopic) {
      ((MMXPersonalTopic) topic).setUserId(mCon.getUserId());
    }
    PubSubIQHandler<MMXTopic, TopicTags> iqHandler =
        new PubSubIQHandler<MMXTopic, TopicTags>();
    iqHandler.sendSetIQ(mCon, Constants.PubSubCommand.getTags.toString(), topic,
        TopicTags.class, iqHandler);
    TopicTags topicTags = iqHandler.getResult();
    return topicTags;
  }

  /**
   * Set tags to a user or global topic
   * @param topic A topic object.
   * @param tags A list of tags or an empty list.
   * @return The status.
   * @throws TopicNotFoundException
   * @throws MMXException
   */
  public MMXStatus setAllTags(MMXTopic topic, List<String> tags)
                            throws TopicNotFoundException, MMXException {
    return doTags(PubSubCommand.setTags, topic, tags);
  }

  /**
   * Add tags to a user or global topic.
   * @param topic A topic object.
   * @param tags A list of tags to be added.
   * @return The status.
   * @throws TopicNotFoundException
   * @throws MMXException
   */
  public MMXStatus addTags(MMXTopic topic, List<String> tags)
      throws TopicNotFoundException, MMXException {
    return doTags(PubSubCommand.addTags, topic, tags);
  }

  /**
   * Remove tags from a user or global topic.
   * @param topic A topic object.
   * @param tags A list of tags to be renoved.
   * @return The status.
   * @throws TopicNotFoundException
   * @throws MMXException
   */
  public MMXStatus removeTags(MMXTopic topic, List<String> tags)
      throws TopicNotFoundException, MMXException {
    return doTags(PubSubCommand.removeTags, topic, tags);
  }

  private MMXStatus doTags(PubSubCommand cmd, MMXTopic topic,
      List<String> tags) throws TopicNotFoundException, MMXException {
    if (cmd == PubSubCommand.setTags) {
      if (tags == null) {
        tags = new ArrayList<String>(0);
      }
      if (!tags.isEmpty()) {
        validateTags(tags);
      }
    } else {
      validateTags(tags);
    }
    if (topic instanceof MMXPersonalTopic) {
      ((MMXPersonalTopic) topic).setUserId(mCon.getUserId());
    }
    TopicTags rqt = new TopicTags(topic.getId(),
        Utils.escapeNode(topic.getUserId()), topic.getName(), tags);
    PubSubIQHandler<TopicTags, MMXStatus> iqHandler =
        new PubSubIQHandler<TopicTags, MMXStatus>();
    iqHandler.sendSetIQ(mCon, cmd.toString(), rqt, MMXStatus.class, iqHandler);
    return iqHandler.getResult();
  }

  // Convert a nodeID into various topic object.
  MMXTopic nodeToTopic(String nodeId) {
    if (nodeId.charAt(0) != TopicHelper.TOPIC_DELIM) {
      return null;
    }
    int index1 = nodeId.indexOf(TopicHelper.TOPIC_DELIM, 1);
    if (index1 < 0) {
      return null;
    }
    int index2 = nodeId.indexOf(TopicHelper.TOPIC_DELIM, index1+1);
    if (index2 < 0) {
      return null;
    }
    String userId = nodeId.substring(index1+1, index2);
    String topicName = nodeId.substring(index2+1);
    return toTopic(userId, topicName);
  }

  // userId can be null (from custom IQ) or "*" (from nodeID)
  MMXTopic toTopic(String userId, String topicName) {
    if (userId == null || userId.charAt(0) == TopicHelper.TOPIC_FOR_APP) {
      return new MMXGlobalTopic(topicName);
    } else if (mCon.getUserId().equals(userId)) {
      return new MMXPersonalTopic(topicName).setUserId(userId);
    } else {
      return new MMXUserTopic(userId, topicName);
    }
  }

  MMXTopicInfo toTopicInfo(TopicInfo info) {
    MMXTopic topic = toTopic(info.getUserId(), info.getName());
    return new MMXTopicInfo(topic, info);
  }

  private void validateTags(List<String> tags) throws MMXException {
    if (tags == null || tags.isEmpty()) {
      throw new MMXException("List of tags cannot be null or empty", StatusCode.BAD_REQUEST);
    }
    try {
      TagUtil.validateTags(tags);
    } catch (IllegalArgumentException e) {
      throw new MMXException(e.getMessage(), StatusCode.BAD_REQUEST);
    }
  }

  /**
   * Get the privacy list for a topic.
   * @param topic The topic object with topic ID.
   * @return
   * @throws MMXException
   */
  public List<MMXid> getPrivacyList(MMXTopic topic) throws MMXException {
    if (topic instanceof MMXPersonalTopic && topic.getUserId() == null) {
      ((MMXPersonalTopic) topic).setUserId(mCon.getUserId());
    }
    String nodeId = getNodeId(topic);
    return PrivacyManager.getInstance(mCon).getPrivacyList(nodeId);
  }

  /**
   * Set the privacy list for a topic.  If <code>xids</code> is empty, it will
   * clear the privacy list.
   * @param topic
   * @param xids
   * @throws MMXException
   */
  public void setPrivacyList(MMXTopic topic, List<MMXid> xids) throws MMXException {
    if (topic instanceof MMXPersonalTopic && topic.getUserId() == null) {
      ((MMXPersonalTopic) topic).setUserId(mCon.getUserId());
    }
    String nodeId = getNodeId(topic);
    if (xids == null) {
      PrivacyManager.getInstance(mCon).deletePrivacyList(nodeId);
    } else {
      PrivacyManager.getInstance(mCon).setPrivacyList(nodeId, xids);
    }
  }

  // Affiliation Management packet
  private static class AffiliationPacket implements PacketExtension {
    private final String mJid;
    private final Affiliation.Type mType;

    public AffiliationPacket(String jid, Affiliation.Type type) {
      mJid = jid;
      mType = type;
    }

    public String getJid() {
      return mJid;
    }

    public Affiliation.Type getAffiliation() {
      return mType;
    }

    @Override
    public String getElementName() {
      return "affiliation";
    }

    @Override
    public String getNamespace() {
      return null;
    }

    @Override
    public CharSequence toXML() {
      return '<' + getElementName() + " jid='" + mJid +
                "' affiliation='" + mType.toString() + "' />";
    }
  }

  // Parser for affiliations management packet.
  private static class AffiliationsProvider implements PacketExtensionProvider {
    @Override
    public PacketExtension parseExtension(XmlPullParser parser) throws Exception {
      String nodeId = null;
      List<AffiliationPacket> list = new ArrayList<AffiliationPacket>();
      boolean done = false;
      do {
        nodeId = parser.getAttributeValue(null, "node");
        int eventType = parser.next();
        if (eventType == XmlPullParser.START_TAG) {
          if ("affiliation".equals(parser.getName())) {
            String jid = parser.getAttributeValue(null, "jid");
            String affiliation = parser.getAttributeValue(null, "affiliation");
            try {
              list.add(new AffiliationPacket(jid, Affiliation.Type.valueOf(affiliation)));
            } catch (Throwable e) {
              // Ignore invalid affiliation type.
              e.printStackTrace();
            }
          }
        } else if (eventType == XmlPullParser.END_TAG) {
          if (PubSubElementType.AFFILIATIONS.getElementName().equals(parser.getName())) {
            done = true;
          }
        }
      } while (!done);

      return new AffiliationsPacket(nodeId, list);
    }
  }

  // Affiliations Management packet
  private static class AffiliationsPacket extends NodeExtension {
    private final List<AffiliationPacket> mList;

    static {
      // Register the parser for affiliations extension
      ProviderManager.addExtensionProvider(
          PubSubElementType.AFFILIATIONS.getElementName(),
          PubSubNamespace.OWNER.getXmlns(), new AffiliationsProvider());
    }

    public AffiliationsPacket(String nodeId) {
      super(PubSubElementType.AFFILIATIONS, nodeId);
      mList = null;
    }

    public AffiliationsPacket(String nodeId, List<AffiliationPacket> list) {
      super(PubSubElementType.AFFILIATIONS, nodeId);
      mList = list;
    }

    public List<AffiliationPacket> getAffiliations() {
      return mList;
    }

    @Override
    public CharSequence toXML() {
      if (mList == null) {
        return super.toXML();
      } else {
        StringBuilder sb = new StringBuilder(256);
        sb.append('<')
          .append(getElementName())
          .append(" node='")
          .append(getNode())
          .append("'>");
        for (AffiliationPacket affiliation : mList) {
          sb.append(affiliation.toXML());
        }
        sb.append("</")
          .append(getElementName())
          .append('>');
        return sb.toString();
      }
    }
  }

  /**
   * Get the subscriber white-list from a topic.
   * @param topic A topic owned by the current user.
   * @return
   * @throws TopicNotFoundException
   * @throws TopicPermissionException
   * @throws MMXException
   */
  public List<MMXid> getWhitelist(MMXPersonalTopic topic)
        throws TopicNotFoundException, TopicPermissionException, MMXException {
    if (topic.getUserId() == null) {
      topic.setUserId(mCon.getUserId());
    }
    String nodeId = getNodeId(topic);
    try {
      PubSub iq = PubSub.createPubsubPacket("pubsub."+mCon.getDomain(), Type.GET,
          new AffiliationsPacket(nodeId), PubSubNamespace.OWNER);
      PubSub packet = (PubSub) mCon.getXMPPConnection().createPacketCollectorAndSend(iq).
          nextResultOrThrow();
      AffiliationsPacket affsExt = (AffiliationsPacket) packet.getExtension(
          PubSubElementType.AFFILIATIONS);
      List<MMXid> whitelist = new ArrayList<MMXid>();
      for (AffiliationPacket affExt : affsExt.getAffiliations()) {
        if (affExt.getAffiliation() == Affiliation.Type.member) {
          whitelist.add(new MMXid(XIDUtil.extractUserId(affExt.getJid()), null, null));
        }
      }
      return whitelist;
    } catch (XMPPErrorException e) {
      if (Condition.item_not_found.equals(e.getXMPPError().getCondition())) {
        throw new TopicNotFoundException("Personal "+topic.getName()+" not found");
      }
      if (Condition.forbidden.equals(e.getXMPPError().getCondition())) {
        throw new TopicPermissionException("Not the owner");
      }
      throw new MMXException("Unable to get subscriber whitelist", e);
    } catch (Throwable e) {
      throw new MMXException("Unable to get subscriber whitelist", e);
    }
  }

  /**
   * Set users to the subscriber white-list to a topic of the current user.
   * @param topic A topic owned by the current user.
   * @param xids A list of users (user ID's) allowed to subscribe.
   * @throws TopicNotFoundException
   * @throws TopicPermissionException
   * @throws MMXException
   */
  public void setWhitelist(MMXPersonalTopic topic, List<MMXid> xids)
        throws TopicNotFoundException, TopicPermissionException, MMXException {
    setAffiliations(topic, xids, Affiliation.Type.member);
  }

  /**
   * Revoke users from the white-list to a topic of the current user.
   * @param topic A topic owned by the current user.
   * @param xids A list of users (user ID's) are outcast
   * @throws TopicNotFoundException
   * @throws TopicPermissionException
   * @throws MMXException
   */
  public void revokeWhitelist(MMXPersonalTopic topic, List<MMXid> xids)
      throws TopicNotFoundException, TopicPermissionException, MMXException {
    setAffiliations(topic, xids, Affiliation.Type.outcast);
  }

  private void setAffiliations(MMXPersonalTopic topic, List<MMXid> xids,
                                Affiliation.Type type)
      throws TopicNotFoundException, TopicPermissionException, MMXException {
    if (topic.getUserId() == null) {
      topic.setUserId(mCon.getUserId());
    }
    String nodeId = getNodeId(topic);
    try {
      List<AffiliationPacket> affs = new ArrayList<AffiliationPacket>(xids.size());
      for (MMXid xid : xids) {
        affs.add(new AffiliationPacket(XIDUtil.makeXID(
            xid, mCon.getAppId(), mCon.getDomain()), type));
      }
      PubSub iq = PubSub.createPubsubPacket("pubsub."+mCon.getDomain(), Type.SET,
          new AffiliationsPacket(nodeId, affs), PubSubNamespace.OWNER);
      Packet packet = mCon.getXMPPConnection().createPacketCollectorAndSend(iq).
          nextResultOrThrow();
      Log.i(TAG, packet.toString());
    } catch (XMPPErrorException e) {
      if (Condition.item_not_found.equals(e.getXMPPError().getCondition())) {
        throw new TopicNotFoundException("Personal "+topic.getName()+" not found");
      }
      if (Condition.forbidden.equals(e.getXMPPError().getCondition())) {
        throw new TopicPermissionException("Not the owner");
      }
      throw new MMXException("Unable to set subscriber whitelist", e);
    } catch (Throwable e) {
      throw new MMXException("Unable to set subscriber whitelist", e);
    }
  }

  // Get the node ID from topic object.  If the topic ID is available, the
  // node ID will be derived from it.  Otherwise, look up the node ID by the
  // fully qualified name from the server (it is slower)
  private String getNodeId(MMXTopic topic) throws MMXException {
    if (topic.getId() != null) {
      return TopicHelper.toNodeId(mAppId, topic.getId());
    } else {
      MMXTopicInfo topicInfo = getTopic(topic);
      return TopicHelper.toNodeId(mAppId, topicInfo.getTopic().getId());
    }
  }
}
