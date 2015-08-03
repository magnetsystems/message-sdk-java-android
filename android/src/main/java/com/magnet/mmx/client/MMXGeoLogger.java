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

package com.magnet.mmx.client;

import com.magnet.mmx.client.common.AccountManager;
import com.magnet.mmx.client.common.DeviceManager;
import com.magnet.mmx.client.common.IMMXClient;
import com.magnet.mmx.client.common.MMXException;
import com.magnet.mmx.client.common.MMXPersonalTopic;
import com.magnet.mmx.client.common.MMXUserTopic;
import com.magnet.mmx.client.common.MMXid;
import com.magnet.mmx.client.common.MessageManager;
import com.magnet.mmx.client.common.PubSubManager;
import com.magnet.mmx.client.common.TopicNotFoundException;
import com.magnet.mmx.protocol.GeoLoc;
import com.magnet.mmx.util.TopicHelper;

/**
 * This logger provides a simple mechanism to log the geo-location and clear
 * historic geo-location of the current device.
 */
public class MMXGeoLogger {
  /**
   * Update the geo-location of the current user.  If the geo-location topic
   * does not exist, it will be created.  The published geo-location is only
   * accessible by the application administrator.
   * @param client A connected client.
   * @param geoLoc The geo-location of the current user.
   * @return The log ID.
   * @throws MMXException
   */
  public static String updateGeoLocation(final MMXClient client, GeoLoc geoLoc)
          throws MMXException {
    return com.magnet.mmx.client.common.MMXGeoLogger.updateGeoLocation(new IMMXClient() {
      public AccountManager getAccountManager() throws MMXException {
        throw new RuntimeException("Not yet implemented.");
      }

      public DeviceManager getDeviceManager() throws MMXException {
        throw new RuntimeException("Not yet implemented.");
      }

      public PubSubManager getPubSubManager() throws MMXException {
        return client.getPubSubManager().getInternalManager();
      }

      public MessageManager getMessageManager() throws MMXException {
        throw new RuntimeException("Not yet implemented.");
      }

      public void suspendDelivery() throws MMXException {
        client.suspendDelivery();
      }

      public void resumeDelivery() throws MMXException {
        client.resumeDelivery();
      }

      public MMXid getClientId() throws MMXException {
        return client.getClientId();
      }
    }, geoLoc);
  }

  /**
   * Clear all historic geo-location log for the current user.
   * @param client A connected client.
   * @throws MMXException
   */
  public static void clearGeoLocaction(MMXClient client) throws MMXException {
    try {
      MMXPubSubManager pubsubMgr = client.getPubSubManager();
      pubsubMgr.clearAllItems(new MMXPersonalTopic(TopicHelper.TOPIC_GEOLOC));
    } catch (TopicNotFoundException e) {
      // Ignored.
    }
  }

  /**
   * @hide
   * Track a user's geo-location by current user or by current device only
   * after the location has been published. If a subscriptions already exists,
   * no new subscription will be done.  This API is intended for application
   * server to track clients, but client application may invoke it too.  Due
   * to privacy concern, this API is removed for now.  If client application
   * is interested in a similar functionality, the client application can
   * create a user topic with a random UUID as topic name and sends a private
   * invitation message with the topic name and user ID to an invitee.
   * @param client A connected client.
   * @param userId The tracked user ID (no appID.)
   * @param thisDeviceOnly true for current device; false for current user.
   * @return The subscription ID.
   * @throws TopicNotFoundException The user has not published the geo-location yet.
   * @throws MMXException
   */
  static String startTracking(MMXClient client, String userId,
                              boolean thisDeviceOnly) throws TopicNotFoundException, MMXException {
    if (userId == null) {
      throw new IllegalArgumentException("userId cannot be null");
    }
    MMXPubSubManager pubsubMgr = client.getPubSubManager();
    return pubsubMgr.subscribe(new MMXUserTopic(userId, TopicHelper.TOPIC_GEOLOC),
            thisDeviceOnly);
  }

  /**
   * @hide
   * Stop tracking a given user's geo-location.  If <code>subscriptionId</code>
   * is null, all subscriptions will be cancelled.
   * @param client A connected client.
   * @param userId The user ID being tracked by the current user.
   * @param subscriptionId The subscription ID from
   *          {@link #startTracking(MMXClient, String, boolean)}, or null.
   * @return false if the location was not tracked; true if the tracking is stopped.
   * @throws MMXException
   */
  static boolean stopTracking(MMXClient client, String userId,
                              String subscriptionId) throws MMXException {
    if (userId == null) {
      throw new IllegalArgumentException("userId cannot be null");
    }
    if (subscriptionId == null) {
      throw new IllegalArgumentException("subscriptionId cannot be null");
    }
    MMXPubSubManager pubsubMgr = client.getPubSubManager();
    return pubsubMgr.unsubscribe(new MMXUserTopic(userId, TopicHelper.TOPIC_GEOLOC),
            subscriptionId);
  }
}
