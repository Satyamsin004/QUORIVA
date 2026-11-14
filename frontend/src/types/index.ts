export * from './auth';
export * from './workspace';
export * from './document';
export * from './search';
export * from './chat';
export * from './library';
export * from './knowledge';
export * from './learning';
export * from './system';

export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
  timestamp: string;
}

export interface PageResponse<T> {
  content: T[];
  totalElements?: number;
  totalPages?: number;
  number?: number;
  size?: number;
  page?: {
    size: number;
    number: number;
    totalElements: number;
    totalPages: number;
  };
}
