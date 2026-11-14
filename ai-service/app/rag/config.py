"""
CS Scholar AI - RAG Configuration
Controls token budgets, chunk limits, history window, and grounding thresholds.
"""
from pydantic import BaseModel, Field
from app.core.config import settings


class RAGConfig(BaseModel):
    max_context_tokens: int = Field(default=settings.rag_max_context_tokens)
    max_retrieved_chunks: int = Field(default=settings.rag_max_retrieved_chunks)
    history_max_messages: int = Field(default=settings.rag_history_max_messages)
    temperature: float = Field(default=settings.llm_temperature)
    max_output_tokens: int = Field(default=settings.llm_max_output_tokens)
    tutor_system_name: str = "CS Scholar AI Academic Tutor"


rag_config = RAGConfig()

