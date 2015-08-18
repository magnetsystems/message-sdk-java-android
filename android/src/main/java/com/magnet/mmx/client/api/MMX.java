package com.magnet.mmx.client.api;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;

import com.magnet.mmx.client.AbstractMMXListener;
import com.magnet.mmx.client.FileBasedClientConfig;
import com.magnet.mmx.client.MMXClient;
import com.magnet.mmx.client.MMXClientConfig;
import com.magnet.mmx.client.MMXTask;
import com.magnet.mmx.client.common.*;
import com.magnet.mmx.protocol.MMXStatus;
import com.magnet.mmx.protocol.MMXTopic;
import com.magnet.mmx.protocol.UserInfo;

import java.util.Iterator;
import java.util.LinkedList;

/**
 * The main entry point for Magnet Message.
 */
public final class MMX {
  public enum FailureCode {
    DEVICE_ERROR,
    DEVICE_CONNECTION_FAILED,
    SERVER_AUTH_FAILED,
    SERVER_BAD_STATUS,
    SERVER_EXCEPTION
  }
  /**
   * The listener interface for handling incoming messages and message acknowledgements.
   */
  public interface OnMessageReceivedListener {
    /**
     * Invoked when incoming message is received.
     *
     * @param message the incoming message
     * @return true to consume this message, false for additional listeners to be called
     */
    boolean onMessageReceived(MMXMessage message);

    /**
     * Called when an acknowledgement is received.
     *
     * @param from the user who acknowledged the message
     * @param messageId the message id that was acknowledged
     * @return true to consume this message, false for additional listeners to be called
     */
    boolean onMessageAcknowledgementReceived(MMXid from, String messageId);
  }

  /**
   * The listener interface used by the asynchronous calls.
   *
   * @param <T> The parameter type with which the onSuccess callback will be invoked
   */
  public interface OnFinishedListener<T> {
    /**
     * Invoked if the operation succeeded
     *
     * @param result the result of the operation
     */
    void onSuccess(T result);

    /**
     * Invoked if the operation failed
     * 
     * @param code the failure code
     * @param ex the exception, null if no exception
     */
    void onFailure(FailureCode code, Throwable ex);
  }

  private static final String TAG = MMX.class.getSimpleName();
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
  private final LinkedList<OnMessageReceivedListener> mListeners = new LinkedList<OnMessageReceivedListener>();

  private AbstractMMXListener mGlobalListener = new AbstractMMXListener() {
    @Override
    public void handleMessageReceived(MMXClient mmxClient, com.magnet.mmx.client.common.MMXMessage mmxMessage, String receiptId) {
      notifyMessageReceived(MMXMessage.fromMMXMessage(null, mmxMessage));
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
  };

  private MMX(Context context, MMXClientConfig config) {
    mContext = context.getApplicationContext();
    mClient = MMXClient.getInstance(context, config);
    mHandlerThread = new HandlerThread("MagnetMessage");
    mHandlerThread.start();
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
  static synchronized void init(Context context, MMXClientConfig config) {
    if (sInstance == null) {
      sInstance = new MMX(context, config);
    } else {
      Log.w(TAG, "MMX.init():  MMX has already been initialized.  Ignoring this call.");
    }
  }

  /**
   * Start sending/receiving messages as the specified user.
   *
   * @param username the username
   * @param password the password
   * @param listener listener for success or failure
   */
  public static void login(String username, byte[] password,
                           final MMX.OnFinishedListener<Void> listener) {
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
              //// FIXME: 8/17/15 MOVE THIS TO MMXClient on successful login and call MMXConnection.setDisplayName
              UserInfo info = client.getAccountManager().getUserInfo();
              sInstance.mCurrentUser = new MMXUser.Builder()
                      .username(info.getUserId())
                      .displayName(info.getDisplayName())
                      .build();
            } catch (MMXException ex) {
              Log.e(TAG, "login(): login succeeded but unable to retrieve user info", ex);
            }
            listener.onSuccess(null);
            unregisterListener = true;
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

      public void onPubsubItemReceived(MMXClient client, MMXTopic topic,
                                       com.magnet.mmx.client.common.MMXMessage message) {

      }

      public void onErrorReceived(MMXClient client, MMXErrorMessage error) {

      }
    });
    sInstance.mCurrentUser = null;
    getMMXClient().connectWithCredentials(username, password, MMX.getGlobalListener(),
            new MMXClient.ConnectionOptions().setAutoCreate(false));
  }

  /**
   * Start sending/receiving messages as an anonymous user.
   *
   * @deprecated
   * @param listener listener for success or failure
   */
  static void loginAnonymous(final MMX.OnFinishedListener<Void> listener) {
    getGlobalListener().registerListener(new MMXClient.MMXListener() {
      public void onConnectionEvent(MMXClient client, MMXClient.ConnectionEvent event) {
        Log.d(TAG, "login() received connection event: " + event);
        boolean unregisterListener = false;
        switch (event) {
          case AUTHENTICATION_FAILURE:
            if (listener != null) {
              listener.onFailure(MMX.FailureCode.SERVER_AUTH_FAILED, null);
            }
            unregisterListener = true;
            break;
          case CONNECTED:
            if (listener != null) {
              listener.onSuccess(null);
              unregisterListener = true;
            }
            break;
          case CONNECTION_FAILED:
            if (listener != null) {
              listener.onFailure(MMX.FailureCode.DEVICE_CONNECTION_FAILED, null);
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

      public void onPubsubItemReceived(MMXClient client, MMXTopic topic,
                                       com.magnet.mmx.client.common.MMXMessage message) {

      }

      public void onErrorReceived(MMXClient client, MMXErrorMessage error) {

      }
    });
    sInstance.mCurrentUser = null;
    getMMXClient().connectAnonymous(MMX.getGlobalListener(),
            new MMXClient.ConnectionOptions());
  }

  /**
   * Stop sending/receiving messages.
   */
  public static void logout(final MMX.OnFinishedListener<Void> listener) {
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
  public static boolean registerListener(OnMessageReceivedListener listener) {
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
  public static boolean unregisterListener(OnMessageReceivedListener listener) {
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
      Iterator<OnMessageReceivedListener> listeners = sInstance.mListeners.descendingIterator();
      while (listeners.hasNext()) {
        OnMessageReceivedListener listener = listeners.next();
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
      Iterator<OnMessageReceivedListener> listeners = sInstance.mListeners.descendingIterator();
      while (listeners.hasNext()) {
        OnMessageReceivedListener listener = listeners.next();
        try {
          if (listener.onMessageAcknowledgementReceived(from, originalMessageId)) {
            //listener returning true means consume the message
            break;
          }
        } catch (Exception ex) {
          Log.d(TAG, "notifyMessageAcknowledged(): Caught exception while calling listener: " + listener, ex);
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

  abstract static class MMXStatusTask extends MMXTask<MMXStatus> {
    private final OnFinishedListener<Void> mListener;

    MMXStatusTask(OnFinishedListener<Void> listener) {
      super(getMMXClient(), getHandler());
      mListener = listener;
    }

    @Override
    public abstract MMXStatus doRun(MMXClient mmxClient) throws Throwable;

    @Override
    public final void onException(Throwable exception) {
      if (mListener != null) {
        mListener.onFailure(MMX.FailureCode.SERVER_EXCEPTION, exception);
      }
    }

    @Override
    public final void onResult(MMXStatus result) {
      if (mListener != null) {
        if (result.getCode() == MMXStatus.SUCCESS) {
          mListener.onSuccess(null);
        } else {
          mListener.onFailure(MMX.FailureCode.SERVER_BAD_STATUS, new Exception(result.toString()));
        }
      }
    }
  }
}
