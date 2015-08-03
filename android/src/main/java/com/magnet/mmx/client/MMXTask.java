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
import android.os.HandlerThread;

import com.magnet.mmx.client.common.Log;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Convenience class for executing asynchronous MMXManager calls.
 *
 * @param <T> the return type of the desired method call
 */
public class MMXTask<T> implements Runnable {
  private static final String TAG = MMXTask.class.getSimpleName();
  private static final int MAX_CALL_WAIT_TIME = 20000; //20 seconds
  private T mResult = null;
  private Throwable mException = null;
  private final AtomicBoolean mResultReceived = new AtomicBoolean(false);
  private MMXClient mClient = null;
  private HandlerThread mHandlerThread = null;
  private Handler mHandler = null;

  /**
   * Will execute the task on the specified handler.
   */
  public MMXTask(MMXClient mmxClient, Handler handler) {
    if (mmxClient == null) {
      throw new IllegalArgumentException("MMXClient cannot be null.");
    }
    mClient = mmxClient;
    if (handler != null) {
      mHandler = handler;
    } else {
      mHandlerThread = new HandlerThread("MMXTaskThread");
      mHandlerThread.start();
      mHandler = new Handler(mHandlerThread.getLooper());
    }
  }

  /**
   * Will execute this task on a new HandlerThread.
   */
  public MMXTask(MMXClient mmxClient) {
    this(mmxClient, null);
  }

  /**
   * This method may be called to remake the synchronous call that was made asynchronous
   * back to being a synchronous call, which currently doesn't make sense, so it's private
   * for now.
   *
   * @return the result of the doRun() method
   * @throws MobileRuntimeException
   */
  final T get() throws Throwable {
    synchronized (mResultReceived) {
      if (!mResultReceived.get()) {
        try {
          Log.d(TAG, "get() waiting for the result");
          mResultReceived.wait(MAX_CALL_WAIT_TIME);
        } catch (InterruptedException e) {
          Log.e(TAG, "get(): caught exception", e);
          mException = e;
        }
      }
    }
    if (mException != null) {
      throw mException;
    }
    return mResult;
  }

  /**
   * Override this method to handle a result from the doRun() method.
   * The default implementation is a no-op.
   *
   * @param result the result returned from the doRun() method
   */
  public void onResult(T result) {
    //default implementation does nothing
    Log.w(TAG, "onResult(): DEFAULT IMPLEMENTATION WAS NOT OVERRIDDEN");
  }

  /**
   * Override this method to handle an exception from the doRun() method.
   * The default implementation is a no-op.
   *
   * @param exception the result returned from the doRun() method
   */
  public void onException(Throwable exception) {
    //default implementation does nothing
    Log.w(TAG, "onException(): DEFAULT IMPLEMENTATION WAS NOT OVERRIDDEN", exception);
  }

  private void setResult(T result) {
    synchronized (mResultReceived) {
      mResult = result;
      mResultReceived.set(true);
      mResultReceived.notifyAll();
    }
    try {
      onResult(result);
    } catch (Exception ex) {
      Log.e(TAG, "Exception caught.", ex);
    }
  }

  private void setException(Throwable exception) {
    synchronized (mResultReceived) {
      mException = exception;
      mResultReceived.set(true);
      mResultReceived.notifyAll();
    }
    try {
      onException(exception);
    } catch (Exception ex) {
      Log.e(TAG, "Exception caught.", ex);
    }
  }

  /**
   * Override this method to perform the desired call.  When this method returns,
   * the onResult() OR onException() methods will be called.
   *
   * @param mmxClient the MMXClient instance associated with this task
   * @return the return value type that was specified when constructing this task
   * @throws Exception
   */
  public T doRun(MMXClient mmxClient) throws Throwable {
    return null;
  }

  /**
   * The actual Runnable.run() implementation wraps the doRun() task.
   */
  public final void run() {
    try {
      setResult(doRun(mClient));
    } catch (Throwable ex) {
      setException(ex);
    }
  }

  /**
   * This will actually execute the task.
   */
  public final void execute() {
    mHandler.post(this);
  }

  /**
   * Attempt to cancel this task.  The task may
   * still be executed if it's currently in progress.
   */
  public final void cancel() {
    mHandler.removeCallbacks(this);
  }

  protected void finalize() throws Throwable {
    if (mHandlerThread != null) {
      mHandlerThread.quit();
    }
  }
}
