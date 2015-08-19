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

package com.magnet.mmx.client.common;

import com.magnet.mmx.protocol.Constants;

/**
 * Settings for connection to MMX server.
 */
public interface MMXSettings {
  /**
   * Default service name used by MMX server.
   */
  public static final String DEFAULT_SERVICE_NAME = Constants.MMX_DOMAIN;
  /**
   * MMX server host name or IP address (String.)
   */
  public static final String PROP_HOST = "host";
  /**
   * MMX server port (int.)
   */
  public static final String PROP_PORT = "port";
  /**
   * MMX service name
   */
  public static final String PROP_SERVICE_NAME = "serviceName";
  /**
   * The application ID for current app (String.)
   */
  public static final String PROP_APPID = "appId";
  /**
   * The API Key for current application to use (String.)
   */
  public static final String PROP_APIKEY = "apiKey";
  /**
   * The user ID for the app server (String.)  This account is created when the
   * application is registered to MMX server.
   */
  public static final String PROP_SERVERUSER = "serverUser";    // server user (no %appid)
  /**
   * The secret key.
   */
  public static final String PROP_GUESTSECRET = "guestSecret";
  /**
   * The user identity (String.)  A convenient property for the application
   * to store the user ID.
   */
  public static final String PROP_USER = "user";      // user ID (no appId)
  /**
   * The resource identifier (String.)
   */
  public static final String PROP_RESOURCE = "resource";
  /**
   * The password of the user (String.)  A convenient property for the
   * application to store a protected password.
   */
  public static final String PROP_PASSWD = "passwd";
  /**
   * The display name for the user (String.)
   */
  public static final String PROP_NAME = "name";
  /**
   * The email address for the user (String.)
   */
  public static final String PROP_EMAIL = "email";
  /**
   * The phone number for the user (String.)  It is highly recommended.
   */
  public static final String PROP_PHONE = "phone";
//  /**
//   * The priority for the current connection (int.)  MMX Server will deliver
//   * messages to the connection with the highest priority.  The range is -128 to
//   * 128.  Default is 0.
//   */
//  public static final String PROP_PRIORITY = "priority";
  /**
   * Enable auto reconnect capability (boolean.)  This option is only
   * suitable to Java client.  Default is false.
   */
  public static final String PROP_ENABLE_RECONNECT = "enableReconnect";
  /**
   * Enable protocol compression (boolean.)  Default is true.
   */
  public static final String PROP_ENABLE_COMPRESSION = "enableCompression";
  /**
   * Enable online mode immediately after the connection (boolean.)  If
   * disabled, offline messages will not be delivered to the connected device
   * automatically.  Default is true.
   */
  public static final String PROP_ENABLE_ONLINE = "enableOnline";
  /**
   * Enable TLS connection (boolean.)  Default is false.
   */
  public static final String PROP_ENABLE_TLS = "enableTLS";
  /**
   * Enable sync for the roster list after the connection (boolean.)  Default is
   * false.
   */
  public static final String PROP_ENABLE_SYNC = "enableSync";
  /**
   * Enable proxy connection (boolean.)  Default is false.
   */
  public static final String PROP_ENABLE_PROXY = "enableProxy";
  /**
   * Number of latest published item for each subscribed topic to be sent after
   * the connection (int.)  Default is 1.  0 will disable this feature and -1
   * means to get the max items configured by the server.
   */
  public static final String PROP_MAX_LAST_PUB_ITEMS = "maxLastPubItems";

  public static final String PROP_PRESENCE_MODE = "presenceMode";
  public static final String PROP_PRESENCE_STATUS = "presenceStatus";
  public static final String PROP_PROXYTYPE = "proxy-type";
  public static final String PROP_PROXYHOST = "proxy-host";
  public static final String PROP_PROXYPORT = "proxy-port";
  public static final String PROP_PROXYUSER = "proxy-user";
  public static final String PROP_PROXYPWD = "proxy-passwd";

  /**
   * Get an integer property.
   * @param name
   * @param defVal
   * @return
   */
  public int getInt(String name, int defVal);
  /**
   * Set an integer property.
   * @param name
   * @param val
   */
  public void setInt(String name, int val);
  /**
   * Get a string property.
   * @param name
   * @param defVal
   * @return
   */
  public String getString(String name, String defVal);
  /**
   * Set a string property.
   * @param name
   * @param val
   */
  public void setString(String name, String val);
  /**
   * Get a boolean property.
   * @param name
   * @param defVal
   * @return
   */
  public boolean getBoolean(String name, boolean defVal);
  /**
   * Set a boolean property.
   * @param name
   * @param val
   */
  public void setBoolean(String name, boolean val);
  /**
   * Clone this object.
   * @return A cloned object.
   */
  public MMXSettings clone();
}
