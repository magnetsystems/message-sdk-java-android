/**
 * Copyright (c) 2012-2015 Magnet Systems. All rights reserved.
 */
package com.magnet.mmx.client.api;

/**
 * The match criteria to look up channel by subscribers
 */
public enum ChannelMatchType {
  /**
   * Match any one in the provided user list
   */
  ANY_MATCH,
  /**
   * Match exactly in the provided user list
   */
  EXACT_MATCH,
  /**
   * Match some in the provided user list
   */
  SUBSET_MATCH
}
