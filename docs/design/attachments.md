# Ticket Attachments (S3 Presigned)

## Overview

Attachments use S3 presigned URLs for upload and download. The backend stores
metadata and validates tenant ownership.

## Sequence

```mermaid
sequenceDiagram
title attachment-upload
participant U as Member
participant API as Backend
participant SEC as Spring Security
participant TC as TenantContext
participant SVC as TicketAttachmentService
participant S3 as S3Presigner
participant DB as Postgres

U->>API: POST /tickets/{id}/attachments/presign-upload + JWT
API->>SEC: Validate JWT
SEC-->>TC: Authenticated
TC->>SVC: orgId from TenantContext
SVC->>DB: Verify ticket belongs to org
DB-->>SVC: OK
SVC->>S3: Create presigned PUT
S3-->>SVC: uploadUrl + headers
SVC->>DB: Save attachment metadata (PENDING)
SVC-->>API: PresignUploadResponse
```
