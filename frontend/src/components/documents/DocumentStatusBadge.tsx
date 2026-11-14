import React from 'react';
import { ProcessingStatus, EmbeddingStatus } from '@/types/document';
import { CheckCircle2, Loader2, Clock, AlertCircle } from 'lucide-react';

interface DocumentStatusBadgeProps {
  processingStatus: ProcessingStatus;
  embeddingStatus?: EmbeddingStatus;
  size?: 'sm' | 'md';
}

export type DisplayStatusType =
  | 'READY'
  | 'EXTRACTING'
  | 'WAITING_EMBEDDINGS'
  | 'EMBEDDING'
  | 'PROCESSING_FAILED'
  | 'EMBEDDING_FAILED'
  | 'PENDING'
  | 'PENDING_UPLOAD';

export function getDocumentDisplayStatus(
  processingStatus: ProcessingStatus,
  embeddingStatus?: EmbeddingStatus
): { type: DisplayStatusType; label: string } {
  // Ingestion failed
  if (processingStatus === 'FAILED') {
    return { type: 'PROCESSING_FAILED', label: 'Processing failed' };
  }

  // Ingestion completed, but embedding failed
  if (processingStatus === 'PROCESSED' && embeddingStatus === 'FAILED') {
    return { type: 'EMBEDDING_FAILED', label: 'Embedding failed' };
  }

  // Ingestion in progress
  if (processingStatus === 'PROCESSING') {
    return { type: 'EXTRACTING', label: 'Extracting content' };
  }

  // Ingestion completed
  if (processingStatus === 'PROCESSED') {
    if (
      embeddingStatus === 'EMBEDDED' ||
      embeddingStatus === 'COMPLETED' ||
      embeddingStatus === 'READY' ||
      embeddingStatus === 'PARTIALLY_READY'
    ) {
      return { type: 'READY', label: 'Ready for AI Search' };
    }
    if (
      embeddingStatus === 'EMBEDDING_IN_PROGRESS' ||
      embeddingStatus === 'IN_PROGRESS' ||
      embeddingStatus === 'PROCESSING'
    ) {
      return { type: 'EMBEDDING', label: 'Generating embeddings' };
    }
    // Pending embedding or not started
    return { type: 'WAITING_EMBEDDINGS', label: 'Waiting for embeddings' };
  }

  // Pending upload
  if (processingStatus === 'PENDING_UPLOAD') {
    return { type: 'PENDING_UPLOAD', label: 'Pending upload' };
  }

  // PENDING_PROCESSING, UPLOADED, etc.
  return { type: 'PENDING', label: 'Pending processing' };
}

export const DocumentStatusBadge: React.FC<DocumentStatusBadgeProps> = ({
  processingStatus,
  embeddingStatus,
  size = 'md',
}) => {
  const { type, label } = getDocumentDisplayStatus(processingStatus, embeddingStatus);

  const sizeClasses =
    size === 'sm' ? 'px-2.5 py-0.5 text-[11px]' : 'px-3 py-1 text-xs';

  switch (type) {
    case 'READY':
      return (
        <span
          className={`inline-flex items-center gap-1.5 rounded-full font-medium bg-[#C7F000]/10 text-[#C7F000] border border-[#C7F000]/30 shadow-xs ${sizeClasses}`}
        >
          <CheckCircle2 className="w-3.5 h-3.5 text-[#C7F000] shrink-0" />
          <span>{label}</span>
        </span>
      );

    case 'EXTRACTING':
      return (
        <span
          className={`inline-flex items-center gap-1.5 rounded-full font-medium bg-sky-500/10 text-sky-400 border border-sky-500/30 ${sizeClasses}`}
        >
          <Loader2 className="w-3.5 h-3.5 text-sky-400 animate-spin shrink-0" />
          <span>{label}</span>
        </span>
      );

    case 'WAITING_EMBEDDINGS':
      return (
        <span
          className={`inline-flex items-center gap-1.5 rounded-full font-medium bg-purple-500/10 text-purple-300 border border-purple-500/30 ${sizeClasses}`}
        >
          <Clock className="w-3.5 h-3.5 text-purple-300 shrink-0" />
          <span>{label}</span>
        </span>
      );

    case 'EMBEDDING':
      return (
        <span
          className={`inline-flex items-center gap-1.5 rounded-full font-medium bg-[#FF2D8D]/10 text-[#FF2D8D] border border-[#FF2D8D]/30 ${sizeClasses}`}
        >
          <Loader2 className="w-3.5 h-3.5 text-[#FF2D8D] animate-spin shrink-0" />
          <span>{label}</span>
        </span>
      );

    case 'PENDING_UPLOAD':
      return (
        <span
          className={`inline-flex items-center gap-1.5 rounded-full font-medium bg-white/5 text-zinc-400 border border-white/10 ${sizeClasses}`}
        >
          <Clock className="w-3.5 h-3.5 text-zinc-500 shrink-0" />
          <span>{label}</span>
        </span>
      );

    case 'PENDING':
      return (
        <span
          className={`inline-flex items-center gap-1.5 rounded-full font-medium bg-amber-500/10 text-amber-400 border border-amber-500/30 ${sizeClasses}`}
        >
          <Clock className="w-3.5 h-3.5 text-amber-400 shrink-0" />
          <span>{label}</span>
        </span>
      );

    case 'EMBEDDING_FAILED':
    case 'PROCESSING_FAILED':
    default:
      return (
        <span
          className={`inline-flex items-center gap-1.5 rounded-full font-medium bg-rose-500/10 text-rose-400 border border-rose-500/30 ${sizeClasses}`}
        >
          <AlertCircle className="w-3.5 h-3.5 text-rose-400 shrink-0" />
          <span>{label}</span>
        </span>
      );
  }
};
