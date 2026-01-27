-- V9: Invitations table for user onboarding
-- Supports invitation-based organization membership

BEGIN;

CREATE TABLE IF NOT EXISTS invitations (
  id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  org_id       UUID NOT NULL REFERENCES orgs(id) ON DELETE CASCADE,
  email        VARCHAR(255) NOT NULL,
  token        VARCHAR(255) NOT NULL UNIQUE,
  role         VARCHAR(50) NOT NULL,
  expires_at   TIMESTAMPTZ NOT NULL,
  created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
  created_by   UUID REFERENCES users(id) ON DELETE SET NULL,
  
  CONSTRAINT chk_invitation_role CHECK (role IN ('ADMIN', 'MEMBER'))
);

-- Index for token lookup (primary access pattern)
CREATE INDEX idx_invitations_token ON invitations(token);

-- Index for email lookup (validation)
CREATE INDEX idx_invitations_email ON invitations(email);

-- Index for cleanup queries (expired invitations)
CREATE INDEX idx_invitations_expires_at ON invitations(expires_at);

COMMIT;
