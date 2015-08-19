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

import com.magnet.mmx.client.common.GlobalAddress;
import com.magnet.mmx.client.common.Log;
import com.magnet.mmx.client.common.MMXException;
import com.magnet.mmx.client.common.MMXMessageStatus;
import com.magnet.mmx.client.common.MMXPayload;
import com.magnet.mmx.client.common.MMXid;
import com.magnet.mmx.client.common.MessageManager;
import com.magnet.mmx.client.common.Options;
import com.magnet.mmx.protocol.Constants;
import com.magnet.mmx.util.MMXQueue;
import com.magnet.mmx.util.MMXQueue.Item;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This manager handles the sending and cancelling of messages, including
 * the ability to query the state of a previously sent message.
 */
public final class MMXMessageManager extends MMXManager {
  private static final String TAG = MMXMessageManager.class.getSimpleName();
  private MessageManager mMessageManager = null;

  MMXMessageManager(MMXClient mmxClient, Handler handler) {
    super(mmxClient, handler);
    onConnectionChanged();
  }

  /**
   * Convenience method to send a simple text message to a single recipient.
   *
   * Sends a payload using the messaging system.  If the MMXClient is not connected,
   * the sending of this payload will be queued and sent upon the next successful connection.
   * If the message could not be queued, this method will return null.  Otherwise, it returns a
   * generated ID for this message to be later used when handling delivery
   * receipts (if enabled.)

   * @param recipient the target user's identifier
   * @param message the simple text message
   * @param options any options to specify for this message
   * @return the generated message id of the resulting message
   * @throws MMXException Empty recipient or invalid payload size
   */
  public String sendText(final MMXid recipient, final String message,
                          final Options options) throws MMXException {
    return sendPayload(new MMXid[] {recipient}, new MMXPayload(message),
                        options);
  }

  /**
   * Convenience method to send a text message with a type to multiple
   * recipients.
   *
   * @param recipients the target user's identifier
   * @param message the text message
   * @param options any options to specify for this message
   * @return the generated message id of the resulting message
   * @throws MMXException Empty recipients or invalid payload size
   */
  String sendText(final MMXid[] recipients, final String message, final Options options)
                      throws MMXException {
    return sendPayload(recipients, new MMXPayload(message), options);
  }

  /**
   * Sends a payload using the messaging system.  If the MMXClient is not connected,
   * the sending of this payload will be queued and sent upon the next successful connection.
   * If the message could not be queued, this method will return null.  Otherwise, it returns a
   * generated ID for this message to be later used when handling delivery
   * receipts (if enabled.)
   *
   * @param recipient A recipient
   * @param payload an application payload
   * @param options the send options for this message
   * @return the generated message id of the resulting message
   * @throws MMXException Empty recipient or invalid payload size
   */
  public String sendPayload(final MMXid recipient, final MMXPayload payload, 
                             final Options options) throws MMXException {
    return sendPayload(new MMXid[] { recipient }, payload, options);
  }
  
  /**
   * Sends a payload using the messaging system.  If the MMXClient is not connected,
   * the sending of this payload will be queued and sent upon the next successful connection.
   * If the message could not be queued, this method will return null.  Otherwise, it returns a
   * generated ID for this message to be later used when handling delivery
   * receipts (if enabled.)
   *  
   * @param recipients multiple recipients
   * @param payload an application payload
   * @param options the send options for this message
   * @return the generated message id of the resulting message
   * @throws MMXException Empty recipients or invalid payload size
   */
  String sendPayload(final MMXid[] recipients, final MMXPayload payload, 
                     final Options options) throws MMXException {
    checkDestroyed();
    if (recipients == null || recipients.length == 0) {
      throw new MMXException("Recipient cannot be empty", MMXException.BAD_REQUEST);
    }
    mMessageManager.validatePayload(payload);
    
    MMXClient client = getMMXClient();
    String messageId = client.getMMXConnection().genId();
    if (client.isConnected()) {
      if (Log.isLoggable(TAG, Log.VERBOSE)) {
        Log.v(TAG, "sendPayload(): Connected.  Sending message: " + messageId);
      }
      sendPayload(messageId, recipients, payload, options);
    } else {
      //save it to be delivered later.
      if (Log.isLoggable(TAG, Log.VERBOSE)) {
        Log.v(TAG, "sendPayload(): NOT connected.  Storing message to be sent when connected: " + messageId);
      }
      MMXQueue.Item.Message item = new MMXQueue.Item.Message(messageId, payload);
      item.setDestination(GlobalAddress.convertDestination(GlobalAddress.Type.USER, recipients));
      item.setOptions(options);
      client.getQueue().addItem(item);
    }
    return messageId;
  }

  void sendPayload(final String messageId, final MMXid[] recipients,
                   final MMXPayload payload, final Options options) {
    checkDestroyed();
    getHandler().post(new Runnable() {
      public void run() {
        try {
          if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "sendMessage(): sending message with id=" + messageId);
          }
          String newMessageId = mMessageManager.sendPayload(messageId, recipients,
                  payload, options);
          if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "sendMessage():  Message sent: " + newMessageId);
          }
        } catch (MMXException e) {
          Log.e(TAG, "sendMessage(): Caught exception during send.", e);
        }
      }
    });
  }

  /**
   * Attempts to cancel a pending message with the specified id.  This is
   * client-only functionality and will only work for messages that have not
   * been sent.
   *
   * @param messageId The id of the message to cancel.  This is the value returned by sendMessage()
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
   * Returns the status of a single message.  If the message id does not exist,
   * the status will be Constants.MessageState.UNKNOWN.
   *
   * Messages are "pending" when sendText() or sendPayload is called while the
   * MMXClient is disconnected.  These messages will be sent upon the next successful
   * connection.
   *
   * @param messageId The id of the message.  This is the value returned by sendMessage()
   * @return The states for all recipients of the specified message id
   */
  public List<MMXMessageStatus> getMessageState(String messageId) {
    return getMessagesState(Arrays.asList(new String[] {messageId})).get(messageId);
  }

  /**
   * Get the states of multiple messages by their message ID's.  A message may
   * be locally queued, in-transmit, or delivered.  If the message id does not
   * exist, the status will be Constants.MessageState.UNKNOWN.
   *
   * Messages are "pending" when sendText() or sendPayload is called while the
   * MMXClient is disconnected.  These messages will be sent upon the next successful
   * connection.
   *
   * @param messageIds An array list of message ID's.
   * @return A Map of message ID and list of message states for all recipients
   */
  public Map<String, List<MMXMessageStatus>> getMessagesState(
                                                    List<String> messageIds) {
    checkDestroyed();
    Map<String, List<MMXMessageStatus>> resultMap = new HashMap<String, List<MMXMessageStatus>>();
    if (!getMMXClient().isConnected()) {
      if (Log.isLoggable(TAG, Log.DEBUG)) {
        Log.d(TAG, "getMessageStates():  disconnected.  Only checking local messages");
      }
    } else {
      if (Log.isLoggable(TAG, Log.DEBUG)) {
        Log.d(TAG, "getMessageStates():  Connected so checking with server.");
      }
      try {
        resultMap = mMessageManager.getMessagesState(messageIds);
      } catch (MMXException e) {
        Log.e(TAG,
            "getMessageStates(): Caught exception while trying to receive message states.  Returning null.",
            e);
        // Return an empty set if there is an internal error or I/O error.
        return resultMap;
      }
    }

    // add the client-pending messages; discard the payload to avoid blowing up
    // the heap space.
    Map<String, MMXQueue.Item> pendingMessages = getMMXClient().getQueue()
        .getPendingItems(Item.Type.MESSAGE, true);
    if (Log.isLoggable(TAG, Log.DEBUG)) {
      Log.d(TAG, "getMessageStates(): found " + pendingMessages.size() + " CLIENT_PENDING messages");
    }
    for (String messageId : messageIds) {
      List<MMXMessageStatus> list = resultMap.get(messageId);
      if (list == null) {
        list = new ArrayList<MMXMessageStatus>();
        resultMap.put(messageId, list);
      }
      Item.Message msg = (Item.Message) pendingMessages.get(messageId);
      if (msg == null) {
        list.add(new MMXMessageStatus(null, Constants.MessageState.UNKNOWN));
      } else {
        list.addAll(convertToMsgStatList(msg.getDestination(),
                                      Constants.MessageState.CLIENT_PENDING));
      }
    }
    return resultMap;
  }
  
  private List<MMXMessageStatus> convertToMsgStatList(GlobalAddress[] addrs, 
                                                Constants.MessageState state) {
    ArrayList<MMXMessageStatus> list = new ArrayList<MMXMessageStatus>(addrs.length);
    for (GlobalAddress addr : addrs) {
      list.add(new MMXMessageStatus(addr.getXid(), state));
    }
    return list;
  }

  /**
   * Send a delivery receipt when a message sender requests for one. It will do nothing if the receiptId
   * is null.
   * @param receiptId The delivery receipt ID from {@link com.magnet.mmx.client.common.MMXMessage#getReceiptId()}
   */
  public void sendReceipt(final String receiptId) {
    checkDestroyed();
    if (receiptId == null || receiptId.length() == 0)
      return;

    getHandler().post(new Runnable() {
      public void run() {
        try {
          com.magnet.mmx.client.common.MessageManager.getInstance(
                  getMMXClient().getMMXConnection()).sendDeliveryReceipt(receiptId);
        } catch (MMXException e) {
          Log.e(TAG, "sendReceipt(): caught exception while acking message.", e);
        }
      }
    });
  }

  @Override
  void onConnectionChanged() {
    mMessageManager = MessageManager.getInstance(getMMXClient().getMMXConnection());
  }
}
