import React, { useEffect } from 'react';
import { SearchResultItem } from '@/types/search';
import { X, FileText, Bot, Hash, BookOpen } from 'lucide-react';
import { PdfPagePreview } from '@/components/common/PdfPagePreview';

interface SearchSourceDrawerProps {
  isOpen: boolean;
  onClose: () => void;
  result: SearchResultItem | null;
  onAskTutor?: (passagePrompt: string) => void;
}

export const SearchSourceDrawer: React.FC<SearchSourceDrawerProps> = ({
  isOpen,
  onClose,
  result,
  onAskTutor,
}) => {
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape' && isOpen) {
        onClose();
      }
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [isOpen, onClose]);

  if (!isOpen || !result) return null;

  const docTitle = result.documentName || result.documentTitle || 'Academic Document';
  const textContent = result.chunkContent || result.content || '';
  const scoreVal = result.rerankerScore ?? result.score ?? 0;
  const sigmoid = (z: number) => 1 / (1 + Math.exp(-z));
  const normalized = scoreVal >= 0 && scoreVal <= 1 ? scoreVal : sigmoid(scoreVal);
  const scorePercent = Math.min(100, Math.max(1, Math.round(normalized * 100)));

  const handleAskTutor = () => {
    if (onAskTutor) {
      const prompt = result.sectionHeading
        ? `Explain the concepts of "${result.sectionHeading}" from "${docTitle}" in detail.`
        : `Explain the key topics covered in "${docTitle}".`;
      onAskTutor(prompt);
      onClose();
    }
  };

  return (
    <div className="fixed inset-0 z-50 overflow-hidden text-[#F5F5F5]">
      {/* Backdrop */}
      <div
        className="fixed inset-0 bg-black/80 backdrop-blur-md transition-opacity"
        onClick={onClose}
        aria-hidden="true"
      />

      <div className="fixed inset-y-0 right-0 max-w-full flex pl-10">
        <div className="w-screen max-w-xl bg-[#171719] shadow-2xl border-l border-white/10 flex flex-col">
          {/* Header */}
          <div className="px-6 py-5 border-b border-white/10 flex items-center justify-between bg-[#111114]">
            <div className="flex items-center gap-2.5">
              <div className="p-2 rounded-xl bg-[#FF2D8D]/10 text-[#FF2D8D] border border-[#FF2D8D]/20">
                <BookOpen className="w-5 h-5" />
              </div>
              <div>
                <h3 className="text-sm font-semibold text-white">Academic Source Citation</h3>
                <p className="text-xs text-[#A1A1AA]">Full verified passage and context</p>
              </div>
            </div>
            <button
              type="button"
              onClick={onClose}
              className="p-1.5 text-[#A1A1AA] hover:text-white rounded-lg hover:bg-white/5 transition"
            >
              <X className="w-5 h-5" />
            </button>
          </div>

          {/* Body */}
          <div className="flex-1 overflow-y-auto px-6 py-6 space-y-6">
            {/* Document Card */}
            <div className="p-4 rounded-2xl bg-[#202025] border border-white/10 space-y-3">
              <div className="flex items-start justify-between gap-3">
                <div className="min-w-0">
                  <div className="flex items-center gap-2 text-[#FF2D8D] text-[11px] font-semibold uppercase tracking-wider mb-1">
                    <FileText className="w-3.5 h-3.5" />
                    <span>Source Material</span>
                  </div>
                  <h4 className="text-sm font-semibold text-white truncate">
                    {docTitle}
                  </h4>
                </div>
                <div className="shrink-0 text-right">
                  <span className="inline-block px-2.5 py-1 rounded-full text-xs font-semibold bg-[#C7F000]/10 text-[#C7F000] border border-[#C7F000]/30">
                    {scorePercent}% Match
                  </span>
                </div>
              </div>

              <div className="flex flex-wrap gap-2 pt-2 border-t border-white/10 text-xs text-[#A1A1AA]">
                {(result.pageNumber || result.slideNumber) && (
                  <span className="px-2 py-0.5 rounded-md bg-[#171719] border border-white/10 font-medium text-white">
                    Page / Slide: {result.pageNumber || result.slideNumber}
                  </span>
                )}
                {result.sectionHeading && (
                  <span className="px-2 py-0.5 rounded-md bg-[#171719] border border-white/10 font-medium text-white truncate max-w-xs">
                    Section: {result.sectionHeading}
                  </span>
                )}
                {result.chunkIndex !== undefined && (
                  <span className="px-2 py-0.5 rounded-md bg-[#171719] border border-white/10 font-mono text-[#A1A1AA]">
                    Chunk #{result.chunkIndex}
                  </span>
                )}
              </div>
            </div>

            {/* Exact PDF Page Visual Preview */}
            {(result.documentId || (result as any).id) && (result.pageNumber || result.slideNumber) && (
              <PdfPagePreview
                documentId={result.documentId || (result as any).id}
                pageNumber={result.pageNumber || result.slideNumber || 1}
                documentTitle={docTitle}
              />
            )}

            {/* Passage Text */}
            <div>
              <h5 className="text-[11px] font-semibold text-[#A1A1AA] uppercase tracking-wider mb-2">
                Ground-Truth Passage Content
              </h5>
              <div className="p-4 rounded-2xl bg-[#09090B] border border-white/5 text-sm text-zinc-300 leading-relaxed font-serif whitespace-pre-wrap selection:bg-[#FF2D8D]/30">
                {textContent}
              </div>
            </div>

            {/* Technical Metadata */}
            <div className="p-4 rounded-2xl border border-white/10 bg-[#202025] space-y-2 text-xs">
              <div className="flex items-center justify-between text-[#A1A1AA]">
                <span className="flex items-center gap-1.5">
                  <Hash className="w-3.5 h-3.5 text-[#FF2D8D]" />
                  Chunk ID
                </span>
                <span className="font-mono text-white truncate max-w-[200px]" title={result.chunkId}>
                  {result.chunkId}
                </span>
              </div>
              <div className="flex items-center justify-between text-[#A1A1AA]">
                <span>Relevance Match Score</span>
                <span className="font-mono text-[#C7F000]">{scoreVal.toFixed(4)}</span>
              </div>
            </div>
          </div>

          {/* Footer Actions */}
          <div className="p-5 border-t border-white/10 bg-[#111114] flex items-center justify-between gap-3">
            <button
              type="button"
              onClick={onClose}
              className="px-4 py-2 text-xs font-medium text-[#A1A1AA] hover:text-white rounded-xl transition hover:bg-white/5"
            >
              Close
            </button>
            {onAskTutor && (
              <button
                type="button"
                onClick={handleAskTutor}
                className="btn-chartreuse inline-flex items-center gap-2"
              >
                <Bot className="w-4 h-4 text-black" />
                <span>Ask AI Tutor About This</span>
              </button>
            )}
          </div>
        </div>
      </div>
    </div>
  );
};
