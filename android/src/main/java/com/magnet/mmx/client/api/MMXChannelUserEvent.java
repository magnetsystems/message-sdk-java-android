package com.magnet.mmx.client.api;

/**
 * Abstract class for Channel events associated with User's activity on a channel
 */
public abstract class MMXChannelUserEvent extends MMXChannelEvent {
    private MMXUser user;
    private MMXChannel channel;

    public MMXChannelUserEvent(MMXUser user, MMXChannel channel) {
        this.user = user;
        this.channel = channel;
    }

    public MMXUser getUser() {
        return user;
    }

    public MMXChannel getChannel() {
        return channel;
    }
}
