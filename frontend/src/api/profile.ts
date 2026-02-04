import { apiRequest } from "./client";

export interface Profile {
  id: string;
  email: string;
  displayName?: string;
  avatarS3Key?: string;
  lastLoginAt?: string;
}

export interface UpdateProfileRequest {
  displayName?: string;
  avatarS3Key?: string;
}

export async function getProfile(): Promise<Profile> {
  return apiRequest("/users/me/profile", { method: "GET" });
}

export async function updateProfile(data: UpdateProfileRequest): Promise<Profile> {
  return apiRequest("/users/me/profile", {
    method: "POST",
    body: JSON.stringify(data)
  });
}

export async function syncLogin(): Promise<void> {
  await apiRequest("/users/me/login-sync", { method: "POST" });
}

export async function presignAvatar(fileName: string, contentType: string) {
  const params = new URLSearchParams({ fileName, contentType });
  return apiRequest<{ uploadUrl: string; headers: Record<string, string>; key: string }>(
    `/users/me/avatar/presign?${params.toString()}`,
    { method: "GET" }
  );
}

export async function getAvatarUrl(): Promise<string | null> {
  try {
    const res = await apiRequest<{ url: string }>("/users/me/avatar-url", { method: "GET" });
    return res.url;
  } catch {
    return null;
  }
}
