import React from 'react';
import { Plus, MessageSquare, Trash2, Sparkles, Folder } from 'lucide-react';
import { Conversation } from '@/types/chat';

interface Props {
  conversations: Conversation[];
  activeConversationId?: string | null;
  onSelectConversation: (id: string) => void;
  onNewConversation: () => void;
  onDeleteConversation: (id: string) => void;
  workspaceName?: string;
  isLoading?: boolean;
}

export const ConversationSidebar: React.FC<Props> = ({
  conversations,
  activeConversationId,
  onSelectConversation,
  onNewConversation,
  onDeleteConversation,
  workspaceName = 'Personal Workspace',
  isLoading = false,
}) => {
  return (
    <div className="w-80 bg-[#171719] border-r border-white/10 flex flex-col h-full text-[#F5F5F5]">
      {/* Workspace Indicator & New Chat Button */}
      <div className="p-4 border-b border-white/10 space-y-3">
        <div className="flex items-center gap-2 text-xs font-medium text-[#A1A1AA] truncate">
          <Folder className="h-4 w-4 text-[#FF2D8D] flex-shrink-0" />
          <span className="truncate">{workspaceName}</span>
        </div>

        <button
          type="button"
          onClick={onNewConversation}
          disabled={isLoading}
          className="w-full flex items-center justify-center gap-2 px-4 py-2.5 rounded-xl bg-[#FF2D8D] hover:bg-[#FF479D] active:bg-[#BD0F62] text-white font-semibold text-xs tracking-wide transition shadow-lg shadow-[#FF2D8D]/20 disabled:opacity-50"
        >
          <Plus className="h-4 w-4" />
          <span>New Discussion</span>
        </button>
      </div>

      {/* Conversations List */}
      <div className="flex-1 overflow-y-auto p-3 space-y-1">
        <div className="px-2 py-1.5 text-[10px] font-semibold text-[#A1A1AA] uppercase tracking-wider flex items-center justify-between">
          <span>Past Sessions</span>
          <span className="text-[#A1A1AA] font-mono">{conversations.length}</span>
        </div>

        {conversations.length === 0 ? (
          <div className="text-center py-10 text-xs text-[#A1A1AA] px-4">
            <Sparkles className="h-6 w-6 text-zinc-600 mx-auto mb-2" />
            <p>No conversations yet. Start a discussion with your academic tutor!</p>
          </div>
        ) : (
          conversations.map((conv) => {
            const isActive = conv.id === activeConversationId;
            return (
              <div
                key={conv.id}
                onClick={() => onSelectConversation(conv.id)}
                className={`group flex items-center justify-between px-3 py-2.5 rounded-xl text-xs transition cursor-pointer ${isActive
                  ? 'bg-[#202025] text-white border border-[#FF2D8D]/50 shadow-sm'
                  : 'text-[#A1A1AA] hover:text-white hover:bg-white/[0.03]'
                  }`}
              >
                <div className="flex items-center gap-2.5 min-w-0">
                  <MessageSquare
                    className={`h-4 w-4 flex-shrink-0 ${isActive ? 'text-[#FF2D8D]' : 'text-zinc-500'
                      }`}
                  />
                  <span className="truncate font-medium">{conv.title || 'Discussion'}</span>
                </div>

                <button
                  type="button"
                  onClick={(e) => {
                    e.stopPropagation();
                    if (confirm('Delete this conversation?')) {
                      onDeleteConversation(conv.id);
                    }
                  }}
                  className="opacity-0 group-hover:opacity-100 p-1 text-zinc-500 hover:text-rose-400 rounded transition"
                  title="Delete conversation"
                >
                  <Trash2 className="h-3.5 w-3.5" />
                </button>
              </div>
            );
          })
        )}
      </div>
    </div>
  );
};
