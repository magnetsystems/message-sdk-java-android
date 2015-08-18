package com.magnet.mmx.client.api;

import com.magnet.mmx.client.MMXAccountManager;
import com.magnet.mmx.client.MMXClient;
import com.magnet.mmx.client.MMXClientConfig;
import com.magnet.mmx.client.MMXTask;
import com.magnet.mmx.client.common.Log;
import com.magnet.mmx.protocol.MMXStatus;
import com.magnet.mmx.protocol.SearchAction;
import com.magnet.mmx.protocol.UserInfo;
import com.magnet.mmx.protocol.UserQuery;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLHandshakeException;

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
   * Register a user with the specified username and password
   *
   * @param password the password
   * @param listener the listener, true for success, false otherwise
   */
  public void register(final byte[] password,
                       final MMX.OnFinishedListener<Void> listener) {
    MMXTask<Integer> task = new MMXTask<Integer>(MMX.getMMXClient(), MMX.getHandler()) {
      @Override
      public Integer doRun(MMXClient mmxClient) throws Throwable {
        Log.d(TAG, "register(): begin");
        MMXClientConfig config = MMX.getMMXClient().getConnectionInfo().clientConfig;
        HttpURLConnection conn;
        int port = 5220;
        if (config.getRESTPort() > 0) {
          port = config.getRESTPort();
        }
        MMXClient.SecurityLevel sec =  config.getSecurityLevel();
        if (sec == MMXClient.SecurityLevel.NONE) {
          URL url = new URL("http", config.getHost(), port, "/mmxmgmt/api/v1/users");
          conn = (HttpURLConnection) url.openConnection();
        } else {
          URL url = new URL("https", config.getHost(), port, "/mmxmgmt/api/v1/users");
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
        if (exception != null && exception instanceof SSLHandshakeException) {
          Log.e(TAG, "register: SSLHandshake exception.  This is most likely because SecurityLevel " +
                  "is configured RELAXED or STRICT but the RESTport is configured as the non-SSL-enabled " +
                  "port.  Typically the non-SSL RESTport is 5220 and the SSL-enabled RESTport is 5221.");
        } else {
          Log.d(TAG, "register(): exception caught", exception);
        }
        listener.onFailure(MMX.FailureCode.DEVICE_ERROR, exception);
      }

      @Override
      public void onResult(Integer result) {
        Log.d(TAG, "register(): result=" + result);
        switch (result) {
          case 201:
            //success
            listener.onSuccess(null);
            break;
          default:
            listener.onFailure(MMX.FailureCode.SERVER_BAD_STATUS, null);
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
}
