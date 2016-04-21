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

public class MaxAndroidJsonConfig extends MaxAndroidConfig {
  private ConfigJson configJson;
  private Map<String, String> configMap;

  public MaxAndroidJsonConfig(Context context, int resId) {
    //mContext = context;
    InputStream is = context.getResources().openRawResource(resId);
    Gson gson = new Gson();
    configJson = gson.fromJson(new InputStreamReader(is), ConfigJson.class);
  }

  @Override public String getBaseUrl() {
    return "http://192.168.101.141:8443/api";
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
