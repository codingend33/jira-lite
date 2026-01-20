-- V7: Ticket query optimization for list/filter/sort

CREATE INDEX IF NOT EXISTS idx_tickets_org_status ON tickets(org_id, status);

CREATE INDEX IF NOT EXISTS idx_tickets_org_project ON tickets(org_id, project_id);

CREATE INDEX IF NOT EXISTS idx_tickets_org_created_at ON tickets(org_id, created_at DESC);
