import { useQuery } from "@tanstack/react-query";
import { listMembers } from "../api/members";

export const memberKeys = {
  all: ["org", "members"] as const
};

export function useOrgMembers() {
  return useQuery({
    queryKey: memberKeys.all,
    queryFn: () => listMembers()
  });
}
