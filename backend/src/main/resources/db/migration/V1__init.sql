-- V1: Initial schema for Jira Lite (multi-tenant)
-- Rules:
-- - Tenant-owned tables include org_id.
-- - Composite foreign keys (org_id, id) prevent cross-org references.

BEGIN;

-- For gen_random_uuid()
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- Orgs
CREATE TABLE IF NOT EXISTS orgs (
  id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  name        TEXT NOT NULL,
  created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Users (global)
CREATE TABLE IF NOT EXISTS users (
  id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  email        TEXT NOT NULL UNIQUE,
  display_name TEXT,
  created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Memberships (RBAC minimal)
CREATE TABLE IF NOT EXISTS org_memberships (
  org_id      UUID NOT NULL,
  user_id     UUID NOT NULL,
  role        TEXT NOT NULL,
  created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
  PRIMARY KEY (org_id, user_id),
  CONSTRAINT fk_membership_org FOREIGN KEY (org_id) REFERENCES orgs(id) ON DELETE CASCADE,
  CONSTRAINT fk_membership_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  CONSTRAINT ck_membership_role CHECK (role IN ('ADMIN', 'MEMBER'))
);

CREATE INDEX IF NOT EXISTS idx_org_memberships_user_id ON org_memberships(user_id);
CREATE INDEX IF NOT EXISTS idx_org_memberships_org_id ON org_memberships(org_id);

-- Projects
CREATE TABLE IF NOT EXISTS projects (
  id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  org_id      UUID NOT NULL,
  project_key TEXT NOT NULL,
  name        TEXT NOT NULL,
  created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT fk_projects_org FOREIGN KEY (org_id) REFERENCES orgs(id) ON DELETE CASCADE,
  CONSTRAINT uq_projects_org_project_key UNIQUE (org_id, project_key),
  CONSTRAINT uq_projects_org_id_id UNIQUE (org_id, id)
);

CREATE INDEX IF NOT EXISTS idx_projects_org_id ON projects(org_id);

-- Tickets
CREATE TABLE IF NOT EXISTS tickets (
  id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  org_id      UUID NOT NULL,
  project_id  UUID NOT NULL,
  ticket_key  TEXT NOT NULL,
  title       TEXT NOT NULL,
  description TEXT,
  status      TEXT NOT NULL DEFAULT 'OPEN',
  priority    TEXT NOT NULL DEFAULT 'MEDIUM',
  created_by  UUID,
  assignee_id UUID,
  created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),

  CONSTRAINT fk_tickets_org FOREIGN KEY (org_id) REFERENCES orgs(id) ON DELETE CASCADE,

  -- Prevent cross-org project reference
  CONSTRAINT fk_tickets_project FOREIGN KEY (org_id, project_id)
    REFERENCES projects(org_id, id) ON DELETE RESTRICT,

  CONSTRAINT fk_tickets_created_by FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL,
  CONSTRAINT fk_tickets_assignee FOREIGN KEY (assignee_id) REFERENCES users(id) ON DELETE SET NULL,

  CONSTRAINT uq_tickets_org_ticket_key UNIQUE (org_id, ticket_key),
  CONSTRAINT uq_tickets_org_id_id UNIQUE (org_id, id),

  CONSTRAINT ck_tickets_status CHECK (status IN ('OPEN', 'IN_PROGRESS', 'DONE', 'CANCELLED')),
  CONSTRAINT ck_tickets_priority CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH', 'URGENT'))
);

CREATE INDEX IF NOT EXISTS idx_tickets_org_id ON tickets(org_id);
CREATE INDEX IF NOT EXISTS idx_tickets_project_id ON tickets(project_id);
CREATE INDEX IF NOT EXISTS idx_tickets_status ON tickets(status);
CREATE INDEX IF NOT EXISTS idx_tickets_created_at ON tickets(created_at);

-- Ticket comments
CREATE TABLE IF NOT EXISTS ticket_comments (
  id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  org_id      UUID NOT NULL,
  ticket_id   UUID NOT NULL,
  author_id   UUID,
  body        TEXT NOT NULL,
  created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),

  CONSTRAINT fk_comments_org FOREIGN KEY (org_id) REFERENCES orgs(id) ON DELETE CASCADE,
  CONSTRAINT fk_comments_ticket FOREIGN KEY (org_id, ticket_id)
    REFERENCES tickets(org_id, id) ON DELETE CASCADE,
  CONSTRAINT fk_comments_author FOREIGN KEY (author_id) REFERENCES users(id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_ticket_comments_org_id ON ticket_comments(org_id);
CREATE INDEX IF NOT EXISTS idx_ticket_comments_ticket_id ON ticket_comments(ticket_id);

-- Ticket attachments
CREATE TABLE IF NOT EXISTS ticket_attachments (
  id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  org_id        UUID NOT NULL,
  ticket_id     UUID NOT NULL,
  uploaded_by   UUID,
  file_name     TEXT NOT NULL,
  content_type  TEXT NOT NULL,
  file_size     BIGINT NOT NULL DEFAULT 0,
  s3_key        TEXT,
  created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),

  CONSTRAINT fk_attachments_org FOREIGN KEY (org_id) REFERENCES orgs(id) ON DELETE CASCADE,
  CONSTRAINT fk_attachments_ticket FOREIGN KEY (org_id, ticket_id)
    REFERENCES tickets(org_id, id) ON DELETE CASCADE,
  CONSTRAINT fk_attachments_uploader FOREIGN KEY (uploaded_by) REFERENCES users(id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_ticket_attachments_org_id ON ticket_attachments(org_id);
CREATE INDEX IF NOT EXISTS idx_ticket_attachments_ticket_id ON ticket_attachments(ticket_id);

COMMIT;

