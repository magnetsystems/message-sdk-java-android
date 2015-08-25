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
import com.magnet.mmx.protocol.TagListHolder;
import com.magnet.mmx.restAssured.utils.TestUtils;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.logging.Logger;

import static com.jayway.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;

public class TestDeviceTagsAPI {
  private static final Logger LOGGER = Logger.getLogger(TestDeviceTagsAPI.class.getName());

  @Before
  public void setUp() {
    RestAssured.baseURI = TestUtils.HTTPBaseUrl;
    RestAssured.port = 5220;
    RestAssured.basePath = "/mmxmgmt/api/v1/";
  }

  @Test
  public void test01Add_Get_DeleteDeviceTag() {
    TagListHolder holder = new TagListHolder();
    String[] tags = {"device tag", "tag", "test"};
    holder.setTags(Arrays.asList(tags));
    GsonBuilder builder = new GsonBuilder();
    String payload = builder.create().toJson(holder);

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

    String responseGet =
        given().log().all().
            contentType(TestUtils.JSON).
            headers(TestUtils.toHeaders(TestUtils.mmxApiHeaders)).
            when()
            .get("devices/" + TestUtils.DEVICE_ID_1 + "/tags").
            then().
            statusCode(200).
            body("deviceId", equalTo(TestUtils.DEVICE_ID_1)).
            body("tags", hasItems("device tag", "tag", "test")).
            body("tags.size()", is(3)).
            extract().asString();

    LOGGER.info(responseGet);

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

    String responseGetAfterDelete =
        given().log().all().
            contentType(TestUtils.JSON).
            headers(TestUtils.toHeaders(TestUtils.mmxApiHeaders)).
            when().
            get("devices/" + TestUtils.DEVICE_ID_1 + "/tags").
            then().
            statusCode(200).
            body(containsString("[]")).
            extract().asString();
  }
}
