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
import junit.framework.Assert;
import junit.framework.TestCase;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import static com.jayway.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.*;

@Ignore("Only works with real device tokens")
public class TestSendPushAPI extends TestCase {
    @Before
    public void setUp() {
        RestAssured.baseURI = TestUtils.HTTPBaseUrl;
        RestAssured.port = 5220;
        RestAssured.basePath = "/mmxmgmt/api/v1/";
    }

    @Test
    public void testPostBasicPush() {
        String payLoad = "{\n" +
                "    \"target\": {\n" +
                "        \"deviceIds\":[" +
                "\"" + TestUtils.DEVICE_ID_1 + "\", \"d2\", \"d3\"]\n" +
                "    },\n" +
                "    \"options\" :{\n" +
                "        \"ttl\": 3600\n" +
                "    },\n" +
                "    \"body\":\"Test Push Message\",\n" +
                "    \"title\" : \"Hello world\",\n" +
                "    \"sound\" : \"chime.aiff\",\n" +
                "    \"custom\" : {\"action\":\"dosomething\", \n" +
                "                \"url\":\"http://preview.magnet.com\"\n" +
                "                },\n" +
                "    \"android\": {\"icon\":\"min-icon\"},\n" +
                "    \"ios\": {\"badge\":\"1\" ,\"silent\":\"true\", \"category\":\"Invite\"}\n" +
                "}";
        System.out.println(payLoad);

        String response =
                given().log().all().
                        contentType(TestUtils.JSON).
                        headers(TestUtils.toHeaders(TestUtils.mmxApiHeaders)).
                        body(payLoad).
                when().
                        post("send_push").
                then().
                        statusCode(200).
                        body("count.requested", equalTo(1)).
                        body("count.sent", equalTo(1)).
                        body("count.unsent", equalTo(0)).
                        body("sentList.deviceId", hasItem(TestUtils.DEVICE_ID_1)).
                        body("sentList.pushId", notNullValue()).
                extract().asString();
    }

    public void testPost_GetBasicPush() {
        String payLoad = "{\n" +
                "    \"target\": {\n" +
                "        \"deviceIds\":[" +
                "\"" + TestUtils.DEVICE_ID_1 + "\", \"d2\", \"d3\"]\n" +
                "    },\n" +
                "    \"options\" :{\n" +
                "        \"ttl\": 3600\n" +
                "    },\n" +
                "    \"body\":\"Test Push Message\",\n" +
                "    \"title\" : \"Hello world\",\n" +
                "    \"sound\" : \"chime.aiff\",\n" +
                "    \"custom\" : {\"action\":\"dosomething\", \n" +
                "                \"url\":\"http://preview.magnet.com\"\n" +
                "                },\n" +
                "    \"android\": {\"icon\":\"min-icon\"},\n" +
                "    \"ios\": {\"badge\":\"1\" ,\"silent\":\"true\", \"category\":\"Invite\"}\n" +
                "}";
        System.out.println(payLoad);

        String pushId =
                given().log().all().
                        contentType(TestUtils.JSON).
                        headers(TestUtils.toHeaders(TestUtils.mmxApiHeaders)).
                        body(payLoad).
                when().
                        post("send_push").
                        then().
                        statusCode(200).
                        body("count.requested", equalTo(1)).
                        body("count.sent", equalTo(1)).
                        body("count.unsent", equalTo(0)).
                        body("sentList.deviceId", hasItem(TestUtils.DEVICE_ID_1)).
                        body("sentList.pushId", notNullValue()).
                        extract().path("sentList.pushId[0]").toString();
        System.out.println(pushId);
        String responseGet =
                given().log().all().
                        contentType(TestUtils.JSON).
                        headers(TestUtils.toHeaders(TestUtils.mmxApiHeaders)).
                when().
                        get("push").
                then().
                        statusCode(200).
                        extract().path("results").toString();
        Assert.assertTrue(responseGet.contains(pushId));

        System.out.println(responseGet);
    }
}
