package com.magnet.mmx.client.helper;

import com.magnet.server.sdk.bean.*;
import org.apache.oltu.oauth2.common.exception.OAuthProblemException;
import org.apache.oltu.oauth2.common.exception.OAuthSystemException;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DeveloperContext {

    private static final String appPassword = "magnet";
    private static final String userLastname = "magnet-test-family";
    private static final String userPassword = "magnet";
    private static final String userFirstnamePrefix = "user-";
    private static final String usernamePrefix = "user-";

    //END POINT
//    private String baseUrl = "http://ec2s-mms20.magnet.com:8443";

    //DEVELOPER
    private String developerUsername = "developer";
    private String developerPasword = "developer";
    private String developerToken;

    //CLIENT/APP
    private Client app;
//    private String appToken = null;

    //USERS
    private Map<String, User> users = new HashMap<String, User>();
    private Map<String, String> userTokens = new HashMap<String, String>();


    private DeveloperServiceHelper developerServiceHelper;

    public DeveloperContext(String baseUrl, String developerUsername, String developerPasword) {

//        this.baseUrl = baseUrl;
        this.developerUsername = developerUsername;
        this.developerPasword = developerPasword;
        developerServiceHelper = new DeveloperServiceHelper(baseUrl);
    }

    //CLIENT/APP
    public String createApp() throws IOException, OAuthSystemException, OAuthProblemException {

        developerToken = developerServiceHelper.getDeveloperToken(developerUsername, developerPasword);
        app = new Client();
        app.setClientName("magnet-test-" + UUID.randomUUID());
        app.setClientDescription("magnet-test-app");
        app.setOwnerEmail("test@magnet.com");
        developerServiceHelper.createApp(developerToken, app, appPassword);
//        appToken = developerServiceHelper.getAppToken(developerToken, app.getOauthClientId());
        developerServiceHelper.getAppToken(developerToken, app.getOauthClientId());
        app = developerServiceHelper.getApp(developerToken, app.getOauthClientId());
        return app.getMmxAppId();
    }

    //USERS
    public User registerUser(String suffix) throws IOException {

        User user = new User();
        String firstname = userFirstnamePrefix + suffix;
        String username = usernamePrefix + suffix;
        user.setFirstName(firstname);
        user.setLastName(userLastname);
        user.setUserName(username);
        user.setPassword(userPassword);
        user.setEmail(username + "@magnet.com");
        users.put(suffix, user);

        developerServiceHelper.registerUserToApp(developerToken, app.getOauthClientId(), user);

        return user;
    }
    public void authenticateUser(String suffix, String deviceId) throws IOException {

        User user = users.get(suffix);
        String userToken = developerServiceHelper.getUserToken(user, deviceId);
        userTokens.put(suffix, userToken);

        Device device = new Device();
        device.setClientId(app.getOauthClientId());
        device.setDeviceId(deviceId);
        device.setDeviceStatus(DeviceStatus.ACTIVE);
        device.setUserId(user.getUserIdentifier());
        device.setLabel("label-" + suffix);
        device.setOs(OsType.ANDROID);
        device.setOsVersion("5.1.0");
        device.setPushAuthority(PushAuthorityType.GCM);
        device.setTags(new String[]{"test"});

        developerServiceHelper.registerDevice(userToken, device);
    }

    public String getUserId(String suffix) {

        User user = users.get(suffix);
        String id = null;
        if (user != null) {
            id = user.getUserIdentifier();
        }
        return id;
    }
    public String getUserToken(String suffix) {
        return userTokens.get(suffix);
    }
}
