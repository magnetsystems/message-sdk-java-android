/*   Copyright (c) 2016 Magnet Systems, Inc.  All Rights Reserved.
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
package com.magnet.max.client;

import java.util.List;
import java.util.Map;

import org.apache.oltu.oauth2.common.exception.OAuthProblemException;
import org.apache.oltu.oauth2.common.exception.OAuthSystemException;

import com.google.gson.JsonObject;
import com.magnet.max.common.ApiException;
import com.magnet.max.common.AppAuthResult;
import com.magnet.max.common.Client;
import com.magnet.max.common.ClientRegistrationRequest;
import com.magnet.max.common.Device;
import com.magnet.max.common.PaginatedResult;
import com.magnet.max.common.User;
import com.squareup.okhttp.RequestBody;

import retrofit.Call;
import retrofit.Response;
import retrofit.http.Body;
import retrofit.http.Field;
import retrofit.http.FormUrlEncoded;
import retrofit.http.GET;
import retrofit.http.Multipart;
import retrofit.http.POST;
import retrofit.http.PUT;
import retrofit.http.Part;
import retrofit.http.Path;
import retrofit.http.Query;

/**
 * This is a trimmed version of MAX services and it has no dependencies on any
 * server modules.
 */
public interface MaxRestService {

    @FormUrlEncoded
    @POST("/api/com.magnet.server/developers/session")
    Call<JsonObject> login(@Field("username") String username, @Field("password") String password) throws OAuthProblemException, OAuthSystemException;

    //    @GET("/api/com.magnet.server/developers/enableSetupService")
    //    Call<Properties> enableSetupService(@Query("isEnabled") boolean enableSetupService) ;
    //
    //    @PUT("/api/com.magnet.server/developers/profile")
    //    Call<User> updateProfile(@Body UpdateUserProfileRequest userProfileRequest);
    //
    //    @GET("/api/com.magnet.server/developers/logout")
    //    Call<JsonElement> getLogout() ;
    //
    //    @POST("/developers/logout")
    //    Call<JsonElement> postLogout();
    //
    //
    @GET("/api/com.magnet.server/developers/apps")
    Call<List<Client>> getClients();

    @GET("/api/com.magnet.server/developers/apps/{oauthClientId}")
    Call<Client> getClient(@Path("oauthClientId") String oauthClientId) throws ApiException;

    //    @DELETE("/api/com.magnet.server/developers/apps/{oauthClientId}")
    //    Call<Boolean> deleteClient(@Path("oauthClientId") String oauthClientId) throws ApiException;
    //
    @PUT("/api/com.magnet.server/developers/apps/{oauthClientId}")
    Call<Client> updateClient(@Path("oauthClientId") String oauthClientId, @Body Client client) throws ApiException;

    @PUT("/api/com.magnet.server/developers/apps/{oauthClientId}/config")
    Call<Map<String,String>> updateClientConfig(@Path("oauthClientId") String oauthClientId, @Body Map<String, String> clientConfig) throws ApiException;

    @POST("/api/com.magnet.server/developers/apps/enrollment")
    Call<JsonObject> register(@Body ClientRegistrationRequest clientRegistrationRequest) throws OAuthSystemException;

    @GET("/api/com.magnet.server/developers/apps/{oauthClientId}/login")
    Call<JsonObject> loginToApp(@Path("oauthClientId") String oauthClientId);

    //    @POST("/api/com.magnet.server/developers/apps/{oauthClientId}/logout")
    //    Call<JsonObject> logoutOfApp(@Path("oauthClientId") String oauthClientId);

    @POST("/api/com.magnet.server/developers/user")
    Call<User> addUser(@Body User user) throws ApiException;


    @POST("/api/com.magnet.server/devices")
    Call<JsonObject> registerDevice(@Body Device device) throws ApiException;

    @GET("/api/com.magnet.server/developers/{clientId}/users/all")
    Call<List<User>> getAllUsers(@Path("clientId") String clientId, @Query("take") int take, @Query("skip") int skip) throws ApiException;

    @GET("/api/com.magnet.server/developers/whoami")
    Call<User> whoAmI() throws ApiException;
    //
    //    @PUT("/api/com.magnet.server/developers/user/{userId}")
    //    Call<User> updateUser(@Path("userId") String userId, @Body User user) throws ApiException;
    //
    //
    //    @DELETE("/api/com.magnet.server/developers/user/{userId}")
    //    Call<Boolean> deleteUser(@Path("userId") String userId) throws ApiException;
    //
    //
    //    @PUT("/api/com.magnet.server/developers/user/deactivation/{userId}")
    //    Call<User> deactivateUser(@Path("userId") String userId) throws ApiException;
    //
    //
    //    @PUT("/api/com.magnet.server/developers/user/activation/{userId}")
    //    Call<User> activateUser(@Path("userId") String userId) throws ApiException;
    //
    @GET("/api/com.magnet.server/developers/users/usernames")
    Call<List<User>> getUsersByUserNames(@Query("userNames") List<String> userNames);

    @GET("/api/com.magnet.server/developers/users/ids")
    Call<List<User>> getUsersByUserIds(@Query("userIds") List<String> userIds);


    @GET("/api/com.magnet.server/developers/users/query")
    Call<PaginatedResult> searchUsers(@Query("q") String query);
//    @GET("/api/com.magnet.server/developers/users/query")
//    Call<PaginatedResult> searchUsers(@Query("q") String query, @Query("take") int take, @Query("skip") int skip, @Query("sort") String sort);

    @POST("/api/com.magnet.server/applications/session")
    Call<AppAuthResult> authenticateApp() throws ApiException;

//    @GET("/api/com.magnet.server/applications/session")
//    Call<Client> getClient() throws ApiException;

    @GET("/api/com.magnet.server/toolassets/download/config")
    Response downloadProperties(@Query("platform") String platform, @Query("requestId") String requestId) throws ApiException;

    // CERTS
    @Multipart
    @POST("/js/rest/apps/{MMX_APP_ID}/uploadAPNSCertificate")
    Call<JsonObject> uploadCertificate(@Path("MMX_APP_ID") String mmxAppId,
                                       @Part("apnsCertPassword") RequestBody password,
                                       @Part("qquuid") RequestBody qquuid,
                                       @Part("certfile\"; filename=\"TrungAPNSCertificates.p12") RequestBody certificate);

    //USERS
    @POST(" /api/com.magnet.server/user/session")
    Call<Map<String, Object>> authenticateUser(
            @Query("grant_type") String grantType,
            @Query("username") String username,
            @Query("password") String password,
            @Query("client_id") String clientId,
            @Query("scope") String scope
    ) throws ApiException;


    @POST("/api/com.magnet.server/user/enrollment")
    Call<User> registerUser(@Body User user) throws ApiException;

}
