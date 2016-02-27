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
package com.magnet.mmx.restAssured.nonsecured;

import com.google.gson.GsonBuilder;
import com.jayway.restassured.RestAssured;
import com.jayway.restassured.response.Response;
import com.magnet.mmx.protocol.AppEntity;
import com.magnet.mmx.protocol.AppInfo;
import com.magnet.mmx.restAssured.utils.TestUtils;

import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Test;

import java.util.logging.Logger;

import static com.jayway.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;

/**
 * App Management integration test.  It tests both original version API (apps)
 * and newer version API (integration/apps); both API's use the same internal
 * MMXAppManager to perform the operations.  Mostly the request and response
 * to these two API's are same, except old update request is AppEntity and new
 * update request is AppInfo.  But the plan is to deprecate the "apps" API and
 * use token-based authentication for "integration/apps" API.
 */
public class TestAppManagementAPI {

  private static final Logger LOGGER = Logger.getLogger(TestAppManagementAPI.class.getName());
  private static final String APP_RESOURCE = "/mmxadmin/rest/v1/apps";
  private static final String INT_APP_RESOURCE = "/mmxadmin/rest/v1/integration/apps";

    @Before
    public void setUp() {
        RestAssured.baseURI = TestUtils.HTTPBaseUrl;
        RestAssured.port = 6060;
        RestAssured.basePath = "";
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }

    @Test
    public void test01AppCreateGetAndDelete() {
      AppInfo request = new AppInfo();
      String name = "Hello World";
      String ownerEmail = "tester@magnet.com";
      String guestSecret = "supersecret";

      request.setGuestSecret(guestSecret);
      request.setName(name);
      request.setOwnerEmail(ownerEmail);
      request.setOwnerId(TestUtils.appOwner);
      request.setServerUserId(TestUtils.serveruser);
      GsonBuilder builder = new GsonBuilder();
      String payload = builder.create().toJson(request);

      Response responsePost =
                given().log().all().
                        authentication().preemptive().basic(TestUtils.user, TestUtils.pass).
                        contentType(TestUtils.JSON).
                        headers(TestUtils.toHeaders(TestUtils.mmxApiHeaders)).
                        body(payload).
                when().
                        post(APP_RESOURCE);
      LOGGER.info(responsePost.asString());

        String appId = responsePost.then().
                statusCode(201)
                .body("name", equalTo(name))
                 .extract()
                 .path("appId");

      Response responseGet =
          given().log().all().
              authentication().preemptive().basic(TestUtils.user, TestUtils.pass).
              contentType(TestUtils.JSON).
              headers(TestUtils.toHeaders(TestUtils.mmxApiHeaders)).
              when().
              get(APP_RESOURCE + "/" + appId);

      responseGet.then()
          .body("name", equalTo(name))
          .body("appId", equalTo(appId))
          .body("apnsCertUploaded", equalTo(Boolean.FALSE));


      Response responseDelete =
          given().log().all().
              authentication().preemptive().basic(TestUtils.user, TestUtils.pass).
              contentType(TestUtils.JSON).
              headers(TestUtils.toHeaders(TestUtils.mmxApiHeaders)).
              when().
              delete(APP_RESOURCE + "/" + appId);
      LOGGER.info(responseDelete.asString());

      responseDelete.then().
          statusCode(200);
    }

  @Test
  public void test02AppCreateWithBadGoogleAPIKey() {
    AppInfo request = new AppInfo();
    String name = "Hello World";
    String ownerEmail = "tester@magnet.com";
    String guestSecret = "supersecret";
    String googleAPIKey = "bogus";

    request.setGuestSecret(guestSecret);
    request.setName(name);
    request.setOwnerEmail(ownerEmail);
    request.setOwnerId(TestUtils.appOwner);
    request.setServerUserId(TestUtils.serveruser);
    request.setGoogleApiKey(googleAPIKey);
    GsonBuilder builder = new GsonBuilder();
    String payload = builder.create().toJson(request);

    Response responsePost =
        given().log().all().
            authentication().preemptive().basic(TestUtils.user, TestUtils.pass).
            contentType(TestUtils.JSON).
            headers(TestUtils.toHeaders(TestUtils.mmxApiHeaders)).
            body(payload).
            when().
            post(APP_RESOURCE);
    LOGGER.info(responsePost.asString());

    responsePost.then().
        statusCode(400)
        .body("code", equalTo("INVALID_GOOGLE_API_KEY"))
        .body("message", containsString("Supplied Google API Key is invalid"));
  }


  @Test
  public void test03AppCreateAndGetConfigurationAndDelete() {
    AppInfo request = new AppInfo();
    String name = "Configuration Test";
    String ownerEmail = "tester@magnet.com";
    String guestSecret = "supersecret";

    request.setGuestSecret(guestSecret);
    request.setName(name);
    request.setOwnerEmail(ownerEmail);
    request.setServerUserId(TestUtils.serveruser);
    request.setOwnerId(TestUtils.appOwner);
    GsonBuilder builder = new GsonBuilder();
    String payload = builder.create().toJson(request);

    Response responsePost =
        given().log().all().
            authentication().preemptive().basic(TestUtils.user, TestUtils.pass).
            contentType(TestUtils.JSON).
            headers(TestUtils.toHeaders(TestUtils.mmxApiHeaders)).
            body(payload).
            when().
            post(APP_RESOURCE);
    LOGGER.info(responsePost.asString());

    String appId = responsePost.then().
        statusCode(201)
        .body("name", equalTo(name))
        .extract()
        .path("appId");

    Response configurationGetResponse =
        given().log().all().
            authentication().preemptive().basic(TestUtils.user, TestUtils.pass).
            contentType(TestUtils.JSON).
            headers(TestUtils.toHeaders(TestUtils.mmxApiHeaders)).
            when().
            get(APP_RESOURCE + "/" + appId + "/configurations");

    LOGGER.info("configurationGetResponse: " + configurationGetResponse.asString());

        configurationGetResponse.then().
            body("get(0).key", equalTo("mmx.wakeup.mute.minutes")).
            body("get(0).value", equalTo("30"));

    Response responseDelete =
        given().log().all().
            authentication().preemptive().basic(TestUtils.user, TestUtils.pass).
            contentType(TestUtils.JSON).
            headers(TestUtils.toHeaders(TestUtils.mmxApiHeaders)).
            when().
            delete(APP_RESOURCE + "/" + appId);
    LOGGER.info(responseDelete.asString());

    responseDelete.then().
        statusCode(200);
  }

  /**
   * CRUD test using the .../mmxadmin/rest/v1/integration/apps URL (actually
   * it is used by MMX 2.x)
   */
  @Test
  public void test04AppCreateGetUpdateAndDelete() {
    AppInfo appInfo = new AppInfo();
    String name = "App CRUD Test";
    String ownerEmail = "tester@magnet.com";
    String guestSecret = "supersecret";
    String googleAPIKey = "AIzaSyAdWMnq-33UuP4eoVhg9H3Qf2sRIGk0fZI";
    String googleProjectId = "148651144468";

    // Create app with AppInfo as request and AppEntity as response
    appInfo.setGuestSecret(guestSecret);
    appInfo.setName(name);
    appInfo.setOwnerEmail(ownerEmail);
    appInfo.setServerUserId(TestUtils.serveruser);
    appInfo.setOwnerId(TestUtils.appOwner);
    GsonBuilder builder = new GsonBuilder();
    String payload = builder.create().toJson(appInfo);

    Response responsePost =
        given().log().all()
          .authentication()
              .preemptive().basic(TestUtils.user, TestUtils.pass)
          .contentType(TestUtils.JSON)
          .headers(TestUtils.toHeaders(TestUtils.mmxApiHeaders))
          .body(payload)
        .when()
          .post(INT_APP_RESOURCE);
    LOGGER.info("create App: "+responsePost.asString());

    String appId = responsePost.then()
          .statusCode(201)
          .body("name", equalTo(name))
          .body("googleAPIKey", CoreMatchers.nullValue())
          .body("googleProjectId", CoreMatchers.nullValue())
        .extract()
          .path("appId");

    // Update app with AppInfo as request and AppEntity as response
    appInfo.setAppId(appId);
    appInfo.setGoogleApiKey(googleAPIKey);
    appInfo.setGoogleProjectId(googleProjectId);
    payload = builder.create().toJson(appInfo);

    Response responsePut =
        given().log().all()
          .authentication()
              .preemptive().basic(TestUtils.user, TestUtils.pass)
          .contentType(TestUtils.JSON)
          .headers(TestUtils.toHeaders(TestUtils.mmxApiHeaders))
          .body(payload)
        .when()
          .put(INT_APP_RESOURCE);
    LOGGER.info("update App: " + responsePut.asString());

    responsePut.then()
        .statusCode(201)
        .body("googleAPIKey", equalTo(googleAPIKey))
        .body("googleProjectId", equalTo(googleProjectId));

    // Get app with AppEntity as response
    Response responseGet =
        given().log().all()
          .authentication().preemptive().basic(TestUtils.user, TestUtils.pass)
          .contentType(TestUtils.JSON)
          .headers(TestUtils.toHeaders(TestUtils.mmxApiHeaders))
        .when()
          .get(INT_APP_RESOURCE + "/" + appId);

    LOGGER.info("get App: " + responseGet.asString());

    // TODO: shouldn't the status code be 200 instead of 201?
    responseGet.then()
        .statusCode(201)
        .body("googleAPIKey", equalTo(googleAPIKey))
        .body("googleProjectId", equalTo(googleProjectId));

    // Delete app
    Response responseDelete =
        given().log().all()
          .authentication()
              .preemptive().basic(TestUtils.user, TestUtils.pass)
          .contentType(TestUtils.JSON)
          .headers(TestUtils.toHeaders(TestUtils.mmxApiHeaders))
        .when()
          .delete(INT_APP_RESOURCE + "/" + appId);
    LOGGER.info("delete App: " + responseDelete.asString());

    responseDelete.then().
        statusCode(200);
  }
}
