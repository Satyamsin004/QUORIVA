import React, { useState, useRef, useEffect } from 'react';
import { Bot, BookOpen, Cpu, ShieldCheck, Sparkles, Terminal } from 'lucide-react';
import { ChatMessage, MessageSource } from '@/types/chat';
import { ChatMessageItem } from './ChatMessageItem';
import { ChatInput } from './ChatInput';
import { SourcePanel } from './SourcePanel';

interface Props {
  messages: ChatMessage[];
  isLoading: boolean;
  onSendMessage: (content: string, provider: string) => void;
  activeConversationTitle?: string;
  disabled?: boolean;
}

export const ChatWindow: React.FC<Props> = ({
  messages,
  isLoading,
  onSendMessage,
  activeConversationTitle = 'AI Academic Tutor',
  disabled = false,
}) => {
  const [sourcePanelOpen, setSourcePanelOpen] = useState(false);
  const [activeSources, setActiveSources] = useState<MessageSource[]>([]);
  const [selectedSourceId, setSelectedSourceId] = useState<string | null>(null);

  const scrollRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (scrollRef.current) {
      scrollRef.current.scrollTop = scrollRef.current.scrollHeight;
    }
  }, [messages, isLoading]);

  const handleCitationClick = (sourceLabel: string, message: ChatMessage) => {
    if (message.sources && message.sources.length > 0) {
      setActiveSources(message.sources);
      setSelectedSourceId(sourceLabel);
      setSourcePanelOpen(true);
    }
  };

  const handleStarterClick = (prompt: string) => {
    onSendMessage(prompt, 'groq');
  };

  return (
    <div className="flex-1 flex flex-col h-full bg-[#09090B] overflow-hidden relative text-[#F5F5F5]">
      {/* Chat Header */}
      <div className="px-6 py-4 border-b border-white/10 bg-[#171719]/80 backdrop-blur-md flex items-center justify-between z-10">
        <div className="flex items-center gap-3">
          <div className="h-9 w-9 rounded-xl bg-[#FF2D8D]/10 border border-[#FF2D8D]/30 flex items-center justify-center text-[#FF2D8D]">
            <Bot className="h-5 w-5" />
          </div>
          <div>
            <h2 className="font-semibold text-white text-sm">{activeConversationTitle}</h2>
            <div className="flex items-center gap-2 text-xs text-[#A1A1AA]">
              <span className="inline-block h-2 w-2 rounded-full bg-[#C7F000] animate-pulse" />
              <span className="font-medium text-white/90">Grounded Academic RAG</span>
              <span>•</span>
              <span className="flex items-center gap-1 text-[#FF2D8D]">
                <ShieldCheck className="h-3 w-3" /> Page-level Citations
              </span>
            </div>
          </div>
        </div>

        {activeSources.length > 0 && (
          <button
            onClick={() => setSourcePanelOpen(!sourcePanelOpen)}
            className="flex items-center gap-1.5 px-3.5 py-1.5 rounded-full border border-white/10 bg-[#202025] text-xs text-white hover:border-[#FF2D8D]/50 transition"
          >
            <BookOpen className="h-3.5 w-3.5 text-[#C7F000]" />
            <span>Sources ({activeSources.length})</span>
          </button>
        )}
      </div>

      {/* Messages Feed */}
      <div ref={scrollRef} className="flex-1 overflow-y-auto">
        {messages.length === 0 ? (
          /* Reference 1 Inspired Empty State: Glowing Orb + Clean Welcome */
          <div className="max-w-2xl mx-auto px-6 py-16 flex flex-col items-center text-center space-y-6">
            {/* Glowing Iridescent Orb from Reference 1 */}
            <div className="relative mb-2">
              <div className="glowing-orb" />
              <div className="absolute inset-0 flex items-center justify-center pointer-events-none">
                <Sparkles className="w-8 h-8 text-white/90 drop-shadow-md animate-pulse" />
              </div>
            </div>

            <div className="space-y-2">
              <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-white/5 border border-white/10 text-xs text-[#A1A1AA]">
                <Terminal className="w-3.5 h-3.5 text-[#C7F000]" />
                <span>Personal AI Research Assistant</span>
              </div>
              <h3 className="text-3xl sm:text-4xl font-serif text-white tracking-tight font-normal">
                What are we learning today?
              </h3>
              <p className="text-xs sm:text-sm text-[#A1A1AA] max-w-md mx-auto leading-relaxed">
                Consult your course notes, textbooks, and algorithms. Every answer is synthesized with exact page-level citations.
              </p>
            </div>

            {/* Quick Prompt Cards */}
            <div className="w-full grid grid-cols-1 sm:grid-cols-3 gap-3 pt-6 text-left">
              <button
                type="button"
                onClick={() =>
                  handleStarterClick(
                    'Explain the difference between Paging and Segmentation in Operating Systems.'
                  )
                }
                className="p-4 rounded-2xl border border-white/10 bg-[#171719] hover:bg-[#202025] hover:border-[#FF2D8D]/50 transition group space-y-1.5"
              >
                <div className="flex items-center gap-1.5 text-[#FF2D8D] text-[11px] font-semibold uppercase tracking-wider">
                  <Cpu className="h-3 w-3" />
                  <span>Operating Systems</span>
                </div>
                <p className="text-xs text-white font-medium group-hover:text-[#FF2D8D] transition">
                  Paging vs Segmentation
                </p>
              </button>

              <button
                type="button"
                onClick={() =>
                  handleStarterClick(
                    'How does the Raft consensus algorithm handle leader election and split votes?'
                  )
                }
                className="p-4 rounded-2xl border border-white/10 bg-[#171719] hover:bg-[#202025] hover:border-[#C7F000]/50 transition group space-y-1.5"
              >
                <div className="flex items-center gap-1.5 text-[#C7F000] text-[11px] font-semibold uppercase tracking-wider">
                  <Cpu className="h-3 w-3" />
                  <span>Distributed Systems</span>
                </div>
                <p className="text-xs text-white font-medium group-hover:text-[#C7F000] transition">
                  Raft Leader Election
                </p>
              </button>

              <button
                type="button"
                onClick={() =>
                  handleStarterClick(
                    'Explain the time and space complexity of Dijkstra algorithm with a min-heap.'
                  )
                }
                className="p-4 rounded-2xl border border-white/10 bg-[#171719] hover:bg-[#202025] hover:border-[#FF2D8D]/50 transition group space-y-1.5"
              >
                <div className="flex items-center gap-1.5 text-[#FF2D8D] text-[11px] font-semibold uppercase tracking-wider">
                  <Cpu className="h-3 w-3" />
                  <span>Algorithms</span>
                </div>
                <p className="text-xs text-white font-medium group-hover:text-[#FF2D8D] transition">
                  Dijkstra with Min-Heap
                </p>
              </button>
            </div>
          </div>
        ) : (
          <div className="divide-y divide-white/5">
            {messages.map((message) => (
              <ChatMessageItem
                key={message.id}
                message={message}
                onCitationClick={handleCitationClick}
              />
            ))}
          </div>
        )}

        {/* Loading Bubble */}
        {isLoading && (
          <div className="flex gap-4 p-5 bg-[#171719] border-y border-white/10">
            <div className="h-9 w-9 rounded-xl bg-gradient-to-br from-[#FF2D8D] to-[#BD0F62] flex items-center justify-center text-white shadow-lg shadow-[#FF2D8D]/20">
              <Bot className="h-5 w-5 animate-pulse" />
            </div>
            <div className="space-y-2">
              <div className="flex items-center gap-2">
                <span className="font-semibold text-sm text-white">QUORIVA AI Tutor</span>
                <span className="text-xs text-[#A1A1AA] animate-pulse">
                  Consulting workspace materials & synthesizing answer...
                </span>
              </div>
              <div className="flex items-center gap-1.5 py-1">
                <span className="h-2 w-2 rounded-full bg-[#FF2D8D] animate-bounce [animation-delay:-0.3s]" />
                <span className="h-2 w-2 rounded-full bg-[#C7F000] animate-bounce [animation-delay:-0.15s]" />
                <span className="h-2 w-2 rounded-full bg-white animate-bounce" />
              </div>
            </div>
          </div>
        )}
      </div>

      {/* Input Form */}
      <ChatInput
        onSendMessage={onSendMessage}
        isLoading={isLoading}
        disabled={disabled}
      />

      {/* Slide-over Evidence Source Panel */}
      <SourcePanel
        isOpen={sourcePanelOpen}
        onClose={() => setSourcePanelOpen(false)}
        sources={activeSources}
        activeSourceId={selectedSourceId}
        onSelectSource={(label) => setSelectedSourceId(label)}
      />
    </div>
  );
};
