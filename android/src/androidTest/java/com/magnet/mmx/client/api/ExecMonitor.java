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

class ExecMonitor<S, F> {
  enum Status {
    INVOKED,
    FAILED,
    WAITING,
  }

  private S mRtnValue;
  private F mFailedValue;
  private ExecMonitor.Status mStatus = Status.WAITING;
  
  public void reset(S rtnValue, F failValue) {
    mRtnValue = rtnValue;
    mFailedValue = failValue;
    mStatus = Status.WAITING;
  }
  
  // Only be called if waitFor() returns EXECED
  public S getReturnValue() {
    return mRtnValue;
  }
  
  // Only be called if waitFor() returns FAILED
  public F getFailedValue() {
    return mFailedValue;
  }
  
  public synchronized void invoked(S value) {
    mStatus = Status.INVOKED;
    mRtnValue = value;
    notify();

  }
  
  public synchronized void failed(F value) {
    mStatus = Status.FAILED;
    mFailedValue = value;
    notify();
  }
  
  public synchronized ExecMonitor.Status waitFor(long timeout) {
    if (mStatus == Status.WAITING) {
      // Not executed yet, wait for result.
      try {
        wait(timeout);
      } catch (InterruptedException e) {
        // Ignored
      }
    }
    return mStatus;
  }
}