package com.magnet.max.common;

public class Device {

    private String deviceId;

    private OsType os;

    private PushAuthorityType pushAuthority;

    private String deviceToken;

    private String osVersion;

    private String userId;

    private String clientId;

    private String label;

    private String[] tags;

    DeviceStatus deviceStatus;

    public Device(){

    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public DeviceStatus getDeviceStatus() {
        return deviceStatus;
    }

    public void setDeviceStatus(DeviceStatus deviceStatus) {
        this.deviceStatus = deviceStatus;
    }

    public String getDeviceToken() {
        return deviceToken;
    }

    public void setDeviceToken(String deviceToken) {
        this.deviceToken = deviceToken;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public OsType getOs() {
        return os;
    }

    public void setOs(OsType os) {
        this.os = os;
    }

    public String getOsVersion() {
        return osVersion;
    }

    public void setOsVersion(String osVersion) {
        this.osVersion = osVersion;
    }

    public PushAuthorityType getPushAuthority() {
        return pushAuthority;
    }

    public void setPushAuthority(PushAuthorityType pushAuthority) {
        this.pushAuthority = pushAuthority;
    }

    public String[] getTags() {
        return tags;
    }

    public void setTags(String[] tags) {
        this.tags = tags;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }
}
