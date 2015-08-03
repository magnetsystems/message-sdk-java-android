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

package com.magnet.mmx.util;

import java.util.LinkedList;
import java.util.Queue;

/**
 * @hide
 * A thread with a queue.
 */
public class QueueExecutor extends Thread {
  private boolean mDone;
  private Queue<Runnable> mQueue = new LinkedList<Runnable>();
  
  /**
   * Default constructor.
   * @param name The thread name.
   * @param isDaemon true for a daemon thread.
   */
  public QueueExecutor(String name, boolean isDaemon) {
    super(name);
    this.setDaemon(isDaemon);
  }
  
  /**
   * Post a task to the queue for execution.
   * @param task
   */
  public void post(Runnable task) {
    synchronized(mQueue) {
      mQueue.offer(task);
      mQueue.notify();
    }
  }
  
  /**
   * Quit this thread.  Any pending tasks will be lost.
   */
  public void quit() {
    synchronized(mQueue) {
      mDone = true;
      mQueue.notify();
    }
  }
  
  /**
   * The main loop.
   */
  @Override
  final public void run() {
    Runnable task;
    do {
      try {
        while ((task = mQueue.poll()) != null) {
          task.run();
        }
        synchronized(mQueue) {
          if (mDone) {
            return;
          }
          mQueue.wait();
        }
      } catch (Throwable e) {
        e.printStackTrace();
      }
    } while (!mDone);
  }
}
