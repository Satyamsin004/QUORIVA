import { apiClient } from './apiClient';
import {
  ApiResponse,
  PageResponse,
  AcademicSubject,
  KnowledgeTopicSummary,
  KnowledgeTopicDetail,
  KnowledgeGraph,
  LearningPath,
  SearchTopicsParams,
} from '@/types';

export const knowledgeService = {
  async getSubjects(): Promise<AcademicSubject[]> {
    const res = await apiClient.get<ApiResponse<AcademicSubject[]>>('/knowledge/subjects');
    return res.data.data;
  },

  async getSubjectBySlug(slug: string): Promise<AcademicSubject> {
    const res = await apiClient.get<ApiResponse<AcademicSubject>>(`/knowledge/subjects/slug/${encodeURIComponent(slug)}`);
    return res.data.data;
  },

  async getTopicsBySubject(subjectId: string): Promise<KnowledgeTopicSummary[]> {
    const res = await apiClient.get<ApiResponse<KnowledgeTopicSummary[]>>(`/knowledge/subjects/${subjectId}/topics`);
    return res.data.data;
  },

  async searchTopics(params: SearchTopicsParams = {}): Promise<PageResponse<KnowledgeTopicSummary>> {
    const res = await apiClient.get<ApiResponse<PageResponse<KnowledgeTopicSummary>>>('/knowledge/topics', {
      params,
    });
    return res.data.data;
  },

  async getTopicDetail(idOrSlug: string): Promise<KnowledgeTopicDetail> {
    const res = await apiClient.get<ApiResponse<KnowledgeTopicDetail>>(`/knowledge/topics/${encodeURIComponent(idOrSlug)}`);
    return res.data.data;
  },

  async getFullGraph(subjectId?: string, maxNodes = 50): Promise<KnowledgeGraph> {
    const params: Record<string, string | number> = { maxNodes };
    if (subjectId) {
      params.subjectId = subjectId;
    }
    const res = await apiClient.get<ApiResponse<KnowledgeGraph>>('/knowledge/graph', { params });
    return res.data.data;
  },

  async getTopicNeighborhoodGraph(idOrSlug: string, depth = 1): Promise<KnowledgeGraph> {
    const res = await apiClient.get<ApiResponse<KnowledgeGraph>>(
      `/knowledge/graph/topics/${encodeURIComponent(idOrSlug)}`,
      { params: { depth } }
    );
    return res.data.data;
  },

  async getLearningPath(idOrSlug: string): Promise<LearningPath> {
    const res = await apiClient.get<ApiResponse<LearningPath>>(
      `/knowledge/topics/${encodeURIComponent(idOrSlug)}/learning-path`
    );
    return res.data.data;
  },

  async getCuratedLearningPaths(): Promise<LearningPath[]> {
    const res = await apiClient.get<ApiResponse<LearningPath[]>>('/knowledge/learning-paths');
    return res.data.data;
  },
};

