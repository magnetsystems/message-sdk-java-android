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

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Date;
import java.util.HashMap;
import java.util.Properties;
import java.util.UUID;

import javax.crypto.NoSuchPaddingException;
import javax.net.SocketFactory;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;

import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPConnection.FromMode;
import org.jivesoftware.smack.debugger.ConsoleDebugger;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.Presence.Mode;
import org.jivesoftware.smack.sasl.SASLError;
import org.jivesoftware.smack.sasl.SASLErrorException;

import com.magnet.mmx.client.common.GlobalAddress.User;
import com.magnet.mmx.protocol.Constants.UserCreateMode;
import com.magnet.mmx.protocol.MMXStatus;
import com.magnet.mmx.protocol.MMXid;
import com.magnet.mmx.protocol.StatusCode;
import com.magnet.mmx.protocol.UserCreate;
import com.magnet.mmx.protocol.UserInfo;
import com.magnet.mmx.util.BinCodec;
import com.magnet.mmx.util.DefaultEncryptor;
import com.magnet.mmx.util.MMXQueue;
import com.magnet.mmx.util.QueueExecutor;
import com.magnet.mmx.util.XIDUtil;

/**
 * @hide
 * MMXConnection con = new MMXConnection(context);
 * {@link MMXConnection#setMessageListener(MMXMessageListener)}
 * {@link #connect(MMXSettings, listener)} {@link #authenticate(user, password,
 * deviceId, 0)}
 * {@link MMXConnection#sendMessage(User[], String, Message, Options)}; ...
 * {@link SessionManager#getInstance(MMXConnection)};
 * {@link SessionManager#create(com.magnet.mmx.client.SessionManager.SessionType, String, SessionListener)}
 * {@link Session#sendInvitation(String, User)}; ...
 * {@link SessionManager#join(String, String, Date, SessionListener)}
 * {@link #disconnect()}
 *
 */
public class MMXConnection implements ConnectionListener {
  /**
   * A special priority to mark the connection as unavailable.  All off-line
   * messages will not to be delivered to this client until the priority is
   * set between -128 and 128.
   */
  public final static int NOT_AVAILABLE = -255;
  private final static String TAG = "MMXConnection";
  private final HashMap<String, Object> mManagers = new HashMap<String, Object>();
  private MMXContext mContext;
  private MagnetXMPPConnection mCon;
  private MMXConnectionListener mConListener;
  private MMXSettings mSettings;
  private MMXMessageListener mMsgListener;
  private MMXQueue mQueue;
  private QueueExecutor mExecutor;
  private AnonyAccount mAnonyAcct;
  private String mPubSubServiceName;
  private String mAppId;
  private String mApiKey;
  private MMXid mXID;     // caching the MMX ID (userID/deviceID)
  private String mConToken; // MD5 of host-port-userID
  private String mUUID;
  private int mPriority;
  private long mSeq;

  /**
   * Auto create the account if the account does not exist.
   */
  public final static int AUTH_AUTO_CREATE = 0x1;
  /**
   * The account being created is an anonymous account.
   */
  public final static int AUTH_ANONYMOUS = 0x2;
  /**
   * Disable delivering off-line messages when a successful login.  Reconnection
   * has no knowledge about this behavior.
   */
  public final static int NO_DELIVERY_ON_LOGIN = 0x4;

  static {
    // Register the Message Providers, so it can parse unsolicited messages.
    MMXPayloadMsgHandler.registerMsgProvider();
    MMXSignalMsgHandler.registerMsgProvider();
  }

  /**
   * Constructor with an application context.
   *
   * @param context
   * @see #destroy()
   */
  public MMXConnection(MMXContext context, MMXSettings settings) {
    this(context, null, settings);
  }

  /**
   * Constructor with an application context and a queue if desired (may be null).
   * If the queue parameter is null, offline operations (i.e. PubSubManager.publishToTopic())
   * will NOT be queued when offline.
   *
   * @param context the application context
   * @param queue the queue for this connection
   * @see #destroy()
   */
  public MMXConnection(MMXContext context, MMXQueue queue, MMXSettings settings) {
    mContext = context;
    mQueue = queue;
    mExecutor = new QueueExecutor("CallbackThread", true);
    mExecutor.start();
    mSettings = settings.clone();
    initId();
  }

  /**
   * Retrieves the thread in which all messaging callback will be run.
   * @return The thread for all messaging callback will be run in.
   */
  QueueExecutor getExecutor() {
    return mExecutor;
  }

  /**
   * Retrieves the queue associated with this connection or null if not specified.
   * @return the queue associated with this connection or null of no queue
   */
  MMXQueue getQueue() {
    return mQueue;
  }

  /**
   * Destroy this object and free up the resources. This is the counterpart of
   * the constructor.
   */
  public void destroy() {
    disconnect();

    if (mExecutor != null) {
      mExecutor.quit();
      mExecutor = null;
    }
    mSettings = null;
    mConListener = null;
    mMsgListener = null;

    mContext = null;
  }

  /**
   * Implicit destructor.
   */
  @Override
  protected void finalize() {
    destroy();
  }

  // Get a singleton manager by name, or register a new instance by name.
  public Object getManager(String name, Creator creator) {
    Object mgr = mManagers.get(name);
    if (mgr == null) {
      synchronized (mManagers) {
        if ((mgr = mManagers.get(name)) == null) {
          mgr = creator.newInstance(this);
          mManagers.put(name, mgr);
        }
      }
    }
    return mgr;
  }

  public MMXContext getContext() {
    return mContext;
  }

  public synchronized String genId() {
    return mUUID + '-' + Long.toString((++mSeq), 36);
  }

  // 16-byte UUID in private encoding.
  private synchronized void initId() {
    byte[] dst = new byte[16];
    UUID uuid = UUID.randomUUID();
    BinCodec.longToBytes(uuid.getMostSignificantBits(), dst, 0);
    BinCodec.longToBytes(uuid.getLeastSignificantBits(), dst, 8);
    mUUID = BinCodec.encodeToString(dst, false).substring(0, 22);
    mSeq = 0;
  }

  /**
   * Set an optional user display name.  It is sent along with the From meta
   * header.
   * @param displayName A user display name.
   */
  public void setDisplayName(String displayName) {
    mSettings.setString(MMXSettings.PROP_NAME, displayName);
    if (mXID != null) {
      mXID.setDisplayName(displayName);
    }
  }

  /**
   * Get the current priority of this connection.
   * @return {@link #NOT_AVAILABLE}, or -128 to 128.
   */
  public int getPriority() {
    return mPriority;
  }

  /**
   * Set the priority of this connection.  The priority controls the message
   * flow.  Messages targeting to a user (bared JID) will be delivered according
   * to the highest priority.  If same user with multiple connected devices have
   * the same priority, messages will be delivered to all of them.  If the
   * priority is between -1 and -128 inclusively, messages targeting to the
   * end-point (full JID) will be delivered.  A special priority
   * {@link #NOT_AVAILABLE} will disable the message delivery completely to the
   * end-point that it will appear as off-line.
   * @param priority {@link #NOT_AVAILABLE}, or between -128 and 128
   * @return The prior priority.
   * @throws MMXException
   */
  public int setPriority(int priority) throws MMXException {
    Presence presence;
    if (priority == NOT_AVAILABLE) {
      presence = new Presence(Presence.Type.unavailable);
    } else if (priority >= 0 && priority <= 128) {
      presence = new Presence(Presence.Type.available, "Online", priority,
          Mode.available);
    } else if (priority < 0 && priority >= -128) {
      // Type.unavailable will make the connection invisible.
      presence = new Presence(Presence.Type.available, "Blocking", priority,
          Mode.dnd);
    } else {
      throw new IllegalArgumentException("Priority must be >= -128 and <= 128");
    }
    try {
      mCon.sendPacket(presence);
      int oldPriority = mPriority;
      mPriority = priority;
      return oldPriority;
    } catch (Throwable e) {
      throw new MMXException(e.getMessage(), e);
    }
  }

  /**
   * Establish a connection to the MMX server.
   * @param listener
   * @throws ConnectionException
   * @throws MMXException
   * @see #authenticate(String, String, String, int)
   * @see #disconnect()
   */
  public void connect(MMXConnectionListener listener)
      throws ConnectionException, MMXException {
    connect(listener, null, null, null);
  }

  /**
   * Establish a connection to the MMX server with SSL context.
   * @param listener
   * @param sslContext
   * @throws ConnectionException
   * @throws MMXException
   * @see #authenticate(String, String, String, int)
   * @see #disconnect()
   */
  public void connect(MMXConnectionListener listener,
                      SSLContext sslContext)
          throws ConnectionException, MMXException {
    connect(listener, null, null, sslContext);
  }

  public void connect(MMXConnectionListener listener,
          HostnameVerifier hostnameVerifier,
          SocketFactory socketFactory,
          SSLContext sslContext)
          throws ConnectionException, MMXException {
    if (mCon != null && mCon.isConnected()) {
      throw new MMXException("client is still connected");
    }

    String host = mSettings.getString(MMXSettings.PROP_HOST, "localhost");
    int port = mSettings.getInt(MMXSettings.PROP_PORT, 5222);
    String serviceName = mSettings.getString(MMXSettings.PROP_SERVICE_NAME,
                                             MMXSettings.DEFAULT_SERVICE_NAME);
    ConnectionConfiguration config;
    if (serviceName == null) {
      config = new ConnectionConfiguration(host, port);
    } else {
      config = new ConnectionConfiguration(host, port, serviceName);
    }
    config.setReconnectionAllowed(mSettings.getBoolean(
            MMXSettings.PROP_ENABLE_RECONNECT, true));
    config.setRosterLoadedAtLogin(mSettings.getBoolean(
            MMXSettings.PROP_ENABLE_SYNC, false));
    //TODO:  mSettings should be moved back to connect-time instead of
    //TODO:  instantiation (was needed for android offline pub.)  Once that
    //TODO:  happens, we should do these things through settings instead of as parameters

    // Let the caller to control when to send the presence.  It also allows
    // the reconnection to resume the previous message flow state; it is
    // different from what smack does.
    config.setSendPresence(false);
    config.setCompressionEnabled(mSettings.getBoolean(
            MMXSettings.PROP_ENABLE_COMPRESSION, true));
    config.setSecurityMode(mSettings.getBoolean(MMXSettings.PROP_ENABLE_TLS,
            false) ? ConnectionConfiguration.SecurityMode.required
            : ConnectionConfiguration.SecurityMode.disabled);
    if (hostnameVerifier != null) {
      config.setHostnameVerifier(hostnameVerifier);
    }
    if (socketFactory != null) {
      config.setSocketFactory(socketFactory);
    }
    if (sslContext != null) {
      config.setCustomSSLContext(sslContext);
    }

    switch (Log.getLoggable(null)) {
    case Log.VERBOSE:
      config.setDebuggerEnabled(true);
      ConsoleDebugger.printInterpreted = true;
      break;
    default:
      config.setDebuggerEnabled(false);
      ConsoleDebugger.printInterpreted = false;
      break;
    }

    mConListener = listener;
    destroyManagers();

    mCon = new MagnetXMPPConnection(config);
    mCon.setFromMode(FromMode.USER);
    mCon.addConnectionListener(this);

    // add the packet listeners MMX payload message or error messages.
    MessageManager.getInstance(this).initPacketListener();

    try {
      mCon.connect();
    } catch (SmackException.ConnectionException e) {
      throw new ConnectionException(
          "Unable to connect to " + host + ":" + port, e);
    } catch (Throwable e) {
      throw new MMXException(e.getMessage(), e);
    }
  }

  private void destroyManagers() {
    for (Object mgr : mManagers.values()) {
      if (mgr instanceof Closeable) {
        try {
          ((Closeable) mgr).close();
        } catch (IOException e) {
          // Ignored.
        }
      }
    }
    mManagers.clear();
  }

  /**
   * Manual reconnect after the connection was abruptly lost. If the user has
   * logged out, reconnection will not be allowed.
   *
   * @return true if reconnect successfully, false if already connected.
   * @throws MMXException
   *           Unable to reconnect due to error, or logged out explicitly.
   */
  public boolean reconnect() throws MMXException {
    if (mCon == null) {
      throw new MMXException("Connection has been terminated explicitly");
    }
    if (mCon.isConnected()) {
      return false;
    }
    if (!mCon.wasAuthenticated()) {
      throw new MMXException("User has logged out explicitly");
    }
    try {
      mCon.connect();
      return true;
    } catch (Throwable e) {
      throw new MMXException("Unable to reconnect", e);
    }
  }

  /**
   * Tear down the connection and remove the instances of all managers tied to
   * this connection.
   *
   * @see #connect(MMXSettings, MMXConnectionListener)
   */
  public void disconnect() {
    if (mCon != null) {
      if (mCon.isConnected()) {
        try {
          mCon.disconnect();
          mXID = null;
          mAnonyAcct = null;
          destroyManagers();
        } catch (NotConnectedException e) {
          e.printStackTrace();
        }
      }
      mCon = null;
    }
  }

  private static class AnonyAccount {
    private final static String ANONYMOUS_PREFIX = "_anon-";
    private final static String ANONYMOUS_FILE = "com.magnet.sec-anonymous.bin";
    private final static String PROP_USER_ID = "userId";
    private final static String PROP_PASSWORD = "password";
    private String mUserId;
    private String mPassword;
    private final DefaultEncryptor mEncryptor;

    private AnonyAccount(byte[] passcode) throws InvalidKeyException,
        NoSuchAlgorithmException, NoSuchPaddingException,
        InvalidKeySpecException, UnsupportedEncodingException {
      byte[] fkey = new byte[32];
      byte[] key = "zpdi3901!)939v91a{F{#>@['d.JBBs?".getBytes();
      int minLen = Math.min(fkey.length, key.length);
      System.arraycopy(key, 0, fkey, 0, minLen);
      minLen = Math.min(fkey.length, passcode.length);
      for (int i = 0; i < minLen; i++) {
        fkey[i] ^= passcode[i];
      }
      mEncryptor = new DefaultEncryptor(fkey);
    }

    public boolean load(MMXContext context) throws IOException {
      FileInputStream fis = null;
      InputStream is = null;
      try {
        fis = new FileInputStream(new File(context.getFilePath(ANONYMOUS_FILE)));
        is = mEncryptor.decodeStream(fis);
        Properties props = new Properties();
        props.load(is);
        mUserId = props.getProperty(PROP_USER_ID);
        mPassword = props.getProperty(PROP_PASSWORD);
        return true;
      } catch (IOException e) {
        return false;
      } finally {
        if (is != null) {
          is.close();
        } else if (fis != null) {
          fis.close();
        }
      }
    }

    public void generate(MMXContext context) {
      mUserId = generateAnonymousUser(context);
      mPassword = generateRandomPassword();
    }

    public void save(MMXContext context) throws IOException {
      FileOutputStream fos = null;
      OutputStream os = null;
      try {
        fos = new FileOutputStream(
            new File(context.getFilePath(ANONYMOUS_FILE)));
        os = mEncryptor.encodeStream(fos);
        Properties props = new Properties();
        props.setProperty(PROP_USER_ID, mUserId);
        props.setProperty(PROP_PASSWORD, mPassword);
        props.save(os, "DO NOT MODIFY; IT IS A GENERATED FILE");
      } catch (IOException e) {
        Log.e(
            TAG,
            "Did you get InvalidKeyException: Illegal key size or default parameters?\n"
                + "You may need to install a Java Cryptography Extension Unlimited Strength "
                + "Jurisdiction Policy Files 7 Download from "
                + "http://www.oracle.com/technetwork/java/javase/downloads/jce-7-download-432124.html\n"
                + "Read the README from the zip about installation.");
        throw e;
      } finally {
        if (os != null) {
          os.close();
        } else if (fos != null) {
          fos.close();
        }
      }
    }

    private String generateAnonymousUser(MMXContext context) {
      UUID uuid = UUID.randomUUID();
      String result = Long.toString(Math.abs(uuid.getMostSignificantBits()), 32) +
               Long.toString(Math.abs(uuid.getLeastSignificantBits()), 32);
      return ANONYMOUS_PREFIX + result;
    }

    private String generateRandomPassword() {
      SecureRandom random = new SecureRandom();
      return new BigInteger(130, random).toString(32);
    }
  }

  /**
   * Login anonymously.  The semi-anonymous account is generated based on the
   * device ID and persistent that it can be used in PubSub. Only one login is
   * allowed per process.  As soon as the login is success, all off-line
   * messages will be delivered to this client automatically.
   * @throws MMXException
   */
  public void loginAnonymously() throws MMXException {
    loginAnonymously(false);
  }

  /**
   * Login anonymously with message flow control. The semi-anonymous account is
   * generated based on the device ID and persistent that it can be used in
   * PubSub. Only one login is allowed per process.  The <code>noDelivery</code>
   * will disable all off-line messages to be delivered to this account
   * automatically when the login is success.
   * @param noDelivery true to disable delivering off-line messages on login.
   *
   * @throws MMXException
   */
  public void loginAnonymously(boolean noDelivery) throws MMXException {
    if (mAnonyAcct == null) {
      try {
        AnonyAccount anonyAcct = new AnonyAccount(getAppId().getBytes());
        if (!anonyAcct.load(mContext)) {
          anonyAcct.generate(mContext);
          anonyAcct.save(mContext);
        }
        mAnonyAcct = anonyAcct;
      } catch (Throwable e) {
        throw new MMXException(e.getMessage(), e);
      }
    }
    int flags = AUTH_ANONYMOUS|AUTH_AUTO_CREATE;
    if (noDelivery) {
      flags |= NO_DELIVERY_ON_LOGIN;
    }
    authenticate(mAnonyAcct.mUserId, mAnonyAcct.mPassword,
        mContext.getDeviceId(), flags);
  }

  // Logout from an account, but keep the connection. Since Smack does not
  // support keeping the connection, we fake it by disconnect and connect.
  protected void logout() throws MMXException {
    try {
      mCon.disconnect();
      mAnonyAcct = null;
      mCon.resetAuthFailure();
      mCon.connect();
    } catch (Throwable e) {
      throw new MMXException(e.getMessage(), e);
    }
  }

  /**
   * Authenticate an MMX user. There are multiple login options: create an
   * account if it does not exist, create the account as anonymous, disable
   * delivering off-line messages after the successful login.  If there is an
   * authentication failure, the connection is still retained.
   *
   * @param userId The un-escaped user ID without appId.
   * @param password The user password
   * @param resource The device ID
   * @param flags
   *          Combination of {@link #AUTH_AUTO_CREATE}, {@link #AUTH_ANONYMOUS}
   *          and {@link #NO_DELIVERY_ON_LOGIN}
   * @throws MMXException Unable to create account.
   * @see MMXConnectionListener#onAuthenticated(MMXid)
   * @see MMXConnectionListener#onAuthFailed(MMXid)
   * @see MMXConnectionListener#onAccountCreated(MMXid)
   */
  public void authenticate(String userId, String password, String resource,
      int flags) throws MMXException {
    authenticateRaw(makeUserName(userId), password, resource, flags);
  }

  /**
   * Authenticate an XMPP user.  There are multiple login options: create an
   * account if it does not exist, create the account as anonymous, disable
   * delivering off-line messages after the successful login.  The userID will
   * be passed to XMPP server as-is. If there is an authentication failure, the
   * connection is still retained.
   *
   * @param userName The escaped node of the JID (i.e. userID%appID)
   * @param password The user password.
   * @param resource The resource of the JID
   * @param flags
   *          Combination of {@link #AUTH_AUTO_CREATE}, {@link #AUTH_ANONYMOUS},
   *          and {@link #NO_DELIVERY_ON_LOGIN}
   * @throws MMXException Unable to create the account.
   * @see MMXConnectionListener#onAuthenticated(MMXid)
   * @see MMXConnectionListener#onAuthFailed(MMXid)
   * @see MMXConnectionListener#onAccountCreated(MMXid)
   */
  public void authenticateRaw(String userName, String password, String resource,
                                int flags) throws MMXException {
    if (mCon.isAuthenticated()) {
      return;
    }

    // Use the priority for message flow control.
    mPriority = ((flags & NO_DELIVERY_ON_LOGIN) != 0) ? NOT_AVAILABLE : 0;
    try {
      resetAuthFailure();
      mCon.login(userName, password, resource);
    } catch (SASLErrorException e) {
      if (!e.getSASLFailure().getSASLError().equals(SASLError.not_authorized)) {
        // Some SASL errors.
        throw new MMXException(e.getMessage(), e);
      }
      String userId = XIDUtil.getUserId(userName);
      // Not authorized: invalid password or account does not exist.
      if ((flags & AUTH_AUTO_CREATE) == 0) {
        if (mConListener != null) {
          mConListener.onAuthFailed(new MMXid(userId, null));
        }
        return;
      }

      // Try in-band user creation. If not supported, try custom IQ to create
      // the account. If authentication failed, return.
      // int ret = createAccount(userName, password);
      // if ((ret != 0) && ((ret >= 0) || !customCreateAccount(
      //      userId, password, flags & AUTH_ANONYMOUS))) {
      //   return;
      // }
      UserCreateMode mode = ((flags & AUTH_ANONYMOUS) != 0) ?
          UserCreateMode.GUEST : UserCreateMode.UPGRADE_USER;
      if (!customCreateAccount(userId, password, mode)) {
        return;
      }

      // Callback if the auto account creation is success.
      if (mConListener != null) {
        mConListener.onAccountCreated(new MMXid(userId, null));
      }

      // Account is created, now try to log in again.
      try {
        resetAuthFailure();
        mCon.login(userName, password, resource);
        return;
      } catch (Throwable t) {
        throw new MMXException(t.getMessage(), t);
      }
    } catch (Throwable e) {
      throw new MMXException(e.getMessage(), e);
    }
  }

  /**
   * Use custom IQ to create an account.  The user account is automatically
   * scoped by an application.
   *
   * @param userId The user ID (without %appID.)
   * @param password A password.
   * @param mode Guest user or actual user.
   * @return true for success; false for authentication failure.
   * @throws MMException Unable to create account.
   */
  protected boolean customCreateAccount(String userId, String password,
      UserCreateMode mode) throws MMXException {
    if (!XIDUtil.validateUserId(userId)) {
      throw new MMXException("User ID '" + userId + "' cannot contain "
          + XIDUtil.INVALID_CHARS);
    }
    UserCreate account = new UserCreate();
    account.setPriKey(mSettings.getString(MMXSettings.PROP_GUESTSECRET, null));
    account.setApiKey(getApiKey());
    account.setAppId(getAppId());
    account.setCreateMode(mode);
    account.setUserId(userId);
    account.setPassword(password);
    account.setDisplayName(mSettings.getString(MMXSettings.PROP_NAME, userId));
    account.setEmail(mSettings.getString(MMXSettings.PROP_EMAIL, null));
    try {
      MMXStatus status = AccountManager.getInstance(this).createAccount(account);
      if (status.getCode() == StatusCode.SUCCESS) {
        return true;
      }
      throw new MMXException(status.getMessage(), status.getCode());
    } catch (MMXException e) {
      // userId is taken
      if ((e.getCode() == StatusCode.INTERNAL_ERROR) || (e.getCode() == StatusCode.CONFLICT)) {
        if (mConListener != null) {
          mConListener.onAuthFailed(new MMXid(userId, null));
        }
        return false;
      }
      throw e;
    } catch (Throwable e) {
      throw new MMXException(e.getMessage(), e);
    }
  }

  /**
   * Set or remove a listener for invitations, messages, pubsub and presences.
   *
   * @param listener
   *          A listener or null.
   * @see MMXMessageHandler
   */
  public void setMessageListener(MMXMessageListener listener) {
    mMsgListener = listener;
  }

  /**
   * Get the listener for invitation, messages, pubsub and presences.
   *
   * @return
   */
  public MMXMessageListener getMessageListener() {
    return mMsgListener;
  }

  /**
   * Broadcast a message to a group of users chosen through a filter.
   *
   * @param filter
   * @param msgType
   * @param payload
   * @param options
   * @return
   */
  // public String broadcastMessage(Filter filter, Msg<?> payload,
  // ReliableOptions options) {
  // return null;
  // }

  /**
   * Check if the current connection is using an anonymous account.
   *
   * @return
   */
  public boolean isAnonymous() {
    if (mCon == null) {
      return false;
    }
    return (mAnonyAcct != null) && getUserId().equals(mAnonyAcct.mUserId);
  }

  /**
   * Check if the current connection is authenticated (not using anonymous
   * account.)
   *
   * @return
   */
  public boolean isAuthenticated() {
    if (mCon == null) {
      return false;
    }
    return mCon.isAuthenticated() && !isAnonymous();
  }

  /**
   * Check if the current connection is alive.
   *
   * @return
   */
  public boolean isConnected() {
    if (mCon == null) {
      return false;
    }
    return mCon.isConnected();
  }

  /**
   * Get the MMX ID (userID and deviceID) of the login user.
   * @return
   */
  public MMXid getXID() {
    if (mCon == null) {
      return null;
    }
    if (mXID == null) {
      mXID = XIDUtil.toXid(getUser(),
                           mSettings.getString(MMXSettings.PROP_NAME, null));
    }
    return mXID;
  }

  /**
   * Get the full XID (XEP-0106 compliant) of the login user in String format.
   * @return A format of userID%appID@domain/resource.
   */
  public String getUser() {
    if (mCon == null) {
      return null;
    }
    return mCon.getUser();
  }

  /**
   * @hide
   * Extract the XEP-0106 compliant user ID of the login user.
   * @return
   */
  public String extractUserId() {
    if (mCon == null) {
      return null;
    }
    return XIDUtil.extractUserId(mCon.getUser());
  }

  /**
   * Get the human readable userID (without appID) of the authenticated user.
   * @return
   */
  public String getUserId() {
    MMXid xid = getXID();
    if (xid == null) {
      return null;
    }
    return xid.getUserId();
  }

  /**
   * Make a multi-tenant user name from a user ID.  The user name is the node
   * part of XID.
   *
   * @param userId A user ID without appId.
   * @return An escaped node with "userID%appId".
   */
  public String makeUserName(String userId) {
    return XIDUtil.makeEscNode(userId, getAppId());
  }

  String getAppId() {
    if (mAppId == null) {
      mAppId = mSettings.getString(MMXSettings.PROP_APPID, null);
    }
    return mAppId;
  }

  String getApiKey() {
    if (mApiKey == null) {
      mApiKey = mSettings.getString(MMXSettings.PROP_APIKEY, null);
    }
    return mApiKey;
  }

  String getPubSubService() {
    if (mPubSubServiceName == null && mCon != null) {
      mPubSubServiceName = "pubsub." + mCon.getServiceName();
    }
    return mPubSubServiceName;
  }

  String getDomain() {
    if (mCon == null) {
      return null;
    }
    return mCon.getServiceName();
  }

  MagnetXMPPConnection getXMPPConnection() {
    return mCon;
  }

  // Get a MD5 token to represent the connection. This token is part of a file
  // name to persist some state information.
  String getConnectionToken() {
    if (mConToken == null) {
      String token = mCon.getHost() + '-' + mCon.getPort() + '-'
          + mCon.getUser();
      try {
        MessageDigest md5 = MessageDigest.getInstance("MD5");
        md5.update(token.getBytes());
        byte[] digest = md5.digest();
        mConToken = BinCodec.encodeToString(digest, false).substring(0, 22);
      } catch (Throwable e) {
        Log.w(TAG, "Cannot hash the connection token; use plain text", e);
        mConToken = token;
      }
    }
    return mConToken;
  }

  void resetAuthFailure() {
    mCon.resetAuthFailure();
  }

  MMXSettings getSettings() {
    return mSettings;
  }

  @Override
  public void authenticated(XMPPConnection con) {
    boolean isMMXUser = XIDUtil.getAppId(con.getUser()) != null;
    MMXid user = getXID();

    // Do explicit message flow control for login.
    try {
      setPriority(getPriority());
    } catch (MMXException e) {
      Log.e(TAG, "Unable to send presence with priority", e);
    }

    // Fetch the display name if it is an MMX user.
    if (isMMXUser) {
      try {
        UserInfo self = AccountManager.getInstance(this).getUserInfo();
        if (mXID != null) {
          mXID.setDisplayName(self.getDisplayName());
        }
      } catch (MMXException e) {
        Log.e(TAG, "Unable to retrieve user profile for "+con.getUser());
      }
    }

    if (mConListener != null) {
      mConListener.onAuthenticated(user);
    }

    // If it is not a MMX user, skip asking for missed published items.
    if (!isMMXUser) {
      return;
    }

    // After authenticated, ask MMX to send the very last published item from
    // each subscribed topic since the last receiving time. TODO: some last
    // published items may have been delivered to other devices before; how
    // smart is the PubSub implementation?  Should it be a settings per topic
    // or per subscription in the server?
    int maxItems = mSettings.getInt(MMXSettings.PROP_MAX_LAST_PUB_ITEMS, 1);
    if (maxItems != 0) {
      try {
        Date lastDeliveryTime = PubSubManager.getInstance(this)
            .getLastDelivery();
        MMXStatus status = PubSubManager.getInstance(this)
            .requestLastPublishedItems(maxItems, lastDeliveryTime);
        if (Log.isLoggable(TAG, Log.DEBUG)) {
          Log.d(TAG, "sendLastPublishedItems(): " + status.getMessage() + ", code="
              + status.getCode());
        }
      } catch (MMXException e) {
        Log.e(TAG, "sendLastPublishedItems() failed", e);
      }
    }
  }

  @Override
  public void connected(XMPPConnection con) {
    if (mConListener != null) {
      mConListener.onConnectionEstablished();
    }
  }

  @Override
  public void connectionClosed() {
    if (mConListener != null) {
      mConListener.onConnectionClosed();
    }
  }

  @Override
  public void connectionClosedOnError(Exception cause) {
    if (mConListener != null) {
      mConListener.onConnectionFailed(cause);
    }
  }

  @Override
  public void reconnectingIn(int interval) {
    if (mConListener != null) {
      mConListener.onReconnectingIn(interval);
    }
  }

  @Override
  public void reconnectionFailed(Exception cause) {
    if (mConListener != null) {
      mConListener.onConnectionFailed(cause);
    }
  }

  @Override
  public void reconnectionSuccessful() {
    // After the reconnection, resume to the previous presence state (or message
    // flow control.)  MMX has a different behavior from Smack which reuses
    // the initial connection config for sending <presence> or not.
    if (mPriority != NOT_AVAILABLE) {
      try {
        setPriority(mPriority);
      } catch (MMXException e) {
        Log.e(TAG, "OnReconnection: unable to send presence with priority "+mPriority);
      }
    }

    if (mConListener != null) {
      mConListener.onConnectionEstablished();
    }
  }
}
