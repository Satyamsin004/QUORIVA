"""
CS Scholar AI - Context Assembler
Selects top retrieved chunks within token budgets and builds ephemeral source ID mappings.
"""
import logging
from typing import List, Dict
from app.rag.schemas import RetrievedContextChunk, AssembledContext
from app.rag.config import rag_config
from app.retrieval.schemas import SearchResultItem

logger = logging.getLogger(__name__)


class ContextAssembler:
    """Assembles retrieved search results into a bounded, formatted context window."""

    def __init__(self, max_context_tokens: int = rag_config.max_context_tokens, max_chunks: int = rag_config.max_retrieved_chunks):
        self.max_context_tokens = max_context_tokens
        self.max_chunks = max_chunks

    @staticmethod
    def estimate_tokens(text: str) -> int:
        """Heuristic token estimation (~4 characters per token + word buffer)."""
        if not text:
            return 0
        return max(1, len(text) // 4)

    def assemble(self, search_results: List[SearchResultItem]) -> AssembledContext:
        """
        Takes hybrid retrieval search results, filters to max chunks and token budget,
        and assigns ephemeral source labels (S1, S2, etc.).
        """
        selected_chunks: List[RetrievedContextChunk] = []
        source_map: Dict[str, RetrievedContextChunk] = {}
        formatted_parts: List[str] = []
        accumulated_tokens = 0

        for idx, item in enumerate(search_results[:self.max_chunks]):
            source_id = f"S{idx + 1}"
            chunk_tokens = self.estimate_tokens(item.chunk_content)

            # Check token budget
            if accumulated_tokens + chunk_tokens > self.max_context_tokens and selected_chunks:
                logger.info(
                    "Context token budget reached (%d + %d > %d). Stopping at %d chunks.",
                    accumulated_tokens, chunk_tokens, self.max_context_tokens, len(selected_chunks)
                )
                break

            doc_title = item.document_name or "Untitled Document"
            meta_parts = [f"Document: {doc_title}"]
            if item.page_number:
                meta_parts.append(f"Page: {item.page_number}")
            if item.slide_number:
                meta_parts.append(f"Slide: {item.slide_number}")
            if item.section_heading:
                meta_parts.append(f"Section: {item.section_heading}")

            meta_str = " | ".join(meta_parts)

            chunk = RetrievedContextChunk(
                source_id=source_id,
                chunk_id=item.chunk_id,
                document_id=item.document_id,
                document_title=doc_title,
                content=item.chunk_content,
                token_count=chunk_tokens,
                page_number=item.page_number,
                slide_number=item.slide_number,
                section_heading=item.section_heading,
                relevance_score=item.reranker_score,
                rank=idx + 1
            )

            selected_chunks.append(chunk)
            source_map[source_id] = chunk
            accumulated_tokens += chunk_tokens

            chunk_repr = f"[{source_id}] {meta_str}\n{item.chunk_content.strip()}"
            formatted_parts.append(chunk_repr)

        full_formatted = "\n\n---\n\n".join(formatted_parts)

        return AssembledContext(
            chunks=selected_chunks,
            formatted_text=full_formatted,
            total_tokens=accumulated_tokens,
            source_map=source_map
        )

