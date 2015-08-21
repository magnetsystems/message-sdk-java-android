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

import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.Serializable;
import java.util.Arrays;
import java.util.HashSet;

import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smackx.receipts.DeliveryReceipt;
import org.jivesoftware.smackx.receipts.DeliveryReceiptRequest;

import com.magnet.mmx.client.common.MMXPayloadMsgHandler.MMXPacketExtension;
import com.magnet.mmx.protocol.Constants;
import com.magnet.mmx.protocol.Payload;
import com.magnet.mmx.util.DisposableBinFile;
import com.magnet.mmx.util.DisposableFile;
import com.magnet.mmx.util.XIDUtil;

/**
 * This class represents an incoming message with application payload or
 * delivery receipt. A message contains envelop information, message meta data,
 * and application payload, and/or an optional delivery receipt.  The envelop
 * contains the sender, the target recipient and the message ID.  The headers
 * contains meta data required by the payload.  The payload contains the
 * application data.
 */
public class MMXMessage implements Serializable {
  private static final String TAG = "MMXMessage";
  private static final long serialVersionUID = -5040082668605553404L;
  private String mMsgId;
  private String mFrom;
  private String mTo;
  private String mReceiptMsgId;
  private String mReceiptId;
  private MMXPayload mPayload;
  private transient MMXid mFromXid;
  private transient MMXid mToXid;
  private transient MMXid[] mToXids;
  private transient MMXid[] mReplyAll;
//  private transient MMXid mReplyTo;

  // A wrapper for ordinary message with optional delivery receipt.
  MMXMessage(Message msg) {
    mMsgId = msg.getPacketID();
    mFrom = msg.getFrom();
    mTo = msg.getTo();
    mReceiptId = parseDeliveryReceiptRequest(msg);
    mReceiptMsgId = parseDeliveryReceipt(msg);
    mPayload = parsePayload(msg);
  }

  // A wrapper for a published item.  The from is useless; it only contains
  // the pubsub service, not the publisher JID.
  MMXMessage(String itemId, String from, String to, MMXPayload payload) {
    mMsgId = itemId;
    mFrom = from;
    mTo = to;
    mPayload = payload;
  }

  /**
   * Get the globally unique ID of this message.
   * @return The message ID.
   */
  public String getId() {
    return mMsgId;
  }

  /**
   * Get the sender identifier.
   * @return The identifier of the sender.
   */
  public MMXid getFrom() {
    if (mFromXid == null) {
      if (mPayload != null) {
        mFromXid = mPayload.unmarshallFrom();
      }
      if (mFromXid == null) {
        mFromXid = XIDUtil.toXid(mFrom, null);
      }
    }
    return mFromXid;
  }

//  /**
//   * Get the reply-to address. If the sender does not specify a reply-to
//   * address, it will get the address from the "from".
//   *
//   * @return The replying address for this message.
//   */
//  public MMXid getReplyTo() {
//    if (mReplyTo == null) {
//      String replyTo;
//      if ((replyTo = mPayload.getReplyTo()) == null) {
//        mReplyTo = getFrom();
//      } else {
//        mReplyTo = XIDUtil.toXid(replyTo, null);
//      }
//    }
//    return mReplyTo;
//  }

  /**
   * Get the current recipient with the display name.
   * @return The current recipient.
   */
  public MMXid getTo() {
    if (mToXid == null) {
      MMXid[] tos = getTos();
      if (tos != null) {
        MMXid self = XIDUtil.toXid(mTo, null);
        for (MMXid to : tos) {
          if (to.equalsTo(self)) {
            return mToXid = to;
          }
        }
        mToXid = self;
      }
    }
    return mToXid;
  }

  /**
   * Get all explicitly specified recipients of this message.  If no explicit
   * recipients are specified, an empty array will be returned.
   * @return A non-null array of recipients.
   */
  public MMXid[] getTos() {
    if (mToXids == null && mPayload != null) {
      mToXids = mPayload.unmarshallTo();
      if (mToXids == null) {
        mToXids = new MMXid[0];
      }
    }
    return mToXids;
  }

  /**
   * Return a list of explicit recipients for Reply-All.  It always includes the
   * sender and everyone in the To-list excluding the current user and duplicate
   * users.
   * @return A non-null array of recipients.
   */
  public MMXid[] getReplyAll() {
    if (mReplyAll == null) {
      MMXid[] tos = getTos();
      if (tos != null) {
        MMXid self = XIDUtil.toXid(mTo, null);
        MMXid sender = getFrom();
        HashSet<MMXid> set = new HashSet<MMXid>(tos.length);
        // Sender is always included, but exclude current user from the to-list.
        set.add(sender);
        for (MMXid to : tos) {
          if (!to.equalsTo(self)) {
            set.add(to);
          }
        }
        mReplyAll = new MMXid[set.size()];
        set.toArray(mReplyAll);
      }
    }
    return mReplyAll;
  }

  /**
   * Get the payload in this message.
   * @return A payload or null.
   */
  public MMXPayload getPayload() {
    return mPayload;
  }

//  /**
//   * A convenient method to get the optional meta headers from the payload.
//   * @return A Map object or null.
//   * @deprecated Use {@link MMXPayload#getAllMetaData()}
//   */
//  @Deprecated
//  public Map<String, String> getAllMetaData() {
//    return (mPayload == null) ? null : mPayload.getAllMetaData();
//  }

  /**
   * Get the message ID from the delivery receipt if it exists.
   * @return null or a message ID.
   */
  public String getMsgIdFromReceipt() {
    return mReceiptMsgId;
  }

  /**
   * Get the delivery receipt ID if this message has a delivery receipt request.
   * @return Delivery receipt ID, or null.
   */
  public String getReceiptId() {
    return mReceiptId;
  }

  String getFromJID() {
    return mFrom;
  }

  String getToJID() {
    return mTo;
  }

  /**
   * The string representation of this object for debug purpose.
   */
  @Override
  public String toString() {
    return "[ id="+getId()+", from="+getFrom()+", to="+getTo()+
        ", tos="+Arrays.asList(getTos())+", rcptId="+getReceiptId()+
        ", rcptMsgId="+getMsgIdFromReceipt()+", data="+getPayload()+" ]";
  }

  // @return A delivery receipt ID if there is a request for delivery receipt.
  private String parseDeliveryReceiptRequest(Message msg) {
    if (msg.getExtension(DeliveryReceiptRequest.ELEMENT,
                         DeliveryReceipt.NAMESPACE) == null) {
      return null;
    } else {
      return MessageManager.genReceiptId(msg.getFrom(), msg.getPacketID());
    }
  }

  // @return the message ID in the delivery receipt
  private String parseDeliveryReceipt(Message msg) {
    DeliveryReceipt dr = msg.getExtension(DeliveryReceipt.ELEMENT,
        DeliveryReceipt.NAMESPACE);
    if (dr != null) {
      return dr.getId();
    } else {
      return null;
    }
  }

  private MMXPayload parsePayload(Message msg) {
    MMXPacketExtension extension = msg.getExtension(Constants.MMX,
        Constants.MMX_NS_MSG_PAYLOAD);
    if (extension == null) {
      return null;
    } else {
      return extension.getPayload();
    }
  }

  // Assemble multiple payloads into a complete message.
  // Return true if the message is fully assembled.
  boolean assemble(MMXContext context) throws IOException {
    Payload pl = mPayload.getPayloadExt();
    String cid = pl.getCid();
    int total = pl.getDataSize();
    int offset = pl.getDataOffset();
    if (cid == null || (offset == 0 && (offset + pl.getDataLen()) == total)) {
      return true;
    }

    // Write a partial payload to a file whose name is "cid.dat".
    DisposableFile file = new DisposableBinFile(
        context.getFilePath(cid+".dat"), true);
    RandomAccessFile raf = null;
    try {
      raf = new RandomAccessFile(file, "rw");
      raf.seek(offset);
      raf.write(pl.getData().toString().getBytes());
      if (file.length() < total) {
        return false;
      }
      // Got all partial payloads; use the new payload based on the file.
      mPayload = new MMXPayload(pl.getMsgType(), file);
      return true;
    } finally {
      if (raf != null) {
        raf.close();
      }
    }
  }
}
