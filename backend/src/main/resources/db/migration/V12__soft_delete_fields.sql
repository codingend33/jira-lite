-- =============================================================================
-- V12: Soft Delete & Archive Fields for Deletion Strategy
-- =============================================================================

-- -----------------------------------------------------------------------------
-- 1. Projects table - Add archive and soft delete fields
-- -----------------------------------------------------------------------------
ALTER TABLE projects ADD COLUMN IF NOT EXISTS archived_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE projects ADD COLUMN IF NOT EXISTS archived_by UUID;
ALTER TABLE projects ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE projects ADD COLUMN IF NOT EXISTS deleted_by UUID;
ALTER TABLE projects ADD COLUMN IF NOT EXISTS purge_after TIMESTAMP WITH TIME ZONE;
ALTER TABLE projects ADD COLUMN IF NOT EXISTS restored_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE projects ADD COLUMN IF NOT EXISTS restored_by UUID;

-- -----------------------------------------------------------------------------
-- 2. Tickets table - Add soft delete fields
-- -----------------------------------------------------------------------------
ALTER TABLE tickets ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE tickets ADD COLUMN IF NOT EXISTS deleted_by UUID;
ALTER TABLE tickets ADD COLUMN IF NOT EXISTS purge_after TIMESTAMP WITH TIME ZONE;
ALTER TABLE tickets ADD COLUMN IF NOT EXISTS deleted_reason VARCHAR(255);
ALTER TABLE tickets ADD COLUMN IF NOT EXISTS restored_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE tickets ADD COLUMN IF NOT EXISTS restored_by UUID;

-- -----------------------------------------------------------------------------
-- 3. Ticket Comments table - Add soft delete fields
-- -----------------------------------------------------------------------------
ALTER TABLE ticket_comments ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE ticket_comments ADD COLUMN IF NOT EXISTS deleted_by UUID;

-- -----------------------------------------------------------------------------
-- 4. Ticket Attachments table - Add soft delete fields
-- -----------------------------------------------------------------------------
ALTER TABLE ticket_attachments ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE ticket_attachments ADD COLUMN IF NOT EXISTS deleted_by UUID;

-- -----------------------------------------------------------------------------
-- 5. Indexes for efficient querying
-- -----------------------------------------------------------------------------

-- Projects: Active (not archived, not deleted)
CREATE INDEX IF NOT EXISTS idx_projects_active 
    ON projects(org_id) 
    WHERE deleted_at IS NULL AND archived_at IS NULL;

-- Projects: Archived only
CREATE INDEX IF NOT EXISTS idx_projects_archived 
    ON projects(org_id, archived_at) 
    WHERE deleted_at IS NULL AND archived_at IS NOT NULL;

-- Projects: Trash (soft deleted)
CREATE INDEX IF NOT EXISTS idx_projects_trash 
    ON projects(org_id, deleted_at) 
    WHERE deleted_at IS NOT NULL;

-- Projects: For scheduled cleanup
CREATE INDEX IF NOT EXISTS idx_projects_purge 
    ON projects(purge_after) 
    WHERE purge_after IS NOT NULL AND deleted_at IS NOT NULL;

-- Tickets: Active (not deleted)
CREATE INDEX IF NOT EXISTS idx_tickets_active 
    ON tickets(org_id, project_id) 
    WHERE deleted_at IS NULL;

-- Tickets: Trash (soft deleted)
CREATE INDEX IF NOT EXISTS idx_tickets_trash 
    ON tickets(org_id, deleted_at) 
    WHERE deleted_at IS NOT NULL;

-- Tickets: For scheduled cleanup
CREATE INDEX IF NOT EXISTS idx_tickets_purge 
    ON tickets(purge_after) 
    WHERE purge_after IS NOT NULL AND deleted_at IS NOT NULL;

-- Comments: Active (not deleted)
CREATE INDEX IF NOT EXISTS idx_comments_active 
    ON ticket_comments(ticket_id) 
    WHERE deleted_at IS NULL;

-- Attachments: Active (not deleted)
CREATE INDEX IF NOT EXISTS idx_attachments_active 
    ON ticket_attachments(ticket_id) 
    WHERE deleted_at IS NULL;
