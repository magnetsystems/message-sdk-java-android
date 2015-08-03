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

import com.magnet.mmx.client.common.MMXSettings;

class MMXSettingsImpl implements MMXSettings {
  private HashMap<String, String> mSettingsMap = new HashMap<String, String>();

  public boolean getBoolean(String key, boolean defaultValue) {
    String value = mSettingsMap.get(key);
    if (value == null) {
      return defaultValue;
    }
    return Boolean.valueOf(value);
  }

  public int getInt(String key, int defaultValue) {
    String value = mSettingsMap.get(key);
    if (value == null) {
      return defaultValue;
    }
    return Integer.valueOf(value);
  }

  public String getString(String key, String defaultValue) {
    String value = mSettingsMap.get(key);
    if (value == null) {
      return defaultValue;
    }
    return value;
  }

  public void setBoolean(String key, boolean value) {
    mSettingsMap.put(key, String.valueOf(value));
  }

  public void setInt(String key, int value) {
    mSettingsMap.put(key, String.valueOf(value));
  }

  public void setString(String key, String value) {
    mSettingsMap.put(key, value);
  }
  
  public MMXSettings clone() {
    MMXSettingsImpl settings = new MMXSettingsImpl();
    settings.mSettingsMap.putAll(mSettingsMap);
    return settings;
  }
}
