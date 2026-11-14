"""
CS Scholar AI - RAG Module
Exports RAGService, ContextAssembler, PromptBuilder, CitationValidator, schemas, and exceptions.
"""
from app.rag.schemas import (
    GroundingStatus,
    RetrievedContextChunk,
    AssembledContext,
    VerifiedCitation,
    TutorAnswerResponse,
    InternalTutorChatRequest
)
from app.rag.exceptions import RAGError, InsufficientEvidenceError, CitationValidationError
from app.rag.config import rag_config, RAGConfig
from app.rag.context_assembler import ContextAssembler
from app.rag.prompt_builder import PromptBuilder
from app.rag.citation_validator import CitationValidator
from app.rag.service import RAGService

__all__ = [
    "GroundingStatus",
    "RetrievedContextChunk",
    "AssembledContext",
    "VerifiedCitation",
    "TutorAnswerResponse",
    "InternalTutorChatRequest",
    "RAGError",
    "InsufficientEvidenceError",
    "CitationValidationError",
    "rag_config",
    "RAGConfig",
    "ContextAssembler",
    "PromptBuilder",
    "CitationValidator",
    "RAGService"
]
