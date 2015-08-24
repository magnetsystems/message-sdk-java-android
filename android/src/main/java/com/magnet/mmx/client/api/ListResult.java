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

import java.util.Collections;
import java.util.List;

/**
 * The results of certain operations that return lists of objects.
 */
public class ListResult<T> {
  /**
   * The total count of results
   */
  public final int totalCount;

  /**
   * The result channels
   */
  public final List<T> items;

  ListResult(int totalCount, List<T> items) {
    this.totalCount = totalCount;
    this.items = Collections.unmodifiableList(items);
  }
}
