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

import com.magnet.mmx.protocol.MMXTopic;

/**
 * This class represents a subscription to a topic.
 */
public class MMXSubscription implements Serializable {
  private static final long serialVersionUID = -5589445686560504482L;
  private String mId;
  private MMXTopic mTopic;
  private String mDeviceId;

  MMXSubscription(MMXTopic topic, String id, String deviceId) {
    mId = id;
    mTopic = topic;
    mDeviceId = deviceId;
  }

  /**
   * Get the topic for this subscription.
   * @return The topic object.
   */
  public MMXTopic getTopic() {
    return mTopic;
  }

  /**
   * Get the subscription ID.
   * @return The subscription ID.
   */
  public String getId() {
    return mId;
  }

  /**
   * Get the device ID that was specified for this subscription.
   * @return A device ID, or null.
   */
  public String getDeviceId() {
    return mDeviceId;
  }

  /**
   * The string representation of this subscription.
   */
  @Override
  public String toString() {
    return "[topic="+mTopic+", sid="+mId+", devId="+mDeviceId+"]";
  }
}
