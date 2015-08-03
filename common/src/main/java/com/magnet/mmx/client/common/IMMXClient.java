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

/**
 * The convenient client interface provides the basic functionality.
 *
 */
public interface IMMXClient {
  /**
   * Get the account manager.  The primary functions are to change password and
   * query for users.
   * @return
   * @throws MMXException
   */
  public AccountManager getAccountManager() throws MMXException;
  /**
   * Get the device manager.  The primary function is to get all registered
   * devices belonging to a user.
   * @return
   * @throws MMXException
   */
  public DeviceManager getDeviceManager() throws MMXException;
  /**
   * Get the Pub/Sub Manager.
   * @return
   * @throws MMXException
   */
  public PubSubManager getPubSubManager() throws MMXException;
  /**
   * Get the Messaging Manager.
   * @return
   * @throws MMXException
   */
  public MessageManager getMessageManager() throws MMXException;
  /**
   * Inform the MMX server to suspend delivering messages to this client.
   * @throws MMXException Not connecting to MMX server.
   */
  public void suspendDelivery() throws MMXException;
  /**
   * Inform the MMX server to resume delivering messages to this client.
   * @throws MMXException Not connecting to MMX server.
   */
  public void resumeDelivery() throws MMXException;
  /**
   * Get the MMX ID of the current authenticated end-point (user and device.)
   * @return The MMX ID that represents the current authenticated end-point.
   * @throw MMXException Not connecting to MMX server
   */
  public MMXid getClientId() throws MMXException;
}
