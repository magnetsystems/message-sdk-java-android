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

public interface MMXConnectionListener {
  /**
   * Connection and authentication to the server is done.  It may be the first
   * connection or any subsequent reconnection.
   */
  public void onConnectionEstablished();

  /**
   * Attempt to reconnect in <code>interval</code> seconds.  The
   * <code>interval</code> may be -1 if the wait is indefinite until the
   * connectivity is available.
   * @param interval Number of seconds to start the reconnection, or -1.
   */
  public void onReconnectingIn(int interval);

  /**
   * Connection to the server is closed normally or reconnection is aborted.
   */
  public void onConnectionClosed();

  /**
   * Fail connecting to the server or connection is closed on error,
   */
  public void onConnectionFailed(Exception cause);

  /**
   * A user is authenticated.  The <code>user</code> will have the device
   * end-point information.
   * @param user A MMX ID of the end-point.
   */
  public void onAuthenticated(MMXid user);

  /**
   * Authentication failure.  The <code>user</code> only has the human readable
   * user ID.
   * @param user A MMX ID of the user.
   */
  public void onAuthFailed(MMXid user);

  /**
   * The user account is created automatically and successfully.  The <code>
   * user</code> only has the human readable user ID.
   * @param user A MMX ID of the user.
   */
  public void onAccountCreated(MMXid user);
}
