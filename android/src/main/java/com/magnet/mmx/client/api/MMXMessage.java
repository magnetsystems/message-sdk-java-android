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

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.magnet.mmx.client.MMXClient;
import com.magnet.mmx.client.MMXTask;
import com.magnet.mmx.client.common.Log;
import com.magnet.mmx.client.common.MMXException;
import com.magnet.mmx.client.common.MMXPayload;
import com.magnet.mmx.client.common.MMXid;
import com.magnet.mmx.client.common.Options;
import com.magnet.mmx.protocol.MMXTopic;

/**
 * The message class
 */
public class MMXMessage {
  private static final String TAG = MMXMessage.class.getSimpleName();

  /**
   * Failure codes for the MMXMessage class.
   */
  public static class FailureCode extends MMX.FailureCode {
    public static final FailureCode INVALID_RECIPIENT = new FailureCode(404, "INVALID_RECIPIENT");
    public static final FailureCode CONTENT_TOO_LARGE = new FailureCode(413, "CONTENT_TOO_LARGE");
    
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
   * The OnFinishedListener for MMXMessage methods.
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
     * Set the message type for the MMXMessage object.
     *
     * @param type the message type
     * @return this Builder instance
     */
    MMXMessage.Builder type(String type) {
      mMessage.type(type);
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
     * Validate and builds the MMXMessage
     *
     * @return the MMXMessage
     * @throws IllegalArgmentException
     */
    public MMXMessage build() {
      //validate message
      if (mMessage.mChannel == null && mMessage.mRecipients.size() == 0) {
        throw new IllegalArgumentException("No channel and no recipients are specified");
      }
      return mMessage;
    }
  }

  private String mId;
  private String mType;
  private Date mTimestamp;
  private MMXUser mSender;
  private MMXChannel mChannel;
  private Set<MMXUser> mRecipients = new HashSet<MMXUser>();
  private Map<String, String> mContent = new HashMap<String, String>();
  private String mReceiptId;

  /**
   * Default constructor
   */
  MMXMessage() {

  }

  /**
   * Set the message id of this MMXMessage object.
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
   * Set the message type for this MMXMessage object.
   *
   * @param type the type
   * @return this MMXMessage object
   */
  MMXMessage type(String type) {
    mType = type;
    return this;
  }

  /**
   * The message type for this MMXMessage
   *
   * @return the message type
   */
  String getType() {
    return mType;
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

  // Publish this message to a channel.  This code should belong to MMXChannel.
  String publish(final MMXChannel.OnFinishedListener<String> listener) {
    if (MMX.getCurrentUser() == null) {
      //FIXME:  This needs to be done in MMXClient/MMXMessageManager.  Do it here for now.
      final Throwable exception = new IllegalStateException("Cannot send message.  " +
              "There is no current user.  Please login() first.");
      if (listener == null) {
        Log.w(TAG, "send() failed", exception);
      } else {
        MMX.getHandler().post(new Runnable() {
          public void run() {
            listener.onFailure(MMXChannel.FailureCode.fromMMXFailureCode(
                    MMX.FailureCode.BAD_REQUEST, exception), exception);
          }
        });
      }
      return null;
    }
    final String generatedMessageId = MMX.getMMXClient().generateMessageId();
    final String type = getType() != null ? getType() : null;
    final MMXPayload payload = new MMXPayload(type, "");
    for (Map.Entry<String, String> entry : mContent.entrySet()) {
      payload.setMetaData(entry.getKey(), entry.getValue());
    }
    MMXTask<String> task = new MMXTask<String>(MMX.getMMXClient(),
        MMX.getHandler()) {
      @Override
      public String doRun(MMXClient mmxClient) throws Throwable {
        String publishedId = mmxClient.getPubSubManager().publish(
            generatedMessageId, mChannel.getMMXTopic(), payload);
        if (!generatedMessageId.equals(publishedId)) {
          throw new RuntimeException(
              "SDK Error: The returned published message id does not match the generated message id.");
        }
        return publishedId;
      }

      @Override
      public void onException(Throwable exception) {
        if (listener != null) {
          listener.onFailure(MMXChannel.FailureCode.fromMMXFailureCode(
              MMXChannel.FailureCode.DEVICE_ERROR, exception), exception);
        }
      }

      @Override
      public void onResult(String result) {
        if (listener != null) {
          listener.onSuccess(result);
        }
      }
    };
    id(generatedMessageId);
    task.execute();
    return generatedMessageId;
  }
  
  /**
   * Send the current message to server.  If the message is addressed to
   * recipients, the {@link OnFinishedListener#onSuccess(Object)} will be called
   * with the message id for the message to all valid recipients.  If there are
   * any invalid recipients in the message, a partial failure code
   * {@link FailureCode#INVALID_RECIPIENT} in 
   * {@link OnFinishedListener#onFailure(FailureCode, Throwable)} will be
   * invoked.  The message ID and a set of invalid recipients can be retrieved
   * from {@link MMXUser.InvalidUserException#getUsers()}. If this message is
   * addressed to a channel, the listener will be called with the id of the
   * published message.  Common failure codes are 
   * {@link FailureCode#CONTENT_TOO_LARGE}, {@link FailureCode#BAD_REQUEST}, or
   * FailureCode#DEVICE_ERROR.
   *
   * @param listener the listener for this method call
   */
  public String send(final OnFinishedListener<String> listener) {
    if (MMX.getCurrentUser() == null) {
      //FIXME:  This needs to be done in MMXClient/MMXMessageManager.  Do it here for now.
      final Throwable exception = new IllegalStateException("Cannot send message.  " +
              "There is no current user.  Please login() first.");
      if (listener == null) {
        Log.w(TAG, "send() failed", exception);
      } else {
        MMX.getHandler().post(new Runnable() {
          public void run() {
            listener.onFailure(FailureCode.fromMMXFailureCode(MMX.FailureCode.BAD_REQUEST, exception), exception);
          }
        });
      }
      return null;
    }
    final String generatedMessageId = MMX.getMMXClient().generateMessageId();
    final String type = getType() != null ? getType() : null;
    final MMXPayload payload = new MMXPayload(type, "");
    for (Map.Entry<String, String> entry : mContent.entrySet()) {
      payload.setMetaData(entry.getKey(), entry.getValue());
    }
    MMXTask<String> task;
    if (mChannel != null) {
      task = new MMXTask<String>(MMX.getMMXClient(), MMX.getHandler()) {
        @Override
        public String doRun(MMXClient mmxClient) throws Throwable {
          String publishedId = mmxClient.getPubSubManager().publish(
              generatedMessageId, mChannel.getMMXTopic(), payload);
          if (!generatedMessageId.equals(publishedId)) {
            throw new RuntimeException(
                "SDK Error: The returned published message id does not match the generated message id.");
          }
          return publishedId;
        }

        @Override
        public void onException(Throwable exception) {
          if (listener != null) {
            listener.onFailure(FailureCode.fromMMXFailureCode(
                FailureCode.DEVICE_ERROR, exception), exception);
          }
        }

        @Override
        public void onResult(String result) {
          if (listener != null) {
            listener.onSuccess(result);
          }
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
          if (listener != null) {
            synchronized (sMessageSendListeners) {
              HashSet<String> recipientSet = new HashSet<String>();
              for (MMXid mmxId : recipientsArray) {
                recipientSet.add(mmxId.getUserId());
              }
              sMessageSendListeners.put(generatedMessageId, new RecipientListenerPair(recipientSet, listener));
            }
          }
          String messageId = mmxClient.getMessageManager().sendPayload(generatedMessageId, recipientsArray, payload,
                  new Options().enableReceipt(true));

          if (!generatedMessageId.equals(messageId)) {
            throw new RuntimeException("SDK Error:  The returned message id does not match the generated message id");
          }
          return messageId;
        }

        @Override
        public void onException(Throwable exception) {
          if (listener != null) {
            listener.onFailure(FailureCode.fromMMXFailureCode(FailureCode.DEVICE_ERROR, exception), exception);
          }
        }

        @Override
        public void onResult(String result) {
          // No-op.  Wait until a server ack or an error message is received.
        }
      };
    }
    id(generatedMessageId);
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
                      OnFinishedListener<String> listener) {
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
                         OnFinishedListener<String> listener) {
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
   * Acknowledge this message.  This will send a delivery receipt back to the
   * original sender.
   *
   * @param listener the listener for this call
   * @see OnFinishedListener
   */
  public final void acknowledge(final OnFinishedListener<Void> listener) {
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
        if (listener != null) {
          listener.onFailure(FailureCode.fromMMXFailureCode(FailureCode.DEVICE_ERROR, exception), exception);
        }
      }

      @Override
      public void onResult(Void result) {
        if (listener != null) {
          listener.onSuccess(null);
        }
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
      MMXUser receiver = MMXUser.fromMMXid(message.getTo());
      recipients.add(receiver);
      MMXid[] otherRecipients = message.getReplyAll();
      if (otherRecipients != null) {
        for (MMXid otherRecipient : otherRecipients) {
          MMXUser recipient = MMXUser.fromMMXid(otherRecipient);
          recipients.add(recipient);
        }
      }
    }
    HashMap<String, String> content = new HashMap<String, String>();
    for (Map.Entry<String,String> entry : message.getPayload().getAllMetaData().entrySet()) {
      content.put(entry.getKey(), entry.getValue());
    }

    MMXMessage newMessage = new MMXMessage();
    MMXUser sender = MMXUser.fromMMXid(message.getFrom());
    return newMessage
            .sender(sender)
            .id(message.getId())
            .channel(MMXChannel.fromMMXTopic(topic))
            .timestamp(message.getPayload().getSentTime())
            .recipients(recipients)
            .content(content);
  }

  //For handling the onSuccess of send() messages when server ack is received
  private class RecipientListenerPair {
    private final Set<String> recipients;
    private final OnFinishedListener<String> listener;
    private Exception exception;

    private RecipientListenerPair(Set<String> recipients,
                                  OnFinishedListener<String> listener) {
      this.recipients = recipients;
      this.listener = listener;
    }

    private synchronized boolean onAckOrErrorReceived(String recipient) {
      recipients.remove(recipient);
      return recipients.size() == 0;
    }
  }

  private static HashMap<String, RecipientListenerPair> sMessageSendListeners =
          new HashMap<String, RecipientListenerPair>();

  static void handleServerAck(MMXid recipient, String messageId) {
    synchronized (sMessageSendListeners) {
      RecipientListenerPair listenerPair = sMessageSendListeners.get(messageId);
      if (listenerPair != null) {
        if (listenerPair.onAckOrErrorReceived(recipient.getUserId())) {
          sMessageSendListeners.remove(messageId);
          // If there are any invalid recipients, onFailure will be invoke for backward
          // compatibility.  Otherwise, we have to introduce a new method like
          // "onResult(String messageId, Set<MMXUser> invalidRecipients)".
          if (listenerPair.exception != null)
            listenerPair.listener.onFailure(FailureCode.INVALID_RECIPIENT, listenerPair.exception);
          else
            listenerPair.listener.onSuccess(messageId);
        }
      }
    }
  }
  
  static void handleMessageSendError(MMXid recipient, String messageId, 
                            MMXMessage.FailureCode code, Throwable throwable) {
    synchronized (sMessageSendListeners) {
      RecipientListenerPair listenerPair = sMessageSendListeners.get(messageId);
      if (listenerPair != null) {
        if (code.equals(FailureCode.INVALID_RECIPIENT) && recipient != null) {
          // Save all invalid recipients into a single exception.
          if (listenerPair.exception == null) {
            listenerPair.exception = new MMXUser.InvalidUserException(
                "Invalid recipient", messageId);
          }
          ((MMXUser.InvalidUserException) listenerPair.exception).addUser(
              new MMXUser.Builder().username(recipient.getUserId()).build());
          throwable = listenerPair.exception;
        }
        if (recipient == null || listenerPair.onAckOrErrorReceived(recipient.getUserId())) {
          sMessageSendListeners.remove(messageId);
          // It is possible that throwable is null.
          listenerPair.listener.onFailure(code, throwable);
        }
      }
    }
  }
}
