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
import com.jayway.restassured.specification.RequestSpecification;
import com.magnet.mmx.protocol.TagListHolder;
import com.magnet.mmx.restAssured.utils.TestUtils;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.logging.Logger;

import static com.jayway.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;

/**
 * For testing device search rest API.
 */
public class TestDeviceSearchAPI {
  private static final Logger LOGGER = Logger.getLogger(TestDeviceSearchAPI.class.getName());

  @Before
  public void setUp() {
    RestAssured.baseURI = TestUtils.HTTPBaseUrl;
    RestAssured.port = 5220;
    RestAssured.basePath = "/mmxmgmt/api/v1/";
    RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
  }

  @Test
  public void test01Search_UsingOSType() {
    Integer expectedTotal = Integer.valueOf(7);
    String osType = "IOS";
    String responseGet =
        given().log().all().
            contentType(TestUtils.JSON).
            headers(TestUtils.toHeaders(TestUtils.mmxApiHeaders)).
            when()
            .param("os_type", osType)
            .get("devices").
            then().
            statusCode(200).
            body("total", equalTo(expectedTotal)).
            body("results[0].deviceId", equalTo("9D49D5B1-8694-48A3-9D00-EC00ACE25794")).
            body("results.size()", is(expectedTotal)).
            extract().asString();
  }

  @Test
  public void test01Search_UsingStatus() {
    Integer expectedTotal = Integer.valueOf(14);
    String status = "ACTIVE";
    String responseGet =
        given().log().all().
            contentType(TestUtils.JSON).
            headers(TestUtils.toHeaders(TestUtils.mmxApiHeaders)).
            when()
            .param("status", status)
            .get("devices").
            then().
            statusCode(200).
            body("total", equalTo(expectedTotal)).
            body("results.size()", is(expectedTotal)).
            extract().asString();
  }

  @Test
  public void test01Search_UsingMixedCaseStatusValue() {
    Integer expectedTotal = Integer.valueOf(14);
    String status = "actIve";
    String responseGet =
        given().log().all().
            contentType(TestUtils.JSON).
            headers(TestUtils.toHeaders(TestUtils.mmxApiHeaders)).
            when()
            .param("status", status)
            .get("devices").
            then().
            statusCode(200).
            body("total", equalTo(expectedTotal)).
            body("results.size()", is(expectedTotal)).
            extract().asString();
  }

  @Test
  public void test01Search_WithPagination() {
    Integer expectedTotal = Integer.valueOf(14);
    Integer pageSize = Integer.valueOf(5);
    Integer offset = Integer.valueOf(0);
    String status = "actIve";
    String responseGet =
        given().log().all().
            contentType(TestUtils.JSON).
            headers(TestUtils.toHeaders(TestUtils.mmxApiHeaders)).
            when()
            .param("status", status)
            .param(TestUtils.SIZE_PARAM, pageSize)
            .param(TestUtils.OFFSET_PARAM, offset)
            .get("devices").
            then().
            statusCode(200).
            body("total", equalTo(expectedTotal)).
            body("results.size()", is(pageSize)).
            extract().asString();
  }

  @Test
  public void testSearch_With_Tags() {
    TagListHolder holder = new TagListHolder();
    String[] appliedTags = {"secure", "office", "test"};
    holder.setTags(Arrays.asList(appliedTags));
    GsonBuilder builder = new GsonBuilder();
    String payload = builder.create().toJson(holder);
    /**
     * Apply some tags to a device first
     */
    String responsePost =
        given().log().all().
            contentType(TestUtils.JSON).
            headers(TestUtils.toHeaders(TestUtils.mmxApiHeaders)).
            body(payload).
            when().post("devices/" + TestUtils.DEVICE_ID_1 + "/tags").
            then().
            statusCode(201).
            body(containsString("Successfully Created Tags")).
            extract().asString();
    LOGGER.info(responsePost);

    /**
     * search using those tags
     */
    Integer expectedTotal = Integer.valueOf(1);
    RequestSpecification requestSpecification =
        given().log().all().
            contentType(TestUtils.JSON).
            headers(TestUtils.toHeaders(TestUtils.mmxApiHeaders));

    String[] searchTags = {appliedTags[0].toUpperCase(), "Random", "other"};

    for (int i=0; i<searchTags.length; i++) {
      requestSpecification.param("tag", searchTags[i]);
    }
    requestSpecification.get("devices").
            then().
            statusCode(200).
            body("total", equalTo(expectedTotal)).
            body("results.size()", is(expectedTotal)).
            body("results[0].deviceId", equalTo(TestUtils.DEVICE_ID_1)).
            extract().asString();
    /**
     * remove the tags
     */
    String responseDelete =
        given().log().all().
            contentType(TestUtils.JSON).
            headers(TestUtils.toHeaders(TestUtils.mmxApiHeaders)).
            when().
            delete("devices/" + TestUtils.DEVICE_ID_1 + "/tags").
            then().
            statusCode(200).
            extract().asString();
    LOGGER.info(responseDelete);
  }
}
