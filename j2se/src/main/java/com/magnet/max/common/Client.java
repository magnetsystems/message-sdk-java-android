package com.magnet.max.common;

import java.util.Map;
import java.util.concurrent.TimeUnit;

public class Client {

    private String internalId;

    private String clientName;

    private String clientDescription;

    private ClientStatus clientStatus;

    private long createdTime;

    private String oauthClientId;

    private String oauthSecret;

    private String redirectUrl;

    private String ownerEmail;

    private long expiresIn;

    private TimeUnit expirationTimeUnit;

    private String mmxAppId;

    private String developerId;

    //private String serverUserId;
    private Map<String,String> clientConfigs;

    public static final String SERVER_USER = "serveruser";

    public Client(){

    }

    public String getClientDescription() {
        return clientDescription;
    }

    public void setClientDescription(String clientDescription) {
        this.clientDescription = clientDescription;
    }

    public String getClientName() {
        return clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    public ClientStatus getClientStatus() {
        return clientStatus;
    }

    public void setClientStatus(ClientStatus clientStatus) {
        this.clientStatus = clientStatus;
    }

    public long getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(long createdTime) {
        this.createdTime = createdTime;
    }

    public TimeUnit getExpirationTimeUnit() {
        return expirationTimeUnit;
    }

    public void setExpirationTimeUnit(TimeUnit expirationTimeUnit) {
        this.expirationTimeUnit = expirationTimeUnit;
    }

    public long getExpiresIn() {
        return expiresIn;
    }

    public void setExpiresIn(long expiresIn) {
        this.expiresIn = expiresIn;
    }

    public String getInternalId() {
        return internalId;
    }

    public void setInternalId(String internalId) {
        this.internalId = internalId;
    }

    public String getMmxAppId() {
        return mmxAppId;
    }

    public void setMmxAppId(String mmxAppId) {
        this.mmxAppId = mmxAppId;
    }

    public String getOauthClientId() {
        return oauthClientId;
    }

    public void setOauthClientId(String oauthClientId) {
        this.oauthClientId = oauthClientId;
    }

    public String getOauthSecret() {
        return oauthSecret;
    }

    public void setOauthSecret(String oauthSecret) {
        this.oauthSecret = oauthSecret;
    }

    public String getOwnerEmail() {
        return ownerEmail;
    }

    public void setOwnerEmail(String ownerEmail) {
        this.ownerEmail = ownerEmail;
    }

    public String getRedirectUrl() {
        return redirectUrl;
    }

    public void setRedirectUrl(String redirectUrl) {
        this.redirectUrl = redirectUrl;
    }

    public String getDeveloperId() {
        return developerId;
    }

    public void setDeveloperId(String developerId) {
        this.developerId = developerId;
    }

    public Map<String, String> getClientConfigs() {
        return clientConfigs;
    }

    public void setClientConfigs(Map<String, String> clientConfigs) {
        this.clientConfigs = clientConfigs;
    }
}
