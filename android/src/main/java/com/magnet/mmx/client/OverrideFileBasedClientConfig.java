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

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Similar to a FileBasedClientConfig but allows for overriding of the host, port, and security level.
 * The overridden values are persisted based on the "name" of this configuration and persisted
 * values will apply until this config is reset().
 */
public class OverrideFileBasedClientConfig extends FileBasedClientConfig {
  private static final String SHARED_PREF_NAME = "ConfigOverrides-";
  private static final String PREF_HOST = "host";
  private static final String PREF_PORT = "port";
  private static final String PREF_SECURITY_LEVEL = "securityLevel";
  private static final String PREF_DOMAIN_NAME = "domainName";

  private SharedPreferences mOverridesSharedPref = null;

  /**
   * The constructor for this class.
   *
   * @param context the Android context for this application
   * @param rawResId the resource id of the original file-based config (R.raw.foo)
   */
  public OverrideFileBasedClientConfig(Context context, int rawResId) {
    super(context, rawResId);
    String name = context.getResources().getResourceEntryName(rawResId);
    mOverridesSharedPref = context.getSharedPreferences(SHARED_PREF_NAME + name, Context.MODE_PRIVATE);
  }

  /**
   * Returns the host value.
   *
   * @return the overridden host value or the original one if not overridden.
   */
  public String getHost() {
    String overriddenHost = mOverridesSharedPref.getString(PREF_HOST, null);
    return overriddenHost != null ? overriddenHost : super.getHost();
  }

  /**
   * Overrides the host value for this config.
   *
   * @param host the host value
   */
  public void setHost(String host) {
    SharedPreferences.Editor editor = mOverridesSharedPref.edit();
    if (host == null || host.isEmpty()) {
      editor.remove(PREF_HOST);
    } else {
      editor.putString(PREF_HOST, host);
    }
    editor.commit();
  }

  /**
   * Returns the port value.
   *
   * @return the overridden port value or the original one if not overridden.
   */
  public int getPort() {
    int overriddenPort = mOverridesSharedPref.getInt(PREF_PORT, -1);
    return overriddenPort > 0 ? overriddenPort : super.getPort();
  }

  /**
   * Overrides the port value for this config.
   *
   * @param port the port value
   */
  public void setPort(int port) {
    SharedPreferences.Editor editor = mOverridesSharedPref.edit();
    if (port <= 0) {
      editor.remove(PREF_PORT);
    } else {
      editor.putInt(PREF_PORT, port);
    }
    editor.commit();
  }

  /**
   * Returns the service name.
   *
   * @return the overridden service name value or the original one if not overridden.
   */
  public String getDomainName() {
    String overriddenDomainName = mOverridesSharedPref.getString(PREF_DOMAIN_NAME, null);
    return overriddenDomainName != null ? overriddenDomainName : super.getDomainName();
  }

  /**
   * Overrides the domain name for this config.
   *
   * @param domainName the domainName value
   */
  public void setDomainName(String domainName) {
    SharedPreferences.Editor editor = mOverridesSharedPref.edit();
    if (domainName == null || domainName.isEmpty()) {
      editor.remove(PREF_DOMAIN_NAME);
    } else {
      editor.putString(PREF_DOMAIN_NAME, domainName);
    }
    editor.commit();
  }

  /**
   * Returns the security level.
   *
   * @return the overridden SecurityLevel value or the original one if not overridden.
   */
  public MMXClient.SecurityLevel getSecurityLevel() {
    String securityLevelStr = mOverridesSharedPref.getString(PREF_SECURITY_LEVEL, null);
    return securityLevelStr != null ?
            MMXClient.SecurityLevel.valueOf(securityLevelStr) : super.getSecurityLevel();
  }

  /**
   * Overrides the security level for this config.
   *
   * @param securityLevel the SecurityLevel value
   */
  public void setSecurityLevel(MMXClient.SecurityLevel securityLevel) {
    SharedPreferences.Editor editor = mOverridesSharedPref.edit();
    if (securityLevel == null) {
      editor.remove(PREF_SECURITY_LEVEL);
    } else {
      editor.putString(PREF_SECURITY_LEVEL, securityLevel.name());
    }
    editor.commit();
  }

  /**
   * Resets the overrides for this configuration name.
   */
  public void reset() {
    SharedPreferences.Editor editor = mOverridesSharedPref.edit();
    editor.clear();
    editor.commit();
  }
}
