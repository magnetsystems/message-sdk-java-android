/*   Copyright (c) 2016 Magnet Systems, Inc.
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

import java.util.Date;

import com.google.gson.annotations.SerializedName;

/**
 * This is the response from all App Management API.  It is copied from
 * com.magnet.mmx.server.common.data.AppEntity.
 *
 * Caller must use com.magnet.mmx.util.GsonData class to de-serialize the JSON.
 */
public class AppEntity {
  private int id;
  private String name;
  private String serverUserId;
  private String appId;
  private String appAPIKey;
  private String googleAPIKey;
  private String googleProjectId;
  private String apnsCertPassword;
  private boolean apnsCertProduction;
  private boolean apnsCertUploaded;

  private String guestUserId;
  private String guestSecret;
  private String ownerId;
  private String ownerEmail;

  @SerializedName("iso8601CreationDate")
  private Date creationDate;
  @SerializedName("iso8601Modificationdate")
  private Date modificationdate;

  public int getId() {
    return id;
  }
  public String getName() {
    return name;
  }
  public String getServerUserId() {
    return serverUserId;
  }
  public String getAppId() {
    return appId;
  }
  public String getAppAPIKey() {
    return appAPIKey;
  }
  public String getGoogleAPIKey() {
    return googleAPIKey;
  }
  public String getGoogleProjectId() {
    return googleProjectId;
  }
  public String getApnsCertPassword() {
    return apnsCertPassword;
  }
  public boolean isApnsCertProduction() {
    return apnsCertProduction;
  }
  public boolean isApnsCertUploaded() {
    return apnsCertUploaded;
  }
  public String getGuestUserId() {
    return guestUserId;
  }
  public String getGuestSecret() {
    return guestSecret;
  }
  public String getOwnerId() {
    return ownerId;
  }
  public String getOwnerEmail() {
    return ownerEmail;
  }
  public Date getCreationDate() {
    return creationDate;
  }
  public Date getModificationdate() {
    return modificationdate;
  }
}
