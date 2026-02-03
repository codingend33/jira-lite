import { apiRequest } from "./client";
import { PagedResponse } from "./types";

export interface AuditLog {
  id: string;
  actorUserId: string;
  action: string;
  entityType: string;
  entityId: string;
  details?: string;
  createdAt: string;
}

export async function listAuditLogs(params?: {
  page?: number;
  size?: number;
  action?: string;
  actorUserId?: string;
}): Promise<PagedResponse<AuditLog>> {
  const search = new URLSearchParams();
  if (params?.page !== undefined) search.set("page", String(params.page));
  if (params?.size !== undefined) search.set("size", String(params.size));
  if (params?.action) search.set("action", params.action);
  if (params?.actorUserId) search.set("actorUserId", params.actorUserId);
  const qs = search.toString();
  const url = qs ? `/audit/logs?${qs}` : "/audit/logs";
  return apiRequest(url, { method: "GET" });
}
