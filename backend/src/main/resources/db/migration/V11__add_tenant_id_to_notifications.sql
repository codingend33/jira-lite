-- V11: Align notifications table with tenant-aware model

BEGIN;

-- 1) Add tenant_id if missing
ALTER TABLE notifications
  ADD COLUMN IF NOT EXISTS tenant_id UUID;

-- 2) Backfill tenant_id from org memberships (one org per user)
UPDATE notifications n
SET tenant_id = om.org_id
FROM org_memberships om
WHERE om.user_id = n.user_id
  AND n.tenant_id IS NULL;

-- 3) Index for tenant queries
CREATE INDEX IF NOT EXISTS idx_notifications_tenant_id ON notifications(tenant_id);

-- Optional strictness (skip if historical rows may be orphaned)
-- ALTER TABLE notifications ALTER COLUMN tenant_id SET NOT NULL;

COMMIT;
