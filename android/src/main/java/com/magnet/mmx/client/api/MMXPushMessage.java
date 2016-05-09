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

import com.magnet.max.android.ApiCallback;
import com.magnet.max.android.ApiError;
import java.util.HashMap;
import java.util.Map;

import com.magnet.max.android.User;
import com.magnet.mmx.client.MMXClient;
import com.magnet.mmx.client.MMXTask;
import com.magnet.mmx.client.common.Log;
import com.magnet.mmx.client.common.MMXException;
import com.magnet.mmx.protocol.MMXid;
import com.magnet.mmx.protocol.PushResult;

/**
 * This class holds the message payload, and operations for the message.  If
 * the message targets to the recipients, it will be used for ad hoc messaging.
 * If the message targets to a channel, it will be used for group chat or forum
 * discussions.
 */
public class MMXPushMessage {
  private static final String TAG = MMXPushMessage.class.getSimpleName();

  /**
   * Failure codes for the MMXPushMessage class.
   */
  public static final ApiError INVALID_RECIPIENT = new ApiError(404, "INVALID_RECIPIENT");
  public static final ApiError CONTENT_TOO_LARGE = new ApiError(413, "CONTENT_TOO_LARGE");

  /**
   * The builder for the MMXPushMessage class
   */
  public static final class Builder {
    private MMXPushMessage mMessage;

    public Builder() {
      mMessage = new MMXPushMessage();
    }

    /**
     * Set the message type for the MMXPushMessage object.
     *
     * @param type the message type
     * @return this Builder instance
     */
    public MMXPushMessage.Builder type(String type) {
      mMessage.type(type);
      return this;
    }

    /**
     * Set the set of recipients for the MMXPushMessage
     *
     * @param recipient the recipient
     * @return this Builder instance
     */
    public MMXPushMessage.Builder recipient(User recipient) {
      mMessage.recipient(recipient);
      return this;
    }

    /**
     * Sets the content for the MMXPushMessage
     * NOTE:  The values in the map will be flattened to their toString() representations.
     *
     * @param content the content
     * @return this Builder instance
     */
    public MMXPushMessage.Builder content(Map<String, Object> content) {
      mMessage.content(content);
      return this;
    }

    /**
     * Validate and builds the MMXPushMessage
     *
     * @return the MMXPushMessage
     * @throws IllegalArgmentException
     */
    public MMXPushMessage build() {
      //validate message
      if (mMessage.mRecipient == null) {
        throw new IllegalArgumentException("No recipient is specified");
      }
      return mMessage;
    }
  }
  
//  /**
//   * The exception contains a list of recipient user ID's that a message
//   * cannot be sent to.
//   */
//  public static class InvalidRecipientException extends MMXException {
//    private Set<String> mUserIds = new HashSet<String>();
//    
//    public InvalidRecipientException(String msg) {
//      super(msg, StatusCode.NOT_FOUND);
//    }
//    
//    private void addUserId(String userId) {
//      mUserIds.add(userId);
//    }
//    
//    public Set<String> getUserIds() {
//      return mUserIds;
//    }
//    
//    public String toString() {
//      return super.toString()+", uids="+mUserIds;
//    }
//  }

  private String mType;
  private User mRecipient;
  private Map<String, Object> mContent = new HashMap<String, Object>();

  /**
   * Default constructor
   */
  MMXPushMessage() {
  }

  /**
   * Set the message type for this MMXPushMessage object.
   *
   * @param type the type
   * @return this MMXPushMessage object
   */
  MMXPushMessage type(String type) {
    mType = type;
    return this;
  }

  /**
   * The message type for this MMXPushMessage
   *
   * @return the message type
   */
  String getType() {
    return mType;
  }

  /**
   * Set the recipient
   *
   * @param recipients the recipients
   * @return this MMXPushMessage object
   */
  MMXPushMessage recipient(User recipient) {
    mRecipient = recipient;
    return this;
  }

  /**
   * The recipient for this message
   *
   * @return the recipients
   */
  public User getRecipient() {
    return mRecipient;
  }

  /**
   * Sets the content for this message
   *
   * @param content the content
   * @return this MMXPushMessage instance
   */
  MMXPushMessage content(Map<String, Object> content) {
    mContent = content;
    return this;
  }

  /**
   * The content for this message
   *
   * @return the content
   */
  public Map<String, Object> getContent() {
    return mContent;
  }

  /**
   * Send the current message to server and deliver via a native push mechanism.
   * The {@link ApiCallback#success(Object)} will be called
   * with the push result for all active devices of the recipient.  If there are
   * any failure of sending the message to the server, the
   * {@link ApiCallback#failure(FailureCode, Throwable)} will be
   * invoked.  Common failure codes are {@link FailureCode#BAD_REQUEST}, or
   * FailureCode#DEVICE_ERROR.
   *
   * @param listener the listener for this method call
   */
  public void send(final ApiCallback<PushResult> listener) {
    if (MMX.getCurrentUser() == null) {
      //FIXME:  This needs to be done in MMXClient/MMXPushManager.  Do it here for now.
      final Throwable exception = new IllegalStateException("Cannot send message.  " +
              "There is no current user.  Please login() first.");
      if (listener == null) {
        Log.w(TAG, "send() failed", exception);
      } else {
        MMX.getCallbackHandler().post(new Runnable() {
          public void run() {
            listener.failure(new ApiError(MMX.BAD_REQUEST_CODE, null, exception));
          }
        });
      }
      return;
    }
    final String type = getType() != null ? getType() : null;
    MMXTask<PushResult> task = new MMXTask<PushResult>(MMX.getMMXClient(),
        MMX.getHandler()) {
      @Override
      public PushResult doRun(MMXClient mmxClient) throws Throwable {
        MMXid target = new MMXid(mRecipient.getUserIdentifier(), null,
                                 mRecipient.getUserName());
        PushResult result = mmxClient.getPushManager().sendPayload(target,
            mType, mContent);
        return result;
      }

      @Override
      public void onException(final Throwable exception) {
        if (listener != null) {
          MMX.getCallbackHandler().post(new Runnable() {
            public void run() {
              listener.failure(new ApiError(exception));
            }
          });
        }
      }

      @Override
      public void onResult(final PushResult result) {
        if (listener != null) {
          MMX.getCallbackHandler().post(new Runnable() {
            public void run() {
              listener.success(result);
            }
          });
        }
      }
    };
    task.execute();
  }
}
