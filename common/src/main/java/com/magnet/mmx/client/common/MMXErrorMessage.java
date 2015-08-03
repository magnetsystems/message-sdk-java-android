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

import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.XMPPError;

import com.magnet.mmx.protocol.MMXError;

/**
 * An error message with MMXError payload or custom error
 * paylaod.
 */
public class MMXErrorMessage extends MMXMessage {
  private static final long serialVersionUID = 1022569846183355789L;
  private XMPPError mXmppError;
  private MMXError mMMXError;
  private MMXPayload mCustomError;

  MMXErrorMessage(Message msg) {
    super(msg);
    mXmppError = msg.getError();
    MMXPayload payload = getPayload();
    if ((mXmppError == null) && (payload != null)) {
      if (MMXError.getType().equals(payload.getType())) {
        mMMXError = MMXError.fromJson(payload.getDataAsText().toString());
      } else {
        mCustomError = payload;
      }
    }
  }

  /**
   * Check if the error payload is MMXError.
   * @return
   */
  public boolean isMMXError() {
    return mMMXError != null;
  }

  /**
   * @hide
   * Check if the error payload is XMPPError.
   * @return
   */
  public boolean isXMPPError() {
    return mXmppError != null;
  }

  /**
   * Check if the error payload is a custom payload wrapped inside MMXPayload.
   * @return
   */
  public boolean isCustomError() {
    return mCustomError != null;
  }

  /**
   * The MMX error associated with this message
   * @return
   */
  public MMXError getMMXError() {
    return mMMXError;
  }

  /**
   * @hide
   * The XMPP error associated with this message
   * @return
   */
  public XMPPError getXMPPError() {
    return mXmppError;
  }

  /**
   * The custom error associated with this message
   * @return
   */
  public MMXPayload getCustomError() {
    return mCustomError;
  }

  @Override
  public String toString() {
    return "[ "+super.toString()+", MMXerr="+mMMXError+", XmppErr="+mXmppError
            +", CustErr="+mCustomError+" ]";
  }
}
