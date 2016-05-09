package com.magnet.max.common;

import java.util.Map;

public class User {

    private String userIdentifier;

    private String clientId;

    private String firstName;

    private String lastName;

    private String email;

    private String userName;

    private String password;

    private UserRealm userRealm;

    private UserStatus userStatus;

    private String[] roles;

    private String otpCode;

    private String[] tags;

    private Map<String,String> userAccountData; // Change name to  extras

    private Map<String,String> challengePreferences; //? What is my moms name : "Vinni"

    private String externalUserId;  //realmUserId



    public User() {
    }


//    public String getMmxUserId(){
//        return this.mmxUserId;
//    }
//
//    public void setMmxUserId(String mmxUserId) {
//        this.mmxUserId = mmxUserId;
//    }

    public String getOtpCode() {
        return otpCode;
    }

    public void setOtpCode(String otpCode) {
        this.otpCode = otpCode;
    }

    public void setStatus(UserStatus userStatus) {
        this.userStatus = userStatus;
    }

    public UserStatus getUserStatus() {
        return userStatus;
    }

    public String getClientId() {
        return clientId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String[] getRoles() {
        return roles;
    }

    public void setRoles(String[] roles) {
        this.roles = roles;
    }

    public Map<String, String> getUserAccountData() {
        return userAccountData;
    }

    public void setUserAccountData(Map<String, String> userAccountData) {
        this.userAccountData = userAccountData;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public UserRealm getUserRealm() {
        return userRealm;
    }

    public void setUserRealm(UserRealm userRealm) {
        this.userRealm = userRealm;
    }

    public String getUserIdentifier() {
        return userIdentifier;
    }

    public void setUserIdentifier(String userIdentifier) {
        this.userIdentifier = userIdentifier;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public void setUserStatus(UserStatus userStatus) {
        this.userStatus = userStatus;
    }

    public Map<String, String> getChallengePreferences() {
        return challengePreferences;
    }

    public void setChallengePreferences(Map<String, String> challengePreferences) {
        this.challengePreferences = challengePreferences;
    }

    public String getExternalUserId() {
        return externalUserId;
    }

    public void setExternalUserId(String externalUserId) {
        this.externalUserId = externalUserId;
    }

    public String[] getTags() {
        return tags;
    }

    public void setTags(String[] tags) {
        this.tags = tags;
    }
}
