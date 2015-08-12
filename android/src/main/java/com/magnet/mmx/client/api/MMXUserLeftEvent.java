package com.magnet.mmx.client.api;

/**
 *  An event indicating that a user has left a channel
 */
public class MMXUserLeftEvent extends MMXChannelUserEvent {
    public MMXUserLeftEvent(MMXUser user, MMXChannel channel) {
        super(user, channel);
    }
}
