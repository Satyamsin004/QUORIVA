export type GroundingStatus = 'GROUNDED' | 'PARTIALLY_GROUNDED' | 'INSUFFICIENT_EVIDENCE' | 'COMPLETED' | 'FAILED';

export interface MessageSource {
  id: string;
  chunkId?: string;
  documentId?: string;
  documentTitle?: string;
  sourceRank: number;
  citationLabel: string; // e.g. "S1", "S2"
  relevanceScore?: number;
  pageNumber?: number;
  slideNumber?: number;
  sectionHeading?: string;
  chunkContent?: string;
}

export interface ChatMessage {
  id: string;
  conversationId: string;
  role: 'USER' | 'ASSISTANT' | 'SYSTEM' | string;
  content: string;
  status: GroundingStatus;
  provider?: string;
  modelName?: string;
  inputTokens?: number;
  outputTokens?: number;
  latencyMs?: number;
  createdAt: string;
  sources?: MessageSource[];
}

export interface Conversation {
  id: string;
  workspaceId: string;
  title: string;
  status: string;
  createdAt: string;
  updatedAt: string;
  lastMessageAt: string;
  messageCount: number;
}

