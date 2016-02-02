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
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jivesoftware.smack.packet.XMPPError;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;

import com.magnet.max.android.ApiCallback;
import com.magnet.max.android.ApiError;
import com.magnet.max.android.MaxCore;
import com.magnet.max.android.MaxModule;
import com.magnet.max.android.User;
import com.magnet.mmx.BuildConfig;
import com.magnet.mmx.client.AbstractMMXListener;
import com.magnet.mmx.client.DeviceIdAccessor;
import com.magnet.mmx.client.DeviceIdGenerator;
import com.magnet.mmx.client.FileBasedClientConfig;
import com.magnet.mmx.client.MMXClient;
import com.magnet.mmx.client.MMXClientConfig;
import com.magnet.mmx.client.common.Log;
import com.magnet.mmx.client.common.MMXErrorMessage;
import com.magnet.mmx.client.common.MMXPayload;
import com.magnet.mmx.client.common.MessageHandlingException;
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
 * <li>{@link #registerWakeupBroadcast(Context, Intent)} to register a GCM wake up listener</li>
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
    public boolean onMessageAcknowledgementReceived(User from, String messageId) {
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
  private Context mContext = null;
  private MMXClient mClient = null;
  private User mCurrentUser = null;
  private HandlerThread mHandlerThread = null;
  private Handler mHandler = null;
  private static MMXModule sModule = null;
  private static MMX sInstance = null;
  private static SharedPreferences sSharedPrefs = null;
  private static Handler sCallbackHandler = new Handler(Looper.getMainLooper());

  // Avoid concurrent logging
  private final AtomicBoolean mLoggingIn = new AtomicBoolean(false);

  /**
   * The listeners will be added in order (most recent at end)
   * They should be called in order from most recently registered (from the end)
   */
  private static final LinkedList<EventListener> sListeners = new LinkedList<EventListener>();

  private AbstractMMXListener mGlobalListener = new AbstractMMXListener() {
    @Override
    public void handleMessageReceived(MMXClient mmxClient, com.magnet.mmx.client.common.MMXMessage mmxMessage, String receiptId) {
      MMXPayload payload = mmxMessage.getPayload();
      String type = payload.getType();
      if (MMXChannel.MMXInvite.TYPE.equals(type)) {
        MMXChannel.MMXInvite invite = MMXChannel.MMXInvite.fromMMXMessage(MMXMessage.fromMMXMessage(null, mmxMessage));
        if (invite != null) {
          notifyInviteReceived(invite);
        }
      } else if (MMXChannel.MMXInviteResponse.TYPE.equals(type)) {
        MMXChannel.MMXInviteResponse inviteResponse = MMXChannel.MMXInviteResponse.fromMMXMessage(MMXMessage.fromMMXMessage(null, mmxMessage));
        if (inviteResponse != null) {
          notifyInviteResponseReceived(inviteResponse);
        }
      } else {

        MMXMessage message = MMXMessage.fromMMXMessage(null, mmxMessage);
        if (message != null) {
          message.receiptId(receiptId);
          notifyMessageReceived(message);
        } else {
          throw new MessageHandlingException("Unable to handle message.");
        }
      }
    }

    @Override
    public void handleMessageDelivered(MMXClient mmxClient, MMXid recipient, String messageId) {

    }

    @Override
    public void handlePubsubItemReceived(MMXClient mmxClient, MMXTopic mmxTopic, com.magnet.mmx.client.common.MMXMessage mmxMessage) {
      MMXMessage message = MMXMessage.fromMMXMessage(mmxTopic, mmxMessage);
      if (message != null) {
        notifyMessageReceived(message);
      }
    }

    @Override
    public void onMessageDelivered(MMXClient mmxClient, MMXid recipient, String messageId) {
      notifyMessageAcknowledged(recipient, messageId);
      super.onMessageDelivered(mmxClient, recipient, messageId);
    }

    @Override
    public void onMessageSubmitted(MMXClient mmxClient, String messageId) {
      MMXMessage.handleMessageSubmitted(messageId);
      super.onMessageSubmitted(mmxClient, messageId);
    }
    
    @Override
    public void onMessageAccepted(MMXClient mmxClient, List<MMXid> invalidRecipients, String messageId) {
      MMXMessage.handleMessageAccepted(invalidRecipients, messageId);
      super.onMessageAccepted(mmxClient, invalidRecipients, messageId);
    }

    @Override
    public void onConnectionEvent(MMXClient mmxClient, MMXClient.ConnectionEvent connectionEvent) {
      switch (connectionEvent) {
        case AUTHENTICATION_FAILURE:
          Log.w(TAG, "onConnectionEvent : AUTHENTICATION_FAILURE, + mLoggingIn : " + mLoggingIn.get());
          if (!mLoggingIn.get()) {
            MaxCore.userTokenInvalid(null, null);
            notifyLoginRequired(LoginReason.CREDENTIALS_EXPIRED);
          }
          break;
        case CONNECTED:
          Log.w(TAG, "onConnectionEvent : CONNECTED, + mLoggingIn : " + mLoggingIn.get());
          if (!mLoggingIn.get()) {
            sInstance.mCurrentUser = User.getCurrentUser();
            notifyLoginRequired(LoginReason.SERVICE_AVAILABLE);
          }
          break;
        case CONNECTION_FAILED:
          Log.w(TAG, "onConnectionEvent : CONNECTION_FAILED, + mLoggingIn : " + mLoggingIn.get());
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
    // onMessageSendError().  The implementation is to be handled by
    // onMesasgeSendError(); it will break the backward compatibility.  To use
    // the hybrid mode, see git commit 422c8d147c386ccf939b064ddebe40dd99fe9cf6
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
          notifyMessageSendError(mmxErr.getMsgId(), fcode, text);
        } else if (mmxErr.getCode() == MMXMessage.FailureCode.CONTENT_TOO_LARGE.getValue()) {
          fcode = MMXMessage.FailureCode.CONTENT_TOO_LARGE;
          text = mmxErr.getMessage();
          notifyMessageSendError(mmxErr.getMsgId(), fcode, text);
        } else {
          fcode = MMXMessage.FailureCode.fromMMXFailureCode(FailureCode.SERVER_ERROR, null);
          text = mmxErr.getMessage();
          notifyMessageSendError(mmxErr.getMsgId(), fcode, text);
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
    mHandlerThread = new HandlerThread("MMX");
    mHandlerThread.start();
    mClient = MMXClient.getInstance(context, config);
    mHandler = new Handler(mHandlerThread.getLooper());
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
  public static synchronized void init(Context context, MMXClientConfig config) {
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
   */
  private static void enableIncomingMessages(boolean enable) {
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
                "connected.  Ensure that login() has been called.", ex);
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
    Log.d(TAG, "--------login MMX for user " + username + ", token : " + new String(password));
    if (!sInstance.mLoggingIn.compareAndSet(false, true)) {
      Log.d(TAG, "login() already logging in, returning failure");
      getCallbackHandler().post(new Runnable() {
        public void run() {
          listener.onFailure(FailureCode.DEVICE_CONCURRENT_LOGIN, null);
        }
      });
      return;
    }

    getGlobalListener().registerListener(new MMXClient.MMXListener() {
      public void onConnectionEvent(MMXClient client, MMXClient.ConnectionEvent event) {
        Log.d(TAG, "login() received connection event: " + event);
        boolean unregisterListener = false;
        switch (event) {
          case AUTHENTICATION_FAILURE:
            getCallbackHandler().post(new Runnable() {
              public void run() {
                listener.onFailure(MMX.FailureCode.SERVER_AUTH_FAILED, null);
              }
            });
            unregisterListener = true;
            break;
          case CONNECTED:
            sInstance.mCurrentUser = User.getCurrentUser();
            getCallbackHandler().post(new Runnable() {
              public void run() {
                listener.onSuccess(null);
              }
            });
            unregisterListener = true;
            break;
          case CONNECTION_FAILED:
            sInstance.mCurrentUser = null;
            getCallbackHandler().post(new Runnable() {
              public void run() {
                listener.onFailure(MMX.FailureCode.SERVICE_UNAVAILABLE, null);
              }
            });
            unregisterListener = true;
            break;
        }
        if (unregisterListener) {
          sInstance.mLoggingIn.set(false);
          synchronized (sInstance.mLoggingIn) {
            sInstance.mLoggingIn.notify();
          }
          getGlobalListener().unregisterListener(this);
        }
      }

      public void onMessageReceived(MMXClient client,
                                    com.magnet.mmx.client.common.MMXMessage message,
                                    String receiptId) { }

      public void onSendFailed(MMXClient client, String messageId) { }

      public void onMessageDelivered(MMXClient client, MMXid recipient, String messageId) { }

      public void onMessageSubmitted(MMXClient client, String messageId) { }
      
      public void onMessageAccepted(MMXClient client, List<MMXid> invalidRecipients, String messageId) { }

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
    Log.d(TAG, "--------logout MMX for user " + (sInstance.mCurrentUser != null ? sInstance.mCurrentUser.getUserName() : ""));
    getGlobalListener().registerListener(new MMXClient.MMXListener() {
      public void onConnectionEvent(MMXClient client, MMXClient.ConnectionEvent event) {
        Log.d(TAG, "logout() received connection event: " + event);
        boolean unregisterListener = false;
        switch (event) {
          case DISCONNECTED:
            if (listener != null) {
              getCallbackHandler().post(new Runnable() {
                public void run() {
                  listener.onSuccess(null);
                }
              });
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
      
      public void onMessageAccepted(MMXClient client, List<MMXid> invalidRecipients, String messageId) { }

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
  public static User getCurrentUser() {
    return sInstance == null ? null : sInstance.mCurrentUser;
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
    if (listener == null) {
      throw new IllegalArgumentException("Listener cannot be null.");
    }
    synchronized (sListeners) {
      boolean exists = sListeners.remove(listener);
      sListeners.add(listener);
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
    if (listener == null) {
      throw new IllegalArgumentException("Listener cannot be null.");
    }
    synchronized (sListeners) {
      return sListeners.remove(listener);
    }
  }

  private static EventListener[] cloneListeners() {
    checkState();
    synchronized (sListeners) {
      EventListener[] result = new EventListener[sListeners.size()];
      return sListeners.toArray(result);
    }
  }

  private static void notifyMessageSendError(final String msgId,
                                             final MMXMessage.FailureCode code, final String text) {
    final EventListener[] listeners = cloneListeners();
    if (listeners.length == 0) {
      throw new IllegalStateException("Error dropped because there were no listeners registered.");
    }
    getCallbackHandler().post(new Runnable() {
      public void run() {
        for (int i=listeners.length; --i>=0;){
          EventListener listener = listeners[i];
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
    });
  }

  private static void notifyMessageReceived(final MMXMessage message) {
    final EventListener[] listeners = cloneListeners();
    if (listeners.length == 0) {
      throw new IllegalStateException("Message dropped because there were no listeners registered.");
    }
    getCallbackHandler().post(new Runnable() {
      public void run() {
        for (int i=listeners.length; --i>=0;) {
          EventListener listener = listeners[i];
          try {
            if (listener.onMessageReceived(message)) {
              //listener returning true means consume the message
              break;
            }
          } catch (Throwable ex) {
            Log.d(TAG, "notifyMessageReceived(): Caught exception while calling listener: " + listener, ex);
          }
        }
      }
    });
  }

  private static void notifyMessageAcknowledged(final MMXid from, final String originalMessageId) {
    final EventListener[] listeners = cloneListeners();
    if (listeners.length == 0) {
        throw new IllegalStateException("Acknowledgement dropped because there were no listeners registered.");
    }
    HashSet<String> userToRetrieve = new HashSet<String>();
    userToRetrieve.add(from.getUserId());

    UserCache userCache = UserCache.getInstance();
    userCache.fillCacheByUserId(userToRetrieve, UserCache.DEFAULT_ACCEPTED_AGE);
    final User fromUser = userCache.getByUserId(from.getUserId());
    getCallbackHandler().post(new Runnable() {
      public void run() {
        for (int i=listeners.length; --i>=0;) {
          EventListener listener = listeners[i];
          try {
            if (listener.onMessageAcknowledgementReceived(fromUser, originalMessageId)) {
              //listener returning true means consume the message
              break;
            }
          } catch (Exception ex) {
            Log.d(TAG, "notifyMessageAcknowledged(): Caught exception while calling listener: " + listener, ex);
          }
        }
      }
    });
  }

  private static void notifyLoginRequired(final LoginReason reason) {
    getCallbackHandler().post(new Runnable() {
      public void run() {
        EventListener[] listeners = cloneListeners();
        for (int i=listeners.length;--i>=0;) {
          EventListener listener = listeners[i];
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
    });
  }

  private static void notifyInviteReceived(final MMXChannel.MMXInvite invite) {
    getCallbackHandler().post(new Runnable() {
      public void run() {
        EventListener[] listeners = cloneListeners();
        for (int i=listeners.length;--i>=0;) {
          EventListener listener = listeners[i];
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
    });
  }

  private static void notifyInviteResponseReceived(final MMXChannel.MMXInviteResponse inviteResponse) {
    getCallbackHandler().post(new Runnable() {
      public void run() {
        final EventListener[] listeners = cloneListeners();
        for (int i=listeners.length;--i>=0;) {
          EventListener listener = listeners[i];
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
    });
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

  private synchronized static SharedPreferences getSharedPrefs(Context context) {
    if (sSharedPrefs == null) {
      sSharedPrefs = context
              .getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE);
    }
    return sSharedPrefs;
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
  public synchronized static void registerWakeupBroadcast(Context context, Intent intent) {
    //FIXME:  check to see if the broadcast receiver was registered
    getSharedPrefs(context).edit().putString(PREF_WAKEUP_INTENT_URI,
            intent.toUri(Intent.URI_INTENT_SCHEME)).apply();
  }

  /**
   * Unregisters the wakeup broadcast intent.
   * @throws IllegalStateException {@link #init(Context, int)} is not called yet
   */
  public synchronized static void unregisterWakeupBroadcast(Context context) {
    getSharedPrefs(context).edit().remove(PREF_WAKEUP_INTENT_URI).apply();
  }

  /**
   * Perform the wakeup
   */
  private synchronized static void wakeup(Context context, Intent nestedIntent) {
    String intentUri = getSharedPrefs(context).getString(PREF_WAKEUP_INTENT_URI, null);
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
   * @see #registerWakeupBroadcast(Context, Intent)
   */
  public static final class MMXWakeupListener implements MMXClient.MMXWakeupListener {
    /**
     * This class represents a push message using the Intent interface.  A push
     * message is usually sent from Console, transported by GCM, and received
     * by the Wakeup Receiver.
     * @deprecated com.magnet.mmx.client.api.MMXPushEvent
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

    public void onWakeupReceived(Context applicationContext, Intent intent) {
      Log.d(TAG, "onWakeupReceived() start");
      wakeup(applicationContext, intent);
    }
  }

  /**
   * Retreive the MaxModule for MMX
   * @return the MMX MaxModule
   */
  public static synchronized final MaxModule getModule() {
    if (sModule == null) {
      sModule = new MMXModule();
    }
    return sModule;
  }

  /**
   * The MaxModule implementation to be used with Max.initModule()
   */
  public static class MMXModule implements MaxModule {
    private final String TAG = MMXModule.class.getSimpleName();
    private Context mContext;
    private ApiCallback<Boolean> mCallback;

    private MMXModule() {
    }

    @Override
    public String getName() {
      return MMXModule.class.getSimpleName();
    }

    @Override
    public void onInit(Context context, final Map<String, String> configs,
                       ApiCallback<Boolean> callback) {
      Log.d(TAG, "onInit(): start");
      for (Map.Entry<String,String> entry:configs.entrySet()) {
        Log.d(TAG, "onInit(): key=" + entry.getKey() + ", value=" + entry.getValue());
      }
      mContext = context.getApplicationContext();
      mCallback = callback;

      //map the configs into a clientConfig and init
      MMXClientConfig config = new MaxClientConfig(configs);
      MMX.init(context, config);
    }

    @Override
    public void onAppTokenUpdate(String appToken, String appId, String deviceId, ApiCallback<Boolean> callback) {
      //not implemented for now
      Log.d(TAG, "onAppTokenUpdate(): Not implemented for now.  appId=" + appId +
              ", deviceId=" + deviceId + ", appToken=" + appToken);
    }

    @Override
    public void onUserTokenUpdate(final String userToken, final String userId,
        final String deviceId, final ApiCallback<Boolean> callback) {
      Log.d(TAG, "onUserTokenUpdate(): userId=" + userId +
              ", deviceId=" + deviceId + ", userToken=" + userToken);
      //set the deviceId
      DeviceIdGenerator.setDeviceIdAccessor(mContext, new DeviceIdAccessor() {
        public String getId(Context context) {
          return deviceId;
        }

        public boolean obfuscated() {
          return false;
        }
      });
      if (MMX.getCurrentUser() != null) {
        Log.d(TAG, "onUserTokenUpdate(): already logged in, performing logout/login");
        //if already logged in, need to logout/login again
        MMX.logout(new MMX.OnFinishedListener<Void>() {
          public void onSuccess(Void result) {
            Log.d(TAG, "onUserTokenUpdate(): logout success");
            loginHelper(userId, deviceId, userToken, callback);
          }

          public void onFailure(MMX.FailureCode code, Throwable ex) {
            Log.e(TAG, "onUserTokenUpdate(): logout failure: " + code, ex);
            loginHelper(userId, deviceId, userToken, callback);
          }
        });
      } else {
        loginHelper(userId, deviceId, userToken, callback);
      }
    }

    private void loginHelper(final String userId, final String deviceId,
                             final String userToken, final ApiCallback<Boolean> callback) {
      if (userId != null && deviceId != null && userToken != null) {
        MMX.login(userId, userToken.getBytes(), new MMX.OnFinishedListener<Void>() {
          @Override
          public void onSuccess(Void result) {
            Log.d(TAG, "loginHelper(): success");
            if(null != callback) {
              callback.success(true);
            } else if (mCallback != null) {
              mCallback.success(true);
            }
          }

          @Override
          public void onFailure(MMX.FailureCode code, Throwable ex) {
            Log.e(TAG, "loginHelper(): failure=" + code, ex);
            ApiError error = new ApiError("Unable to login: " +
                code, ApiError.API_ERROR_UNEXPECTED, ex);
            if(null != callback) {
              callback.failure(error);
            } else if (mCallback != null) {
              mCallback.failure(error);
            }
          }
        });
      }
    }

    @Override
    public void onClose(boolean gracefully) {
      Log.d(TAG, "onClose(): gracefully = " + gracefully);
    }

    @Override
    public void onUserTokenInvalidate(final ApiCallback<Boolean> callback) {
      logoutHelper();
    }

    @Override
    public void deInitModule(final ApiCallback<Boolean> callback) {
      logoutHelper();
    }

    private void logoutHelper() {
      if (getCurrentUser() != null) {
        MMX.logout(new OnFinishedListener<Void>() {
                     @Override
                     public void onSuccess(Void result) {
                       Log.d(TAG, "logoutHelper(): logout successful");
                     }

                     @Override
                     public void onFailure(FailureCode code, Throwable ex) {
                       Log.w(TAG, "logoutHelper(): logout failed: " + code, ex);
                     }
                   }
        );
      } else {
        Log.d(TAG, "logoutHelper(): not logged in");
      }
    }
  }

  private static class MaxClientConfig implements MMXClientConfig {
    private final String TAG = MaxClientConfig.class.getSimpleName();

    private Map<String,String> mConfigs = null;
    private static final String APP_ID = "mmx-appId";
    private static final String APP_API_KEY = "mmx-apiKey";
    private static final String APP_GCM_SENDER_ID = "mmx-gcmSenderId";
    private static final String SECURITY_POLICY = "security-policy";
    private static final String DOMAIN = "mmx-domain";
    private static final String PORT = "mmx-port";
    private static final String HOST = "mmx-host";
    private static final String REST_PORT = "mmx-rest-port";

    private MaxClientConfig(Map<String,String> configs) {
      mConfigs = configs;
    }

    @Override
    public String getAppId() {
      return mConfigs.get(APP_ID);
    }

    @Override
    public String getApiKey() {
      return mConfigs.get(APP_API_KEY);
    }

    @Override
    public String getGcmSenderId() {
      return mConfigs.get(APP_GCM_SENDER_ID);
    }

    @Override
    public String getServerUser() {
      Log.d(TAG, "getServerUser(): NOT IMPLEMENTED");
      return null;
    }

    @Override
    public String getAnonymousSecret() {
      Log.d(TAG, "getAnonymousSecret(): NOT IMPLEMENTED");
      return null;
    }

    @Override
    public String getHost() {
      return mConfigs.get(HOST);
    }

    @Override
    public int getPort() {
      return Integer.parseInt(mConfigs.get(PORT));
    }

    @Override
    public int getRESTPort() {
      return Integer.parseInt(mConfigs.get(REST_PORT));
    }

    @Override
    public String getDomainName() {
      return mConfigs.get(DOMAIN);
    }

    @Override
    public MMXClient.SecurityLevel getSecurityLevel() {
      return MMXClient.SecurityLevel.valueOf(mConfigs.get(SECURITY_POLICY));
    }

    @Override
    public String getDeviceId() {
      Log.d(TAG, "getDeviceId(): NOT IMPLEMENTED");
      return null;
    }

    @Override
    public boolean obfuscateDeviceId() {
      return false;
    }
  }

  /**
   * FOR TESTING:  Sets the callback handler for MMX callbacks.
   * By default, the MMX will make callbacks on the main thread.
   *
   * @param handler the handler to use for callbacks
   */
  public static void setCallbackHandler(Handler handler) {
    if (handler == null) {
      throw new IllegalArgumentException("Callback handler cannot be null.");
    }
    sCallbackHandler = handler;
  }

  /**
   * Retrieve the callback handler for MMX.
   *
   * @return the callback handler
   */
  static Handler getCallbackHandler() {
    return sCallbackHandler;
  }
}


