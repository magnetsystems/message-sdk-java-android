package com.magnet.mmx.client.api;

import android.content.Context;

import com.magnet.mmx.client.AbstractMMXListener;
import com.magnet.mmx.client.MMXClient;
import com.magnet.mmx.client.common.*;
import com.magnet.mmx.protocol.MMXTopic;

import java.util.Iterator;
import java.util.LinkedList;

/**
 * The main entry point for Magnet Message.
 */
public final class MagnetMessage {
  public enum FailureCode {DEVICE_ERROR, SERVER_ERROR, EXCEPTION}
  /**
   * The listener interface for handling incoming messages.
   */
  public interface OnMessageReceivedListener {
    /**
     * Invoked when incoming message is received.
     *
     * @param message the incoming message
     * @return true to consume this message, false for additional listeners to be called
     */
    public boolean onMessageReceived(MMXMessage message);
  }

  public interface OnFinishedListener<T> {
    public void onSuccess(T result);

    public void onFailure(FailureCode code, Exception ex);
  }

  private static final String TAG = MagnetMessage.class.getSimpleName();
  private Context mContext = null;
  private MMXClient mClient = null;
  private static MagnetMessage sInstance = null;
  /**
   * The listeners will be added in order (most recent at end)
   * They should be called in order from most recently registered (from the end)
   */
  private final LinkedList<OnMessageReceivedListener> mListeners = new LinkedList<OnMessageReceivedListener>();

  private AbstractMMXListener mGlobalListener = new AbstractMMXListener() {
    @Override
    public void handleMessageReceived(MMXClient mmxClient, com.magnet.mmx.client.common.MMXMessage mmxMessage, String receiptId) {

    }

    @Override
    public void handleMessageDelivered(MMXClient mmxClient, MMXid recipient, String messageId) {

    }

    @Override
    public void handlePubsubItemReceived(MMXClient mmxClient, MMXTopic mmxTopic, com.magnet.mmx.client.common.MMXMessage mmxMessage) {

    }

    @Override
    public void onConnectionEvent(MMXClient mmxClient, MMXClient.ConnectionEvent connectionEvent) {
      Log.d(TAG, "onConnectionEvent(): Event received: " + connectionEvent);
      super.onConnectionEvent(mmxClient, connectionEvent);
    }
  };

  private MagnetMessage(Context context, int configResId) {
    mContext = context.getApplicationContext();
    mClient = MMXClient.getInstance(context, configResId);
    mClient.connectAnonymous(mGlobalListener, new MMXClient.ConnectionOptions());
  }

  /**
   * Starts this MagnetMessage session.
   *
   * @param context the Android context
   * @param configResId the R.raw. resource id containing the configuration
   */
  public synchronized static void startSession(Context context, int configResId) {
    if (sInstance == null) {
      sInstance = new MagnetMessage(context, configResId);
    }
  }

  private synchronized static void checkSessionState() {
    if (sInstance == null) {
      throw new IllegalStateException("MagnetMessage.startSession() must be called prior to invoking" +
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
    checkSessionState();
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
    checkSessionState();
    if (listener == null) {
      throw new IllegalArgumentException("Listener cannot be null.");
    }
    synchronized (sInstance.mListeners) {
      return sInstance.mListeners.remove(listener);
    }
  }

  private static void notifyMessageReceived(MMXMessage message) {
    checkSessionState();
    synchronized (sInstance.mListeners) {
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

  static synchronized Context getContext() {
    checkSessionState();
    return sInstance.mContext;
  }

  static synchronized MMXClient getMMXClient() {
    checkSessionState();
    return sInstance.mClient;
  }

  static synchronized AbstractMMXListener getGlobalListener() {
    return sInstance.mGlobalListener;
  }

  static void connectWithCredentials(String username, byte[] password) {
    checkSessionState();
    sInstance.mClient.connectWithCredentials(username, password,
            sInstance.mGlobalListener, new MMXClient.ConnectionOptions());
  }
}
