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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.util.Log;

import com.magnet.mmx.client.common.MMXContext;

class MMXContextImpl extends MMXContext {
  private static final String TAG = MMXContextImpl.class.getSimpleName();
  private static final String MMX_SUBDIR = "mmx";
  private Context mContext;

  MMXContextImpl(Context context) {
    super();
    mContext = context;
  }

  public synchronized File getDataDir() {
    File dataDir = new File(mContext.getFilesDir(), MMX_SUBDIR);
    if (!dataDir.exists()) {
      dataDir.mkdirs();
    }
    return dataDir;
  }

  public String getAppVersion() {
    try {
      PackageInfo pi = mContext.getPackageManager().getPackageInfo(mContext.getPackageName(), 0);
      return pi.versionName;
    } catch (NameNotFoundException e) {
      Log.e(TAG, "getAppVersion(): exception caught ", e);
    }
    return "UNKNOWN";
  }

  public String getDeviceId() {
    return DeviceIdGenerator.getUniqueDeviceId(mContext);
  }

  public void log(String tag, String data) {
    Log.d(tag, data);
  }
  
  @Override
  public FileOutputStream openFileOutput(String name) throws IOException {
    return mContext.openFileOutput(name, Context.MODE_PRIVATE|Context.MODE_APPEND);
  }

  @Override
  public FileInputStream openFileInput(String name) throws IOException {
    return mContext.openFileInput(name);
  }
}
