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
package com.magnet.mmx.client.api;

import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLHandshakeException;

import org.json.JSONObject;

import com.magnet.mmx.client.MMXAccountManager;
import com.magnet.mmx.client.MMXClient;
import com.magnet.mmx.client.MMXClientConfig;
import com.magnet.mmx.client.MMXTask;
import com.magnet.mmx.client.common.Log;
import com.magnet.mmx.client.common.MMXException;
import com.magnet.mmx.client.common.MMXid;
import com.magnet.mmx.protocol.SearchAction;
import com.magnet.mmx.protocol.StatusCode;
import com.magnet.mmx.protocol.UserInfo;
import com.magnet.mmx.protocol.UserQuery;
import com.magnet.mmx.util.XIDUtil;

/**
 * The MMXUser class
 */
public class MMXUser {
  private static final String TAG = MMXUser.class.getSimpleName();

  /**
   * Failure codes for the MMXUser class.
   */
  public static class FailureCode extends MMX.FailureCode {
    public static final FailureCode REGISTRATION_INVALID_USERNAME = new FailureCode(101, "REGISTRATION_INVALID_USERNAME");
    public static final FailureCode REGISTRATION_USER_ALREADY_EXISTS = new FailureCode(102, "REGISTRATION_USER_ALREADY_EXISTS");
    public static final FailureCode REGISTRATION_NO_DISPLAY_NAME = new FailureCode(103, "REGISTRATION_NO_DISPLAY_NAME"); 
    
    FailureCode(int value, String description) {
      super(value, description);
    }

    FailureCode(MMX.FailureCode code) { super(code); };

    static FailureCode fromMMXFailureCode(MMX.FailureCode code, Throwable throwable) {
      if (throwable instanceof MMXException) {
        return new FailureCode(((MMXException) throwable).getCode(), throwable.getMessage());
      } else {
        return new FailureCode(code);
      }
    }
  }
  
  /**
   * Exception for a group of invalid users.  This exception is returned if the
   * recipients of a message are invalid.
   */
  public static class InvalidUserException extends MMXException {
    private String mMsgId;
    private Set<MMXUser> mUsers = new HashSet<MMXUser>();
    
    public InvalidUserException(String msg, String messageId) {
      super(msg, StatusCode.NOT_FOUND);
      mMsgId = messageId;
    }
    
    public String getMessageId() {
      return mMsgId;
    }
    
    public InvalidUserException addUser(MMXUser user) {
      mUsers.add(user);
      return this;
    }
    
    public Set<MMXUser> getUsers() {
      return mUsers;
    }
  }

  /**
   * The OnFinishedListener for MMXUser methods.
   *
   * @param <T> The type of the onSuccess result
   */
  public static abstract class OnFinishedListener<T> implements IOnFinishedListener<T, FailureCode> {
    /**
     * Called when the operation completes successfully
     *
     * @param result the result of the operation
     */
    public abstract void onSuccess(T result);

    /**
     * Called if the operation fails
     *
     * @param code the failure code
     * @param throwable the throwable associated with this failure (may be null)
     */
    public abstract void onFailure(FailureCode code, Throwable throwable);
  }

  /**
   * The builder for the MMXUser class
   */
  public static final class Builder {
    private MMXUser mUser;

    public Builder() {
      mUser = new MMXUser();
    }

    /**
     * Set the username for this user object
     *
     * @param username the username
     * @return this Builder object
     */
    public Builder username(String username) {
      mUser.username(username);
      return this;
    }

    /**
     * Set the display name for this MMXUser
     *
     * @param displayName the display name
     * @return this Builder object
     */
    public Builder displayName(String displayName) {
      mUser.displayName(displayName);
      return this;
    }

    /**
     * Returns the MMXUser class
     *
     * @return the MMXUser
     */
    public MMXUser build() {
      return mUser;
    }
  }

  private String mUsername;
  private String mDisplayName;

  /**
   * Default constructor
   */
  private MMXUser() {

  }

  /**
   * Set the username for this user object
   *
   * @param username the username
   * @return this MMXUser object
   */
  MMXUser username(String username) {
    mUsername = username;
    return this;
  }

  /**
   * Returns the username of this MMXUser
   *
   * @return the username
   */
  public String getUsername() {
    return mUsername;
  }

  /**
   * Set the display name for this MMXUser
   *
   * @param displayName the display name
   * @return this MMXUser object
   */
  MMXUser displayName(String displayName) {
    mDisplayName = displayName;
    return this;
  }

  /**
   * The display name for this MMXUser
   *
   * @return the display name
   */
  public String getDisplayName() {
    return mDisplayName;
  }

  /**
   * Register a user with the specified username and password.  Possible failure
   * codes are: {@link FailureCode#REGISTRATION_INVALID_USERNAME},
   * {@link FailureCode#REGISTRATION_USER_ALREADY_EXISTS}.
   *
   * @param password the password
   * @param listener the listener, true for success, false otherwise
   */
  public void register(final byte[] password,
                       final OnFinishedListener<Void> listener) {
    MMXTask<Integer> task = new MMXTask<Integer>(MMX.getMMXClient(), MMX.getHandler()) {
      @Override
      public Integer doRun(MMXClient mmxClient) throws Throwable {
        if (!XIDUtil.validateUserId(getUsername())) {
          throw new MMXException("Username is invalid", FailureCode.REGISTRATION_INVALID_USERNAME.getValue());
        }
        if (getDisplayName() == null || getDisplayName().isEmpty()) {
          throw new MMXException("Display name is not specified", FailureCode.REGISTRATION_NO_DISPLAY_NAME.getValue());
        }
        Log.d(TAG, "register(): begin");
        MMXClientConfig config = MMX.getMMXClient().getConnectionInfo().clientConfig;
        HttpURLConnection conn;
        MMXClient.SecurityLevel sec =  config.getSecurityLevel();
        int port = config.getRESTPort();
        if (sec == MMXClient.SecurityLevel.NONE) {
          if (port < 0) {
            port = 5220;//default NON-SSL port
          }
          URL url = new URL("http", config.getHost(), port, "/mmxmgmt/api/v1/users");
          Log.d(TAG, "register(): connecting to " + url.toString());
          conn = (HttpURLConnection) url.openConnection();
        } else {
          if (port < 0) {
            port = 5221;//default SSL port
          }
          URL url = new URL("https", config.getHost(), port, "/mmxmgmt/api/v1/users");
          Log.d(TAG, "register(): connecting to " + url.toString());
          conn = (HttpsURLConnection) url.openConnection();
          if (config.getSecurityLevel() == MMXClient.SecurityLevel.RELAXED) {
            HttpsURLConnection sConn = (HttpsURLConnection) conn;
            sConn.setHostnameVerifier(MMX.getMMXClient().getNaiveHostnameVerifier());
            sConn.setSSLSocketFactory(MMX.getMMXClient().getNaiveSSLContext().getSocketFactory());
          }
        }
        conn.setReadTimeout(10000);
        conn.setConnectTimeout(15000);
        conn.setRequestMethod("POST");
        conn.setDoInput(true);
        conn.setDoOutput(true);
        conn.addRequestProperty("Content-Type", "application/json");
        conn.addRequestProperty("X-mmx-app-id", config.getAppId());
        conn.addRequestProperty("X-mmx-api-key", config.getApiKey());

        JSONObject jsonParam = new JSONObject();
        jsonParam.put("username", getUsername());
        jsonParam.put("password", new String(password));
        jsonParam.put("name", getDisplayName());

        OutputStreamWriter out = new OutputStreamWriter(conn.getOutputStream());
        out.write(jsonParam.toString());
        out.close();

        int resultCode = conn.getResponseCode();
        String resultMessage = conn.getResponseMessage();
        Log.d(TAG, "registerUser(): resultCode=" + resultCode + ", resultMessage=" + resultMessage);
        return resultCode;
      }

      @Override
      public void onException(Throwable exception) {
        if (listener == null) {
          return;
        }
        FailureCode code;
        if (exception instanceof SSLHandshakeException) {
          Log.e(TAG, "register: SSLHandshake exception.  This is most likely because SecurityLevel " +
                  "is configured RELAXED or STRICT but the RESTport is configured as the non-SSL-enabled " +
                  "port.  Typically the non-SSL RESTport is 5220 and the SSL-enabled RESTport is 5221.");
          code = FailureCode.fromMMXFailureCode(MMX.FailureCode.SERVICE_UNAVAILABLE, exception);
        } else {
          Log.d(TAG, "register(): exception caught", exception);
          code = FailureCode.fromMMXFailureCode(FailureCode.SERVER_ERROR, exception);
        }
        listener.onFailure(code, exception);
      }

      @Override
      public void onResult(Integer result) {
        if (listener == null) {
          return;
        }
        Log.d(TAG, "register(): result=" + result);
        switch (result) {
          case 201:
            //success
            listener.onSuccess(null);
            break;
          case 400:
            // most likely that username is too short
            listener.onFailure(FailureCode.REGISTRATION_INVALID_USERNAME, null);
            break;
          case 409:
            listener.onFailure(FailureCode.REGISTRATION_USER_ALREADY_EXISTS, null);
            break;
          default:
            listener.onFailure(FailureCode.fromMMXFailureCode(FailureCode.SERVER_ERROR, null), null);
            break;
        }
      }
    };
    task.execute();
  }

  /**
   * Change the current logged-in user's password
   *
   * @param newPassword the new password
   */
  public void changePassword(final byte[] newPassword,
                             final OnFinishedListener<Void> listener) {
    MMXTask<Void> task = new MMXTask<Void>(MMX.getMMXClient(), MMX.getHandler()) {
      @Override
      public Void doRun(MMXClient mmxClient) throws Throwable {
        mmxClient.getAccountManager().changePassword(new String(newPassword));
        return null;
      }

      @Override
      public void onException(Throwable exception) {
        if (listener != null) {
          listener.onFailure(FailureCode.fromMMXFailureCode(FailureCode.DEVICE_ERROR, exception), exception);
        }
      }

      @Override
      public void onResult(Void result) {
        if (listener != null) {
          listener.onSuccess(result);
        }
      }
    };
    task.execute();
  }

  /**
   * Find users whose display name starts with the specified text.  If the user
   * does not have a display name, the user cannot be found.  Although display 
   * name is optional during the new user registration, it is very useful
   * to have the display name set {@link Builder#displayName(String)}.
   *
   * @param startsWith the search string
   * @param limit the maximum number of users to return
   * @param listener listener for success or failure
   * @deprecated {@link #findByDisplayName(String, int, OnFinishedListener)}
   */
  @Deprecated
  public static void findByName(final String startsWith, final int limit,
      final OnFinishedListener<ListResult<MMXUser>> listener) {
    findByDisplayName(startsWith, limit, listener);
  }

  /**
   * Find users whose display name starts with the specified text.  If the user
   * does not have a display name, the user cannot be found.  Although display
   * name is optional during the new user registration, it is very useful
   * to have the display name set {@link Builder#displayName(String)}.
   *
   * @param startsWith the search string
   * @param limit the maximum number of users to return
   * @param listener listener for success or failure
   */
  @Deprecated
  public static void findByDisplayName(final String startsWith, final int limit,
      final OnFinishedListener<ListResult<MMXUser>> listener) {
    findByDisplayName(startsWith, 0, limit, listener);
  }

    /**
     * Find users whose display name starts with the specified text.  If the user
     * does not have a display name, the user cannot be found.  Although display
     * name is optional during the new user registration, it is very useful
     * to have the display name set {@link Builder#displayName(String)}.
     *
     * @param startsWith the search string
     * @param offset the offset of users to return
     * @param maxRows the maximum number of users to return
     * @param listener listener for success or failure
     */
  public static void findByDisplayName(final String startsWith, final int offset, final int maxRows,
                                  final OnFinishedListener<ListResult<MMXUser>> listener) {
    MMXTask<ListResult<MMXUser>> task = new MMXTask<ListResult<MMXUser>>(MMX.getMMXClient(), MMX.getHandler()) {
      @Override
      public ListResult<MMXUser> doRun(MMXClient mmxClient) throws Throwable {
        UserQuery.Search search = new UserQuery.Search().setDisplayName(startsWith, SearchAction.Match.PREFIX);
        UserQuery.Response response = mmxClient.getAccountManager().searchBy(SearchAction.Operator.AND, search, offset, maxRows);
        List<UserInfo> userInfos = response.getUsers();
        ArrayList<MMXUser> resultList = new ArrayList<MMXUser>();
        for (UserInfo userInfo : userInfos) {
          resultList.add(fromUserInfo(userInfo));
        }
        return new ListResult<MMXUser>(response.getTotalCount(), resultList);
      }

      @Override
      public void onException(Throwable exception) {
        if (listener != null) {
          listener.onFailure(FailureCode.fromMMXFailureCode(FailureCode.DEVICE_ERROR, exception), exception);
        }
      }

      @Override
      public void onResult(ListResult<MMXUser> result) {
        if (listener != null) {
          listener.onSuccess(result);
        }
      }
    };
    task.execute();
  }

  /**
   * Retrieve the MMXUser objects by the specified set of usernames.  The
   * usernames are case-insensitive.
   *
   * @param usernames the usernames to lookup
   * @param listener the listener for the results of this operation
   * @see #getUsername()
   * @deprecated {@link #getByUserNames(HashSet, OnFinishedListener)}
   */
  @Deprecated
  public static void findByNames(final HashSet<String> usernames,
      final OnFinishedListener<HashMap<String, MMXUser>> listener) {
    getByUsernames(usernames, listener);
  }
  
  /**
   * Retrieve the MMXUser objects by the specified set of usernames.  The
   * usernames are case-insensitive.
   *
   * @param usernames the usernames to lookup
   * @param listener the listener for the results of this operation
   * @see #getUsername()
   */
  public static void getByUsernames(final HashSet<String> usernames,
                                 final OnFinishedListener<HashMap<String, MMXUser>> listener) {
    MMXTask<Map<String, UserInfo>> task =
            new MMXTask<Map<String, UserInfo>>(MMX.getMMXClient(), MMX.getHandler()) {
      @Override
      public Map<String, UserInfo> doRun(MMXClient mmxClient) throws Throwable {
        MMXAccountManager am = mmxClient.getAccountManager();
        return am.getUserInfo(usernames);
      }

      @Override
      public void onException(Throwable exception) {
        if (listener != null) {
          listener.onFailure(FailureCode.fromMMXFailureCode(FailureCode.DEVICE_ERROR, exception), exception);
        }
      }

      @Override
      public void onResult(Map<String, UserInfo> result) {
        if (listener == null) {
          return;
        }
        HashMap<String, MMXUser> results = new HashMap<String, MMXUser>();
        for (Map.Entry<String, UserInfo> entry : result.entrySet()) {
          results.put(entry.getKey(), fromUserInfo(entry.getValue()));
        }
        listener.onSuccess(results);
      }
    };
    task.execute();
  }

  static MMXUser fromUserInfo(UserInfo userInfo) {
    return new MMXUser.Builder()
            .displayName(userInfo.getDisplayName())
            .username(userInfo.getUserId())
            .build();
  }

  static MMXUser fromMMXid(MMXid mmxId) {
    return new MMXUser.Builder()
            .displayName(mmxId.getDisplayName())
            .username(mmxId.getUserId())
            .build();
  }
  /**
   * Implementation of the equals method that checks username equality only (case-insensitive)
   *
   * @param o
   * @return true if equal
   */
  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    MMXUser mmxUser = (MMXUser) o;

    return mUsername.equalsIgnoreCase(mmxUser.mUsername);

  }

  @Override
  public int hashCode() {
    return mUsername.hashCode();
  }

  @Override
  public String toString() {
    return "[name="+mUsername+", display="+mDisplayName+"]";
  }
  
  /**
   * Validates the specified userId.  Returns true for a valid userId.
   * @param userId the user id to validate
   * @return true if valid, false otherwise
   */
  public static boolean isValidUserId(String userId) {
    return XIDUtil.validateUserId(userId);
  }
}
