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

/**
 * The exception is thrown when the operation is denied due to insufficient
 * privilege.
 */
public class TopicPermissionException extends MMXException {
  /**
   * The default constructor.
   * @param msg A message.
   */
  public TopicPermissionException(String msg) {
    super(msg, FORBIDDEN);
  }
}
