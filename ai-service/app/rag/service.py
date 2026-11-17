"""
CS Scholar AI - RAG Service Orchestrator
Coordinates hybrid retrieval, context window budgeting, prompt construction,
LLM generation, citation verification, and grounding assessment.
"""
import logging
import re
import time
from typing import Optional
from sqlalchemy.orm import Session

from app.rag.schemas import (
    InternalTutorChatRequest,
    TutorAnswerResponse,
    GroundingStatus
)
from app.rag.context_assembler import ContextAssembler
from app.rag.prompt_builder import PromptBuilder
from app.rag.citation_validator import CitationValidator
from app.rag.config import rag_config
from app.retrieval.service import RetrievalService
from app.retrieval.schemas import InternalSearchRequest
from app.providers.factory import LLMProviderFactory
from app.providers.schemas import LLMGenerationConfig

logger = logging.getLogger(__name__)


class RAGService:
    """End-to-end Academic Tutor RAG service."""

    def __init__(
        self,
        retrieval_service: Optional[RetrievalService] = None,
        context_assembler: Optional[ContextAssembler] = None,
        prompt_builder: Optional[PromptBuilder] = None,
        citation_validator: Optional[CitationValidator] = None
    ):
        self.retrieval_service = retrieval_service or RetrievalService()
        self.context_assembler = context_assembler or ContextAssembler()
        self.prompt_builder = prompt_builder or PromptBuilder()
        self.citation_validator = citation_validator or CitationValidator()

    def answer(self, db: Session, request: InternalTutorChatRequest) -> TutorAnswerResponse:
        start_time = time.time()
        logger.info(
            "RAG tutor answer requested for user=%s, workspace=%s, query='%s'",
            request.user_id, request.workspace_id, request.query[:60]
        )

        # 1. Clean query for semantic and lexical search (remove meta-instructions, typos, and style requests)
        raw_query = request.query.strip()
        search_query = raw_query
        
        # Normalize common student typos
        search_query = re.sub(r'\bcopuled\b', 'coupled', search_query, flags=re.IGNORECASE)
        search_query = re.sub(r'\bdiffrent\b', 'different', search_query, flags=re.IGNORECASE)
        search_query = re.sub(r'\bdepper\b', 'deeper', search_query, flags=re.IGNORECASE)

        # Strip linguistic / meta instructions from search query so BM25 / vector search matches academic text
        meta_patterns = [
            r'\b(?:can\s+u|can\s+you|please|pls)\s+explain\s+in\s+hinglish\b',
            r'\bin\s+hinglish\b',
            r'\bin\s+hindi\b',
            r'\bin\s+\d+\s+bullets?\s*(?:points?)?\b',
            r'\bin\s+bullets?\s*(?:points?)?\b',
            r'\bwhat\s+is\s+(.*?)\s+(?:means|mean)\b',
            r'\bwhat\s+does\s+(.*?)\s+mean\b',
        ]
        cleaned_search = search_query
        for pat in meta_patterns:
            cleaned_search = re.sub(pat, r'\1' if r'\1' in pat else '', cleaned_search, flags=re.IGNORECASE)
        cleaned_search = re.sub(r'\s+', ' ', cleaned_search).strip()
        if len(cleaned_search) >= 3:
            search_query = cleaned_search

        # Augment with previous conversation context if follow-up
        if request.history:
            for item in reversed(request.history):
                content = item.get("content", "") if isinstance(item, dict) else getattr(item, "content", "")
                role = item.get("role", "") if isinstance(item, dict) else getattr(item, "role", "")
                if role.lower() in ("user", "assistant") and content:
                    # Append key terms from last turn to help search
                    words = [w for w in content.replace("*", "").split() if len(w) > 4][:5]
                    if words:
                        search_query = f"{search_query} {' '.join(words)}"
                    break

        # 2. Execute Hybrid Retrieval across workspace documents
        search_req = InternalSearchRequest(
            query=search_query,
            workspace_id=request.workspace_id,
            user_id=request.user_id,
            top_k=rag_config.max_retrieved_chunks
        )
        search_res = self.retrieval_service.search(db, search_req)
        all_timings = dict(search_res.timings or {})

        # 3. Assemble Context & Assign Ephemeral Source IDs (S1, S2, ...)
        t_ctx_start = time.time()
        context = self.context_assembler.assemble(search_res.results)
        all_timings["context_assembly_ms"] = int((time.time() - t_ctx_start) * 1000)

        # If no documents matched in workspace and there is NO history, return guidance to upload materials
        if not search_res.results and not request.history:
            logger.info("No documents found in workspace %s for query", request.workspace_id)
            return TutorAnswerResponse(
                answer="I could not find any relevant materials or documents in your workspace to answer this question. Please make sure your course documents have been uploaded and processed.",
                grounding_status=GroundingStatus.INSUFFICIENT_EVIDENCE,
                insufficient_evidence=True,
                citations=[],
                provider=request.provider or "none",
                model_name=request.model_name or "none",
                input_tokens=0,
                output_tokens=0,
                latency_ms=all_timings.get("total_retrieval_ms", 0),
                timings=all_timings
            )

        # 4. Build Prompt with Injection Boundaries and Conversation History
        t_prompt_start = time.time()
        messages = self.prompt_builder.build_messages(
            query=request.query,
            context=context,
            history=request.history
        )
        all_timings["prompt_build_ms"] = int((time.time() - t_prompt_start) * 1000)

        # 5. Resolve LLM Provider & Generate Answer
        provider = LLMProviderFactory.get_provider(
            provider_name=request.provider,
            model_name=request.model_name
        )
        gen_config = LLMGenerationConfig(
            temperature=rag_config.temperature,
            max_output_tokens=rag_config.max_output_tokens,
            json_mode=True
        )

        llm_response = provider.generate(
            messages=messages,
            config=gen_config,
            model_name=request.model_name
        )
        all_timings["llm_generation_ms"] = llm_response.latency_ms

        # 6. Parse JSON Payload
        parsed = llm_response.parsed_json or {}
        raw_answer = parsed.get("answer") or llm_response.content
        insufficient_evidence_flag = bool(parsed.get("insufficient_evidence", False))
        claimed_sources = parsed.get("cited_sources", [])

        # 7. Audit Citations & Compute Grounding Status
        t_cite_start = time.time()
        verified_citations, grounding_status, final_insufficient = self.citation_validator.validate(
            answer=raw_answer,
            insufficient_evidence_flag=insufficient_evidence_flag,
            claimed_sources=claimed_sources,
            context=context
        )
        all_timings["citation_validation_ms"] = int((time.time() - t_cite_start) * 1000)

        total_latency = int((time.time() - start_time) * 1000)
        all_timings["total_latency_ms"] = total_latency

        logger.info(
            "Tutor request completed in %dms (retrieval=%dms, rerank=%dms, llm=%dms)",
            total_latency,
            all_timings.get("total_retrieval_ms", 0),
            all_timings.get("reranking_ms", 0),
            all_timings.get("llm_generation_ms", 0)
        )

        return TutorAnswerResponse(
            answer=raw_answer,
            grounding_status=grounding_status,
            insufficient_evidence=final_insufficient,
            citations=verified_citations,
            provider=llm_response.provider,
            model_name=llm_response.model_name,
            input_tokens=llm_response.input_tokens,
            output_tokens=llm_response.output_tokens,
            latency_ms=total_latency,
            timings=all_timings
        )

