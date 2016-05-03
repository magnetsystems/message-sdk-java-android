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
import java.io.FileWriter;
import java.io.IOException;
import java.util.Properties;

import com.magnet.mmx.client.common.Log;

/**
 * The connection settings for Java client with a persistent storage.  This
 * implementation uses a Properties file to load and store the settings.  The
 * MMX client library does not persist any settings, but the MMX Java SDK
 * provides persistent storage for ease of use.  Developers are free to override
 * the settings persister.
 */
public class MMXSettings implements com.magnet.mmx.client.common.MMXSettings,
                                        MMXPersistable {

  public static final String PROP_MMSBASEURL = "mmsBaseUrl";
  public static final String PROP_MMSDEVUSER = "mmsDevUser";
  public static final String PROP_MMSDEVPASSWD = "mmsDevPasswd";
  public static final String PROP_MMSUSERID = "mmsUserId";
  public static final String PROP_MMSOAUTHCLIENTID = "mmsOauthClientId";
  public static final String PROP_MMSOAUTHSECRET = "mmsOauthSecret";

  /**
   * Auto Registering Device is needed for V1 support
   * Default value = false (DISABLED).
   */
  public static final String ENABLE_AUTO_REGISTER_DEVICE = "false";

  private final static String TAG = "MMXSettings";
  protected String mName;
  protected Properties mProps = new Properties();
  protected MMXContext mContext;

  /**
   * Constructor for the connection settings using a file.
   * @param context The context with the application data directory.
   * @param name The settings file name under the application data directory.
   */
  public MMXSettings(MMXContext context, String name) {
    mContext = context;
    mName = name;
  }

  /**
   * Get the file name of the settings.
   * @return
   */
  public String getName() {
    return mName;
  }

  /**
   * Copy the entire settings from <code>src</code> to this.
   * @param src
   */
  public void copyAll(MMXSettings src) {
    mProps.putAll(src.mProps);
  }

  /**
   * Get an integer property.
   * @param name One of the predefined property name.
   * @param defVal A default value.
   */
  @Override
  public int getInt(String name, int defVal) {
    String val = mProps.getProperty(name);
    if (val == null) {
      return defVal;
    }
    return Integer.parseInt(val);
  }

  /**
   * Set an integer property.
   * @param name One of the predefined property name.
   * @param value The value to be set.
   */
  @Override
  public void setInt(String name, int value) {
    mProps.setProperty(name, String.valueOf(value));
  }

  /**
   * Get a string property.
   * @param name One of the predefined property name.
   * @param defVal A default value.
   */
  @Override
  public String getString(String name, String defVal) {
    return mProps.getProperty(name, defVal);
  }

  /**
   * Set a string property.
   * @param name One of the predefined property names.
   * @param value The value to be set.
   */
  @Override
  public void setString(String name, String value) {
    if (value == null) {
      mProps.remove(name);
    } else {
      mProps.setProperty(name, value);
    }
  }

  /**
   * Get a boolean property.
   * @param name One of the predefined property names.
   * @param defVal A default value.
   */
  @Override
  public boolean getBoolean(String name, boolean defVal) {
    String val = mProps.getProperty(name);
    if (val == null) {
      return defVal;
    }
    if ("true".equalsIgnoreCase(val) || "1".equals(val) || "yes".equalsIgnoreCase(val)) {
      return true;
    }
    return false;
  }

  /**
   * Set the boolean property.
   * @param name One of the pre-defined property names.
   * @param value The value to be set.
   */
  @Override
  public void setBoolean(String name, boolean value) {
    mProps.setProperty(name, value ? "true" : "false");
  }

  /**
   * Clone this object.
   * @return The clone object.
   */
  @Override
  public MMXSettings clone() {
    MMXSettings settings = new MMXSettings(mContext, mName);
    settings.mProps.putAll(mProps);
    settings.mContext = mContext;
    return settings;
  }

  /**
   * Check if the file exists.
   * @return true if the setting files exists; otherwise false.
   */
  @Override
  public boolean exists() {
    File file = new File(mContext.getFilePath(mName));
    return file.exists();
  }

  /**
   * Load the settings from the file.
   * @return true if loaded successfully; otherwise false.
   */
  @Override
  public boolean load() {
    FileInputStream fis = null;
    try {
      if (Log.isLoggable(TAG, Log.DEBUG)) {
        Log.d(TAG, "Loading settings from "+mContext.getFilePath(mName));
      }
      fis = mContext.openFileInput(mName);
      mProps.load(fis);
      return true;
    } catch (IOException e) {
      Log.e(TAG, "Cannot load settings", e);
      return false;
    } finally {
      if (fis != null) {
        try {
          fis.close();
        } catch (Throwable e) {
          // Ignored.
        }
      }
    }
  }

  /**
   * Persist the changes to the file.
   * @return true if saved successfully; otherwise false.
   */
  @Override
  public boolean save() {
    FileWriter writer = null;
    try {
      writer = new FileWriter(mContext.getFilePath(mName));
      mProps.store(writer, "MMX Settings");
      if (Log.isLoggable(TAG, Log.DEBUG)) {
        Log.d(TAG, "Saving settings to "+mContext.getFilePath(mName));
      }
      return true;
    } catch (IOException e) {
      Log.e(TAG, "Cannot store settings", e);
      return false;
    } finally {
      if (writer != null) {
        try {
          writer.close();
        } catch (Throwable e) {
          // Ignored.
        }
      }
    }
  }

  @Override
  public String toString() {
    return mProps.toString();
  }
}
