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

import java.util.HashSet;
import java.util.Set;

import com.magnet.mmx.protocol.StatusCode;

/**
 * An exception holding a set of invalid users.  It is thrown if a message is
 * sent to a number of invalid recipients.
 *
 */
public class UserNotFoundException extends MMXException {
  private Set<MMXid> mUsers = new HashSet<MMXid>();

  public UserNotFoundException(String msg) {
    super(msg, StatusCode.NOT_FOUND);
  }

  /**
   * Add a user to a not found group.
   * @param userId
   */
  public void addUser(MMXid user) {
    mUsers.add(user);
  }

  /**
   * Get a group of users that are not found or invalid.
   * @return
   */
  public Set<MMXid> getUsers() {
    return mUsers;
  }
}
