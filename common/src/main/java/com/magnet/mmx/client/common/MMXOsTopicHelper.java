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

import com.magnet.mmx.protocol.MMXTopicId;
import com.magnet.mmx.protocol.MMXTopicOptions;
import com.magnet.mmx.protocol.OSType;
import com.magnet.mmx.protocol.StatusCode;
import com.magnet.mmx.protocol.TopicAction;
import com.magnet.mmx.util.TopicHelper;

/**
 * @hide
 * This helper class provides various functionality for a device to manage
 * the topic tied with its operating system version.
 */
public class MMXOsTopicHelper {
  private final static String TAG = "MMXOsTopicHelper";

  /**
   * Create a topic for all versions of the OS.  This topic is created during
   * the application registration process.  Typically, a device subscribes
   * a specific version via {@link #subscribeOSVersion(OSType, String)} or all
   * versions via {@link #subscribeOS(OSType)}.  Then some client publishes an
   * item targeting to all versions of an OS {@link #publishOS(OSType, MMXPayload)}
   * or to a version of OS {@link #publishOSVersion(OSType, String, MMXPayload)}.
   * This method is equivalent to {@link #createOSVersion(OSType, String)} with
   * a null version.
   * @param client A connected client.
   * @param type The non-null OS type.
   * @throws MMXException
   */
  public static void createOS(IMMXClient client, OSType os)
      throws MMXException {
    createOSVersion(client, os, null);
  }

  /**
   * Create a topic for a version of the OS (e.g. ANDROID/4.1.3.)  Application
   * must create an OS version topic explicitly before it can be subscribed or
   * published. If <code>version</code> is null, all versions topic
   * {@link TopicHelper#TOPIC_LEAF_ALL} will be created.
   * @param client A connected client.
   * @param type The non-null OS type
   * @param version null for all versions, or a non-empty OS version.
   * @throws MMXException
   */
  public static void createOSVersion(IMMXClient client, OSType os,
      String version) throws MMXException {
    if (os == null) {
      throw new MMXException("OS type cannot be null", StatusCode.BAD_REQUEST);
    }
    if (version == null) {
      version = TopicHelper.TOPIC_LEAF_ALL;
    } else {
      validateVersion(version);
    }
    try {
      PubSubManager pubsubMgr = client.getPubSubManager();
      MMXTopicOptions options = new MMXTopicOptions()
            .setPublisherType(TopicAction.PublisherType.owner);
      pubsubMgr.createTopic(new MMXGlobalTopic(
          TopicHelper.makeOSTopic(os, version)), options);
    } catch (TopicExistsException e) {
      Log.w(TAG, e.getMessage()+" already exists", null);
    }
  }

  /**
   * Publish an item to all versions of the OS.
   * @param client A connected client.
   * @param os An OS type.
   * @param payload A payload.
   * @return A publisshed item ID.
   * @throws MMXException
   */
  public static String publishOS(IMMXClient client, OSType os,
      MMXPayload payload) throws MMXException {
    return publishOSVersion(client, os, null, payload);
  }

  /**
   * Publish an item to a version of the OS.  If the topic does not exist, it
   * will be created automatically.
   * @param client A connected client.
   * @param os An OS type.
   * @param version null for all versions, or a non-empty OS version.
   * @param payload A payload.
   * @return A published item ID.
   * @throws MMXException
   */
  public static String publishOSVersion(IMMXClient client, OSType os,
      String version, MMXPayload payload) throws MMXException {
    if (version == null) {
      version = TopicHelper.TOPIC_LEAF_ALL;
    } else {
      validateVersion(version);
    }

    PubSubManager pubsubMgr = client.getPubSubManager();
    MMXTopicId topic = new MMXTopicId(TopicHelper.makeOSTopic(os, version));
    boolean redo;
    do {
      try {
        redo = false;
        return pubsubMgr.publish(topic, payload);
      } catch (TopicNotFoundException e) {
        createOSVersion(client, os, version);
        redo = true;
      }
    } while (redo);
    return null;
  }

  /**
   * Subscribe to all versions of the OS for the current device.  Since the
   * OS version is device specific, the subscription is bounded to the
   * device. If a subscription already exists, it is a no-op.  This method is
   * same as {@link #subscribeOSVersion(OSType, String)} with null version.
   * @param client A connected client.
   * @param os An OS type.
   * @return A subscription ID.
   * @throws TopicNotFoundException
   * @throws MMXException
   */
  public static String subscribeOS(IMMXClient client, OSType os)
      throws TopicNotFoundException, MMXException {
    return subscribeOSVersion(client, os, null);
  }

  /**
   * Unsubscribe all versions of the OS topic from this device.  This method is
   * equivalent to calling {@link #unsubscribeOSVersion(OSType, String, String)}
   * with a null version.
   * @param client A connected client.
   * @param os An OS type.
   * @param subscriptionId A non-null subscription ID.
   * @return false if topic is not subscribed; true otherwise.
   * @throws TopicNotFoundException
   * @throws MMXException
   */
  public static boolean unsubscribeOS(IMMXClient client, OSType os,
      String subscriptionId) throws TopicNotFoundException, MMXException {
    return unsubscribeOSVersion(client, os, null, subscriptionId);
  }

  /**
   * Subscribe to a version of OS topic for the current device.  If a
   * subscription already exists, it is a no-op.
   * @param client A connected client.
   * @param os An OS type.
   * @param version null for any versions, or a non-empty OS version.
   * @return A subscription ID.
   * @throws TopicNotFoundException
   * @throws MMXException
   */
  public static String subscribeOSVersion(IMMXClient client, OSType os,
      String version) throws TopicNotFoundException, MMXException {
    validateVersion(version);
    PubSubManager pubsubMgr = client.getPubSubManager();
    String topic = TopicHelper.makeOSTopic(os, version);
    return pubsubMgr.subscribe(new MMXTopicId(topic), true);
  }

  /**
   * Unsubscribe a version of the OS topic from this device.
   * @param client A connected client.
   * @param os An OS type.
   * @param version null for all versions, or a non-empty OS version.
   * @param subscriptionId A non-null subscription ID.
   * @return false if topic is not subscribed; true otherwise.
   * @throws TopicNotFoundException
   * @throws MMXException
   */
  public static boolean unsubscribeOSVersion(IMMXClient client, OSType os,
      String version, String subscriptionId)
          throws TopicNotFoundException, MMXException {
    validateVersion(version);
    if (subscriptionId == null || subscriptionId.isEmpty()) {
      throw new MMXException("Subscription ID cannot be null or empty", StatusCode.BAD_REQUEST);
    }
    PubSubManager pubsubMgr = client.getPubSubManager();
    String topic = TopicHelper.makeOSTopic(os, version);
    return pubsubMgr.unsubscribe(new MMXTopicId(topic), subscriptionId);
  }

  // Validate the version value.
  private static void validateVersion(String version) throws MMXException {
    if (version.isEmpty() || version.indexOf(TopicHelper.TOPIC_DELIM) >= 0) {
      throw new MMXException("Version cannot be empty or contain '/'",
                              StatusCode.BAD_REQUEST);
    }
  }
}
