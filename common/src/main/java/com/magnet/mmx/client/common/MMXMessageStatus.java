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

import java.io.Serializable;

import com.magnet.mmx.protocol.Constants.MessageState;
import com.magnet.mmx.protocol.MsgsState.MessageStatus;
import com.magnet.mmx.util.XIDUtil;

/**
 * The status of a message.
 */
public class MMXMessageStatus implements Serializable {
  private static final long serialVersionUID = 8685690457023256301L;
  private MessageState mState;
  private MMXid mRecipient;

  MMXMessageStatus(MessageStatus status) {
    mState = status.getState();
    if (status.getRecipient() != null) {
      mRecipient = new MMXid(XIDUtil.getUserId(status.getRecipient()));
    }
  }

  /**
   * @hide
   * @param recipient
   * @param state
   */
  public MMXMessageStatus(MMXid recipient, MessageState state) {
    mState = state;
    mRecipient = recipient;
  }

  /**
   * Get the state of the message.
   *
   * <p/>The possible values for MessageState are:
   *
   * <p/>UNKNOWN -- The message is in an unknown state.
   * <p/>CLIENT_PENDING -- client-only: the message has not been communicated MMX and can be cancelled.
   * <p/>PENDING -- Every message starts in this state
   * <p/>WAKEUP_REQUIRED -- Recipient is offline and hence we need to send a wake-up notification
   * <p/>WAKEUP_TIMEDOUT -- Message wake up has been timed out
   * <p/>WAKEUP_SENT -- We are waiting for recipient to wake up
   * <p/>DELIVERY_ATTEMPTED -- Recipient is online and hence we transitioned to this state
   * <p/>DELIVERED -- delivered to the endpoint
   * <p/>RECEIVED -- Message has been processed by the endpoint
   * <p/>TIMEDOUT -- Timeout experienced by server when attempting delivery
   *
   * @return the message state
   */
  public MessageState getState() {
    return mState;
  }

  /**
   * Get the identifier of the message recipient.
   * @return The MMX user identifier or MMX end-point identifier.
   */
  public MMXid getRecipient() {
    return mRecipient;
  }

  /**
   * Return a debug information.
   */
  @Override
  public String toString() {
    return "[state="+mState+", rcpt="+mRecipient+"]";
  }
}
