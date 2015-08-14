package com.magnet.mmx.client.api;

import com.magnet.mmx.client.MMXAccountManager;
import com.magnet.mmx.client.MMXClient;
import com.magnet.mmx.client.MMXTask;
import com.magnet.mmx.protocol.MMXStatus;
import com.magnet.mmx.protocol.SearchAction;
import com.magnet.mmx.protocol.UserInfo;
import com.magnet.mmx.protocol.UserQuery;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The MMXUser class
 */
public class MMXUser {
  private static final String TAG = MMXUser.class.getSimpleName();

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
    public final Set<MMXUser> users;

    private FindResult(int totalCount, Set<MMXUser> users) {
      this.totalCount = totalCount;
      this.users = Collections.unmodifiableSet(users);
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
   * @param password the password
   * @param listener the listener, true for success, false otherwise
   */
  public void register(final byte[] password,
                       final MMX.OnFinishedListener<Void> listener) {
    final boolean isConnected = MMX.getMMXClient().isConnected();
    final MMX.MMXStatusTask createAccountTask =
            new MMX.MMXStatusTask(listener) {
      @Override
      public MMXStatus doRun(MMXClient mmxClient) throws Throwable {
        try {
          MMXAccountManager.Account account = new MMXAccountManager.Account()
                  .username(getUsername())
                  .email(getEmail())
                  .displayName(getDisplayName())
                  .password(password);
          return mmxClient.getAccountManager().createAccount(account);
        } finally {
          if (!isConnected) {
            MMX.logout(null);
          }
        }
      }
    };

    if (!isConnected) {
      MMX.loginAnonymous(new MMX.OnFinishedListener<Void>() {
        public void onSuccess(Void result) {
          createAccountTask.execute();
        }

        public void onFailure(MMX.FailureCode code, Throwable ex) {
          listener.onFailure(code, ex);
        }
      });
    } else {
      createAccountTask.execute();
    }
  }

  /**
   * Change the current logged-in user's password
   *
   * @param newPassword the new password
   */
  public void changePassword(final byte[] newPassword,
                             final MMX.OnFinishedListener<Void> listener) {
    MMXTask<Void> task = new MMXTask<Void>(MMX.getMMXClient(), MMX.getHandler()) {
      @Override
      public Void doRun(MMXClient mmxClient) throws Throwable {
        mmxClient.getAccountManager().changePassword(new String(newPassword));
        return null;
      }

      @Override
      public void onException(Throwable exception) {
        if (listener != null) {
          listener.onFailure(MMX.FailureCode.SERVER_EXCEPTION, exception);
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
   * Finds users whose display name starts with the specified text
   *
   * @param startsWith the search string
   * @param limit the maximum number of users to return
   * @param listener listener for success or failure
   */
  public static void findByName(final String startsWith, final int limit,
                                      final MMX.OnFinishedListener<FindResult> listener) {
    MMXTask<FindResult> task = new MMXTask<FindResult>(MMX.getMMXClient(), MMX.getHandler()) {
      @Override
      public FindResult doRun(MMXClient mmxClient) throws Throwable {
        UserQuery.Search search = new UserQuery.Search().setDisplayName(startsWith, SearchAction.Match.PREFIX);
        UserQuery.Response response = mmxClient.getAccountManager().searchBy(SearchAction.Operator.AND, search, limit);
        List<UserInfo> userInfos = response.getUsers();
        ArrayList<MMXUser> resultList = new ArrayList<MMXUser>();
        for (UserInfo userInfo : userInfos) {
          resultList.add(new MMXUser.Builder()
                  .username(userInfo.getUserId())
                  .displayName(userInfo.getDisplayName())
                  .email(userInfo.getEmail())
                  .build());
        }
        return new FindResult(response.getTotalCount(), new HashSet<MMXUser>(resultList));
      }

      @Override
      public void onException(Throwable exception) {
        if (listener != null) {
          listener.onFailure(MMX.FailureCode.SERVER_EXCEPTION, exception);
        }
      }

      @Override
      public void onResult(FindResult result) {
        if (listener != null) {
          listener.onSuccess(result);
        }
      }
    };
    task.execute();
  }
}
