-- V2: Extend core domain tables with additional fields and optimized indexes
-- This migration adds missing columns, improves constraints, and optimizes indexes
-- for Day 3 baseline: core business table enhancements

-- Add missing 'status' field to org_membership (for ACTIVE/INVITED/DISABLED states)
ALTER TABLE org_memberships ADD COLUMN IF NOT EXISTS status VARCHAR NOT NULL DEFAULT 'ACTIVE';

-- Add CHECK constraint for membership status (idempotent via DO block)
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE table_name = 'org_memberships' AND constraint_name = 'ck_org_membership_status'
    ) THEN
        ALTER TABLE org_memberships
          ADD CONSTRAINT ck_org_membership_status CHECK (status IN ('ACTIVE', 'INVITED', 'DISABLED'));
    END IF;
END $$;

-- Add updated_at to org_membership for audit trail
ALTER TABLE org_memberships ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ NOT NULL DEFAULT now();

-- Add cognito_sub to users for AWS Cognito integration
ALTER TABLE users ADD COLUMN IF NOT EXISTS cognito_sub TEXT UNIQUE;

-- Add updated_at to ticket_comments for audit trail
ALTER TABLE ticket_comments ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ NOT NULL DEFAULT now();

-- Add upload_status field to ticket_attachments
ALTER TABLE ticket_attachments ADD COLUMN IF NOT EXISTS upload_status VARCHAR NOT NULL DEFAULT 'UPLOADED';

-- Add CHECK constraint for upload_status (idempotent via DO block)
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE table_name = 'ticket_attachments' AND constraint_name = 'ck_ticket_attachment_upload_status'
    ) THEN
        ALTER TABLE ticket_attachments
          ADD CONSTRAINT ck_ticket_attachment_upload_status CHECK (upload_status IN ('PENDING', 'UPLOADED', 'FAILED'));
    END IF;
END $$;

-- Add updated_at to ticket_attachments for audit trail
ALTER TABLE ticket_attachments ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ NOT NULL DEFAULT now();

-- =========================================
-- Optimize indexes for query patterns
-- =========================================

-- Composite index for ticket filtering by org, project, and status
CREATE INDEX IF NOT EXISTS idx_tickets_org_project_status ON tickets(org_id, project_id, status);

-- Index for sorting tickets by recent updates
CREATE INDEX IF NOT EXISTS idx_tickets_updated_at ON tickets(updated_at DESC);

-- Composite index for fetching ticket comments with pagination
CREATE INDEX IF NOT EXISTS idx_ticket_comments_ticket_created ON ticket_comments(ticket_id, created_at);

-- Composite index for fetching ticket attachments with pagination
CREATE INDEX IF NOT EXISTS idx_ticket_attachments_ticket_created ON ticket_attachments(ticket_id, created_at);

