package com.magnet.mmx.client.api;

/**
 * MMXInviteResponse class representing the response to an invite to join a private conversation
 */

public class MMXInviteResponse {
    enum Status {
        /**
         * status code indicating that the Invite has been accepted
         */
        ACCEPTED,

        /**
         * status code indicating that the Invite has been rejected
         */
        REJECTED,

        /**
         * undefined
         */

        UNKNOWN
    }

    private MMXInvite invite;
    private Status status;
    private String message;

    public MMXInviteResponse(MMXInvite invite, Status status, String message) {
        this.invite = invite;
        this.status = status;
        this.message = message;
    }

    public MMXInviteResponse(MMXInvite invite, Status status) {
        this.invite = invite;
        this.status = status;
    }

    /**
     *
     * @return The invite for which this response was generated
     */
    public MMXInvite getInvite() {
        return invite;
    }

    /**
     *
     * @return The status of the response {@link com.magnet.mmx.client.api.MMXInviteResponse.Status}
     */
    public Status getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }


    /**
     * Reply to the Inviter with the specified status and message
     */
    public void send() {
    }
}
