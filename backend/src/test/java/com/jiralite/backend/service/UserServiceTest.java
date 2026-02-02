package com.jiralite.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;

import java.net.URL;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.jiralite.backend.dto.AvatarPresignResponse;
import com.jiralite.backend.dto.UpdateProfileRequest;
import com.jiralite.backend.entity.UserEntity;
import com.jiralite.backend.repository.UserRepository;
import com.jiralite.backend.security.tenant.TenantContext;
import com.jiralite.backend.security.tenant.TenantContextHolder;
import com.jiralite.backend.service.S3PresignService.PresignResult;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private S3PresignService s3PresignService;

    private UserService userService;
    private UUID orgId;
    private UUID userId;

    @BeforeEach
    void setUp() throws Exception {
        userService = new UserService(userRepository, s3PresignService);
        orgId = UUID.randomUUID();
        userId = UUID.randomUUID();
        TenantContextHolder.set(new TenantContext(orgId.toString(), userId.toString(), Set.of("ADMIN"), "trace"));
        UserEntity entity = new UserEntity();
        entity.setId(userId);
        entity.setEmail("u@test.com");
        lenient().when(userRepository.findById(userId)).thenReturn(Optional.of(entity));
        lenient().when(userRepository.save(org.mockito.ArgumentMatchers.any(UserEntity.class)))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void updateProfile_updatesDisplayAndAvatar() {
        UpdateProfileRequest req = new UpdateProfileRequest();
        req.setDisplayName("New Name");
        req.setAvatarS3Key("avatars/key.png");

        userService.updateProfile(req);

        ArgumentCaptor<UserEntity> captor = ArgumentCaptor.forClass(UserEntity.class);
        verify(userRepository).save(captor.capture());
        UserEntity saved = captor.getValue();
        assertThat(saved.getDisplayName()).isEqualTo("New Name");
        assertThat(saved.getAvatarS3Key()).isEqualTo("avatars/key.png");
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    @Test
    void syncLastLogin_setsTimestamp() {
        userService.syncLastLogin();
        ArgumentCaptor<UserEntity> captor = ArgumentCaptor.forClass(UserEntity.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getLastLoginAt()).isBeforeOrEqualTo(OffsetDateTime.now());
    }

    @Test
    void presignAvatarUpload_returnsKeyAndUrl() throws Exception {
        PresignResult presign = new PresignResult(new URL("https://s3/upload"), Map.of("h", "v"), OffsetDateTime.now().plusMinutes(5));
        when(s3PresignService.presignAvatar(userId, "pic.png", "image/png")).thenReturn(presign);

        AvatarPresignResponse resp = userService.presignAvatarUpload("pic.png", "image/png");

        assertThat(resp.uploadUrl()).isEqualTo("https://s3/upload");
        assertThat(resp.headers().get("h")).isEqualTo("v");
        assertThat(resp.key()).isEqualTo("avatars/" + userId + "/pic.png");
    }
}
