import React, { useState } from 'react';
import { Modal } from '@/components/common/Modal';
import { DocumentItem } from '@/types/document';
import { DocumentStatusBadge } from './DocumentStatusBadge';
import { documentService } from '@/services/documentService';
import { Download, FileText, Calendar, Hash, Layers, HardDrive, Loader2, Copy, Check } from 'lucide-react';

interface DocumentDetailsModalProps {
  document: DocumentItem | null;
  isOpen: boolean;
  onClose: () => void;
}

export const DocumentDetailsModal: React.FC<DocumentDetailsModalProps> = ({
  document,
  isOpen,
  onClose,
}) => {
  const [isDownloading, setIsDownloading] = useState(false);
  const [copiedHash, setCopiedHash] = useState(false);

  if (!document) return null;

  const displayName = document.originalFilename || document.originalFileName || document.title;
  const hashVal = document.checksumSha256 || document.fileHash;

  const formatFileSize = (bytes?: number): string => {
    if (!bytes || bytes <= 0) return '0 B';
    if (bytes < 1024) return `${bytes} B`;
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
    return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
  };

  const handleDownload = async () => {
    setIsDownloading(true);
    try {
      const res = await documentService.getDownloadUrl(document.id);
      if (res && res.downloadUrl) {
        window.open(res.downloadUrl, '_blank', 'noopener,noreferrer');
      }
    } catch (err: unknown) {
      console.error('Download error:', err);
      alert('Failed to retrieve secure download link. Please try again.');
    } finally {
      setIsDownloading(false);
    }
  };

  const handleCopyHash = () => {
    if (hashVal) {
      navigator.clipboard.writeText(hashVal);
      setCopiedHash(true);
      setTimeout(() => setCopiedHash(false), 2000);
    }
  };

  return (
    <Modal isOpen={isOpen} onClose={onClose} title="Academic Document Details" maxWidth="lg">
      <div className="space-y-6 text-[#F5F5F5]">
        {/* Document Header Info */}
        <div className="flex items-start gap-4 p-4 rounded-2xl bg-[#202025] border border-white/10">
          <div className="p-3 rounded-xl bg-[#FF2D8D]/10 text-[#FF2D8D] border border-[#FF2D8D]/20 shrink-0">
            <FileText className="w-6 h-6" />
          </div>
          <div className="min-w-0 flex-1">
            <h4 className="text-base font-semibold text-white truncate">
              {displayName}
            </h4>
            <div className="flex flex-wrap items-center gap-2 mt-2">
              <DocumentStatusBadge
                processingStatus={document.processingStatus}
                embeddingStatus={document.embeddingStatus}
                size="sm"
              />
              <span className="text-xs px-2 py-0.5 rounded-md bg-white/5 border border-white/10 text-white font-mono uppercase font-semibold">
                {document.fileType}
              </span>
            </div>
          </div>
        </div>

        {/* Detailed Metadata Grid */}
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 text-xs">
          <div className="p-3.5 rounded-2xl border border-white/10 bg-[#202025] space-y-1">
            <div className="flex items-center gap-1.5 text-[#A1A1AA] font-medium">
              <HardDrive className="w-3.5 h-3.5" />
              <span>File Size</span>
            </div>
            <p className="font-semibold text-white text-sm">
              {formatFileSize(document.fileSize)}
            </p>
          </div>

          <div className="p-3.5 rounded-2xl border border-white/10 bg-[#202025] space-y-1">
            <div className="flex items-center gap-1.5 text-[#A1A1AA] font-medium">
              <Layers className="w-3.5 h-3.5" />
              <span>Extracted Pages / Slides</span>
            </div>
            <p className="font-semibold text-white text-sm">
              {document.pageCount && document.pageCount > 0
                ? `${document.pageCount} ${document.pageCount === 1 ? 'page' : 'pages'}`
                : 'Pending extraction'}
            </p>
          </div>

          <div className="p-3.5 rounded-2xl border border-white/10 bg-[#202025] space-y-1">
            <div className="flex items-center gap-1.5 text-[#A1A1AA] font-medium">
              <Calendar className="w-3.5 h-3.5" />
              <span>Uploaded On</span>
            </div>
            <p className="font-semibold text-white text-sm">
              {new Date(document.createdAt).toLocaleString(undefined, {
                year: 'numeric',
                month: 'short',
                day: 'numeric',
                hour: '2-digit',
                minute: '2-digit',
              })}
            </p>
          </div>

          <div className="p-3.5 rounded-2xl border border-white/10 bg-[#202025] space-y-1">
            <div className="flex items-center gap-1.5 text-[#A1A1AA] font-medium">
              <Hash className="w-3.5 h-3.5" />
              <span>SHA-256 Checksum</span>
            </div>
            <div className="flex items-center justify-between gap-1">
              <span className="font-mono text-zinc-300 truncate" title={hashVal}>
                {hashVal ? `${hashVal.slice(0, 16)}...` : 'Pending'}
              </span>
              {hashVal && (
                <button
                  type="button"
                  onClick={handleCopyHash}
                  className="p-1 text-[#A1A1AA] hover:text-white rounded transition"
                  title="Copy checksum"
                >
                  {copiedHash ? (
                    <Check className="w-3.5 h-3.5 text-[#C7F000]" />
                  ) : (
                    <Copy className="w-3.5 h-3.5" />
                  )}
                </button>
              )}
            </div>
          </div>
        </div>

        {/* Storage Key info */}
        {document.storageKey && (
          <div className="p-3 rounded-2xl bg-[#09090B] border border-white/5 text-xs font-mono text-zinc-400 break-all">
            <span className="text-[#A1A1AA] select-none">S3 Object Key: </span>
            {document.storageKey}
          </div>
        )}

        {/* Footer Actions */}
        <div className="flex items-center justify-between pt-4 border-t border-white/10">
          <button
            type="button"
            onClick={onClose}
            className="px-4 py-2 text-xs font-medium text-[#A1A1AA] hover:text-white hover:bg-white/5 rounded-xl transition"
          >
            Close
          </button>
          <button
            type="button"
            onClick={handleDownload}
            disabled={isDownloading}
            className="btn-chartreuse inline-flex items-center gap-2"
          >
            {isDownloading ? (
              <Loader2 className="w-4 h-4 animate-spin text-black" />
            ) : (
              <Download className="w-4 h-4 text-black" />
            )}
            <span>Download Original</span>
          </button>
        </div>
      </div>
    </Modal>
  );
};
