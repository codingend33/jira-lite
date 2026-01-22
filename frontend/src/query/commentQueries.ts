import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { createComment, listComments } from "../api/comments";

export const commentKeys = {
  list: (ticketId: string) => ["tickets", ticketId, "comments"] as const
};

export function useComments(ticketId: string) {
  return useQuery({
    queryKey: commentKeys.list(ticketId),
    queryFn: () => listComments(ticketId),
    enabled: Boolean(ticketId)
  });
}

export function useCreateComment(ticketId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (body: string) => createComment(ticketId, body),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: commentKeys.list(ticketId) })
  });
}
