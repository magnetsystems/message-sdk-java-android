/*
 * Copyright (c) 2015 Magnet Systems, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.magnet.mmx.client.utils;

import android.content.Context;
import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.magnet.max.android.config.MaxAndroidConfig;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

public class MaxAndroidJsonConfig implements MaxAndroidConfig {
  private ConfigJson configJson;
  private Map<String, String> configMap;

  public MaxAndroidJsonConfig(Context context, int resId) {
    //mContext = context;
    InputStream is = context.getResources().openRawResource(resId);
    Gson gson = new Gson();
    configJson = gson.fromJson(new InputStreamReader(is), ConfigJson.class);
  }

  @Override public String getBaseUrl() {
    return "http://10.0.3.2:8443/api";
  }

  @Override public String getClientId() {
    return configJson.getClientId();
  }

  @Override public String getClientSecret() {
    return configJson.getClientSecret();
  }

  @Override public String getScope() {
    return configJson.getScope();
  }

  @Override public Map<String, String> getAllConfigs() {
    if(null == configMap) {
      configMap = new HashMap<>();
      configMap.put("client_id", configJson.getClientId());
      configMap.put("client_secret", configJson.getClientSecret());
    }
    return configMap;
  }

  private static class ConfigJson {
    private String scope;
    @SerializedName("client_secret")
    private String clientSecret;
    @SerializedName("client_id")
    private String clientId;

    public String getScope() {
      return scope;
    }

    public String getClientSecret() {
      return clientSecret;
    }

    public String getClientId() {
      return clientId;
    }
  }
}
