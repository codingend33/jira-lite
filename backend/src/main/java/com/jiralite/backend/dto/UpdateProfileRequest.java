package com.jiralite.backend.dto;

import jakarta.validation.constraints.Size;

public class UpdateProfileRequest {
    @Size(max = 255)
    private String displayName;

    @Size(max = 512)
    private String avatarS3Key;

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
}
