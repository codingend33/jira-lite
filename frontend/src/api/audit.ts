import { apiRequest } from "./client";

export interface AuditLog {
  id: string;
  actorUserId: string;
  action: string;
  entityType: string;
  entityId: string;
  details?: string;
  createdAt: string;
}

export async function listAuditLogs(): Promise<AuditLog[]> {
  return apiRequest("/audit/logs", { method: "GET" });
}
