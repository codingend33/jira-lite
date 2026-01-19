package com.jiralite.backend.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.jiralite.backend.entity.OrgEntity;

public interface OrgRepository extends JpaRepository<OrgEntity, UUID> {
}
