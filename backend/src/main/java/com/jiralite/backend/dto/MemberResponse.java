package com.jiralite.backend.dto;

import java.util.UUID;

/**
 * Org member response DTO.
 */
public record MemberResponse(
        UUID userId,
        String email,
        String displayName,
        String role,
        String status,
        String avatarUrl) {
}
