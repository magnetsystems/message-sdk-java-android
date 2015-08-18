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
import android.os.Environment;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

public class ClientTestConfigImpl implements MMXClientConfig {
  // this comes from seed_citest01.sql
  public static final String TEST_HOST_NAME = "192.168.101.130";
  public static final int TEST_PORT = 5222;
  public static final int TEST_REST_PORT = 5220;
  public static final MMXClient.SecurityLevel TEST_SECURITY_LEVEL = MMXClient.SecurityLevel.NONE;
  private static final String APP_ID = "kbridgjulsw";
  private static final String API_KEY = "a4c18500-adb8-4bae-a8f8-55d0c4419961";

  // edit as needed for your own mmx server
//  public static final String TEST_HOST_NAME = "192.168.101.158";
//  public static final String TEST_HOST_NAME = "10.0.2.2";

//  private static final String APP_ID = "i1qv5rgkiqs";
//  private static final String API_KEY = "67c1d536-be10-4531-8a04-4ec17e0354cd";


  private static final String GCM_SENDERID = "599981932022";
  private static final String SERVER_USERID = "app1-148ce57da48%19867695-960c-4f67-9cbd-88d0bbae3fe1";
//  private static final String GUEST_SECRET = "4e411f595c41047a179f57d8d47a45b9a5596b4269fb5bc07fed09c1497b4f257c0ebfff32413cd1";
  private static final String GUEST_SECRET = "foobar";
  private static final String SERVICE_NAME = "mmx";

  private FileBasedClientConfig mFileConfig = null;
  private static final String MMX_CONFIG_OVERRIDE_FILENAME = "mmx-debug.properties";

  public ClientTestConfigImpl(Context context) {
    File mmxDebugFile = new File(Environment.getExternalStorageDirectory(), MMX_CONFIG_OVERRIDE_FILENAME);
    if (mmxDebugFile.exists()) {
      //load the properties
      try {
        mFileConfig = new FileBasedClientConfig(context, new FileInputStream(mmxDebugFile));
      } catch (FileNotFoundException e) {
        throw new IllegalStateException(e);
      }
    }
  }

  @Override
  public String getAppId() {
    if (mFileConfig != null) {
      return mFileConfig.getAppId();
    }
    return APP_ID;
  }

  @Override
  public String getApiKey() {
    if (mFileConfig != null) {
      return mFileConfig.getApiKey();
    }
    return API_KEY;
  }

  @Override
  public String getGcmSenderId() {
    if (mFileConfig != null) {
      return mFileConfig.getGcmSenderId();
    }
    return GCM_SENDERID;
  }

  @Override
  public String getServerUser() {
    if (mFileConfig != null) {
      return mFileConfig.getServerUser();
    }
    return SERVER_USERID;
  }

  public String getAnonymousSecret() {
    if (mFileConfig != null) {
      return mFileConfig.getAnonymousSecret();
    }
    return GUEST_SECRET;
  }

  @Override
  public String getHost() {
    if (mFileConfig != null) {
      return mFileConfig.getHost();
    }
    return TEST_HOST_NAME;
  }

  @Override
  public int getPort() {
    if (mFileConfig != null) {
      return mFileConfig.getPort();
    }
    return TEST_PORT;
  }

  public int getRESTPort() {
    if (mFileConfig != null) {
      return mFileConfig.getPort();
    }
    return TEST_REST_PORT;
  }

  public String getDomainName() {
    if (mFileConfig != null) {
      return mFileConfig.getDomainName();
    }
    return SERVICE_NAME;
  }

  @Override
  public MMXClient.SecurityLevel getSecurityLevel() {
    if (mFileConfig != null) {
      return mFileConfig.getSecurityLevel();
    }
    return TEST_SECURITY_LEVEL;
  }

  @Override
  public String getDeviceId() {
    return "ClientTestConfigImpl_DEVICE_ID";
  }
}
