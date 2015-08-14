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

import java.util.Iterator;
import java.util.LinkedList;

/**
 * The main entry point for Magnet Message.
 */
public final class MagnetMessage {
  public enum FailureCode {
    DEVICE_ERROR,
    DEVICE_CONNECTION_FAILED,
    SERVER_AUTH_FAILED,
    SERVER_BAD_STATUS,
    SERVER_EXCEPTION
  }
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

  private static final String TAG = MagnetMessage.class.getSimpleName();
  private Context mContext = null;
  private MMXClient mClient = null;
  private HandlerThread mHandlerThread = null;
  private Handler mHandler = null;
  private static MagnetMessage sInstance = null;

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
  };

  private MagnetMessage(Context context, MMXClientConfig config) {
    mContext = context.getApplicationContext();
    mClient = MMXClient.getInstance(context, config);
    mHandlerThread = new HandlerThread("MagnetMessage");
    mHandlerThread.start();
    mHandler = new Handler(mHandlerThread.getLooper());
  }

  private void start(final OnFinishedListener<Void> listener) {
    if (!mClient.isConnected()) {
      if (listener != null) {
        mGlobalListener.registerListener(new MMXClient.MMXListener() {
          public void onConnectionEvent(MMXClient client, MMXClient.ConnectionEvent event) {
            Boolean result = null;
            FailureCode failureCode = null;
            switch (event) {
              case CONNECTED:
                result = Boolean.TRUE;
                break;
              case CONNECTION_FAILED:
                result = Boolean.FALSE;
                failureCode = FailureCode.DEVICE_CONNECTION_FAILED;
                break;
              case AUTHENTICATION_FAILURE:
                result = Boolean.FALSE;
                failureCode = FailureCode.SERVER_AUTH_FAILED;
                break;
              case DISCONNECTED:
                result = Boolean.FALSE;
                failureCode = FailureCode.DEVICE_CONNECTION_FAILED;
                break;
            }
            if (result != null) {
              if (result) {
                listener.onSuccess(null);
              } else {
                listener.onFailure(failureCode, null);
              }
              mGlobalListener.unregisterListener(this);
            }
          }

          public void onMessageReceived(MMXClient client, com.magnet.mmx.client.common.MMXMessage message, String receiptId) {
          }

          public void onSendFailed(MMXClient client, String messageId) {
          }

          public void onMessageDelivered(MMXClient client, MMXid recipient, String messageId) {
          }

          public void onPubsubItemReceived(MMXClient client, MMXTopic topic, com.magnet.mmx.client.common.MMXMessage message) {
          }

          public void onErrorReceived(MMXClient client, MMXErrorMessage error) {
          }
        });
      }
      mClient.connectAnonymous(mGlobalListener, new MMXClient.ConnectionOptions());
    } else {
      if (listener != null) {
        listener.onSuccess(null);
      }
    }
  }

  private void stop(OnFinishedListener<Void> listener) {
    if (mClient.isConnected()) {
      mClient.disconnect();
    } else {
      if (listener != null) {
        listener.onSuccess(null);
      }
    }
  }

  /**
   * Init the MagnetMessage API.
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
   * @param context the Android context
   * @param config the MMXClientConfig
   */
  static synchronized void init(Context context, MMXClientConfig config) {
    if (sInstance == null) {
      sInstance = new MagnetMessage(context, config);
    }
  }

  /**
   * Starts this MagnetMessage session.
   */
  public static synchronized void startSession(OnFinishedListener<Void> listener) {
    checkState();
    sInstance.start(listener);
  }

  /**
   * Ends this MagnetMessage session.
   */
  public static synchronized void endSession(OnFinishedListener<Void> listener) {
    checkState();
    sInstance.stop(listener);
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
        mListener.onFailure(MagnetMessage.FailureCode.SERVER_EXCEPTION, exception);
      }
    }

    @Override
    public final void onResult(MMXStatus result) {
      if (mListener != null) {
        if (result.getCode() == MMXStatus.SUCCESS) {
          mListener.onSuccess(null);
        } else {
          mListener.onFailure(MagnetMessage.FailureCode.SERVER_BAD_STATUS, new Exception(result.toString()));
        }
      }
    }

  }
}
