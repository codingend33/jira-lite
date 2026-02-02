import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { createTicket, getTicket, listTickets, searchTickets, transitionTicket, updateTicket } from "../api/tickets";
import { TicketFilters } from "../api/tickets";

export const ticketKeys = {
  all: ["tickets"] as const,
  list: (filters: TicketFilters) => ["tickets", "list", filters] as const,
  detail: (ticketId: string) => ["tickets", "detail", ticketId] as const,
  search: (keyword: string) => ["tickets", "search", keyword] as const
};

export function useTickets(filters: TicketFilters) {
  return useQuery({
    queryKey: ticketKeys.list(filters),
    queryFn: () => listTickets(filters)
  });
}

export function useTicket(ticketId: string) {
  return useQuery({
    queryKey: ticketKeys.detail(ticketId),
    queryFn: () => getTicket(ticketId),
    enabled: Boolean(ticketId)
  });
}

export function useSearchTickets(keyword: string) {
  return useQuery({
    queryKey: ticketKeys.search(keyword),
    queryFn: () => searchTickets(keyword),
    enabled: keyword.trim().length > 0
  });
}

export function useCreateTicket() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: createTicket,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ticketKeys.all })
  });
}

export function useUpdateTicket() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({
      id,
      payload
    }: {
      id: string;
      payload: { title?: string; description?: string; priority?: string; assigneeId?: string | null };
    }) => updateTicket(id, payload),
    onSuccess: (_data, variables) => {
      queryClient.invalidateQueries({ queryKey: ticketKeys.detail(variables.id) });
      queryClient.invalidateQueries({ queryKey: ticketKeys.all });
    }
  });
}

export function useTransitionTicket() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, status }: { id: string; status: string }) => transitionTicket(id, status),
    onSuccess: (_data, variables) => {
      queryClient.invalidateQueries({ queryKey: ticketKeys.detail(variables.id) });
      queryClient.invalidateQueries({ queryKey: ticketKeys.all });
    }
  });
}
