export type FileType = 'PDF' | 'DOCX' | 'PPTX';

export type ProcessingStatus =
  | 'PENDING_UPLOAD'
  | 'UPLOADED'
  | 'PENDING_PROCESSING'
  | 'PROCESSING'
  | 'PROCESSED'
  | 'FAILED'
  | 'DELETING'
  | 'DELETED';

export type EmbeddingStatus =
  | 'NOT_STARTED'
  | 'PENDING_EMBEDDING'
  | 'EMBEDDING_IN_PROGRESS'
  | 'PROCESSING'
  | 'EMBEDDED'
  | 'COMPLETED'
  | 'FAILED'
  | string;

export interface DocumentItem {
  id: string;
  workspaceId: string;
  workspaceName?: string;
  ownerId: string;
  ownerName?: string;
  title: string;
  originalFilename: string;
  originalFileName?: string;
  storageKey: string;
  fileType: FileType;
  mimeType: string;
  fileSize: number;
  checksumSha256?: string;
  fileHash?: string;
  pageCount?: number;
  description?: string;
  subject?: string;
  processingStatus: ProcessingStatus;
  embeddingStatus?: EmbeddingStatus;
  visibility?: string;
  createdAt: string;
  updatedAt: string;
  uploadedAt?: string;
}

export interface UploadUrlRequest {
  workspaceId: string;
  filename: string;
  fileName?: string;
  contentType?: string;
  mimeType?: string;
  fileSize: number;
  fileType?: FileType;
  checksumSha256?: string;
  title?: string;
  description?: string;
  subject?: string;
}

export interface UploadUrlResponse {
  documentId: string;
  uploadUrl: string;
  storageKey: string;
  expiresInSeconds: number;
  headers?: Record<string, string>;
}

export interface CompleteUploadRequest {
  checksumSha256?: string;
}

export interface DocumentStatusResponse {
  documentId: string;
  processingStatus: ProcessingStatus;
  embeddingStatus?: string;
  pageCount?: number;
  errorMessage?: string;
  uploadedAt?: string;
  updatedAt: string;
}

export interface DownloadUrlResponse {
  documentId: string;
  downloadUrl: string;
  expiresInSeconds: number;
  filename?: string;
}
