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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.oltu.oauth2.common.exception.OAuthProblemException;
import org.apache.oltu.oauth2.common.exception.OAuthSystemException;

import com.google.gson.JsonObject;
import com.magnet.max.common.Client;
import com.magnet.max.common.ClientRegistrationRequest;
import com.magnet.max.common.Device;
import com.magnet.max.common.PaginatedResult;
import com.magnet.max.common.User;
import com.magnet.max.common.UserRealm;
import com.magnet.max.common.UserStatus;
import com.magnet.mmx.util.Base64;
import com.squareup.okhttp.RequestBody;

import retrofit.Response;

public class MaxRestServiceImpl extends RestService<MaxRestService> {
  private static class MaxMediaType {
    public final static String APPLICATION_FORM_URLENCODED = "application/x-www-form-urlencoded";
    public final static String APPLICATION_JSON = "application/json";
    public final static String APPLICATION_OCTET_STREAM = "application/octet-stream";
    public final static String APPLICATION_XML = "application/xml";
    public final static String MULTIPART_FORM_DATA = "multipart/form-data";
    public final static String TEXT_HTML = "text/html";
    public final static String TEXT_PLAIN = "text/plain";
    public final static String TEXT_XML = "text/xml";
  }

  public MaxRestServiceImpl(String baseUrl) {
    super(baseUrl);
  }

  public void createApp(String developerToken, Client app, String appPassword)
      throws OAuthSystemException, IOException, MaxServiceException {
    MaxRestService maxRestService = getService(developerToken);

    ClientRegistrationRequest registrationRequest = new ClientRegistrationRequest();
    registrationRequest.setClient_name(app.getClientName());
    registrationRequest.setPassword(appPassword);
    registrationRequest.setClient_description(app.getClientDescription());
    registrationRequest.setOwner_email(app.getOwnerEmail());

    Response<JsonObject> response = maxRestService.register(registrationRequest)
        .execute();
    checkResponseForErrors(response);
    if (response.body() != null && response.body().get("client_id") != null) {
      app.setOauthClientId(response.body().get("client_id").getAsString());
    }
  }

  public String developerLogin(String username, String password)
      throws IOException, OAuthSystemException, OAuthProblemException,
      MaxServiceException {
    Map<String, String> headers = new HashMap<>();
    MaxRestService maxRestService = getService(
        MaxMediaType.APPLICATION_FORM_URLENCODED, headers);
    Response<JsonObject> response = maxRestService.login(username, password)
        .execute();
    checkResponseForErrors(response);
    return response.body().get("access_token").getAsString();
  }

  public void updateApp(String developerToken, Client app, String appPassword)
      throws OAuthSystemException, IOException, MaxServiceException {
    MaxRestService maxRestService = getService(developerToken);
    ClientRegistrationRequest registrationRequest = new ClientRegistrationRequest();
    registrationRequest.setClient_name(app.getClientName());
    registrationRequest.setPassword(appPassword);
    registrationRequest.setClient_description(app.getClientDescription());
    registrationRequest.setOwner_email(app.getOwnerEmail());

    Response<JsonObject> response = maxRestService.register(registrationRequest)
        .execute();
    checkResponseForErrors(response);
    if (response.body() != null && response.body().get("client_id") != null) {
      app.setOauthClientId(response.body().get("client_id").getAsString());
    }
  }

    /**
     * Update the configuration of an app.  Possible key and value types in
     * <code>clientConfig</code> are:<table>
     * <tr><td>apnscertproduction</td><td>boolean in string</td></tr>
     * <tr><td>name</td><td>string</td></tr>
     * <tr><td>mms-application-endpoint</td><td>http://{host}:{port}/api</td></tr>
     * <tr><td>mmx-host</td><td>string</td></tr>
     * <tr><td>googleprojectid</td><td>string</td></tr>
     * <tr><td>googleapikey</td><td>string</td></tr>
     * </table>
     *
//     * @param developerToken
//     * @param oauthClientId
//     * @param clientConfig
//     * @return Map<String, String>
//     * @throws IOException
//     * @throws OAuthSystemException
//     * @throws MaxServiceException
     */
  public Map<String, String> updateAppConfig(String developerToken,
      String oauthClientId, Map<String, String> clientConfig)
          throws IOException, OAuthSystemException, MaxServiceException {
    MaxRestService maxRestService = getService(developerToken);

    Response<Map<String, String>> response = maxRestService.updateClientConfig(
        oauthClientId, clientConfig).execute();
    checkResponseForErrors(response);
    return response.body();
  }

  public String getAppToken(String developerToken, String clientId)
      throws IOException, MaxServiceException {
    MaxRestService maxRestService = getService(developerToken);

    Response<JsonObject> response = maxRestService.loginToApp(clientId)
        .execute();
    checkResponseForErrors(response);

    String cookie = response.headers().get("Set-Cookie");
    int indx = cookie.indexOf("Magnet-Dev-Console-App-Cookie=");
    return cookie.substring(indx + "Magnet-Dev-Console-App-Cookie=".length(),
        cookie.indexOf(";"));
  }

  public Client getApp(String developerToken, String clientId)
      throws IOException, MaxServiceException {
    MaxRestService maxRestService = getService(developerToken);

    Response<Client> response = maxRestService.getClient(clientId).execute();
    checkResponseForErrors(response);
    return response.body();
  }

  public Client getAppByAppNAme(String developerToken, String appName)
      throws IOException, MaxServiceException {
    MaxRestService maxRestService = getService(developerToken);

    // get all apps
    Response<List<Client>> response = maxRestService.getClients().execute();
    checkResponseForErrors(response);
    for (Client app : response.body()) {
      if (appName.equals(app.getClientName())) {
        return app;
      }
    }
    return null;
  }

//    public Client getApp(String appToken) throws IOException, MaxServiceException {
//      MaxRestService maxRestService = getService(appToken);
//      Response<Client> response = maxRestService.getClient().execute();
//      checkResponseForErrors(response);
//      return response.body();
//    }

  public String authenticateApp(Client app) throws IOException,
      MaxServiceException {
    Map<String, String> headers = new HashMap<>();
    String authorization = Base64.encodeBytes((app.getOauthClientId() +
        ":" + app.getOauthSecret()).getBytes());
    headers.put("Authorization", "Basic " + authorization);

    MaxRestService maxRestService = getService(
        MaxMediaType.APPLICATION_FORM_URLENCODED, headers);
    Response<JsonObject> response = maxRestService.authenticateApp().execute();
    checkResponseForErrors(response);

    return response.body().get("access_token").getAsString().trim();
  }

  public User whoAmI(String userToken) throws IOException, MaxServiceException {
    MaxRestService maxRestService = getService(userToken);
    Response<User> response = maxRestService.whoAmI().execute();
    checkResponseForErrors(response);
    return response.body();
  }

  public List<User> getAllUsers(String developerToken, String appId)
      throws IOException {
    MaxRestService maxRestService = getService(developerToken);
    Response<List<User>> response = maxRestService.getAllUsers(appId, 100, 0)
        .execute();
    return response.body();
  }

  // TODO ACCEPTS APP TOKEN
  public List<User> getUsersByUserNames(String developerToken,
      List<String> userNames) throws IOException, MaxServiceException {
    MaxRestService maxRestService = getService(developerToken);
    Response<List<User>> response = maxRestService.getUsersByUserNames(
        userNames).execute();
    checkResponseForErrors(response);
    return response.body();
  }

    //TODO  ACCEPTS APP TOKEN
  public List<User> getUsersByIds(String developerToken, List<String> userIds)
      throws IOException, MaxServiceException {
    MaxRestService maxRestService = getService(developerToken);
    Response<List<User>> response = maxRestService.getUsersByUserIds(userIds)
        .execute();
    checkResponseForErrors(response);
    return response.body();
  }

    //TODO  ACCEPTS APP TOKEN
  public List<User> getUserSearchByUsername(String developerToken,
      String userName) throws IOException, MaxServiceException {
    String query = "userName:" + userName;
    MaxRestService maxRestService = getService(developerToken);
    Response<PaginatedResult> response = maxRestService.searchUsers(query)
        .execute();
    checkResponseForErrors(response);
    return response.body().getResult();
  }

  public List<User> getUserSearchByUserid(String developerToken, String userId)
      throws IOException, MaxServiceException {

    String query = "userIdentifier:" + userId;
    MaxRestService maxRestService = getService(developerToken);
    Response<PaginatedResult> response = maxRestService.searchUsers(query)
        .execute();
    checkResponseForErrors(response);
    List<Map<String, Object>> list = response.body().getResult();
    List<User> userList = new ArrayList<>();
    for (Map<String, Object> map : list) {
      User user = new User();
      user.setChallengePreferences((Map<String, String>) map.get(
          "challengePreferences"));
      user.setClientId((String) map.get("clientId"));
      user.setEmail((String) map.get("email"));
      user.setExternalUserId((String) map.get("externalUserId"));
      user.setFirstName((String) map.get("firstName"));
      user.setLastName((String) map.get("lastName"));
      user.setOtpCode((String) map.get("otpCode"));
      user.setPassword((String) map.get("password"));
      user.setRoles(list2array((List<String>) map.get("roles")));
      user.setTags(list2array((List<String>) map.get("tags")));
      user.setUserAccountData((Map<String, String>) map.get("userAccountData"));
      user.setUserIdentifier((String) map.get("userIdentifier"));
      user.setUserName((String) map.get("userName"));
      user.setUserRealm(UserRealm.valueOf((String) map.get("userRealm")));
      user.setUserStatus(UserStatus.valueOf((String) map.get("userStatus")));
      userList.add(user);
    }

    // 0 = {LinkedTreeMap$Node@1630} "challengePreferences" -> "null"
    // 1 = {LinkedTreeMap$Node@1631} "clientId" ->
    // "ca9f81d4-9a11-4dbf-be92-2b0a9f48ac28"
    // 2 = {LinkedTreeMap$Node@1632} "email" -> "user-1@magnet.com"
    // 3 = {LinkedTreeMap$Node@1633} "externalUserId" -> "null"
    // 4 = {LinkedTreeMap$Node@1634} "firstName" -> "?????? ????????"
    // 5 = {LinkedTreeMap$Node@1635} "lastName" -> "?????????"
    // 6 = {LinkedTreeMap$Node@1636} "otpCode" -> "n/a"
    // 7 = {LinkedTreeMap$Node@1637} "password" -> "n/a"
    // 8 = {LinkedTreeMap$Node@1638} "roles" -> " size = 1"
    // 9 = {LinkedTreeMap$Node@1639} "tags" -> " size = 1"
    // 10 = {LinkedTreeMap$Node@1640} "userAccountData" -> " size = 0"
    // 11 = {LinkedTreeMap$Node@1641} "userIdentifier" ->
    // "8a00a8a35388478001538d4765860038"
    // 12 = {LinkedTreeMap$Node@1642} "userName" -> "user-1"
    // 13 = {LinkedTreeMap$Node@1643} "userRealm" -> "DB"
    // 14 = {LinkedTreeMap$Node@1644} "userStatus" -> "ACTIVE"
    return userList;
  }

  private static String[] list2array(List<String> list) {
    if (list == null) {
      return null;
    }
    String[] arr = new String[list.size()];
    return list.toArray(arr);
  }

  public void registerUserToApp(String developerToken, String appId, User user)
      throws IOException, MaxServiceException {
    MaxRestService maxRestService = getService(developerToken);

    user.setUserRealm(UserRealm.DB);
    user.setTags(new String[] { "test" });
    user.setRoles(new String[] { "user" });
    user.setClientId(appId);

    Response<User> response = maxRestService.addUser(user).execute();
    checkResponseForErrors(response);

    user.setUserIdentifier(response.body().getUserIdentifier());
  }

  public void registerUser(String appToken, User user) throws IOException,
      MaxServiceException {
    MaxRestService maxRestService = getService(appToken);
    user.setUserRealm(UserRealm.DB);
    user.setRoles(new String[] { "user" });
    Response<User> response = maxRestService.registerUser(user).execute();
    checkResponseForErrors(response);
    user.setUserIdentifier(response.body().getUserIdentifier());
  }

  public void registerDevice(String userToken, Device device)
      throws IOException, MaxServiceException {
    MaxRestService maxRestService = getService(userToken);

    Response<JsonObject> response = maxRestService.registerDevice(device)
        .execute();
    checkResponseForErrors(response);
  }

//    public String getUserToken(User user, String deviceId) {
//
//        HttpRequestExecutor executor = HttpRequestExecutor.withSingleClient();
//
//        String data = "grant_type=password" +
//                "&username=" + user.getUserName() +
//                "&password=" + user.getPassword() +
//                "&client_id=" + user.getClientId() +
//                "&scope=user";
//
//        com.magnet.server.intg.http.response.Response response = executor.execute(
//                RequestBuilder.postRequest(baseUrl).withPath("/api/com.magnet.server/user/session")
//                  .withHeader("Content-type", MediaType.APPLICATION_FORM_URLENCODED)
//                  .withData(data).withHeader("MMS-DEVICE-ID", deviceId).build()
//        );
//
//        Map nvPairs = new Gson().fromJson(response.getOutput(), Map.class);
//        return (String) nvPairs.get("access_token");
//    }

  public String getUserToken(User user, String deviceId) throws IOException,
      MaxServiceException {
    Map<String, String> headers = new HashMap<>();
    headers.put("MMS-DEVICE-ID", deviceId);
    MaxRestService maxRestService = getService(
        MaxMediaType.APPLICATION_FORM_URLENCODED, headers);

    Response<Map<String, Object>> response = maxRestService.authenticateUser(
        "password", // grant_type
        user.getUserName(), // username
        user.getPassword(), // password
        user.getClientId(), // client_id
        "user" // scope
    ).execute();
    checkResponseForErrors(response);
    return (String) response.body().get("access_token");
  }

  public Properties downloadProperties(String developerToken, String platform,
      String appName) throws IOException, MaxServiceException {
    MaxRestService maxRestService = getService(developerToken);

    Response response = maxRestService.downloadProperties(platform, appName);
    checkResponseForErrors(response);

    InputStream inputStream = response.raw().body().byteStream();
    Properties p = new Properties();
    p.load(inputStream);

    return p;
  }

  public void uploadApnsCertificate(String token, String mmxAppId, String appId,
      String certPassword, File certificate) throws IOException,
          MaxServiceException {
    MaxRestService maxRestService = getService(token);

    RequestBody p1 = RequestBody.create(com.squareup.okhttp.MediaType.parse(
        MaxMediaType.MULTIPART_FORM_DATA), certPassword);
    RequestBody p2 = RequestBody.create(com.squareup.okhttp.MediaType.parse(
        MaxMediaType.MULTIPART_FORM_DATA), appId);
    RequestBody p3 = RequestBody.create(com.squareup.okhttp.MediaType.parse(
        MaxMediaType.MULTIPART_FORM_DATA), certificate);

    Response<JsonObject> response = maxRestService.uploadCertificate(mmxAppId,
        p1, p2, p3).execute();
    checkResponseForErrors(response);
  }

  public void uploadApnsCertificate(String token, String mmxAppId, String appId,
      String certPassword, byte[] certificate) throws IOException,
          MaxServiceException {
    MaxRestService maxRestService = getService(token,
        MaxMediaType.APPLICATION_FORM_URLENCODED);

    RequestBody p1 = RequestBody.create(com.squareup.okhttp.MediaType.parse(
        MaxMediaType.MULTIPART_FORM_DATA), certPassword);
    RequestBody p2 = RequestBody.create(com.squareup.okhttp.MediaType.parse(
        MaxMediaType.MULTIPART_FORM_DATA), appId);
    RequestBody p3 = RequestBody.create(com.squareup.okhttp.MediaType.parse(
        MaxMediaType.MULTIPART_FORM_DATA), certificate);

    Response<JsonObject> response = maxRestService.uploadCertificate(mmxAppId,
        p1, p2, p3).execute();
    checkResponseForErrors(response);
  }

  // SERVICE
  private MaxRestService getService(String token) {
    return getService(token, MaxMediaType.APPLICATION_JSON);
  }

  private MaxRestService getService(String token, String mediaType) {
    Map<String, String> headers = new HashMap<>();
    headers.put("Authorization", "Bearer " + token);
    return getService(mediaType, headers);
  }

  private MaxRestService getService(String mediaType,
      Map<String, String> headers) {
    return super.getService(mediaType, headers, MaxRestService.class);
  }
}