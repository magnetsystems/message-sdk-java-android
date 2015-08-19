package com.magnet.mmx.client.api;

import com.magnet.mmx.client.MMXClient;
import com.magnet.mmx.client.MMXTask;
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

  /**
   * The builder for the MMXMessage class
   */
  public static final class Builder {
    private MMXMessage mMessage;

    public Builder() {
      mMessage = new MMXMessage();
    }

    /**
     * Set the message id of the MMXMessage object.
     *
     * @param id the message id
     * @return this Builder instance
     */
    MMXMessage.Builder id(String id) {
      mMessage.id(id);
      return this;
    }

    /**
     * Set timestamp for the MMXMessage (sent time).
     *
     * @param timestamp the timestamp
     * @return this Builder instance
     */
    MMXMessage.Builder timestamp(Date timestamp) {
      mMessage.timestamp(timestamp);
      return this;
    }

    /**
     * Set the sender for the MMXMessage.
     *
     * @param sender the sender
     * @return this Builder instance
     */
    MMXMessage.Builder sender(MMXUser sender) {
      mMessage.sender(sender);
      return this;
    }

    /**
     * Set the channel for the MMXMessage
     *
     * @param channel the channel
     * @return this Builder instance
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
     * @return this Builder instance
     */
    public MMXMessage.Builder recipients(Set<MMXUser> recipients) {
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
     * @return this Builder instance
     */
    public MMXMessage.Builder content(Map<String, String> content) {
      mMessage.content(content);
      return this;
    }

    /**
     * Sets the receiptId for this MMXMessage
     *
     * @param receiptId the receiptId
     * @return this Builder instance
     */
    MMXMessage.Builder receiptId(String receiptId) {
      mMessage.mReceiptId = receiptId;
      return this;
    }

    /**
     * Builds the MMXMessage
     *
     * @return the MMXMessage
     */
    public MMXMessage build() {
      return mMessage;
    }
  }

  private String mId;
  private Date mTimestamp;
  private MMXUser mSender;
  private MMXChannel mChannel;
  private Set<MMXUser> mRecipients = new HashSet<MMXUser>();
  private Map<String, String> mContent = new HashMap<String, String>();
  private String mReceiptId;

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
  MMXMessage sender(MMXUser sender) {
    mSender = sender;
    return this;
  }

  /**
   * The sender of this MMXMessage.
   * NOTE:  This is for incoming messages only.
   *
   * @return the sender
   */
  public MMXUser getSender() {
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
  MMXMessage recipients(Set<MMXUser> recipients) {
    mRecipients = recipients;
    return this;
  }

  /**
   * The recipients for this message
   *
   * @return the recipients
   */
  public Set<MMXUser> getRecipients() {
    return mRecipients;
  }

  /**
   * Sets the content for this message
   *
   * @param content the content
   * @return this MMXMessage instance
   */
  MMXMessage content(Map<String, String> content) {
    mContent = content;
    return this;
  }

  /**
   * The content for this message
   *
   * @return the content
   */
  public Map<String, String> getContent() {
    return mContent;
  }

  /**
   * Sets the receiptId for this MMXMessage
   *
   * @param receiptId the receiptId
   * @return this MMXMessage instance
   */
  MMXMessage receiptId(String receiptId) {
    mReceiptId = receiptId;
    return this;
  }

  /**
   * The receiptId for this message
   *
   * @return the receipt id
   */
  String getReceiptId() {
    return mReceiptId;
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
  public String send(final MMX.OnFinishedListener<String> listener) {
    //validate message
    if (mChannel != null && mRecipients.size() > 0) {
      throw new IllegalArgumentException("Cannot send to both a channel and recipients");
    } else if (mChannel == null && mRecipients.size() == 0) {
      throw new IllegalArgumentException("Unable to send.  No channel and no recipients");
    }
    final String generatedMessageId = MMX.getMMXClient().generateMessageId();
    final MMXPayload payload = new MMXPayload("");
    for (Map.Entry<String, String> entry : mContent.entrySet()) {
      payload.setMetaData(entry.getKey(), entry.getValue());
    }
    MMXTask<String> task;
    if (mChannel != null) {
      task = new MMXTask<String>(MMX.getMMXClient(), MMX.getHandler()) {
        @Override
        public String doRun(MMXClient mmxClient) throws Throwable {
          String publishedId = mmxClient.getPubSubManager().publish(generatedMessageId, mChannel.getMMXTopic(), payload);
          //TODO:  Delay this until the server ack is received
          if (!generatedMessageId.equals(publishedId)) {
            throw new RuntimeException("SDK Error: The returned published message id does not match the generated message id.");
          }
          return publishedId;
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
    } else {
      task = new MMXTask<String>(MMX.getMMXClient(), MMX.getHandler()) {
        @Override
        public String doRun(MMXClient mmxClient) throws Throwable {
          MMXid[] recipientsArray = new MMXid[mRecipients.size()];
          int index = 0;
          for (MMXUser recipient : mRecipients) {
            recipientsArray[index++] = new MMXid(recipient.getUsername(), null);
          }
          String messageId = mmxClient.getMessageManager().sendPayload(generatedMessageId, recipientsArray, payload,
                  new Options().enableReceipt(true));
          //TODO:  Delay this until the server ack is received
          if (!generatedMessageId.equals(messageId)) {
            throw new RuntimeException("SDK Error:  The returned message id does not match the generated message id");
          }
          return messageId;
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
    }
    task.execute();
    return generatedMessageId;
  }

  /**
   * Reply to the sender of the current message with the specified content
   *
   * @param replyContent the content to include in the reply
   * @param listener onSuccess will return the message id of the reply message
   * @return the message id
   */
  public String reply(Map<String, String> replyContent,
                      MMX.OnFinishedListener<String> listener) {
    if (mTimestamp == null) {
      throw new IllegalStateException("Cannot reply on an outgoing message.");
    }
    MMXMessage reply = buildReply(replyContent, false);
    return reply.send(listener);
  }

  /**
   * Reply to all of the recipients with the specified content
   *
   * @param replyContent the content to include in the reply
   * @param listener onSuccess will returh the message id of the reply message
   * @return the message id
   */
  public String replyAll(Map<String, String> replyContent,
                         MMX.OnFinishedListener<String> listener) {
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
  private MMXMessage buildReply(Map<String, String> content, boolean isReplyAll) {
    MMXUser me = MMX.getCurrentUser();
    HashSet<MMXUser> replyRecipients = new HashSet<MMXUser>();
    replyRecipients.add(mSender);
    if (isReplyAll) {
      for (MMXUser recipient : mRecipients) {
        if (!recipient.equals(me)) {
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
  }

  /**
   * Acknowledge this message.  This will invoke
   *
   * @param listener the listener for this call
   * @see MMX.OnFinishedListener
   */
  public final void acknowledge(final MMX.OnFinishedListener<Void> listener) {
    if (mReceiptId == null) {
      throw new IllegalArgumentException("Cannot acknowledge() this message: " + mId);
    }
    MMXTask<Void> task = new MMXTask<Void>(MMX.getMMXClient(), MMX.getHandler()) {
      @Override
      public Void doRun(MMXClient mmxClient) throws Throwable {
        MMX.getMMXClient().getMessageManager().sendReceipt(mReceiptId);
        return null;
      }

      @Override
      public void onException(Throwable exception) {
        listener.onFailure(MMX.FailureCode.SERVER_EXCEPTION, exception);
      }

      @Override
      public void onResult(Void result) {
        listener.onSuccess(null);
      }
    };
    task.execute();
  }

  /**
   * Convenience method to construct this object from a lower level MMXMessage object
   *
   * @param message the lower level MMXMessage object
   * @return a new object of this type
   */
  static MMXMessage fromMMXMessage(MMXTopic topic, com.magnet.mmx.client.common.MMXMessage message) {
    HashSet<MMXUser> recipients = new HashSet<MMXUser>();
    if (topic == null) {
      //this is normal message.  getReplyAll() returns null for pubsub messages
      MMXUser receiver = new MMXUser.Builder()
              .username(message.getTo().getUserId())
              .displayName(message.getTo().getDisplayName())
              .build();
      recipients.add(receiver);
      MMXid[] otherRecipients = message.getReplyAll();
      if (otherRecipients != null) {
        for (MMXid otherRecipient : otherRecipients) {
          MMXUser recipient = new MMXUser.Builder()
                  .username(otherRecipient.getUserId())
                  .displayName(otherRecipient.getDisplayName())
                  .build();
          recipients.add(recipient);
        }
      }
    }
    HashMap<String, String> content = new HashMap<String, String>();
    for (Map.Entry<String,String> entry : message.getPayload().getAllMetaData().entrySet()) {
      content.put(entry.getKey(), entry.getValue());
    }

    MMXMessage newMessage = new MMXMessage();
    MMXUser sender = new MMXUser.Builder()
            .username(message.getFrom().getUserId())
            .displayName(message.getFrom().getDisplayName())
            .build();
    return newMessage
            .sender(sender)
            .id(message.getId())
            .channel(MMXChannel.fromMMXTopic(topic))
            .timestamp(message.getPayload().getSentTime())
            .recipients(recipients)
            .content(content);
  }
}
