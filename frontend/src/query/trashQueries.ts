import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
    listTrash,
    restoreProject,
    restoreTicket,
    TrashItem
} from "../api/trash";
import { projectKeys } from "./projectQueries";

export const trashKeys = {
    all: ["trash"] as const,
    list: (type: string) => [...trashKeys.all, type] as const
};

export function useTrash(type: "all" | "project" | "ticket" = "all") {
    return useQuery<TrashItem[]>({
        queryKey: trashKeys.list(type),
        queryFn: () => listTrash(type)
    });
}

export function useRestoreProject() {
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn: restoreProject,
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: trashKeys.all });
            queryClient.invalidateQueries({ queryKey: projectKeys.all });
        }
    });
}

export function useRestoreTicket() {
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn: restoreTicket,
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: trashKeys.all });
            queryClient.invalidateQueries({ queryKey: ["tickets"] });
        }
    });
}

export function useSoftDeleteTicket() {
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn: ({ id, reason }: { id: string; reason?: string }) => import("../api/trash").then(m => m.deleteTicket(id, reason)),
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: ["tickets"] });
            queryClient.invalidateQueries({ queryKey: trashKeys.all });
        }
    });
}
