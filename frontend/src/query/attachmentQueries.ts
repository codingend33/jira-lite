import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  confirmUpload,
  deleteAttachment,
  listAttachments,
  presignDownload,
  presignUpload,
  uploadToS3
} from "../api/attachments";

export const attachmentKeys = {
  list: (ticketId: string) => ["tickets", ticketId, "attachments"] as const
};

export function useAttachments(ticketId: string) {
  return useQuery({
    queryKey: attachmentKeys.list(ticketId),
    queryFn: () => listAttachments(ticketId),
    enabled: Boolean(ticketId)
  });
}

export function useUploadAttachment(ticketId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (file: File) => {
      const presign = await presignUpload(ticketId, {
        fileName: file.name,
        contentType: file.type,
        fileSize: file.size
      });
      await uploadToS3(presign.uploadUrl, file, presign.headers);
      await confirmUpload(ticketId, presign.attachmentId);
      return presign.attachmentId;
    },
    onSuccess: () => queryClient.invalidateQueries({ queryKey: attachmentKeys.list(ticketId) })
  });
}

export function useDownloadAttachment(ticketId: string) {
  return useMutation({
    mutationFn: (attachmentId: string) => presignDownload(ticketId, attachmentId)
  });
}

export function useDeleteAttachment(ticketId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (attachmentId: string) => deleteAttachment(ticketId, attachmentId),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: attachmentKeys.list(ticketId) })
  });
}
