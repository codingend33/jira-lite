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

    public OrgMemberService(OrgMembershipRepository membershipRepository, UserRepository userRepository) {
        this.membershipRepository = membershipRepository;
        this.userRepository = userRepository;
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
        return toResponse(saved, user);
    }

    @Transactional
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

        return toResponse(membership, user);
    }

    @Transactional
    public void deleteMember(UUID userId) {
        OrgMembershipEntity membership = getMembership(userId);
        membershipRepository.delete(membership);
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
