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
import android.util.Log;

import com.magnet.mmx.client.common.AccountManager;
import com.magnet.mmx.client.common.MMXConnection;
import com.magnet.mmx.client.common.MMXConnectionListener;
import com.magnet.mmx.client.common.MMXException;
import com.magnet.mmx.client.common.MMXid;
import com.magnet.mmx.protocol.Constants;
import com.magnet.mmx.protocol.MMXStatus;
import com.magnet.mmx.protocol.SearchAction;
import com.magnet.mmx.protocol.UserCreate;
import com.magnet.mmx.protocol.UserId;
import com.magnet.mmx.protocol.UserInfo;
import com.magnet.mmx.protocol.UserQuery;
import com.magnet.mmx.protocol.UserTags;
import com.magnet.mmx.util.XIDUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Account Manager allows user to change the password, update the display
 * name and email address, and query for users.
 */
public class MMXAccountManager extends MMXManager {
  private static final String TAG = MMXAccountManager.class.getSimpleName();
  private AccountManager mAccountManager = null;

  MMXAccountManager(MMXClient mmxClient, Handler handler) {
    super(mmxClient, handler);
    onConnectionChanged();
  }

  /**
   * Add tags to the current user.  The tags must not be null or empty, and
   * each tag cannot be longer than 25 characters; otherwise MMXException with
   * BAD_REQUEST status code will be thrown.
   *
   * @param tags the tags to add for the current user
   * @return the status of the call
   * @throws MMXException
   */
  public MMXStatus addTags(List<String> tags) throws MMXException {
    checkDestroyed();
    return mAccountManager.addTags(tags);
  }

  /**
   * Change the password for the current authenticated user.
   *
   * @param newPassword the new password for the user
   * @throws MMXException
   */
  public void	changePassword(String newPassword) throws MMXException {
    checkDestroyed();
    mAccountManager.changePassword(newPassword);
  }

  /**
   * Get the tags for the current user.
   *
   * @return the user tags for the current user
   * @throws MMXException
   */
  public UserTags getAllTags() throws MMXException {
    checkDestroyed();
    return mAccountManager.getAllTags();
  }

  /**
   * Get the account information of the current user.
   *
   * @return the user info for the current user
   * @throws MMXException
   */
  public UserInfo getUserInfo() throws MMXException {
    checkDestroyed();
    return mAccountManager.getUserInfo();
  }

  /**
   * Remove tags from the current user.  The tags must not be null or empty, and
   * each tag cannot be longer than 25 characters; otherwise MMXException with
   * BAD_REQUEST status code will be thrown.
   *
   * @param tags the tags to remove
   * @return the status for this request
   * @throws MMXException
   */
  public MMXStatus removeTags(List<String> tags) throws MMXException {
    checkDestroyed();
    return mAccountManager.removeTags(tags);
  }

  /**
   * Set the tags for the current user.  If the list is null or empty, all tags
   * will be removed.
   *
   * @param tags A list of tags, or null.
   * @return The status.
   * @throws MMXException
   */
  public MMXStatus setAllTags(List<String> tags) throws MMXException {
    checkDestroyed();
    return mAccountManager.setAllTags(tags);
  }

  /**
   * Search for users with the matching attributes.
   *
   * @param operator the operator for this search
   * @param attributes the attributes for this search
   * @param maxRows the maximum number of rows to return
   * @return the users matching the search criteria
   * @throws MMXException
   */
  public UserQuery.Response	searchBy(SearchAction.Operator operator,
                                      UserQuery.Search attributes, Integer maxRows) throws MMXException {
    checkDestroyed();
    return mAccountManager.searchBy(operator, attributes, maxRows);
  }

  /**
   * Update the current user's account info.  The userId in <code>info</code>
   * cannot be changed and it is ignored.
   *
   * @param info the updated information
   * @return the status for the current operation
   * @throws MMXException
   */
  public MMXStatus updateAccount(UserInfo info) throws MMXException {
    checkDestroyed();
    return mAccountManager.updateAccount(info);
  }

  /**
   * Creates an account with the specified parameters.  This method can be called
   * while disconnected or connected.  If the username already exists, the result code
   * will be {@link MMXStatus#CONFLICT}.  If the username is invalid, the result code will
   * be {@link MMXStatus#BAD_REQUEST}.
   *
   * @param account the account to create
   * @return the result {@link MMXStatus#getCode()}
   */
  public MMXStatus createAccount(Account account) {
    MMXClient client = getMMXClient();
    if (!client.isValidUsername(account.getUsername())) {
      return new MMXStatus().setCode(MMXStatus.BAD_REQUEST).setMessage(
              "invalid username specified: " + account.getUsername());
    }
    if (account.getPassword() == null || account.getPassword().length == 0) {
      return new MMXStatus().setCode(MMXStatus.BAD_REQUEST).setMessage(
              "invalid password specified");
    }
    MMXConnection connection = client.getMMXConnection();
    boolean needsConnect = !connection.isConnected();
    if (needsConnect) {
      try {
        connection.connect(null, client.getHostnameVerifierOverride(),
                client.getSocketFactoryOverride(), client.getSSLContextOverride());
      } catch (MMXException e) {
        Log.e(TAG, "createAccount(): exception caught during connect", e);
        return new MMXStatus().setCode(MMXStatus.INTERNAL_ERROR).setMessage(e.getMessage());
      }
      if (!connection.isConnected()) {
        return new MMXStatus().setCode(MMXStatus.INTERNAL_ERROR).setMessage("Connection timeout; unable to connect.");
      }
    }

    try {
      MMXClientConfig config = client.getConfig();
      return mAccountManager.createAccount(new UserCreate()
                      .setAppId(config.getAppId())
                      .setApiKey(config.getApiKey())
                      .setPriKey(config.getAnonymousSecret())
                      .setDisplayName(account.getDisplayName())
                      .setEmail(account.getEmail())
                      .setUserId(account.getUsername())
                      .setPassword(new String(account.getPassword()))
                      .setCreateMode(Constants.UserCreateMode.UPGRADE_USER)
      );
    } catch (MMXException e) {
      Log.e(TAG, "createAccount(): exception caught", e);
      return new MMXStatus().setCode(MMXStatus.INTERNAL_ERROR).setMessage(e.getMessage());
    } finally {
      if (needsConnect) {
        //we did the connection, so disconnect
        connection.disconnect();
      }
    }
  }

  /**
   * Get user information of multiple users by their user ID's.
   * @param uids A set of unescaped user ID's.
   * @return A map of user ID (key) and user Info (value).
   * @throws MMXException
   */
  public Map<String, UserInfo> getUserInfo(Set<String> uids) throws MMXException {
    checkDestroyed();
    return mAccountManager.getUserInfo(uids);
  }

  /**
   * Get a user information of the specified user ID.
   * @param uid An un-escaped user ID.
   * @return A user info.
   * @throws MMXException User not found.
   */
  public UserInfo getUserInfo(String uid) throws MMXException {
    checkDestroyed();
    return mAccountManager.getUserInfo(uid);
  }

  @Override
  void onConnectionChanged() {
    mAccountManager = AccountManager.getInstance(getMMXClient().getMMXConnection());
  }

  /**
   * To be used with {@link #createAccount(Account)}.
   */
  public static final class Account {
    private String mUsername;
    private byte[] mPassword;
    private String mDisplayName;
    private String mEmail;

    public Account() {
    }

    /**
     * Set the username for this account
     * @param username the username
     */
    public void setUsername(String username) {
      mUsername = username;
    }

    /**
     * Set the username for this account
     * @param username the username
     * @return this account instance
     */
    public Account username(String username) {
      setUsername(username);
      return this;
    }

    /**
     * Retreive the username for this account
     * @return the username
     */
    public String getUsername() {
      return mUsername;
    }

    /**
     * Set the password for this account
     * @param password the password
     */
    public void setPassword(byte[] password) {
      mPassword = password;
    }

    /**
     * Set the password for this account
     * @param password the password
     * @return this account instance
     */
    public Account password(byte[] password) {
      setPassword(password);
      return this;
    }

    /**
     * Retreive the password for this account
     * @return the password
     */
    public byte[] getPassword() {
      return mPassword;
    }

    /**
     * Set the display name for this account
     * @param displayName the display name
     */
    public void setDisplayName(String displayName) {
      mDisplayName = displayName;
    }

    /**
     * Set the display name for this account
     * @param displayName the display name
     * @return this account instance
     */
    public Account displayName(String displayName) {
      setDisplayName(displayName);
      return this;
    }

    /**
     * Retreive the display name for this account
     * @return the display name
     */
    public String getDisplayName() {
      return mDisplayName;
    }

    /**
     * Set the email for this account
     * @param email the email
     */
    public void setEmail(String email) {
      mEmail = email;
    }

    /**
     * Set the email for this account
     * @param email the email
     * @return this account instance
     */
    public Account email(String email) {
      setEmail(email);
      return this;
    }

    /**
     * Retreive the email for this account
     * @return the email
     */
    public String getEmail() {
      return mEmail;
    }
  }
}
