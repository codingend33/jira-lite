import { apiRequest } from "./client";
import { Member } from "./types";

export async function listMembers(): Promise<Member[]> {
  return apiRequest<Member[]>("/org/members/lookup");
}
