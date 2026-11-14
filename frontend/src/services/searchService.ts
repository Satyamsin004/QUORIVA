import { apiClient } from './apiClient';
import { SearchResponse, ApiResponse } from '@/types';

export interface SearchParams {
  query: string;
  workspaceId: string;
  topK?: number;
  limit?: number;
}

export const searchService = {
  async search(
    queryOrParams: string | SearchParams,
    workspaceIdArg?: string,
    topKArg = 10
  ): Promise<SearchResponse> {
    let query: string;
    let workspaceId: string;
    let topK: number;

    if (typeof queryOrParams === 'object') {
      query = queryOrParams.query;
      workspaceId = queryOrParams.workspaceId;
      topK = queryOrParams.topK ?? queryOrParams.limit ?? 10;
    } else {
      query = queryOrParams;
      workspaceId = workspaceIdArg || '';
      topK = topKArg;
    }

    const res = await apiClient.post<ApiResponse<SearchResponse>>('/search', {
      query: query.trim(),
      workspaceId,
      topK,
    });
    return res.data.data;
  },
};
