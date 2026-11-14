const apiBase = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api/v1';
const serverRoot = apiBase.replace(/\/api\/v\d+.*$/, '');

export const APP_CONFIG = {
  name: import.meta.env.VITE_APP_NAME || 'QUORIVA',
  tagline: 'Understand what you study.',
  description: 'Your personal AI learning space.',
  environment: import.meta.env.VITE_APP_ENV || 'production',
  version: import.meta.env.VITE_APP_VERSION || '1.0.0',
  apiBaseUrl: apiBase,
  backendHealthUrl: `${serverRoot}/actuator/health`,
};
