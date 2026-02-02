package com.jiralite.backend.repository;

import java.util.Optional;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.jiralite.backend.entity.TicketEntity;

public interface TicketRepository extends JpaRepository<TicketEntity, UUID>, JpaSpecificationExecutor<TicketEntity> {
    Optional<TicketEntity> findByIdAndOrgId(UUID id, UUID orgId);

    long countByOrgIdAndProjectId(UUID orgId, UUID projectId);

    long countByOrgIdAndAssigneeId(UUID orgId, UUID assigneeId);

    @Query("select t from TicketEntity t where t.orgId = :orgId and " +
            "(lower(t.title) like lower(concat('%', :keyword, '%')) or lower(t.description) like lower(concat('%', :keyword, '%')))")
    List<TicketEntity> searchTickets(@Param("orgId") UUID orgId, @Param("keyword") String keyword);
}
