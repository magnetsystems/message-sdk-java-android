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

import java.util.Map;

import android.os.Handler;

import com.magnet.mmx.client.common.MMXException;
import com.magnet.mmx.client.common.PushManager;
import com.magnet.mmx.protocol.Constants;
import com.magnet.mmx.protocol.MMXid;
import com.magnet.mmx.protocol.Notification;
import com.magnet.mmx.protocol.PushResult;

/**
 * The push manager to deliver a payload using the native push mechanism (e.g.
 * GCM or APNS.)
 */
public class MMXPushManager extends MMXManager {
  private static final String TAG = MMXPushManager.class.getSimpleName();
  private PushManager mPushManager;

  MMXPushManager(MMXClient client, Handler handler) {
    super(client, handler);
    onConnectionChanged();
  }
  
  @Override
  void onConnectionChanged() {
    mPushManager = PushManager.getInstance(getMMXClient().getMMXConnection());
  }

  /**
   * Deliver a payload to a recipient using its native push mechanism.  The
   * payload will be converted to JSON and delivered via GCM or APNS.  If
   * {@link MMXid#getDeviceId()} is null, the payload will be delivered to all
   * active devices registered to the recipient.  The <code>payload</code>
   * contains a set of key-value pairs.  The keys can be
   * {@link Constants#PAYLOAD_PUSH_TITLE}, {@link Constants#PAYLOAD_PUSH_BODY},
   * {@link Constants#PAYLOAD_PUSH_ICON}, {@link Constants#PAYLOAD_PUSH_SOUND},
   * {@link Constants#PAYLOAD_PUSH_BADGE}, any APNS specific elements, or any
   * custom one.  If no custom key-value pairs are specified, the push payload
   * will be shown as push notification automatically; otherwise, the push
   * payload will be handed off to the application for handling.  The optional
   * <code>type</code> is used to identify the push payload; it is similar to
   * the APNS <i>category</i>.
   * @param recipient The user or the end-point.
   * @param type An optional type of this push message (e.g. "Greetings".)
   * @param payload A dictionary of key-value pairs.
   * @return
   * @throws MMXException
   */
  public PushResult sendPushMessage(MMXid recipient, String type,
                                     Map<String, Object> payload) throws MMXException {
    return mPushManager.push(recipient.getUserId(), recipient.getDeviceId(),
                              type, payload);
  }
  
  /**
   * Deliver a push notification to a recipient using its native push mechanism.
   * The <code>type</code> can be from {@link Notification#getType()} or a
   * custom value.  The <code>payload</code> can be a subclass of Notification.
   * @param recipient The user or the end-point.
   * @param type An optional type of this push notification.
   * @param payload A push notification payload.
   * @return
   * @throws MMXException
   */
  public PushResult sendPushMessage(MMXid recipient, String type,
                                    Notification payload) throws MMXException {
    return mPushManager.push(recipient.getUserId(), recipient.getDeviceId(),
        type, payload);
  }
}
