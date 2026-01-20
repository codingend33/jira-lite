# ADR-0008: Comments and Attachments with S3 Presigned URLs

## Status

Accepted

## Context

The system needs ticket comments and attachments. Files must be uploaded without
proxying through the backend. Presigned URLs enable direct S3 upload/download.

## Decision

- Implement ticket comments (list/create) scoped by orgId from TenantContext.
- Implement attachment metadata storage and presigned upload/download URLs.
- Use S3Presigner (AWS SDK v2) with short-lived URLs.
- Store attachment metadata with upload_status (PENDING/UPLOADED/FAILED).

## Consequences

- Upload requires a confirm call to mark status as UPLOADED.
- No secrets are stored in code; bucket/region are configured via env variables.
- Future work: delete attachments, virus scan, and lifecycle policies.
