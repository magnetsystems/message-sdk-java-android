/*   Copyright (c) 2016 Magnet Systems, Inc.
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
package com.magnet.max.common;

import java.util.Map;

/**
 * The response from the application authentication.
 */
public class AppAuthResult {
  private String scope;
  private String token_type;
  private String access_token;
  private String expires_in;
  private Map<String, String> config;

  public String getScope() {
    return scope;
  }

  public String getTokenType() {
    return token_type;
  }

  public String getAccessToken() {
    return access_token;
  }

  public int getExpiresIn() {
    return Integer.parseInt(expires_in);
  }

  public String getMmxDomain() {
    return config.get("mmx-domain");
  }

  public String getMmxHost() {
    return config.get("mmx-host");
  }

  public String getMmxPort() {
    return config.get("mmx-port");
  }

  /**
   * Get the MMX App ID.
   */
  public String getMmxAppId() {
    return config.get("mmx_app_id");
  }

  /**
   * Check if TLS is required by the server.  If the security policy is not
   * specified or is NONE, TLS is not required.
   */
  public boolean isMmxTlsEnabled() {
    String secPolicy = getMmxSecurityPolicy();
    return (secPolicy != null) && !("NONE".equals(secPolicy));
  }

  /**
   * Get the TLS security policy.  NONE means no TLS.  STRICT means
   * Strict TLS (no-middle-attack), RELAXED means standard TLS.
   * @return NONE, RELAXED or STRICT
   */
  public String getMmxSecurityPolicy() {
    return config.get("security-policy");
  }

  /**
   * Get the mobile configuration.
   */
  public Map<String, String> getConfig() {
    return config;
  }
}
