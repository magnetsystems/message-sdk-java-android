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
import com.jayway.restassured.response.Response;
import com.jayway.restassured.specification.RequestSpecification;
import com.magnet.mmx.restAssured.utils.TestUtils;
import junit.framework.TestCase;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;

import static com.jayway.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.core.IsNot.not;


public class TestPubsubAPI extends TestCase {
    @Before
    public void setUp() {
        RestAssured.baseURI = TestUtils.HTTPBaseUrl;
        RestAssured.port = 5220;
        RestAssured.basePath = "/mmxmgmt/api/v1";
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }

    @Test
    public void test01CreateTopic() {
        // Create Topic
        String payload = "{\n" +
                "\"topicName\":\"testTopic1\",\n" +
                "\"description\":\"testTopic1 Desc\"\n" +
                "}";
        Response response =
                given().log().all().
                        authentication().preemptive().basic(TestUtils.user, TestUtils.pass).
                        contentType(TestUtils.JSON).
                        headers(TestUtils.toHeaders(TestUtils.mmxApiHeaders)).
                        body(payload).
                when().post("topics");
        System.out.println("^^^^^ response: " + response.asString() + "^^^^^^");

        response.then().
                statusCode(201);
    }

    @Test
    public void test02GetTopic() {
        String payload ="{\n" +
                "\"topicName\":\"testTopic2\",\n" +
                "\"description\":\"testTopic2 Desc\"\n" +
                "}";
        Response responsePost =
                given().log().all().
                        authentication().preemptive().basic(TestUtils.user, TestUtils.pass).
                        contentType(TestUtils.JSON).
                        headers(TestUtils.toHeaders(TestUtils.mmxApiHeaders)).
                        body(payload).
                when().post("topics");

        System.out.println("^^^^^ responsePost: " + responsePost.asString() + "^^^^^^");
        responsePost.then().statusCode(201);

        // Get Topic
        Response responseGet =
                given().log().all().
                        authentication().preemptive().basic(TestUtils.user, TestUtils.pass).
                        contentType(TestUtils.JSON).
                        headers(TestUtils.toHeaders(TestUtils.mmxApiHeaders)).
                when().
                        get("topics/testTopic2");
        System.out.println("^^^^^ responseGet: " + responseGet.asString() + "^^^^^^");

        responseGet.then().
                statusCode(200).
                assertThat().body("topicName", equalTo("testTopic2"));
    }

    @Test
    public void test03DeleteTopic() {
        String payload = "{\n" +
                "\"topicName\":\"testTopic3\",\n" +
                "\"description\":\"testTopic3 Desc\"\n" +
                "}";
        Response responsePost =
                given().log().all().
                        authentication().preemptive().basic(TestUtils.user, TestUtils.pass).
                        contentType(TestUtils.JSON).
                        headers(TestUtils.toHeaders(TestUtils.mmxApiHeaders)).
                        body(payload).
                        when().
                        post("topics");
        System.out.println("^^^^^ responsePost: " + responsePost.getStatusCode() + "^^^^^^");

        Response responseDelete =
                given().log().all().
                        authentication().preemptive().basic(TestUtils.user, TestUtils.pass).
                        contentType(TestUtils.JSON).
                        headers(TestUtils.toHeaders(TestUtils.mmxApiHeaders)).
                when().delete("topics/" + "testTopic3");
        System.out.println("^^^^^ responseGet: " + responseDelete.asString() + "^^^^^^");

        responseDelete.then().statusCode(200);

        // Get topic back
        Response responseGet =
                given().log().all().
                        authentication().preemptive().basic(TestUtils.user, TestUtils.pass).
                        contentType(TestUtils.JSON).
                        headers(TestUtils.toHeaders(TestUtils.mmxApiHeaders)).
                when().
                        get("topics");
        System.out.println("^^^^^ responseGet: " + responseGet.asString() + "^^^^^^");

       responseGet.then().statusCode(200).body("results.topicName", not(hasItem("testTopic3")));


    }


    @Test
    public void test04PublishTopic() {
        String payload = "{\n" +
                "\"topicName\":\"testTopic4\",\n" +
                "\"description\":\"testTopic4 Desc\"\n" +
                "}";
        Response responsePost =
                given().log().all().
                        authentication().preemptive().basic(TestUtils.user, TestUtils.pass).
                        contentType(TestUtils.JSON).
                        headers(TestUtils.toHeaders(TestUtils.mmxApiHeaders)).
                        body(payload).
                when().
                        post("topics");
        System.out.println("^^^^^ responsePost: " + responsePost.getStatusCode() + "^^^^^^");

        // Publish item to topic
        payload = "{\n" +
                " \"content\":\"publish test message to global topic1\",\n" +
                " \"messageType\":\"normal\",\n" +
                " \"contentType\":\"text\"\n" +
                "}";

        Response responsePublishMessagePost =
                given().log().all().
                        authentication().preemptive().basic(TestUtils.user, TestUtils.pass).
                        contentType(TestUtils.JSON).
                        headers(TestUtils.toHeaders(TestUtils.mmxApiHeaders)).
                        body(payload).
                when().log().all().
                        post("/topics/testTopic4/publish");

        System.out.println("^^^^ response from responsePublishMessagePost: " + responsePublishMessagePost.asString());
        String messageId = responsePublishMessagePost.then().
                statusCode(200).
                assertThat().body("status", equalTo("OK")).
                extract().path("messageId").toString();

        // List published items summary
        given().log().all().
                authentication().preemptive().basic(TestUtils.user, TestUtils.pass).
                contentType(TestUtils.JSON).
                headers(TestUtils.toHeaders(TestUtils.mmxApiHeaders)).
        when().log().all().
                queryParam("topicName", "testTopic4").
                get("topicssummary").
        then().log().all().
                assertThat().body("topics.any {it.topicName == 'testTopic4'}", is(true)).
                assertThat().body("topics.any {it.publishedItemCount >= 1}", is(true));

        // List published items count
        Response itemsResponse = given().log().all().
                authentication().preemptive().basic(TestUtils.user, TestUtils.pass).
                contentType(TestUtils.JSON).
                headers(TestUtils.toHeaders(TestUtils.mmxApiHeaders)).
        when().log().all().
                get("topics/testTopic4/items");

        itemsResponse.then().
                statusCode(200).
                assertThat().body("totalCount >= 1", is(true));

        String itemId = itemsResponse.then().extract()
                        .path("items[0].itemId");

      Response responseGetItemsByIdResponse =
          given().log().all().
              authentication().preemptive().basic(TestUtils.user, TestUtils.pass).
              contentType(TestUtils.JSON).
              headers(TestUtils.toHeaders(TestUtils.mmxApiHeaders)).
              param("id", itemId).
              then().log().all().
              get("/topics/testTopic4/items/byids");

      responseGetItemsByIdResponse.then().statusCode(200);
    }

  @Test
  public void test05DeleteNonExistantTopic() {
    String nonExistantTopic = "BadTopic1234";
    Response responseDelete =
        given().log().all().
            authentication().preemptive().basic(TestUtils.user, TestUtils.pass).
            contentType(TestUtils.JSON).
            headers(TestUtils.toHeaders(TestUtils.mmxApiHeaders)).
            when().delete("topics/" + nonExistantTopic);

    responseDelete.then()
      .body("message", startsWith("Topic with name")).statusCode(400);
  }

  @Test
  public void test06PublishToNonExistantTopic() {
    String nonExistantTopic = "BadTopic1234";

    // Publish item to topic
    String payload = "{\n" +
        " \"content\":\"publish test message to global topic\",\n" +
        " \"messageType\":\"normal\",\n" +
        " \"contentType\":\"text\"\n" +
        "}";

    Response responsePublishMessagePost =
        given().log().all().
            authentication().preemptive().basic(TestUtils.user, TestUtils.pass).
            contentType(TestUtils.JSON).
            headers(TestUtils.toHeaders(TestUtils.mmxApiHeaders)).
            body(payload).
            when().log().all().
            post("/topics/" +  nonExistantTopic + "/publish");

    responsePublishMessagePost.then()
        .body("message", startsWith("Topic with name")).statusCode(400);

  }

  @Test
  public void test07PublishTopicWithNoContent() {
    String payload = "{\n" +
        "\"topicName\":\"testTopic4\",\n" +
        "\"description\":\"testTopic4 Desc\"\n" +
        "}";
    Response responsePost =
        given().log().all().
            authentication().preemptive().basic(TestUtils.user, TestUtils.pass).
            contentType(TestUtils.JSON).
            headers(TestUtils.toHeaders(TestUtils.mmxApiHeaders)).
            body(payload).
            when().
            post("topics");
    System.out.println("^^^^^ responsePost: " + responsePost.getStatusCode() + "^^^^^^");

    // Publish item to topic
    payload = "{\n" +
        " \"messageType\":\"normal\",\n" +
        " \"contentType\":\"text\"\n" +
        "}";

    Response responsePublishMessagePost =
        given().log().all().
            authentication().preemptive().basic(TestUtils.user, TestUtils.pass).
            contentType(TestUtils.JSON).
            headers(TestUtils.toHeaders(TestUtils.mmxApiHeaders)).
            body(payload).
            when().log().all().
            post("/topics/testTopic4/publish");

    System.out.println("^^^^ response from responsePublishMessagePost: " + responsePublishMessagePost.asString());
    responsePublishMessagePost.then().
        statusCode(400).
        assertThat().body("status", equalTo("ERROR")).
        assertThat().body("message", containsString("Topic message content can't be empty"));
  }

    @AfterClass
    public void tearDown() {
        RestAssured.port = 5220;
        TestUtils.mmxApiHeaders.put("X-mmx-app-owner", TestUtils.appOwner);

        RequestSpecification request1 = given().log().all().
                authentication().preemptive().basic(TestUtils.user, TestUtils.pass).
                contentType(TestUtils.JSON).
                headers(TestUtils.toHeaders(TestUtils.mmxApiHeaders));
        RequestSpecification request2 = given().log().all().
                authentication().preemptive().basic(TestUtils.user, TestUtils.pass).
                contentType(TestUtils.JSON).
                headers(TestUtils.toHeaders(TestUtils.mmxApiHeaders));
        RequestSpecification request3 = given().log().all().
                authentication().preemptive().basic(TestUtils.user, TestUtils.pass).
                contentType(TestUtils.JSON).
                headers(TestUtils.toHeaders(TestUtils.mmxApiHeaders));
        RequestSpecification request4 = given().log().all().
                authentication().preemptive().basic(TestUtils.user, TestUtils.pass).
                contentType(TestUtils.JSON).
                headers(TestUtils.toHeaders(TestUtils.mmxApiHeaders));

        // delete testTopic1 topic
        request1.when().delete("topics/testTopic1");
        request2.when().delete("topics/testTopic2");
        request3.when().delete("topics/testTopic3");
        request4.when().delete("topics/testTopic4");
    }
}
