package com.magnet.mmx.client.api;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.HandlerThread;

import com.magnet.mmx.client.AbstractMMXListener;
import com.magnet.mmx.client.FileBasedClientConfig;
import com.magnet.mmx.client.MMXClient;
import com.magnet.mmx.client.MMXClientConfig;
import com.magnet.mmx.client.common.*;
import com.magnet.mmx.protocol.MMXTopic;

import java.util.Iterator;
import java.util.LinkedList;

/**
 * The main entry point for Magnet Message.
 */
public final class MMX {
  /**
   * Possible failure codes returned in the OnFinishedListener.onFailure method.
   * @see com.magnet.mmx.client.api.MMX.OnFinishedListener#onFailure(FailureCode, Throwable)
   */
  public static class FailureCode {
    public static final FailureCode DEVICE_ERROR = new FailureCode(1);
    public static final FailureCode DEVICE_CONNECTION_FAILED = new FailureCode(2);
    public static final FailureCode SERVER_AUTH_FAILED = new FailureCode(3);
    public static final FailureCode SERVER_BAD_STATUS = new FailureCode(4);
    public static final FailureCode SERVER_EXCEPTION = new FailureCode(5);
    private int mValue;

    FailureCode(int value) {
      mValue = value;
    }

    public final int getValue() {
      return mValue;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      FailureCode that = (FailureCode) o;

      return mValue == that.mValue;

    }

    @Override
    public int hashCode() {
      return mValue;
    }
  }

  /**
   * Possible reasons for EventListener.onLoginRequired
   * @see com.magnet.mmx.client.api.MMX.EventListener#onLoginRequired(LoginReason)
   */
  public enum LoginReason {
    DISCONNECTED
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
     * Called when an acknowledgement is received.  The default implementation of this is a
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
     * Called when a login is required. The default implementation of this
     * method is a no-op.
     *
     * @param reason the reason why login is required
     * @return true to consume this event, false for additional listeners to be called
     * @see #login(String, byte[], OnFinishedListener)
     */
    public boolean onLoginRequired(LoginReason reason) {
      //default implementation is a no-op
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
        notifyMessageReceived(MMXMessage.fromMMXMessage(null, mmxMessage));
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

    public void onMessageAccepted(MMXClient mmxClient, MMXid recipient, String messageId) {
      MMXMessage.handleServerAck(recipient, messageId);
      super.onMessageAccepted(mmxClient, recipient, messageId);
    }

    @Override
    public void onConnectionEvent(MMXClient mmxClient, MMXClient.ConnectionEvent connectionEvent) {
      switch (connectionEvent) {
        case DISCONNECTED:
          notifyLoginRequired(LoginReason.DISCONNECTED);
          break;
      }
      super.onConnectionEvent(mmxClient, connectionEvent);
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
   * Start sending/receiving messages as the specified user.  To receive messages:  1) implement and
   * register an EventListener, 2) enable incoming messages
   *
   * @param username the username
   * @param password the password
   * @param listener listener for success or failure
   * @see com.magnet.mmx.client.api.MMX.EventListener
   * @see #enableIncomingMessages(boolean)
   */
  public static void login(String username, byte[] password,
                           final OnFinishedListener<Void> listener) {
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
            listener.onFailure(MMX.FailureCode.DEVICE_CONNECTION_FAILED, null);
            unregisterListener = true;
            break;
        }
        if (unregisterListener) {
          getGlobalListener().unregisterListener(this);
        }
      }

      public void onMessageReceived(MMXClient client,
                                    com.magnet.mmx.client.common.MMXMessage message,
                                    String receiptId) {

      }

      public void onSendFailed(MMXClient client, String messageId) {

      }

      public void onMessageDelivered(MMXClient client, MMXid recipient, String messageId) {

      }

      public void onMessageAccepted(MMXClient client, MMXid recipient, String messageId) {

      }

      public void onPubsubItemReceived(MMXClient client, MMXTopic topic,
                                       com.magnet.mmx.client.common.MMXMessage message) {

      }

      public void onErrorReceived(MMXClient client, MMXErrorMessage error) {

      }
    });
    sInstance.mCurrentUser = null;
    getMMXClient().connectWithCredentials(username, password, MMX.getGlobalListener(),
            new MMXClient.ConnectionOptions().setAutoCreate(false).setSuspendDelivery(true));
  }

  /**
   * Stop sending/receiving messages.
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
                                    String receiptId) {

      }

      public void onSendFailed(MMXClient client, String messageId) {

      }

      public void onMessageDelivered(MMXClient client, MMXid recipient, String messageId) {

      }

      public void onMessageAccepted(MMXClient client, MMXid recipient, String messageId) {

      }

      public void onPubsubItemReceived(MMXClient client,
                                       MMXTopic topic,
                                       com.magnet.mmx.client.common.MMXMessage message) {

      }

      public void onErrorReceived(MMXClient client, MMXErrorMessage error) {

      }
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
   */
  public synchronized static void registerWakeupBroadcast(Intent intent) {
    checkState();
    //FIXME:  check to see if the broadcast receiver was registered
    sInstance.mSharedPrefs.edit().putString(PREF_WAKEUP_INTENT_URI,
            intent.toUri(Intent.URI_INTENT_SCHEME)).apply();
  }

  /**
   * Unregisters the wakeup broadcast intent.
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
   * Default implementation fo the internal wakeup listener.  This will ulimtately cause the
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
}

