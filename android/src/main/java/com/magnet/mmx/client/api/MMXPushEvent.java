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

import java.io.Serializable;
import java.net.URISyntaxException;
import java.util.Map;

import android.content.Intent;
import android.util.Log;

import com.magnet.mmx.client.MMXClient;
import com.magnet.mmx.protocol.Constants;
import com.magnet.mmx.protocol.GCMPayload;
import com.magnet.mmx.protocol.PushMessage;
import com.magnet.mmx.protocol.PushMessage.Action;
import com.magnet.mmx.util.GsonData;
import com.magnet.mmx.util.InvalidMessageException;

/**
 * This class represents a push event sent by MMX server via GCM.  There are
 * different situations that a client will receive this event.
 * <ul>
 * <li>a channel wake-up notification: a special payload
 * com.magnet.mmx.protocol.PubSubNotification is sent by the server via GCM or
 * APNS when a message is published to a channel while the subscriber is not
 * connected to the server.</li>
 * <li>a retrieval event for ad-hoc messages: a wake-up event without any
 * payload is sent when a message is available while the recipient is not
 * connected to the server.<li>
 * <li>a push message sent from another client.
 * </ul>
 * The client must provide a custom receiver and convert its Intent into
 * MMXPushEvent via {@link #fromIntent(Intent)}.  By examining the type using
 * {@link #getType()}, the caller will handle each payload accordingly.
 *
 * @see MMX
 */
public class MMXPushEvent implements Serializable {
  private static final long serialVersionUID = 2789163511461919579L;
  private final static String TAG = MMXPushEvent.class.getSimpleName();
  private final PushMessage mPushMessage;

  /**
   * Convert the intent from the custom receiver into MMXPushEvent.  If the
   * intent does not conform to MMX server GCM format, it will return null.
   * @param intent An intent from the custom receiver.
   * @return A MMXPushEvent object or null.
   */
  public static MMXPushEvent fromIntent(Intent intent) {
    String uri = intent.getStringExtra(MMX.EXTRA_NESTED_INTENT);
    if (uri == null) {
      return null;
    }
    try {
      intent = Intent.parseUri(uri, Intent.URI_INTENT_SCHEME);
      // A special case for "retrieve" wakeup message.
      if (intent.getAction().equals(MMXClient.ACTION_RETRIEVE_MESSAGES)) {
        return new MMXPushEvent(Action.WAKEUP,
                                  Constants.PingPongCommand.retrieve.name());
      }
      String msg = intent.getStringExtra("msg");
      if (msg == null) {
        return null;
      }
      if (Log.isLoggable(TAG, Log.DEBUG)) {
        Log.d(TAG, "GCM msg="+msg);
      }
      return new MMXPushEvent(msg);
    } catch (InvalidMessageException e) {
      // Not an MMX push GCM.
      Log.w(TAG, "Failed to decode GCM into push event", e);
      return null;
    } catch (URISyntaxException e) {
      Log.e(TAG, "parse nested intent failed", e);
      return null;
    }
  }

  private MMXPushEvent(Action action, String type) {
    mPushMessage = new PushMessage(action, type, null);
  }

  private MMXPushEvent(String msg) {
    mPushMessage = PushMessage.decode(msg, null);
  }

  /**
   * Get the action type (wake-up/silent notification) or (push/non-silent
   * notification) of the notification.
   * @return
   */
  public Action getAction() {
    return mPushMessage == null ? null : mPushMessage.getAction();
  }

  /**
   * Get the push message type (if any.)  It can be an ad-hoc message wake-up
   * ("retrieve"), a channel wake-up ("pubsub"), or a custom type.
   * @return A push message type or null.
   */
  public String getType() {
    return mPushMessage == null ? null : mPushMessage.getType();
  }

  /**
   * Get the push message delivered via GCM.  The push message provides access
   * to all notification properties: title, body, badge, sound, and icon.
   * @return The GCM push message.
   */
  public GCMPayload getPushMessage() {
    return mPushMessage == null ? null : (GCMPayload) mPushMessage.getPayload();
  }

  /**
   * Get the key-value pairs payload from this event.
   * @return A Map of key-value pairs.
   */
  protected Map<String, ? super Object> getPayload() {
    GCMPayload payload = getPushMessage();
    if (payload == null) {
      return null;
    }
    return payload.getMmx();
  }

  /**
   * Get the custom payload as a Map object from the event payload.
   * Alternatively, use {@link #getCustomObject(Class)} if there is a class
   * associated with the custom payload.  The custom payload includes extra
   * push notification attributes to make silent and push notification similar
   * in Android.
   * @return A Map object for the payload.
   * @see #getCustomMap()
   */
  public Map<String, Object> getCustomMap() {
    Map<String, ? super Object> mmx = getPayload();
    if (mmx == null) {
      return null;
    }
    Map<String, Object> custom = (Map<String, Object>) mmx.get(
        GCMPayload.KEY_CUSTOM_CONTENT);
    if (custom != null) {
      // Silent or push notification are same in Android; copy push notification
      // attributes to the custom payload to simplify the client API.
      GCMPayload payload = getPushMessage();
      if (custom.get(Constants.PAYLOAD_PUSH_TITLE) == null &&
          payload.getTitle() != null) {
        custom.put(Constants.PAYLOAD_PUSH_TITLE, payload.getTitle());
      }
      if (custom.get(Constants.PAYLOAD_PUSH_BODY) == null &&
          payload.getBody() != null) {
        custom.put(Constants.PAYLOAD_PUSH_BODY, payload.getBody());
      }
      if (custom.get(Constants.PAYLOAD_PUSH_SOUND) == null &&
          payload.getSound() != null) {
        custom.put(Constants.PAYLOAD_PUSH_SOUND, payload.getSound());
      }
      if (custom.get(Constants.PAYLOAD_PUSH_BADGE) == null &&
          payload.getBadge() != null) {
        custom.put(Constants.PAYLOAD_PUSH_BADGE, payload.getBadge());
      }
      if (custom.get(Constants.PAYLOAD_PUSH_ICON) == null &&
          payload.getIcon() != null) {
        custom.put(Constants.PAYLOAD_PUSH_ICON, payload.getIcon());
      }
    }
    if (Log.isLoggable(TAG, Log.DEBUG)) {
      Log.d(TAG, "combined getCustomMap(): "+custom);
    }
    return custom;
  }

  /**
   * Get the custom payload as Object of class <code>clz</code> from the event
   * payload.  Typically developer uses {@link #getType()} to identify the
   * payload, then specifies a proper class for the payload.  Alternatively,
   * develop can use {@link #getCustomMap()} to get the key-value pairs.
   * @param clz The class of the custom object.
   * @return A custom object for the payload.
   * @see #getCustomMap()
   * @see #getType()
   */
  public <T> T getCustomObject(Class<T> clz) {
    Map<String, Object> custom = getCustomMap();
    if (custom == null) {
      return null;
    }
    return GsonData.fromMap(custom, clz);
  }

  @Override
  public String toString() {
    return (mPushMessage == null) ? null : mPushMessage.toString();
  }
}
