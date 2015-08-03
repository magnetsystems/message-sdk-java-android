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

import java.util.HashMap;

import android.util.Log;

import com.magnet.mmx.BuildConfig;
import com.magnet.mmx.client.common.Logger;

/**
 * @hide
 * The Android implementation of logger.
 */
public class AndroidLogger implements Logger {
  private static int sDefaultLogLevel = BuildConfig.DEBUG ? 
      com.magnet.mmx.client.common.Log.DEBUG : com.magnet.mmx.client.common.Log.INFO;
  private static HashMap<String, Integer> sLogTags = new HashMap<String, Integer>();
  
  @Override
  public int getLoggable(String tag) throws UnsupportedOperationException {
    Integer level = (tag == null) ? null : sLogTags.get(tag);
    if (level != null)
      return level;
    return sDefaultLogLevel;
  }

  @Override
  public void setLoggable(String tag, int level)
                            throws UnsupportedOperationException {
    if (tag != null) {
      sLogTags.put(tag, level);
    } else {
      sDefaultLogLevel = level;
      // For asmack logging.
      java.util.logging.Logger.global.setLevel(LEVELS[level]);
    }
  }

  @Override
  public void verbose(String tag, String data, Throwable cause) {
    Log.v(tag, data, cause);
  }

  @Override
  public void debug(String tag, String data, Throwable cause) {
    Log.d(tag, data, cause);
  }

  @Override
  public void info(String tag, String data, Throwable cause) {
    Log.i(tag, data, cause);
  }

  @Override
  public void warn(String tag, String data, Throwable cause) {
    Log.w(tag, data, cause);
  }

  @Override
  public void error(String tag, String data, Throwable cause) {
    Log.e(tag, data, cause);
  }
}
