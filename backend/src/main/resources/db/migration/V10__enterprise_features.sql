-- V10: Enterprise features (avatars, audit logs, notifications)

BEGIN;

-- 1) Users: avatar + last login time
ALTER TABLE users
  ADD COLUMN IF NOT EXISTS avatar_s3_key VARCHAR(512),
  ADD COLUMN IF NOT EXISTS last_login_at TIMESTAMPTZ;

-- 2) Audit logs
CREATE TABLE IF NOT EXISTS audit_logs (
  id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  tenant_id    UUID NOT NULL,
  actor_user_id UUID,
  action       VARCHAR(100) NOT NULL,
  entity_type  VARCHAR(100) NOT NULL,
  entity_id    VARCHAR(100),
  details      TEXT,
  created_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_audit_logs_tenant_created_at
  ON audit_logs (tenant_id, created_at DESC);

-- 3) Notifications
CREATE TABLE IF NOT EXISTS notifications (
  id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id    UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  type       VARCHAR(50) NOT NULL,
  content    TEXT NOT NULL,
  is_read    BOOLEAN NOT NULL DEFAULT FALSE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_notifications_user_read_created_at
  ON notifications (user_id, is_read, created_at DESC);

COMMIT;
