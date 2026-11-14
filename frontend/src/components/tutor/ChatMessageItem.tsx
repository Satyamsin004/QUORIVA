import React from 'react';
import ReactMarkdown from 'react-markdown';
import { User, Bot, CheckCircle2, AlertTriangle, AlertCircle, Clock, Cpu } from 'lucide-react';
import { ChatMessage, GroundingStatus } from '@/types/chat';
import { InsufficientEvidenceNotice } from './InsufficientEvidenceNotice';

interface Props {
  message: ChatMessage;
  onCitationClick: (sourceLabel: string, message: ChatMessage) => void;
}

export const ChatMessageItem: React.FC<Props> = ({ message, onCitationClick }) => {
  const isUser = message.role.toUpperCase() === 'USER';

  const renderGroundingBadge = (status: GroundingStatus) => {
    switch (status) {
      case 'GROUNDED':
        return (
          <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-[11px] font-semibold bg-[#C7F000]/10 text-[#C7F000] border border-[#C7F000]/30">
            <CheckCircle2 className="h-3 w-3" />
            Source Grounded
          </span>
        );
      case 'PARTIALLY_GROUNDED':
        return (
          <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-[11px] font-semibold bg-amber-500/10 text-amber-300 border border-amber-500/30">
            <AlertTriangle className="h-3 w-3" />
            Partially Grounded
          </span>
        );
      case 'INSUFFICIENT_EVIDENCE':
        return (
          <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-[11px] font-semibold bg-[#FF2D8D]/10 text-[#FF2D8D] border border-[#FF2D8D]/30">
            <AlertCircle className="h-3 w-3" />
            Insufficient Evidence
          </span>
        );
      default:
        return null;
    }
  };

  // Helper to replace [S1], [S2] with interactive badges inside text nodes
  const renderInteractiveText = (text: string) => {
    const parts = text.split(/(\[S\d+\])/g);
    return parts.map((part, index) => {
      const match = part.match(/^\[(S\d+)\]$/);
      if (match) {
        const sourceLabel = match[1];
        return (
          <button
            key={index}
            type="button"
            onClick={() => onCitationClick(sourceLabel, message)}
            className="inline-flex items-center mx-1 px-2 py-0.5 rounded-md bg-[#FF2D8D]/15 text-[#FF2D8D] hover:bg-[#FF2D8D]/30 text-xs font-mono font-bold border border-[#FF2D8D]/30 transition cursor-pointer"
            title={`View citation source [${sourceLabel}]`}
          >
            [{sourceLabel}]
          </button>
        );
      }
      return part;
    });
  };

  return (
    <div
      className={`flex gap-4 p-5 sm:p-6 transition ${
        isUser ? 'bg-[#09090B]' : 'bg-[#171719] border-y border-white/5'
      }`}
    >
      {/* Avatar */}
      <div className="flex-shrink-0">
        {isUser ? (
          <div className="h-9 w-9 rounded-xl bg-[#202025] flex items-center justify-center border border-white/10 text-white">
            <User className="h-4 w-4" />
          </div>
        ) : (
          <div className="h-9 w-9 rounded-xl bg-gradient-to-br from-[#FF2D8D] to-[#BD0F62] flex items-center justify-center text-white shadow-lg shadow-[#FF2D8D]/20">
            <Bot className="h-4 w-4" />
          </div>
        )}
      </div>

      {/* Message Content & Metadata */}
      <div className="flex-1 space-y-3 max-w-4xl">
        {/* Header with role and status badge */}
        <div className="flex flex-wrap items-center justify-between gap-2">
          <div className="flex items-center gap-2.5">
            <span className="font-semibold text-xs uppercase tracking-wider text-white">
              {isUser ? 'You' : 'QUORIVA AI Tutor'}
            </span>
            {!isUser && renderGroundingBadge(message.status)}
          </div>

          {!isUser && (
            <div className="flex items-center gap-3 text-xs text-[#A1A1AA]">
              {message.provider && (
                <span className="flex items-center gap-1 font-mono text-[11px]">
                  <Cpu className="h-3 w-3 text-[#FF2D8D]" />
                  {message.provider} {message.modelName ? `(${message.modelName})` : ''}
                </span>
              )}
              {message.latencyMs !== undefined && message.latencyMs > 0 && (
                <span className="flex items-center gap-1 text-[11px]">
                  <Clock className="h-3 w-3 text-[#A1A1AA]" />
                  {message.latencyMs}ms
                </span>
              )}
            </div>
          )}
        </div>

        {/* Insufficient Evidence Warning Banner */}
        {!isUser && message.status === 'INSUFFICIENT_EVIDENCE' && (
          <InsufficientEvidenceNotice />
        )}

        {/* Body Text */}
        <div className="text-sm leading-relaxed text-[#F5F5F5] font-sans selection:bg-[#FF2D8D]/30 prose prose-invert max-w-none">
          <ReactMarkdown
            components={{
              p: ({ children }) => (
                <p className="mb-2.5 text-[#F5F5F5] leading-relaxed">
                  {React.Children.map(children, (child) =>
                    typeof child === 'string' ? renderInteractiveText(child) : child
                  )}
                </p>
              ),
              strong: ({ children }) => (
                <strong className="font-semibold text-white">
                  {children}
                </strong>
              ),
              ul: ({ children }) => (
                <ul className="list-disc list-outside pl-5 mb-3 space-y-1 text-[#F5F5F5]">
                  {children}
                </ul>
              ),
              ol: ({ children }) => (
                <ol className="list-decimal list-outside pl-5 mb-3 space-y-1 text-[#F5F5F5]">
                  {children}
                </ol>
              ),
              li: ({ children }) => (
                <li className="leading-relaxed">
                  {React.Children.map(children, (child) =>
                    typeof child === 'string' ? renderInteractiveText(child) : child
                  )}
                </li>
              ),
              h1: ({ children }) => (
                <h1 className="text-base font-bold text-white mb-2 mt-3 pb-1 border-b border-white/10">
                  {children}
                </h1>
              ),
              h2: ({ children }) => (
                <h2 className="text-sm font-bold text-white mb-2 mt-2.5 text-[#FF2D8D]">
                  {children}
                </h2>
              ),
              h3: ({ children }) => (
                <h3 className="text-xs font-bold uppercase tracking-wider text-[#C7F000] mb-1.5 mt-2">
                  {children}
                </h3>
              ),
              code: ({ children }) => (
                <code className="bg-[#202025] text-[#C7F000] px-1.5 py-0.5 rounded text-xs font-mono border border-white/10">
                  {children}
                </code>
              ),
            }}
          >
            {message.content}
          </ReactMarkdown>
        </div>

        {/* Citation Source Badges Summary */}
        {!isUser && message.sources && message.sources.length > 0 && (
          <div className="pt-3 border-t border-white/10 flex flex-wrap items-center gap-2">
            <span className="text-[11px] text-[#A1A1AA] font-semibold uppercase tracking-wider">
              Verified Sources:
            </span>
            {message.sources.map((s) => (
              <button
                key={s.id || s.citationLabel}
                type="button"
                onClick={() => onCitationClick(s.citationLabel, message)}
                className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-lg text-xs bg-[#202025] hover:bg-[#2A2A32] text-zinc-300 border border-white/10 transition"
              >
                <span className="font-mono font-bold text-[#FF2D8D]">[{s.citationLabel}]</span>
                <span className="max-w-[140px] truncate text-white">{s.documentTitle || 'Resource'}</span>
                {s.pageNumber && <span className="text-[#A1A1AA]">p.{s.pageNumber}</span>}
              </button>
            ))}
          </div>
        )}
      </div>
    </div>
  );
};
