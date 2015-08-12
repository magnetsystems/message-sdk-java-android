package com.magnet.mmx.client.api;

/**
 * MMXInvite class representing an Invite for joining a MMXChannel
 */
public class MMXInvite {

    /**
     * State of the Invitation
     */

    enum State {
        /**
         * The target user has not yet responded to the invite
         */
        PENDING,

        /**
         * The target user has accepted the invite
         */
        ACCEPTED,

        /**
         * The target user has rejected the invite
         */
        REJECTED
    }

    private State state = State.PENDING;
    private MMXUser user;
    private MMXChannel channel;

    /**
     *
     * @param user The user to which invite will be sent
     * @param channel The channel for which user is to be invited
     */
    public MMXInvite(MMXUser user, MMXChannel channel) {
        this.user = user;
        this.channel = channel;
    }

    /**
     * Send the invite to the user for the channel
     *
     * @param listener Listener callback
     */
    public void send(MagnetMessage.OnFinishedListener<Boolean> listener) {
    }

    /**
     * Get the state of the invite
     *
     * @return The state of the Invite see {@link com.magnet.mmx.client.api.MMXInvite.State}
     */
    public State getState() {
        return state;
    }

    /**
     * Get the user to whom invite was sent
     *
     * @return  user to whom invite was sent
     */
    public MMXUser getUser() {
        return user;
    }

    /**
     * Get the channel for which this Invite was created
     *
     * @return user for which this Invite was created
     */
    public MMXChannel getChannel() {
        return channel;
    }
}
