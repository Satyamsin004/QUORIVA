import React from 'react';
import { ServiceStatus } from '@/types';
import { CheckCircle2, AlertCircle, Clock, XCircle } from 'lucide-react';

interface StatusBadgeProps {
  status: ServiceStatus;
}

export const StatusBadge: React.FC<StatusBadgeProps> = ({ status }) => {
  switch (status) {
    case 'healthy':
      return (
        <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-medium bg-emerald-100 text-emerald-800">
          <CheckCircle2 className="w-3.5 h-3.5 text-emerald-600" />
          Operational
        </span>
      );
    case 'unhealthy':
      return (
        <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-medium bg-amber-100 text-amber-800">
          <AlertCircle className="w-3.5 h-3.5 text-amber-600" />
          Degraded
        </span>
      );
    case 'checking':
      return (
        <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-medium bg-blue-100 text-blue-800">
          <Clock className="w-3.5 h-3.5 text-blue-600 animate-spin" />
          Checking...
        </span>
      );
    case 'offline':
    default:
      return (
        <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-medium bg-rose-100 text-rose-800">
          <XCircle className="w-3.5 h-3.5 text-rose-600" />
          Offline
        </span>
      );
  }
};

