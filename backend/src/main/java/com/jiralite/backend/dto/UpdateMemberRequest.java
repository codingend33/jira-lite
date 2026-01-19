package com.jiralite.backend.dto;

import jakarta.validation.constraints.Pattern;

/**
 * Request to update member role/status.
 */
public class UpdateMemberRequest {

    @Pattern(regexp = "ADMIN|MEMBER", message = "role must be ADMIN or MEMBER")
    private String role;

    @Pattern(regexp = "ACTIVE|INVITED|DISABLED", message = "status must be ACTIVE, INVITED, or DISABLED")
    private String status;

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
