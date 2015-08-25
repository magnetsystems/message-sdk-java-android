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

public class TestUsersManagementAPI extends TestCase {
  private static final String INVALID_CHAR_MESSAGE = "Invalid character specified in username";
  private static final String DUPLICATE_USER = "User with username:%s already exists";
  private static final String REVISED_USERS_RESOURCE = "mmxmgmt/api/v1/users";
    @Before
    public void setUp() {
        RestAssured.baseURI = TestUtils.HTTPBaseUrl;
        RestAssured.port = 5220;
        RestAssured.basePath = "";
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }

    @Test
    public void test01Create_Get() {
        TestUtils.mmxApiHeaders.put("X-mmx-app-owner", TestUtils.appOwner);

        // Create User
        String payload = "{\n" +
                "  \"username\": \"test01user\",\n" +
                "  \"password\": \"pass\",\n" +
                "  \"appId\": " +
                "\"" + TestUtils.appId + "\",\n" +
                "  \"name\": \"test1 user\",\n" +
                "  \"email\": \"test01User@email.com\",\n" +
                "  \"isAdmin\" : true\n" +
                "}";
        Response responsePost =
                given().log().all().
                        authentication().preemptive().basic(TestUtils.user, TestUtils.pass).
                        contentType(TestUtils.JSON).
                        headers(TestUtils.toHeaders(TestUtils.mmxApiHeaders)).
                        body(payload).
                when().
                        post(REVISED_USERS_RESOURCE);
        System.out.println(responsePost.asString());

        responsePost.then().
                statusCode(201);

        // Get User by userName
        RestAssured.port = 5220;
        Response responseGet =
                given().log().all().
                        authentication().preemptive().basic(TestUtils.user, TestUtils.pass).
                        contentType(TestUtils.JSON).
                        headers(TestUtils.toHeaders(TestUtils.mmxApiHeaders)).
                when().
                        queryParam("name", "test1 user").
                        get(REVISED_USERS_RESOURCE);
        System.out.println(responseGet.asString());

        responseGet.then().
                statusCode(200).
                assertThat().body("results.any {it.username == 'test01user'}", is(true)).
                assertThat().body("results.any {it.name == 'test1 user'}", is(true)).
                assertThat().body("results.any {it.email == 'test01User@email.com'}", is(true));
    }

    @Test
    public void test02Modify_GetUser() {
        TestUtils.mmxApiHeaders.put("X-mmx-app-owner", TestUtils.appOwner);

        // Create User
        String payload = "{\n" +
                "  \"username\": \"test02user\",\n" +
                "  \"password\": \"pass\",\n" +
                "  \"appId\": " +
                "\"" + TestUtils.appId + "\",\n" +
                "  \"name\": \"test2 user\",\n" +
                "  \"email\": \"test02User@email.com\",\n" +
                "  \"isAdmin\" : true\n" +
                "}";
        Response responsePost =
                given().log().all().
                        authentication().preemptive().basic(TestUtils.user, TestUtils.pass).
                        contentType(TestUtils.JSON).
                        headers(TestUtils.toHeaders(TestUtils.mmxApiHeaders)).
                        body(payload).
                when().
                        post(REVISED_USERS_RESOURCE);
        System.out.println(responsePost.asString());

        responsePost.then().
                statusCode(201);

        // update User
        payload = "{\n" +
                "  \"username\": \"test02user\",\n" +
                "  \"appId\": " +
                "\"" + TestUtils.appId + "\",\n" +
                "  \"email\": \"test02UserUpdated@magnet.com\",\n" +
                "  \"name\": \"test2 user updated\",\n" +
                "  \"isAdmin\" : false\n" +
                "}";
        Response responsePut =
                given().log().all().
                        authentication().preemptive().basic(TestUtils.user, TestUtils.pass).
                        contentType(TestUtils.JSON).
                        headers(TestUtils.toHeaders(TestUtils.mmxApiHeaders)).
                        body(payload).
                when().
                        put(REVISED_USERS_RESOURCE);
        System.out.println(responsePut.asString());

        responsePut.then().
                statusCode(200);

        // Get User by email
        RestAssured.port = 5220;
        Response responseGet =
                given().log().all().
                        authentication().preemptive().basic(TestUtils.user, TestUtils.pass).
                        contentType(TestUtils.JSON).
                        headers(TestUtils.toHeaders(TestUtils.mmxApiHeaders)).
                when().
                        queryParam("email", "test02UserUpdated").
                        get(REVISED_USERS_RESOURCE);
        System.out.println(responseGet.asString());

        responseGet.then().
                statusCode(200).
                assertThat().body("results.any {it.username == 'test02user'}", is(true)).
                assertThat().body("results.any {it.name == 'test2 user updated'}", is(true)).
                assertThat().body("results.any {it.email == 'test02UserUpdated@magnet.com'}", is(true));
    }

    @Test
    public void test03DeleteUser() {
        TestUtils.mmxApiHeaders.put("X-mmx-app-owner", TestUtils.appOwner);

        // Create User
        String payload = "{\n" +
                "  \"username\": \"test03user\",\n" +
                "  \"password\": \"pass\",\n" +
                "  \"appId\": " +
                "\"" + TestUtils.appId + "\",\n" +
                "  \"name\": \"test3 user\",\n" +
                "  \"email\": \"test03User@email.com\",\n" +
                "  \"isAdmin\" : true\n" +
                "}";
        Response responsePost =
                given().log().all().
                        authentication().preemptive().basic(TestUtils.user, TestUtils.pass).
                        contentType(TestUtils.JSON).
                        headers(TestUtils.toHeaders(TestUtils.mmxApiHeaders)).
                        body(payload).
                when().
                        post(REVISED_USERS_RESOURCE);
        System.out.println(responsePost.asString());

        responsePost.then().
                statusCode(201);

        // Delete
        Response responseDelete =
                given().log().all().
                        authentication().preemptive().basic(TestUtils.user, TestUtils.pass).
                        contentType(TestUtils.JSON).
                        headers(TestUtils.toHeaders(TestUtils.mmxApiHeaders)).
                when().
                        delete(REVISED_USERS_RESOURCE + "/test03User");
        System.out.println(responseDelete.asString());

        responseDelete.then().
                statusCode(200);

        // get all users
        RestAssured.port = 5220;
        Response responseGet =
                given().log().all().
                        authentication().preemptive().basic(TestUtils.user, TestUtils.pass).
                        contentType(TestUtils.JSON).
                        headers(TestUtils.toHeaders(TestUtils.mmxApiHeaders)).
                when().
                        get(REVISED_USERS_RESOURCE);
        System.out.println(responseGet.asString());

        responseGet.then().
                statusCode(200).
                assertThat().body("results.any {it.username == 'test03user'}", is(false)).
                assertThat().body("results.any {it.name == 'test3 user'}", is(false)).
                assertThat().body("results.any {it.email == 'test03User@email.com'}", is(false));
    }

    @AfterClass
    public void tearDown() {
        RestAssured.port = 5220;
        TestUtils.mmxApiHeaders.put("X-mmx-app-owner", TestUtils.appOwner);

        RequestSpecification request = given().log().all().
                authentication().preemptive().basic(TestUtils.user, TestUtils.pass).
                contentType(TestUtils.JSON).
                headers(TestUtils.toHeaders(TestUtils.mmxApiHeaders));

        // delete test01User
        request.when().delete(REVISED_USERS_RESOURCE + "/test01user");
        request.when().delete(REVISED_USERS_RESOURCE + "/test02user");
        request.when().delete(REVISED_USERS_RESOURCE + "/test03user");
        request.when().delete(REVISED_USERS_RESOURCE + "/test08user");
    }


  @Test
  public void test01Create_WithLongName() {
    TestUtils.mmxApiHeaders.put("X-mmx-app-owner", TestUtils.appOwner);

    // Create User
    String payload = "{\n" +
        "  \"username\": \"12345678901234567890123456789012345678901234567890123\",\n" +
        "  \"password\": \"pass\",\n" +
        "  \"appId\": " +
        "\"" + TestUtils.appId + "\",\n" +
        "  \"name\": \"test1 user\",\n" +
        "  \"email\": \"test01User@email.com\",\n" +
        "  \"isAdmin\" : true\n" +
        "}";
    Response responsePost =
        given().log().all().
            authentication().preemptive().basic(TestUtils.user, TestUtils.pass).
            contentType(TestUtils.JSON).
            headers(TestUtils.toHeaders(TestUtils.mmxApiHeaders)).
            body(payload).
            when().
            post(REVISED_USERS_RESOURCE);
    System.out.println(responsePost.asString());

    responsePost.then().
        statusCode(400)
        .body("code", equalTo(48))
        .body("message", containsString("Username must be 5 to 42 characters"));
  }

  @Test
  public void test04Create_WithPercentageInUserName() {
    TestUtils.mmxApiHeaders.put("X-mmx-app-owner", TestUtils.appOwner);
    String payload = "{\n" +
        "  \"username\": \"amazing%user\",\n" +
        "  \"password\": \"pass\",\n" +
        "  \"appId\": " +
        "\"" + TestUtils.appId + "\",\n" +
        "  \"name\": \"test1 user\",\n" +
        "  \"email\": \"test01User@email.com\",\n" +
        "  \"isAdmin\" : true\n" +
        "}";
    Response responsePost =
        given().log().all().
            authentication().preemptive().basic(TestUtils.user, TestUtils.pass).
            contentType(TestUtils.JSON).
            headers(TestUtils.toHeaders(TestUtils.mmxApiHeaders)).
            body(payload).
            when().
            post(REVISED_USERS_RESOURCE);

    responsePost.then().
        statusCode(400)
        .body("code", equalTo(48))
        .body("message", containsString(INVALID_CHAR_MESSAGE));
  }

  @Test
  public void test05Create_WithAtSignInUserName() {
    TestUtils.mmxApiHeaders.put("X-mmx-app-owner", TestUtils.appOwner);
    String payload = "{\n" +
        "  \"username\": \"amazing@user\",\n" +
        "  \"password\": \"pass\",\n" +
        "  \"appId\": " +
        "\"" + TestUtils.appId + "\",\n" +
        "  \"name\": \"test1 user\",\n" +
        "  \"email\": \"test01User@email.com\",\n" +
        "  \"isAdmin\" : true\n" +
        "}";
    Response responsePost =
        given().log().all().
            authentication().preemptive().basic(TestUtils.user, TestUtils.pass).
            contentType(TestUtils.JSON).
            headers(TestUtils.toHeaders(TestUtils.mmxApiHeaders)).
            body(payload).
            when().
            post(REVISED_USERS_RESOURCE);

    responsePost.then().
        statusCode(201);
  }

  @Test
  public void test06Create_WithSlashInUserName() {
    TestUtils.mmxApiHeaders.put("X-mmx-app-owner", TestUtils.appOwner);
    String payload = "{\n" +
        "  \"username\": \"amazing/user\",\n" +
        "  \"password\": \"pass\",\n" +
        "  \"appId\": " +
        "\"" + TestUtils.appId + "\",\n" +
        "  \"name\": \"test1 user\",\n" +
        "  \"email\": \"test01User@email.com\",\n" +
        "  \"isAdmin\" : true\n" +
        "}";
    Response responsePost =
        given().log().all().
            authentication().preemptive().basic(TestUtils.user, TestUtils.pass).
            contentType(TestUtils.JSON).
            headers(TestUtils.toHeaders(TestUtils.mmxApiHeaders)).
            body(payload).
            when().
            post(REVISED_USERS_RESOURCE);

    responsePost.then().
        statusCode(400)
        .body("code", equalTo(48))
        .body("message", containsString(INVALID_CHAR_MESSAGE));
  }

  @Test
  public void test07Create_WithAmpersandInUserName() {
    TestUtils.mmxApiHeaders.put("X-mmx-app-owner", TestUtils.appOwner);
    String payload = "{\n" +
        "  \"username\": \"amazing&user\",\n" +
        "  \"password\": \"pass\",\n" +
        "  \"appId\": " +
        "\"" + TestUtils.appId + "\",\n" +
        "  \"name\": \"test1 user\",\n" +
        "  \"email\": \"test01User@email.com\",\n" +
        "  \"isAdmin\" : true\n" +
        "}";
    Response responsePost =
        given().log().all().
            authentication().preemptive().basic(TestUtils.user, TestUtils.pass).
            contentType(TestUtils.JSON).
            headers(TestUtils.toHeaders(TestUtils.mmxApiHeaders)).
            body(payload).
            when().
            post(REVISED_USERS_RESOURCE);

    responsePost.then().
        statusCode(400)
        .body("code", equalTo(48))
        .body("message", containsString(INVALID_CHAR_MESSAGE));
  }

  @Test
  public void test08CreateDuplicateUser() {
    TestUtils.mmxApiHeaders.put("X-mmx-app-owner", TestUtils.appOwner);
    String userName = "test08user";
    // Create User
    String payload = "{\n" +
        "  \"username\": \"" + userName+ "\",\n" +
        "  \"password\": \"pass\",\n" +
        "  \"appId\": " +
        "\"" + TestUtils.appId + "\",\n" +
        "  \"name\": \"test08 user\",\n" +
        "  \"email\": \"test08User@email.com\",\n" +
        "  \"isAdmin\" : true\n" +
        "}";
    Response responsePost =
        given().log().all().
            authentication().preemptive().basic(TestUtils.user, TestUtils.pass).
            contentType(TestUtils.JSON).
            headers(TestUtils.toHeaders(TestUtils.mmxApiHeaders)).
            body(payload).
            when().
            post(REVISED_USERS_RESOURCE);

    responsePost.then().
        statusCode(201);

    /**
     * try creating the same user again
     */
    Response duplicatePost =
        given().log().all().
            authentication().preemptive().basic(TestUtils.user, TestUtils.pass).
            contentType(TestUtils.JSON).
            headers(TestUtils.toHeaders(TestUtils.mmxApiHeaders)).
            body(payload).
            when().
            post(REVISED_USERS_RESOURCE);

    String errorMessage = String.format(DUPLICATE_USER, userName);

    duplicatePost.then().
        statusCode(409)
        .body("code", equalTo(48))
        .body("message", containsString(errorMessage));

    // Delete
    Response responseDelete =
        given().log().all().
            authentication().preemptive().basic(TestUtils.user, TestUtils.pass).
            contentType(TestUtils.JSON).
            headers(TestUtils.toHeaders(TestUtils.mmxApiHeaders)).
            when().
            delete(REVISED_USERS_RESOURCE + "/" +  userName );

    responseDelete.then().
        statusCode(200);
  }
}
