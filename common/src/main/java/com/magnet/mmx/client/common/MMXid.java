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

package com.magnet.mmx.client.common;

import java.io.Serializable;
import java.util.Map;

import com.google.gson.annotations.SerializedName;
import com.magnet.mmx.util.GsonData;

/**
 * This class represents an identifier for an MMX user or MMX end-point.  The
 * MMX identifier for end-point targets to a specific user device.  The MMX
 * identifier for user targets to the user regardless of devices.  An MMX
 * user identifier can be derived from an MMX end-point identifier by:
 * <pre>
 * MMXid endpointXID;
 * ...
 * MMXid userXID = new MMXid(endpointXID.getUserId());
 * </pre>
 */
public class MMXid implements Serializable {
  private static final long serialVersionUID = -4167064339466261739L;
  @SerializedName("userId")
  private String mUserId;
  @SerializedName("devId")
  private String mDeviceId;

  /**
   * Construct an identifier for a user.  The <code>userId</code> must not be
   * in the escaped format and it will be stored in lower case.
   * @param userId A non-null user ID.
   */
  public MMXid(String userId) {
    if (userId == null) {
      throw new IllegalArgumentException("User ID cannot be null");
    }
    mUserId = userId.toLowerCase();
  }

  /**
   * Construct an identifier for an end-point.  The <code>userId</code> must not
   * be in the escaped format and it will be stored in lower case.
   * @param userId A non-null user ID.
   * @param deviceId A device ID.
   */
  public MMXid(String userId, String deviceId) {
    this(userId);
    mDeviceId = deviceId;
  }

  /**
   * Get the user ID.  When comparing the user ID, make sure that it is case
   * insensitive.
   * @return The user ID in lower case.
   */
  public String getUserId() {
    return mUserId;
  }

  /**
   * Get the device ID.  A null value may be returned if this identifier is for
   * user.  The device ID is case sensitive.
   * @return The device ID, or null.
   */
  public String getDeviceId() {
    return mDeviceId;
  }

  /**
   * Check if two MMXid's are equal.  The equality is defined as both user ID's
   * and their device ID's (if specified) are same.  If any one device ID
   * is not specified, they are considered as equal as well.  The user ID is
   * case insensitive; however, the device ID is case sensitive.
   * @param xid The other MMXid to be compared.
   * @return true if equal; otherwise, false.
   */
  public boolean equalsTo(MMXid xid) {
    if (xid == this) {
      return true;
    }
    if ((xid == null) || !mUserId.equalsIgnoreCase(xid.mUserId)) {
      return false;
    }
    if (mDeviceId != null && xid.mDeviceId != null) {
      return mDeviceId.equals(xid.mDeviceId);
    }
    return true;
  }

  /**
   * Check if two MMXid's are identical.  The user ID (case insensitive) and
   * device ID (case sensitive, but optional) must match.
   */
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (!(obj instanceof MMXid)) {
      return false;
    }
    MMXid xid = (MMXid) obj;
    if ((xid == null) || !mUserId.equalsIgnoreCase(xid.mUserId)) {
      return false;
    }
    if (mDeviceId == null && xid.mDeviceId == null) {
      return true;
    }
    if (mDeviceId != null) {
      return mDeviceId.equals(xid.mDeviceId);
    }
    return false;
  }

  /**
   * A hash code from the user ID and optional device ID.
   */
  @Override
  public int hashCode() {
    if (mDeviceId == null) {
      return mUserId.hashCode();
    } else {
      return mUserId.hashCode() ^ mDeviceId.hashCode();
    }
  }

  /**
   * Get a string representation of the identifier.
   * @return A string in "userID" or "userID/deviceID" format.
   * @see #parse(String)
   */
  @Override
  public String toString() {
    return (mDeviceId == null) ? mUserId : (mUserId+'/'+mDeviceId);
  }

  /**
   * Get a JSON representation of the identifier.
   * @return A JSON string representation.
   */
  public String toJson() {
    return GsonData.getGson().toJson(this);
  }

  /**
   * Convert the string representation of an identifier into the object.
   * @param xid The value from {@link #toString()}
   * @return An MMXid object.
   * @see #toString()
   */
  public static MMXid parse(String xid) {
    int slash = xid.indexOf('/');
    if (slash < 0) {
      return new MMXid(xid);
    } else {
      return new MMXid(xid.substring(0, slash), xid.substring(slash+1));
    }
  }

  /**
   * Convert the JSON representation of an identifier into the object.
   * @param json The JSON string representation.
   * @return An MMXid object.
   */
  public static MMXid fromJson(String json) {
    return GsonData.getGson().fromJson(json, MMXid.class);
  }

  // Convert a Map into the object.
  static MMXid fromMap(Map<String, String> map) {
    return new MMXid(map.get("userId"), map.get("devId"));
  }
}
