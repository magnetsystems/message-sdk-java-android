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

import com.magnet.mmx.protocol.MMXTopicId;

/**
 * A global topic under the application.
 */
public class MMXGlobalTopic extends MMXTopicId implements MMXVisibleTopic {
  private static final long serialVersionUID = -8128460100986420099L;

  /**
   * Constructor with the topic name.
   * @param topic A topic name.
   */
  public MMXGlobalTopic(String topic) {
    super(null, topic);
  }

  /**
   * @hide
   * Determine if this topic is under the user name-space.
   * @return Always false.
   */
  @Override
  public boolean isUserTopic() {
    return false;
  }
}
