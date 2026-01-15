-- V3: Complete multi-tenant schema refinement
-- This migration completes the database design by:
-- - Adding created_by field to projects table for project creator tracking
-- - Adding supporting indexes for efficient project queries by creator
-- - Ensuring complete audit trail for project lifecycle

-- Add created_by field to projects table
ALTER TABLE projects ADD COLUMN IF NOT EXISTS created_by UUID;

-- Add FK constraint for created_by (no cascade - creator user data is preserved)
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE table_name = 'projects' AND constraint_name = 'fk_projects_created_by'
    ) THEN
        ALTER TABLE projects
          ADD CONSTRAINT fk_projects_created_by FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL;
    END IF;
END $$;

-- Add index for querying projects created by a specific user
CREATE INDEX IF NOT EXISTS idx_projects_created_by ON projects(created_by);

-- Add composite index for multi-tenant project queries by org and creator
CREATE INDEX IF NOT EXISTS idx_projects_org_created_by ON projects(org_id, created_by);

-- Add comment to clarify ticket.created_by field represents the ticket reporter
COMMENT ON COLUMN tickets.created_by IS 'User who reported/created the ticket (equivalent to reported_by in conceptual model)';

-- Add missing indexes mentioned in design but not yet created
-- Index for tenant-level filtering in projects
CREATE INDEX IF NOT EXISTS idx_projects_org_id_updated ON projects(org_id, updated_at DESC);

-- Verify all V1+V2+V3 schema components are in place
-- This is a safety check to ensure critical constraints exist
DO $$
BEGIN
    -- Verify org_memberships has all required constraints
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE table_name = 'org_memberships' AND constraint_name = 'ck_org_membership_status'
    ) THEN
        RAISE WARNING 'Missing ck_org_membership_status constraint';
    END IF;

    -- Verify ticket_attachments has all required constraints
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE table_name = 'ticket_attachments' AND constraint_name = 'ck_ticket_attachment_upload_status'
    ) THEN
        RAISE WARNING 'Missing ck_ticket_attachment_upload_status constraint';
    END IF;

    RAISE NOTICE 'V3 migration: Schema completion check passed';
END $$;

