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

//import com.magnet.mmx.protocol.DeliveryPolicy;
//import com.magnet.mmx.protocol.RetryPolicy;

/**
 * Various options for message delivery.  The sender can specify if the
 * delivery receipt should be included, or the message can be dropped if the
 * recipient is offline.
 */
public class Options implements Serializable {
  private static final long serialVersionUID = -2045835234989471872L;
//  private StateListener mStateListener;
//  private RetryPolicy mSendPolicy;
//  private DeliveryPolicy mDeliveryPolicy;
  private boolean mDroppable;
  private boolean mRequestReceipt;

  /**
   * @hide
   * Allow a message to be dropped if the recipient is not online. It also
   * disables the reliable message delivery.  Default is reliable using store
   * and forward mechanism.
   * @param droppable
   *          true to drop the message if the recipient is offline; otherwise,
   *          false.
   * @return This object.
   */
  public Options setDroppable(boolean droppable) {
    mDroppable = droppable;
    return this;
  }

  /**
   * @hide
   * Check if the message can be dropped if the recipient is not online.
   * @return true if the message can be dropped; otherwise, false.
   */
  public boolean isDroppable() {
    return mDroppable;
  }

  /**
   * Enable or disable delivery receipt.  The default is disable.
   * @param requestReceipt true to enable delivery receipt.
   * @return This object.
   */
  public Options enableReceipt(boolean requestReceipt) {
    mRequestReceipt = requestReceipt;
    return this;
  }

  /**
   * Check if delivery receipt is enabled for the outgoing message.
   * @return true if delivery receipt is enabled; otherwise, false.
   */
  public boolean isReceiptEnabled() {
    return mRequestReceipt;
  }

//  /**
//   * Set the notification of state changed via out of process mechanism.  This
//   * method is not available to J2SE environment, but it can be implemented
//   * via Android Intent.
//   * @param action
//   * @param extras
//   * @return
//   */
//  public Options setNotification(String action, Serializable extras) {
//    throw new UnsupportedOperationException();
//  }

//  /**
//   * Use the in-process listener for the state changed.  This feature is
//   * dropped because it is too expensive.
//   * @param listener
//   * @return
//   */
//  public Options setStateListener(StateListener listener) {
//    mStateListener = listener;
//    return this;
//  }

//  /**
//   * Specify the retry policy for sending message.  This policy is only used
//   * by the sender.
//   * @param policy
//   * @return
//   */
//  public Options setRetryPolicy(RetryPolicy policy) {
//    mSendPolicy = policy;
//    return this;
//  }

//  /**
//   * Specify the retry policy for delivering a reliable message from MMX server.
//   * @param policy
//   * @return
//   */
//  public Options setDeliveryPolicy(DeliveryPolicy policy) {
//    mDeliveryPolicy = policy;
//    return this;
//  }
}
