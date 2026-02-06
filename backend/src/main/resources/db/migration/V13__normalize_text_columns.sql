-- V13: Normalize long text columns to TEXT to prevent varchar(255) truncation errors

BEGIN;

ALTER TABLE tickets
  ALTER COLUMN description TYPE TEXT;

ALTER TABLE projects
  ALTER COLUMN description TYPE TEXT;

ALTER TABLE ticket_comments
  ALTER COLUMN body TYPE TEXT;

ALTER TABLE notifications
  ALTER COLUMN content TYPE TEXT;

ALTER TABLE audit_logs
  ALTER COLUMN details TYPE TEXT;

COMMIT;
