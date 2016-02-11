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

import android.os.Parcel;
import android.os.Parcelable;
import com.google.gson.reflect.TypeToken;
import com.magnet.max.android.Attachment;
import com.magnet.max.android.rest.marshalling.Iso8601DateConverter;
import com.magnet.max.android.util.EqualityUtil;
import com.magnet.max.android.util.HashCodeBuilder;
import com.magnet.max.android.util.MagnetUtils;
import com.magnet.max.android.util.ParcelableHelper;
import com.magnet.max.android.util.StringUtil;
import com.magnet.mmx.client.internal.channel.PubSubItem;
import com.magnet.mmx.util.GsonData;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.magnet.max.android.ApiCallback;
import com.magnet.max.android.User;
import com.magnet.mmx.client.MMXClient;
import com.magnet.mmx.client.MMXTask;
import com.magnet.mmx.client.common.Log;
import com.magnet.mmx.client.common.MMXException;
import com.magnet.mmx.client.common.MMXPayload;
import com.magnet.mmx.client.common.Options;
import com.magnet.mmx.protocol.MMXTopic;
import com.magnet.mmx.protocol.MMXid;
import com.magnet.mmx.protocol.StatusCode;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * This class holds the message payload, and operations for the message.  If
 * the message targets to the recipients, it will be used for ad hoc messaging.
 * If the message targets to a channel, it will be used for group chat or forum
 * discussions.
 */
public class MMXMessage implements Parcelable {
  private static final String TAG = MMXMessage.class.getSimpleName();

  public static final String CONTENT_ATTACHMENTS = "_attachments";

  /**
   * Failure codes for the MMXMessage class.
   */
  public static class FailureCode extends MMX.FailureCode {
    public static final FailureCode INVALID_RECIPIENT = new FailureCode(404, "INVALID_RECIPIENT");
    public static final FailureCode CONTENT_TOO_LARGE = new FailureCode(413, "CONTENT_TOO_LARGE");
    public static final FailureCode NO_RECEIPT_ID = new FailureCode(430, "NO_RECEIPT_ID");

    FailureCode(int value, String description) {
      super(value, description);
    }

    FailureCode(MMX.FailureCode code) { super(code); }

    static FailureCode fromMMXFailureCode(MMX.FailureCode code, Throwable throwable) {
      if (throwable != null) {
        Log.d(TAG, "fromMMXFailureCode() ex="+throwable.getClass().getName());
      } else {
        Log.d(TAG, "fromMMXFailureCode() ex=null");
      }
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
   *
   *
   * @param <T> The type of the onSuccess result
   */
  public static abstract class OnFinishedListener<T> implements IOnFinishedListener<T, FailureCode> {
    /**
     * Called when the operation completes successfully
     *
     * @param result the result of the operation
     */
    @Override
    public abstract void onSuccess(T result);

    /**
     * Called if the operation fails
     *
     * @param code the failure code
     * @param throwable the throwable associated with this failure (may be null)
     */
    @Override
    public abstract void onFailure(FailureCode code, Throwable throwable);
  }
  /**
   * The builder for the MMXMessage class
   */
  public static final class Builder {
    private final MMXMessage mMessage;

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
    MMXMessage.Builder sender(User sender) {
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
    public MMXMessage.Builder recipients(Set<User> recipients) {
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
     * Adds attachments to the message
     * @param attachments
     * @return
     */
    public MMXMessage.Builder attachments(Attachment... attachments) {
      if(null != attachments && attachments.length > 0) {
        for (Attachment attachment : attachments) {
          mMessage.mAttachments.add(attachment);
        }
      }
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
      //if (mMessage.mChannel == null && mMessage.mRecipients.size() == 0) {
      //  throw new IllegalArgumentException("No channel and no recipients are specified");
      //}
      if (mMessage.mChannel != null && mMessage.mRecipients.size() > 0) {
        throw new IllegalArgumentException("Only either channel or recipients should be specified");
      }
      return mMessage;
    }
  }

  /**
   * The exception contains a list of recipient user ID's that a message
   * cannot be sent to.
   */
  public static class InvalidRecipientException extends MMXException {
    private final String mMsgId;
    private final Set<String> mUserIds = new HashSet<String>();

    public InvalidRecipientException(String msg, String messageId) {
      super(msg, StatusCode.NOT_FOUND);
      mMsgId = messageId;
    }

    public String getMessageId() {
      return mMsgId;
    }

    private void addUserId(String userId) {
      mUserIds.add(userId);
    }

    public Set<String> getUserIds() {
      return mUserIds;
    }

    @Override
    public String toString() {
      return super.toString()+", msgId="+mMsgId+", uids="+mUserIds;
    }
  }

  private String mId;
  private String mType;
  private Date mTimestamp;
  private User mSender;
  private MMXChannel mChannel;
  private Set<User> mRecipients = new HashSet<User>();
  private Map<String, String> mContent = new HashMap<String, String>();
  private String mReceiptId;
  private List<Attachment> mAttachments = new ArrayList<Attachment>();

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
  MMXMessage sender(User sender) {
    mSender = sender;
    return this;
  }

  /**
   * The sender of this MMXMessage.
   * NOTE:  This is for incoming messages only.
   *
   * @return the sender
   */
  public User getSender() {
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
  MMXMessage recipients(Set<User> recipients) {
    mRecipients = recipients;
    return this;
  }

  /**
   * The recipients for this message
   *
   * @return the recipients
   */
  public Set<User> getRecipients() {
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
   * The attachments of this message
   * @return attachments
   */
  public List<Attachment> getAttachments() {
    return mAttachments;
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
        MMX.getCallbackHandler().post(new Runnable() {
          @Override
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
        Throwable uploadError = uploadAttachments(payload, generatedMessageId, null);
        if(null != uploadError) {
          throw new IllegalStateException("Failed to upload attachment for message " + generatedMessageId);
        }

        String publishedId = mmxClient.getPubSubManager().publish(
            generatedMessageId, mChannel.getMMXTopic(), payload);
        if (!generatedMessageId.equals(publishedId)) {
          throw new RuntimeException(
              "SDK Error: The returned published message id does not match the generated message id.");
        }
        return publishedId;
      }

      @Override
      public void onException(final Throwable exception) {
        if (listener != null) {
          MMX.getCallbackHandler().post(new Runnable() {
            @Override
            public void run() {
              listener.onFailure(MMXChannel.FailureCode.fromMMXFailureCode(
                      MMXChannel.FailureCode.DEVICE_ERROR, exception), exception);
            }
          });
        }
      }

      @Override
      public void onResult(final String result) {
        if (listener != null) {
          MMX.getCallbackHandler().post(new Runnable() {
            @Override
            public void run() {
              listener.onSuccess(result);
            }
          });
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
   * from {@link User#getUsersByUserNames(List, ApiCallback)}. If this message is
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
        MMX.getCallbackHandler().post(new Runnable() {
          @Override
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
          Throwable uploadError = uploadAttachments(payload, generatedMessageId, null);
          if(null != uploadError) {
            throw new IllegalStateException("Failed to upload attachment for message " + generatedMessageId);
          }

          String publishedId = mmxClient.getPubSubManager().publish(
              generatedMessageId, mChannel.getMMXTopic(), payload);
          if (!generatedMessageId.equals(publishedId)) {
            throw new RuntimeException(
                "SDK Error: The returned published message id does not match the generated message id.");
          }
          return publishedId;
        }

        @Override
        public void onException(final Throwable exception) {
          if (listener != null) {
            MMX.getCallbackHandler().post(new Runnable() {
              @Override
              public void run() {
                listener.onFailure(FailureCode.fromMMXFailureCode(
                        FailureCode.DEVICE_ERROR, exception), exception);
              }
            });
          }
        }

        @Override
        public void onResult(final String result) {
          if (listener != null) {
            MMX.getCallbackHandler().post(new Runnable() {
              @Override
              public void run() {
                listener.onSuccess(result);
              }
            });
          }
        }
      };
    } else {
      if(mRecipients.size() == 0) {
        throw new IllegalArgumentException("Recipients is not specified");
      }

      task = new MMXTask<String>(MMX.getMMXClient(), MMX.getHandler()) {
        @Override
        public String doRun(MMXClient mmxClient) throws Throwable {
          Throwable uploadError = uploadAttachments(payload, generatedMessageId, null);
          if(null != uploadError) {
            throw new IllegalStateException("Failed to upload attachment for message " + generatedMessageId);
          }

          MMXid[] recipientsArray = new MMXid[mRecipients.size()];
          int index = 0;
          for (User recipient : mRecipients) {
            recipientsArray[index++] = new MMXid(recipient.getUserIdentifier(), null, recipient.getUserName());
          }
          if (listener != null) {
            synchronized (sMessageSendListeners) {
              sMessageSendListeners.put(generatedMessageId,
                  new MessageListenerPair(listener, MMXMessage.this));
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
        public void onException(final Throwable exception) {
          if (listener != null) {
            MMX.getCallbackHandler().post(new Runnable() {
              @Override
              public void run() {
                listener.onFailure(FailureCode.fromMMXFailureCode(FailureCode.DEVICE_ERROR, exception), exception);
              }
            });
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

  @Override public int hashCode() {
    return new HashCodeBuilder().hash(mId).hash(mType).hash(mSender).hash(mRecipients)
        .hash(mChannel).hash(mContent).hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (!EqualityUtil.quickCheck(this, obj)) {
      return false;
    }

    MMXMessage theOther = (MMXMessage) obj;

    return StringUtil.isStringValueEqual(mId, theOther.getId()) &&
        StringUtil.isStringValueEqual(mType, theOther.getType()) &&
        null != mChannel ? mChannel.equals(theOther.getChannel()) : null == theOther.getChannel() &&
        null != mSender ? mSender.equals(theOther.getSender()) : null == theOther.getSender() &&
        null != mRecipients ? mRecipients.equals(theOther.getRecipients()) : null == theOther.getRecipients() &&
        null != mContent ? mContent.equals(theOther.getContent()) : null == theOther.getContent();
  }

  @Override
  public String toString() {
    return new StringBuilder().append("{")
        .append("id = ").append(mId).append(", ")
        .append("type = ").append(mType).append(", ")
        .append("sender = ").append(mSender).append(", ")
        .append("channel = ").append(mChannel).append(", ")
        .append("recipients = ").append(StringUtil.toString(mRecipients)).append(", ")
        .append("content = ").append(StringUtil.toString(mContent))
        .append("}")
        .toString();
  }

  /**
   * Build a reply message.
   *
   * @param isReplyAll reply to other recipients in addition to the sender
   * @return a new MMXMessage instance for the reply
   */
  private MMXMessage buildReply(Map<String, String> content, boolean isReplyAll) {
    User me = MMX.getCurrentUser();
    HashSet<User> replyRecipients = new HashSet<User>();
    replyRecipients.add(mSender);
    if (isReplyAll) {
      for (User recipient : mRecipients) {
        if (!recipient.getUserIdentifier().equals(me.getUserIdentifier())) {
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
      //throw new IllegalArgumentException("Cannot acknowledge() this message: " + mId);
      if(null != listener) {
        listener.onFailure(FailureCode.NO_RECEIPT_ID, new IllegalArgumentException("Cannot acknowledge() this message: " + mId));
      }

      return;
    }

    MMXTask<Void> task = new MMXTask<Void>(MMX.getMMXClient(), MMX.getHandler()) {
      @Override
      public Void doRun(MMXClient mmxClient) throws Throwable {
        MMX.getMMXClient().getMessageManager().sendReceipt(mReceiptId);
        return null;
      }

      @Override
      public void onException(final Throwable exception) {
        if (listener != null) {
          MMX.getCallbackHandler().post(new Runnable() {
            @Override
            public void run() {
              listener.onFailure(FailureCode.fromMMXFailureCode(FailureCode.DEVICE_ERROR, exception), exception);
            }
          });
        }
      }

      @Override
      public void onResult(Void result) {
        if (listener != null) {
          MMX.getCallbackHandler().post(new Runnable() {
            @Override
            public void run() {
              listener.onSuccess(null);
            }
          });
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
    UserCache userCache = UserCache.getInstance();
    HashSet<String> usersToRetrieve = new HashSet<String>();
    usersToRetrieve.add(message.getFrom().getUserId());
    MMXid toUserId = message.getTo();
    if (toUserId != null) {
      usersToRetrieve.add(message.getTo().getUserId());
    }

    //identify all the users that need to be retrieved and populate the cache
    MMXid[] otherRecipients = message.getReplyAll();
    if (otherRecipients != null) {
      //this is normal message.  getReplyAll() returns null for pubsub messages
      for (MMXid mmxId : otherRecipients) {
        Log.d(TAG, "------otherRecipients : " + mmxId.getUserId());
        usersToRetrieve.add(mmxId.getUserId());
      }
    }

    //fill the cache
    userCache.fillCacheByUserId(usersToRetrieve, UserCache.DEFAULT_ACCEPTED_AGE); //five minutes old is ok

    HashSet<User> recipients = new HashSet<User>();
    //populate the values
    User receiver;
    if (null == topic && toUserId != null) {
      receiver = userCache.getByUserId(message.getTo().getUserId());
      if (receiver == null) {
        Log.e(TAG, "fromMMXMessage(): FAILURE: Unable to retrieve receiver from cache:  " +
                "receiver=" + receiver + ".  Message will be dropped.");
        return null;
      }
      Log.d(TAG, "------receiver : " + receiver.getUserIdentifier());
      recipients.add(receiver);
    }
    User sender = userCache.getByUserId(message.getFrom().getUserId());
    if (sender == null) {
      Log.e(TAG, "fromMMXMessage(): FAILURE: Unable to retrieve sender from cache:  " +
              "sender=" + sender + ".  Message will be dropped.");
      return null;
    }

    if (otherRecipients != null) {
      for (MMXid otherRecipient : otherRecipients) {
        recipients.add(userCache.getByUserId(otherRecipient.getUserId()));
      }
    }

    //populate the message content
    HashMap<String, String> content = new HashMap<String, String>();
    for (Map.Entry<String,String> entry : message.getPayload().getAllMetaData().entrySet()) {
      if(!CONTENT_ATTACHMENTS.equals(entry.getKey())) {
        content.put(entry.getKey(), entry.getValue());
      }
    }

    MMXMessage.Builder newMessage = new MMXMessage.Builder();

    // Extract attachments
    String attachmentsStr = MagnetUtils.trimQuotes(message.getPayload().getAllMetaData().get(CONTENT_ATTACHMENTS));
    if(StringUtil.isNotEmpty(attachmentsStr)) {
      List<Attachment> attachments = GsonData.getGson().fromJson(attachmentsStr, new TypeToken<List<Attachment>>() {}.getType());
      if(null != attachments && attachments.size() > 0) {
        newMessage.attachments(attachments.toArray(new Attachment[0]));
      }
    }
    Log.d(TAG, "-----------message conversion, topic : " + topic + ", message : " + message);
    if(null != topic) {
      Log.d(TAG, "It's a channel message");
      newMessage.channel(MMXChannel.fromMMXTopic(topic));
    } else if(recipients.size() > 0){
      Log.d(TAG, "It's a in-app message");
      newMessage.recipients(recipients);
    } else {
      throw new IllegalArgumentException("Neither recipients nor channel is set in message.");
    }
    return newMessage
            .sender(sender).id(message.getId())
            .timestamp(message.getPayload().getSentTime()).content(content)
            .build();
  }

  static MMXMessage fromPubSubItem(PubSubItem pubSubItem) {
    UserCache userCache = UserCache.getInstance();
    HashSet<String> usersToRetrieve = new HashSet<String>();
    usersToRetrieve.add(pubSubItem.getPublisher().getUserId());

    //fill the cache
    userCache.fillCacheByUserId(usersToRetrieve, UserCache.DEFAULT_ACCEPTED_AGE); //five minutes old is ok

    //populate the values
    User sender = userCache.getByUserId(pubSubItem.getPublisher().getUserId());
    if (sender == null) {
      Log.e(TAG, "fromMMXMessage(): FAILURE: Unable to retrieve sender from cache:  " +
          "sender=" + sender + ".  Message will be dropped.");
      return null;
    }

    //populate the message content
    HashMap<String, String> content = new HashMap<String, String>();
    for (Map.Entry<String,String> entry : pubSubItem.getContent().entrySet()) {
      if(!CONTENT_ATTACHMENTS.equals(entry.getKey())) {
        content.put(entry.getKey(), entry.getValue());
      }
    }

    MMXMessage.Builder newMessage = new MMXMessage.Builder();

    // Extract attachments
    String attachmentsStr = MagnetUtils.trimQuotes(pubSubItem.getContent().get(CONTENT_ATTACHMENTS));
    if(StringUtil.isNotEmpty(attachmentsStr)) {
      List<Attachment> attachments = GsonData.getGson().fromJson(attachmentsStr, new TypeToken<List<Attachment>>() {}.getType());
      if(null != attachments && attachments.size() > 0) {
        newMessage.attachments(attachments.toArray(new Attachment[0]));
      }
    }

    return newMessage
        .sender(sender).id(pubSubItem.getItemId())
        .timestamp(Iso8601DateConverter.fromString(pubSubItem.getMetaData().get("creationDate"))).content(content)
        .build();
  }

  //For handling the onSuccess of send() messages when server ack is received
  private class MessageListenerPair {
    private final OnFinishedListener<String> listener;
    private final MMXMessage message;

    private MessageListenerPair(OnFinishedListener<String> listener, MMXMessage message) {
      this.listener = listener;
      this.message = message;
    }
  }

  private static HashMap<String, MessageListenerPair> sMessageSendListeners =
          new HashMap<String, MessageListenerPair>();

  static void handleMessageSubmitted(String messageId) {
//    synchronized (sMessageSendListeners) {
//      MessageListenerPair listenerPair = sMessageSendListeners.get(messageId);
//      if (listenerPair != null) {
//        listenerPair.listener.onSuccess(messageId);
//      }
//    }
  }

  static void handleMessageAccepted(List<MMXid> invalidRecipients, final String messageId) {
    Log.d(TAG, "handleMessageAccepted() invalid="+invalidRecipients+" msgId="+messageId);
    synchronized (sMessageSendListeners) {
      final MessageListenerPair listenerPair = sMessageSendListeners.remove(messageId);
      if (listenerPair != null) {
        if (invalidRecipients == null || invalidRecipients.isEmpty()) {
          MMX.getCallbackHandler().post(new Runnable() {
            @Override
            public void run() {
              listenerPair.listener.onSuccess(messageId);
            }
          });
        } else {
          final InvalidRecipientException ex = new InvalidRecipientException(
              "Invalid recipients", messageId);
          for (MMXid xid : invalidRecipients) {
            ex.addUserId(xid.getUserId());
          }
          MMX.getCallbackHandler().post(new Runnable() {
            @Override
            public void run() {
              listenerPair.listener.onFailure(MMXMessage.FailureCode.INVALID_RECIPIENT, ex);
            }
          });
        }
      }
    }
  }

  static MMXMessage getSendingMessage(String messageId) {
    synchronized (sMessageSendListeners) {
      MessageListenerPair listenerPair = sMessageSendListeners.get(messageId);
      if (listenerPair == null) {
        return null;
      }
      return listenerPair.message;
    }
  }

  private Throwable uploadAttachments(MMXPayload payload, final String messageId,
      final Attachment.UploadListener progressListener) {
    for(final Attachment attachment : mAttachments) {
      if(Attachment.Status.COMPLETE == attachment.getStatus()
          && StringUtil.isNotEmpty(attachment.getAttachmentId())) {
        Log.d(TAG, "Attahcment " + attachment.getName() + " is already uploaded");
        if(null != progressListener) {
          progressListener.onComplete(attachment);
        }
        continue;
      }

      //Set meta data
      attachment.addMetaData("metadata_message_id", messageId);
      if(null != mChannel) {
        attachment.addMetaData("metadata_channel_name", mChannel.getName());
        attachment.addMetaData("metadata_channel_is_public", String.valueOf(mChannel.isPublic()));
      } else {
        StringBuilder sb = new StringBuilder();
        int count = 0;
        for(User u : mRecipients) {
          sb.append(u.getUserIdentifier());
          if(count++ != mRecipients.size() - 1) {
            sb.append(",");
          }
        }
        attachment.addMetaData("metadata_recipients", sb.toString());
      }

      final CountDownLatch uploadSignal = new CountDownLatch(1);
      final AtomicReference<Throwable> uploadError = new AtomicReference<Throwable>();
      attachment.upload(new Attachment.UploadListener() {
        @Override public void onStart(Attachment attachment) {
          if(null != progressListener) {
            progressListener.onStart(attachment);
          }
        }

        @Override public void onComplete(Attachment attachment) {
          if(null != progressListener) {
            progressListener.onComplete(attachment);
          }
          uploadSignal.countDown();
        }

        @Override public void onError(Attachment attachment, Throwable throwable) {
          uploadError.set(throwable);

          if(null != progressListener) {
            progressListener.onError(attachment, throwable);
          }

          uploadSignal.countDown();
        }
      });

      try {
        uploadSignal.await(60, TimeUnit.SECONDS);
      } catch (InterruptedException e) {
        uploadError.set(e);

        if(null != progressListener) {
          progressListener.onError(attachment, new Exception("Timeout when uploading attachment " + attachment.getName()));
        }
      }

      if(null != uploadError.get()) {
        return uploadError.get();
      }

      if(Attachment.Status.COMPLETE == attachment.getStatus()
          && StringUtil.isNotEmpty(attachment.getAttachmentId())) {
        if(null != progressListener) {
          progressListener.onComplete(attachment);
        }
        Log.d(TAG, "Attachment " + attachment.getName() + " is uploaded successfully.");
      } else {
        String message = "Failed to upload attachment " + attachment.getName();
        Log.d(TAG, message);
        if(uploadSignal.getCount() > 0) {
          if(null != progressListener) {
            progressListener.onError(attachment, new Exception(message));
          }
        }
      }
    }

    payload.setMetaData(CONTENT_ATTACHMENTS, GsonData.getGson().toJson(mAttachments));

    return null;
  }

  //----------------Parcelable Methods----------------

  @Override public int describeContents() {
    return 0;
  }

  @Override public void writeToParcel(Parcel dest, int flags) {
    dest.writeString(this.mId);
    dest.writeString(this.mType);
    dest.writeLong(mTimestamp != null ? mTimestamp.getTime() : -1);
    dest.writeParcelable(this.mSender, flags);
    dest.writeParcelable(this.mChannel, 0);
    dest.writeParcelableArray(ParcelableHelper.setToArray(this.mRecipients), flags);
    dest.writeBundle(ParcelableHelper.stringMapToBundle(this.mContent));
    dest.writeString(this.mReceiptId);
    dest.writeList(this.mAttachments);
  }

  protected MMXMessage(Parcel in) {
    this.mId = in.readString();
    this.mType = in.readString();
    long tmpMTimestamp = in.readLong();
    this.mTimestamp = tmpMTimestamp == -1 ? null : new Date(tmpMTimestamp);
    this.mSender = in.readParcelable(User.class.getClassLoader());
    this.mChannel = in.readParcelable(MMXChannel.class.getClassLoader());
    User[] tmpRecipients = (User[]) in.readParcelableArray(User.class.getClassLoader());
    if(null != tmpRecipients) {
      this.mRecipients = new HashSet<User>(Arrays.asList(tmpRecipients));
    }
    this.mContent = ParcelableHelper.stringMapfromBundle(in.readBundle());
    this.mReceiptId = in.readString();
    this.mAttachments = new ArrayList<Attachment>();
    in.readTypedList(this.mAttachments, Attachment.CREATOR);
  }

  public static final Parcelable.Creator<MMXMessage> CREATOR =
      new Parcelable.Creator<MMXMessage>() {
        @Override
        public MMXMessage createFromParcel(Parcel source) {
          return new MMXMessage(source);
        }

        @Override
        public MMXMessage[] newArray(int size) {
          return new MMXMessage[size];
        }
      };
}
