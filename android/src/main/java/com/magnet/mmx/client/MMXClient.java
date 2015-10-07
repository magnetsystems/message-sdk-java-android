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

import java.io.IOException;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.net.SocketFactory;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.http.conn.ssl.AllowAllHostnameVerifier;
import org.jivesoftware.smack.SmackAndroid;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;

import com.magnet.mmx.client.common.DeviceManager;
import com.magnet.mmx.client.common.Invitation;
import com.magnet.mmx.client.common.Log;
import com.magnet.mmx.client.common.MMXConnection;
import com.magnet.mmx.client.common.MMXContext;
import com.magnet.mmx.client.common.MMXErrorMessage;
import com.magnet.mmx.client.common.MMXException;
import com.magnet.mmx.client.common.MMXMessage;
import com.magnet.mmx.client.common.MMXMessageListener;
import com.magnet.mmx.client.common.MMXPayload;
import com.magnet.mmx.client.common.MMXSettings;
import com.magnet.mmx.protocol.AuthData;
import com.magnet.mmx.protocol.CarrierEnum;
import com.magnet.mmx.protocol.Constants;
import com.magnet.mmx.protocol.DevReg;
import com.magnet.mmx.protocol.MMXError;
import com.magnet.mmx.protocol.MMXStatus;
import com.magnet.mmx.protocol.MMXTopic;
import com.magnet.mmx.protocol.MMXid;
import com.magnet.mmx.protocol.OSType;
import com.magnet.mmx.protocol.PushType;
import com.magnet.mmx.util.DefaultEncryptor;

/**
 * The primary entry point for interacting with MMX.  The named MMXClient instances are
 * managed by this class.  In most cases, when retrieving an MMXClient instance with getInstance()
 * a name doesn't need to be provided. There are two ways to configure the MMXClient instance.
 *
 * 1.  Using the properties file in res/raw and providing the getInstance() method with the id
 * 2.  Implementing an MMXClientConfig class and passing it to the getInstance() method.
 */
public final class MMXClient {
  private static final String TAG = MMXClient.class.getSimpleName();
  private static final char[] USERNAME_INVALID_CHARS;
  private static final int USERNAME_LENGTH_MIN = 1;
  private static final int USERNAME_LENGTH_MAX = 40;
  private static final String USERNAME_INVALID_CHARS_STR = 
      Constants.AT_SIGN_DISALLOWED ? "%,/,@,&" : "%,/,&";

  static {
    int start = 0, i = 0;
    int len = USERNAME_INVALID_CHARS_STR.length();
    USERNAME_INVALID_CHARS = new char[(len+1)/2];
    while ((start = USERNAME_INVALID_CHARS_STR.indexOf(',', start)) > 0) {
      USERNAME_INVALID_CHARS[i++] = USERNAME_INVALID_CHARS_STR.charAt(start-1);
      ++start;
    }
    USERNAME_INVALID_CHARS[i] = USERNAME_INVALID_CHARS_STR.charAt(len-1);
  }

  /**
   * Options to specify for a connection.  By default, this class will specify
   * <code>
   * autoCreate = false
   * suspendDelivery = false
   * </code>
   *
   * This means that in the default case, the user will NOT be auto-created if they don't
   * exist and messages will delivered to the callback upon connecting.
   */
  public static final class ConnectionOptions {
    private boolean mAutoCreate = false;
    private boolean mSuspendDelivery = false;

    //These are used in MMXClient internally and may be exposed in the future
    private boolean mAnonymous = false;
    private String mUsername = null;
    private String mPassword = null;

    /**
     * The auto-creation flag.
     *
     * @return true if the user will be auto-created
     */
    public boolean isAutoCreate() {
      return mAutoCreate;
    }

    /**
     * Set the auto-create flag for this options object.
     *
     * @param autoCreate true if the user should be auto-created, false otherwise
     * @return this builder object
     */
    public ConnectionOptions setAutoCreate(boolean autoCreate) {
      mAutoCreate = autoCreate;
      return this;
    }

    /**
     * The current state for the "suspend delivery" option.
     *
     * @return true if delivery will be suspended upon connect
     * @see MMXClient#resumeDelivery()
     */
    public boolean isSuspendDelivery() {
      return mSuspendDelivery;
    }

    /**
     * Set the suspend delivery flag for this options object.  If true, messages will not be delivered
     * to the callback until resumeDelivery() is called.
     *
     * @param suspendDelivery true if message delivery should be suspended upon connecting, false otherwise
     * @return this builder object
     * @see com.magnet.mmx.client.MMXClient#resumeDelivery()
     */
    public ConnectionOptions setSuspendDelivery(boolean suspendDelivery) {
      mSuspendDelivery = suspendDelivery;
      return this;
    }

    private boolean isAnonymous() {
      return mAnonymous;
    }

    private ConnectionOptions setAnonymous(boolean anonymous) {
      mAnonymous = anonymous;
      return this;
    }

    private String getUsername() {
      return mUsername;
    }

    private ConnectionOptions setUsername(String username) {
      mUsername = username;
      return this;
    }

    private String getPassword() {
      return mPassword;
    }

    private ConnectionOptions setPassword(String password) {
      mPassword = password;
      return this;
    }

    protected ConnectionOptions clone() {
      return new ConnectionOptions()
              .setSuspendDelivery(mSuspendDelivery).setAutoCreate(mAutoCreate)
              .setAnonymous(mAnonymous).setUsername(mUsername).setPassword(mPassword);
    }
  }

  /**
   * The enumeration of events that will cause MMXListener.onConnectionEvent() to be invoked.
   */
  public enum ConnectionEvent {
    /**
     * Successfully connected
     */
    CONNECTED,
    /**
     * Reconnecting to the server
     */
    RECONNECTING,
    /**
     * Authentication failed
     */
    AUTHENTICATION_FAILURE,
    /**
     * Unable to connect to the server
     */
    CONNECTION_FAILED,
    /**
     * Disconnected from server
     */
    DISCONNECTED,
    /**
     * Registration with the wakeup service failed (GCM)
     */
    WAKEUP_REGISTRATION_FAILED
  }

  /**
   * The various security levels.
   */
  public enum SecurityLevel {
    /**
     * No security.  This will not force TLS.
     */
    NONE,

    /**
     * Strict security.  Forces TLS and valid SSL certs/hostnames
     */
    STRICT,

    /**
     * Relaxed security.  Forces TLS, but allows self-signed certs and invalid hostnames
     */
    RELAXED
  }

  private static final HashMap<String, MMXClient> sInstanceMap = new HashMap<String, MMXClient>();
  private static final String DEFAULT_MMX_NAME = "__DEFAULT__";
  private static final int TCP_CONNECTION_TIMEOUT = 7000;
  private static final String SHARED_PREF_NAME = "com.magnet.mmx.MMXClient-";
  private static final String SHARED_PREF_KEY_GCM_REGID = "GCM_REGID";
  private static final String SHARED_PREF_KEY_GCM_REGID_APPVERSION = "GCM_REGID_APPVERSION";
  private static final String SHARED_PREF_KEY_GCM_WAKEUP_ENABLED = "WAKEUP_ENABLED";
  private static final String SHARED_PREF_KEY_AUTH_MODE = "AUTH_MODE";

  //config items
//  private static final String SHARED_PREF_KEY_CONFIG_DEBUG_DEVICE_ID = "DEBUG_DEVICE_ID";

  //There is only one registered wakeup listener for all instances.
  private static final String STATIC_SHARED_PREF_NAME = MMXClient.class.getSimpleName();
  private static final String STATIC_SHARED_PREF_KEY_WAKEUP_LISTENER_CLASS = "WAKEUP_LISTENER_CLASS";
  private static final String STATIC_SHARED_PREF_KEY_WAKEUP_INTERVAL = "WAKEUP_INTERVAL";

  /**
   * Intent used to invoke the MMXClient.MMXWakeupListener during the polling interval. See {@link #setWakeupInterval(Context, long)}
   */
  public static final String ACTION_WAKEUP = "com.magnet.mmx.MMXClient.WAKEUP";
  /**
   * Intent used to invoke the MMXClient.MMXWakeupListener when a new incoming message is queued on the MMX server
   */
  public static final String ACTION_RETRIEVE_MESSAGES = "com.magnet.mmx.MMXClient.RETRIEVE_MESSAGES";
  /**
   * Intent used to invoke the MMXClient.MMXWakeupListener when a push notification is received
   */
  public static final String ACTION_PUSH_RECEIVED = "com.magnet.mmx.MMXClient.PUSH_RECEIVED";

  /**
   * Extra String field that contains the unique push id from push{@link #ACTION_PUSH_RECEIVED}
   */
  public static final String EXTRA_PUSH_ID = "com.magnet.mmx.MMXClient.EXTRA_PUSH_ID";
  /**
   * Extra String field that contains the title text from push{@link #ACTION_PUSH_RECEIVED}
   */
  public static final String EXTRA_PUSH_TITLE = "com.magnet.mmx.MMXClient.EXTRA_PUSH_TITLE";
  /**
   * Extra String field that contains the body text from push{@link #ACTION_PUSH_RECEIVED}
   */
  public static final String EXTRA_PUSH_BODY = "com.magnet.mmx.MMXClient.EXTRA_PUSH_BODY";
  /**
   * Extra String field that contains the sound name from push{@link #ACTION_PUSH_RECEIVED}
   */
  public static final String EXTRA_PUSH_SOUND = "com.magnet.mmx.MMXClient.EXTRA_PUSH_SOUND";
  /**
   * Extra String field that contains the icon name from push{@link #ACTION_PUSH_RECEIVED}
   */
  public static final String EXTRA_PUSH_ICON = "com.magnet.mmx.MMXClient.EXTRA_PUSH_ICON";
  /**
   * Extra String field that contains the custom json block from push{@link #ACTION_PUSH_RECEIVED}
   */
  public static final String EXTRA_PUSH_CUSTOM_JSON = "com.magnet.mmx.MMXClient.EXTRA_PUSH_CUSTOM";

  final String mName;
  private final Context mContext;
  private MMXClientConfig mConfig;
  private SharedPreferences mSharedPreferences = null;
  private MMXConnection mConnection = null;
  private MMXContext mMMXContext = null;
  private MMXSettings mSettings = null;
  private ConnectionOptions mConnectionOptions = null;
  private MMXListener mMMXListener = null;
  private HandlerThread mMessagingThread = null;
  private final Handler mMessagingHandler;
  private ConnectionInfo mConnectionInfo = null;
  private PersistentQueue mQueue = null;

  //managers
  private HashMap<Class, MMXManager> mManagers = new HashMap<Class, MMXManager>();

  private byte[] mEncryptionKey = new byte[256/8];
  private String mEncryptionString = "zpdi3901!)9" + "39v91a{F{#" + ">@['d.JBBs?";
  DefaultEncryptor mEncryptor = null;

  //private MMXClientConfig mConfig = null;

  private MMXMessageListener mMessageListener = new MMXMessageListener() {
    public void onMessageReceived(final MMXMessage message, String receiptId) {
      if (Log.isLoggable(TAG, Log.DEBUG)) {
        Log.d(TAG, "onMessageReceived() start");
      }
      notifyMessageReceived(message, receiptId);
    }

    public void onMessageSent(String msgId) {
      if (Log.isLoggable(TAG, Log.DEBUG)) {
        Log.d(TAG, "onMessageSent() start; msgID="+msgId);
      }
    }
    
    public void onMessageSubmitted(String msgId) {
      if (Log.isLoggable(TAG, Log.DEBUG)) {
        Log.d(TAG, "onMessageSubmitted() start: msgID="+msgId);
      }
      notifyMessageSubmitted(msgId);
    }
    
    public void onMessageAccepted(MMXid receiver, String msgId) {
      if (Log.isLoggable(TAG, Log.DEBUG)) {
        Log.d(TAG, "onMessageAccepted() start: msgID="+msgId);
      }
      notifyMessageAccepted(msgId);
    }
    
    public void onMessageFailed(String msgId) {
      if (Log.isLoggable(TAG, Log.DEBUG)) {
        Log.d(TAG, "onMessageFailed() start; msgID="+msgId);
      }
      notifySendFailed(msgId, null);
    }

    public void onMessageDelivered(MMXid recipient, String msgId) {
      if (Log.isLoggable(TAG, Log.DEBUG)) {
        Log.d(TAG, "onMessageDelivered() start");
      }
      notifyMessageDelivered(recipient, msgId);
    }

    public void onInvitationReceived(Invitation invitation) {
      // TODO Auto-generated method stub

    }

    public void onAuthReceived(AuthData auth) {
      // TODO Auto-generated method stub

    }

    public void onItemReceived(MMXMessage message, MMXTopic topic) {
      if (Log.isLoggable(TAG, Log.DEBUG)) {
        Log.d(TAG, "onItemReceived() topic="+topic.getName() + ";message=" + message.getPayload().getDataAsText());
      }
      notifyPubsubItemReceived(topic, message);
    }
    
    public void onErrorMessageReceived(MMXErrorMessage message) {
      if (Log.isLoggable(TAG, Log.DEBUG)) {
        Log.d(TAG, "onErrorMessageReceived() msg="+message);
      }
      notifyErrorReceived(message);

    }
  };

  private MMXClient(String name, Context context,
                    final MMXClientConfig config) {
    mName = name;
    mContext = context.getApplicationContext();
    mMMXContext = new MMXContextImpl(context);
    mSharedPreferences = getSharedPrefs(context, name);
    mMessagingThread = new HandlerThread("MMXHandlerThread-" + name);
    mMessagingThread.start();
    mMessagingHandler = new Handler(mMessagingThread.getLooper());

    applyConfig(config);
    mConnection = new MMXConnection(mMMXContext, getQueue(), mSettings);
    mConnection.setMessageListener(mMessageListener);

    try {
      MessageDigest digester = MessageDigest.getInstance("SHA-256");
      String encryptionString = mEncryptionString +
              context.getPackageName() + mMMXContext.getDeviceId();
      digester.update(encryptionString.getBytes());
      mEncryptionKey = digester.digest();
      mEncryptor = new DefaultEncryptor(mEncryptionKey);
    } catch (Exception e) {
      Log.e(TAG, "MMXClient(): Unable to initialize encryptor.", e);
    }
  }

  /**
   * The context associated with this client
   * @return the context
   */
  Context getContext() {
    return mContext;
  }

  MMXConnection getMMXConnection() { return mConnection; }

  SSLContext getSSLContextOverride() {
    ConnectionInfo connectionInfo = getConnectionInfo();
    MMXClientConfig config = connectionInfo.clientConfig;
    SecurityLevel securityLevel = config.getSecurityLevel();
    SSLContext sslContext = null;
    if (securityLevel == SecurityLevel.RELAXED) {
      //if security is "RELAXED"
      //sslContext is used when the socket is being upgraded during starttls
      sslContext = getNaiveSSLContext();
    }
    return sslContext;
  }

  HostnameVerifier getHostnameVerifierOverride() {
    ConnectionInfo connectionInfo = getConnectionInfo();
    MMXClientConfig config = connectionInfo.clientConfig;
    SecurityLevel securityLevel = config.getSecurityLevel();
    HostnameVerifier verifier = null;
    if (securityLevel == SecurityLevel.RELAXED) {
      //if security is "RELAXED"
      //sslContext is used when the socket is being upgraded during starttls
      verifier = getNaiveHostnameVerifier();
    }
    return verifier;
  }

  SocketFactory getSocketFactoryOverride() {
    return new MMXSocketFactoryWrapper(SocketFactory.getDefault());
  }

  /**
   * Set new configuration for this MMXClient instance.  Values will not take effect until the
   * client reconnects.
   *
   * @param newConfig A MMXClientConfig containing the new configuration values
   */
  public synchronized void applyConfig(MMXClientConfig newConfig) {
    if (newConfig.getHost() == null || newConfig.getPort() == -1) {
      throw new IllegalArgumentException("The supplied MMXClientConfig does not specify the " +
              "host or port.  host=" + newConfig.getHost() + ", port=" + newConfig.getPort());
    }
    SecurityLevel sec = newConfig.getSecurityLevel();
    if (sec != SecurityLevel.STRICT) {
      Log.w(TAG, "WARNING!!!  The security level is NOT set to STRICT.  Security level = " + sec);
      if (sec == SecurityLevel.RELAXED) {
        Log.w(TAG, "WARNING!!!  Even though this connection will be over SSL, this security level bypasses " +
                "certain security checks related to hostname verification and certificate verification.  " +
                "This is a potential security risk and should only be used for non-production environments.");
      } else {
        Log.w(TAG, "WARNING!!!  This security level does not enforce any TLS.  This is a potential security risk " +
                "and should only be used for non-production environments.");
      }
    }
    mSettings = buildConnectionSettings(newConfig);
    mConfig = newConfig;

    // Use a new accessor for the device ID.  It is not optimal because we don't
    // know if the device ID is changed.
    DeviceIdGenerator.setDeviceIdAccessor(new DeviceIdAccessor() {
      public String getId(Context context) {
        return mConfig.getDeviceId();
      }
    });
  }

  /**
   * Retrieve the instance of MMXClient with the specified name.
   * If the name doesn't exist, a new client will be created.
   *
   * @param name The name of this instance
   * @param context The context to use for this instance
   * @param config The configuration to use for this instance
   * @return The MMXClient instance
   */
  public static MMXClient getInstance(String name, Context context, MMXClientConfig config) {
    synchronized (sInstanceMap) {
      MMXClient instance = sInstanceMap.get(name);
      if (instance == null) {
        instance = new MMXClient(name, context, config);
        instance.initClient();
        sInstanceMap.put(name, instance);
      } else {
        instance.applyConfig(config);
      }
      return instance;
    }
  }

  /**
   * Retrieve the instance of MMXClient with the default name.  This method
   * will use the specified resId to configure this instance.
   *
   * @param context The context to use for this instance
   * @param configResId The resource id for the configuration properties file (R.raw.xxxxxx)
   * @return The MMXClient instance
   */
  public static MMXClient getInstance(Context context, int configResId) {
    return getInstance(DEFAULT_MMX_NAME, context, configResId);
  }

  /**
   * Retrieve the instance of MMXClient with the specified name.
   * If the name doesn't exist, a new client will be created.  This method
   * will use the specified resId to configure this instance.
   *
   * @param name The name of this instance
   * @param context The context to use for this instance
   * @param configResId The resource id for the configuration properties file (R.raw.xxxxxx)
   * @return The MMXClient instance
   */
  public static MMXClient getInstance(String name, Context context, int configResId) {
    FileBasedClientConfig config = new FileBasedClientConfig(context, configResId);
    return getInstance(name, context, config);
  }

  /**
   * Retrieves the default MMXClient instance.  This is a convenience
   * method for apps that are only working with a single MMXClient.
   *
   * @param context The context to use for this instance
   * @return The MMXClient instance
   */
  public static MMXClient getInstance(Context context, MMXClientConfig config) {
    return getInstance(DEFAULT_MMX_NAME, context, config);
  }

  private static SharedPreferences getSharedPrefs(Context context, String name) {
    return context.getSharedPreferences(SHARED_PREF_NAME + name, Context.MODE_PRIVATE);
  }
  
  /**
   * Get the MMX ID of the current authenticated end-point.
   * @return The MMX ID that represents the current authenticated end-point.
   * @throws MMXException Not connecting to MMX server
   */
  public MMXid getClientId() throws MMXException {
    if (!mConnection.isConnected()) {
      throw new MMXException("Not connecting to MMX server");
    }
    return mConnection.getXID();
  }

  /**
   * Retrieves the config for this MMXClient instance.
   * @return the config associated with this MMXClient instance
   */
  MMXClientConfig getConfig() {
    return mConfig;
  }
  
  /**
   * Inform the MMX server to suspend delivering messages to this client.
   * @throws MMXException Not connecting to MMX server.
   */
  public void suspendDelivery() throws MMXException {
    if (mConnection == null) {
      throw new MMXException("Not connecting to MMX server");
    }
    mConnection.setPriority(MMXConnection.NOT_AVAILABLE);
  }
  
  /**
   * Inform the MMX server to resume delivering messages to this client.
   * @throws MMXException Not connecting to MMX server.
   */
  public void resumeDelivery() throws MMXException {
    if (mConnection == null) {
      throw new MMXException("Not connecting to MMX server");
    }
    mConnection.setPriority(0);
  }

  /**
   * Helper method to validate username.  This should happen on the server, but
   * validating on the client will prevent an unnecessary network call.
   *
   * @param username the supplied username to validate
   * @return true if valid, false otherwise
   */
  boolean isValidUsername(String username) {
    if (username == null ||
            username.length() < USERNAME_LENGTH_MIN ||
            username.length() > USERNAME_LENGTH_MAX) {
      Log.e(TAG, "isValidUsername(): Username cannot be null and must be in the range " +
                  USERNAME_LENGTH_MIN + "-" + USERNAME_LENGTH_MAX + " inclusive.");
      return false;
    }
    for (char c : USERNAME_INVALID_CHARS) {
      if (username.indexOf(c) != -1) {
        Log.e(TAG, "isValidUsername(): Username cannot contain the characters: " + USERNAME_INVALID_CHARS_STR);
        return false;
      }
    }
    return true;
  }

  private synchronized void connectHelper(final MMXListener listener, final ConnectionOptions options) {
    if (listener == null) {
      //require a listener
      throw new IllegalArgumentException("Listener cannot be null.");
    }
    //register the listener
    mMMXListener = listener;
    mConnectionOptions = options;

    String username = options.getUsername();
    int authMode = (options.isAnonymous() ? MMXConnection.AUTH_ANONYMOUS : 0) |
            (options.isAutoCreate() ? MMXConnection.AUTH_AUTO_CREATE : 0);

    if (!options.isAnonymous() && !isValidUsername(username)) {
      notifyConnectionEvent(ConnectionEvent.CONNECTION_FAILED);
      return;
    }

    //store the info in preferences for now
    SharedPreferences.Editor prefEditor = mSharedPreferences.edit();
    prefEditor.putString(SHARED_PREF_KEY_GCM_REGID, null); //reset the gcm key when doing a new connect.
    prefEditor.putBoolean(SHARED_PREF_KEY_GCM_WAKEUP_ENABLED, true);
    prefEditor.putInt(SHARED_PREF_KEY_AUTH_MODE, authMode);
    prefEditor.apply();
    mConnectionInfo = null; //re-read the connection info

    //if this method is called, it will force a disconnect and reconnect
    disconnect();
    doConnect();
  }

  /**
   * Connect to MMX with the named user and specified password.
   *
   * @param username username
   * @param password password
   * @param listener the listener to use for this connection
   * @param options the connection options to use
   */
  public synchronized void connectWithCredentials(final String username, final byte[] password,
                                                  final MMXListener listener, final ConnectionOptions options) {
    ConnectionOptions cOptions;
    if (options == null) {
      cOptions = new ConnectionOptions();
    } else {
      cOptions = options.clone();
    }
    cOptions.setUsername(username)
              .setPassword(new String(password));
    connectHelper(listener, cOptions);
  }

  /**
   * Connect to MMX without the need to create a username/password.  Subsequent calls using this method
   * will use the same "anonymous" user/password until the data is cleared for this application.
   *
   * @param listener The listener to use for this connection
   * @param options the connection options to use (NOTE: the auto-create option is ignored)
   */
  public synchronized void connectAnonymous(final MMXListener listener, final ConnectionOptions options) {
    // anonymous username and password will be generated.
    ConnectionOptions cOptions;
    if (options == null) {
      cOptions = new ConnectionOptions();
    } else {
      cOptions = options.clone();
    }
    cOptions.setAnonymous(true).setAutoCreate(true);
    connectHelper(listener, cOptions);
  }

  /**
   * If connected as a named user, a complete disconnect will be performed (forgetting user credentials)
   * and the client will subsequently connect anonymously.  If this client is already connected anonymously,
   * this method will do nothing.  If not connected at all, the client will connect anonymously (existing
   * user credentials will be forgotten).
   *
   * Note: if this client was NEVER connected before (no valid connection information), this method will
   * throw an IllegalStateException.
   */
  public void goAnonymous() {
    ConnectionInfo info = getConnectionInfo();
    if (mMMXListener == null) {
      throw new IllegalArgumentException("Cannot call goAnonymous() without first calling connect()");
    }
    if (isConnected()) {
      if ((info.authMode & MMXConnection.AUTH_ANONYMOUS) != 0) {
        //if already anonymous, nothing to do
        return;
      } else {
        //connected but not anonymous, disconnect completely first.
        disconnect(true);
        //There's a slight chance of a race condition here if the other thread executes
        //the disconnect so fast that mIsDisconnecting is notified before the wait.
        synchronized (mIsDisconnecting) {
          try {
            mIsDisconnecting.wait();
          } catch (InterruptedException e) {
            Log.e(TAG, "goAnonymous(): caught exception while waiting for disconnect.");
          }
        }
      }
    }
    //finally connect anonymously
    connectAnonymous(mMMXListener, null);
  }

  /**
   * Retrieves the current connection info.
   * @return the connection info for this client instance
   */
  public synchronized ConnectionInfo getConnectionInfo() {
    if (mConnectionInfo == null) {
      String gcmRegId = mSharedPreferences.getString(SHARED_PREF_KEY_GCM_REGID, null);
      int gcmRegIdAppVersion = mSharedPreferences.getInt(SHARED_PREF_KEY_GCM_REGID_APPVERSION, -1);
      boolean isWakeupEnabled = mSharedPreferences.getBoolean(SHARED_PREF_KEY_GCM_WAKEUP_ENABLED, true);
      int authMode = mSharedPreferences.getInt(SHARED_PREF_KEY_AUTH_MODE, 0);

      //build the client config
      mConnectionInfo = new ConnectionInfo(mConfig,
              mConnectionOptions != null ? mConnectionOptions.getUsername() : null,
              mConnectionOptions != null ? mConnectionOptions.getPassword() : null,
              gcmRegId, gcmRegIdAppVersion, isWakeupEnabled, authMode);
    }
    return mConnectionInfo;
  }

  private final Runnable mConnectionRunnable = new Runnable() {
    public void run() {
      if (mIsDisconnecting.get()) {
        synchronized (mIsDisconnecting) {
          try {
            mIsDisconnecting.wait();
          } catch (InterruptedException e) {
            Log.w(TAG, "ConnectionRunnable: interrupted while waiting for disconnecting.  returning");
            return;
          }
        }
      }
      if (Log.isLoggable(TAG, Log.DEBUG)) {
        Log.d(TAG, "ConnectionRunnable: begin");
      }
      if (mConnection != null && mConnection.isConnected()) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
          Log.d(TAG, "ConnectionRunnable: already connected, returning.");
        }
        return;
      }
      ConnectionInfo connectionInfo = getConnectionInfo();
      MMXClientConfig config = connectionInfo.clientConfig;
      if (Log.isLoggable(TAG, Log.DEBUG)) {
        Log.d(TAG, "ConnectionRunnable: attempting connection to: " +
                config.getHost() + " on port " +
                config.getPort());
      }
      HostnameVerifier verifier = getHostnameVerifierOverride();
      SSLContext sslContext = getSSLContextOverride();
      try {
        mConnection.connect(new MMXConnectionListener(), verifier,
                getSocketFactoryOverride(), sslContext);

        if ((connectionInfo.authMode & MMXConnection.AUTH_ANONYMOUS) != 0) {
          mConnection.loginAnonymously(mConnectionOptions.isSuspendDelivery());
        } else {
          if (connectionInfo.username == null) {
            //anonymous has a generated username/password
            throw new IllegalArgumentException("Unable to login with null username");
          } else {
            String username = connectionInfo.username;
            String deviceId = mMMXContext.getDeviceId();
            if (Log.isLoggable(TAG, Log.DEBUG)) {
              Log.d(TAG, "ConnectionRunnable: Attempting login with " + username
                      + ", resource=" + deviceId + ", authMode=" + connectionInfo.authMode);
            }

            // Explicitly enable the message flow.
            int noDelivery = mConnectionOptions.isSuspendDelivery() ?
                MMXConnection.NO_DELIVERY_ON_LOGIN : 0;
            mConnection.authenticate(username, connectionInfo.password, deviceId,
                    connectionInfo.authMode|noDelivery);
          }
        }
      } catch (Exception e) {
        Log.e(TAG, "ConnectionRunnable:  Connection failed.  Exception caught.", e);
        disconnect();
        notifyConnectionEvent(ConnectionEvent.CONNECTION_FAILED);
      }
    }
  };

  private void doConnect() {
    synchronized (mMessagingHandler) {
      mMessagingHandler.post(mConnectionRunnable);
    }
  }

  private MMXSettings buildConnectionSettings(MMXClientConfig config) {
    MMXSettings settings = new MMXSettingsImpl();
    settings.setString(MMXSettings.PROP_HOST, config.getHost());
    settings.setInt(MMXSettings.PROP_PORT, config.getPort());
    settings.setString(MMXSettings.PROP_APPID, config.getAppId());
    settings.setString(MMXSettings.PROP_APIKEY, config.getApiKey());
    settings.setString(MMXSettings.PROP_SERVERUSER, config.getServerUser());
    settings.setString(MMXSettings.PROP_GUESTSECRET, config.getAnonymousSecret());
    settings.setString(MMXSettings.PROP_SERVICE_NAME, config.getDomainName());
    SecurityLevel securityLevel = config.getSecurityLevel();
    settings.setBoolean(MMXSettings.PROP_ENABLE_TLS, securityLevel != SecurityLevel.NONE);
    return settings;
  }

  final AtomicBoolean mIsDisconnecting = new AtomicBoolean(false);

  /**
   * Disconnect the client completely from MMX.
   * This method will deactivate the device if the flag is specified.
   *
   * @param deactivateDevice true indicates that the device should be deactivated
   */
  public void disconnect(final boolean deactivateDevice) {
    synchronized (mMessagingHandler) {
      if (mIsDisconnecting.get()) {
        return;
      }
      mIsDisconnecting.set(true);
      mMessagingHandler.post(new Runnable() {
        public void run() {
          if (deactivateDevice && isConnected()) {
            if (!isConnected()) {
              Log.e(TAG, "disconnect(): not connected, cannot deactivate the device. connect and try again.");
              return;
            }
            //unregister device
            try {
              if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "disconnect():  deactivating device");
              }
              MMXStatus status = DeviceManager.getInstance(mConnection).unregister(mMMXContext.getDeviceId());
              if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "disconnect(): deactivation completed with status=" + status);
              }
            } catch (MMXException e) {
              Log.e(TAG, "disconnect(): caught exception while deactivating", e);
            }

            //clear settings
            synchronized (MMXClient.this) {
              if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "disconnect():  clearing stored connection information");
              }
              mSharedPreferences.edit()
                      .remove(SHARED_PREF_KEY_GCM_REGID)
                      .remove(SHARED_PREF_KEY_GCM_REGID_APPVERSION)
                      .remove(SHARED_PREF_KEY_AUTH_MODE)
                      .remove(SHARED_PREF_KEY_GCM_WAKEUP_ENABLED)
                      .apply();
              mConnectionInfo = null;
              mConnectionOptions = null;

              //clear the database and filesystem
              if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "disconnect(): removing database and pending send messages");
              }
              getQueue().removeAllItems();
            }
          }

          //finally, disconnect
          if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "disconnect():  disconnecting from server");
          }
          if (!isConnected()) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
              Log.d(TAG, "disconnect():  not connected, so just notifying the callback.");
            }
            synchronized (mIsDisconnecting) {
              mIsDisconnecting.set(false);
              mIsDisconnecting.notify();
            }
          } else {
            mConnection.disconnect();
          }
          mConnection.destroy();
          mConnection = new MMXConnection(mMMXContext, getQueue(), mSettings);
          mConnection.setMessageListener(mMessageListener);

          synchronized (MMXClient.this) {
            //notify the internal managers that the connection has changed
            for (MMXManager manager : mManagers.values()) {
              manager.onConnectionChanged();
            }
          }
        }
      });
    }
  }

  synchronized PersistentQueue getQueue() {
    if (mQueue == null) {
      mQueue = new PersistentQueue(this);
    }
    return mQueue;
  }

  /**
   * Disconnects from the server, but the device/user remain
   * registered and a wakeup will connect the user with the arguments
   * that were previously supplied in the connect() method.
   */
  public void disconnect() {
    disconnect(false);
  }

  /**
   * If the MMXClient is currently connected.
   * @return true if the client is currently connected
   */
  public boolean isConnected() {
    return mConnection != null && mConnection.isConnected();
  }

  @SuppressWarnings("unchecked")
  static void handleWakeup(Context context, Intent intent) {
    if (Log.isLoggable(TAG, Log.DEBUG)) {
      Log.d(TAG, "handleWakeup(): starting.");
    }
    synchronized (MMXClient.class) {
      SharedPreferences prefs = context.getSharedPreferences(STATIC_SHARED_PREF_NAME, Context.MODE_PRIVATE);
      String wakeupListenerClassname = prefs.getString(STATIC_SHARED_PREF_KEY_WAKEUP_LISTENER_CLASS, null);
      if (wakeupListenerClassname == null) {
        Log.e(TAG, "handleWakeup(): THERE IS NO WAKEUP LISTENER SPECIFIED.  " +
        		"The application should specify this by calling MMXClient.registerWakeupListener().");
      } else {
        if (Log.isLoggable(TAG, Log.VERBOSE)) {
          Log.v(TAG, "handleWakeup():  Looking up wakeup listener: " + wakeupListenerClassname);
        }
        try {
          Class<? extends MMXWakeupListener> clazz = (Class<? extends MMXWakeupListener>) Class.forName(wakeupListenerClassname);
          MMXWakeupListener wakeupListener = clazz.newInstance();
          wakeupListener.onWakeupReceived(context.getApplicationContext(), intent);
        } catch (Exception e) {
          Log.e(TAG, "handleWakeup(): Exception caught while calling the wakeup listener: " + wakeupListenerClassname);
        }
      }
      //TODO:  perhaps only do this if the listener was called successfully.
      scheduleWakeupAlarm(context, getWakeupInterval(context));
    }
  }

  private static void scheduleWakeupAlarm(Context context, long interval) {
    if (Log.isLoggable(TAG, Log.DEBUG)) {
      Log.d(TAG, "scheduleWakeupAlarm(): called with interval=" + interval);
    }
    AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
    if (interval > 0) {
      //a time was set
      alarmManager.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + interval, getWakeupIntent(context));
    } else {
      //cancel
      if (Log.isLoggable(TAG, Log.DEBUG)) {
        Log.d(TAG, "scheduleWakeupAlarm(): cancelling alarm");
      }
      alarmManager.cancel(getWakeupIntent(context));
    }
  }

  static PendingIntent getWakeupIntent(Context context) {
    Intent intent = new Intent(context, MMXWakeupIntentService.class);
    intent.setAction(ACTION_WAKEUP);
    return PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
  }

  static long getWakeupInterval(Context context) {
    SharedPreferences prefs = context.getSharedPreferences(STATIC_SHARED_PREF_NAME, Context.MODE_PRIVATE);
    return prefs.getLong(STATIC_SHARED_PREF_KEY_WAKEUP_INTERVAL, 0l);
  }

  /**
   * This will set the polling interval for messages in milliseconds and then
   * schedule the next alarm.  The alarm will be a WAKEUP alarm and cause the device
   * to stay awake, so be cautious when setting to small intervals.
   *
   * The Application.onCreate should specify a wakeup listener class MMXContext.registerWakeupListener()
   *
   * @param context The Android context
   * @param interval The amount of time to wait between wakeup alarms.  0 will cancel the wakeup
   */
  public static void setWakeupInterval(Context context, long interval) {
    SharedPreferences prefs = context.getSharedPreferences(STATIC_SHARED_PREF_NAME, Context.MODE_PRIVATE);
    prefs.edit().putLong(STATIC_SHARED_PREF_KEY_WAKEUP_INTERVAL, interval).apply();
    scheduleWakeupAlarm(context, interval);
  }

  /**
   * Register the wakeup listener for MMX.  There can only be one registered
   * wakeup listener class.
   *
   * @param context The Android context
   * @param wakeupListenerClass The class of that implements the MMXWakeupListener interface
   */
  public static void registerWakeupListener(Context context,
      Class<? extends MMXWakeupListener> wakeupListenerClass) {
    synchronized (MMXClient.class) {
      SharedPreferences prefs = context.getSharedPreferences(STATIC_SHARED_PREF_NAME, Context.MODE_PRIVATE);
      prefs.edit().putString(STATIC_SHARED_PREF_KEY_WAKEUP_LISTENER_CLASS,
          wakeupListenerClass != null ? wakeupListenerClass.getName() : null).apply();
    }
  }


  private void notifyConnectionEvent(final ConnectionEvent event) {
    synchronized (this) {
      mMessagingHandler.post(new Runnable() {
        public void run() {
          try {
            mMMXListener.onConnectionEvent(MMXClient.this, event);
          } catch (Exception ex) {
            Log.e(TAG, "notifyConnectionEvent(): Caught runtime exception during " +
                    "the callback", ex);
          }
        }
      });
    }
  }

  private void notifyErrorReceived(final MMXErrorMessage message) {
    synchronized (this) {
      mMessagingHandler.post(new Runnable() {
        public void run() {
          try {
            mMMXListener.onErrorReceived(MMXClient.this, message);
          } catch (Exception ex) {
            Log.e(TAG, "notifyConnectionEvent(): Caught runtime exception during " +
                "the callback", ex);
          }
        }
      });
    }
  }
  
  private void notifyMessageReceived(final MMXMessage message, final String receiptId) {
    synchronized (this) {
      mMessagingHandler.post(new Runnable() {
        public void run() {
          try {
              mMMXListener.onMessageReceived(MMXClient.this, message, receiptId);
          } catch (Exception ex) {
            Log.e(TAG, "notifyConnectionEvent(): Caught runtime exception during " +
                "the callback", ex);
          }
        }
      });
    }
  }

  private void notifyPubsubItemReceived(final MMXTopic topic, final MMXMessage message) {
    synchronized (this) {
      mMessagingHandler.post(new Runnable() {
        public void run() {
          try {
            mMMXListener.onPubsubItemReceived(MMXClient.this, topic, message);
          } catch (Exception ex) {
            Log.e(TAG, "notifyConnectionEvent(): Caught runtime exception during " +
                    "the callback", ex);
          }
        }
      });
    }
  }

  private void notifyMessageDelivered(final MMXid recipient, final String messageId) {
    synchronized (this) {
      mMessagingHandler.post(new Runnable() {
        public void run() {
          try {
            mMMXListener.onMessageDelivered(MMXClient.this, recipient, messageId);
          } catch (Exception ex) {
            Log.e(TAG, "notifyConnectionEvent(): Caught runtime exception during " +
                "the callback", ex);
          }
        }
      });
    }
  }

  private void notifySendFailed(final String messageId, final MMXException cause) {
    synchronized (this) {
      mMessagingHandler.post(new Runnable() {
        public void run() {
          try {
            mMMXListener.onSendFailed(MMXClient.this, messageId);
          } catch (Exception ex) {
            Log.e(TAG, "notifySendFailed(): Caught runtime exception during " +
                "the callback", ex);
          }
        }
      });
    }
  }

  private void notifyMessageSubmitted(final String messageId) {
    synchronized (this) {
      mMessagingHandler.post(new Runnable() {
        public void run() {
          try {
            mMMXListener.onMessageSubmitted(MMXClient.this, messageId);
          } catch (Exception ex) {
            Log.e(TAG, "notifyMessageAccepted(): Caught runtime exception during " +
                "the callback", ex);
          }
        }
      });
    }
  }
  
  private void notifyMessageAccepted(final String messageId) {
    synchronized (this) {
      mMessagingHandler.post(new Runnable() {
        public void run() {
          try {
            mMMXListener.onMessageAccepted(MMXClient.this, messageId);
          } catch (Exception ex) {
            Log.e(TAG, "notifyMessageAccepted(): Caught runtime exception during " +
                "the callback", ex);
          }
        }
      });
    }
  }
  
  /**
   * Register with the gcm service.  This will retrieve the gcm token if it doesn't already
   * exist.
   */
  private void initClient() {
    if (Log.isLoggable(TAG, Log.DEBUG)) {
      Log.d(TAG, "initClient(): starting.");
    }
    SmackAndroid.init(mContext);
  }

  /**
   * Generates a message id
   *
   * @return a generated message id, null if the connection is unavailable
   */
  public String generateMessageId() {
    if (mConnection != null) {
      return mConnection.genId();
    }
    return null;
  }

  private void registerDeviceWithServer() {
    synchronized (mMessagingHandler) {
      mMessagingHandler.post(new Runnable() {
        public void run() {
          if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "registerDeviceWithServer() start");
          }
          ConnectionInfo connectionInfo = getConnectionInfo();
          GooglePlayServicesWrapper googlePlayServicesWrapper = GooglePlayServicesWrapper.getInstance(mContext);
          int result = googlePlayServicesWrapper.isPlayServicesAvailable();
          if (result == GooglePlayServicesWrapper.PLAY_SERVICES_SERVICE_VERSION_UPDATE_REQUIRED) {
            googlePlayServicesWrapper.showErrorNotification(result);
          }
          String gcmSenderId = connectionInfo.clientConfig.getGcmSenderId();
          boolean isGcmWakeupEnabled = connectionInfo.isGcmWakeupEnabled &&
              result == GooglePlayServicesWrapper.PLAY_SERVICES_AVAILABLE &&
              gcmSenderId != null && !gcmSenderId.trim().isEmpty();
          if (isGcmWakeupEnabled) {
            String gcmRegId = connectionInfo.gcmRegId;
            int gcmRegIdAppVersion = connectionInfo.gcmRegIdAppVersion;
            int appVersion = getAppVersion();
            boolean isNeedNewToken = gcmRegId == null ||
                gcmRegIdAppVersion < 0 ||
                gcmRegIdAppVersion != appVersion;

            if (isNeedNewToken) {
              if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "registerDeviceWithServer() need new gcm token, registering.");
              }
              synchronized (MMXClient.this) {
                try {
                  gcmRegId = googlePlayServicesWrapper.registerGcmNew(gcmSenderId);
                  SharedPreferences.Editor prefEditor = mSharedPreferences.edit();
                  prefEditor.putString(SHARED_PREF_KEY_GCM_REGID, gcmRegId);
                  prefEditor.putInt(SHARED_PREF_KEY_GCM_REGID_APPVERSION, appVersion);
                  prefEditor.apply();
                  mConnectionInfo = null;

                  //reload the connection info since been updated
                  connectionInfo = getConnectionInfo();
                } catch(Exception ex) {
                  Log.e(TAG, "registerDeviceWithServer() GCM registration failed!", ex);
                  notifyConnectionEvent(ConnectionEvent.WAKEUP_REGISTRATION_FAILED);
                }
              }
            }
          } else {
            // Provide better diagnostic why MMX Wakeup is not working.
            int i = 0;
            StringBuilder sb = new StringBuilder("MMX Wakeup is disabled:");
            if (!connectionInfo.isGcmWakeupEnabled) {
              sb.append(i==0?" ":", ").append("GCM Wakeup is explicitly disabled in the config");
              ++i;
            }
            if (result != GooglePlayServicesWrapper.PLAY_SERVICES_AVAILABLE) {
              sb.append(i==0?" ":", ").append(googlePlayServicesWrapper.getErrorString(result));
              ++i;
            }
            if (gcmSenderId == null || gcmSenderId.isEmpty()) {
              sb.append(i==0?" ":", ").append("GCM Sender ID is not specified");
              ++i;
            }
            Log.w(TAG, sb.toString());
          }

          //Register this device with MMX server.
          if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "registerDeviceWithServer() create DevReg.Request()");
          }
          DevReg devReg = new DevReg();
          devReg.setPushType(isGcmWakeupEnabled ? PushType.GCM.toString() : null);
          devReg.setPushToken(isGcmWakeupEnabled ? connectionInfo.gcmRegId : null);
          devReg.setDevId(mMMXContext.getDeviceId());
          devReg.setModelInfo(Build.MANUFACTURER+" "+Build.MODEL);
          // TODO: it will be nice to get the Security Setting's Owner info
          devReg.setDisplayName(devReg.getModelInfo());
          try {
            CarrierEnum carrier = DeviceUtil.getCarrier(mContext);
            devReg.setCarrierInfo((carrier == null) ? null : carrier.toString());
          } catch (SecurityException ex) {
            Log.w(TAG, "registerDeviceWithServer(): Unable to get carrier info: " + ex.getMessage());
          }
          try {
            devReg.setPhoneNumber(DeviceUtil.getLineNumber(mContext));
          } catch (SecurityException ex) {
            Log.w(TAG, "registerDeviceWithServer(): Unable to get phone number info: " + ex.getMessage());
          }
          devReg.setOsType(OSType.ANDROID.toString());
          devReg.setOsVersion(Build.VERSION.RELEASE);
          // Register the client protocol version numbers.
          devReg.setVersionMajor(Constants.MMX_VERSION_MAJOR);
          devReg.setVersionMinor(Constants.MMX_VERSION_MINOR);
          try {
            MMXStatus status = getDeviceManager().register(devReg);
            if (Log.isLoggable(TAG, Log.DEBUG)) {
              Log.d(TAG, "registerDeviceWithServer(): device registration completed with status=" + status);
            }
            notifyConnectionEvent(ConnectionEvent.CONNECTED);
            getQueue().processPendingItems();
          } catch (MMXException e) {
            Log.e(TAG, "registerDeviceWithServer(): caught MMXException code=" + e.getCode(), e);
            if (e.getCode() == 400) {
              //if status is unsuccessful, disconnect
              notifyConnectionEvent(ConnectionEvent.AUTHENTICATION_FAILURE);
              disconnect();
            }
          }
        }
      });
    }
  }
  
  /**
   * Retrieves the app version number from PackageManager.
   * @return the app version from package manager
   */
  private int getAppVersion() {
    try {
      PackageInfo pi = mContext.getPackageManager().getPackageInfo(mContext.getPackageName(), 0);
      return pi.versionCode;
    } catch (NameNotFoundException e) {
      Log.e(TAG, "getAppVersion(): exception caught ", e);
    }
    return -1;
  }

  /**
   * The MMX connection listener that listens for Smack connection events.
   * @author login7
   */
  private class MMXConnectionListener implements com.magnet.mmx.client.common.MMXConnectionListener {
    private final String TAG = MMXConnectionListener.class.getSimpleName();

    public void onAuthenticated(MMXid user) {
      if (Log.isLoggable(TAG, Log.DEBUG)) {
        Log.d(TAG, "onAuthenticated() begin");
      }
      registerDeviceWithServer();
    }

    @Override
    public void onAuthFailed(MMXid user) {
      if (Log.isLoggable(TAG, Log.DEBUG)) {
        Log.d(TAG, "onAuthFailed() begin");
      }
      notifyConnectionEvent(ConnectionEvent.AUTHENTICATION_FAILURE);
      disconnect();
    }

    public void onConnectionEstablished() {
      if (Log.isLoggable(TAG, Log.DEBUG)) {
        Log.d(TAG, "onConnectionEstablished() begin");
      }
      //notifyConnectionEvent(ConnectionEvent.CONNECTED);
    }

    public void onReconnectingIn(int interval) {
      if (Log.isLoggable(TAG, Log.DEBUG)) {
        Log.d(TAG, "onReconnectingIn() begin " + interval);
      }
      if (interval < 0) {
        //this means that there is no connectivity and
        //the reconnection manager is waiting for network events
        //before attempt to reconnect.  Any larger interval
        //implies a server connection failure, which will
        //ultimately result in an onConnectionFailed callback
        //if the situation persists
        notifyConnectionEvent(ConnectionEvent.RECONNECTING);
      }
    }

    public void onConnectionFailed(Exception cause) {
      if (Log.isLoggable(TAG, Log.DEBUG)) {
        Log.d(TAG, "onConnectionFailed() begin");
      }
      notifyConnectionEvent(ConnectionEvent.CONNECTION_FAILED);
    }

    public void onConnectionClosed() {
      if (Log.isLoggable(TAG, Log.DEBUG)) {
        Log.d(TAG, "onConnectionClosed() begin");
      }
      notifyConnectionEvent(ConnectionEvent.DISCONNECTED);
      synchronized (mIsDisconnecting) {
        mIsDisconnecting.set(false);
        mIsDisconnecting.notify();
      }
    }

    @Override
    public void onAccountCreated(MMXid user) {
      if (Log.isLoggable(TAG, Log.DEBUG)) {
        Log.d(TAG, "onAccountCreated() user="+user);
      }
    }

  }

  /**
   * Implement if interested in MMXClient events.  In most cases,
   * this should be implemented and registered with a subsequent call
   * to registerMMXListener.
   */
  public interface MMXListener {
    /**
     * Called when a connection event occurs.
     * @param client The instance of the MMXClient
     * @param event The event that occurred
     */
    void onConnectionEvent(MMXClient client, ConnectionEvent event);

    /**
     * Called when a message is received.
     *
     * @param client The instance of the MMXClient
     * @param message The message that was received
     * @param receiptId A delivery receipt ID or null.
     */
    void onMessageReceived(MMXClient client, MMXMessage message, String receiptId);

    /**
     * Called when a message send fails due to the loss of connection.
     * @param client The instance of the MMXClient
     * @param messageId The id of the message that failed
     */
    void onSendFailed(MMXClient client, String messageId);

    /**
     * Called when a delivery is confirmed for a message that was sent
     * with the ack option.
     *
     * @param client The instance of the MMXClient
     * @param messageId The id of the message for which the receipt was returned
     */
    void onMessageDelivered(MMXClient client, MMXid recipient, String messageId);

    /**
     * Called when a multicast message has been submitted to the server.
     * @param client
     * @param messageId
     */
    void onMessageSubmitted(MMXClient client, String messageId);
    
    /**
     * Called when a message has been accepted for delivery by the MMX server
     *
     * @param client the instance of the MMXClient
     * @param messageId the message id of the message
     */
    void onMessageAccepted(MMXClient client, String messageId);

    /**
     * Called when a pubsub item is received.
     * @param client The instance of the MMXClient
     * @param topic The topic for the pubsub item that was received
     * @param message The message of the pubsub item
     */
    void onPubsubItemReceived(MMXClient client, MMXTopic topic, MMXMessage message);
    
    /**
     * Called when an error message is received.  The payload in the error
     * message can be an MMXError or custom; use {@link MMXError#getType()} to identify the payload, and use
     * {@link MMXError#fromJson(String)} and {@link MMXPayload#getDataAsText()}
     * to construct the MMXError payload.
     * @param client The instance of the MMXClient
     * @param error The error message
     */
    void onErrorReceived(MMXClient client, MMXErrorMessage error);
  }

  /**
   * The current connection properties for this instance of MMXClient.  This contains the latest
   * persisted values that will take effect on the next connect() call.  The caller should not
   * cache this object, but instead call getConnectionInfo() to get the most up-to-date values.
   */
  public final class ConnectionInfo {
    /**
     * The MMXClientConfig that is used for this instance
     */
    public final MMXClientConfig clientConfig;
    /**
     * The username that this MMXClient is connected with
     */
    public final String username;
    private final String password;
    /**
     * The GCM registration id if this device has been registered
     */
    public final String gcmRegId;
    /**
     * The GCM registration app version if this device has been registered
     */
    public final int gcmRegIdAppVersion;
    /**
     * Whether GCM wakeup is currently enabled
     */
    public final boolean isGcmWakeupEnabled;
    /**
     * The auth mode for this MMXClient instance
     */
    public final int authMode;

    private ConnectionInfo(MMXClientConfig config, String username, String password,
        String gcmRegId, int gcmRegIdAppVersion, boolean isGcmWakeupEnabled, int authMode) {
      this.clientConfig = config;
      this.username = username;
      this.password = password;
      this.gcmRegId = gcmRegId;
      this.gcmRegIdAppVersion = gcmRegIdAppVersion;
      this.isGcmWakeupEnabled = isGcmWakeupEnabled;
      this.authMode = authMode;
    }
  }

  /**
   * Returns whether or not the wake-up functionality is enabled
   * for this MMX Client.  Wake-up functionality allows the MMX server
   * to wake up the device to deliver messages to the MMX client application.
   *
   * Wake-up functionality is only supported after the application invokes the
   * connect method successfully.
   *
   * @return true if GCM wakeup is enabled for this client
   */
  public boolean isGcmWakeUpEnabled() {
    return getConnectionInfo().isGcmWakeupEnabled;
  }

  /**
   * Enables/disables the wake-up functionality for this MMX client.  See
   * isWakeUpEnabled() for more details.
   *
   * @param isWakeupEnabled true to enable GCM push messages
   */
  public void setGcmWakeUpEnabled(boolean isWakeupEnabled) {
    synchronized (MMXClient.this) {
      SharedPreferences.Editor prefEditor = mSharedPreferences.edit();
      prefEditor.putBoolean(SHARED_PREF_KEY_GCM_WAKEUP_ENABLED, isWakeupEnabled);
      prefEditor.apply();
      mConnectionInfo = null;
    }
    registerDeviceWithServer();
  }

  /**
   * Implement this interface with a default public constructor.  This implementation is required
   * if the application needs to handle any wake-up events (including GCM push messages or timer-based
   * wake-ups).
   *
   * To use the MMX client wake-up functionality, register the MMXWakeupListener implementation
   * {@link #registerWakeupListener(Context, Class)}.  Setup either a scheduled wake-up
   * {@link #scheduleWakeupAlarm(Context, long)} OR configure GCM for your application
   * {@see https://developers.google.com/cloud-messaging/android/start} and register the
   * GCM project ID in the MMX client configuration {@link MMXClientConfig}.
   */
  public interface MMXWakeupListener {
    /**
     * Called when the application has been woken up either from
     * a GCM or some other MMX wakeup mechanism.
     *
     * @param applicationContext The application's context
     * @param intent The intent that caused this wakeup
     */
    void onWakeupReceived(Context applicationContext, Intent intent);
  }

  public synchronized HostnameVerifier getNaiveHostnameVerifier() {
    if (mNaiveHostnameVerifier == null) {
      mNaiveHostnameVerifier = new AllowAllHostnameVerifier();
    }
    return mNaiveHostnameVerifier;
  }

  /**
   * Doesn't throw exceptions when any SSL cert is handed to it.
   * Allows all certs.
   */
  private static class NaiveTrustManager implements X509TrustManager {
    public void checkClientTrusted(X509Certificate[] arg0, String arg1)
        throws CertificateException {
      Log.d(TAG, "NaiveTrustManager.checkClientTrusted() start");
    }

    public void checkServerTrusted(X509Certificate[] arg0, String arg1)
        throws CertificateException {
      Log.d(TAG, "NaiveTrustManager.checkServerTrusted() start");
    }

    public X509Certificate[] getAcceptedIssuers() {
      Log.d(TAG, "NaiveTrustManager.getAcceptedIssuers() start");
      return null;
    }

  }

  private SSLContext mNaiveSslContext = null;
  private HostnameVerifier mNaiveHostnameVerifier = null;

  public synchronized SSLContext getNaiveSSLContext() {
    if (mNaiveSslContext == null) {
      try {
        TrustManager[] tm = new TrustManager[]{new NaiveTrustManager()};
        mNaiveSslContext = SSLContext.getInstance("TLS");
        mNaiveSslContext.init(null, tm, new SecureRandom());
      } catch (Exception e) {
        Log.e(TAG, "getNaiveSSLContext(): caught exception", e);
      }
    }
    return mNaiveSslContext;
  }

  class MMXSocketFactoryWrapper extends SocketFactory {
    private SocketFactory mBaseFactory;
    public MMXSocketFactoryWrapper(SocketFactory baseFactory) {
      mBaseFactory = baseFactory;
    }

    /**
     * If SSL is required, the caller MUST verify the server's
     * identify when calling this method by using a HostnameVerifier.verify().
     */
    public Socket createSocket(String s, int i) throws IOException, UnknownHostException {
      Socket socket = mBaseFactory.createSocket();
      if (socket instanceof SSLSocket) {
        SSLSocket sslSocket = (SSLSocket) socket;
        sslSocket.setEnabledCipherSuites(sslSocket.getSupportedCipherSuites());
      }
      InetSocketAddress addr = new InetSocketAddress(s, i);
      socket.connect(addr, TCP_CONNECTION_TIMEOUT);
      return socket;
    }

    @Override
    public Socket createSocket(String s, int i, InetAddress inetAddress, int i2) throws IOException, UnknownHostException {
      return mBaseFactory.createSocket(s, i, inetAddress, i2);
    }

    @Override
    public Socket createSocket(InetAddress inetAddress, int i) throws IOException {
      return mBaseFactory.createSocket(inetAddress, i);
    }

    @Override
    public Socket createSocket(InetAddress inetAddress, int i, InetAddress inetAddress2, int i2) throws IOException {
      return mBaseFactory.createSocket(inetAddress, i, inetAddress2, i2);
    }
  }

  /**
   * Retrieve the MMXMessageManager instance for this client.
   *
   * @return the MMXMessageManager instance associated with this client
   */
  public synchronized MMXMessageManager getMessageManager() {
    MMXManager manager = mManagers.get(MMXMessageManager.class);
    if (manager == null) {
      manager = new MMXMessageManager(this, mMessagingHandler);
      mManagers.put(MMXMessageManager.class, manager);
    }
    return (MMXMessageManager)manager;
  }

  /**
   * Retrieve the MMXPubSubManager instance for this client.
   *
   * @return the MMXPubSubManager instance associated with this client
   */
  public synchronized MMXPubSubManager getPubSubManager() {
    MMXManager manager = mManagers.get(MMXPubSubManager.class);
    if (manager == null) {
      manager = new MMXPubSubManager(this, mMessagingHandler);
      mManagers.put(MMXPubSubManager.class, manager);
    }
    return (MMXPubSubManager)manager;
  }

  /**
   * Retrieve the MMXAccountManager instance for this client.
   *
   * @return the MMXAccountManager instance associated with this client
   */
  public synchronized MMXAccountManager getAccountManager() {
    MMXManager manager = mManagers.get(MMXAccountManager.class);
    if (manager == null) {
      manager = new MMXAccountManager(this, mMessagingHandler);
      mManagers.put(MMXAccountManager.class, manager);
    }
    return (MMXAccountManager)manager;
  }

  /**
   * Retrieve the MMXDeviceManager instance for this client.
   *
   * @return the MMXDeviceManager instance associated with this client
   */
  public synchronized MMXDeviceManager getDeviceManager() {
    MMXManager manager = mManagers.get(MMXDeviceManager.class);
    if (manager == null) {
      manager = new MMXDeviceManager(this, mMessagingHandler);
      mManagers.put(MMXDeviceManager.class, manager);
    }
    return (MMXDeviceManager)manager;
  }

  /**
   * Clears the location information for the current user
   *
   * @throws MMXException
   */
  public void clearLocation() throws MMXException {
      MMXGeoLogger.clearGeoLocaction(this);
  }

  /**
   * Attempts to cancel a pending message with the specified id.  This is
   * client-only functionality and will only work for messages that have not
   * been sent.
   *
   * @param messageId The id of the message to cancel.  This is the value returned by sendMessage()
   * @return true if canceled successfully
   */
  boolean cancelMessage(String messageId) {
    if (messageId == null) {
      Log.w(TAG, "cancelMessage(): cannot cancel a null messageId, returning false.");
      return false;
    }
    return getQueue().removeItem(messageId);
  }

  /**
   * Returns the messaging handler for this MMXClient instance.
   *
   * @return the messaging handler associated with this MMXClient
   */
  Handler getHandler() {
    return mMessagingHandler;
  }

  private String bin2hex(byte[] data) {
    return String.format("%0" + (data.length*2) + "X", new BigInteger(1, data));
  }
}
