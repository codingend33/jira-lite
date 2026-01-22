import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  archiveProject,
  createProject,
  deleteProject,
  getProject,
  listProjects,
  unarchiveProject,
  updateProject
} from "../api/projects";

export const projectKeys = {
  all: ["projects"] as const
};

export function useProjects() {
  return useQuery({
    queryKey: projectKeys.all,
    queryFn: listProjects
  });
}

export function useProject(projectId: string) {
  return useQuery({
    queryKey: [...projectKeys.all, projectId],
    queryFn: () => getProject(projectId),
    enabled: Boolean(projectId)
  });
}

export function useCreateProject() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: createProject,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: projectKeys.all })
  });
}

export function useUpdateProject() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, payload }: { id: string; payload: { name?: string; description?: string } }) =>
      updateProject(id, payload),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: projectKeys.all })
  });
}

export function useDeleteProject() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: deleteProject,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: projectKeys.all })
  });
}

export function useArchiveProject() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: archiveProject,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: projectKeys.all })
  });
}

export function useUnarchiveProject() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: unarchiveProject,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: projectKeys.all })
  });
}
