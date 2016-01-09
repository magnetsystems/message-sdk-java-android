/**
 * Copyright (c) 2012-2016 Magnet Systems. All rights reserved.
 */
package com.magnet.mmx.client.api;

import java.util.Map;
import org.assertj.core.data.MapEntry;

import static junit.framework.Assert.assertNotNull;
import static org.assertj.core.api.Assertions.assertThat;

public class AssertionUtils {

  public static <K, V> void assertMap(Map<K, V> expected, Map<K, V> actual) {
    assertNotNull(actual);
    MapEntry[] expectedEntries = new MapEntry[expected.size()];
    int i = 0;
    for(Map.Entry e : expected.entrySet()) {
      expectedEntries[i++] = MapEntry.entry(e.getKey(), e.getValue());
    }

    assertThat(actual).containsOnly(expectedEntries);
  }

}
