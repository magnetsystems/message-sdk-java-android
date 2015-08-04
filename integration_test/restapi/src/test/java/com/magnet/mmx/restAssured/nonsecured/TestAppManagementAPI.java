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
package com.magnet.mmx.restAssured.nonsecured;

import com.google.gson.GsonBuilder;
import com.jayway.restassured.RestAssured;
import com.jayway.restassured.response.Response;
import com.magnet.mmx.protocol.AppCreateRequest;
import com.magnet.mmx.restAssured.utils.TestUtils;
import org.junit.Before;
import org.junit.Test;

import java.util.logging.Logger;

import static com.jayway.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;

public class TestAppManagementAPI {

  private static final Logger LOGGER = Logger.getLogger(TestAppManagementAPI.class.getName());
  private static final String APP_RESOURCE = "/mmxadmin/rest/v1/apps";
    @Before
    public void setUp() {
        RestAssured.baseURI = TestUtils.HTTPBaseUrl;
        RestAssured.port = 6060;
        RestAssured.basePath = "";
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }

    @Test
    public void test01AppCreateGetAndDelete() {
      AppCreateRequest request = new AppCreateRequest();
      String name = "Hello World";
      String ownerEmail = "tester@magnet.com";
      String guestSecret = "supersecret";

      request.setGuestSecret(guestSecret);
      request.setName(name);
      request.setOwnerEmail(ownerEmail);
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
  public void test01AppCreateWithBadGoogleAPIKey() {
    AppCreateRequest request = new AppCreateRequest();
    String name = "Hello World";
    String ownerEmail = "tester@magnet.com";
    String guestSecret = "supersecret";
    String googleAPIKey = "bogus";

    request.setGuestSecret(guestSecret);
    request.setName(name);
    request.setOwnerEmail(ownerEmail);
    request.setOwnerId(TestUtils.appOwner);
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
            post("/mmxadmin/rest/v1/apps");
    LOGGER.info(responsePost.asString());

    responsePost.then().
        statusCode(400)
        .body("code", equalTo("INVALID_GOOGLE_API_KEY"))
        .body("message", containsString("Supplied Google API Key is invalid"));
  }


  @Test
  public void test02AppCreateAndGetConfigurationAndDelete() {
    AppCreateRequest request = new AppCreateRequest();
    String name = "Configuration Test";
    String ownerEmail = "tester@magnet.com";
    String guestSecret = "supersecret";

    request.setGuestSecret(guestSecret);
    request.setName(name);
    request.setOwnerEmail(ownerEmail);
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

}
