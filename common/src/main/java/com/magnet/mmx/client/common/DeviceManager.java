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

import java.util.ArrayList;
import java.util.List;

import com.magnet.mmx.protocol.Constants;
import com.magnet.mmx.protocol.Constants.DeviceCommand;
import com.magnet.mmx.protocol.DevId;
import com.magnet.mmx.protocol.DevList;
import com.magnet.mmx.protocol.DevReg;
import com.magnet.mmx.protocol.DevTags;
import com.magnet.mmx.protocol.MMXStatus;
import com.magnet.mmx.protocol.StatusCode;
import com.magnet.mmx.util.TagUtil;

/**
 * Provide device registration, unregistration and query for device ID's.
 */
public class DeviceManager {
  private final static String TAG = "DeviceManager";
  private MMXConnection mCon;
  private final static Creator sCreator = new Creator() {
    @Override
    public Object newInstance(MMXConnection con) {
      return new DeviceManager(con);
    }
  };

  static class DevRegIQHandler<Request, Result>
                          extends MMXIQHandler<Request, Result> {
    @Override
    public String getElementName() {
      return Constants.MMX;
    }

    @Override
    public String getNamespace() {
      return Constants.MMX_NS_DEV;
    }
  }

  /**
   * @hide
   * Get the instance of this manager.
   * @param con
   * @return
   */
  public static DeviceManager getInstance(MMXConnection con) {
    return (DeviceManager) con.getManager(TAG, sCreator);
  }

  protected DeviceManager(MMXConnection con) {
    mCon = con;
    MMXIQHandler<DevReg, MMXStatus> iqHandler =
        new DevRegIQHandler<DevReg, MMXStatus>();
    iqHandler.registerIQProvider();
  }

  /**
   * @hide
   * Register a device with optional push registration.  The apiKey will be set
   * and sent to MMX server automatically for validation.
   * @param devInfo
   * @throws MMXException
   */
  public MMXStatus register(DevReg devInfo) throws MMXException {
    devInfo.setApiKey(mCon.getApiKey());
    DevRegIQHandler<DevReg, MMXStatus> iqHandler =
        new DevRegIQHandler<DevReg, MMXStatus>();
    iqHandler.sendSetIQ(mCon, Constants.DeviceCommand.REGISTER.name(), devInfo, MMXStatus.class,
        iqHandler);
    return iqHandler.getResult();
  }

  /**
   * @hide
   * Unregister the device.  All push registrations will be removed too.
   * @param devId
   * @throws MMXException
   * @see {@link MMXContext#getDeviceId()}
   */
  public MMXStatus unregister(String devId) throws MMXException {
    DevId rqt = new DevId(devId);
    DevRegIQHandler<DevId, MMXStatus> iqHandler =
        new DevRegIQHandler<DevId, MMXStatus>();
    iqHandler.sendSetIQ(mCon, Constants.DeviceCommand.UNREGISTER.name(), rqt,
        MMXStatus.class, iqHandler);
    return iqHandler.getResult();
  }

  /**
   * Get the tags for the current device.
   * @return A tag info object.
   * @throws MMXException Device Not Found.
   */
  public DevTags getAllTags() throws MMXException {
    DevId rqt = new DevId(mCon.getContext().getDeviceId());
    DevRegIQHandler<DevId, DevTags> iqHandler =
        new DevRegIQHandler<DevId, DevTags>();
    iqHandler.sendGetIQ(mCon, Constants.DeviceCommand.GETTAGS.toString(), rqt,
        DevTags.class, iqHandler);
    DevTags devTags = iqHandler.getResult();
    return devTags;
  }

  /**
   * Set the tags to the current device.  If the list is null or empty, all
   * tags will be removed.
   * @param tags A list of tags, or an empty list.
   * @return The status.
   * @throws MMXException Device Not Found.
   */
  public MMXStatus setAllTags(List<String> tags) throws MMXException {
    return doTags(DeviceCommand.SETTAGS, tags);
  }

  /**
   * Add the tags to the current device.
   * @param tags A list of tags to be added.
   * @return The status.
   * @throws MMXException
   */
  public MMXStatus addTags(List<String> tags) throws MMXException {
    return doTags(DeviceCommand.ADDTAGS, tags);
  }

  /**
   * Remove the tags from the current device.
   * @param tags A list of tags to be removed.
   * @return The status.
   * @throws MMXException
   */
  public MMXStatus removeTags(List<String> tags) throws MMXException {
    return doTags(DeviceCommand.REMOVETAGS, tags);
  }

  private MMXStatus doTags(DeviceCommand cmd, List<String> tags)
                            throws MMXException {
    if (cmd == DeviceCommand.SETTAGS) {
      if (tags == null) {
        tags = new ArrayList<String>(0);
      }
      if (!tags.isEmpty()) {
        validateTags(tags);
      }
    } else {
      validateTags(tags);
    }
    DevTags devTags = new DevTags(mCon.getContext().getDeviceId(), tags);
    DevRegIQHandler<DevTags, MMXStatus> iqHandler =
        new DevRegIQHandler<DevTags, MMXStatus>();
    iqHandler.sendSetIQ(mCon, cmd.toString(), devTags, MMXStatus.class, iqHandler);
    return iqHandler.getResult();
  }

  /**
   * Get all registered devices belong to the current user.
   * @return A list of device ID's.
   * @throws MMXException
   */
  public DevList getDevices() throws MMXException {
    return getDevices(null);
  }

  /**
   * Get all registered devices belonging to a user.  If <code>userId</code>
   * is null, the current user will be assumed.
   * @param userId The user ID (without appID)
   * @return A list of device ID's or an empty list.
   * @throws MMXException
   */
  public DevList getDevices(String userId) throws MMXException {
    DevRegIQHandler<String, DevList> iqHandler =
        new DevRegIQHandler<String, DevList>();
    iqHandler.sendGetIQ(mCon, Constants.DeviceCommand.QUERY.name(), userId,
        DevList.class, iqHandler);
    return iqHandler.getResult();
  }

//  /**
//   * @hide
//   * Get all the extras for the current device.
//   * @return Any extras properties or an empty map.
//   * @throws MMXException
//   */
//  public Map<String, String> getAllExtras() throws MMXException {
//    // TODO: not implemented
//    throw new MMXException("Not implemented", StatusCode.NOT_IMPLEMENTED);
//  }
//
//  /**
//   * @hide
//   * Update the extras for the current device.
//   * @param extras null to remove all extras, or an update
//   * @return
//   * @throws MMXException
//   */
//  public MMXStatus setAllExtras(Map<String, String> extras) throws MMXException {
//    // TODO: not implemented
//    throw new MMXException("Not implemented", StatusCode.NOT_IMPLEMENTED);
//  }

  private void validateTags(List<String> tags) throws MMXException {
    if (tags == null || tags.isEmpty()) {
      throw new MMXException("List of tags cannot be null or empty", StatusCode.BAD_REQUEST);
    }
    try {
      TagUtil.validateTags(tags);
    } catch (IllegalArgumentException e) {
      throw new MMXException(e.getMessage(), StatusCode.BAD_REQUEST);
    }
  }
}
