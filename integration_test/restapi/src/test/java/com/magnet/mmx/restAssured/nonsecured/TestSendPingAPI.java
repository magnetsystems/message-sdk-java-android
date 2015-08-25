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
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import static com.jayway.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.notNullValue;

public class TestSendPingAPI  {
    @Before
    public void setUp() {
        RestAssured.baseURI = TestUtils.HTTPBaseUrl;
        RestAssured.port = 5220;
        RestAssured.basePath = "/mmxmgmt/api/v1/";
       RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }

    @Test
    public void testPostBasicPingBadToken() {
        String payLoad = "{\n" +
                "    \"target\": {\n" +
                "        \"deviceIds\":[\"" +
                TestUtils.DEVICE_ID_1 +
                "\"]\n" +
                "    },\n" +
                "    \"options\" :{\n" +
                "        \"ttl\": 3600\n" +
                "    }\n" +
                "}";

        System.out.println(payLoad);
        String response =
                given().log().all().
                        contentType(TestUtils.JSON).
                        headers(TestUtils.toHeaders(TestUtils.mmxApiHeaders)).
                        body(payLoad).
                when().
                        post("send_ping").
                then().
                        statusCode(200).
                        body("count.requested", equalTo(1)).
                        body("count.sent", equalTo(0)).
                        body("count.unsent", equalTo(1)).
                extract().asString();

        System.out.println("^^^^^ response = " + response);
    }


    @Test
    @Ignore("Only works with real device tokens")
    public void testPost_GetBasicPing() {
        String payLoad = "{\n" +
                "    \"target\": {\n" +
                "        \"deviceIds\":[" +
                "\"" + TestUtils.DEVICE_ID_1 +
                "\"]\n" +
                "    },\n" +
                "    \"options\" :{\n" +
                "        \"ttl\": 3600\n" +
                "    }\n" +
                "}";

        System.out.println(payLoad);
        String pingId =
                given().log().all().
                        contentType(TestUtils.JSON).
                        headers(TestUtils.toHeaders(TestUtils.mmxApiHeaders)).
                            body(payLoad).
                            when().
                            post("send_ping").
                            then().
                            statusCode(200).
                            body("count.requested", equalTo(1)).
                            body("count.sent", equalTo(1)).
                            body("count.unsent", equalTo(0)).
                            body("sentList.deviceId", hasItem(TestUtils.DEVICE_ID_1)).
                            body("sentList.pushId", notNullValue()).
                            extract().path("sentList.pushId[0]").toString();

        String responseGet =
                given().log().all().
                        contentType(TestUtils.JSON).
                        headers(TestUtils.toHeaders(TestUtils.mmxApiHeaders)).
                when().
                        get("push").
                then().
                    statusCode(200).
                        extract().path("results").toString();

        Assert.assertTrue(responseGet.contains(pingId));
    }
}
