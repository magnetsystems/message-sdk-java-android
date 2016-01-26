package com.magnet.mmx.client.helper;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.magnet.mms.data.beans.ClientRegistrationRequest;
import com.magnet.server.intg.http.HttpRequestExecutor;
import com.magnet.server.intg.http.request.RequestBuilder;
import com.magnet.server.sdk.bean.*;
import com.squareup.okhttp.Interceptor;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import org.apache.oltu.oauth2.common.exception.OAuthProblemException;
import org.apache.oltu.oauth2.common.exception.OAuthSystemException;
import retrofit.GsonConverterFactory;
import retrofit.Response;
import retrofit.Retrofit;

import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.util.Map;

public class DeveloperServiceHelper {

    private String baseUrl;

    public DeveloperServiceHelper(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getDeveloperToken(String username, String password) throws IOException, OAuthSystemException, OAuthProblemException {

        Retrofit retrofit = new Retrofit.Builder()
//                .baseUrl(DeveloperContext.getToolsBaseUrl())
                .baseUrl(baseUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        DeveloperService developerService = retrofit.create(DeveloperService.class);
        Response<JsonObject> response = developerService.login(username, password).execute();
        return response.body().get("access_token").getAsString();
    }
    public void createApp(final String developerToken, Client app, String appPassword) throws OAuthSystemException, IOException {

        OkHttpClient httpClient = new OkHttpClient();
        httpClient.interceptors().add(new Interceptor() {
            @Override
            public com.squareup.okhttp.Response intercept(Chain chain) throws IOException {
                Request original = chain.request();

                Request request = original.newBuilder()
                        .header("Content-type", "application/json")
                        .header("Authorization", "Bearer " + developerToken)
                        .method(original.method(), original.body())
                        .build();

                return chain.proceed(request);
            }
        });

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .client(httpClient)
                .build();

        DeveloperService developerService = retrofit.create(DeveloperService.class);

        ClientRegistrationRequest registrationRequest = new ClientRegistrationRequest();
        registrationRequest.setClient_name(app.getClientName());
        registrationRequest.setPassword(appPassword);
        registrationRequest.setClient_description(app.getClientDescription());
        registrationRequest.setOwner_email(app.getOwnerEmail());

        Response<JsonObject> response = developerService.register(registrationRequest).execute();
        app.setOauthClientId(response.body().get("client_id").getAsString());
    }

    public String getAppToken(final String developerToken, String clientId) throws IOException {

        OkHttpClient httpClient = new OkHttpClient();
        httpClient.interceptors().add(new Interceptor() {
            @Override
            public com.squareup.okhttp.Response intercept(Chain chain) throws IOException {
                Request original = chain.request();

                Request request = original.newBuilder()
                        .header("Content-type", "application/json")
                        .header("Authorization", "Bearer " + developerToken)
                        .method(original.method(), original.body())
                        .build();

                return chain.proceed(request);
            }
        });

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .client(httpClient)
                .build();

        DeveloperService developerService = retrofit.create(DeveloperService.class);

        Response<JsonObject> response = developerService.loginToApp(clientId).execute();
        String cookie = response.headers().get("Set-Cookie");
        int indx = cookie.indexOf("Magnet-Dev-Console-App-Cookie=");
        return cookie.substring(indx + "Magnet-Dev-Console-App-Cookie=".length(), cookie.indexOf(";"));
    }

    public Client getApp(final String developerToken, String clientId) throws IOException {

        OkHttpClient httpClient = new OkHttpClient();
        httpClient.interceptors().add(new Interceptor() {
            @Override
            public com.squareup.okhttp.Response intercept(Chain chain) throws IOException {
                Request original = chain.request();

                Request request = original.newBuilder()
                        //                        .header("Content-type", "application/json")
                        .header("Authorization", "Bearer " + developerToken)
                        .method(original.method(), original.body())
                        .build();

                return chain.proceed(request);
            }
        });

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .client(httpClient)
                .build();
        DeveloperService developerService = retrofit.create(DeveloperService.class);
        Response<Client> response = developerService.getClient(clientId).execute();
        return response.body();
    }
    public void registerUserToApp(final String developerToken, String appId, User user) throws IOException {

        OkHttpClient httpClient = new OkHttpClient();
        httpClient.interceptors().add(new Interceptor() {
            @Override
            public com.squareup.okhttp.Response intercept(Chain chain) throws IOException {
                Request original = chain.request();

                Request request = original.newBuilder()
                        .header("Content-type", "application/json")
                        .header("Authorization", "Bearer " + developerToken)
                        .method(original.method(), original.body())
                        .build();

                return chain.proceed(request);
            }
        });

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .client(httpClient)
                .build();

        DeveloperService developerService = retrofit.create(DeveloperService.class);

        user.setUserRealm(UserRealm.DB);
        user.setTags(new String[]{"test"});
        user.setRoles(new String[]{"user"});
        user.setClientId(appId);

        Response<User> response = developerService.addUser(user).execute();

        user.setUserIdentifier(response.body().getUserIdentifier());
    }
    public void registerDevice(final String userToken, Device device) throws IOException {

        OkHttpClient httpClient = new OkHttpClient();
        httpClient.interceptors().add(new Interceptor() {
            @Override
            public com.squareup.okhttp.Response intercept(Chain chain) throws IOException {
                Request original = chain.request();

                Request request = original.newBuilder()
                        .header("Content-type", "application/json")
                        .header("Authorization", "Bearer " + userToken)
                        .method(original.method(), original.body())
                        .build();

                return chain.proceed(request);
            }
        });

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .client(httpClient)
                .build();

        DeveloperService developerService = retrofit.create(DeveloperService.class);

        Response<JsonObject> response = developerService.registerDevice(device).execute();
        System.out.println("Device Registration - RESP - " + response.raw());
    }

    public String getUserToken(User user, String deviceId) {

        HttpRequestExecutor executor = HttpRequestExecutor.withSingleClient();

        String data = "grant_type=password" +
                "&username=" + user.getUserName() +
                "&password=" + user.getPassword() +
                "&client_id=" + user.getClientId() +
                "&scope=user"
                ;

        com.magnet.server.intg.http.response.Response response = executor.execute(
                RequestBuilder.postRequest(baseUrl)
                        .withPath("/api/com.magnet.server/user/session")
                        .withHeader("Content-type", MediaType.APPLICATION_FORM_URLENCODED)
                        .withData(data)
                        .withHeader("MMS-DEVICE-ID", deviceId)
                        .build());

        Map nvPairs = new Gson().fromJson(response.getOutput(), Map.class);
        return (String) nvPairs.get("access_token");
    }

}