import { apiRequest } from './client';
import { PagedResponse } from './types';

const baseUrl = import.meta.env.VITE_API_BASE_URL;

export type Notification = {
  id: string;
  content: string;
  createdAt: string;
  read?: boolean;
  isRead?: boolean;
};

export async function listNotifications(params?: { page?: number; size?: number }): Promise<PagedResponse<Notification>> {
  const search = new URLSearchParams();
  if (params?.page !== undefined) search.set('page', String(params.page));
  if (params?.size !== undefined) search.set('size', String(params.size));
  const qs = search.toString();
  const url = qs ? `/notifications?${qs}` : '/notifications';
  return apiRequest(url, { method: 'GET' });
}

export async function markNotificationRead(id: string): Promise<void> {
  await apiRequest(`/notifications/${id}/read`, { method: 'PATCH' });
}

export type NotificationEvent =
  | { type: "notification"; data: Notification }
  | { type: "connected" };

// Manual SSE using fetch so we can attach Authorization header.
export async function connectNotificationStream(accessToken: string, onEvent: (ev: NotificationEvent) => void) {
  const path = "/notifications/stream";
  const url = baseUrl ? `${baseUrl.replace(/\/$/, "")}${path}` : path;
  const resp = await fetch(url, {
    headers: {
      Authorization: `Bearer ${accessToken}`,
      Accept: "text/event-stream"
    }
  });
  // 即便后端未显式发送 connected 事件，先行触发一次，便于前端状态
  onEvent({ type: "connected" });
  if (!resp.ok || !resp.body) {
    throw new Error(`Stream error: ${resp.status}`);
  }
  const reader = resp.body.getReader();
  const decoder = new TextDecoder();
  let buffer = "";
  let done = false;
  while (!done) {
    const { value, done: isDone } = await reader.read();
    done = isDone;
    if (done || !value) break;
    buffer += decoder.decode(value, { stream: true });
    const lines = buffer.split("\n");
    buffer = lines.pop() ?? "";
    let eventType = "message";
    let dataStr = "";
    for (const line of lines) {
      if (line.startsWith("event:")) eventType = line.slice(6).trim();
      if (line.startsWith("data:")) dataStr += line.slice(5).trim();
    }
    if (eventType === "notification" && dataStr) {
      onEvent({ type: "notification", data: JSON.parse(dataStr) });
    } else if (eventType === "connected") {
      onEvent({ type: "connected" });
    }
  }
}
