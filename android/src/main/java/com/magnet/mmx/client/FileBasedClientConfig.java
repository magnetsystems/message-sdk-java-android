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

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import android.content.Context;
import android.util.Log;

import com.magnet.mmx.protocol.Constants;

/**
 * An implementation of the MMXClientConfig that reads the config from the
 * specified file.  This file is expected to be a properties file with the following values:
 *
 * <code>
 *   appId=<MMX App ID>
 *   apiKey=<MMX API key>
 *   gcmSenderId=<gcm project/sender id>
 *   serverUser=<MMX server user id>
 *   anonymousSecret=<MMX app guest secret>
 *   host=<hostname of the MMX server>
 *   port=5222
 *   securityLevel=RELAXED
 *   domainName=mmx
 * </code>
 *
 */
public class FileBasedClientConfig implements MMXClientConfig {
  private static final String TAG = FileBasedClientConfig.class.getSimpleName();
  private Properties mProps = null;
  protected Context mContext = null;

  private static final String PROP_APPID = "appId";
  private static final String PROP_APIKEY = "apiKey";
  private static final String PROP_GCM_SENDERID = "gcmSenderId";
  private static final String PROP_SERVER_USER = "serverUser";
  private static final String PROP_ANONYMOUS_SECRET = "anonymousSecret";
  private static final String PROP_HOST = "host";
  private static final String PROP_PORT = "port";
  private static final String PROP_REST_PORT = "RESTPort";
  private static final String PROP_SECURITY_LEVEL = "securityLevel";
  private static final String PROP_DOMAIN_NAME = "domainName";

  /**
   * Creates an MMXClientConfig instance based on the specified name.
   *
   * @param context the android context
   * @param rawResId the R.raw.foo value of the properties file
   */
  public FileBasedClientConfig(Context context, int rawResId) {
    this(context);
    InputStream is = mContext.getResources().openRawResource(rawResId);
    loadProps(is);
  }

  /**
   * Creates an MMXClientConfig instance based on the specified inputstream.
   *
   * @param context the android context
   * @param is The property file inputstream
   */
  public FileBasedClientConfig(Context context, InputStream is) {
    this(context);
    loadProps(is);
  }

  private FileBasedClientConfig(Context context) {
    mContext = context.getApplicationContext();
    mProps = new Properties();
  }

  private void loadProps(InputStream is) {
    try {
      mProps.load(is);
    } catch (IOException e) {
      Log.e(TAG, "FileBasedClientConfig(): Exception caught while loading config", e);
      throw new RuntimeException(e);
    }
  }

  /**
   * Retrieve the configured App ID.
   *
   * @return the configured App ID
   */
  public String getAppId() {
    return mProps.getProperty(PROP_APPID);
  }

  /**
   * Retrieve the configured API key.
   *
   * @return the configured API key
   */
  public String getApiKey() {
    return mProps.getProperty(PROP_APIKEY);
  }

  /**
   * Retrieve the configured GCM sender ID/project ID.
   *
   * @return the configured gcmSenderId
   */
  public String getGcmSenderId() {
    return mProps.getProperty(PROP_GCM_SENDERID);
  }

  /**
   * Retrieve the configured server user.
   *
   * @return the configured server user
   */
  public String getServerUser() {
    return mProps.getProperty(PROP_SERVER_USER);
  }

  /**
   * Retrieve the configured anonymous secret.
   *
   * @return the configured anonymous secret
   */
  public String getAnonymousSecret() {
    return mProps.getProperty(PROP_ANONYMOUS_SECRET);
  }

  /**
   * Retrieve the configured hostname.
   *
   * @return the configured hostname
   */
  public String getHost() { return mProps.getProperty(PROP_HOST); }

  /**
   * Retrieve the configured port.
   *
   * @return the configured port
   */
  public int getPort() {
    String portString = mProps.getProperty(PROP_PORT);
    return portString != null ? Integer.valueOf(portString) : -1;
  }

  /**
   * The port for the MMX REST APIs
   *
   * @return the port number for the REST APIs, -1 if not specified
   */
  public int getRESTPort() {
    String portString = mProps.getProperty(PROP_REST_PORT);
    return portString != null ? Integer.valueOf(portString) : -1;
  }

  /**
   * Retrieve the configured domain name.
   *
   * @return the configured domain name
   */
  public String getDomainName() {
    String domainName = mProps.getProperty(PROP_DOMAIN_NAME);
    return domainName == null ? Constants.MMX_DOMAIN : domainName;
  }

  /**
   * Retrieve the configured security level.
   *
   * @return the configured security level
   */
  public MMXClient.SecurityLevel getSecurityLevel() {
    String securityLevel = mProps.getProperty(PROP_SECURITY_LEVEL);
    return securityLevel != null ? MMXClient.SecurityLevel.valueOf(securityLevel) : MMXClient.SecurityLevel.STRICT;
  }
  
  /**
   * Retrieve the phone ID as device ID.  Developer overrides this method for
   * custom device ID (e.g, Bluetooth address for tablet, vehicle identification
   * number, phone number.)
   * 
   * @return the device ID for the client, or null.
   */
  public String getDeviceId() {
    return DeviceIdAccessor.sPhoneIdAccessor.getId(mContext);
  }
  
  /**
   * Check if the device ID should be obfuscated.
   * 
   * @return true to obfuscate the device ID; otherwise, false.
   */
  public boolean obfuscateDeviceId() {
    return DeviceIdAccessor.sPhoneIdAccessor.obfuscated();
  }
}
