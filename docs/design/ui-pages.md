# UI Pages

## Overview

This document describes the current frontend page map, route protection, and
major page behavior for the React + TypeScript UI.

## Route Map

Public routes:

- `/login`
- `/invite` (accept invitation flow)

Authenticated routes:

- `/create-org`
- `/dashboard`
- `/projects`
- `/projects/:projectId`
- `/tickets`
- `/tickets/new`
- `/tickets/:ticketId`
- `/tickets/:ticketId/edit`
- `/trash`
- `/settings/profile`
- `/settings/members`
- `/notifications`

Default and fallback:

- `/` redirects to `/dashboard`
- unknown paths redirect to `/projects`

## Access Control (UI Behavior)

Authentication:

- All routes except `/login` and `/invite` are wrapped by `ProtectedRoute`.

Role-based behavior in UI:

- `ADMIN` can perform org/project/trash administrative actions.
- `MEMBER` can view most content, but action buttons are disabled/hidden where
  required by current business rules.

Current notable constraints:

- Settings members page is view-only for members.
- Trash page is view-only for members (restore actions hidden).
- Project and ticket mutation buttons are gated in the page UI and backed by API
  authorization.

## Page Specifications

### Login (`/login`)

- Initiates Cognito Hosted UI login.
- Handles callback state and token exchange through auth context flow.
- Redirect behavior after login is determined by auth state/org claim handling.

### Accept Invitation (`/invite`)

- Reads `token` query param.
- If unauthenticated: stores pending token and redirects to Cognito login.
- If authenticated: calls invitation acceptance API and then forces re-login to
  refresh claims.

### Create Organization (`/create-org`)

- Form to create first organization for authenticated users without org context.
- On success, forces re-login to refresh `org_id` and role claims.

### Dashboard (`/dashboard`)

- Summary cards: Active Projects, My Tickets, Team Members.
- Cards navigate to `/projects`, `/tickets?assignedTo=me`, `/settings/members`.
- Recent activity stream from audit logs with:
  - type filter
  - free-text filter
  - grouped display (Today / Yesterday / Earlier)
  - incremental pagination ("show more" / "load next page")

### Projects (`/projects`)

- Lists all projects in current org.
- Shows status, description, and creator label.
- Admin actions:
  - create project
  - archive / unarchive
  - move archived project to trash (delete)
- Member behavior:
  - list remains visible
  - mutation actions disabled or blocked in UI

### Project Detail (`/projects/:projectId`)

- Displays project metadata and creator.
- Shows project-scoped tickets with filter/sort/pagination controls.
- Supports navigation to ticket detail and create-ticket page.

### Tickets (`/tickets`)

- Paged ticket list with filters: status, priority, project, sort.
- Supports URL-driven modes:
  - search mode via `keyword`
  - "my tickets" mode via `assignedTo=me`
- Ticket cards show status, priority, project, assignee, creator.
- Entry action for creating new ticket.

### Ticket Form (`/tickets/new`, `/tickets/:ticketId/edit`)

- Shared create/edit form mode.
- Uses projects list for project selection.
- Submits to create or update ticket APIs depending on mode.

### Ticket Detail (`/tickets/:ticketId`)

- Ticket metadata and status transition.
- Comments section: list + create comment.
- Attachments section:
  - upload
  - download
  - delete attachment
- Ticket delete flow:
  - visible for admin or creator
  - confirmation dialog before moving to trash

### Trash (`/trash`)

- Unified trash list for projects and tickets.
- Filter by `all`, `project`, `ticket`.
- Shows purge countdown and deleted timestamp.
- Admin: can restore items.
- Member: view-only warning, no restore action.

### Settings Profile (`/settings/profile`)

- Manage display name and avatar upload.
- In-app password change dialog:
  - current/new/confirm fields
  - show/hide password toggles
  - validation and error handling
- Displays last login timestamp when available.

### Settings Members (`/settings/members`)

- Members table: avatar, name, email, role, status.
- Admin:
  - invite members
  - update roles
  - remove non-admin members
- Member:
  - can view list
  - cannot invite, remove, or change role

### Notifications (`/notifications`)

- Paged notification list.
- Mark-as-read action.
- Empty state handling.

## Layout and Navigation

- Top navigation includes: Dashboard, Projects, Tickets, Trash.
- Global search and notifications entry are available in layout.
- Settings access is available from profile menu.

## Cross-Page Events

- Global `api:auth-failed` event triggers:
  - error toast
  - redirect to `/login`
- Global `api:forbidden` event triggers permission toast except on pages where
  in-page permission messaging is intentionally used (trash and settings/members).
