import { apiRequest } from "./client";
import { Attachment, PresignDownloadResponse, PresignUploadResponse } from "./types";

export async function listAttachments(ticketId: string): Promise<Attachment[]> {
  return apiRequest<Attachment[]>(`/tickets/${ticketId}/attachments`);
}

export async function presignUpload(
  ticketId: string,
  payload: { fileName: string; contentType: string; fileSize: number }
): Promise<PresignUploadResponse> {
  return apiRequest<PresignUploadResponse>(`/tickets/${ticketId}/attachments/presign-upload`, {
    method: "POST",
    body: JSON.stringify(payload)
  });
}

export async function confirmUpload(ticketId: string, attachmentId: string): Promise<Attachment> {
  return apiRequest<Attachment>(`/tickets/${ticketId}/attachments/${attachmentId}/confirm`, {
    method: "POST"
  });
}

export async function presignDownload(
  ticketId: string,
  attachmentId: string
): Promise<PresignDownloadResponse> {
  return apiRequest<PresignDownloadResponse>(
    `/tickets/${ticketId}/attachments/${attachmentId}/presign-download`
  );
}

export async function deleteAttachment(ticketId: string, attachmentId: string): Promise<void> {
  await apiRequest<void>(`/tickets/${ticketId}/attachments/${attachmentId}`, {
    method: "DELETE"
  });
}

export async function uploadToS3(
  uploadUrl: string,
  file: File,
  headers: Record<string, string>
): Promise<void> {
  const response = await fetch(uploadUrl, {
    method: "PUT",
    headers,
    body: file
  });
  if (!response.ok) {
    throw new Error("Failed to upload attachment");
  }
}
