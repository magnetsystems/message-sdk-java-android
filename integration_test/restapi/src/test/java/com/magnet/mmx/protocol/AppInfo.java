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
package com.magnet.mmx.protocol;

/**
 * App Info for creation and update app requests.  It is copied from
 * com.magnet.mmx.server.plugin.mmxmgmt.servlet.AppInfo class.
 */
public class AppInfo {
  String name;
  String ownerId;
  String appId;
  String apiKey;
  String googleApiKey;
  String googleProjectId;
  String apnsCertPassword;
  String ownerEmail;
  String guestSecret;
  String serverUserId;
  boolean apnsCertProduction;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getGuestSecret() {
    return guestSecret;
  }

  public void setGuestSecret(String guestSecret) {
    this.guestSecret = guestSecret;
  }

  public String getOwnerId() {
    return ownerId;
  }

  public void setOwnerId(String ownerId) {
    this.ownerId = ownerId;
  }

  public String getOwnerEmail() {
    return ownerEmail;
  }

  public void setOwnerEmail(String ownerEmail) {
    this.ownerEmail = ownerEmail;
  }

  public String getGoogleApiKey() {
    return googleApiKey;
  }

  public void setGoogleApiKey(String googleApiKey) {
    this.googleApiKey = googleApiKey;
  }

  public String getAppId() {
    return appId;
  }

  public void setAppId(String appId) {
    this.appId = appId;
  }

  public String getApiKey() {
    return apiKey;
  }

  public void setApiKey(String apiKey) {
    this.apiKey = apiKey;
  }

  public String getGoogleProjectId() {
    return googleProjectId;
  }

  public void setGoogleProjectId(String googleProjectId) {
    this.googleProjectId = googleProjectId;
  }

  public String getApnsCertPassword() {
    return apnsCertPassword;
  }

  public void setApnsCertPassword(String apnsCertPassword) {
    this.apnsCertPassword = apnsCertPassword;
  }

  public String getServerUserId() {
    return serverUserId;
  }

  public void setServerUserId(String serverUserId) {
    this.serverUserId = serverUserId;
  }

  public boolean isApnsCertProduction() {
    return apnsCertProduction;
  }

  public void setApnsCertProduction(boolean apnsCertProduction) {
    this.apnsCertProduction = apnsCertProduction;
  }

}
