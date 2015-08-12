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
  public static final class Builder {
    private MMXMessage mMessage;

    public Builder() {
      mMessage = new MMXMessage();
    }

    /**
     * Set the message id of the MMXMessage object.
     *
     * @param id the message id
     * @return this builder object
     */
    MMXMessage.Builder id(String id) {
      mMessage.id(id);
      return this;
    }

    /**
     * Set timestamp for the MMXMessage (sent time).
     *
     * @param timestamp the timestamp
     * @return this MMXMessage object
     */
    MMXMessage.Builder timestamp(Date timestamp) {
      mMessage.timestamp(timestamp);
      return this;
    }

    /**
     * Set the sender for the MMXMessage.
     *
     * @param sender the sender
     * @return this MMXMessage object
     */
    MMXMessage.Builder sender(MMXid sender) {
      mMessage.sender(sender);
      return this;
    }

    /**
     * Set the channel for the MMXMessage
     *
     * @param channel the channel
     * @return this MMXMessage object
     */
    public MMXMessage.Builder channel(MMXChannel channel) {
      if (mMessage.getRecipients().size() > 0) {
        throw new IllegalArgumentException("Cannot set both the recipients and channel in a message.");
      }
      mMessage.channel(channel);
      return this;
    }

    /**
     * Set the set of recipients for the MMXMssage
     *
     * @param recipients the recipients
     * @return this MMXMessage object
     */
    public MMXMessage.Builder recipients(Set<MMXid> recipients) {
      if (mMessage.getChannel() != null) {
        throw new IllegalArgumentException("Cannot set both the recipients and channel in a message.");
      }
      mMessage.recipients(recipients);
      return this;
    }

    /**
     * Sets the content for the MMXMessage
     * NOTE:  The values in the map will be flattened to their toString() representations.
     *
     * @param content the content
     * @return this MMXMessage instance
     */
    public MMXMessage.Builder content(HashMap<String, Object> content) {
      mMessage.content(content);
      return this;
    }

    public MMXMessage build() {
      return mMessage;
    }
  }

  private String mId;
  private Date mTimestamp;
  private MMXid mSender;
  private MMXChannel mChannel;
  private Set<MMXid> mRecipients = new HashSet<MMXid>();
  private Map<String, Object> mContent = new HashMap<String, Object>();

  /**
   * Default constructor
   */
  private MMXMessage() {

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
  MMXMessage channel(MMXChannel channel) {
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
  MMXMessage recipients(Set<MMXid> recipients) {
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
   * Sets the content for this message
   * NOTE:  The values in the map will be flattened to their toString() representations.
   *
   * @param content the content
   * @return this MMXMessage instance
   */
  MMXMessage content(Map<String, Object> content) {
    mContent = content;
    return this;
  }

  /**
   * The content for this message
   * NOTE:  The values in the map will be flattened to their toString() representations.
   *
   * @return the content
   */
  public Map<String, Object> getContent() {
    return mContent;
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
  public String send(MagnetMessage.OnFinishedListener<String> listener) {
    //validate message
    if (mChannel != null && mRecipients.size() > 0) {
      throw new IllegalArgumentException("Cannot send to both a channel and recipients");
    } else if (mChannel == null && mRecipients.size() == 0) {
      throw new IllegalArgumentException("Unable to send.  No channel and no recipients");
    }

    MMXClient client = MagnetMessage.getMMXClient();
    MMXPayload payload = new MMXPayload("");
    for (Map.Entry<String, Object> entry : mContent.entrySet()) {
      payload.setMetaData(entry.getKey(), entry.getValue().toString());
    }
    if (mChannel != null) {
      try {
        String publishedId = client.getPubSubManager().publish(new MMXGlobalTopic(mChannel.getName()), payload);
        //TODO:  Delay this until the server ack is received
        listener.onSuccess(publishedId);
        return publishedId;
      } catch (MMXException e) {
        listener.onFailure(MagnetMessage.FailureCode.SERVER_EXCEPTION, e);
        return null;
      }
    } else {
      try {
        MMXid[] recipientsArray = new MMXid[mRecipients.size()];
        mRecipients.toArray(recipientsArray);
        String messageId = client.getMessageManager().sendPayload(recipientsArray, payload, new Options());
        //TODO:  Delay this until the server ack is received
        listener.onSuccess(messageId);
        return messageId;
      } catch (MMXException e) {
        listener.onFailure(MagnetMessage.FailureCode.SERVER_EXCEPTION, e);
        return null;

      }
    }
  }

  public String reply(Map<String, Object> replyContent,
                      MagnetMessage.OnFinishedListener<String> listener) {
    if (mTimestamp == null) {
      throw new IllegalStateException("Cannot reply on an outgoing message.");
    }
    MMXMessage reply = buildReply(replyContent, false);
    return reply.send(listener);
  }

  public String replyAll(Map<String, Object> replyContent,
                         MagnetMessage.OnFinishedListener<String> listener) {
    if (mTimestamp == null) {
      throw new IllegalStateException("Cannot reply on an outgoing message.");
    }
    MMXMessage reply = buildReply(replyContent, true);
    return reply.send(listener);
  }


  /**
   * Build a reply message.
   *
   * @param isReplyAll reply to other recipients in addition to the sender
   * @return a new MMXMessage instance for the reply
   */
  private MMXMessage buildReply(Map<String, Object> content, boolean isReplyAll) {
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
      return new MMXMessage()
              .channel(mChannel)
              .recipients(replyRecipients)
              .content(content);
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
    MMXid[] otherRecipients = message.getReplyAll();
    if (otherRecipients != null) {
      //TODO: message.getReplyAll() may return null.  It should probably return a zero-length array.
      for (MMXid otherRecipient : otherRecipients) {
        recipients.add(otherRecipient);
      }
    }
    HashMap<String, Object> content = new HashMap<String, Object>();
    for (Map.Entry<String,String> entry : message.getPayload().getAllMetaData().entrySet()) {
      content.put(entry.getKey(), entry.getValue());
    }

    MMXMessage newMessage = new MMXMessage();
    return newMessage
            .sender(message.getFrom())
            .id(message.getId())
            .channel(MMXChannel.fromMMXTopic(topic))
            .timestamp(message.getPayload().getSentTime())
            .recipients(recipients)
            .content(content);
  }
}
