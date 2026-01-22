import { apiRequest } from "./client";
import { Comment } from "./types";

export async function listComments(ticketId: string): Promise<Comment[]> {
  return apiRequest<Comment[]>(`/tickets/${ticketId}/comments`);
}

export async function createComment(ticketId: string, body: string): Promise<Comment> {
  return apiRequest<Comment>(`/tickets/${ticketId}/comments`, {
    method: "POST",
    body: JSON.stringify({ body })
  });
}
