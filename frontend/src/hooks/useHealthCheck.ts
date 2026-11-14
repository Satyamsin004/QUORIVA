import { useState, useEffect, useCallback } from 'react';
import { checkSystemHealth } from '@/services/healthService';
import { SystemStatusState } from '@/types';
import { APP_CONFIG } from '@/utils/constants';

export const useHealthCheck = () => {
  const [status, setStatus] = useState<SystemStatusState>({
    frontend: {
      name: 'React Frontend',
      status: 'healthy',
      url: window.location.origin,
      details: {
        environment: APP_CONFIG.environment,
        version: APP_CONFIG.version,
      },
      latencyMs: 1,
    },
    backend: {
      name: 'Spring Boot Backend',
      status: 'checking',
      url: APP_CONFIG.backendHealthUrl,
    },
    aiService: {
      name: 'FastAPI AI Service',
      status: 'checking',
      url: 'via Spring Boot Actuator',
    },
  });

  const [loading, setLoading] = useState(false);

  const runChecks = useCallback(async () => {
    setLoading(true);
    const result = await checkSystemHealth();

    setStatus((prev: SystemStatusState) => ({
      ...prev,
      backend: result.backend || prev.backend,
      aiService: result.aiService || prev.aiService,
    }));
    setLoading(false);
  }, []);

  useEffect(() => {
    runChecks();
  }, [runChecks]);

  return { status, loading, refresh: runChecks };
};
