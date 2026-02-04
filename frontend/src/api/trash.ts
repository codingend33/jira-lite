import { apiRequest } from "./client";
import { Project, Ticket } from "./types";

export type TrashItem = {
    id: string;
    type: "PROJECT" | "TICKET";
    name: string;
    key: string;
    deletedAt: string;
    deletedBy: string | null;
    purgeAfter: string;
    daysRemaining: number;
};

/**
 * List all items in trash (projects and tickets).
 */
export async function listTrash(type: "all" | "project" | "ticket" = "all"): Promise<TrashItem[]> {
    return apiRequest<TrashItem[]>(`/trash?type=${type}`);
}

/**
 * List trashed projects only.
 */
export async function listTrashProjects(): Promise<Project[]> {
    return apiRequest<Project[]>("/projects/trash");
}

/**
 * List trashed tickets only.
 */
export async function listTrashTickets(): Promise<Ticket[]> {
    return apiRequest<Ticket[]>("/tickets/trash");
}

/**
 * Restore a project from trash.
 */
export async function restoreProject(projectId: string): Promise<Project> {
    return apiRequest<Project>(`/projects/${projectId}/restore`, { method: "POST" });
}

/**
 * Restore a ticket from trash.
 */
export async function restoreTicket(ticketId: string): Promise<Ticket> {
    return apiRequest<Ticket>(`/tickets/${ticketId}/restore`, { method: "POST" });
}

/**
 * Soft delete a ticket (move to trash).
 */
export async function deleteTicket(ticketId: string, reason?: string): Promise<void> {
    const params = reason ? `?reason=${encodeURIComponent(reason)}` : "";
    await apiRequest<void>(`/tickets/${ticketId}/delete${params}`, { method: "POST" });
}
