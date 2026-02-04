export type ErrorResponse = {
  code: string;
  message: string;
  traceId?: string;
};

export type PageMeta = {
  number: number;
  size: number;
  totalElements: number;
  totalPages: number;
};

export type PagedResponse<T> = {
  content: T[];
  page: PageMeta;
};

export type Project = {
  id: string;
  key: string;
  name: string;
  description?: string | null;
  status: string;
  createdBy?: string | null;
  createdAt: string;
  updatedAt: string;
};

export type Ticket = {
  id: string;
  projectId: string;
  key: string;
  title: string;
  description?: string | null;
  status: string;
  priority: string;
  assigneeId?: string | null;
  createdBy?: string | null;
  createdAt: string;
  updatedAt: string;
};

export type Comment = {
  id: string;
  authorId?: string | null;
  body: string;
  createdAt: string;
  updatedAt: string;
};

export type Attachment = {
  id: string;
  fileName: string;
  contentType: string;
  fileSize: number;
  status: string;
  createdAt: string;
  updatedAt: string;
};

export type Member = {
  userId: string;
  email?: string | null;
  displayName?: string | null;
  avatarUrl?: string | null;
  role: string;
  status: string;
};

export type PresignUploadResponse = {
  attachmentId: string;
  uploadUrl: string;
  headers: Record<string, string>;
  expiresAt: string;
};

export type PresignDownloadResponse = {
  attachmentId: string;
  downloadUrl: string;
  expiresAt: string;
};
