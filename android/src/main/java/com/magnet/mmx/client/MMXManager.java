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

import android.os.Handler;

/**
 * Base class for the android-specific managers.
 */
abstract public class MMXManager {
  private MMXClient mMMXClient;
  private Handler mHandler;
  private boolean mDestroyed;

  protected MMXManager(MMXClient mmxClient, Handler handler) {
    if (mmxClient == null) {
      throw new IllegalArgumentException("MMXClient cannot be null.");
    }
    if (handler == null) {
      throw new IllegalArgumentException("Handler cannot be null.");
    }
    mMMXClient = mmxClient;
    mHandler = handler;
  }

  /**
   * Retrieves the MMXClient associated with this manager.
   *
   * @return the MMXClient associated with this manager
   */
  protected final MMXClient getMMXClient() {
    return mMMXClient;
  }

  /**
   * Retrieves the Handler associated with this manager.
   *
   * @return the Handler associated with this manager
   */
  protected final Handler getHandler() { return mHandler; }

  /**
   * Called when MMXClient is disconnected.  Any clean-up work performed by the
   * manager should be done here because this class will no longer be referenced by the
   * client.  It also may be useful to protect against any further actions by checking the
   * isDestroyed() method before calling just in case the application is misusing any remaining
   * references.
   */
  protected void destroy() {
    mDestroyed = true;
  }

  protected final boolean isDestroyed() {
    return mDestroyed;
  }

  /**
   * Can be called at the beginning of any method.  Will throw an IllegalStateException
   * if the manager has already been destroyed.
   *
   * @throws IllegalStateException
   */
  protected final void checkDestroyed() throws IllegalStateException {
    if (isDestroyed()) {
      throw new IllegalStateException("Cannot perform this action because the manager has been destroyed.");
    }
  }

  /**
   * Called when the client's connection has been changed.
   */
  abstract void onConnectionChanged();

}
