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
