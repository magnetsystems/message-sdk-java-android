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
import junit.framework.TestCase;
import org.junit.Before;
import org.junit.Test;

import static com.jayway.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.*;

public class TestSendMessageAPI extends TestCase {
    @Before
    public void setUp() {
        RestAssured.baseURI = TestUtils.HTTPBaseUrl;
        RestAssured.port = 5220;
        RestAssured.basePath = "/mmxmgmt/api/v1/";
      RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }

    @Test
    public void testPostBasicMessageToUser() {
        String payLoad = "{\"recipientUsernames\":" +
                "[\"" + TestUtils.USER_ID_1 +
                "\"],\n" +
                "\"content\":\"this is a simple message\",\n" +
                "\"receipt\":true,\n" +
                "\"metadata\":{\"content-type\":\"text\",\"content-encoding\":\"simple\"}}";

        String response = given().
                log().all().
                contentType(TestUtils.JSON).
                headers(TestUtils.toHeaders(TestUtils.mmxApiHeaders)).
                body(payLoad).
        when().
                post("send_message").
        then().
                statusCode(200).
                body("count.sent", equalTo(1)).
                body("count.requested", equalTo(1)).
                body("count.unsent", equalTo(0)).
                body("sentList.messageId", notNullValue()).
                body("sentList.deviceId", hasItem(nullValue())).
                body("sentList.recipientUsername", hasItem(TestUtils.USER_ID_1)).
        extract().asString();

        System.out.println(response);
    }

    @Test
    public void testPostBasicMessageToDevice() {
        String payLoad = "{\"deviceId\":" +
                "\"" + TestUtils.DEVICE_ID_1 +
                "\",\n" +
                "\"content\":\"this is a simple message\",\n" +
                "\"receipt\":true,\n" +
                "\"metadata\":{\"content-type\":\"text\",\"content-encoding\":\"simple\"}}";

        String response = given().
                log().all().
                contentType(TestUtils.JSON).
                headers(TestUtils.toHeaders(TestUtils.mmxApiHeaders)).
                body(payLoad).
                when().
                post("send_message").
                then().
                statusCode(200).
                body("count.sent", equalTo(1)).
                body("count.requested", equalTo(1)).
                body("count.unsent", equalTo(0)).
                body("sentList.messageId", notNullValue()).
                body("sentList.deviceId", hasItem(TestUtils.DEVICE_ID_1)).
//                body("sentList.recipientUsername", hasItem("daniel")).
                extract().asString();

        System.out.println(response);
    }

    @Test
    public void testGetMessageById() {
      String payLoad = "{\"recipientUsernames\":" +
                "[\"" + TestUtils.USER_ID_1 +
                "\"],\n" +
                "\"content\":\"this is a simple message\",\n" +
                "\"receipt\":true,\n" +
                "\"metadata\":{\"content-type\":\"text\",\"content-encoding\":\"simple\"}}";

        String messageId =
                given().log().all().
                    contentType(TestUtils.JSON).
                headers(TestUtils.toHeaders(TestUtils.mmxApiHeaders)).
                body(payLoad).
        when().log().all().
                post("send_message").
        then().log().all().
                statusCode(200).
                body("count.sent", equalTo(1)).
                body("count.requested", equalTo(1)).
                body("count.unsent", equalTo(0)).
                body("sentList.messageId", notNullValue()).
                body("sentList.deviceId", hasItem(nullValue())).
                body("sentList.recipientUsername", hasItem(TestUtils.USER_ID_1)).
                extract().path("sentList.messageId[0]");
        System.out.println(messageId);

        String response =
                given().
                        log().all().
                        contentType(TestUtils.JSON).
                        headers(TestUtils.toHeaders(TestUtils.mmxApiHeaders)).
                when().
                        get("message/" + messageId).
                then().
                        statusCode(200).
                        body(containsString(messageId)).
                        extract().asString();
        System.out.println(response);
    }

  @Test
  public void testPostBasicMessageToNonExistingUsers() {
    String payLoad = "{\"recipientUsernames\":" +
        "[\"missing1\", \"missing2\""+
        "],\n" +
        "\"content\":\"this is a simple message\",\n" +
        "\"receipt\":true,\n" +
        "\"metadata\":{\"content-type\":\"text\",\"content-encoding\":\"simple\"}}";

    String response = given().
        log().all().
        contentType(TestUtils.JSON).
        headers(TestUtils.toHeaders(TestUtils.mmxApiHeaders)).
        body(payLoad).
        when().
        post("send_message").
        then().
        statusCode(200).
        body("count.sent", equalTo(0)).
        body("count.requested", equalTo(2)).
        body("count.unsent", equalTo(2)).
        body("unsentList.recipientUsername", notNullValue()).
        body("unsentList.code", hasItems(48, 48)).
        body("unsentList.message", hasItems("Username not found")).
        extract().asString();

    System.out.println(response);
  }

  @Test
  public void testPostBasicMessageUserNamesExceedingAllowedSize() {
    StringBuilder builder = new StringBuilder();
    for (int i=0;i < 1005;i++) {
      String username= "u" + i;
      if (i!=0) {
        builder.append(",");
      }
      builder.append("\"" + username + "\"");

    }
    String list = builder.toString();

    String payLoad = "{\"recipientUsernames\":" +
        "[" + list +
        "],\n" +
        "\"content\":\"this is a simple message\",\n" +
        "\"receipt\":true,\n" +
        "\"metadata\":{\"content-type\":\"text\",\"content-encoding\":\"simple\"}}";

    String response = given().
        log().all().
        contentType(TestUtils.JSON).
        headers(TestUtils.toHeaders(TestUtils.mmxApiHeaders)).
        body(payLoad).
        when().
        post("send_message").
        then().
        statusCode(400).
        body("code", equalTo(50)).
        body("message", containsString("Supplied username list exceeds the maximum allowed. At the most 1000 names are permitted")).
        extract().asString();

    System.out.println(response);
  }
}
