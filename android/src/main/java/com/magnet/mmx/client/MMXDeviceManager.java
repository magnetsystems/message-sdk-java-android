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

import android.os.Handler;

import com.magnet.mmx.client.common.DeviceManager;
import com.magnet.mmx.client.common.MMXException;
import com.magnet.mmx.client.common.MMXid;
import com.magnet.mmx.protocol.DevList;
import com.magnet.mmx.protocol.DevReg;
import com.magnet.mmx.protocol.DevTags;
import com.magnet.mmx.protocol.MMXStatus;

import java.util.List;

/**
 * Manager class used to manage the user's device information, including listing the
 * devices for the current user and adding/removing extras and tags for a device.
 *
 */
public class MMXDeviceManager extends MMXManager {
  private static final String TAG = MMXDeviceManager.class.getSimpleName();
  private DeviceManager mDeviceManager = null;

  MMXDeviceManager(MMXClient mmxClient, Handler handler) {
    super(mmxClient, handler);
    onConnectionChanged();
  }

  /**
   * Adds tags for the current device.  The tags must not be null or empty, and
   * each tag cannot be longer than 25 characters; otherwise MMXException with
   * BAD_REQUEST status code will be thrown.
   *
   * @param tags the tags for the device
   * @return the status of this request
   * @throws MMXException
   */
  public MMXStatus addTags(List<String> tags) throws MMXException {
    checkDestroyed();
    return mDeviceManager.addTags(tags);
  }

  /**
   * Retrieves all of the tags for the current device.
   *
   * @return the tags for the current device
   * @throws MMXException
   */
  public DevTags getAllTags() throws MMXException {
    checkDestroyed();
    return mDeviceManager.getAllTags();
  }

  /**
   * Retrieves a list of devices for the current user.
   *
   * @return a list of devices associated with the current user
   * @throws MMXException
   */
  public DevList getDevices() throws MMXException {
    checkDestroyed();
    return mDeviceManager.getDevices();
  }

  /**
   * Associates the specified device registration information with the current user.
   *
   * @param deviceRegistration the device registration information for this device
   * @return the status of the request
   * @throws MMXException
   */
  MMXStatus register(DevReg deviceRegistration) throws MMXException {
    checkDestroyed();
    return mDeviceManager.register(deviceRegistration);
  }

  /**
   * Remove the tags from the current device.  The tags must not be null or empty, and
   * each tag cannot be longer than 25 characters; otherwise MMXException with
   * BAD_REQUEST status code will be thrown.
   * @param tags A list of tags to be removed.
   * @return The status.
   * @throws MMXException
   */
  public MMXStatus removeTags(List<String> tags) throws MMXException {
    checkDestroyed();
    return mDeviceManager.removeTags(tags);
  }

  /**
   * Set the tags to the current device.  If the list is null or empty, all
   * tags will be removed.
   * @param tags A list of tags, or an empty list.
   * @return The status.
   * @throws MMXException Device Not Found.
   */
  public MMXStatus setAllTags(List<String> tags) throws MMXException {
    checkDestroyed();
    return mDeviceManager.setAllTags(tags);
  }


  /**
   * Unregister the device.  All push registrations will be removed too.
   * @param deviceId the device id to unregister
   * @throws MMXException
   */
  MMXStatus unregister(String deviceId) throws MMXException {
    checkDestroyed();
    return mDeviceManager.unregister(deviceId);
  }

  @Override
  void onConnectionChanged() {
    mDeviceManager = DeviceManager.getInstance(getMMXClient().getMMXConnection());
  }

  /**
   * Retrieves a list of devices for the specified user.
   * @param id the id of the user
   * @return a list of devices for the specified user
   */
  public DevList getDevices(MMXid id) throws MMXException {
    checkDestroyed();
    return mDeviceManager.getDevices(id.getUserId());
  }
}
