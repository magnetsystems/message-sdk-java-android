package com.magnet.mmx.client.api;

import com.magnet.mmx.client.MMXAccountManager;
import com.magnet.mmx.client.MMXClient;
import com.magnet.mmx.client.common.*;
import com.magnet.mmx.client.common.MMXMessage;
import com.magnet.mmx.protocol.MMXStatus;
import com.magnet.mmx.protocol.MMXTopic;
import com.magnet.mmx.protocol.SearchAction;
import com.magnet.mmx.protocol.UserInfo;
import com.magnet.mmx.protocol.UserQuery;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The MMXUser class
 */
public class MMXUser {
  private static final String TAG = MMXUser.class.getSimpleName();
  private static MMXUser sCurrentUser = null;

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
     * Set the email for this MMXUser
     *
     * @param email the email
     * @return this Builder object
     */
    public Builder email(String email) {
      mUser.email(email);
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

  /**
   * The result object returned by the "find" methods
   */
  public static class FindResult {
    public final int totalCount;
    public final List<MMXUser> users;

    private FindResult(int totalCount, List<MMXUser> users) {
      this.totalCount = totalCount;
      this.users = Collections.unmodifiableList(users);
    }
  }


  private String mUsername;
  private String mDisplayName;
  private String mEmail;

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
   * Set the email for this MMXUser
   *
   * @param email the email
   * @return this MMXUser object
   */
  MMXUser email(String email) {
    mEmail = email;
    return this;
  }

  /**
   * The email address for this user
   *
   * @return the email address
   */
  public String getEmail() {
    return mEmail;
  }

  /**
   * Register a user with the specified username and password
   *
   * @param user the user to register
   * @param password the password
   * @param listener the listener, true for success, false otherwise
   */
  public static void register(MMXUser user, byte[] password,
                              MagnetMessage.OnFinishedListener<Boolean> listener) {
    MMXClient client = MagnetMessage.getMMXClient();
    MMXAccountManager.Account account = new MMXAccountManager.Account()
            .username(user.getUsername())
            .email(user.getEmail())
            .displayName(user.getDisplayName())
            .password(password);
    MMXStatus status = client.getAccountManager().createAccount(account);
    if (status.getCode() == MMXStatus.SUCCESS) {
      listener.onSuccess(Boolean.TRUE);
    } else {
      Log.e(TAG, "register() calling onFailure because of MMXStatus: " + status);
      listener.onFailure(MagnetMessage.FailureCode.SERVER_BAD_STATUS, null);
    }
  }

  /**
   * Authenticate the current session with the specified username and password
   *
   * @param username the username
   * @param password the password
   */
  public static void login(String username, byte[] password,
                           final MagnetMessage.OnFinishedListener<Void> listener) {
    MagnetMessage.getGlobalListener().registerListener(new MMXClient.MMXListener() {
      public void onConnectionEvent(MMXClient client, MMXClient.ConnectionEvent event) {
        Log.d(TAG, "login() received connection event: " + event);
        boolean unregisterListener = false;
        switch (event) {
          case AUTHENTICATION_FAILURE:
            listener.onFailure(MagnetMessage.FailureCode.SERVER_AUTH_FAILED, null);
            unregisterListener = true;
            break;
          case CONNECTED:
            try {
              UserInfo info = client.getAccountManager().getUserInfo();
              sCurrentUser = new MMXUser.Builder()
                      .email(info.getEmail())
                      .username(info.getUserId())
                      .displayName(info.getDisplayName())
                      .build();
            } catch (MMXException ex) {
              Log.e(TAG, "login(): login succeeded but unable to retrieve user info", ex);
            }
            listener.onSuccess(null);
            unregisterListener = true;
            break;
          case CONNECTION_FAILED:
            listener.onFailure(MagnetMessage.FailureCode.DEVICE_CONNECTION_FAILED, null);
            unregisterListener = true;
            break;
        }
        if (unregisterListener) {
          MagnetMessage.getGlobalListener().unregisterListener(this);
        }
      }

      public void onMessageReceived(MMXClient client, MMXMessage message, String receiptId) {

      }

      public void onSendFailed(MMXClient client, String messageId) {

      }

      public void onMessageDelivered(MMXClient client, MMXid recipient, String messageId) {

      }

      public void onPubsubItemReceived(MMXClient client, MMXTopic topic, MMXMessage message) {

      }

      public void onErrorReceived(MMXClient client, MMXErrorMessage error) {

      }
    });
    sCurrentUser = null;
    MagnetMessage.getMMXClient().connectWithCredentials(username, password, MagnetMessage.getGlobalListener(),
            new MMXClient.ConnectionOptions().setAutoCreate(false));
  }

  /**
   * Logout of the current session.
   */
  public static void logout(final MagnetMessage.OnFinishedListener<Void> listener) {
    MagnetMessage.getGlobalListener().registerListener(new MMXClient.MMXListener() {
      public void onConnectionEvent(MMXClient client, MMXClient.ConnectionEvent event) {
        Log.d(TAG, "logout() received connection event: " + event);
        boolean unregisterListener = false;
        switch (event) {
          case AUTHENTICATION_FAILURE:
            listener.onFailure(MagnetMessage.FailureCode.SERVER_AUTH_FAILED, null);
            unregisterListener = true;
            break;
          case CONNECTED:
            listener.onSuccess(null);
          case CONNECTION_FAILED:
            listener.onFailure(MagnetMessage.FailureCode.DEVICE_CONNECTION_FAILED, null);
            unregisterListener = true;
            break;
        }
        if (unregisterListener) {
          MagnetMessage.getGlobalListener().unregisterListener(this);
        }
      }

      public void onMessageReceived(MMXClient client, MMXMessage message, String receiptId) {

      }

      public void onSendFailed(MMXClient client, String messageId) {

      }

      public void onMessageDelivered(MMXClient client, MMXid recipient, String messageId) {

      }

      public void onPubsubItemReceived(MMXClient client, MMXTopic topic, MMXMessage message) {

      }

      public void onErrorReceived(MMXClient client, MMXErrorMessage error) {

      }
    });
    sCurrentUser = null;
    MagnetMessage.getMMXClient().goAnonymous();
  }

  /**
   * Retrieves the current user for this session.  This may return
   * null if there is no logged-in user.
   *
   * @return the current user, null there is no logged-in user
   */
  public static MMXUser getCurrentUser() {
    return sCurrentUser;
  }

  /**
   * Change the current logged-in user's password
   *
   * @param newPassword the new password
   */
  public static void changePassword(byte[] newPassword,
                                    final MagnetMessage.OnFinishedListener<Void> listener) {
    MMXClient client = MagnetMessage.getMMXClient();
    try {
      client.getAccountManager().changePassword(new String(newPassword));
      listener.onSuccess(null);
    } catch (MMXException e) {
      listener.onFailure(MagnetMessage.FailureCode.SERVER_EXCEPTION, e);
    }
  }

  /**
   * Finds users whose display name starts with the specified text
   *
   * @param startsWith the search string
   * @param limit the maximum number of users to return
   * @return the result, null if the results are unavailable
   */
  public static FindResult findByName(String startsWith, int limit) {
    MMXClient client = MagnetMessage.getMMXClient();
    try {
      UserQuery.Search search = new UserQuery.Search().setDisplayName(startsWith, SearchAction.Match.PREFIX);
      UserQuery.Response response = client.getAccountManager().searchBy(SearchAction.Operator.AND, search, limit);
      List<UserInfo> userInfos = response.getUsers();
      ArrayList<MMXUser> resultList = new ArrayList<MMXUser>();
      for (UserInfo userInfo : userInfos) {
        resultList.add(new MMXUser.Builder()
                .username(userInfo.getUserId())
                .displayName(userInfo.getDisplayName())
                .email(userInfo.getEmail())
                .build());
      }
      return new FindResult(response.getTotalCount(), resultList);
    } catch (MMXException e) {
      Log.e(TAG, "findByName(): failed because of exception", e);
      return null;
    }
  }
}
