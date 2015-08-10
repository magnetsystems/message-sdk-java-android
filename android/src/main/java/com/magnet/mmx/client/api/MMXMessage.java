package com.magnet.mmx.client.api;

import com.magnet.mmx.client.MMXClient;
import com.magnet.mmx.client.common.MMXException;
import com.magnet.mmx.client.common.MMXGlobalTopic;
import com.magnet.mmx.client.common.MMXPayload;
import com.magnet.mmx.client.common.MMXid;
import com.magnet.mmx.client.common.Options;
import com.magnet.mmx.protocol.MMXTopic;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * The message class
 */
public class MMXMessage {
  private String mId;
  private Date mTimestamp;
  private MMXid mSender;
  private MMXChannel mChannel;
  private Set<MMXid> mRecipients = new HashSet<MMXid>();
  private HashMap<String, Object> mContent = new HashMap<String, Object>();

  /**
   * Default constructor
   */
  public MMXMessage() {

  }

  /**
   * Set the message id ofr this MMXMessage object.
   *
   * @param id the message id
   * @return this MMXMessage object
   */
  MMXMessage id(String id) {
    mId = id;
    return this;
  }

  /**
   * The message id for this MMXMessage
   * NOTE:  This is for incoming messages only.
   *
   * @return the message id
   */
  public String getId() {
    return mId;
  }

  /**
   * Set timestamp for this MMXMessage (sent time).
   *
   * @param timestamp the timestamp
   * @return this MMXMessage object
   */
  MMXMessage timestamp(Date timestamp) {
    mTimestamp = timestamp;
    return this;
  }

  /**
   * The timestamp for this MMXMessage (sent time)
   * NOTE:  This is for incoming messages only.
   *
   * @return the timestamp
   */
  public Date getTimestamp() {
    return mTimestamp;
  }

  /**
   * Set the sender for this MMXMessage.
   *
   * @param sender the sender
   * @return this MMXMessage object
   */
  MMXMessage sender(MMXid sender) {
    mSender = sender;
    return this;
  }

  /**
   * The sender of this MMXMessage.
   * NOTE:  This is for incoming messages only.
   *
   * @return the sender
   */
  public MMXid getSender() {
    return mSender;
  }

  /**
   * Set the channel for this message
   *
   * @param channel the channel
   * @return this MMXMessage object
   */
  public MMXMessage channel(MMXChannel channel) {
    mChannel = channel;
    return this;
  }

  /**
   * The channel for this message
   *
   * @return the channel
   */
  public MMXChannel getChannel() {
    return mChannel;
  }

  /**
   * Set the set of recipients
   *
   * @param recipients the recipients
   * @return this MMXMessage object
   */
  public MMXMessage recipients(Set<MMXid> recipients) {
    mRecipients = recipients;
    return this;
  }

  /**
   * The recipients for this message
   *
   * @return the recipients
   */
  public Set<MMXid> getRecipients() {
    return mRecipients;
  }

  /**
   * Add a single recipient to the existing set
   *
   * @param recipient the recipient to add
   * @return this MMXMessage object
   */
  public MMXMessage addRecipient(MMXid recipient) {
    mRecipients.add(recipient);
    return this;
  }

  /**
   * Sets the content for this message
   * NOTE:  The values in the map will be flattened to their toString() representations.
   *
   * @param content the content
   * @return this MMXMessage instance
   */
  public MMXMessage content(HashMap<String, Object> content) {
    mContent = content;
    return this;
  }

  /**
   * The content for this message
   * NOTE:  The values in the map will be flattened to their toString() representations.
   *
   * @return the content
   */
  public HashMap<String, Object> getContent() {
    return mContent;
  }

  /**
   * Add this key value pair to the existing content.  Any content with
   * the same key will be replaced.
   * NOTE:  The values in the map will be flattened to their toString() representations.
   *
   * @param key the key
   * @param value the value
   * @return this MMXMessage instance
   */
  public MMXMessage addContent(String key, Object value) {
    mContent.put(key, value);
    return this;
  }

  /**
   * Send the current message.  If the message has both a topic and
   * other recipients, the OnFinishedListener will be called with the
   * message id for the message to the recipients.  If this message only
   * has a topic, the listener will be called with the id of the published
   * message.
   *
   * @param listener the listener for this method call
   */
  public void send(MagnetMessage.OnFinishedListener<String> listener) {
    //validate message
    MMXClient client = MagnetMessage.getMMXClient();
    MMXPayload payload = new MMXPayload("");
    for (Map.Entry<String, Object> entry : mContent.entrySet()) {
      payload.setMetaData(entry.getKey(), entry.getValue().toString());
    }
    if (mChannel != null) {
      try {
        String publishedId = client.getPubSubManager().publish(new MMXGlobalTopic(mChannel.getName()), payload);
        if (mRecipients.isEmpty()) {
          listener.onSuccess(publishedId);
          return;
        }
      } catch (MMXException e) {
        listener.onFailure(MagnetMessage.FailureCode.EXCEPTION, e);
        return;
      }
    }
    if (!mRecipients.isEmpty()) {
      try {
        String messageId = client.getMessageManager().sendPayload((MMXid[])mRecipients.toArray(), payload, new Options());
        listener.onSuccess(messageId);
      } catch (MMXException e) {
        listener.onFailure(MagnetMessage.FailureCode.EXCEPTION, e);
      }
    }
  }

  /**
   * Build a reply message.
   *
   * @param isReplyAll reply to other recipients in addition to the sender
   * @return a new MMXMessage instance for the reply
   */
  public MMXMessage buildReply(boolean isReplyAll) {
    try {
      MMXid me = MagnetMessage.getMMXClient().getClientId();
      HashSet<MMXid> replyRecipients = new HashSet<MMXid>();
      replyRecipients.add(mSender);
      if (isReplyAll) {
        for (MMXid recipient : mRecipients) {
          if (!recipient.equalsTo(me)) {
            //remove myself from the recipients
            //this applies to instances of me (including other devices)
            replyRecipients.add(recipient);
          }
        }
      }
      HashMap<String, Object> replyContent = new HashMap<String, Object>();
      for (Map.Entry<String,Object> entry : mContent.entrySet()) {
        replyContent.put(entry.getKey(), entry.getValue());
      }
      return new MMXMessage()
              .channel(mChannel)
              .recipients(replyRecipients)
              .content(replyContent);
    } catch (MMXException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Convenience method to construct this object from a lower level MMXMessage object
   *
   * @param message the lower level MMXMessage object
   * @return a new object of this type
   */
  static MMXMessage fromMMXMessage(MMXTopic topic, com.magnet.mmx.client.common.MMXMessage message) {
    HashSet<MMXid> recipients = new HashSet<MMXid>();
    recipients.add(message.getTo());
    for (MMXid otherRecipient : message.getReplyAll()) {
      recipients.add(otherRecipient);
    }
    HashMap<String, Object> content = new HashMap<String, Object>();
    for (Map.Entry<String,String> entry : message.getPayload().getAllMetaData().entrySet()) {
      content.put(entry.getKey(), entry.getValue());
    }

    MMXMessage newMessage = new MMXMessage();
    return newMessage
            .sender(message.getFrom())
            .id(message.getId())
            .channel(MMXChannel.fromTopic(topic))
            .timestamp(message.getPayload().getSentTime())
            .recipients(recipients)
            .content(content);
  }
}
