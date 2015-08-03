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
 * This handles timer-based wake-ups if configured with MMXClient.setWakeupInterval().
 *
 * @see MMXClient#setWakeupInterval(Context, long)
 */
public final class MMXWakeupReceiver extends WakefulBroadcastReceiver {
  public void onReceive(Context context, Intent intent) {
    ComponentName component = new ComponentName(context.getPackageName(), MMXWakeupIntentService.class.getName());
    startWakefulService(context, (intent.setComponent(component)));
    setResultCode(Activity.RESULT_OK);
  }
}
