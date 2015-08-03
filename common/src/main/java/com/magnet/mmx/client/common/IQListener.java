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
package com.magnet.mmx.client.common;

import com.magnet.mmx.protocol.MMXStatus;

/**
 * Listener for the IQ MMX result or error.
 *
 * @param <Result>
 */
public interface IQListener<Result> {
  /**
   * IQ result is received.
   * @param result
   */
  public void onReceived(Result result);
  /**
   * IQ error with MMX extension.
   * @param cmd
   * @param status
   */
  public void onError(String cmd, MMXStatus status);
  /**
   * Standard IQ error.
   * @param xml
   */
  public void onError(String xml);
}
