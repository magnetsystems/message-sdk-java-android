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

  public String getMmxAppId() {
    return config.get("mmx_app_id");
  }

  public boolean isMmxTlsEnabled() {
    return Boolean.parseBoolean(config.get("tls-enabled"));
  }

  public String getMmxSecurityPolicy() {
    return config.get("security-policy");
  }

  public Map<String, String> getConfig() {
    return config;
  }
}
