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

import com.magnet.mmx.client.common.AccountManager;
import com.magnet.mmx.client.common.DeviceManager;
import com.magnet.mmx.client.common.IMMXClient;
import com.magnet.mmx.client.common.Log;
import com.magnet.mmx.client.common.MMXConnection;
import com.magnet.mmx.client.common.MMXConnectionListener;
import com.magnet.mmx.client.common.MMXException;
import com.magnet.mmx.client.common.MMXGeoLogger;
import com.magnet.mmx.client.common.MMXMessageListener;
import com.magnet.mmx.client.common.MessageManager;
import com.magnet.mmx.client.common.PubSubManager;
import com.magnet.mmx.protocol.Constants;
import com.magnet.mmx.protocol.DevReg;
import com.magnet.mmx.protocol.GeoLoc;
import com.magnet.mmx.protocol.MMXStatus;
import com.magnet.mmx.protocol.MMXid;
import com.magnet.mmx.protocol.OSType;

/**
 * This class is the main entry point of the MMX Java client API.  It allows
 * user to connect and authenticate to the MMX server.  All MMX functionalities
 * are provided by various managers.
 */
public class MMXClient implements IMMXClient {
  private final static String TAG = "MMXClient";
  private final MMXContext mContext;
  private final MMXSettings mSettings;
  private final MMXConnection mCon;
  private boolean mDevReg;
  private int mPriority;
  private MMXConnectionListener mConListener;

  private final MMXConnectionListener mConHandler = new MMXConnectionListener() {
    @Override
    public void onConnectionEstablished() {
      if (mConListener != null) {
        mConListener.onConnectionEstablished();
      }
    }

    @Override
    public void onReconnectingIn(int interval) {
      if (mConListener != null) {
        mConListener.onReconnectingIn(interval);
      }
    }

    @Override
    public void onConnectionClosed() {
      if (mConListener != null) {
        mConListener.onConnectionClosed();
      }
    }

    @Override
    public void onConnectionFailed(Exception cause) {
      if (mConListener != null) {
        mConListener.onConnectionFailed(cause);
      }
    }

    @Override
    public void onAuthenticated(MMXid user) {
      if (mConListener != null) {
        mConListener.onAuthenticated(user);
      }
      if (mSettings.getBoolean(MMXSettings.ENABLE_AUTO_REGISTER_DEVICE, false)) {
        registerDeviceWithServer();
      }
    }

    @Override
    public void onAuthFailed(MMXid user) {
      if (mConListener != null) {
        mConListener.onAuthFailed(user);
      }
    }

    @Override
    public void onAccountCreated(MMXid user) {
      if (mConListener != null) {
        mConListener.onAccountCreated(user);
      }
    }
  };

  /**
   * Connection options.
   */
  public static class MMXConnectionOptions {
    private boolean mAutoCreate;
    private boolean mSuspendDelivery;

    /**
     * Check if auto account creation is enabled.
     * @return true if auto account creation is enabled; otherwise, false.
     */
    public boolean isAutoCreate() {
      return mAutoCreate;
    }

    /**
     * Option to create an account if it does not exist.
     * @param autoCreate
     * @return This object.
     */
    public MMXConnectionOptions setAutoCreate(boolean autoCreate) {
      mAutoCreate = autoCreate;
      return this;
    }

    /**
     * Check if suspend delivery is enabled.
     * @return true if suspend delivery is enabled; otherwise, false.
     */
    public boolean isSuspendDelivery() {
      return mSuspendDelivery;
    }

    /**
     * Option to suspend delivering off-line messages when login is success.
     * @param suspendDelivery
     * @return This object.
     */
    public MMXConnectionOptions setSuspendDelivery(boolean suspendDelivery) {
      mSuspendDelivery = suspendDelivery;
      return this;
    }
  }

  /**
   * Constructor with a context which must have a unique resource ID.
   *
   * The minimum settings are
   * MMXSettings.PROP_HOST, MMXSettings.PROP_PORT, MMXSettings.PROP_APIKEY,
   * MMXSettings.PROP_APPID, and MMXSettings.PROP_GUESTSECRET.
   *
   * @param context
   * @param settings the settings for this client
   */
  public MMXClient(MMXContext context, MMXSettings settings) {
    mContext = context;
    mSettings = settings;
    mCon = new MMXConnection(mContext, settings);
    if (mSettings.getString(MMXSettings.PROP_APPID, null) == null) {
      Log.w(TAG, "App ID is not configured; this client cannot be authenticated by the app!");
    }
  }

  /**
   * Get a lower layer connection object for internal testing only.
   * @return
   */
  MMXConnection getConnection() {
    return mCon;
  }

  /**
   * Get the MMX ID of the current authenticated end-point (user and device.)
   * @return The MMX ID that represents the current authenticated end-point.
   * @throws MMXException Not connecting to MMX server
   */
  @Override
  public MMXid getClientId() throws MMXException {
    if (!(mCon.isConnected() && mCon.isAuthenticated())) {
      throw new MMXException("Not connecting to MMX server");
    }
    return mCon.getXID();
  }

  /**
   * Connect and authenticate to the MMX server.  Once the connection is
   * established and authenticated, all off-line messages will be delivered to
   * this client automatically unless
   * {@link MMXConnectionOptions#setSuspendDelivery(boolean)} is set.
   * @param user The user ID.
   * @param passwd The user password.
   * @param conListener A non-null listener for connection and authentication.
   * @param msgListener A non-null Listener for asynchronous messages.
   * @param options Connection options, or null.
   * @throws MMXException Connection or authentication failure.
   * @throws IllegalArgumentException Null listener is specified.
   */
  public void connect(String user, byte[] passwd,
          MMXConnectionListener conListener, MMXMessageListener msgListener,
          MMXConnectionOptions options) throws MMXException {
    connect(user, new String(passwd), conListener, msgListener, options);
  }

  /**
   * Connect anonymously. If the anonymous account does not exist, it
   * will be created automatically.  Once the connection is established
   * successfully, all off-line messages will be delivered to this client.
   * Connection options is not supported yet.
   * @param conListener A non-null listener for connection and authentication.
   * @param msgListener A non-null listener for asynchronous messages.
   * @throws MMXException Connection or authentication failure.
   * @throws IllegalArgumentException Null listener is specified.
   */
  public void connectAnonymously(
          MMXConnectionListener conListener, MMXMessageListener msgListener)
              throws MMXException {
    if (conListener == null) {
      throw new IllegalArgumentException("Connection listener cannot be null");
    }
    if (msgListener == null) {
      throw new IllegalArgumentException("Message listener cannot be null.");
    }

    mConListener = conListener;
    mCon.setMessageListener(msgListener);
    if (!mCon.isConnected()) {
      mCon.connect(mConHandler, null, null, null);
    }
    if (mCon.isAuthenticated() || mCon.isAnonymous()) {
      throw new MMXException("Current session is still alive");
    }
    mCon.loginAnonymously(false);
  }

  private void connect(String user, String passwd,
      MMXConnectionListener conListener, MMXMessageListener msgListener,
      MMXConnectionOptions options) throws MMXException {
    if (conListener == null) {
      throw new IllegalArgumentException("Connection listener cannot be null");
    }
    if (msgListener == null) {
      throw new IllegalArgumentException("Message listener cannot be null.");
    }

    int flags = 0;
    if (options != null) {
      if (options.isAutoCreate()) {
        flags |= MMXConnection.AUTH_AUTO_CREATE;
      }
      if (options.isSuspendDelivery()) {
        flags |= MMXConnection.NO_DELIVERY_ON_LOGIN;
      }
    }

    mConListener = conListener;
    mCon.setMessageListener(msgListener);
    if (!mCon.isConnected()) {
      mCon.connect(mConHandler, null, null, null);
    }

    // If authentication is success, register a device without push registration.
    if (mCon.isAuthenticated() || mCon.isAnonymous()) {
      throw new MMXException("Current session is still alive");
    }
    mCon.authenticate(user, passwd, mContext.getDeviceId(), flags);
  }

  private boolean registerDeviceWithServer() {
    DevReg devReg = new DevReg();
    devReg.setDevId(mContext.getDeviceId());
    devReg.setDisplayName(mCon.getUserId()+"'s device");
    devReg.setOsType(getOSType().toString());
    devReg.setOsVersion(System.getProperty("os.version"));
    // Register the client protocol version numbers.
    devReg.setVersionMajor(Constants.MMX_VERSION_MAJOR);
    devReg.setVersionMinor(Constants.MMX_VERSION_MINOR);
    try {
      MMXStatus status = DeviceManager.getInstance(mCon).register(devReg);
      if (status.getCode() != 201) {
        Log.e(TAG, "Unable to register this client device: "+status.getMessage(), null);
        return mDevReg = false;
      }
      return mDevReg = true;
    } catch (MMXException e) {
      Log.e(TAG, "Unable to register this client device", e);
      return mDevReg = false;
    }
  }

  private OSType getOSType() {
    String osName = System.getProperty("os.name");
    if (osName.contains("Linux") || osName.contains("Solaris")) {
      return OSType.UNIX;
    }
    if (osName.contains("Windows")) {
      return OSType.WINDOWS;
    }
    if (osName.contains("Mac")) {
      return OSType.OSX;
    }
    return OSType.OTHER;
  }

  /**
   * Check if this client is connected to the MMX server.  Connected client
   * does not imply that the client is authenticated.
   * @return
   */
  public boolean isConnected() {
    return mCon != null && mCon.isConnected();
  }

  /**
   * Check if this client is connected and authenticated to the MMX server.
   * @return
   */
  public boolean isAuthenticated() {
    return mCon != null && mCon.isAuthenticated();
  }

  /**
   * Disconnect the current session.
   */
  public void disconnect() throws MMXException {
    disconnect(false);
  }

  /**
   * Disconnect with an option to unregister the current device.
   * @param complete true to unregister the device, false to leave device registered.
   */
  public void disconnect(boolean complete) throws MMXException {
    try {
      if (complete && mDevReg) {
        DeviceManager.getInstance(mCon).unregister(mContext.getDeviceId());
      }
    } finally {
      mCon.disconnect();
    }
  }

  /**
   * Update the geo-location of current user to MMX server.
   * @param location A geo-location
   * @return A log ID.
   * @throws MMXException
   */
  public String updateLocation(GeoLoc location) throws MMXException {
    return MMXGeoLogger.updateGeoLocation(this, location);
  }

  /**
   * Clear all logged geo-location of current user from MMX server.
   * @throws MMXException
   */
  public void clearLocation() throws MMXException {
    MMXGeoLogger.clearGeoLocaction(this);
  }

  /**
   * Inform the MMX server to suspend delivering messages to this client.
   * It saves the current priority and set the client to unavailable.
   * @throws MMXException Not connecting to MMX server.
   */
  public void suspendDelivery() throws MMXException {
    if (!mCon.isConnected()) {
      throw new MMXException("Not connecting to MMX server");
    }
    if (mCon.getPriority() != MMXConnection.NOT_AVAILABLE) {
      mPriority = mCon.setPriority(MMXConnection.NOT_AVAILABLE);
    }
  }

  /**
   * Inform the MMX server to resume delivering messages to this client.
   * It will restore the priority from {@link #suspendDelivery()}.
   * @throws MMXException Not connecting to MMX server.
   */
  public void resumeDelivery() throws MMXException {
    if (!mCon.isConnected()) {
      throw new MMXException("Not connecting to MMX server");
    }
    if (mCon.getPriority() != mPriority) {
      mCon.setPriority(mPriority);
    }
  }

  /**
   * Set the priority of this client for receiving the incoming messages.
   * Messages targeting to a user with priority between -128 and -1 will not be
   * delivered.  However, messages target to an end-point will be delivered
   * if its priority is between -128 and 128.
   * @param priority Value between -128 and 128.
   * @throws MMXException Not connecting to MMX server.
   */
  public void setPriority(int priority) throws MMXException {
    if (priority < -128 || priority > 128) {
      throw new IllegalArgumentException("Invalid priority value: "+priority);
    }
    if (!mCon.isConnected()) {
      throw new MMXException("Not connecting to MMX server");
    }
    mCon.setPriority(mPriority = priority);
  }

  /**
   * Generate a unique message ID.
   * @return
   * @see MessageManager#sendPayload(String, String[], com.magnet.mmx.client.Payload, java.util.Map, com.magnet.mmx.client.common.Options)
   */
  public String genMessageId() {
    return mCon.genId();
  }

  /**
   * Get the Messaging Manager.
   * @return
   */
  @Override
  public MessageManager getMessageManager() throws MMXException {
    return MessageManager.getInstance(mCon);
  }

  /**
   * Get the Pub/Sub Manager.
   * @return
   */
  @Override
  public PubSubManager getPubSubManager() throws MMXException {
    return PubSubManager.getInstance(mCon);
  }

  /**
   * Get the account manager.  The primary functions are to change password and
   * query for users.
   * @return
   */
  @Override
  public AccountManager getAccountManager() throws MMXException {
    return AccountManager.getInstance(mCon);
  }

  /**
   * Get the device manager.  The primary function is to get all registered
   * devices belonging to a user.
   * @return
   */
  @Override
  public DeviceManager getDeviceManager() throws MMXException {
    return DeviceManager.getInstance(mCon);
  }
}
