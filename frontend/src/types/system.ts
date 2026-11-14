export type ServiceStatus = 'healthy' | 'unhealthy' | 'checking' | 'offline';

export interface ServiceHealth {
  name: string;
  status: ServiceStatus;
  url?: string;
  details?: Record<string, unknown>;
  latencyMs?: number;
}

export interface SystemStatusState {
  frontend: ServiceHealth;
  backend: ServiceHealth;
  aiService: ServiceHealth;
}

