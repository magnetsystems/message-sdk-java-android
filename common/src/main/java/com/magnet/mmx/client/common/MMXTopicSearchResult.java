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

import java.util.List;

/**
 * Result of the topic search.
 */
public class MMXTopicSearchResult {
  private int mTotal;
  private List<MMXTopicInfo> mResults;

  /**
   * @hide
   */
  public MMXTopicSearchResult(int total, List<MMXTopicInfo> results) {
    mTotal = total;
    mResults = results;
  }

  /**
   * Get the total count.
   * @return
   */
  public int getTotal() {
    return mTotal;
  }

  /**
   * Return a list of matching topic info.
   * @return
   */
  public List<MMXTopicInfo> getResults() {
    return mResults;
  }
}
