package com.magnet.mmx.client.helper;

//import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
//import com.magnet.common.PaginatedResult;
//import com.magnet.mms.data.beans.ClientRegistrationRequest;
//import com.magnet.server.sdk.bean.Client;
//import com.magnet.server.sdk.bean.UpdateUserProfileRequest;
//import com.magnet.server.sdk.bean.User;
//import com.magnet.server.sdk.exception.ApiException;
import com.magnet.mms.data.beans.ClientRegistrationRequest;
import com.magnet.server.sdk.bean.Client;
import com.magnet.server.sdk.bean.Device;
import com.magnet.server.sdk.bean.User;
import com.magnet.server.sdk.exception.ApiException;
import org.apache.oltu.oauth2.common.exception.OAuthProblemException;
import org.apache.oltu.oauth2.common.exception.OAuthSystemException;
import retrofit.Call;
import retrofit.http.*;

//import java.util.List;
//import java.util.Map;
//import java.util.Properties;

public interface DeveloperService {

    @FormUrlEncoded
    @POST("/api/com.magnet.server/developers/session")
    Call<JsonObject> login(@Field("username") String username, @Field("password") String password) throws OAuthProblemException, OAuthSystemException;

//    @GET("/api/com.magnet.server/developers/enableSetupService")
//    Call<Properties> enableSetupService(@Query("isEnabled") boolean enableSetupService) ;
//
//
//    @PUT("/api/com.magnet.server/developers/profile")
//    Call<User> updateProfile(@Body UpdateUserProfileRequest userProfileRequest);
//
//
//
//    @GET("/api/com.magnet.server/developers/logout")
//    Call<JsonElement> getLogout() ;
//
//    @POST("/developers/logout")
//    Call<JsonElement> postLogout();
//
//
//    @GET("/api/com.magnet.server/developers/apps")
//    Call<List<Client>> getClients();

    @GET("/api/com.magnet.server/developers/apps/{oauthClientId}")
    Call<Client> getClient(@Path("oauthClientId") String oauthClientId) throws ApiException;

//    @DELETE("/api/com.magnet.server/developers/apps/{oauthClientId}")
//    Call<Boolean> deleteClient(@Path("oauthClientId") String oauthClientId) throws ApiException;
//
//    @PUT("/api/com.magnet.server/developers/apps/{oauthClientId}")
//    Call<Client> updateClient(@Path("oauthClientId") String oauthClientId, @Body Client client) throws ApiException;
//
//    @PUT("/api/com.magnet.server/developers/apps/{oauthClientId}/config")
//    Call<Map<String,String>> updateClientConfig(@Path("oauthClientId") String oauthClientId, @Body Map<String, String> clientConfig) throws ApiException;

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

    //    @GET("/api/com.magnet.server/developers/{clientId}/users/all")
//    Call<List<User>> getAllUsers(@Path("clientId") String clientId, @Query("take") int take, @Query("skip") int skip) throws ApiException;
//
//    @GET("/api/com.magnet.server/developers/whoami")
//    Call<JsonElement> whoAmI() throws ApiException;
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
//    @GET("/api/com.magnet.server/developers/users/usernames")
//    Call<List<User>> getUsersByUserNames(@Query("userNames") List<String> userNames);
//
//
//    @GET("/api/com.magnet.server/developers/users/ids")
//    Call<List<User>> getUsersByUserIds(@Query("userIds") List<String> userIds);
//
//
//    @GET("/api/com.magnet.server/developers/users/query")
//    Call<PaginatedResult> searchUsers(@Query("q") String query, @Query("take") int take, @Query("skip") int skip, @Query("sort") String sort);
}
