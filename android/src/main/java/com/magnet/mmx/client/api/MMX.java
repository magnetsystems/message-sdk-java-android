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

import java.net.URISyntaxException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jivesoftware.smack.packet.XMPPError;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.HandlerThread;

import com.magnet.mmx.BuildConfig;
import com.magnet.mmx.client.AbstractMMXListener;
import com.magnet.mmx.client.FileBasedClientConfig;
import com.magnet.mmx.client.MMXClient;
import com.magnet.mmx.client.MMXClientConfig;
import com.magnet.mmx.client.common.Log;
import com.magnet.mmx.client.common.MMXErrorMessage;
import com.magnet.mmx.client.common.MMXException;
import com.magnet.mmx.client.common.MMXPayload;
import com.magnet.mmx.protocol.Constants;
import com.magnet.mmx.protocol.MMXError;
import com.magnet.mmx.protocol.MMXTopic;
import com.magnet.mmx.protocol.MMXid;

/**
 * The main entry point for Magnet Message.  Application must invoke<ol>
 * <li>{@link #init(Context, int)} to initialize MMX with connection info</li>
 * <li>{@link #registerListener(EventListener)} to register a listener for 
 * invitation, incoming messages, connection and authentication events,</li>
 * <li>{@link #login(String, byte[], OnFinishedListener)} to authenticate the user</li>
 * <li>{@link #start()} to start MMX service</li>
 * </ol>
 * Optionally, the application may invoke<ul>
 * <li>{@link #registerWakeupBroadcast(Intent)} to register a GCM wake up listener</li>
 * </ul>
 * Upon successful completion, the application may proceed with {@link MMXMessage} or
 * {@link MMXChannel}.
 */
public final class MMX {
  /**
   * Possible failure codes returned in the OnFinishedListener.onFailure method.
   * @see com.magnet.mmx.client.api.MMX.OnFinishedListener#onFailure(FailureCode, Throwable)
   */
  public static class FailureCode {
    /**
     * A client error.
     */
    public static final FailureCode DEVICE_ERROR = new FailureCode(10, "DEVICE_ERROR");
    /**
     * The MMX service is not available due to network issue or server issue.
     */
    public static final FailureCode SERVICE_UNAVAILABLE = new FailureCode(11, "SERVICE_UNAVAILABLE");
    /**
     * Concurrent logins are attempted.
     */
    public static final FailureCode DEVICE_CONCURRENT_LOGIN = new FailureCode(12, "DEVICE_CONCURRENT_LOGIN");
    /**
     * Server authentication failure.
     */
    public static final FailureCode SERVER_AUTH_FAILED = new FailureCode(20, "SERVER_AUTH_FAILED");
    /**
     * A bad request submitted to the server.
     */
    public static final FailureCode BAD_REQUEST = new FailureCode(400, "BAD_REQUEST");
    /**
     * A server error.
     */
    public static final FailureCode SERVER_ERROR = new FailureCode(500, "SERVER_ERROR");
    private final int mValue;
    private final String mDescription;
    private String mToString;

    FailureCode(int value, String description) {
      mValue = value;
      mDescription = description;
    }

    FailureCode(FailureCode code) {
      this(code.getValue(), code.getDescription());
    }

    /**
     * The integer code of this failure
     * @return the integer code
     */
    public final int getValue() {
      return mValue;
    }

    /**
     * The description of this failure
     * @return the description
     */
    public final String getDescription() { return mDescription; }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if ((o == null) || !(o instanceof FailureCode)) return false;
      FailureCode that = (FailureCode) o;
      return mValue == that.mValue;
    }

    @Override
    public int hashCode() {
      return mValue;
    }

    public String toString() {
      if (mToString == null) {
        mToString = this.getClass().getSimpleName() +
                    '(' + mValue + ',' + mDescription + ')';
      }
      return mToString;
    }
  }

  /**
   * Possible reasons for EventListener.onLoginRequired
   * @see com.magnet.mmx.client.api.MMX.EventListener#onLoginRequired(LoginReason)
   */
  public enum LoginReason {
    /**
     * @deprecated
     */
    DISCONNECTED,
    /**
     * If current credentials are invalid.  Possible action: prompt user for
     * new credential, account may be disabled.
     */
    CREDENTIALS_EXPIRED,
    /**
     * If the service is unavailable.  Possible action: check the data
     * connectivity.
     */
    SERVICE_UNAVAILABLE,
    /**
     * If service is available and user credential has been resumed.  No actions
     * are needed.
     */
    SERVICE_AVAILABLE,
  }

  /**
   * The listener interface for handling incoming messages and message acknowledgements.
   */
  abstract static public class EventListener {
    /**
     * Invoked when incoming message is received.
     *
     * @param message the incoming message
     * @return true to consume this message, false for additional listeners to be called
     */
    abstract public boolean onMessageReceived(MMXMessage message);

    /**
     * Called when an acknowledgment is received.  The default implementation of this is a
     * no-op.
     *
     * @param from the user who acknowledged the message
     * @param messageId the message id that was acknowledged
     * @return true to consume this message, false for additional listeners to be called
     */
    public boolean onMessageAcknowledgementReceived(MMXUser from, String messageId) {
      //default implementation is a no-op
      return false;
    }

    /**
     * Called when an MMXChannel invitation is received.  The default implementation of this
     * method is a no-op.
     *
     * @param invite the invitation
     * @return true to consume this event, false for additional listeners to be called
     */
    public boolean onInviteReceived(MMXChannel.MMXInvite invite) {
      //default implementation is a no-op
      return false;
    }

    /**
     * Called when a MMXChannel invitation has been responded to.  The default implementation of this
     * method is a no-op.
     *
     * @param inviteResponse the invite response
     * @return true to consume this event, false for additional listeners to be called
     */
    public boolean onInviteResponseReceived(MMXChannel.MMXInviteResponse inviteResponse) {
      //default implementation is a no-op
      return false;
    }

    /**
     * Called when a connection or authentication state is changed. The default
     * implementation of this method is a no-op.  The name of this method does
     * not reflect what it really does; it will be renamed in future release.
     *
     * @param reason the reason why login is required
     * @return true to consume this event, false for additional listeners to be called
     * @see #login(String, byte[], OnFinishedListener)
     */
    public boolean onLoginRequired(LoginReason reason) {
      //default implementation is a no-op
      return false;
    }
    
    /**
     * Called when there is an error while the server processed the message
     * send request.  The default implementation is to log the error.
     * @param messageId the id of a message that caused the error
     * @param code a failure code
     * @param text an optional diagnostic text message
     * @return true to consume this event, false for additional listeners to be called
     */
    public boolean onMessageSendError(String messageId, MMXMessage.FailureCode code, String text) {
      //default implementation is to log it
      Log.e(TAG, "onMessageSendError() message ID="+messageId+", code="+code+", text="+text);
      return false;
    }
  }

  /**
   * The listener interface used by the asynchronous calls.
   *
   * @param <T> The parameter type with which the onSuccess callback will be invoked
   */
  public static abstract class OnFinishedListener<T> implements IOnFinishedListener<T, FailureCode> {
    /**
     * Invoked if the operation succeeded
     *
     * @param result the result of the operation
     */
    public abstract void onSuccess(T result);

    /**
     * Invoked if the operation failed
     * 
     * @param code the failure code
     * @param ex the exception, null if no exception
     */
    public abstract void onFailure(FailureCode code, Throwable ex);
  }

  public static final String EXTRA_NESTED_INTENT = "extraNestedIntent";
  private static final String TAG = MMX.class.getSimpleName();
  private static final String SHARED_PREFS_NAME = MMX.class.getName();
  private static final String PREF_WAKEUP_INTENT_URI = "wakeupIntentUri";
  private final AtomicBoolean mLoggingIn = new AtomicBoolean(false);
  private SharedPreferences mSharedPrefs = null;
  private Context mContext = null;
  private MMXClient mClient = null;
  private MMXUser mCurrentUser = null;
  private HandlerThread mHandlerThread = null;
  private Handler mHandler = null;
  private static MMX sInstance = null;

  /**
   * The listeners will be added in order (most recent at end)
   * They should be called in order from most recently registered (from the end)
   */
  private final LinkedList<EventListener> mListeners = new LinkedList<EventListener>();

  private AbstractMMXListener mGlobalListener = new AbstractMMXListener() {
    @Override
    public void handleMessageReceived(MMXClient mmxClient, com.magnet.mmx.client.common.MMXMessage mmxMessage, String receiptId) {
      MMXPayload payload = mmxMessage.getPayload();
      String type = payload.getType();
      if (MMXChannel.MMXInvite.TYPE.equals(type)) {
        MMXChannel.MMXInvite invite = MMXChannel.MMXInvite.fromMMXMessage(MMXMessage.fromMMXMessage(null, mmxMessage));
        notifyInviteReceived(invite);
      } else if (MMXChannel.MMXInviteResponse.TYPE.equals(type)) {
        MMXChannel.MMXInviteResponse inviteResponse = MMXChannel.MMXInviteResponse.fromMMXMessage(MMXMessage.fromMMXMessage(null, mmxMessage));
        notifyInviteResponseReceived(inviteResponse);
      } else {
        notifyMessageReceived(MMXMessage.fromMMXMessage(null, mmxMessage).receiptId(receiptId));
      }
    }

    @Override
    public void handleMessageDelivered(MMXClient mmxClient, MMXid recipient, String messageId) {

    }

    @Override
    public void handlePubsubItemReceived(MMXClient mmxClient, MMXTopic mmxTopic, com.magnet.mmx.client.common.MMXMessage mmxMessage) {
      notifyMessageReceived(MMXMessage.fromMMXMessage(mmxTopic, mmxMessage));
    }

    @Override
    public void onMessageDelivered(MMXClient mmxClient, MMXid recipient, String messageId) {
      notifyMessageAcknowledged(recipient, messageId);
      super.onMessageDelivered(mmxClient, recipient, messageId);
    }

    @Override
    public void onMessageSubmitted(MMXClient mmxClient, String messageId) {
//      MMXMessage.handleMessageAccepted(messageId);
//      super.onMessageSubmitted(mmxClient, messageId);
    }
    
    @Override
    public void onMessageAccepted(MMXClient mmxClient, String messageId) {
      MMXMessage.handleMessageAccepted(messageId);
      super.onMessageAccepted(mmxClient, messageId);
    }

    @Override
    public void onConnectionEvent(MMXClient mmxClient, MMXClient.ConnectionEvent connectionEvent) {
      switch (connectionEvent) {
        case AUTHENTICATION_FAILURE:
          if (!mLoggingIn.get()) {
            notifyLoginRequired(LoginReason.CREDENTIALS_EXPIRED);
          }
          break;
        case CONNECTED:
          if (!mLoggingIn.get()) {
            try {
              sInstance.mCurrentUser = MMXUser.fromMMXid(mmxClient.getClientId());
              notifyLoginRequired(LoginReason.SERVICE_AVAILABLE);
            } catch (MMXException e) {
              // Ignored; it should not happen.
            }
          }
          break;
        case CONNECTION_FAILED:
          if (!mLoggingIn.get()) {
            sInstance.mCurrentUser = null;
            notifyLoginRequired(LoginReason.SERVICE_UNAVAILABLE);
          }
          break;
      }
      super.onConnectionEvent(mmxClient, connectionEvent);
    }
    
    // There are two possible implementations how a send error can be handled.
    // One way is to handle some errors in onFailure() and some errors in the
    // onMessageSendError(), the other way is to handle all errors in the
    // onMessageSendError().  Current implementation is the hybrid mode because
    // a lot of developers think onFailure() is for errors which is not supposed
    // to be.  In the next check in, the send error will be handled by
    // onMesasgeSendError(); it will break the backward compatibility.
    @Override
    public void onErrorReceived(MMXClient mmxClient, MMXErrorMessage error) {
      XMPPError xmppErr;
      MMXError mmxErr;
      MMXMessage.FailureCode fcode;
      if ((mmxErr = error.getMMXError()) != null) {
        String text;
        if (mmxErr.getCode() == MMXMessage.FailureCode.INVALID_RECIPIENT.getValue()) {
          fcode = MMXMessage.FailureCode.INVALID_RECIPIENT;
          text = (mmxErr.getParams() == null) ? null : (mmxErr.getParams())[0];
//          notifyMessageSendError(mmxErr.getMsgId(), fcode, text);
          MMXMessage.handleMessageSendError(text == null ? null : new MMXid(text, null),
              mmxErr.getMsgId(), fcode, null);
        } else if (mmxErr.getCode() == MMXMessage.FailureCode.CONTENT_TOO_LARGE.getValue()) {
          fcode = MMXMessage.FailureCode.CONTENT_TOO_LARGE;
          text = mmxErr.getMessage();
//          notifyMessageSendError(mmxErr.getMsgId(), fcode, text);
          MMXMessage.handleMessageSendError(null, mmxErr.getMsgId(), fcode,
              new MMXException(text, mmxErr.getCode()));
        } else {
          fcode = MMXMessage.FailureCode.fromMMXFailureCode(FailureCode.SERVER_ERROR, null);
          text = mmxErr.getMessage();
//          notifyMessageSendError(mmxErr.getMsgId(), fcode, text);
          MMXMessage.handleMessageSendError(null, mmxErr.getMsgId(), fcode,
              new MMXException(text, mmxErr.getCode()));
        }
      } else if ((xmppErr = error.getXMPPError()) != null) {
        Log.e(TAG, "onErrorReceived(): unsupported XMPP error="+xmppErr);
        fcode = MMXMessage.FailureCode.fromMMXFailureCode(FailureCode.SERVER_ERROR, null);
        notifyMessageSendError(error.getId(), fcode, xmppErr.getCondition());
      } else {
        Log.w(TAG, "onErrorReceived(): unsupported custom error="+error.getCustomError());
      }
    }
  };

  private MMX(Context context, MMXClientConfig config) {
    mContext = context.getApplicationContext();
    mClient = MMXClient.getInstance(context, config);
    mHandlerThread = new HandlerThread("MMX");
    mHandlerThread.start();
    mHandler = new Handler(mHandlerThread.getLooper());
    mSharedPrefs = context.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE);
  }

  @Override
  protected void finalize() throws Throwable {
    mHandlerThread.quit();
    super.finalize();
  }

  /**
   * Init the MMX API.  This init() method configures MMX using a
   * properties file located in the application's raw resources directory.
   *
   * <pre>
   *   Location:
   *     $APPLICATION_HOME/app/src/main/res/raw/myconfig.properties
   *
   *   Resource ID:
   *     R.raw.myconfig
   * </pre>
   *
   * NOTE:  init() will only execute once; subsequent calls are no-op
   *
   * @param context the Android context
   * @param configResId the R.raw. resource id containing the configuration
   */
  public static synchronized void init(Context context, int configResId) {
    init(context, new FileBasedClientConfig(context, configResId));
  }

  /**
   * This init method can be used for testing purposes.
   *
   * NOTE:  init() will only execute once; subsequent calls are no-op
   *
   * @param context the Android context
   * @param config the MMXClientConfig
   */
  static synchronized void init(Context context, MMXClientConfig config) {
    if (sInstance == null) {
      Log.i(TAG, "App="+context.getPackageName()+", MMX SDK="+BuildConfig.VERSION_NAME+
            ", protocol="+Constants.MMX_VERSION_MAJOR+"."+Constants.MMX_VERSION_MINOR);
      sInstance = new MMX(context, config);
    } else {
      Log.w(TAG, "MMX.init():  MMX has already been initialized.  Ignoring this call.");
    }
    MMXClient.registerWakeupListener(context, MMX.MMXWakeupListener.class);
  }

  /**
   * Enable or disable incoming messages.  The default state after login is disabled.
   *
   * @param enable true to receving incoming message, false otherwise
   * @throws IllegalStateException if the user is not logged-in
   * @see #login(String, byte[], OnFinishedListener)
   * @deprecated {@link #start()}
   */
  public static void enableIncomingMessages(boolean enable) {
    try {
      if (enable) {
        getMMXClient().resumeDelivery();
      } else {
        getMMXClient().suspendDelivery();
      }
    } catch (Exception ex) {
      //This only happens if we are not connected.  suspend is not a problem since we are already disconnected
      if (enable) {
        throw new IllegalStateException("Cannot enable incoming messages because not currently " +
                "connected.  Ensure that login() has been called.");
      }
    }
  }
  
  /**
   * Start the MMX messaging service.  The client application must call this
   * method when it is ready; otherwise, the communication between this client and
   * the server remains blocked.
   */
  public static void start() {
    enableIncomingMessages(true);
  }
  
  /**
   * Stop the MMX service.  The communication between this client and the server
   * will be blocked.
   */
  public static void stop() {
    enableIncomingMessages(false);
  }

  /**
   * Start sending/receiving messages as the specified user.  To receive messages:  1) implement and
   * register an EventListener, 2) enable incoming messages
   *
   * @param username the username
   * @param password the password
   * @param listener listener for success or failure
   * @throws IllegalStateException {@link #init(Context, int)} is not called yet
   * @see com.magnet.mmx.client.api.MMX.EventListener
   * @see #enableIncomingMessages(boolean)
   */
  public static void login(String username, byte[] password,
                           final OnFinishedListener<Void> listener) {
    if (!sInstance.mLoggingIn.compareAndSet(false, true)) {
      Log.d(TAG, "login() already logging in, returning failure");
      listener.onFailure(FailureCode.DEVICE_CONCURRENT_LOGIN, null);
      return;
    }

    getGlobalListener().registerListener(new MMXClient.MMXListener() {
      public void onConnectionEvent(MMXClient client, MMXClient.ConnectionEvent event) {
        Log.d(TAG, "login() received connection event: " + event);
        boolean unregisterListener = false;
        switch (event) {
          case AUTHENTICATION_FAILURE:
            listener.onFailure(MMX.FailureCode.SERVER_AUTH_FAILED, null);
            unregisterListener = true;
            break;
          case CONNECTED:
            try {
              sInstance.mCurrentUser = MMXUser.fromMMXid(client.getClientId());
              listener.onSuccess(null);
              unregisterListener = true;
            } catch (MMXException e) {
              // Should not happen because it is a connected.
              Log.e(TAG, "Unable to set current user profile", e);
            }
            break;
          case CONNECTION_FAILED:
            sInstance.mCurrentUser = null;
            listener.onFailure(MMX.FailureCode.SERVICE_UNAVAILABLE, null);
            unregisterListener = true;
            break;
        }
        if (unregisterListener) {
          sInstance.mLoggingIn.set(false);
          getGlobalListener().unregisterListener(this);
        }
      }

      public void onMessageReceived(MMXClient client,
                                    com.magnet.mmx.client.common.MMXMessage message,
                                    String receiptId) { }

      public void onSendFailed(MMXClient client, String messageId) { }

      public void onMessageDelivered(MMXClient client, MMXid recipient, String messageId) { }

      public void onMessageSubmitted(MMXClient client, String messageId) { }
      
      public void onMessageAccepted(MMXClient client, String messageId) { }

      public void onPubsubItemReceived(MMXClient client, MMXTopic topic,
                                       com.magnet.mmx.client.common.MMXMessage message) { }

      public void onErrorReceived(MMXClient client, MMXErrorMessage error) { }
    });
    sInstance.mCurrentUser = null;
    getMMXClient().connectWithCredentials(username, password, MMX.getGlobalListener(),
            new MMXClient.ConnectionOptions().setAutoCreate(false).setSuspendDelivery(true));
  }

  /**
   * Stop sending/receiving messages.
   * @throws IllegalStateException {@link #init(Context, int)} is not called yet
   */
  public static void logout(final OnFinishedListener<Void> listener) {
    getGlobalListener().registerListener(new MMXClient.MMXListener() {
      public void onConnectionEvent(MMXClient client, MMXClient.ConnectionEvent event) {
        Log.d(TAG, "logout() received connection event: " + event);
        boolean unregisterListener = false;
        switch (event) {
          case DISCONNECTED:
            if (listener != null) {
              listener.onSuccess(null);
            }
            unregisterListener = true;
            break;
        }
        if (unregisterListener) {
          getGlobalListener().unregisterListener(this);
        }
      }

      public void onMessageReceived(MMXClient client,
                                    com.magnet.mmx.client.common.MMXMessage message,
                                    String receiptId) { }

      public void onSendFailed(MMXClient client, String messageId) { }

      public void onMessageDelivered(MMXClient client, MMXid recipient, String messageId) { }

      public void onMessageSubmitted(MMXClient client, String messageId) { }
      
      public void onMessageAccepted(MMXClient client, String messageId) { }

      public void onPubsubItemReceived(MMXClient client,
                                       MMXTopic topic,
                                       com.magnet.mmx.client.common.MMXMessage message) { }

      public void onErrorReceived(MMXClient client, MMXErrorMessage error) { }
    });
    sInstance.mCurrentUser = null;
    getMMXClient().disconnect();
  }

  /**
   * Retrieves the current user for this session.  This may return
   * null if there is no logged-in user.
   *
   * @return the current user, null there is no logged-in user
   */
  public static MMXUser getCurrentUser() {

    return sInstance.mCurrentUser;
  }

  private synchronized static void checkState() {
    if (sInstance == null) {
      throw new IllegalStateException("MagnetMessage.init() must be called prior to invoking" +
              " any subsequent methods.  This call should most likely be placed in the Application.onCreate()" +
              " implementation.");
    }
  }

  /**
   * Registers the specified listener.  Listeners will be called from most-recently registered
   * to least recent.  If this listener has already been registered, it will be moved to "most recent".
   *
   * @param listener the listener to register
   * @return true if newly registered, false otherwise (if listener was already registered)
   * @throws IllegalStateException {@link #init(Context, int)} is not called yet
   */
  public static boolean registerListener(EventListener listener) {
    checkState();
    if (listener == null) {
      throw new IllegalArgumentException("Listener cannot be null.");
    }
    synchronized (sInstance.mListeners) {
      boolean exists = sInstance.mListeners.remove(listener);
      sInstance.mListeners.add(listener);
      return !exists;
    }
  }

  /**
   * Unregisters the specified listener.
   *
   * @param listener the listener to unregister
   * @return true if the listener was unregistered successfully, false if the listener was NOT known
   * @throws IllegalStateException {@link #init(Context, int)} is not called yet
   */
  public static boolean unregisterListener(EventListener listener) {
    checkState();
    if (listener == null) {
      throw new IllegalArgumentException("Listener cannot be null.");
    }
    synchronized (sInstance.mListeners) {
      return sInstance.mListeners.remove(listener);
    }
  }

  private static void notifyMessageSendError(String msgId, MMXMessage.FailureCode code, String text) {
    checkState();
    synchronized (sInstance.mListeners) {
      if (sInstance.mListeners.isEmpty()) {
        throw new IllegalStateException("Error dropped because there were no listeners registered.");
      }
      Iterator<EventListener> listeners = sInstance.mListeners.descendingIterator();
      while (listeners.hasNext()) {
        EventListener listener = listeners.next();
        try {
          if (listener.onMessageSendError(msgId, code, text)) {
            //listener returning true means consume the message
            break;
          }
        } catch (Exception ex) {
          Log.d(TAG, "notifyErrorReceived(): Caught exception while calling listener: " + listener, ex);
        }
      }
    }
  }

  private static void notifyMessageReceived(MMXMessage message) {
    checkState();
    synchronized (sInstance.mListeners) {
      if (sInstance.mListeners.isEmpty()) {
        throw new IllegalStateException("Message dropped because there were no listeners registered.");
      }
      Iterator<EventListener> listeners = sInstance.mListeners.descendingIterator();
      while (listeners.hasNext()) {
        EventListener listener = listeners.next();
        try {
          if (listener.onMessageReceived(message)) {
            //listener returning true means consume the message
            break;
          }
        } catch (Exception ex) {
          Log.d(TAG, "notifyMessageReceived(): Caught exception while calling listener: " + listener, ex);
        }
      }
    }
  }

  private static void notifyMessageAcknowledged(MMXid from, String originalMessageId) {
    synchronized (sInstance.mListeners) {
      if (sInstance.mListeners.isEmpty()) {
        throw new IllegalStateException("Acknowledgement dropped because there were no listeners registered.");
      }
      Iterator<EventListener> listeners = sInstance.mListeners.descendingIterator();
      while (listeners.hasNext()) {
        EventListener listener = listeners.next();
        try {
          MMXUser fromUser = MMXUser.fromMMXid(from);
          if (listener.onMessageAcknowledgementReceived(fromUser, originalMessageId)) {
            //listener returning true means consume the message
            break;
          }
        } catch (Exception ex) {
          Log.d(TAG, "notifyMessageAcknowledged(): Caught exception while calling listener: " + listener, ex);
        }
      }
    }
  }

  private static void notifyLoginRequired(LoginReason reason) {
    synchronized (sInstance.mListeners) {
      Iterator<EventListener> listeners = sInstance.mListeners.descendingIterator();
      while (listeners.hasNext()) {
        EventListener listener = listeners.next();
        try {
          if (listener.onLoginRequired(reason)) {
            //listener returning true means consume the message
            break;
          }
        } catch (Exception ex) {
          Log.d(TAG, "notifyLoginRequired(): Caught exception while calling listener: " + listener, ex);
        }
      }
    }
  }

  private static void notifyInviteReceived(MMXChannel.MMXInvite invite) {
    checkState();
    synchronized (sInstance.mListeners) {
      Iterator<EventListener> listeners = sInstance.mListeners.descendingIterator();
      while (listeners.hasNext()) {
        EventListener listener = listeners.next();
        try {
          if (listener.onInviteReceived(invite)) {
            //listener returning true means consume the message
            break;
          }
        } catch (Exception ex) {
          Log.d(TAG, "notifyInviteReceived(): Caught exception while calling listener: " + listener, ex);
        }
      }
    }
  }

  private static void notifyInviteResponseReceived(MMXChannel.MMXInviteResponse inviteResponse) {
    checkState();
    synchronized (sInstance.mListeners) {
      Iterator<EventListener> listeners = sInstance.mListeners.descendingIterator();
      while (listeners.hasNext()) {
        EventListener listener = listeners.next();
        try {
          if (listener.onInviteResponseReceived(inviteResponse)) {
            //listener returning true means consume the message
            break;
          }
        } catch (Exception ex) {
          Log.d(TAG, "notifyInviteResponseReceived(): Caught exception while calling listener: " + listener, ex);
        }
      }
    }
  }



  /**
   * Helper method to retrieve the Android context.
   *
   * @return the android application context
   */
  static synchronized Context getContext() {
    checkState();
    return sInstance.mContext;
  }

  /**
   * Helper method to retrieve the background thread Handler for
   * MagnetMessage.

   * @return the handler
   */
  static synchronized Handler getHandler() {
    checkState();
    return sInstance.mHandler;
  }

  /**
   * Helper method to retrieve the MMXClient instance.
   *
   * @return the MMXClient instance
   */
  static synchronized MMXClient getMMXClient() {
    checkState();
    return sInstance.mClient;
  }

  /**
   * Retrieves the global MMXListener.
   *
   * @return the global MMXListener
   */
  static synchronized AbstractMMXListener getGlobalListener() {
    return sInstance.mGlobalListener;
  }

  /**
   * Registers the wakeup broadcast intent.  This is handled internally using
   * Intent.toUri().  This is part of the GCM wakeup functionality of MMX.  There are several
   * prerequisites for this to work properly.  Please see the documentation on how to:<br/>
   *
   * <ol>
   *   <li>Register your application for Google Cloud Messaging and receive a senderId and google API key</li>
   *   <li>Configure your MMX application using the Messaging console.  (You will need the senderId and API key from step 1)</li>
   *   <li>Configure your MMX Android application (download the properties file from the Messaging console and place it in res/raw/</li>
   *   <li>Ensure that your manifest file has the appropriate receiver and permission declarations.  (see developer guide)</li>
   * </ol>
   *
   * @param intent the intent to register
   * @throws IllegalStateException {@link #init(Context, int)} is not called yet
   */
  public synchronized static void registerWakeupBroadcast(Intent intent) {
    checkState();
    //FIXME:  check to see if the broadcast receiver was registered
    sInstance.mSharedPrefs.edit().putString(PREF_WAKEUP_INTENT_URI,
            intent.toUri(Intent.URI_INTENT_SCHEME)).apply();
  }

  /**
   * Unregisters the wakeup broadcast intent.
   * @throws IllegalStateException {@link #init(Context, int)} is not called yet
   */
  public synchronized static void unregisterWakeupBroadcast() {
    checkState();
    sInstance.mSharedPrefs.edit().remove(PREF_WAKEUP_INTENT_URI).apply();
  }

  /**
   * Perform the wakeup
   */
  private synchronized static void wakeup(Intent nestedIntent) {
    checkState();
    Context context = sInstance.mContext;
    String intentUri = sInstance.mSharedPrefs.getString(PREF_WAKEUP_INTENT_URI, null);
    if (intentUri != null) {
      try {
        Intent intent = Intent.parseUri(intentUri, Intent.URI_INTENT_SCHEME);
        intent.setPackage(context.getPackageName()); //only broadcast to the current package
        intent.putExtra(EXTRA_NESTED_INTENT, nestedIntent.toUri(Intent.URI_INTENT_SCHEME));
        Log.d(TAG, "wakeup(): sendBroadcast(): " + intent);
        context.sendBroadcast(intent);
      } catch (Exception e) {
        Log.e(TAG, "wakeup(): unable to perform wakeup with uri string: " + intentUri, e);
      }
    } else {
      Log.i(TAG, "wakeup(): no intent was registered with MMX.registerWakeupBroadcast().  ignoring");
    }
  }

  /**
   * Default implementation for the internal wakeup listener.  This will ultimately cause the
   * intent registered in registerWakeupBroadcast() to be broadcast.
   *
   * @see #registerWakeupBroadcast(Intent)
   */
  public static final class MMXWakeupListener implements MMXClient.MMXWakeupListener {
    public void onWakeupReceived(Context applicationContext, Intent intent) {
      Log.d(TAG, "onWakeupReceived() start");
      wakeup(intent);
    }
  }

  /**
   * This class represents a push message using the Intent interface.  A push
   * message is usually sent from Console, transported by GCM, and received
   * by the Wakeup Receiver.
   */
  public static final class MMXPushMessage {
    private final static String TAG = MMXPushMessage.class.getSimpleName();
    private Intent mIntent;

    /**
     * Parse an intent for a push message.  If the intent is not a valid MMX
     * intent, null will be returned.
     * @param intent The intent from the Wakeup Receiver
     * @return MMXPushMessage or null.
     */
    public static MMXPushMessage parse(Intent intent) {
      String nestedIntent = null;
      try {
        nestedIntent = intent.getStringExtra(MMX.EXTRA_NESTED_INTENT);
        if (nestedIntent == null) {
          return null;
        }
        MMXPushMessage mmxPushMsg = new MMXPushMessage();
        mmxPushMsg.mIntent = Intent.parseUri(nestedIntent, Intent.URI_INTENT_SCHEME);
        return mmxPushMsg;
      } catch (URISyntaxException e) {
        Log.w(TAG, "Ignored the malformed MMX intent: "+nestedIntent);
        return null;
      }
    }

    /**
     * Get the intent that contains the push message.
     * @return The intent.
     */
    public Intent getIntent() {
      return mIntent;
    }

    /**
     * Get the text sent from the Console.
     * @return A text.
     */
    public String getText() {
      return mIntent.getStringExtra(MMXClient.EXTRA_PUSH_BODY);
    }
  }
}

