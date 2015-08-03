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

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.telephony.TelephonyManager;

/**
 * Accessor for custom device ID (e.g. vehicle number or phone number.)
 */
public interface DeviceIdAccessor {
  /**
   * Built-in phone ID accessor.  It is the default for compatibility reason.
   * This requires permission <code>android.permission.READ_PHONE_STATE</code>.
   */
  public final static DeviceIdAccessor sPhoneIdAccessor = new DeviceIdAccessor() {
    @Override
    public String getId(Context context) {
      TelephonyManager tm = (TelephonyManager)context.getSystemService(
        Context.TELEPHONY_SERVICE);
      return tm.getDeviceId();
    }
  };
  
  /**
   * Built-in Bluetooth address accessor.  It is useful for phone and tablet.
   * This requires permission <code>android.permission.BLUETOOTH</code>.
   */
  public final static DeviceIdAccessor sNetIdAccessor = new DeviceIdAccessor() {
    @Override
    public String getId(Context context) {
      BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
      return (adapter == null) ? null : adapter.getAddress();
    }
  };
  
  /**
   * Get a custom ID as a device ID.
   * @param context A provided Android application context
   * @return A custom ID, or null if not available.
   */
  public String getId(Context context);
}