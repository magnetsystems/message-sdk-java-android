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

import android.util.Log;

import com.magnet.mmx.client.common.MMXErrorMessage;
import com.magnet.mmx.client.common.MMXMessage;
import com.magnet.mmx.client.common.MMXid;
import com.magnet.mmx.protocol.MMXTopic;

import java.util.ArrayList;

/**
 * This abstract class is a helper class intended to be used with MMXClient.  The implementation
 * of this class is most likely a Singleton and should be used accordingly.  This is because MMXClient
 * enforces the need for a listener (to make sure the application handles any delivered messages)
 * and only allows one listener.
 *
 * This convenience class allows for individual, multiple activities to register and unregister as listeners
 * in onCreate/onDestroy and received the dispatched messages.
 */
abstract public class AbstractMMXListener implements MMXClient.MMXListener {
  private static final String TAG = AbstractMMXListener.class.getSimpleName();
  private final ArrayList<MMXClient.MMXListener> mListeners = new ArrayList<MMXClient.MMXListener>();

  /**
   * Implements the MMXListener method and dispatches the call to any registered listeners.
   * The listeners will be called in the order registered.
   *
   * @param mmxClient the MMXClient instance
   * @param connectionEvent the ConnectionEvent
   */
  public void onConnectionEvent(MMXClient mmxClient, MMXClient.ConnectionEvent connectionEvent) {
    Log.d(TAG, "onConnectionEvent(): start.  event=" + connectionEvent);
    synchronized (mListeners) {
      for (MMXClient.MMXListener listener : mListeners) {
        try {
          listener.onConnectionEvent(mmxClient, connectionEvent);
        } catch (Throwable throwable) {
          Log.e(TAG, "onConnectionEvent(): caught throwable from listener: " + listener, throwable);
        }
      }
    }
  }

  /**
   * Implements the MMXListener method and dispatches the call to any registered listeners.
   * The listeners will be called in the order registered.  This calls handleMessageReceived() BEFORE
   * any registered listeners are called.
   *
   * @param mmxClient the MMXClient instance
   * @param mmxMessage the received message
   * @param receiptId A delivery receipt ID or null if receipt was not requested
   */
  public void onMessageReceived(MMXClient mmxClient, MMXMessage mmxMessage, String receiptId) {
    Log.d(TAG, "onMessageReceived(): start. ");
    try {
      handleMessageReceived(mmxClient, mmxMessage, receiptId);
    } catch (Throwable throwable) {
      Log.e(TAG, "onMessageReceived(): caught throwable calling handleMessageReceived()", throwable);
    }
    synchronized (mListeners) {
      for (MMXClient.MMXListener listener : mListeners) {
        try {
          listener.onMessageReceived(mmxClient, mmxMessage, receiptId);
        } catch (Throwable throwable) {
          Log.e(TAG, "onMessageReceived(): caught throwable from listener: " + listener, throwable);
        }
      }
    }
  }

  /**
   * Called by onMessageReceived prior to any registered listeners getting called.  This method should
   * handle any persistence of messages required as there may not be any currently registered listeners.
   *
   * For example, if the application is not running but is woken up by a GCM message, upon connecting,
   * messages will be retrieved and this method will be called.
   *
   * @param mmxClient the MMXClient instance
   * @param mmxMessage the received message
   * @param receiptId A delivery receipt ID or null if receipt was not requested
   */
  abstract public void handleMessageReceived(MMXClient mmxClient, MMXMessage mmxMessage, String receiptId);

  /**
   * Implements the MMXListener method and dispatches the call to any registered listeners.
   * The listeners will be called in the order registered.
   *
   * @param mmxClient the MMXClient instance
   * @param messageId The id of the message that failed
   */
  public void onSendFailed(MMXClient mmxClient, String messageId) {
    Log.d(TAG, "onSendFailed(): start.  ");
    synchronized (mListeners) {
      for (MMXClient.MMXListener listener : mListeners) {
        try {
          listener.onSendFailed(mmxClient, messageId);
        } catch (Throwable throwable) {
          Log.e(TAG, "onSendFailed(): caught throwable from listener: " + listener, throwable);
        }
      }
    }
  }

  /**
   * Implements the MMXListener method and dispatches the call to any registered listeners.
   * The listeners will be called in the order registered.
   *
   * @param mmxClient the MMXClient instance
   * @param recipient the recipient of the originally sent message (who has acknowledged receipt of the message)
   * @param messageId The id of the message for which the receipt was returned
   */
  public void onMessageDelivered(MMXClient mmxClient, MMXid recipient, String messageId) {
    Log.d(TAG, "onMessageDelivered(): messageId=" + messageId);
    try {
      handleMessageDelivered(mmxClient, recipient, messageId);
    } catch (Throwable throwable) {
      Log.e(TAG, "onMessageReceived(): caught throwable calling handleMessageDelivered()", throwable);
    }
    synchronized (mListeners) {
      for (MMXClient.MMXListener listener : mListeners) {
        try {
          listener.onMessageDelivered(mmxClient, recipient, messageId);
        } catch (Throwable throwable) {
          Log.e(TAG, "onMessageDelivered(): caught throwable from listener: " + listener, throwable);
        }
      }
    }
  }

  /**
   * Called by onMessageDelivered before any of the listeners are processed.
   *
   * @param mmxClient the MMXClient instance
   * @param recipient the recipient of the originally sent message (who has acknowledged receipt of the message)
   * @param messageId the id of the original message
   */
  public abstract void handleMessageDelivered(MMXClient mmxClient, MMXid recipient, String messageId);

  /**
   * Implements the MMXListener method and dispatches the call to any registered listeners.
   * The listeners will be called in the order registered.  This implementation calls
   * handlePubsubItemReceived() BEFORE calling any of the registered listeners.
   *
   * @param mmxClient the MMXClient instance
   * @param mmxTopic the topic of the published message
   * @param mmxMessage the published message
   */
  public void onPubsubItemReceived(MMXClient mmxClient, MMXTopic mmxTopic, MMXMessage mmxMessage) {
    Log.d(TAG, "onPubsubItemReceived(): topic=" + mmxTopic);
    try {
      handlePubsubItemReceived(mmxClient, mmxTopic, mmxMessage);
    } catch (Throwable throwable) {
      Log.e(TAG, "onPubsubItemReceived(): caught throwable calling handlePubsubItemReceived()", throwable);
    }
    synchronized (mListeners) {
      for (MMXClient.MMXListener listener : mListeners) {
        try {
          listener.onPubsubItemReceived(mmxClient, mmxTopic, mmxMessage);
        } catch (Throwable throwable) {
          Log.e(TAG, "onPubsubItemReceived(): caught throwable from listener: " + listener, throwable);
        }
      }
    }
  }

  /**
   * Called by onPubsubItemReceived before any of the listeners are processed.
   *
   * @param mmxClient the MMXClient instance
   * @param mmxTopic the topic associated with this item
   * @param mmxMessage the actual published message
   */
  abstract public void handlePubsubItemReceived(MMXClient mmxClient, MMXTopic mmxTopic, MMXMessage mmxMessage);

  /**
   * Implements the MMXListener method and dispatches the call to any registered listeners.
   * The listeners will be called in the order registered.
   *
   * @param mmxClient the MMXClient instance
   * @param error The error message
   */
  public void onErrorReceived(MMXClient mmxClient, MMXErrorMessage error) {
    Log.d(TAG, "onErrorReceived(): start.  ");
    synchronized (mListeners) {
      for (MMXClient.MMXListener listener : mListeners) {
        try {
          listener.onErrorReceived(mmxClient, error);
        } catch (Throwable throwable) {
          Log.e(TAG, "onErrorReceived(): caught throwable from listener: " + listener, throwable);
        }
      }
    }
  }

  /**
   * Registers a listener.  The callbacks will be dispatched to the registered listeners
   * in the order they were registered.  Be sure to call unregisterListener to prevent memory leaks.
   *
   * @param listener a listener to register
   */
  public final void registerListener(MMXClient.MMXListener listener) {
    synchronized (mListeners) {
      MMXClient.MMXListener existingListener = null;
      for (MMXClient.MMXListener curListener : mListeners) {
        if (curListener == listener) {
          existingListener = listener;
          break;
        }
      }
      if (existingListener == null) {
        mListeners.add(0, listener);
      }
    }
  }

  /**
   * Unregisters a listener.
   *
   * @param listener the listener to unregister
   */
  public final void unregisterListener(MMXClient.MMXListener listener) {
    synchronized (mListeners) {
      for (int i=mListeners.size(); --i >= 0;) {
        if (mListeners.get(i) == listener) {
          mListeners.remove(i);
          break;
        }
      }
    }
  }
}
