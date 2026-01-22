import { apiRequest } from "./client";
import { Project } from "./types";

export async function listProjects(): Promise<Project[]> {
  return apiRequest<Project[]>("/projects");
}

export async function getProject(projectId: string): Promise<Project> {
  return apiRequest<Project>(`/projects/${projectId}`);
}

export async function createProject(payload: {
  key: string;
  name: string;
  description?: string;
}): Promise<Project> {
  return apiRequest<Project>("/projects", {
    method: "POST",
    body: JSON.stringify(payload)
  });
}

export async function updateProject(
  projectId: string,
  payload: { name?: string; description?: string }
): Promise<Project> {
  return apiRequest<Project>(`/projects/${projectId}`, {
    method: "PATCH",
    body: JSON.stringify(payload)
  });
}

export async function deleteProject(projectId: string): Promise<void> {
  await apiRequest<void>(`/projects/${projectId}`, { method: "DELETE" });
}

export async function archiveProject(projectId: string): Promise<Project> {
  return apiRequest<Project>(`/projects/${projectId}/archive`, { method: "POST" });
}

export async function unarchiveProject(projectId: string): Promise<Project> {
  return apiRequest<Project>(`/projects/${projectId}/unarchive`, { method: "POST" });
}
