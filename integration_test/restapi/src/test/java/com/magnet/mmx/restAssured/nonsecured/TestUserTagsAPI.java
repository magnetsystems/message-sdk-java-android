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

import com.jayway.restassured.RestAssured;
import com.magnet.mmx.restAssured.utils.TestUtils;
import org.junit.Before;
import org.junit.Test;

import static com.jayway.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;

public class TestUserTagsAPI {
    @Before
    public void setUp() {
        RestAssured.baseURI = TestUtils.HTTPBaseUrl;
        RestAssured.port = 5220;
        RestAssured.basePath = "/mmxmgmt/api/v1/";
      RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }

    @Test
    public void test01Add_Get_DeleteUserTag() {
        String payload = "{\n" +
                "  \"tags\": [\n" +
                "    \"user tag\",\n" +
                "    \"tag\",\n" +
                "    \"test\"\n" +
                "  ]\n" +
                "}";

        // Create User tags
        String responsePost =
                given().log().all().
                        contentType(TestUtils.JSON).
                        headers(TestUtils.toHeaders(TestUtils.mmxApiHeaders)).
                        body(payload).
                when().
                        post("users/" + TestUtils.USER_ID_1 + "/tags").
                            then().
                                statusCode(201).
                                body(containsString("Successfully Created Tags")).
                                extract().asString();
        System.out.println(responsePost);

        // Get User tags
        String responseGet =
                given().log().all().
                        contentType(TestUtils.JSON).
                        headers(TestUtils.toHeaders(TestUtils.mmxApiHeaders)).
                        when().
                        queryParam("usernames", TestUtils.USER_ID_1).
                        get("users/" + TestUtils.USER_ID_1 + "/tags").
                        then().
                        statusCode(200).
                        body("tags", hasItems("user tag", "tag", "test")).
                        body("tags.size()", is(3)).
                        extract().asString();
        System.out.println(responseGet);

        //Delete Uesr tags
      String responseDelete =
          given().log().all().
                        contentType(TestUtils.JSON).
              headers(TestUtils.toHeaders(TestUtils.mmxApiHeaders)).
              when().
              delete("users/" + TestUtils.USER_ID_1 + "/tags").
                        then().
                        statusCode(200).
                        extract().asString();
        System.out.println(responseDelete);

        String responseGetAfterDelete =
                given().log().all().
                        contentType(TestUtils.JSON).
                        headers(TestUtils.toHeaders(TestUtils.mmxApiHeaders)).
                        when().
                        get("users/" + TestUtils.USER_ID_1 + "/tags").
                        then().
                        statusCode(200).
                        body(containsString("[]")).
                        extract().asString();
    }
}
