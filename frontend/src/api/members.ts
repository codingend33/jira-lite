import { apiRequest } from "./client";
import { Member } from "./types";

export async function listMembers(): Promise<Member[]> {
  return apiRequest<Member[]>("/org/members/lookup", { method: "GET" });
}

export async function listMembersAdmin(): Promise<Member[]> {
  return apiRequest<Member[]>("/org/members", { method: "GET" });
}

export async function removeMember(userId: string): Promise<void> {
  await apiRequest(`/org/members/${userId}`, { method: "DELETE" });
}
