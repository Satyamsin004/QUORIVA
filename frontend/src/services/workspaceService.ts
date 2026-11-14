import { apiClient } from './apiClient';
import { Workspace, CreateWorkspaceRequest, UpdateWorkspaceRequest, ApiResponse } from '@/types';

export const workspaceService = {
  async listWorkspaces(): Promise<Workspace[]> {
    const res = await apiClient.get<ApiResponse<Workspace[]>>('/workspaces');
    return res.data.data;
  },

  async getWorkspace(id: string): Promise<Workspace> {
    const res = await apiClient.get<ApiResponse<Workspace>>(`/workspaces/${id}`);
    return res.data.data;
  },

  async createWorkspace(name: string, description?: string): Promise<Workspace> {
    const payload: CreateWorkspaceRequest = { name, description };
    const res = await apiClient.post<ApiResponse<Workspace>>('/workspaces', payload);
    return res.data.data;
  },

  async updateWorkspace(id: string, name?: string, description?: string): Promise<Workspace> {
    const payload: UpdateWorkspaceRequest = { name, description };
    const res = await apiClient.patch<ApiResponse<Workspace>>(`/workspaces/${id}`, payload);
    return res.data.data;
  },

  async deleteWorkspace(id: string): Promise<void> {
    await apiClient.delete<ApiResponse<void>>(`/workspaces/${id}`);
  },
};

