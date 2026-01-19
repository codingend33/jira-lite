package com.jiralite.backend.entity;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * Composite key for org_memberships.
 */
@Embeddable
public class OrgMembershipId implements Serializable {

    @Column(name = "org_id", nullable = false)
    private UUID orgId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    public OrgMembershipId() {
    }

    public OrgMembershipId(UUID orgId, UUID userId) {
        this.orgId = orgId;
        this.userId = userId;
    }

    public UUID getOrgId() {
        return orgId;
    }

    public void setOrgId(UUID orgId) {
        this.orgId = orgId;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof OrgMembershipId)) {
            return false;
        }
        OrgMembershipId that = (OrgMembershipId) o;
        return Objects.equals(orgId, that.orgId) && Objects.equals(userId, that.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(orgId, userId);
    }
}
