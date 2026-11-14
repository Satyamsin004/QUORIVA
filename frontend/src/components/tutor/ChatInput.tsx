import React, { useState, useRef, useEffect } from 'react';
import { Send, Loader2, Sparkles, Command } from 'lucide-react';

interface Props {
  onSendMessage: (content: string, provider: string) => void;
  isLoading: boolean;
  disabled?: boolean;
}

export const ChatInput: React.FC<Props> = ({ onSendMessage, isLoading, disabled = false }) => {
  const [content, setContent] = useState('');
  const [provider, setProvider] = useState('groq');
  const textareaRef = useRef<HTMLTextAreaElement>(null);

  useEffect(() => {
    if (!isLoading && textareaRef.current) {
      textareaRef.current.focus();
    }
  }, [isLoading]);

  const handleSubmit = (e?: React.FormEvent) => {
    if (e) e.preventDefault();
    if (!content.trim() || isLoading || disabled) return;

    onSendMessage(content.trim(), provider);
    setContent('');

    if (textareaRef.current) {
      textareaRef.current.style.height = 'auto';
    }
  };

  const handleKeyDown = (e: React.KeyboardEvent<HTMLTextAreaElement>) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSubmit();
    }
  };

  const handleInput = () => {
    if (textareaRef.current) {
      textareaRef.current.style.height = 'auto';
      textareaRef.current.style.height = `${Math.min(textareaRef.current.scrollHeight, 180)}px`;
    }
  };

  return (
    <form onSubmit={handleSubmit} className="p-4 sm:p-5 border-t border-white/10 bg-[#09090B]/90 backdrop-blur-md">
      <div className="max-w-4xl mx-auto space-y-3">
        {/* Model and Provider Selector Row */}
        <div className="flex items-center justify-between text-xs px-1 text-[#A1A1AA]">
          <div className="flex items-center gap-2">
            <Sparkles className="h-3.5 w-3.5 text-[#FF2D8D]" />
            <span className="font-medium text-zinc-300">LLM Provider:</span>
            <select
              value={provider}
              onChange={(e) => setProvider(e.target.value)}
              disabled={isLoading || disabled}
              aria-label="Select LLM Provider"
              className="bg-[#171719] border border-white/10 text-white rounded-lg px-2.5 py-1 text-xs focus:outline-none focus:border-[#FF2D8D] font-mono cursor-pointer"
            >
              <option value="groq">Groq • Llama 3.3 70B Versatile</option>
              <option value="gemini">Google Gemini • 1.5 Flash</option>
            </select>
          </div>

          <div className="hidden sm:flex items-center gap-1.5 text-[11px] text-[#A1A1AA]">
            <span>Press</span>
            <kbd className="px-1.5 py-0.5 rounded bg-[#171719] border border-white/10 text-zinc-300 font-mono text-[10px] flex items-center gap-0.5">
              <Command className="w-2.5 h-2.5" /> Enter
            </kbd>
            <span>to send</span>
          </div>
        </div>

        {/* Floating Composer Container */}
        <div className="relative flex items-end gap-2 bg-[#171719] rounded-2xl border border-white/10 focus-within:border-[#FF2D8D]/60 focus-within:ring-1 focus-within:ring-[#FF2D8D]/30 p-2.5 shadow-2xl transition">
          <textarea
            ref={textareaRef}
            rows={1}
            value={content}
            onChange={(e) => setContent(e.target.value)}
            onInput={handleInput}
            onKeyDown={handleKeyDown}
            placeholder={
              disabled
                ? 'Select or create a conversation to begin...'
                : 'Ask a question about algorithms, lecture notes, or system concepts...'
            }
            disabled={disabled || isLoading}
            className="flex-1 bg-transparent text-[#F5F5F5] placeholder-[#A1A1AA] text-sm px-3 py-2 resize-none focus:outline-none max-h-44 disabled:opacity-40 leading-relaxed font-sans"
          />

          <button
            type="submit"
            disabled={!content.trim() || isLoading || disabled}
            className="flex-shrink-0 h-10 w-10 rounded-xl bg-[#C7F000] hover:bg-[#D4F52B] active:bg-[#A6C800] text-black font-bold flex items-center justify-center transition shadow-lg shadow-[#C7F000]/20 disabled:opacity-30 disabled:cursor-not-allowed disabled:shadow-none"
            aria-label="Send message"
          >
            {isLoading ? (
              <Loader2 className="h-5 w-5 animate-spin text-black" />
            ) : (
              <Send className="h-4 w-4 text-black" />
            )}
          </button>
        </div>
      </div>
    </form>
  );
};
