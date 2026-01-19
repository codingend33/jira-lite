-- V5: Org member admin constraints (idempotent)
-- Purpose: ensure org_memberships constraints align with admin management

-- Ensure NOT NULL constraints
ALTER TABLE org_memberships
  ALTER COLUMN org_id SET NOT NULL,
  ALTER COLUMN user_id SET NOT NULL,
  ALTER COLUMN role SET NOT NULL,
  ALTER COLUMN status SET NOT NULL;

-- Ensure role CHECK constraint exists
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE table_name = 'org_memberships' AND constraint_name = 'ck_membership_role'
    ) THEN
        ALTER TABLE org_memberships
          ADD CONSTRAINT ck_membership_role CHECK (role IN ('ADMIN', 'MEMBER'));
    END IF;
END $$;

-- Ensure status CHECK constraint exists
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

-- Ensure unique constraint on (org_id, user_id) if primary key is missing
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE table_name = 'org_memberships' AND constraint_type = 'PRIMARY KEY'
    ) THEN
        ALTER TABLE org_memberships
          ADD CONSTRAINT uq_org_memberships_org_user UNIQUE (org_id, user_id);
    END IF;
END $$;
