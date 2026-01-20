-- V8: Ticket comments + attachments constraints/indexes

CREATE INDEX IF NOT EXISTS idx_comments_org_ticket ON ticket_comments(org_id, ticket_id);
CREATE INDEX IF NOT EXISTS idx_attachments_org_ticket ON ticket_attachments(org_id, ticket_id);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE table_name = 'ticket_attachments' AND constraint_name = 'ck_ticket_attachment_upload_status'
    ) THEN
        ALTER TABLE ticket_attachments
          ADD CONSTRAINT ck_ticket_attachment_upload_status CHECK (upload_status IN ('PENDING', 'UPLOADED', 'FAILED'));
    END IF;
END $$;
