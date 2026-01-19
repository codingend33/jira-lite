-- V6: Projects enhancements (description + status + constraints)

ALTER TABLE projects ADD COLUMN IF NOT EXISTS description TEXT;

ALTER TABLE projects
  ADD COLUMN IF NOT EXISTS status TEXT NOT NULL DEFAULT 'ACTIVE';

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE table_name = 'projects' AND constraint_name = 'ck_projects_status'
    ) THEN
        ALTER TABLE projects
          ADD CONSTRAINT ck_projects_status CHECK (status IN ('ACTIVE', 'ARCHIVED'));
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_projects_org_status ON projects(org_id, status);
