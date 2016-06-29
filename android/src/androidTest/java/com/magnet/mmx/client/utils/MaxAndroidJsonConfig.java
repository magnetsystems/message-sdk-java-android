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
import com.magnet.mmx.client.common.Log;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class MaxAndroidJsonConfig extends MaxAndroidConfig {
  private static final String TAG = MaxAndroidJsonConfig.class.getSimpleName();
  private final ConfigJson configJson;
  private Map<String, String> configMap;
  private final String mBaseUrl;
  private static final String MOBILE_CONFIG_PROPFILE = "mobileconfig.properties";
  private static final String KEY_BASEURL = "mms-application-endpoint";
  private static final String MAX_SERVER_URL = "http://10.0.3.2:8443/api";  // Genymotion environment

  public MaxAndroidJsonConfig(Context context, int resId) {
    //mContext = context;
    InputStream is = context.getResources().openRawResource(resId);
    Gson gson = new Gson();
    configJson = gson.fromJson(new InputStreamReader(is), ConfigJson.class);

    // Get a configurable property from SD Card.
    Properties props = new Properties();
    String primarySD = System.getenv("EXTERNAL_STORAGE");
    if (primarySD != null) {
      FileReader reader = null;
      String path = primarySD + "/" + MOBILE_CONFIG_PROPFILE;
      try {
        reader = new FileReader(path);
        props.load(reader);
      } catch (IOException e) {
        Log.i(TAG, "Cannot load "+path+"; use built-in configurations");
      } finally {
        try {
          reader.close();
        } catch (IOException e) {
          // Ignored.
        }
      }
    }
    mBaseUrl = props.getProperty(KEY_BASEURL, MAX_SERVER_URL);
  }

  @Override public String getBaseUrl() {
    return mBaseUrl;
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
