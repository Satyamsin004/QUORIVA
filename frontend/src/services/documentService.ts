import axios from 'axios';
import { apiClient } from './apiClient';
import {
  DocumentItem,
  UploadUrlRequest,
  UploadUrlResponse,
  CompleteUploadRequest,
  DocumentStatusResponse,
  DownloadUrlResponse,
  ApiResponse,
  PageResponse,
} from '@/types';

export interface ListDocumentsParams {
  workspaceId: string;
  page?: number;
  size?: number;
  query?: string;
  fileType?: string;
  status?: string;
}

export const documentService = {
  async getUploadUrl(req: UploadUrlRequest): Promise<UploadUrlResponse> {
    const payload = {
      workspaceId: req.workspaceId,
      filename: req.filename || req.fileName,
      contentType: req.contentType || req.mimeType || 'application/octet-stream',
      fileSize: req.fileSize,
      checksumSha256: req.checksumSha256,
      title: req.title,
      description: req.description,
      subject: req.subject,
    };
    const res = await apiClient.post<ApiResponse<UploadUrlResponse>>('/documents/upload-url', payload, {
      timeout: 180000,
    });
    return res.data.data;
  },

  async uploadFileToS3(
    uploadUrl: string,
    file: File,
    headersOrProgress?: Record<string, string> | ((percent: number) => void),
    onProgressCallback?: (percent: number) => void
  ): Promise<void> {
    let headers: Record<string, string> | undefined;
    let onProgress: ((percent: number) => void) | undefined;

    if (typeof headersOrProgress === 'function') {
      onProgress = headersOrProgress;
    } else {
      headers = headersOrProgress;
      onProgress = onProgressCallback;
    }

    // S3 presigned PUT requires direct axios call WITHOUT the backend Bearer token header
    const uploadHeaders: Record<string, string> = {
      'Content-Type': file.type || 'application/octet-stream',
      ...(headers || {}),
    };

    await axios.put(uploadUrl, file, {
      headers: uploadHeaders,
      timeout: 300000,
      onUploadProgress: (progressEvent) => {
        if (progressEvent.total && onProgress) {
          const percent = Math.round((progressEvent.loaded * 100) / progressEvent.total);
          onProgress(percent);
        }
      },
    });
  },

  async completeUpload(
    documentIdOrPayload: string | { documentId: string; checksumSha256?: string; uploadSuccessful?: boolean; fileSize?: number; subject?: string },
    checksumSha256?: string
  ): Promise<DocumentItem> {
    let docId: string;
    let payload: CompleteUploadRequest = {};

    if (typeof documentIdOrPayload === 'string') {
      docId = documentIdOrPayload;
      if (checksumSha256) {
        payload = { checksumSha256 };
      }
    } else {
      docId = documentIdOrPayload.documentId;
      if (documentIdOrPayload.checksumSha256) {
        payload = { checksumSha256: documentIdOrPayload.checksumSha256 };
      }
    }

    const res = await apiClient.post<ApiResponse<DocumentItem>>(
      `/documents/${docId}/complete-upload`,
      payload,
      { timeout: 300000 }
    );
    return res.data.data;
  },

  async listDocuments(
    workspaceIdOrParams: string | ListDocumentsParams,
    pageArg = 0,
    sizeArg = 20,
    queryArg?: string,
    fileTypeArg?: string,
    statusArg?: string
  ): Promise<PageResponse<DocumentItem>> {
    let workspaceId: string;
    let page = pageArg;
    let size = sizeArg;
    let query = queryArg;
    let fileType = fileTypeArg;
    let status = statusArg;

    if (typeof workspaceIdOrParams === 'object') {
      workspaceId = workspaceIdOrParams.workspaceId;
      page = workspaceIdOrParams.page ?? 0;
      size = workspaceIdOrParams.size ?? 20;
      query = workspaceIdOrParams.query;
      fileType = workspaceIdOrParams.fileType;
      status = workspaceIdOrParams.status;
    } else {
      workspaceId = workspaceIdOrParams;
    }

    let url = `/documents?workspaceId=${workspaceId}&page=${page}&size=${size}`;
    if (query && query.trim()) {
      url += `&query=${encodeURIComponent(query.trim())}`;
    }
    if (fileType) {
      url += `&fileType=${encodeURIComponent(fileType)}`;
    }
    if (status) {
      url += `&status=${encodeURIComponent(status)}`;
    }

    const res = await apiClient.get<ApiResponse<PageResponse<DocumentItem>>>(url);
    return res.data.data;
  },

  async getDocument(documentId: string): Promise<DocumentItem> {
    const res = await apiClient.get<ApiResponse<DocumentItem>>(`/documents/${documentId}`);
    return res.data.data;
  },

  async getDocumentStatus(documentId: string): Promise<DocumentStatusResponse> {
    const res = await apiClient.get<ApiResponse<DocumentStatusResponse>>(`/documents/${documentId}/status`);
    return res.data.data;
  },

  async getDownloadUrl(documentId: string): Promise<DownloadUrlResponse> {
    const res = await apiClient.get<ApiResponse<DownloadUrlResponse>>(`/documents/${documentId}/download-url`);
    return res.data.data;
  },

  async deleteDocument(documentId: string): Promise<void> {
    await apiClient.delete<ApiResponse<void>>(`/documents/${documentId}`);
  },
};
