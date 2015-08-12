package com.magnet.mmx.client.api;

/**
 *  An event indicating that a user has joined a channel
 */
public class MMXUserJoinedEvent extends MMXChannelUserEvent {
    public MMXUserJoinedEvent(MMXUser user, MMXChannel channel) {
        super(user, channel);
    }
}
