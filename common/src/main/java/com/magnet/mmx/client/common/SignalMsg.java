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
import java.util.Map;

import com.magnet.mmx.protocol.Constants;
import com.magnet.mmx.protocol.MMXid;
import com.magnet.mmx.protocol.MmxHeaders;

/**
 * @hide
 * The payload of signal message received by the MMX server.
 */
public class SignalMsg implements Serializable {
  private static final String TAG = "SignalMsg";
  private static final long serialVersionUID = 6570068723698883699L;

  public static enum Type {
    ACK_ONCE(Constants.SERVER_ACK_KEY),
    ACK_BEGIN(Constants.BEGIN_ACK_KEY),
    ACK_END(Constants.END_ACK_KEY);

    private final String mValue;

    Type(String value) {
      mValue = value;
    }

    String getValue() {
      return mValue;
    }
  }

  private final static String ACK_MSG_ID = "ackForMsgId";
  private final static String SENDER = "sender";
  private final static String RECEIVER = "receiver";
  private final static String USER_ID = "userId";
  private final static String DEV_ID = "devId";

  private String mMsgId;
  private MMXid mSender;
  private MMXid mReceiver;
  private Type mType;

  /**
   * Get the originator of the message.
   * @return
   */
  public MMXid getSender() {
    return mSender;
  }

  /**
   * Get the target message receiver.
   * @return
   */
  public MMXid getReceiver() {
    return mReceiver;
  }

  /**
   * Get the message ID that this ack is for.
   * @return
   */
  public String getMsgId() {
    return mMsgId;
  }

  /**
   * Get the type of the signal.
   * @return
   */
  public Type getType() {
    return mType;
  }
  /**
   * Get the String representation for debug purpose.
   */
  @Override
  public String toString() {
    return "{msgid="+mMsgId+", sndr="+mSender+", rcvr="+mReceiver+", type="+mType+"}";
  }

  public static SignalMsg parse(MmxHeaders mmxMeta) {
    SignalMsg sigMsg = new SignalMsg();

    Map<String, Object> map = null;
    Type[] ackTypes = { Type.ACK_ONCE, Type.ACK_BEGIN, Type.ACK_END };
    for (Type type : ackTypes) {
      if ((map = (Map<String, Object>) mmxMeta.get(type.getValue())) != null) {
        sigMsg.mType = type;
        break;
      }
    }
    if (sigMsg.mType == null) {
      Log.w(TAG, "Cannot handle a non-ack signal message yet: map="+mmxMeta);
      return null;
    }
    Map<String, String> sender = (Map<String, String>) map.get(SENDER);
    Map<String, String> receiver = (Map<String, String>) map.get(RECEIVER);

    sigMsg.mMsgId = (String) map.get(ACK_MSG_ID);
    if (sender != null) {
      sigMsg.mSender = new MMXid(sender.get(USER_ID), null, sender.get(DEV_ID));
    }
    if (receiver != null) {
      sigMsg.mReceiver = new MMXid(receiver.get(USER_ID), null, null);
    }

    return sigMsg;
  }

}
