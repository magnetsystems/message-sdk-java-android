package com.magnet.mmx.client.api;

/**
 *
 */
public interface MMXInviteListener {

    /**
     * Invoked when an invites are received
     *
     * @param invite The invite that is received {@link MMXInvite}
     */
    void onInviteReceived(MMXInvite invite);

    /**
     * Invoked when a response to an invite is received {@link MMXInviteResponse}
     *
     * @param inviteResponse
     */
    void onInviteResponseReceived(MMXInviteResponse inviteResponse);
}
