import React, { useState, useEffect, useCallback, useRef } from 'react';
import { Conversation, ChatMessage } from '@/types/chat';
import { chatService } from '@/services/chatService';
import { useWorkspace } from '@/context/WorkspaceContext';
import { ConversationSidebar } from '@/components/tutor/ConversationSidebar';
import { ChatWindow } from '@/components/tutor/ChatWindow';

interface Props {
  workspaceId?: string;
  workspaceName?: string;
  initialPrompt?: string | null;
  targetConversationId?: string | null;
}

export const TutorPage: React.FC<Props> = ({
  workspaceId: propWorkspaceId,
  workspaceName: propWorkspaceName,
  initialPrompt,
  targetConversationId,
}) => {
  const { currentWorkspace } = useWorkspace();

  const activeWorkspaceId =
    propWorkspaceId ||
    currentWorkspace?.id ||
    '11111111-1111-1111-1111-111111111111';

  const activeWorkspaceName =
    propWorkspaceName ||
    currentWorkspace?.name ||
    'Personal Workspace';

  const [conversations, setConversations] = useState<Conversation[]>([]);
  const [activeConversationId, setActiveConversationId] = useState<string | null>(
    targetConversationId || null
  );
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [isLoadingMessages, setIsLoadingMessages] = useState(false);
  const [isSending, setIsSending] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const initialPromptHandledRef = useRef(false);
  const activeWorkspaceRef = useRef<string>(activeWorkspaceId);

  // Invalidate conversation & message state when workspace changes
  useEffect(() => {
    activeWorkspaceRef.current = activeWorkspaceId;
    setConversations([]);
    setMessages([]);
    setActiveConversationId(targetConversationId || null);
    setError(null);
  }, [activeWorkspaceId, targetConversationId]);

  // Load conversations for workspace
  const loadConversations = useCallback(async () => {
    const targetWsId = activeWorkspaceId;
    try {
      const page = await chatService.listConversations(targetWsId);
      if (activeWorkspaceRef.current === targetWsId) {
        setConversations(page.content);
        if (page.content.length > 0) {
          setActiveConversationId((prev) => {
            const existsInNewWorkspace = page.content.some((c) => c.id === prev);
            return existsInNewWorkspace ? prev : page.content[0].id;
          });
        } else {
          setActiveConversationId(null);
          setMessages([]);
        }
      }
    } catch (err: unknown) {
      if (activeWorkspaceRef.current === targetWsId) {
        console.warn('Could not load conversations:', err);
      }
    }
  }, [activeWorkspaceId]);

  useEffect(() => {
    loadConversations();
  }, [loadConversations]);

  // If targetConversationId changes, switch to it
  useEffect(() => {
    if (targetConversationId) {
      setActiveConversationId(targetConversationId);
    }
  }, [targetConversationId]);

  // Load messages when activeConversationId changes
  useEffect(() => {
    if (!activeConversationId) {
      setMessages([]);
      return;
    }

    const fetchMessages = async () => {
      setIsLoadingMessages(true);
      setError(null);
      try {
        const history = await chatService.getMessages(activeConversationId);
        setMessages(history);
      } catch (err: unknown) {
        console.error('Failed to load messages:', err);
        setError('Failed to load past conversation messages.');
      } finally {
        setIsLoadingMessages(false);
      }
    };

    fetchMessages();
  }, [activeConversationId]);

  // Create new conversation
  const handleNewConversation = async () => {
    setError(null);
    try {
      const created = await chatService.createConversation(
        activeWorkspaceId,
        'New Discussion'
      );
      setConversations((prev) => [created, ...prev]);
      setActiveConversationId(created.id);
      setMessages([]);
    } catch (err: unknown) {
      console.error('Failed to create new conversation:', err);
      setError('Failed to create a new discussion. Please verify service health.');
    }
  };

  // Delete conversation
  const handleDeleteConversation = async (id: string) => {
    try {
      await chatService.deleteConversation(id);
      setConversations((prev) => prev.filter((c) => c.id !== id));
      if (activeConversationId === id) {
        const remaining = conversations.filter((c) => c.id !== id);
        setActiveConversationId(remaining.length > 0 ? remaining[0].id : null);
      }
    } catch (err: unknown) {
      console.error('Failed to delete conversation:', err);
      setError('Failed to delete conversation session.');
    }
  };

  // Send message
  const handleSendMessage = async (content: string, provider: string) => {
    let targetConvId = activeConversationId;
    setError(null);

    // If no active conversation, create one first
    if (!targetConvId) {
      try {
        const newConv = await chatService.createConversation(
          activeWorkspaceId,
          content.slice(0, 50)
        );
        setConversations((prev) => [newConv, ...prev]);
        targetConvId = newConv.id;
        setActiveConversationId(newConv.id);
      } catch (err: unknown) {
        console.error('Failed to create conversation on send:', err);
        setError('Could not initialize discussion. Please check backend connection.');
        return;
      }
    }

    // Optimistic user message
    const tempUserMessage: ChatMessage = {
      id: `temp-${Date.now()}`,
      conversationId: targetConvId,
      role: 'USER',
      content,
      status: 'COMPLETED',
      createdAt: new Date().toISOString(),
    };
    setMessages((prev) => [...prev, tempUserMessage]);
    setIsSending(true);

    try {
      const assistantMessage = await chatService.sendMessage(targetConvId, content, provider);
      setMessages((prev) => [...prev, assistantMessage]);

      // Update sidebar title if needed
      setConversations((prev) =>
        prev.map((c) =>
          c.id === targetConvId
            ? {
                ...c,
                lastMessageAt: new Date().toISOString(),
                title:
                  c.title === 'New Discussion'
                    ? content.slice(0, 50)
                    : c.title,
              }
            : c
        )
      );
    } catch (err: unknown) {
      console.error('Failed to send message:', err);
      setError('Academic tutor was unable to complete the answer. Please check service health and try again.');
    } finally {
      setIsSending(false);
    }
  };

  // Handle prefilled prompt coming from SearchPage
  useEffect(() => {
    if (initialPrompt && !initialPromptHandledRef.current) {
      initialPromptHandledRef.current = true;
      handleSendMessage(initialPrompt, 'groq');
    }
  }, [initialPrompt]);

  const activeConv = conversations.find((c) => c.id === activeConversationId);

  return (
    <div className="flex h-full w-full bg-[#09090B] overflow-hidden font-sans">
      {/* Sidebar */}
      <ConversationSidebar
        conversations={conversations}
        activeConversationId={activeConversationId}
        onSelectConversation={setActiveConversationId}
        onNewConversation={handleNewConversation}
        onDeleteConversation={handleDeleteConversation}
        workspaceName={activeWorkspaceName}
      />

      {/* Main Chat Area */}
      <div className="flex-1 flex flex-col h-full overflow-hidden">
        {error && (
          <div className="p-3 bg-[#FF2D8D]/10 border-b border-[#FF2D8D]/30 text-xs text-[#FF2D8D] font-medium text-center flex items-center justify-center gap-2">
            <span>{error}</span>
            <button
              onClick={() => setError(null)}
              className="underline hover:text-white ml-2 text-[11px]"
            >
              Dismiss
            </button>
          </div>
        )}
        <ChatWindow
          messages={messages}
          isLoading={isSending || isLoadingMessages}
          onSendMessage={handleSendMessage}
          activeConversationTitle={activeConv ? activeConv.title : 'QUORIVA AI Tutor'}
          disabled={false}
        />
      </div>
    </div>
  );
};
