import React from 'react';
import { useHealthCheck } from '@/hooks/useHealthCheck';
import { StatusBadge } from '@/components/common/StatusBadge';
import { Server, Cpu, Monitor, RefreshCw, Activity, ShieldCheck } from 'lucide-react';
import { APP_CONFIG } from '@/utils/constants';

export const SystemHealthPage: React.FC = () => {
  const { status, loading, refresh } = useHealthCheck();

  const services = [
    {
      id: 'frontend',
      title: 'Vite Frontend Client',
      stack: 'React 18 + TypeScript + Tailwind CSS',
      icon: Monitor,
      health: status.frontend,
      port: '5173',
      description: 'Single-page client interface with reactive state and token hydration.',
    },
    {
      id: 'backend',
      title: 'Spring Boot Application Server',
      stack: 'Java 21 + Spring Boot 3.3 + JPA + Redis',
      icon: Server,
      health: status.backend,
      port: '8080',
      description: 'Core REST APIs, JWT session security, and PostgreSQL transactions.',
    },
    {
      id: 'ai-service',
      title: 'FastAPI AI & RAG Retrieval Engine',
      stack: 'Python 3.10 + PyTorch + BAAI BGE + Groq',
      icon: Cpu,
      health: status.aiService,
      port: '8000 (Internal)',
      description: 'Document chunking, 768-dim vector embeddings, and cross-encoder reranking.',
    },
  ];

  return (
    <div className="p-6 sm:p-8 space-y-8 max-w-6xl mx-auto">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-6 border-b border-white/10">
        <div>
          <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-[#FF2D8D]/10 border border-[#FF2D8D]/20 text-[#FF2D8D] text-xs font-semibold uppercase tracking-wider mb-2">
            <Activity className="w-3.5 h-3.5" />
            Infrastructure & Telemetry
          </div>
          <h1 className="text-2xl sm:text-3xl font-bold text-white tracking-tight">
            System Health & Service Matrix
          </h1>
          <p className="text-xs sm:text-sm text-[#A1A1AA] mt-1">
            Real-time status probes and microservice connectivity across the QUORIVA platform.
          </p>
        </div>

        <button
          onClick={refresh}
          disabled={loading}
          className="inline-flex items-center gap-2 px-4 py-2.5 rounded-xl bg-[#171719] hover:bg-[#202025] border border-white/10 text-xs font-semibold text-white transition hover:border-[#FF2D8D]/40 disabled:opacity-50"
        >
          <RefreshCw className={`w-3.5 h-3.5 text-[#C7F000] ${loading ? 'animate-spin' : ''}`} />
          <span>Refresh Telemetry</span>
        </button>
      </div>

      {/* Services Grid */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        {services.map((svc) => {
          const Icon = svc.icon;
          const isUp = svc.health.status === 'healthy';

          return (
            <div
              key={svc.id}
              className="bg-[#171719] border border-white/10 rounded-2xl p-6 flex flex-col justify-between hover:border-white/20 transition-all shadow-card-dark relative group"
            >
              <div className="space-y-4">
                <div className="flex items-center justify-between">
                  <div className="w-10 h-10 rounded-xl bg-[#202025] border border-white/10 flex items-center justify-center text-[#FF2D8D] group-hover:scale-105 transition-transform">
                    <Icon className="w-5 h-5" />
                  </div>
                  <StatusBadge status={svc.health.status} />
                </div>

                <div>
                  <h3 className="text-base font-semibold text-white">{svc.title}</h3>
                  <div className="text-xs text-[#C7F000] font-mono mt-0.5">Port {svc.port}</div>
                </div>

                <p className="text-xs text-[#A1A1AA] leading-relaxed">
                  {svc.description}
                </p>
              </div>

              <div className="pt-4 mt-6 border-t border-white/5 flex items-center justify-between text-xs text-[#A1A1AA]">
                <span className="font-mono text-[11px] truncate max-w-[180px]">{svc.stack}</span>
                <span className="flex items-center gap-1 text-[11px]">
                  <span className={`w-1.5 h-1.5 rounded-full ${isUp ? 'bg-[#C7F000]' : 'bg-[#FF2D8D]'}`} />
                  {svc.health.latencyMs ? `${svc.health.latencyMs}ms` : 'Active'}
                </span>
              </div>
            </div>
          );
        })}
      </div>

      {/* Security & Runtime Card */}
      <div className="bg-[#171719] border border-white/10 rounded-2xl p-6 flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div className="flex items-center gap-4">
          <div className="w-12 h-12 rounded-2xl bg-[#C7F000]/10 border border-[#C7F000]/20 flex items-center justify-center text-[#C7F000] shrink-0">
            <ShieldCheck className="w-6 h-6" />
          </div>
          <div>
            <h4 className="text-sm font-semibold text-white">Cryptographic Request Signing Active</h4>
            <p className="text-xs text-[#A1A1AA] mt-0.5">
              HMAC-SHA256 replay protection, nonce validation, and CSRF token verification are enforced across internal services.
            </p>
          </div>
        </div>

        <div className="shrink-0 flex items-center gap-2">
          <span className="px-3 py-1.5 rounded-xl bg-[#202025] border border-white/10 text-xs font-mono text-[#F5F5F5]">
            Env: {APP_CONFIG.environment}
          </span>
          <span className="px-3 py-1.5 rounded-xl bg-[#202025] border border-white/10 text-xs font-mono text-[#C7F000]">
            v{APP_CONFIG.version}
          </span>
        </div>
      </div>
    </div>
  );
};
