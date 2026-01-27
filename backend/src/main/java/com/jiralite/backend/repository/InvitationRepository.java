package com.jiralite.backend.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.jiralite.backend.entity.InvitationEntity;

/**
 * Repository for managing invitation tokens.
 */
@Repository
public interface InvitationRepository extends JpaRepository<InvitationEntity, UUID> {

    /**
     * Find invitation by token.
     */
    Optional<InvitationEntity> findByToken(String token);

    /**
     * Find all invitations for an organization.
     */
    List<InvitationEntity> findByOrgId(UUID orgId);

    /**
     * Find pending invitations by email.
     */
    List<InvitationEntity> findByEmailAndExpiresAtAfter(String email, Instant now);

    /**
     * Delete expired invitations (for cleanup jobs).
     */
    void deleteByExpiresAtBefore(Instant now);
}
