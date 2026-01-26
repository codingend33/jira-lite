import { getAccessToken, clearTokens } from "../auth/storage";
import { ErrorResponse } from "./types";

const baseUrl = import.meta.env.VITE_API_BASE_URL;

export class ApiError extends Error {
  status: number;
  payload?: ErrorResponse;

  constructor(status: number, message: string, payload?: ErrorResponse) {
    super(message);
    this.status = status;
    this.payload = payload;
  }
}

async function parseJson(response: Response) {
  const text = await response.text();
  if (!text) {
    return null;
  }
  try {
    return JSON.parse(text);
  } catch {
    return null;
  }
}

export async function apiRequest<T>(
  path: string,
  options: RequestInit = {}
): Promise<T> {
  const token = getAccessToken();
  const headers = new Headers(options.headers ?? {});
  headers.set("Accept", "application/json");
  if (options.body && !headers.has("Content-Type")) {
    headers.set("Content-Type", "application/json");
  }
  if (token) {
    headers.set("Authorization", `Bearer ${token}`);
  }

  const url = baseUrl ? `${baseUrl.replace(/\/$/, "")}/${path.replace(/^\//, "")}` : path;

  const response = await fetch(url, {
    ...options,
    headers
  });

  if (response.status === 401) {
    clearTokens();
    window.dispatchEvent(new CustomEvent("api:auth-failed", { detail: { path } }));
  }
  if (response.status === 403) {
    window.dispatchEvent(new CustomEvent("api:forbidden", { detail: { path } }));
  }

  if (!response.ok) {
    const payload = (await parseJson(response)) as ErrorResponse | null;
    throw new ApiError(
      response.status,
      payload?.message ?? "Request failed",
      payload ?? undefined
    );
  }

  const json = await parseJson(response);
  return json as T;
}
