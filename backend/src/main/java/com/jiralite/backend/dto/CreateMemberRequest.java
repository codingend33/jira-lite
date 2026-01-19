package com.jiralite.backend.dto;

import java.util.UUID;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;

/**
 * Request to add an org member (user must already exist).
 */
public class CreateMemberRequest {

    private UUID userId;

    @Email
    private String email;

    @Pattern(regexp = "ADMIN|MEMBER", message = "role must be ADMIN or MEMBER")
    private String role;

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}
