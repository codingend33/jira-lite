package com.jiralite.backend.repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.jiralite.backend.entity.ProjectEntity;

public interface ProjectRepository extends JpaRepository<ProjectEntity, UUID> {

    // --- Basic queries ---
    List<ProjectEntity> findAllByOrgId(UUID orgId);

    Optional<ProjectEntity> findByIdAndOrgId(UUID id, UUID orgId);

    boolean existsByOrgIdAndProjectKey(UUID orgId, String projectKey);

    long countByOrgId(UUID orgId);

    // --- Active projects (not archived, not deleted) ---
    @Query("SELECT p FROM ProjectEntity p WHERE p.orgId = :orgId AND p.deletedAt IS NULL AND p.archivedAt IS NULL")
    List<ProjectEntity> findActiveByOrgId(@Param("orgId") UUID orgId);

    @Query("SELECT COUNT(p) FROM ProjectEntity p WHERE p.orgId = :orgId AND p.deletedAt IS NULL AND p.archivedAt IS NULL")
    long countActiveByOrgId(@Param("orgId") UUID orgId);

    // --- Archived projects (not deleted) ---
    @Query("SELECT p FROM ProjectEntity p WHERE p.orgId = :orgId AND p.deletedAt IS NULL AND p.archivedAt IS NOT NULL")
    List<ProjectEntity> findArchivedByOrgId(@Param("orgId") UUID orgId);

    // --- Trash (soft deleted) ---
    @Query("SELECT p FROM ProjectEntity p WHERE p.orgId = :orgId AND p.deletedAt IS NOT NULL ORDER BY p.deletedAt DESC")
    List<ProjectEntity> findTrashByOrgId(@Param("orgId") UUID orgId);

    // --- For scheduled cleanup ---
    @Query("SELECT p FROM ProjectEntity p WHERE p.deletedAt IS NOT NULL AND p.purgeAfter <= :now")
    List<ProjectEntity> findPurgeCandidates(@Param("now") OffsetDateTime now);

    @Query("SELECT p FROM ProjectEntity p WHERE p.deletedAt IS NOT NULL AND p.purgeAfter <= :now")
    List<ProjectEntity> findPurgeCandidatesPaged(@Param("now") OffsetDateTime now,
            org.springframework.data.domain.Pageable pageable);

    // --- Check for key conflict on restore ---
    @Query("SELECT COUNT(p) > 0 FROM ProjectEntity p WHERE p.orgId = :orgId AND p.projectKey = :key AND p.deletedAt IS NULL")
    boolean existsActiveByOrgIdAndProjectKey(@Param("orgId") UUID orgId, @Param("key") String key);
}
