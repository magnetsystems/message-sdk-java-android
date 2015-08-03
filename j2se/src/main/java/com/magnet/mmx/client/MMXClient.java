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
import com.magnet.mmx.client.common.MMXid;
import com.magnet.mmx.client.common.MessageManager;
import com.magnet.mmx.client.common.PubSubManager;
import com.magnet.mmx.protocol.Constants;
import com.magnet.mmx.protocol.DevReg;
import com.magnet.mmx.protocol.GeoLoc;
import com.magnet.mmx.protocol.MMXStatus;
import com.magnet.mmx.protocol.OSType;

/**
 * This class is the main entry point of the MMX Java client API.  It allows
 * user to connect and authenticate to the MMX server.  All MMX functionalities
 * are provided by various managers.
 */
public class MMXClient implements IMMXClient {
  private final static String TAG = "MMXClient";
  private MMXContext mContext;
  private MMXSettings mSettings;
  private MMXConnection mCon;
  private boolean mDevReg;
  private MMXConnectionListener mConListener;

  private MMXConnectionListener mConHandler = new MMXConnectionListener() {
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
      registerDeviceWithServer();
    }

    @Override
    public void onAuthFailed(MMXid user) {
      if (mConListener != null) {
        mConListener.onAuthFailed(user);
      }
    }

    @Override
    public void onAccountCreated(MMXid user) {
      if (mConListener != null)
        mConListener.onAccountCreated(user);
    }
  };
  
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
  public MMXid getClientId() throws MMXException {
    if (!(mCon.isConnected() && mCon.isAuthenticated())) {
      throw new MMXException("Not connecting to MMX server");
    }
    return mCon.getXID();
  }
  
  /**
   * Connect and authenticate to the MMX server.
   * @param user The user ID.
   * @param passwd The user password.
   * @param conListener A non-null listener for connection and authentication.
   * @param msgListener A non-null Listener for asynchronous messages.
   * @param autoCreate Create the account if it does not exist.
   * @throws MMXException Connection or authentication failure.
   * @throws IllegalArgumentException Null listener is specified.
   */
  public void connect(String user, byte[] passwd,
          MMXConnectionListener conListener, MMXMessageListener msgListener,
          boolean autoCreate) throws MMXException {
    connect(user, new String(passwd), conListener, msgListener, 
        autoCreate ? MMXConnection.AUTH_AUTO_CREATE : 0);
  }
  
  /**
   * Connect anonymously. 
   * If the anonymous account does not exist, it 
   * will be created automatically.  Only one anonymous connection per client
   * is allowed and MMXSettings.PROP_ENABLE_AGENT_MODE must be false.
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
    if (!mCon.isConnected())
      mCon.connect(mConHandler);
    if (mCon.isAuthenticated() || mCon.isAnonymous()) {
      throw new MMXException("Current session is still alive");
    }
    mCon.loginAnonymously();
  }
  
  private void connect(String user, String passwd,
      MMXConnectionListener conListener,
      MMXMessageListener msgListener, int flags) throws MMXException {
    if (conListener == null) {
      throw new IllegalArgumentException("Connection listener cannot be null");
    }
    if (msgListener == null) {
      throw new IllegalArgumentException("Message listener cannot be null.");
    }
    
    mConListener = conListener;
    mCon.setMessageListener(msgListener);
    if (!mCon.isConnected()) {
      mCon.connect(mConHandler);
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
    if (osName.contains("Linux") || osName.contains("Solaris"))
      return OSType.UNIX;
    if (osName.contains("Windows"))
      return OSType.WINDOWS;
    if (osName.contains("Mac"))
      return OSType.OSX;
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
   * This is same as calling {@link #setPriority(int)} with -1 priority.
   * @throws MMXException Not connecting to MMX server.
   */
  public void suspendDelivery() throws MMXException {
    if (!mCon.isConnected())
      throw new MMXException("Not connecting to MMX server");
    mCon.setMessageFlow(-1);
  }

  /**
   * Inform the MMX server to resume delivering messages to this client.
   * This is same as calling {@link #setPriority(int)} with 0 priority.
   * @throws MMXException Not connecting to MMX server.
   */
  public void resumeDelivery() throws MMXException {
    if (!mCon.isConnected())
      throw new MMXException("Not connecting to MMX server");
    mCon.setMessageFlow(0);
  }

  /**
   * Set the priority of this client.  Messages are sent to the highest priority
   * client first.  Any clients with priority below 0 will not receive any
   * messages.
   * @param priority Value between -128 and 128.
   * @throws MMXException Not connecting to MMX server.
   */
  public void setPriority(int priority) throws MMXException {
    if (!mCon.isConnected())
      throw new MMXException("Not connecting to MMX server");
    mCon.setMessageFlow(priority);
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
  public MessageManager getMessageManager() throws MMXException {
    return MessageManager.getInstance(mCon);
  }
  
  /**
   * Get the Pub/Sub Manager.
   * @return
   */
  public PubSubManager getPubSubManager() throws MMXException {
    return PubSubManager.getInstance(mCon);
  }
  
  /**
   * Get the account manager.  The primary functions are to change password and
   * query for users.
   * @return
   */
  public AccountManager getAccountManager() throws MMXException {
    return AccountManager.getInstance(mCon);
  }

  /**
   * Get the device manager.  The primary function is to get all registered 
   * devices belonging to a user.
   * @return
   */
  public DeviceManager getDeviceManager() throws MMXException {
    return DeviceManager.getInstance(mCon);
  }
  
//  public SessionManager getSessionManager() {
//    return SessionManager.getInstance(mCon);
//  }
}
