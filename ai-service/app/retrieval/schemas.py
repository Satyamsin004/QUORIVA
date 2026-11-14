"""
CS Scholar AI - Retrieval Pydantic Schemas & Data Transfer Objects
"""
from typing import List, Optional
from uuid import UUID
from pydantic import BaseModel, Field, ConfigDict


class InternalSearchRequest(BaseModel):
    """Internal search request received from Spring Boot API."""

    model_config = ConfigDict(populate_by_name=True)

    query: str = Field(..., min_length=1, max_length=1000, description="User search query string")
    workspace_id: UUID = Field(..., alias="workspaceId", description="Authorized workspace scope identifier")
    user_id: UUID = Field(..., alias="userId", description="Authenticated user identifier")
    top_k: int = Field(default=10, alias="topK", ge=1, le=100, description="Final number of ranked results to return")

    # Optional retrieval parameter overrides
    dense_candidate_count: Optional[int] = Field(default=None, alias="denseCandidateCount", ge=1, le=200)
    keyword_candidate_count: Optional[int] = Field(default=None, alias="keywordCandidateCount", ge=1, le=200)
    hybrid_candidate_count: Optional[int] = Field(default=None, alias="hybridCandidateCount", ge=1, le=200)
    rrf_k: Optional[int] = Field(default=None, alias="rrfK", ge=1, le=500)


class SearchResultItem(BaseModel):
    """Ranked search result item containing chunk content and grounding metadata."""

    model_config = ConfigDict(populate_by_name=True, serialize_by_alias=True)

    chunk_id: UUID = Field(..., alias="chunkId")
    document_id: UUID = Field(..., alias="documentId")
    document_name: str = Field(..., alias="documentName")
    workspace_id: UUID = Field(..., alias="workspaceId")
    chunk_content: str = Field(..., alias="chunkContent")
    page_number: Optional[int] = Field(default=None, alias="pageNumber")
    slide_number: Optional[int] = Field(default=None, alias="slideNumber")
    section_heading: Optional[str] = Field(default=None, alias="sectionHeading")
    chunk_index: int = Field(..., alias="chunkIndex")
    source_type: str = Field(..., alias="sourceType")
    final_rank: int = Field(..., alias="finalRank")
    reranker_score: float = Field(..., alias="rerankerScore")
    rrf_score: float = Field(..., alias="rrfScore")
    dense_rank: Optional[int] = Field(default=None, alias="denseRank")
    keyword_rank: Optional[int] = Field(default=None, alias="keywordRank")
    dense_similarity: Optional[float] = Field(default=None, alias="denseSimilarity")
    keyword_score: Optional[float] = Field(default=None, alias="keywordScore")
    segment_index: Optional[int] = Field(default=None, alias="segmentIndex")


class InternalSearchResponse(BaseModel):
    """Response returned by internal retrieval search endpoint."""

    model_config = ConfigDict(populate_by_name=True, serialize_by_alias=True)

    query: str
    workspace_id: UUID = Field(..., alias="workspaceId")
    total_candidates: int = Field(..., alias="totalCandidates")
    results: List[SearchResultItem]

