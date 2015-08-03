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
 * A listener for message states and various messages.
 */
public interface MMXMessageListener {
  /**
   * A complete message (even it is empty) received.  This method will not be
   * invoked if the message only contains a delivery receipt.
   * @param message A complete message received.
   * @param receiptId A delivery receipt ID, or null for no delivery receipt request.
   * @see MessageManager#sendDeliveryReceipt(String)
   */
  public void onMessageReceived( MMXMessage message, String receiptId );
  /**
   * A callback when a message is sent successfully.
   * @param msgId The message ID.
   */
  public void onMessageSent( String msgId );
  /**
   * A callback when sending a message failed due to network issue.  Typically
   * it will be followed by the connection close callback.
   * @param msgId The message ID.
   */
  public void onMessageFailed( String msgId );
  /**
   * A callback when a delivery receipt is returned.
   * @param recipient The XID of the sender of the delivery receipt.
   * @param msgId The originating message ID.
   * @see Options#enableReceipt(boolean)
   * @see MessageManager#ackPayload(String, String)
   * @see com.magnet.mmx.util.XIDUtil
   */
  public void onMessageDelivered( MMXid recipient, String msgId );
  /**
   * A callback when an invitation is received for group messaging.  Currently
   * it is not implemented.
   * @param invitation An invitation message.
   */
  public void onInvitationReceived( Invitation invitation );
  /**
   * The presence of a peer user is changed.
   * @param presence
   */
//  public void onPresenceChanged( Presence presence );

  /**
   * A callback in the app server which MMX delegates a user authentication to.
   * Currently it is not implemented.
   * @param auth Authentication data.
   */
  public void onAuthReceived( AuthData auth );

  /**
   * A callback when a published item is received.  The message ID
   * {@link MMXMessage#getId()} in the <code>msg</code> is the published
   * item ID.  The publisher info is not available.
   * @param msg The published item.
   * @param topic The topic which the item is published to.
   */
  public void onItemReceived(MMXMessage msg, MMXTopic topic);

  /**
   * A callback when an error message is received.  The message payload is
   * MMXError, XMPPError or a custom payload.
   * @param message An error message.
   */
  public void onErrorMessageReceived( MMXErrorMessage message );
}
