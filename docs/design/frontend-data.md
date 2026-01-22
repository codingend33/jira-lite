# Frontend Data Layer (React Query)

## Query key convention

- Projects: `["projects"]`
- Tickets list: `["tickets", "list", filters]`
- Ticket detail: `["tickets", "detail", ticketId]`
- Comments: `["tickets", ticketId, "comments"]`
- Attachments: `["tickets", ticketId, "attachments"]`

## Error handling

- API errors surface the backend `{code,message,traceId}` payload.
- `401` clears local tokens and redirects the user back to login.

## Cache and invalidation

- Mutations invalidate list/detail keys for consistency.
- Lists default to `refetchOnWindowFocus=false` for stability.
