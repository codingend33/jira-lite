package com.jiralite.backend.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.jiralite.backend.entity.ProjectEntity;

public interface ProjectRepository extends JpaRepository<ProjectEntity, UUID> {
    List<ProjectEntity> findAllByOrgId(UUID orgId);

    Optional<ProjectEntity> findByIdAndOrgId(UUID id, UUID orgId);

    boolean existsByOrgIdAndProjectKey(UUID orgId, String projectKey);

    long countByOrgId(UUID orgId);
}
