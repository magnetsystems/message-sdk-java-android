/*   Copyright (c) 2016 Magnet Systems, Inc.  All Rights Reserved.
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
package com.magnet.max.client;


import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.oltu.oauth2.common.exception.OAuthSystemException;

import com.magnet.max.common.AppAuthResult;
import com.magnet.max.common.Client;
import com.magnet.max.common.Device;
import com.magnet.max.common.DeviceStatus;
import com.magnet.max.common.OsType;
import com.magnet.max.common.PushAuthorityType;
import com.magnet.max.common.User;
import com.magnet.mmx.client.MMXSettings;

public class MMSClient {
  private String mOAuthClientId;
  private String mOAuthSecret;
  private String mDevUser;
  private String mDevPasswd;
  private String mUserId;

  private String mDevToken;
  private String mAppToken;
  private String mUserToken;

  private transient final MaxRestServiceImpl mMaxService;
  private transient MMXSettings mSettings;
  private transient User mUser;
  private transient Device mDevice;
  private transient MMSAdmin mAdmin;

  public class MMSAdmin {

    MMSAdmin() {
    }

    /**
     * Create an application.  Use {@link #saveApp(Client, MMXSettings)} to
     * persist the application into the settings.
     * @param appName
     * @param description
     * @param appPasswd
     * @param ownerEmail
     * @return
     * @throws OAuthSystemException
     * @throws IOException
     * @throws MaxServiceException
     */
    public Client createApp(String appName, String description, String appPasswd,
                            String ownerEmail)
        throws OAuthSystemException, IOException, MaxServiceException {
      Client client = new Client();
      client.setClientName(appName);
      client.setClientDescription(description);
      client.setOwnerEmail(ownerEmail);
      mMaxService.createApp(mDevToken, client, appPasswd);
      return getApp(client.getOauthClientId());
    }

    /**
     * Persist the application information to a settings.
     * @param client
     * @param settings
     */
    public void saveApp(Client client, MMXSettings settings) {
      settings.setString(MMXSettings.PROP_MMSOAUTHCLIENTID, client.getOauthClientId());
      settings.setString(MMXSettings.PROP_MMSOAUTHSECRET, client.getOauthSecret());
      settings.setString(MMXSettings.PROP_APPID, client.getMmxAppId());
      settings.save();
    }

    public Client getApp(String clientId) throws IOException, MaxServiceException {
      return mMaxService.getApp(mDevToken, clientId);
    }

    public User getUserByName(String userName)
                              throws IOException, MaxServiceException {
      List<User> users = mMaxService.getUsersByUserNames(mDevToken,
          Arrays.asList(userName));
      return (users.size() > 0) ? users.get(0) : null;
    }

    public List<User> getUsers(int offset, int count) throws IOException {
      List<User> users = mMaxService.getAllUsers(mDevToken, mOAuthClientId);
      if ((offset + count) > users.size()) {
        return new ArrayList<User>(0);
      }
      return users.subList(offset, offset + count);
    }

    public void uploadApnsCertificate(String clientId, String certPasswd,
                                      byte[] cert, boolean production)
                                      throws IOException, MaxServiceException {
      Client app = getApp(clientId);
      mMaxService.uploadApnsCertificate(mDevToken, app.getMmxAppId(),
                              app.getOauthClientId(), certPasswd, cert);
    }
  }

  private static boolean isEmpty(String str) {
    return (str == null) || str.isEmpty();
  }

  /**
   * Constructor with a populated settings.
   * @param settings
   */
  public MMSClient(MMXSettings settings) {
    mSettings = settings;

    String baseUrl = mSettings.getString(MMXSettings.PROP_MMSBASEURL, null);
    if (isEmpty(baseUrl)) {
      throw new SecurityException(MMXSettings.PROP_MMSBASEURL+" is not set");
    }
    mMaxService = new MaxRestServiceImpl(baseUrl);

    mDevUser = mSettings.getString(MMXSettings.PROP_MMSDEVUSER, null);
    mDevPasswd = mSettings.getString(MMXSettings.PROP_MMSDEVPASSWD, null);

    mOAuthClientId = mSettings.getString(MMXSettings.PROP_MMSOAUTHCLIENTID, null);
    mOAuthSecret = mSettings.getString(MMXSettings.PROP_MMSOAUTHSECRET, null);
    try {
      authApp();
    } catch (Throwable e) {
      throw new SecurityException("Invalid app credentials", e);
    }

//    mUserName = mSettings.getString(MMXSettings.PROP_USER, null);
//    mUserPasswd = mSettings.getString(MMXSettings.PROP_PASSWD, null);
  }

  /**
   * Constructor with an empty settings.  The calling sequence must be
   * {@link #getAdmin(String, String)} or {@link #getAdmin()},
   * {@link MMSAdmin#createApp(String, String, String, String)}
   * MMSAdmin#
   * @param settings
   * @param baseUrl
   */
  public MMSClient(MMXSettings settings, String baseUrl) {
    mSettings = settings;
    mMaxService = new MaxRestServiceImpl(baseUrl);
    mSettings.setString(MMXSettings.PROP_MMSBASEURL, baseUrl);
    mSettings.save();
  }

  public boolean isAdminAuthenticated() {
    return mDevToken != null;
  }

  public boolean isAppAuthenticated() {
    return mAppToken != null;
  }

  public boolean isUserAuthenticated() {
    return mUserToken != null;
  }

  /**
   * Authenticate an admin with new credentials.
   * @param devUser
   * @param devPasswd
   * @return
   * @throws SecurityException
   */
  public MMSAdmin getAdmin(String devUser, String devPasswd) {
    try {
      mDevToken = mMaxService.developerLogin(devUser, devPasswd);
      mDevUser = devUser;
      mDevPasswd = devPasswd;
      mSettings.setString(MMXSettings.PROP_MMSDEVUSER, mDevUser);
      mSettings.setString(MMXSettings.PROP_MMSDEVPASSWD, mDevPasswd);
      mSettings.save();
      if (mAdmin == null) {
        mAdmin = new MMSAdmin();
      }
      return mAdmin;
    } catch (Throwable e) {
      throw new SecurityException("Invalid developer credentials", e);
    }
  }

  /**
   * Get the admin using the preset credentials.  If the admin has not been
   * authenticated, it will be performed first.
   * @return
   * @throws SecurityException
   */
  public MMSAdmin getAdmin() {
    if (mAdmin == null) {
      if (isEmpty(mDevUser) || isEmpty(mDevPasswd)) {
        throw new SecurityException("Developer credentials are required: "+
            MMXSettings.PROP_MMSDEVUSER+", "+MMXSettings.PROP_MMSDEVPASSWD);
      }
      try {
        mDevToken = mMaxService.developerLogin(mDevUser, mDevPasswd);
      } catch (Throwable e) {
        throw new SecurityException("Invalid developer credentials", e);
      }
      mAdmin = new MMSAdmin();
    }
    return mAdmin;
  }

  /**
   * Authenticate the client and set it to the current application.
   * @return false if no client id or no client secret.
   * @throws MaxServiceException
   * @throws IOException
   * @see #getCurrentApp()
   */
  public boolean authApp() throws IOException, MaxServiceException {
    if (isEmpty(mOAuthClientId) || isEmpty(mOAuthSecret)) {
      return false;
    }

    Client client = new Client();
    client.setOauthClientId(mOAuthClientId);
    client.setOauthSecret(mOAuthSecret);
    AppAuthResult result = mMaxService.authenticateApp(client);
    mAppToken = result.getAccessToken();
    mSettings.setString(MMXSettings.PROP_APPID, result.getMmxAppId());
    mSettings.setString(MMXSettings.PROP_HOST, result.getMmxHost());
    mSettings.setString(MMXSettings.PROP_PORT, result.getMmxPort());
    mSettings.setBoolean(MMXSettings.PROP_ENABLE_TLS, result.isMmxTlsEnabled());
    mSettings.setString(MMXSettings.PROP_SERVICE_NAME, result.getMmxDomain());
    return true;
  }

//  // Get the current client application.
//  private Client getApp() throws IOException, MaxServiceException {
//    if (isEmpty(mOAuthClientId)) {
//      throw new SecurityException("App is not registered");
//    }
//    // MMS does not have API to get the client by appToken.
////    if (isEmpty(mAppToken)) {
////      throw new SecurityException("App is not authenticated");
////    }
////    return mMaxService.getApp(mAppToken, mOAuthClientId);
//    return getAdmin().getApp(mOAuthClientId);
//  }

  public void authUser(String userName, String userPasswd, String deviceId)
      throws IOException, MaxServiceException {
    if (!isAppAuthenticated() && !authApp()) {
      throw new SecurityException("Curernt app has no authentication info");
    }
    User user = new User();
    user.setUserName(userName);
    user.setPassword(userPasswd);
    user.setClientId(mOAuthClientId);
    mUserToken = mMaxService.getUserToken(user, deviceId);

    user = getCurrentUser();
    mUserId = user.getUserIdentifier();
    mSettings.setString(MMXSettings.PROP_MMSUSERID, mUserId);
    mSettings.save();

    Device device = new Device();
    device.setClientId(mOAuthClientId);
    device.setDeviceId(deviceId);
    device.setDeviceStatus(DeviceStatus.ACTIVE);
    device.setUserId(mUserId);
    device.setLabel(System.getProperty("os.arch"));
    device.setOsVersion(System.getProperty("os.version"));
    // Fix MAX-131 with an unsupported push type: OTHERS.
    device.setOs(OsType.OTHER);
    device.setPushAuthority(PushAuthorityType.OTHERS);
    device.setDeviceToken("");

    device.setTags(null);
    mMaxService.registerDevice(mUserToken, device);

    mUser = user;
    mDevice = device;
  }

  public String registerUser(User user) throws IOException, MaxServiceException {
    if (!isAppAuthenticated() && !authApp()) {
      throw new SecurityException("Curernt app has no authentication info");
    }
    mMaxService.registerUser(mAppToken, user);
    StringBuilder name = new StringBuilder();
    if (!isEmpty(user.getFirstName())) {
      name.append(user.getFirstName());
    }
    if (!isEmpty(user.getLastName())) {
      if (name.length() > 0) {
        name.append(' ');
      }
      name.append(user.getLastName());
    }
    mUserId = user.getUserIdentifier();
    mSettings.setString(MMXSettings.PROP_USER, user.getUserName());
    mSettings.setString(MMXSettings.PROP_NAME, name.toString());
    mSettings.setString(MMXSettings.PROP_PASSWD, user.getPassword());
    mSettings.setString(MMXSettings.PROP_EMAIL, user.getEmail());
    mSettings.setString(MMXSettings.PROP_MMSUSERID, mUserId);
    mSettings.save();
    return mUserId;
  }

  public String registerAndAuthenticate(User user, String deviceId)
      throws IOException, MaxServiceException {
    String userId = registerUser(user);
    authUser(user.getUserName(), user.getPassword(), deviceId);
    return userId;
  }

  public User getCurrentUser() throws IOException, MaxServiceException {
    if (isEmpty(mUserToken)) {
      throw new SecurityException("Current user is not authenticated");
    }
    User user = mMaxService.whoAmI(mUserToken);
    return user;
  }

  public String getUserId() {
    if (mUserId != null) {
      return mUserId;
    }
    if (mUser != null) {
      return mUser.getUserIdentifier();
    }
    return null;
  }

  public String getUserToken() {
    return mUserToken;
  }
}




