package com.magnet.mmx.client.api;

import com.magnet.mmx.client.MMXAccountManager;
import com.magnet.mmx.client.MMXClient;
import com.magnet.mmx.client.common.*;
import com.magnet.mmx.client.common.MMXMessage;
import com.magnet.mmx.protocol.MMXStatus;
import com.magnet.mmx.protocol.MMXTopic;

/**
 * The MMXUser class
 */
public class MMXUser {
  private static final String TAG = MMXUser.class.getSimpleName();
  private String mUsername;
  private String mDisplayName;
  private String mEmail;

  /**
   * Default constructor
   */
  public MMXUser() {

  }

  /**
   * Set the username for this user object
   *
   * @param username the username
   * @return this MMXUser object
   */
  public MMXUser username(String username) {
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
  public MMXUser displayName(String displayName) {
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
  public MMXUser email(String email) {
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
            .displayName(user.getDisplayName());
    MMXStatus status = client.getAccountManager().createAccount(account);
    if (status.getCode() == MMXStatus.SUCCESS) {
      listener.onSuccess(Boolean.TRUE);
    } else {
      Log.e(TAG, "register() calling onFailure because of MMXStatus: " + status);
      listener.onFailure(MagnetMessage.FailureCode.SERVER_ERROR, null);
    }
  }

  /**
   * Authenticate the current session with the specified username and password
   *
   * @param username the username
   * @param password the password
   */
  public static void login(String username, byte[] password,
                           final MagnetMessage.OnFinishedListener<Boolean> listener) {
    MagnetMessage.getGlobalListener().registerListener(new MMXClient.MMXListener() {
      public void onConnectionEvent(MMXClient client, MMXClient.ConnectionEvent event) {
        
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
    MagnetMessage.connectWithCredentials(username, password);
  }

  /**
   * Logout of the current session.
   */
  public static void logout() {

  }

  /**
   * Change the current logged-in user's password
   *
   * @param newPassword the new password
   */
  public static void changePassword(byte[] newPassword) {
    MMXClient client = MagnetMessage.getMMXClient();
    try {
      client.getAccountManager().changePassword(new String(newPassword));
    } catch (MMXException e) {
      e.printStackTrace();
    }
  }
}
