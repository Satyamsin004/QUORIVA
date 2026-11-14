import axios from 'axios';
import { apiClient } from './apiClient';
import {
  ApiResponse,
  PageResponse,
  LibraryResourceSummary,
  LibraryResourceDetail,
  LibraryUploadSlotRequest,
  LibraryUploadSlotResponse,
  CreateLibrarySubmissionPayload,
  UpdateLibrarySubmissionPayload,
  ModerationActionPayload,
  ModerationLog,
  CreateReportPayload,
  ReportResolutionPayload,
  LibraryReport,
  LibraryDownloadUrlResponse,
  OptionItem,
  AcademicCategory,
  LibraryResourceType,
  AcademicLevel,
  ReportStatus,
} from '@/types';

export interface SearchLibraryParams {
  q?: string;
  category?: AcademicCategory;
  resourceType?: LibraryResourceType;
  academicLevel?: AcademicLevel;
  tag?: string;
  sort?: 'RECENT' | 'POPULAR' | 'TITLE';
  page?: number;
  size?: number;
}

export const libraryService = {
  async searchResources(params: SearchLibraryParams = {}): Promise<PageResponse<LibraryResourceSummary>> {
    const res = await apiClient.get<ApiResponse<PageResponse<LibraryResourceSummary>>>('/library/resources', {
      params,
    });
    return res.data.data;
  },

  async getResourceDetail(id: string): Promise<LibraryResourceDetail> {
    const res = await apiClient.get<ApiResponse<LibraryResourceDetail>>(`/library/resources/${id}`);
    return res.data.data;
  },

  async downloadResource(id: string): Promise<LibraryDownloadUrlResponse> {
    const res = await apiClient.get<ApiResponse<LibraryDownloadUrlResponse>>(`/library/resources/${id}/download`);
    return res.data.data;
  },

  async requestUploadSlot(req: LibraryUploadSlotRequest): Promise<LibraryUploadSlotResponse> {
    const res = await apiClient.post<ApiResponse<LibraryUploadSlotResponse>>('/library/submissions/upload-url', req);
    return res.data.data;
  },

  async uploadFileToPresignedUrl(
    uploadUrl: string,
    file: File,
    contentType: string,
    onProgress?: (percent: number) => void
  ): Promise<void> {
    await axios.put(uploadUrl, file, {
      headers: {
        'Content-Type': contentType,
      },
      onUploadProgress: (progressEvent) => {
        if (progressEvent.total && onProgress) {
          const percent = Math.round((progressEvent.loaded * 100) / progressEvent.total);
          onProgress(percent);
        }
      },
    });
  },

  async createSubmission(payload: CreateLibrarySubmissionPayload): Promise<LibraryResourceDetail> {
    const res = await apiClient.post<ApiResponse<LibraryResourceDetail>>('/library/submissions', payload);
    return res.data.data;
  },

  async updateSubmission(id: string, payload: UpdateLibrarySubmissionPayload): Promise<LibraryResourceDetail> {
    const res = await apiClient.put<ApiResponse<LibraryResourceDetail>>(`/library/submissions/${id}`, payload);
    return res.data.data;
  },

  async submitForReview(id: string): Promise<LibraryResourceDetail> {
    const res = await apiClient.post<ApiResponse<LibraryResourceDetail>>(`/library/submissions/${id}/submit`);
    return res.data.data;
  },

  async withdrawSubmission(id: string): Promise<LibraryResourceDetail> {
    const res = await apiClient.post<ApiResponse<LibraryResourceDetail>>(`/library/submissions/${id}/withdraw`);
    return res.data.data;
  },

  async deleteSubmission(id: string): Promise<void> {
    await apiClient.delete<ApiResponse<void>>(`/library/submissions/${id}`);
  },

  async getMySubmissions(page = 0, size = 10): Promise<PageResponse<LibraryResourceSummary>> {
    const res = await apiClient.get<ApiResponse<PageResponse<LibraryResourceSummary>>>('/library/my-submissions', {
      params: { page, size },
    });
    return res.data.data;
  },

  // Moderation APIs
  async getModerationQueue(page = 0, size = 15): Promise<PageResponse<LibraryResourceSummary>> {
    const res = await apiClient.get<ApiResponse<PageResponse<LibraryResourceSummary>>>('/library/moderation/queue', {
      params: { page, size },
    });
    return res.data.data;
  },

  async approveResource(id: string, payload?: ModerationActionPayload): Promise<LibraryResourceDetail> {
    const res = await apiClient.post<ApiResponse<LibraryResourceDetail>>(`/library/moderation/resources/${id}/approve`, payload || {});
    return res.data.data;
  },

  async rejectResource(id: string, payload: ModerationActionPayload): Promise<LibraryResourceDetail> {
    const res = await apiClient.post<ApiResponse<LibraryResourceDetail>>(`/library/moderation/resources/${id}/reject`, payload);
    return res.data.data;
  },

  async suspendResource(id: string, payload: ModerationActionPayload): Promise<LibraryResourceDetail> {
    const res = await apiClient.post<ApiResponse<LibraryResourceDetail>>(`/library/moderation/resources/${id}/suspend`, payload);
    return res.data.data;
  },

  async restoreResource(id: string, payload?: ModerationActionPayload): Promise<LibraryResourceDetail> {
    const res = await apiClient.post<ApiResponse<LibraryResourceDetail>>(`/library/moderation/resources/${id}/restore`, payload || {});
    return res.data.data;
  },

  async getResourceModerationLogs(id: string): Promise<ModerationLog[]> {
    const res = await apiClient.get<ApiResponse<ModerationLog[]>>(`/library/moderation/resources/${id}/logs`);
    return res.data.data;
  },

  async getGlobalModerationLogs(page = 0, size = 20): Promise<PageResponse<ModerationLog>> {
    const res = await apiClient.get<ApiResponse<PageResponse<ModerationLog>>>('/library/moderation/logs', {
      params: { page, size },
    });
    return res.data.data;
  },

  // Reporting APIs
  async submitReport(resourceId: string, payload: CreateReportPayload): Promise<LibraryReport> {
    const res = await apiClient.post<ApiResponse<LibraryReport>>(`/library/resources/${resourceId}/reports`, payload);
    return res.data.data;
  },

  async getModerationReports(status?: ReportStatus, page = 0, size = 15): Promise<PageResponse<LibraryReport>> {
    const res = await apiClient.get<ApiResponse<PageResponse<LibraryReport>>>('/library/moderation/reports', {
      params: { status, page, size },
    });
    return res.data.data;
  },

  async resolveReport(reportId: string, payload: ReportResolutionPayload): Promise<LibraryReport> {
    const res = await apiClient.post<ApiResponse<LibraryReport>>(`/library/moderation/reports/${reportId}/resolve`, payload);
    return res.data.data;
  },

  // Taxonomy & Metadata
  async getCategories(): Promise<OptionItem[]> {
    const res = await apiClient.get<ApiResponse<OptionItem[]>>('/library/categories');
    return res.data.data;
  },

  async getResourceTypes(): Promise<OptionItem[]> {
    const res = await apiClient.get<ApiResponse<OptionItem[]>>('/library/resource-types');
    return res.data.data;
  },

  async getRightsTypes(): Promise<OptionItem[]> {
    const res = await apiClient.get<ApiResponse<OptionItem[]>>('/library/rights-types');
    return res.data.data;
  },

  async getAcademicLevels(): Promise<OptionItem[]> {
    const res = await apiClient.get<ApiResponse<OptionItem[]>>('/library/academic-levels');
    return res.data.data;
  },

  async getPopularTags(limit = 15): Promise<string[]> {
    const res = await apiClient.get<ApiResponse<string[]>>('/library/tags/popular', {
      params: { limit },
    });
    return res.data.data;
  },
};
