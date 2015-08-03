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

import com.magnet.mmx.protocol.AuthData;
import com.magnet.mmx.protocol.MMXTopic;

/**
 * Default message handler with no operations.  Developer should extend this
 * class and override the desired methods.
 *
 */
public class MMXMessageHandler implements MMXMessageListener {
  /**
   * A complete message (even it is empty) received.  This method will not be
   * invoked if the message only contains a delivery receipt.
   * @param message A complete message.
   * @param receiptId A delivery receipt ID, or null.
   * @see MessageManager#ackPayload(String, String)
   */
  @Override
  public void onMessageReceived(MMXMessage message, String receiptId) {
  }
  /**
   * An empty callback when a message is sent successfully.
   * @param msgId The message ID.
   */
  @Override
  public void onMessageSent(String msgId) {
  }
  /**
   * An empty callback when the message is failed sending.
   * @param msgId The message ID.
   */
  @Override
  public void onMessageFailed(String msgId) {
  }
  /**
   * An empty callback when a delivery receipt has been returned.
   * @param recipient The XID of the sender of the delivery receipt.
   * @param msgId The originating message ID.
   * @see Options#enableReceipt(boolean)
   * @see MessageManager#ackPayload(String, String)
   * @see com.magnet.mmx.util.XIDUtil
   */
  @Override
  public void onMessageDelivered(MMXid recipient, String msgId) {
  }
  /**
   * An empty callback when an invitation is received.
   * @param invitation
   */
  @Override
  public void onInvitationReceived(Invitation invitation) {
  }
  /**
   * A mobile client requests an app server to authenticate a user.
   * @param auth
   */
  @Override
  public void onAuthReceived(AuthData auth) {
  }
  /**
   * An empty callback when a published item is received.  The message ID
   * {@link MMXMessage#getId()} in the <code>msg</code> is the published
   * item ID.  The sender {@link MMXMessage#getFrom()} in the <code>msg</code>
   * has no meaningful information.
   * @param msg The published item.
   * @param topic The topic which the item is published to.
   */
  @Override
  public void onItemReceived(MMXMessage msg, MMXTopic topic) {
  }
  /**
   * An empty callback when an error message is received.
   * @param msg An error message.
   */
  @Override
  public void onErrorMessageReceived(MMXErrorMessage msg) {
  }
}
