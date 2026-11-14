import React from 'react';
import { X, BookOpen, FileText, Layers, Hash } from 'lucide-react';
import { MessageSource } from '@/types/chat';
import { PdfPagePreview } from '@/components/common/PdfPagePreview';

interface Props {
  isOpen: boolean;
  onClose: () => void;
  sources: MessageSource[];
  activeSourceId?: string | null;
  onSelectSource?: (id: string) => void;
}

export const SourcePanel: React.FC<Props> = ({
  isOpen,
  onClose,
  sources,
  activeSourceId,
  onSelectSource,
}) => {
  if (!isOpen) return null;

  return (
    <div className="fixed inset-y-0 right-0 z-50 w-full max-w-md bg-[#171719] border-l border-white/10 shadow-2xl flex flex-col text-[#F5F5F5]">
      {/* Header */}
      <div className="flex items-center justify-between px-6 py-4 border-b border-white/10 bg-[#09090B]/60 backdrop-blur-sm">
        <div className="flex items-center gap-2">
          <BookOpen className="h-5 w-5 text-[#C7F000]" />
          <h3 className="font-semibold text-white text-sm">Verified Evidence Sources</h3>
          <span className="text-xs px-2 py-0.5 rounded-full bg-[#FF2D8D]/20 text-[#FF2D8D] font-mono">
            {sources.length}
          </span>
        </div>
        <button
          type="button"
          onClick={onClose}
          className="p-1 rounded-lg text-[#A1A1AA] hover:text-white hover:bg-white/5 transition"
          aria-label="Close panel"
        >
          <X className="h-5 w-5" />
        </button>
      </div>

      {/* Sources List */}
      <div className="flex-1 overflow-y-auto p-6 space-y-4">
        {sources.length === 0 ? (
          <div className="text-center py-12 text-[#A1A1AA]">
            <p className="text-xs">No verified citation sources attached to this response.</p>
          </div>
        ) : (
          sources.map((source) => {
            const isSelected = activeSourceId === source.citationLabel || activeSourceId === source.id;
            return (
              <div
                key={source.id || source.citationLabel}
                onClick={() => onSelectSource?.(source.citationLabel)}
                className={`rounded-2xl border p-4 transition cursor-pointer ${
                  isSelected
                    ? 'border-[#FF2D8D] bg-[#202025] ring-1 ring-[#FF2D8D]/40'
                    : 'border-white/10 bg-[#202025]/50 hover:border-white/20 hover:bg-[#202025]'
                }`}
              >
                {/* Badge and Title */}
                <div className="flex items-start justify-between gap-3 mb-2">
                  <div className="flex items-center gap-2">
                    <span className="px-2 py-0.5 text-xs font-mono font-bold rounded bg-[#FF2D8D]/20 text-[#FF2D8D] border border-[#FF2D8D]/30">
                      [{source.citationLabel}]
                    </span>
                    <span className="text-xs font-semibold text-zinc-300">
                      Rank #{source.sourceRank}
                    </span>
                  </div>
                  {source.relevanceScore !== undefined && (
                    <span className="text-xs font-mono text-[#C7F000]">
                      Score: {source.relevanceScore.toFixed(3)}
                    </span>
                  )}
                </div>

                {/* Document & Location Metadata */}
                <div className="space-y-1.5 mb-3 text-xs text-[#A1A1AA]">
                  <div className="flex items-center gap-1.5 text-white font-medium truncate">
                    <FileText className="h-3.5 w-3.5 text-[#FF2D8D] flex-shrink-0" />
                    <span className="truncate">{source.documentTitle || 'Academic Resource'}</span>
                  </div>
                  <div className="flex flex-wrap items-center gap-3 text-[11px] text-[#A1A1AA]">
                    {source.pageNumber && (
                      <span className="flex items-center gap-1">
                        <Layers className="h-3 w-3" /> Page {source.pageNumber}
                      </span>
                    )}
                    {source.slideNumber && (
                      <span className="flex items-center gap-1">
                        <Layers className="h-3 w-3" /> Slide {source.slideNumber}
                      </span>
                    )}
                    {source.sectionHeading && (
                      <span className="flex items-center gap-1 truncate max-w-[200px]">
                        <Hash className="h-3 w-3" /> {source.sectionHeading}
                      </span>
                    )}
                  </div>
                </div>

                {/* Exact PDF Page Visual Preview */}
                {(source.documentId || source.id) && (source.pageNumber || source.slideNumber) && (
                  <PdfPagePreview
                    documentId={source.documentId || source.id}
                    pageNumber={source.pageNumber || source.slideNumber || 1}
                    documentTitle={source.documentTitle}
                  />
                )}

                {/* Excerpt Body */}
                <div className="rounded-xl bg-[#09090B] p-3 border border-white/5 text-xs text-zinc-300 leading-relaxed font-sans">
                  {source.chunkContent || 'No excerpt available.'}
                </div>
              </div>
            );
          })
        )}
      </div>
    </div>
  );
};
