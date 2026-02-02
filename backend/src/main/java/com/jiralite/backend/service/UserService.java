package com.jiralite.backend.service;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jiralite.backend.audit.LogAudit;
import com.jiralite.backend.dto.ErrorCode;
import com.jiralite.backend.dto.UpdateProfileRequest;
import com.jiralite.backend.dto.AvatarPresignResponse;
import com.jiralite.backend.dto.UserProfileResponse;
import com.jiralite.backend.entity.UserEntity;
import com.jiralite.backend.exception.ApiException;
import com.jiralite.backend.repository.UserRepository;
import com.jiralite.backend.security.tenant.TenantContext;
import com.jiralite.backend.security.tenant.TenantContextHolder;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final S3PresignService s3PresignService;

    public UserService(UserRepository userRepository, S3PresignService s3PresignService) {
        this.userRepository = userRepository;
        this.s3PresignService = s3PresignService;
    }

    private UUID currentUserId() {
        TenantContext context = TenantContextHolder.getRequired();
        if (context.userId() == null) {
            throw new ApiException(ErrorCode.UNAUTHORIZED, "Missing user context", HttpStatus.UNAUTHORIZED.value());
        }
        return UUID.fromString(context.userId());
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getProfile() {
        UserEntity user = userRepository.findById(currentUserId())
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "User not found", HttpStatus.NOT_FOUND.value()));
        return toResponse(user);
    }

    @Transactional
    @LogAudit(action = "USER_UPDATE_PROFILE", entityType = "USER")
    public UserProfileResponse updateProfile(UpdateProfileRequest request) {
        UserEntity user = userRepository.findById(currentUserId())
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "User not found", HttpStatus.NOT_FOUND.value()));
        if (request.getDisplayName() != null) {
            user.setDisplayName(request.getDisplayName());
        }
        if (request.getAvatarS3Key() != null) {
            user.setAvatarS3Key(request.getAvatarS3Key());
        }
        user.setUpdatedAt(OffsetDateTime.now());
        return toResponse(userRepository.save(user));
    }

    @Transactional
    public void syncLastLogin() {
        userRepository.findById(currentUserId()).ifPresent(user -> {
            user.setLastLoginAt(OffsetDateTime.now());
            user.setUpdatedAt(OffsetDateTime.now());
            userRepository.save(user);
        });
    }

    @Transactional(readOnly = true)
    public AvatarPresignResponse presignAvatarUpload(String fileName, String contentType) {
        UUID userId = currentUserId();
        var presign = s3PresignService.presignAvatar(userId, fileName, contentType);
        return new AvatarPresignResponse(presign.url().toString(), presign.headersOrEmpty(), presign.expiresAt(),
                buildKey(userId, fileName));
    }

    private String buildKey(UUID userId, String fileName) {
        return "avatars/" + userId + "/" + fileName;
    }

    private UserProfileResponse toResponse(UserEntity user) {
        UserProfileResponse resp = new UserProfileResponse();
        resp.setId(user.getId());
        resp.setEmail(user.getEmail());
        resp.setDisplayName(user.getDisplayName());
        resp.setAvatarS3Key(user.getAvatarS3Key());
        resp.setLastLoginAt(user.getLastLoginAt());
        return resp;
    }
}
