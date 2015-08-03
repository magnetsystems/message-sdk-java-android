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

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @hide
 * A queue with a thread pool.
 */
public class QueuePoolExecutor {
  private String mPrefix;
  private boolean mDaemon;
  private ThreadPoolExecutor mExecutor;

  private class QueueThreadFactory implements ThreadFactory {
    private int mIndex;

    @Override
    public Thread newThread(Runnable r) {
      Thread thread = new Thread(r, mPrefix+"-"+(++mIndex));
      thread.setDaemon(mDaemon);
      return thread;
    }
  }

  public QueuePoolExecutor(String prefix, boolean isDaemon, int maxPoolSize) {
    mPrefix = prefix;
    mDaemon = isDaemon;
    mExecutor = new ThreadPoolExecutor(1, maxPoolSize, 5L, TimeUnit.SECONDS,
        new LinkedBlockingQueue<Runnable>(), new QueueThreadFactory());
  }

  public void post(Runnable task) {
    mExecutor.execute(task);
  }

  public void quit() {
    mExecutor.shutdown();
  }
}
