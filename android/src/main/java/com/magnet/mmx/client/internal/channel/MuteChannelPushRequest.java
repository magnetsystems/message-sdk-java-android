/**
 * Copyright (c) 2012-2016 Magnet Systems. All rights reserved.
 */
package com.magnet.mmx.client.internal.channel;

import java.util.Date;

public class MuteChannelPushRequest {
  private Date untilDate;
  private String channelId;

  public MuteChannelPushRequest(String channelId, Date untilDate) {
    this.untilDate = untilDate;
    this.channelId = channelId;
  }

  public Date getUntilDate() {
    return untilDate;
  }

  public String getChannelId() {
    return channelId;
  }
}
