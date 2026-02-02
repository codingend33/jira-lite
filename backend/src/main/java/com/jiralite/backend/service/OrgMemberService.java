package com.jiralite.backend.service;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jiralite.backend.audit.LogAudit;
import com.jiralite.backend.dto.CreateMemberRequest;
import com.jiralite.backend.dto.ErrorCode;
import com.jiralite.backend.dto.MemberResponse;
import com.jiralite.backend.dto.UpdateMemberRequest;
import com.jiralite.backend.entity.OrgMembershipEntity;
import com.jiralite.backend.entity.OrgMembershipId;
import com.jiralite.backend.entity.UserEntity;
import com.jiralite.backend.exception.ApiException;
import com.jiralite.backend.repository.OrgMembershipRepository;
import com.jiralite.backend.repository.UserRepository;
import com.jiralite.backend.security.tenant.TenantContext;
import com.jiralite.backend.security.tenant.TenantContextHolder;

/**
 * Org member management scoped to the current tenant.
 */
@Service
public class OrgMemberService {

    private final OrgMembershipRepository membershipRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    public OrgMemberService(OrgMembershipRepository membershipRepository, UserRepository userRepository,
            NotificationService notificationService) {
        this.membershipRepository = membershipRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    @Transactional(readOnly = true)
    public List<MemberResponse> listMembers() {
        UUID orgId = getOrgId();
        List<OrgMembershipEntity> memberships = membershipRepository.findAllByIdOrgId(orgId);
        Map<UUID, UserEntity> usersById = loadUsers(memberships);

        return memberships.stream()
                .map(membership -> toResponse(membership, usersById.get(membership.getId().getUserId())))
                .toList();
    }

    @Transactional
    @LogAudit(action = "MEMBER_CREATE", entityType = "ORG_MEMBER")
    public MemberResponse createMember(CreateMemberRequest request) {
        UUID orgId = getOrgId();
        UserEntity user = resolveUser(request);

        OrgMembershipId membershipId = new OrgMembershipId(orgId, user.getId());
        OrgMembershipEntity existing = membershipRepository.findById(membershipId).orElse(null);
        if (existing != null) {
            return toResponse(existing, user);
        }

        OrgMembershipEntity membership = new OrgMembershipEntity();
        membership.setId(membershipId);
        membership.setRole(defaultRole(request.getRole()));
        membership.setStatus("ACTIVE");
        membership.setCreatedAt(OffsetDateTime.now());
        membership.setUpdatedAt(OffsetDateTime.now());

        OrgMembershipEntity saved = membershipRepository.save(membership);
        notifyUser(user.getId(), "ORG_MEMBER_ADDED", "You were added to the organization");
        return toResponse(saved, user);
    }

    @Transactional
    @LogAudit(action = "MEMBER_UPDATE", entityType = "ORG_MEMBER")
    public MemberResponse updateMember(UUID userId, UpdateMemberRequest request) {
        if (request.getRole() == null && request.getStatus() == null) {
            throw new ApiException(ErrorCode.BAD_REQUEST, "role or status is required", HttpStatus.BAD_REQUEST.value());
        }

        OrgMembershipEntity membership = getMembership(userId);
        if (request.getRole() != null) {
            membership.setRole(request.getRole());
        }
        if (request.getStatus() != null) {
            membership.setStatus(request.getStatus());
        }
        membership.setUpdatedAt(OffsetDateTime.now());

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "User not found", HttpStatus.NOT_FOUND.value()));

        notifyUser(user.getId(), "ORG_MEMBER_UPDATED", "Your organization role/status changed");
        return toResponse(membership, user);
    }

    @Transactional
    @LogAudit(action = "MEMBER_DELETE", entityType = "ORG_MEMBER")
    public void deleteMember(UUID userId) {
        OrgMembershipEntity membership = getMembership(userId);
        ensureAdmin();
        membershipRepository.delete(membership);
        notifyUser(userId, "ORG_REMOVED", "You have been removed from the organization");
    }

    private void ensureAdmin() {
        TenantContext context = TenantContextHolder.getRequired();
        if (context.roles() == null || context.roles().stream().noneMatch(r -> r.equals("ADMIN") || r.equals("ROLE_ADMIN"))) {
            throw new ApiException(ErrorCode.FORBIDDEN, "Admin only", HttpStatus.FORBIDDEN.value());
        }
    }

    private void notifyUser(UUID userId, String type, String content) {
        notificationService.createNotification(userId, type, content);
    }

    private UUID getOrgId() {
        TenantContext context = TenantContextHolder.getRequired();
        if (context.orgId() == null || context.orgId().isBlank()) {
            throw new ApiException(ErrorCode.UNAUTHORIZED, "Missing org context", HttpStatus.UNAUTHORIZED.value());
        }
        return UUID.fromString(context.orgId());
    }

    private OrgMembershipEntity getMembership(UUID userId) {
        UUID orgId = getOrgId();
        // Ensure caller is admin (controller already @PreAuthorize, double-check optional)
        return membershipRepository.findByIdOrgIdAndIdUserId(orgId, userId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Membership not found", HttpStatus.NOT_FOUND.value()));
    }

    private UserEntity resolveUser(CreateMemberRequest request) {
        if (request.getUserId() != null) {
            return userRepository.findById(request.getUserId())
                    .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "User not found", HttpStatus.NOT_FOUND.value()));
        }
        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            return userRepository.findByEmail(request.getEmail())
                    .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "User not found", HttpStatus.NOT_FOUND.value()));
        }
        throw new ApiException(ErrorCode.BAD_REQUEST, "userId or email is required", HttpStatus.BAD_REQUEST.value());
    }

    private String defaultRole(String role) {
        return role == null || role.isBlank() ? "MEMBER" : role;
    }

    private Map<UUID, UserEntity> loadUsers(Collection<OrgMembershipEntity> memberships) {
        List<UUID> userIds = memberships.stream()
                .map(membership -> membership.getId().getUserId())
                .toList();
        return userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(UserEntity::getId, Function.identity()));
    }

    private MemberResponse toResponse(OrgMembershipEntity membership, UserEntity user) {
        String email = user != null ? user.getEmail() : null;
        String displayName = user != null ? user.getDisplayName() : null;
        return new MemberResponse(
                membership.getId().getUserId(),
                email,
                displayName,
                membership.getRole(),
                membership.getStatus());
    }
}
