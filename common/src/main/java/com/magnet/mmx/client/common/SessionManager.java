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

import java.util.Date;

public class SessionManager {
  private final static String TAG = "SessionManager";
  private MMXConnection mCon;
  private final static Creator sCreator = new Creator() {
    @Override
    public Object newInstance(MMXConnection con) {
      return new SessionManager(con);
    }
  };

  public enum SessionType {
    /**
     * All historic messages are kept until the session is terminated.
     */
    DATA_SESSION("data"),
    /**
     * No historic messages are kept.
     */
    STREAM_SESSION("stream"),
    /**
     * Only the recent historic messages are kept.  The number of messages
     * being kept is configured.
     */
    CONFERENCE_SESSION("conference");

    private String mType;

    SessionType(String type) {
      mType = type;
    }

    @Override
    public String toString() {
      return mType;
    }
  }

  /**
   * @hide
   * Get the instance of SesisonManager.
   * @param con
   * @return
   */
  public static SessionManager getInstance(MMXConnection con) {
    return (SessionManager) con.getManager(TAG, sCreator);
  }

  protected SessionManager(MMXConnection con) {
    mCon = con;
  }

  public Session getSessionById(String sessionId) {
    return null;
  }

  /**
   * Create a multi-users session.
   * @param type
   * @param password
   * @return
   */
  public Session create(SessionType type, String password,
                         SessionListener listener) {
    return null;
  }

  /**
   * Join a session.
   * @param sessionId
   * @param password
   * @param since Get the historic messages since
   * @param listener
  d * @throws MMXException
   */
  public Session join(String sessionId, String password, Date since,
                    SessionListener listener) throws MMXException {
    return null;
  }

  /**
   * Leave the session.
   * @param sessionId
   * @throws MMXException
   */
  public void leave(String sessionId) throws MMXException {

  }
}
