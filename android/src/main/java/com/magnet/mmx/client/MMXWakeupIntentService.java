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

import com.magnet.mmx.client.common.Log;
import com.magnet.mmx.protocol.Constants;
import com.magnet.mmx.protocol.Constants.PingPongCommand;
import com.magnet.mmx.protocol.GCMPayload;
import com.magnet.mmx.protocol.MMXTypeMapper;
import com.magnet.mmx.protocol.PushMessage;
import com.magnet.mmx.util.UnknownTypeException;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;

import org.json.JSONObject;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

/**
 * The service that handles ALL wakeups for the MMXClient (GCM and timer-based)
 */
public final class MMXWakeupIntentService extends IntentService {
  private static final String TAG = MMXWakeupIntentService.class.getSimpleName();
  private static int sNoteId = 10;

  public static final String HEADER_WAKEUP =
      Constants.MMX + ":" +
      Constants.MMX_ACTION_CODE_WAKEUP + ":" +
      Constants.PingPongCommand.retrieve.name();

  public static final String HEADER_PING =
      Constants.MMX + ":" +
      Constants.MMX_ACTION_CODE_WAKEUP + ":" +
      Constants.PingPongCommand.ping.name();

  public static final String HEADER_PUSH =
      Constants.MMX + ":" +
      Constants.MMX_ACTION_CODE_PUSH + ":" +
      PingPongCommand.notify.name();

  public static final String HEADER_PUBSUB =
      Constants.MMX + ":" +
      Constants.MMX_ACTION_CODE_WAKEUP + ":" +
      PingPongCommand.pubsub.name();

  public MMXWakeupIntentService() {
    super("MMXGcmIntentService");
  }

  @Override
  protected void onHandleIntent(Intent intent) {
    //verify the message is a wakeup message
    String action = intent.getAction();
    if (Log.isLoggable(TAG, Log.DEBUG)) {
      Log.d(TAG, "onHandleIntent(): action=" + action);
    }
    if ("com.google.android.c2dm.intent.RECEIVE".equals(action)) {
      GooglePlayServicesWrapper playServicesWrapper = GooglePlayServicesWrapper.getInstance(this);
      int messageType = playServicesWrapper.getGcmMessageType(intent);
      if (Log.isLoggable(TAG, Log.DEBUG)) {
        Log.d(TAG, "onHandleIntent(): Received message type: " + messageType);
      }
      switch (messageType) {
        case GooglePlayServicesWrapper.GCM_MESSAGE_TYPE_SEND_ERROR:
        case GooglePlayServicesWrapper.GCM_MESSAGE_TYPE_DELETED:
        case GooglePlayServicesWrapper.GCM_MESSAGE_TYPE_SEND_EVENT:
          Log.w(TAG, "onHandleIntent(): Message type is not handled: " + messageType);
          break;
        case GooglePlayServicesWrapper.GCM_MESSAGE_TYPE_MESSAGE:
          //parse the GCM message
          if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "onHandleIntent(): Handling GCM message.  extras: " + intent.getExtras());
          }
          Bundle extras = intent.getExtras();
          String msg = extras.getString("msg");

          boolean isMmxHandle = false;
          if (msg != null) {
            try {
              PushMessage pushMessage = PushMessage.decode(msg, MMXTypeMapper.getInstance());
              isMmxHandle = handleMMXInternalPush(pushMessage);
              Log.d(TAG, "isMmxHandle="+isMmxHandle+", pushMsg="+pushMessage);
            } catch (UnknownTypeException ex) {
              //This is not an internal MMX message type
              Log.i(TAG, "onHandleIntent() forwarding intent to application");
            } catch (Throwable e) {
              Log.e(TAG, "onHandleIntent() generic exception caught while parsing GCM payload.", e);
            }
          }
          if (!isMmxHandle) {
            MMXClient.handleWakeup(this, intent);
          }
          break;
      }
      MMXGcmBroadcastReceiver.completeWakefulIntent(intent);
    } else {
      if (Intent.ACTION_BOOT_COMPLETED.equals(action) || MMXClient.ACTION_WAKEUP.equals(action)) {
        MMXClient.handleWakeup(this, intent);
      } else {
        //log and do nothing
        Log.w(TAG, "onHandleIntent(): Unsupported action: " + action);
      }
      MMXWakeupReceiver.completeWakefulIntent(intent);
    }
  }

  private void invokeNotificationForPush(GCMPayload payload) {
    int iconResId = 0;
    if (payload.getIcon() != null) {
      iconResId = this.getResources().getIdentifier(payload.getIcon(),
          "drawable", this.getPackageName());
    } else {
      iconResId = this.getApplicationInfo().icon;
    }
    Uri soundUri = null;
    if (payload.getSound() != null) {
      // TODO: cannot handle custom sound yet; use default sound.
      soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
    }
    Notification.Builder noteBuilder = new Notification.Builder(this)
            .setAutoCancel(true)
            .setWhen(System.currentTimeMillis())
            .setSmallIcon(iconResId)
            .setSound(soundUri)
            .setContentTitle(payload.getTitle())
            .setContentText(payload.getBody());
    NotificationManager noteMgr = (NotificationManager)
        this.getSystemService(Context.NOTIFICATION_SERVICE);
    noteMgr.notify(sNoteId++, noteBuilder.build());
  }

  private void invokeWakeupHandlerForPush(GCMPayload payload) {
    if (Log.isLoggable(TAG, Log.DEBUG)) {
      Log.d(TAG, "onHandleIntent(): Firing push intent");
    }
    Intent pushIntent = buildPushIntent(payload);
    MMXClient.handleWakeup(this, pushIntent);
  }

  static Intent buildPushIntent(GCMPayload payload) {
    // create an Intent with Push data formatted as Strings in intent extras
    Intent pushIntent = new Intent(MMXClient.ACTION_PUSH_RECEIVED);
    if (!TextUtils.isEmpty(payload.getTitle())) {
      pushIntent.putExtra(MMXClient.EXTRA_PUSH_TITLE, payload.getTitle());
    }
    if (!TextUtils.isEmpty(payload.getBody())) {
      pushIntent.putExtra(MMXClient.EXTRA_PUSH_BODY, payload.getBody());
    }
    if (!TextUtils.isEmpty(payload.getSound())) {
      pushIntent.putExtra(MMXClient.EXTRA_PUSH_SOUND, payload.getSound());
    }
    if (!TextUtils.isEmpty(payload.getIcon())) {
      pushIntent.putExtra(MMXClient.EXTRA_PUSH_ICON, payload.getIcon());
    }
    // extract notification ID from _mmx
    Map<String, ? super Object> mmx = payload.getMmx();

    if (mmx != null) {
      if (!TextUtils.isEmpty((String)mmx.get(Constants.PAYLOAD_ID_KEY))) {
        pushIntent.putExtra(MMXClient.EXTRA_PUSH_ID, (String) mmx.get(Constants.PAYLOAD_ID_KEY));
      }
      // get custom dictionary but pass it as a String in json
      Map<String, ? super Object> customEntries = (Map<String, ? super Object>) mmx.get(Constants.PAYLOAD_CUSTOM_KEY);
      if (customEntries != null) {
        JSONObject jsonObject = new JSONObject(customEntries);
        pushIntent.putExtra(MMXClient.EXTRA_PUSH_CUSTOM_JSON, jsonObject.toString());
      }
    }
    return pushIntent;
  }

  private void invokeCallback(String urlString) {
    //POST to the callback url
    HttpURLConnection conn = null;
    try {
      URL url = new URL(urlString);
      conn = (HttpURLConnection) url.openConnection();
      conn.setRequestMethod("POST");
      int responseCode = conn.getResponseCode();
      if (Log.isLoggable(TAG, Log.DEBUG)) {
        Log.d(TAG, "handleMMXInternalPush(): received response from callback url: " + responseCode);
      }
    } catch (MalformedURLException e) {
      Log.e(TAG, "handleMMXInternalPush(): unable to parse ping callback url: " + urlString);
    } catch (IOException e) {
      Log.e(TAG, "handleMMXInternalPush(): unable to connect to callback url: " + urlString);
    } finally {
      if (conn != null) {
        conn.disconnect();
      }
    }
    return;
  }
  private boolean handleMMXInternalPush(PushMessage pushMessage) {
    PushMessage.Action pushType = pushMessage.getAction();
    if (Log.isLoggable(TAG, Log.DEBUG)) {
      Log.d(TAG, "handleMMXInternalPush(): received command: " + pushType);
    }
    Object payload = pushMessage.getPayload();
    if (!(payload instanceof GCMPayload)) {
      return false;
    }
    GCMPayload gcmPayload = (GCMPayload) pushMessage.getPayload();
    if (gcmPayload == null) {
      return false;
    }
    boolean handled = false;
    String typeName = pushMessage.getType();
    String urlString = (String) gcmPayload.getMmx().get(Constants.PAYLOAD_CALLBACK_URL_KEY);
    if (PushMessage.Action.WAKEUP == pushType) { // "mmx:w:*"
      if (Constants.PingPongCommand.retrieve.name().equalsIgnoreCase(typeName)) { // "mmx:w:retrieve"
        MMXClient.handleWakeup(this, new Intent(MMXClient.ACTION_RETRIEVE_MESSAGES));
        // TODO: should it do the URL callback if specified?  Currently not.
        return true;
      } else if (Constants.PingPongCommand.ping.name().equalsIgnoreCase(typeName)) { // "mmx:w:ping"
        handled = true;
      }
    } else if (PushMessage.Action.PUSH == pushType) {  // "mmx:p:*"
      // TODO: console should use "mmx:p:notify" instead of "mmx:p".
      // TODO: the following code causes MMXPushEvent unable to parse the intent.

      // Only the payload does not have custom content and it has at least one
      // of the notification properties, the notification will be shown.
      // Otherwise, an Intent will be sent.
      boolean doNotify =
          !gcmPayload.getMmx().containsKey(GCMPayload.KEY_CUSTOM_CONTENT) &&
          (gcmPayload.getTitle() != null ||
           gcmPayload.getBody() != null ||
           gcmPayload.getIcon() != null ||
           gcmPayload.getSound() != null);
      if (Log.isLoggable(TAG, Log.DEBUG)) {
        Log.d(TAG, "handleMMXInternalPush(): doNotify="+doNotify);
      }
      if (doNotify) {
        invokeNotificationForPush(gcmPayload);
        handled = true;
      }
//      if (TextUtils.isEmpty(typeName)) { // "mmx:p"
//        String text = gcmPayload.getBody();
//        if (Log.isLoggable(TAG, Log.DEBUG)) {
//          Log.d(TAG, "handleMMXInternalPush(): Received notification with body: " + text);
//        }
//        invokeWakeupHandlerForPush(gcmPayload);
//        if (!TextUtils.isEmpty(urlString)) {
//          invokeCallback(urlString);
//        }
//        return true;
//      }
    }

    // Do the URL callback if it is specified.
    if (!TextUtils.isEmpty(urlString)) {
      invokeCallback(urlString);
    }
    return handled;
  }
}
