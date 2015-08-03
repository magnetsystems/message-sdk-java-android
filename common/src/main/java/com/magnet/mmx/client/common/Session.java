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
 * An ad-hoc group session.
 *
 */
public interface Session {
  /**
   * Get the unique session ID.
   * @return
   */
  public String getId();
  /**
   * Send an invitation to a single user.
   * @param msg
   * @param user
   * @throws MMXException
   */
  public void sendInvitation(String msg, String user) throws MMXException;
  /**
   * Send an invitation to multiple users.
   * @param msg
   * @param users
   * @throws MMXException
   */
  public void sendInvitations(String msg, String[] users) throws MMXException;
  /**
   * Send a message to the group.
   * @param payload
   * @throws MMXException
   */
  public void sendMessage(MMXMessage payload) throws MMXException;
  /**
   * Get the list of current participants.  Currently it is not expected to be
   * a huge group.
   * @return
   * @throws MMXException
   */
  public Participant[] getParticipants() throws MMXException;
  /**
   * Set the listener for this session.
   * @param listener
   */
  public void setSessionListener(SessionListener listener);
}
