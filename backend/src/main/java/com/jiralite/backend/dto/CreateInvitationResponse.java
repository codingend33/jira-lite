package com.jiralite.backend.dto;

/**
 * Response DTO for invitation creation.
 */
public class CreateInvitationResponse {

    private String token;
    private String invitationUrl;
    private String message;

    // Constructors
    public CreateInvitationResponse() {
    }

    public CreateInvitationResponse(String token, String invitationUrl, String message) {
        this.token = token;
        this.invitationUrl = invitationUrl;
        this.message = message;
    }

    // Getters and Setters
    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getInvitationUrl() {
        return invitationUrl;
    }

    public void setInvitationUrl(String invitationUrl) {
        this.invitationUrl = invitationUrl;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
