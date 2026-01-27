package com.jiralite.backend.dto;

import java.util.UUID;

/**
 * Response DTO after successful organization creation.
 */
public class CreateOrganizationResponse {

    private UUID orgId;
    private String orgName;
    private String message;

    // Constructors
    public CreateOrganizationResponse() {
    }

    public CreateOrganizationResponse(UUID orgId, String orgName, String message) {
        this.orgId = orgId;
        this.orgName = orgName;
        this.message = message;
    }

    // Getters and Setters
    public UUID getOrgId() {
        return orgId;
    }

    public void setOrgId(UUID orgId) {
        this.orgId = orgId;
    }

    public String getOrgName() {
        return orgName;
    }

    public void setOrgName(String orgName) {
        this.orgName = orgName;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
