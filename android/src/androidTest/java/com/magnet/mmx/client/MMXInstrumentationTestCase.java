/*   Copyright (c) 2015-2016 Magnet Systems, Inc.
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

//import com.magnet.max.test.client.MMSClient;
//import com.magnet.server.sdk.bean.User;

import android.test.InstrumentationTestCase;

class MMXInstrumentationTestCase extends InstrumentationTestCase {
  protected MMXClient mmxClient;
  protected String appId;
  protected String userId;
  protected String userToken;

  protected void connect(String appName, String mmsUrl, String userName,
                         String deviceId, AbstractMMXListener listener) {
    /*
    try {
      MMSClient mmsClient = new MMSClient(mmsUrl, "developer", "developer");
      appId = mmsClient.createApp(appName, "Android Integration Test", "app-123");
      User user = new User();
      user.setUserName(userName);
      user.setPassword("test");
      user.setLastName("MyLastName");
      user.setFirstName("MyFirstName");
      user.setEmail("myemail@magnet.com");
      mmsClient.registerAndAuthenticate(user, deviceId);
      userId = mmsClient.getUserId();
      userToken = mmsClient.getUserToken();
    } catch (Throwable e) {
      throw new RuntimeException("Cannot create developer context; ask Mladen.", e);
    }

    mmxClient = MMXClient.getInstance(this.getInstrumentation().getTargetContext(),
            new ClientTestConfigImpl(this.getInstrumentation().getTargetContext()));
    MMXClient.ConnectionOptions options = new MMXClient.ConnectionOptions().setAutoCreate(false);
    mmxClient.connectWithCredentials(userId, userToken.getBytes(), listener, options);
    try {
      for (int i=0; i<50; i++) {
        Thread.sleep(100);
        if (mmxClient.isConnected()) {
          break;
        }
      }

    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    */
  }

  protected MMXClient getMMXClient() {
    return mmxClient;
  }

  protected String getAppId() {
    return appId;
  }

  protected String getUserId() {
    return userId;
  }

  protected String getUserToken() {
    return userToken;
  }
}
