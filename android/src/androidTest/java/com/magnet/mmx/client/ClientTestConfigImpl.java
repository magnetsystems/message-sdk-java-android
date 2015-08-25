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

  public static final String TEST_HOST_NAME = "dev-mmx-002.magneteng.com";
  public static final int TEST_PORT = 5222;
  public static final int TEST_REST_PORT = 5221;
  public static final MMXClient.SecurityLevel TEST_SECURITY_LEVEL = MMXClient.SecurityLevel.RELAXED;
  private static final String APP_ID = "kcyidhkopml";
  private static final String API_KEY = "fe940713-8be4-4c2c-8ec3-3e1ac5b71f1e";
  private static final String GCM_SENDERID = null;
  private static final String SERVER_USERID = "foobar";
  private static final String GUEST_SECRET = "de953ec8-b6cd-458d-88e1-49b2acad4f89";
  private static final String SERVICE_NAME = "dev-mmx-002";

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
      return mFileConfig.getRESTPort();
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
