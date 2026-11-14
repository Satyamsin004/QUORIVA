import { apiClient } from './apiClient';
import { Conversation, ChatMessage } from '@/types/chat';
import { ApiResponse, PageResponse } from '@/types';

export const chatService = {
  async listConversations(workspaceId: string, page = 0, size = 20): Promise<PageResponse<Conversation>> {
    const res = await apiClient.get<ApiResponse<PageResponse<Conversation>>>(
      `/conversations?workspaceId=${workspaceId}&page=${page}&size=${size}`
    );
    return res.data.data;
  },

  async createConversation(workspaceId: string, title?: string): Promise<Conversation> {
    const res = await apiClient.post<ApiResponse<Conversation>>('/conversations', {
      workspaceId,
      title: title || 'New Conversation',
    });
    return res.data.data;
  },

  async getConversation(id: string): Promise<Conversation> {
    const res = await apiClient.get<ApiResponse<Conversation>>(`/conversations/${id}`);
    return res.data.data;
  },

  async deleteConversation(id: string): Promise<void> {
    await apiClient.delete<ApiResponse<void>>(`/conversations/${id}`);
  },

  async getMessages(conversationId: string): Promise<ChatMessage[]> {
    const res = await apiClient.get<ApiResponse<ChatMessage[]>>(`/conversations/${conversationId}/messages`);
    return res.data.data;
  },

  async sendMessage(
    conversationId: string,
    content: string,
    provider?: string,
    modelName?: string
  ): Promise<ChatMessage> {
    const res = await apiClient.post<ApiResponse<ChatMessage>>(`/conversations/${conversationId}/messages`, {
      content,
      provider: provider || undefined,
      modelName: modelName || undefined,
    });
    return res.data.data;
  },
};

