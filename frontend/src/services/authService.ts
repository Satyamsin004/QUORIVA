import { apiClient } from './apiClient';
import {
  AuthResponse,
  LoginRequest,
  RegisterRequest,
  UpdateUserRequest,
  User,
  ApiResponse,
} from '@/types';

export const authService = {
  async register(req: RegisterRequest): Promise<AuthResponse> {
    const res = await apiClient.post<ApiResponse<AuthResponse>>('/auth/register', req);
    return res.data.data;
  },

  async login(req: LoginRequest): Promise<AuthResponse> {
    const res = await apiClient.post<ApiResponse<AuthResponse>>('/auth/login', req);
    return res.data.data;
  },

  async refreshToken(): Promise<AuthResponse> {
    const res = await apiClient.post<ApiResponse<AuthResponse>>('/auth/refresh', {});
    return res.data.data;
  },

  async logout(): Promise<void> {
    try {
      await apiClient.post<ApiResponse<void>>('/auth/logout', {});
    } catch (e) {
      console.warn('Logout API call failed or token expired:', e);
    }
  },

  async getMe(): Promise<User> {
    const res = await apiClient.get<ApiResponse<User>>('/auth/me');
    return res.data.data;
  },

  async updateProfile(req: UpdateUserRequest): Promise<User> {
    const res = await apiClient.patch<ApiResponse<User>>('/users/me', req);
    return res.data.data;
  },
};
