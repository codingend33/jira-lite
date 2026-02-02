package com.jiralite.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.jiralite.backend.audit.LogAudit;
import com.jiralite.backend.dto.AvatarPresignResponse;
import com.jiralite.backend.dto.UpdateProfileRequest;
import com.jiralite.backend.dto.UserProfileResponse;
import com.jiralite.backend.service.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/users/me")
@Validated
@Tag(name = "User Profile", description = "Profile and login sync")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/profile")
    @Operation(summary = "Get current user profile")
    public ResponseEntity<UserProfileResponse> getProfile() {
        return ResponseEntity.ok(userService.getProfile());
    }

    @PostMapping("/profile")
    @LogAudit(action = "UPDATE_PROFILE", entityType = "USER")
    @Operation(summary = "Update display name or avatar")
    public ResponseEntity<UserProfileResponse> updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(userService.updateProfile(request));
    }

    @PostMapping("/login-sync")
    @Operation(summary = "Sync last login timestamp")
    public ResponseEntity<Void> syncLogin() {
        userService.syncLastLogin();
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/avatar/presign")
    @Operation(summary = "Get presigned URL for avatar upload")
    public ResponseEntity<AvatarPresignResponse> presignAvatar(
            @RequestParam String fileName,
            @RequestParam(defaultValue = "image/png") String contentType) {
        return ResponseEntity.ok(userService.presignAvatarUpload(fileName, contentType));
    }
}
