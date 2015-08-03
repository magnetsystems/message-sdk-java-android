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

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.WakefulBroadcastReceiver;

/**
 * This is the receiver that handles GCM wakeup messages from MMX.  This needs to be
 * declared in the AndroidManifest.xml file as part of the required GCM config.
 *
 * <pre>
 *   {@code
 *       <uses-permission android:name="com.google.android.c2dm.permission.RECEIVE" />
 *
 *       <permission android:name="YOUR.PACKAGE.NAME.permission.C2D_MESSAGE"
 *          android:protectionLevel="signature" />
 *       <uses-permission android:name="YOUR.PACKAGE.NAME.permission.C2D_MESSAGE" />
 *       ...
 *         <receiver
 *           android:name="com.magnet.mmx.MMXGcmBroadcastReceiver"
 *           android:permission="com.google.android.c2dm.permission.SEND" >
 *           <intent-filter>
 *             <action android:name="com.google.android.c2dm.intent.RECEIVE" />
 *             <category android:name="com.magnet.app.mmxclient" />
 *           </intent-filter>
 *         </receiver>
 *         <receiver
 *           android:name="com.magnet.mmx.MMXWakeupReceiver"
 *           android:exported="false">
 *         </receiver>
 *         <service android:name="com.magnet.mmx.MMXWakeupIntentService" />
 *   }
 * </pre>
 */
public final class MMXGcmBroadcastReceiver extends WakefulBroadcastReceiver {
  public void onReceive(Context context, Intent intent) {
    ComponentName component = new ComponentName(context.getPackageName(), MMXWakeupIntentService.class.getName());
    startWakefulService(context, (intent.setComponent(component)));
    setResultCode(Activity.RESULT_OK);
  }
}
