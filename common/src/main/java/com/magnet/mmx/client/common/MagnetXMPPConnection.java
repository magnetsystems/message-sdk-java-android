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

import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;

/**
 * A special XMPP Connection that can reset authentication failure.  Without
 * this capability, the connection cannot be reused after each login() failure
 * or switching to different user.  The smack library currently requires a new 
 * XMPPConnection instance and all listeners need to be added again; it is a
 * hassle.
 */
public class MagnetXMPPConnection extends XMPPTCPConnection {
  public MagnetXMPPConnection(ConnectionConfiguration config) {
    super(config);
  }
  
  public ConnectionConfiguration getConnectionConfiguration() {
    return config;
  }
  
  /**
   * This method must be called before login to different user.
   */
  public void resetAuthFailure() {
    // Reset the SASLAuthentication between two accounts.
    getSASLAuthentication().authenticationFailed(null);
    // Reset the XMPPConnection reconnection state.
    this.wasAuthenticated = false;
  }
  
  // For reconnection use.
  boolean wasAuthenticated() {
    return this.wasAuthenticated;
  }
  
  /**
   * Send the packet with auto-connect.
   */
  @Override
  public void sendPacket(Packet packet) throws NotConnectedException {
    try {
      if (!isConnected()) {
        this.connect();
      }
    } catch (Throwable e) {
      e.printStackTrace();
      throw new NotConnectedException();
    }
    super.sendPacket(packet);
  }
}
