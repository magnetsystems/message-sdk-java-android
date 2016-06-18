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

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.filter.AndFilter;
import org.jivesoftware.smack.filter.MessageTypeFilter;
import org.jivesoftware.smack.filter.NotFilter;
import org.jivesoftware.smack.filter.OrFilter;
import org.jivesoftware.smack.filter.PacketExtensionFilter;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Message.Type;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smackx.pubsub.EventElement;
import org.jivesoftware.smackx.pubsub.Item;
import org.jivesoftware.smackx.pubsub.ItemsExtension;
import org.jivesoftware.smackx.pubsub.NodeExtension;
import org.jivesoftware.smackx.pubsub.PayloadItem;
import org.jivesoftware.smackx.pubsub.PubSubElementType;
import org.jivesoftware.smackx.pubsub.packet.PubSubNamespace;
import org.jivesoftware.smackx.receipts.DeliveryReceipt;
import org.jivesoftware.smackx.receipts.DeliveryReceiptRequest;

import com.magnet.mmx.client.common.MMXPayloadMsgHandler.MMXPacketExtension;
import com.magnet.mmx.protocol.Constants;
import com.magnet.mmx.protocol.Constants.MessageCommand;
import com.magnet.mmx.protocol.MMXStatus;
import com.magnet.mmx.protocol.MMXTopic;
import com.magnet.mmx.protocol.MMXTopicId;
import com.magnet.mmx.protocol.MMXid;
import com.magnet.mmx.protocol.MmxHeaders;
import com.magnet.mmx.protocol.MsgAck;
import com.magnet.mmx.protocol.MsgEvents;
import com.magnet.mmx.protocol.MsgId;
import com.magnet.mmx.protocol.MsgTags;
import com.magnet.mmx.protocol.MsgsState;
import com.magnet.mmx.protocol.MsgsState.MessageStatus;
import com.magnet.mmx.protocol.StatusCode;
import com.magnet.mmx.util.Converter;
import com.magnet.mmx.util.QueueExecutor;
import com.magnet.mmx.util.TagUtil;
import com.magnet.mmx.util.TopicHelper;
import com.magnet.mmx.util.XIDUtil;

/**
 * The singleton MessageManager allows user to send application data and send
 * delivery receipt as an acknowledgment, and the message status.  It
 */
public class MessageManager implements Closeable {
  private static final String TAG = "MessageManager";
  private int mAckErrors;
  private int mAckCounters;
  private final MMXConnection mCon;
  private QueueExecutor mAckExecutor;
  private final static Creator sCreator = new Creator() {
    @Override
    public Object newInstance(MMXConnection con) {
      return new MessageManager(con);
    }
  };

  private final PacketListener mEventPacketListener = new PacketListener() {
    @Override
    public void processPacket(Packet packet) throws NotConnectedException {
//      mCon.getContext().log(TAG, "Event processPacket() pkt="+packet.toXML());
      final MMXMessageListener listener = mCon.getMessageListener();
      if (listener != null) {
        final String from = packet.getFrom();
        final String to = packet.getTo();
        EventElement event = packet.getExtension("event", PubSubNamespace.EVENT.getXmlns());
        final NodeExtension nodeExt = event.getEvent();
        final MMXTopicId topic = TopicHelper.toTopicId(nodeExt.getNode(), null);
        if (PubSubElementType.ITEMS_EVENT.getElementName().equals(nodeExt.getElementName())) {
          mCon.getExecutor().post(new Runnable() {
            @Override
            public void run() {
              ItemsExtension items = (ItemsExtension) nodeExt;
              List<PayloadItem<MMXPacketExtension>> list =
                  (List<PayloadItem<MMXPacketExtension>>) items.getItems();
              for (Item listItem : list) {
                try {
                  PayloadItem<MMXPacketExtension> item =
                      (PayloadItem<MMXPacketExtension>) listItem;
                  MMXPacketExtension mmx = item.getPayload();
                  MMXMessage msg = new MMXMessage(item.getId(), from, to,
                                                  mmx.getPayload());
                  listener.onItemReceived(msg, topic);
                } catch (ClassCastException e) {
                  // Ignore all non-MMX published item.
                  Log.w(TAG, "Ignoring non-MMX published item ID="+listItem.getId());
                } catch (Throwable e) {
                  Log.w(TAG, "Caught exception handling published item ID="+listItem.getId(), e);
                }
              }
            }
          });


//          DelayInfo delay = packet.getExtension("delay", "urn:xmpp:delay");
//          if (delay != null) {
//            Date tod = delay.getStamp();
//            // The published item is from off-line storage.
//            mCon.getContext().debug(TAG, "delay="+TimeUtil.toString(tod)+
//                ", tod="+TimeUtil.toString(new Date()), null);
//          }

          // Save the last published item delivery time.
          PubSubManager.getInstance(mCon).saveLastDelivery(new Date());
        } else {
          Log.w(TAG, "Ignoring pubsub event: "+
                      nodeExt.getElementName()+", topic="+topic);
        }
      }
    }
  };

  private final PacketListener mErrorMsgPacketListener = new PacketListener() {
    @Override
    public void processPacket(Packet packet) {
      final MMXMessageListener listener = mCon.getMessageListener();
      if (listener != null) {
        final MMXErrorMessage errMsg = new MMXErrorMessage((Message) packet);
        mCon.getExecutor().post(new Runnable() {
          @Override
          public void run() {
            listener.onErrorMessageReceived(errMsg);
          }
        });
      }
    }
  };

  private final PacketListener mMsgPayloadPacketListener = new PacketListener() {
    @Override
    public void processPacket(final Packet packet) throws NotConnectedException {
      final Message xmppmsg = (Message) packet;
      final MMXMessage msg = new MMXMessage(xmppmsg);
      final MMXMessageListener listener = mCon.getMessageListener();
      final String orgMsgId = msg.getMsgIdFromReceipt();
      try {
        if (orgMsgId != null) {
          // Original sender received the delivery receipt.
          if (listener != null) {
            mCon.getExecutor().post(new Runnable() {
              @Override
              public void run() {
                listener.onMessageDelivered(msg.getFrom(), orgMsgId);
                // Must run in a thread because IQ is a blocking call.
                mAckExecutor.post(new SendAck(packet));
              }
            });
          }
        }

        // A mmx stanza is received (partially or completely) along with an
        // optional delivery receipt request.  We don't support a delivery
        // receipt request without mmx stanza yet.
        if (listener != null && msg.getPayload() != null) {
          if (msg.assemble(mCon.getContext())) {
//            // The message is from off-line storage.
//            DelayInfo delay = packet.getExtension("delay", "urn:xmpp:delay");
//            if (delay != null) {
//              Date tod = delay.getStamp();
//              mCon.getContext().debug(TAG, "delay="+TimeUtil.toString(tod), null);
//            }
            mCon.getExecutor().post(new Runnable() {
              @Override
              public void run() {
                try {
                  listener.onMessageReceived(msg, msg.getReceiptId());
                  // Only non fire-and-forget message will trigger an ACK to be sent.
                  if (xmppmsg.getType() != Type.headline) {
                    // Must run in a thread because IQ is a blocking call.
                    mAckExecutor.post(new SendAck(packet));
                  }
                } catch (MessageHandlingException ex) {
                  Log.i(TAG, "Unable to handle the message. NOT sending the ack.", ex);
                }
              }
            });
          } else {
            // Partial message is received.
          }
        }
      } catch (Throwable e) {
        Log.e(TAG, xmppmsg.toString(), e);
      }
    }
  };

  // Reliable message sent callback
  private final PacketListener mMsgSignalPacketListener = new PacketListener() {
    @Override
    public void processPacket(final Packet packet) throws NotConnectedException {
      mCon.getExecutor().post(new Runnable() {
        @Override
        public void run() {
          final MMXMessageListener listener = mCon.getMessageListener();
          MMXSignalMsgHandler.MMXPacketExtension extension = packet.getExtension(
              Constants.MMX, Constants.MMX_NS_MSG_SIGNAL);
          MmxHeaders mmxMeta = extension.getMmxMeta();
          SignalMsg sigMsg = SignalMsg.parse(mmxMeta);
          if (sigMsg.getType() == SignalMsg.Type.ACK_ONCE) {
            mCon.getMessageListener().onMessageAccepted(
                sigMsg.getInvalidReceivers(), sigMsg.getMsgId());
          } else if (sigMsg.getType() == SignalMsg.Type.ACK_BEGIN) {
            mCon.getMessageListener().onMessageSubmitted(sigMsg.getMsgId());
          } else if (sigMsg.getType() == SignalMsg.Type.ACK_END) {
            mCon.getMessageListener().onMessageAccepted(
                sigMsg.getInvalidReceivers(), sigMsg.getMsgId());
          }
        }
      });
    }
  };

//  private PacketListener mMsgPayloadSendListener = new PacketListener() {
//    @Override
//    public void processPacket(Packet packet) throws NotConnectedException {
//      if (mCon.getMessageListener() != null) {
//        MMXMessage msg = new MMXMessage((Message) packet);
//        Payload payload = msg.getPayload().getPayloadExt();
//        if ((payload.getDataOffset()+payload.getDataLen()) == payload.getDataSize()) {
//          mCon.getMessageListener().onMessageSending(msg, new String[] { packet.getTo() } );
//        }
//      }
//    }
//  };

  // Unreliable message sent callback
  private final PacketListener mMsgPayloadSentListener = new PacketListener() {
    @Override
    public void processPacket(final Packet packet) throws NotConnectedException {
      if (mCon.getMessageListener() != null) {
        mCon.getExecutor().post(new Runnable() {
          @Override
          public void run() {
            mCon.getMessageListener().onMessageSent(packet.getPacketID());
          }
        });
      }
    }
  };

  private final PacketListener mMsgPayloadFailedListener = new PacketListener() {
    @Override
    public void processPacket(final Packet packet) throws NotConnectedException {
      if (mCon.getMessageListener() != null) {
        mCon.getExecutor().post(new Runnable() {
          @Override
          public void run() {
            mCon.getMessageListener().onMessageFailed(packet.getPacketID());
          }
        });
      }
    }
  };

  /**
   * @hide
   * Get the instance of MessageManager.
   * @param con
   * @return
   */
  public static MessageManager getInstance(MMXConnection con) {
    return (MessageManager) con.getManager(TAG, sCreator);
  }

  private static class MsgMMXIQHandler<Request, Response>
                                    extends MMXIQHandler<Request, Response> {
    @Override
    public String getElementName() {
      return Constants.MMX;
    }

    @Override
    public String getNamespace() {
      return Constants.MMX_NS_MSG_STATE;
    }
  }

  private static class AckMMXIQHandler extends MMXIQHandler<MsgAck, MMXStatus> {
    @Override
    public String getElementName() {
      return Constants.MMX;
    }

    @Override
    public String getNamespace() {
      return Constants.MMX_NS_MSG_ACK;
    }
  }

  private class SendAck implements Runnable {
    private final Packet mPacket;

    public SendAck(Packet packet) {
      mPacket = packet;
    }

    @Override
    public void run() {
      try {
        MessageManager.this.mAckCounters++;
        MessageManager.this.sendAck(mPacket.getFrom(), mPacket.getTo(),
                                     mPacket.getPacketID());
      } catch (Throwable e) {
        MessageManager.this.mAckErrors++;
        e.printStackTrace();
      }
    }
  }

  private static class PacketCopy extends Packet {
    private final CharSequence text;

    /**
     * Create a copy of a packet with the text to send. The passed text must be
     * a valid text to send to the server, no validation will be done on the
     * passed text.
     *
     * @param text the whole text of the packet to send
     */
    public PacketCopy(CharSequence text) {
      this.text = text;
    }

    @Override
    public CharSequence toXML() {
      return text;
    }
  }

  protected MessageManager(MMXConnection con) {
    mCon = con;
    mAckExecutor = new QueueExecutor("MMX Ack Sender", true);
    mAckExecutor.start();
    MsgMMXIQHandler<MsgsState.Request, MsgsState.Response> msgIQHandler = new
        MsgMMXIQHandler<MsgsState.Request, MsgsState.Response>();
    msgIQHandler.registerIQProvider();
    AckMMXIQHandler ackIQHandler = new AckMMXIQHandler();
    ackIQHandler.registerIQProvider();
  }

  @Override
  public void close() throws IOException {
    if (mAckExecutor != null) {
      mAckExecutor.quit();
      mAckExecutor = null;
    }
  }

  @Override
  protected void finalize() {
    try {
      close();
    } catch (IOException e) {
      // Ignored.
    }
  }

  // This must be called prior to the connection to receive messages.
  void initPacketListener() {
    // Interested in non-error messages with MMX extension or Delivery Receipt.
    PacketFilter msgFilter = new NotFilter(new MessageTypeFilter(Message.Type.error));
    PacketFilter extFilter = new OrFilter(
        new PacketExtensionFilter(Constants.MMX, Constants.MMX_NS_MSG_PAYLOAD),
        new PacketExtensionFilter(DeliveryReceipt.ELEMENT, DeliveryReceipt.NAMESPACE));
    PacketFilter payloadFilter = new AndFilter(extFilter, msgFilter);
    mCon.getXMPPConnection().addPacketListener(mMsgPayloadPacketListener,
                                               payloadFilter);

    PacketFilter signalFilter = new AndFilter(new PacketExtensionFilter(
                    Constants.MMX, Constants.MMX_NS_MSG_SIGNAL), msgFilter);
    mCon.getXMPPConnection().addPacketListener(mMsgSignalPacketListener,
                                              signalFilter);

    // Any MMX or XMPP error messages.
    mCon.getXMPPConnection().addPacketListener(mErrorMsgPacketListener,
        new MessageTypeFilter(Message.Type.error));

    // PubSub event filter.
    PacketFilter eventFilter = new PacketExtensionFilter("event",
        PubSubNamespace.EVENT.getXmlns());
    mCon.getXMPPConnection().addPacketListener(mEventPacketListener,
                                               eventFilter);

    // Sending callback is only applied to message with MMX extension.  The
    // sending callback is not applicable to publishing item because it is
    // based on IQ.s
//    PacketFilter mmxFilter = new PacketExtensionFilter(
//        Constants.MMX, Constants.MMX_NS_MSG_PAYLOAD);
//    mCon.getXMPPConnection().addPacketSendingListener(mMsgPayloadSendListener,
//                                              mmxFilter);

    // Sent and failure callbacks are only applied to message with MMX
    // extension.    These callbacks are not applicable to publishing item
    // because it is based on IQ.
    PacketFilter mmxMsgFilter = new AndFilter(
        new PacketFilter() {
          @Override
          public boolean accept(Packet packet) {
            return packet instanceof org.jivesoftware.smack.packet.Message;
          }
        },
        new PacketExtensionFilter(Constants.MMX, Constants.MMX_NS_MSG_PAYLOAD));
    mCon.getXMPPConnection().addPacketSentListener(mMsgPayloadSentListener,
                                              mmxMsgFilter);
    mCon.getXMPPConnection().addPacketFailedListener(mMsgPayloadFailedListener,
                                              mmxMsgFilter);
  }

  /**
   * Send a message to all subscribers in a topic excluding the sender.  The
   * message is always fire-and-forget, but the sender may request a delivery
   * receipt.
   * @param topic
   * @param payload
   * @param options
   * @return A unique message ID.
   * @throws MMXException
   */
  public String sendPayload(MMXTopic topic, MMXPayload payload, Options options)
                            throws MMXException {
    String userId = Constants.MMX_NODE_PREFIX+((MMXTopicId) topic).getId();
    MMXid[] to = new MMXid[] { new MMXid(userId, null, null) };
    if (options == null) {
      options = new Options();
    }
    options.setDroppable(true);
    return sendPayload(to, payload, options);
  }

  /**
   * Send a payload. A payload can be reliable or droppable. A droppable payload
   * may be dropped if the recipient is not connected. A reliable payload will
   * be stored in the server until the recipient is connected.
   *
   * @param to An array of MMX ID's for user or end-point.
   * @param payload A non-null payload object.
   * @param options Send options or null.
   * @return A unique message ID.
   * @throws MMXException
   */
  public String sendPayload(MMXid[] to, MMXPayload payload,
                             Options options) throws MMXException {
    return sendPayload(null, to, payload, options);
  }

  /**
   * Send a payload with a unique optional message ID. A payload can be reliable
   * or droppable. A droppable payload is not guaranteed on delivery if the
   * recipient is not online. A reliable payload will be stored in the server
   * until the recipient is connected.
   *
   * @param msgId null or a unique message ID.
   * @param to An array of MMX ID's for user or end-point.
   * @param payload A non-null payload object.
   * @param options Send options or null.
   * @return A unique message ID.
   * @throws MMXException
   * @see {@link MMXConnection#genId()}
   */
  public String sendPayload(String msgId, MMXid[] to, MMXPayload payload,
                            Options options) throws MMXException {
    if (payload == null) {
      throw new MMXException("Payload cannot be null", StatusCode.BAD_REQUEST);
    }
    validatePayload(payload);

    String[] xids = XIDUtil.makeXIDs(to, mCon.getAppId(), mCon.getDomain());

    XMPPConnection xmppCon = mCon.getXMPPConnection();
    Message msg = new Message();
    if (msgId == null) {
      msgId = mCon.genId();
    }
    msg.setPacketID(msgId);
    if (options != null && options.isReceiptEnabled()) {
      msg.addExtension(new DeliveryReceiptRequest());
    }

    // Save recipients (because of display name) in mmxmeta stanza
    payload.setFrom(mCon.getXID());
    payload.setTo(to);

    msg.addExtension(new MMXPacketExtension(payload));
    // The headline type will disable off-line storage, message state tracking
    // and receive-ack.
    if (options != null && options.isDroppable()) {
      msg.setType(Message.Type.headline);
    } else {
      msg.setType(Message.Type.chat);
    }

    try {
      if (xids.length > 1) {
        // Unable to use MultiRecipientManager (XEP-0033) because it does not
        // support user display name and the MulticastRouter needs significant
        // changes to support device fan-out, user validation and wakeup.
        // Use MMX multicast implementation which has a special bared MMX ID.
        msg.setTo(XIDUtil.makeXID(Constants.MMX_MULTICAST, mCon.getAppId(),
            mCon.getXMPPConnection().getServiceName()));
        xmppCon.sendPacket(msg);
      } else {
        msg.setTo(xids[0]);
        xmppCon.sendPacket(msg);
      }

      return msg.getPacketID();
    } catch (Throwable e) {
      throw new MMXException(e.getMessage(), e);
    }
  }

  /**
   * Send a reliable error message as a reply to a message.  The following
   * code fragment shows how to send MMXError payload against the original
   * message.  The MMXError payload must include the original message ID.
   * <pre>
   * MMXError error = new MMXError(StatusCode.NOT_FOUND)
   *     .setMessage("Search item not found")
   *     .setSeverity(Severity.TRIVIAL)
   *     .setMsgId(msg.getId());
   * MMXPayload payload = new MMXPayload(error.getMsgType(), error.toJson());
   * this.sendError(msg, payload);
   * </pre>
   * <p>
   * On the receiving side, an MMXErrorMessage will be received and an MMXError
   * payload can be accessed via {@link MMXErrorMessage#getMMXError()}, or a
   * custom error payload can be accessed via {@link MMXErrorMessage#getCustomError()}.
   * </pre>
   * @param originalMessage The original message that caused an error.
   * @param payload A payload with MMXError or custom error object.
   * @return A unique message ID.
   * @throws MMXException
   * @see com.magnet.mmx.protocol.MMXError
   * @see MMXErrorMessage
   */
  public String sendError(MMXMessage originalMessage, MMXPayload payload)
                          throws MMXException {
    if (payload == null) {
      throw new MMXException("Payload cannot be null", StatusCode.BAD_REQUEST);
    }
    if (payload.getSize() > MMXPayload.getMaxSizeAllowed()) {
      throw new MMXException("Payload size exceeds "+
                              MMXPayload.getMaxSizeAllowed()+" bytes",
                              MMXException.REQUEST_TOO_LARGE);
    }
    XMPPConnection xmppCon = mCon.getXMPPConnection();
    Message msg = new Message();
    msg.setType(Message.Type.error);
    msg.setBody(".");
    msg.setPacketID(mCon.genId());
    msg.addExtension(new MMXPacketExtension(payload));
    msg.setTo(originalMessage.getFromJID());
    try {
      xmppCon.sendPacket(msg);
      return msg.getPacketID();
    } catch (Throwable e) {
      throw new MMXException(e.getMessage(), e);
    }
  }

  // TODO: we should offer an option to send a delivery receipt to
  // the user or the originating end-point.
  /**
   * Send the delivery receipt of a message to the originating sender.
   * @param receiptId A delivery receipt ID
   * @throws MMXException
   * @see {@link MMXMessage#hasRequestForReceipt()}
   * @see {@link MMXMessageListener#onMessageReceived(MMXMessage, String)}
   */
  public void sendDeliveryReceipt(String receiptId) throws MMXException {
    String[] msgAndSenderIds = parseReceiptId(receiptId);
    if (msgAndSenderIds == null || msgAndSenderIds.length != 2) {
      return;
    }
    Message msg = new Message();
    msg.setType(Message.Type.chat);
    msg.setTo(msgAndSenderIds[1]);
    msg.setPacketID(mCon.genId());
    msg.setBody(".");
    DeliveryReceipt receipt = new DeliveryReceipt(msgAndSenderIds[0]);
    msg.addExtension(receipt);
    try {
      mCon.getXMPPConnection().sendPacket(msg);
    } catch (NotConnectedException e) {
      throw new MMXException(e.getMessage(), e);
    }
  }

  /**
   * Get the states of multiple messages by their message ID's.  If a message ID
   * does not exist, its state will be {@link Constants.MessageState#UNKNOWN}.
   * @param msgIds An array list of message ID's.
   * @return A Map of message ID and list of message status.
   * @throws MMXException
   */
  public Map<String, List<MMXMessageStatus>> getMessagesState(List<String> msgIds)
                                              throws MMXException {
    MsgsState.Request ids = new MsgsState.Request(msgIds);
    MsgMMXIQHandler<MsgsState.Request, MsgsState.Response> iqHandler = new
        MsgMMXIQHandler<MsgsState.Request, MsgsState.Response>();
    iqHandler.sendGetIQ(mCon, Constants.MessageCommand.query.toString(), ids,
        MsgsState.Response.class, iqHandler);
    MsgsState.Response resp = iqHandler.getResult();
    HashMap<String, List<MMXMessageStatus>> result = new
        HashMap<String, List<MMXMessageStatus>>(resp.size());
    for (Map.Entry<String, MsgsState.MessageStatusList> entry : resp.entrySet()) {
      String msgId = entry.getKey();
      List<MMXMessageStatus> list = result.get(msgId);
      if (list == null) {
        list = new ArrayList<MMXMessageStatus>(entry.getValue().size());
        result.put(msgId, list);
      }
      for (MessageStatus status : entry.getValue()) {
        list.add(new MMXMessageStatus(status));
      }
    }
    return result;
  }

  // Generate a delivery receipt ID based on the sender XID and msg ID.
  static String genReceiptId(String from, String msgId) {
    return Converter.Scrambler.convert((msgId+'#'+from)).toString();
  }

  // Return msg ID and sender ID.
  static String[] parseReceiptId(String receiptId) {
    if (receiptId == null) {
      return null;
    }
    String combinedToken = Converter.Scrambler.convert(receiptId).toString();
    return combinedToken.split("#");
  }

  /**
   * Send an ack for reliable message delivery.  It is sent automatically when
   * a message is received.
   * @param sender The original message sender's full XID.
   * @param rcvr The current user's full XID.
   * @param msgId The message ID to be acknowledged.
   * @return The status.
   * @throws MMXException
   */
  MMXStatus sendAck(String sender, String rcvr, String msgId) throws MMXException {
    try {
      MsgAck ack = new MsgAck(sender, rcvr, msgId);
      AckMMXIQHandler iqHandler = new AckMMXIQHandler();
      iqHandler.sendSetIQ(mCon, Constants.MessageCommand.ack.toString(), ack,
          MMXStatus.class, iqHandler);
      return iqHandler.getResult();
    } catch (MMXException e) {
      throw e;
    } catch (Throwable e) {
      throw new MMXException(e.getMessage(), e);
    }
  }

  /**
   * @hide
   * Get the tags from a message.
   * @param msgId The message ID.
   * @return The message tags.
   * @throws MMXException
   */
  public MsgTags getAllTags(String msgId) throws MMXException {
    return getTags(MsgId.IdType.message, msgId);
  }

  /**
   * @hide
   * Set the tags to a message. The entire old tags will be overwritten by the
   * new tags.
   * @param msgId The message ID.
   * @param tags A list of tags, or an empty list.
   * @return The status.
   * @throws MMXException
   */
  public MMXStatus setAllTags(String msgId, List<String> tags) throws MMXException {
    return doTags(MessageCommand.setTags, MsgId.IdType.message, msgId, tags);
  }

  /**
   * @hide
   * Add the tags to a message.
   * @param msgId The message ID.
   * @param tags A list of tags to be added.
   * @return The status.
   * @throws MMXException
   */
  public MMXStatus addTags(String msgId, List<String> tags) throws MMXException {
    return doTags(MessageCommand.addTags, MsgId.IdType.message, msgId, tags);
  }

  /**
   * @hide
   * Remove the tags from a message.
   * @param msgId The message ID.
   * @param tags A list of tags to be removed.
   * @return The status.
   * @throws MMXException
   */
  public MMXStatus removeTags(String msgId, List<String> tags) throws MMXException {
    return doTags(MessageCommand.removeTags, MsgId.IdType.message, msgId, tags);
  }

  /**
   * @hide
   * Get the events from a message.
   * @param msgId The message ID.
   * @return The message events.
   * @throws MMXException
   */
  public MsgEvents getEvents(String msgId) throws MMXException {
    return getEvents(MsgId.IdType.message, msgId);
  }

  /**
   * @hide
   * Set the events to a message.  The entire old events will be overwritten
   * by the new events.
   * @param msgId The message ID.
   * @param events A list of events, or an empty list.
   * @return The status.
   * @throws MMXException
   */
  public MMXStatus setEvents(String msgId, List<String> events) throws MMXException {
    return doEvents(MessageCommand.setEvents, MsgId.IdType.message, msgId, events);
  }

  /**
   * @hide
   * Add the events to a message.
   * @param msgId The message ID.
   * @param events A list of events to be added.
   * @return The status.
   * @throws MMXException
   */
  public MMXStatus addEvents(String msgId, List<String> events) throws MMXException {
    return doEvents(MessageCommand.addEvents, MsgId.IdType.message, msgId, events);
  }

  /**
   * @hide
   * Remove the events from a message.
   * @param msgId The message ID.
   * @param events A list of events to be removed.
   * @return The status.
   * @throws MMXException
   */
  public MMXStatus removeEvents(String msgId, List<String> events) throws MMXException {
    return doEvents(MessageCommand.removeEvents, MsgId.IdType.message, msgId, events);
  }

  /**
   * @hide
   * Validate the payload before sending it to the server.
   * @throws MMXException
   */
  public void validatePayload(MMXPayload payload) throws MMXException {
    if (payload.getSize() > MMXPayload.getMaxSizeAllowed()) {
      throw new MMXException("Payload size exceeds "+
                              MMXPayload.getMaxSizeAllowed()+" bytes",
                              MMXException.REQUEST_TOO_LARGE);
    }
  }

  // Used by MessageManager and PushManager.
  MsgTags getTags(MsgId.IdType idType, String msgId) throws MMXException {
    if (msgId == null) {
      throw new MMXException("Message ID cannot be null", StatusCode.BAD_REQUEST);
    }
    MsgMMXIQHandler<MsgId, MsgTags> iqHandler = new
        MsgMMXIQHandler<MsgId, MsgTags>();
    iqHandler.sendGetIQ(mCon, Constants.MessageCommand.getTags.toString(),
        new MsgId(idType, msgId), MsgTags.class, iqHandler);
    MsgTags msgTags = iqHandler.getResult();
    return msgTags;
  }

  // Used by MessageManager and PushManager.
  MMXStatus doTags(MessageCommand cmd, MsgId.IdType idType, String msgId,
                            List<String> tags) throws MMXException {
    if (msgId == null) {
      throw new MMXException("Message ID cannot be null", StatusCode.BAD_REQUEST);
    }
    if (cmd == MessageCommand.setTags) {
      if (tags == null) {
        tags = new ArrayList<String>(0);
      }
      if (!tags.isEmpty()) {
        validateTags(tags);
      }
    } else {
      validateTags(tags);
    }
    MsgEvents msgTags = new MsgEvents(idType, msgId, tags);
    MsgMMXIQHandler<MsgEvents, MMXStatus> iqHandler = new
        MsgMMXIQHandler<MsgEvents, MMXStatus>();
    iqHandler.sendSetIQ(mCon, cmd.toString(), msgTags, MMXStatus.class, iqHandler);
    return iqHandler.getResult();
  }

  // Used by MessageManager and PushManager.
  MsgEvents getEvents(MsgId.IdType idType, String msgId) throws MMXException {
    if (msgId == null) {
      throw new MMXException("Message ID cannot be null", StatusCode.BAD_REQUEST);
    }
    MsgMMXIQHandler<MsgId, MsgEvents> iqHandler = new
        MsgMMXIQHandler<MsgId, MsgEvents>();
    iqHandler.sendGetIQ(mCon, Constants.MessageCommand.getEvents.toString(),
        new MsgId(idType, msgId), MsgEvents.class, iqHandler);
    MsgEvents msgEvents = iqHandler.getResult();
    return msgEvents;
  }

  // Used by MessageManager and PushManager.
  MMXStatus doEvents(MessageCommand cmd, MsgId.IdType idType, String msgId,
                            List<String> events) throws MMXException {
    if (msgId == null) {
      throw new MMXException("Message ID cannot be null", StatusCode.BAD_REQUEST);
    }
    if (cmd == MessageCommand.setEvents) {
      if (events == null) {
        events = new ArrayList<String>(0);
      }
    } else {
      if (events == null || events.isEmpty()) {
        throw new MMXException("List of tags cannot be null or empty", StatusCode.BAD_REQUEST);
      }
    }
    MsgEvents msgEvents = new MsgEvents(idType, msgId, events);
    MsgMMXIQHandler<MsgEvents, MMXStatus> iqHandler = new
        MsgMMXIQHandler<MsgEvents, MMXStatus>();
    iqHandler.sendSetIQ(mCon, cmd.toString(), msgEvents, MMXStatus.class, iqHandler);
    return iqHandler.getResult();
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
   * @hide
   * Return the ack statistics.
   * @return The number of acks sent and number of acks encountered error.
   */
  public int[] getAckStat() {
    return new int[] { mAckCounters, mAckErrors };
  }
}
