"""
CS Scholar AI - RAG Pipeline Schemas
Defines models for context assembly, grounding verification, and tutor responses.
"""
from enum import Enum
from typing import List, Optional, Dict
from uuid import UUID
from pydantic import BaseModel, Field, AliasChoices


class GroundingStatus(str, Enum):
    GROUNDED = "GROUNDED"
    PARTIALLY_GROUNDED = "PARTIALLY_GROUNDED"
    INSUFFICIENT_EVIDENCE = "INSUFFICIENT_EVIDENCE"


class RetrievedContextChunk(BaseModel):
    source_id: str = Field(description="Unique ephemeral source identifier (e.g. S1, S2)")
    chunk_id: UUID
    document_id: UUID
    document_title: str
    content: str
    token_count: int
    page_number: Optional[int] = None
    slide_number: Optional[int] = None
    section_heading: Optional[str] = None
    relevance_score: float = 0.0
    rank: int = 1


class AssembledContext(BaseModel):
    chunks: List[RetrievedContextChunk]
    formatted_text: str
    total_tokens: int
    source_map: Dict[str, RetrievedContextChunk]


class VerifiedCitation(BaseModel):
    source_id: str
    chunk_id: UUID
    document_id: UUID
    document_title: str
    page_number: Optional[int] = None
    slide_number: Optional[int] = None
    section_heading: Optional[str] = None
    excerpt: str
    relevance_score: float = 0.0
    rank: int = 1


class TutorAnswerResponse(BaseModel):
    answer: str
    grounding_status: GroundingStatus
    insufficient_evidence: bool = False
    citations: List[VerifiedCitation] = Field(default_factory=list)
    provider: str
    model_name: str
    input_tokens: Optional[int] = None
    output_tokens: Optional[int] = None
    latency_ms: int = 0
    timings: Optional[Dict[str, int]] = None


class InternalTutorChatRequest(BaseModel):
    query: str
    workspace_id: UUID = Field(..., validation_alias=AliasChoices("workspace_id", "workspaceId"))
    user_id: UUID = Field(..., validation_alias=AliasChoices("user_id", "userId"))
    conversation_id: Optional[UUID] = Field(None, validation_alias=AliasChoices("conversation_id", "conversationId"))
    history: List[Dict[str, str]] = Field(default_factory=list, validation_alias=AliasChoices("history", "conversation_history"))
    provider: Optional[str] = None
    model_name: Optional[str] = Field(None, validation_alias=AliasChoices("model_name", "modelName"))

