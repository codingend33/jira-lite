package com.jiralite.backend.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public class UserProfileResponse {
    private UUID id;
    private String email;
    private String displayName;
    private String avatarS3Key;
    private OffsetDateTime lastLoginAt;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getAvatarS3Key() {
        return avatarS3Key;
    }

    public void setAvatarS3Key(String avatarS3Key) {
        this.avatarS3Key = avatarS3Key;
    }

    public OffsetDateTime getLastLoginAt() {
        return lastLoginAt;
    }

    public void setLastLoginAt(OffsetDateTime lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
    }
}
