import { apiRequest } from "./client";
import { PagedResponse, Ticket } from "./types";

export type TicketFilters = {
  status?: string;
  priority?: string;
  projectId?: string;
  page?: number;
  size?: number;
  sort?: string;
};

function buildQuery(filters: TicketFilters): string {
  const params = new URLSearchParams();
  if (filters.status) {
    params.set("status", filters.status);
  }
  if (filters.priority) {
    params.set("priority", filters.priority);
  }
  if (filters.projectId) {
    params.set("projectId", filters.projectId);
  }
  if (filters.page !== undefined) {
    params.set("page", String(filters.page));
  }
  if (filters.size !== undefined) {
    params.set("size", String(filters.size));
  }
  if (filters.sort) {
    params.set("sort", filters.sort);
  }
  const query = params.toString();
  return query ? `?${query}` : "";
}

export async function listTickets(filters: TicketFilters): Promise<PagedResponse<Ticket>> {
  return apiRequest<PagedResponse<Ticket>>(`/tickets${buildQuery(filters)}`);
}

export async function searchTickets(keyword: string): Promise<Ticket[]> {
  const params = new URLSearchParams({ keyword });
  return apiRequest<Ticket[]>(`/tickets/search?${params.toString()}`);
}

export async function getTicket(ticketId: string): Promise<Ticket> {
  return apiRequest<Ticket>(`/tickets/${ticketId}`);
}

export async function createTicket(payload: {
  projectId: string;
  title: string;
  description?: string;
  priority: string;
  assigneeId?: string | null;
}): Promise<Ticket> {
  return apiRequest<Ticket>("/tickets", {
    method: "POST",
    body: JSON.stringify(payload)
  });
}

export async function updateTicket(
  ticketId: string,
  payload: { title?: string; description?: string; priority?: string; assigneeId?: string | null }
): Promise<Ticket> {
  return apiRequest<Ticket>(`/tickets/${ticketId}`, {
    method: "PATCH",
    body: JSON.stringify(payload)
  });
}

export async function transitionTicket(ticketId: string, status: string): Promise<Ticket> {
  return apiRequest<Ticket>(`/tickets/${ticketId}/transition`, {
    method: "POST",
    body: JSON.stringify({ status })
  });
}
