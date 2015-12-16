/**
 * Copyright (c) 2012-2015 Magnet Systems. All rights reserved.
 */
package com.magnet.mmx.client.api;

import com.magnet.max.android.User;
import java.util.List;

public class ChannelSummary {
  private MMXChannel channel;
  private List<User> subscribers;
  private List<MMXMessage> messages;
  private int numberOfSubscribers;
  private User owner;

  public MMXChannel getChannel() {
    return channel;
  }

  public List<User> getSubscribers() {
    return subscribers;
  }

  public List<MMXMessage> getMessages() {
    return messages;
  }

  public int getNumberOfSubscribers() {
    return numberOfSubscribers;
  }

  public User getOwner() {
    return owner;
  }
}
