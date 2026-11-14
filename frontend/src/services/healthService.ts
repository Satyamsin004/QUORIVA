import axios from 'axios';
import { APP_CONFIG } from '@/utils/constants';
import { ServiceHealth, SystemStatusState } from '@/types';

export const checkSystemHealth = async (): Promise<Partial<SystemStatusState>> => {
  const start = performance.now();
  try {
    const res = await axios.get(APP_CONFIG.backendHealthUrl, {
      timeout: 3500,
      validateStatus: (status) => status < 600,
    });
    const latency = Math.round(performance.now() - start);

    const components = res.data?.components || {};
    const backendIsRunning = res.status === 200 || res.status === 503;

    const backendHealth: ServiceHealth = {
      name: 'Spring Boot Backend',
      status: backendIsRunning ? 'healthy' : 'unhealthy',
      url: APP_CONFIG.backendHealthUrl,
      details: {
        overallStatus: res.data?.status || 'UNKNOWN',
        httpStatus: res.status,
      },
      latencyMs: latency,
    };

    // AI service health reported via Spring Boot Actuator
    const aiComponent = components.aiService;
    let aiStatus: ServiceHealth['status'] = 'checking';
    if (aiComponent) {
      aiStatus = aiComponent.status === 'UP' ? 'healthy' : 'unhealthy';
    } else if (!backendIsRunning) {
      aiStatus = 'offline';
    }

    const aiHealth: ServiceHealth = {
      name: 'FastAPI AI Service',
      status: aiStatus,
      url: 'via Spring Boot Actuator',
      details: aiComponent?.details || { status: 'Not monitored or unavailable' },
    };

    return {
      backend: backendHealth,
      aiService: aiHealth,
    };
  } catch (error: unknown) {
    const errorMsg = error instanceof Error ? error.message : 'Connection failed';
    return {
      backend: {
        name: 'Spring Boot Backend',
        status: 'offline',
        url: APP_CONFIG.backendHealthUrl,
        details: { error: errorMsg },
      },
      aiService: {
        name: 'FastAPI AI Service',
        status: 'offline',
        url: 'via Spring Boot Actuator',
        details: { error: 'Backend unreachable' },
      },
    };
  }
};
