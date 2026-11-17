"""
CS Scholar AI - Cross-Encoder Reranker
Scores candidate query-chunk pairs using cross-encoder/ms-marco-MiniLM-L-6-v2,
applying batching, deterministic excerpting, and finite numerical validation.
"""
import logging
import threading
from dataclasses import dataclass
from typing import List, Optional
from uuid import UUID
import numpy as np
import torch
from sentence_transformers import CrossEncoder

from app.retrieval.config import retrieval_config
from app.retrieval.exceptions import RerankerError
from app.retrieval.rrf import HybridCandidate

logger = logging.getLogger(__name__)


@dataclass
class RerankedResult:
    """Final reranked search result item."""

    chunk_id: UUID
    document_id: UUID
    document_name: str
    workspace_id: UUID
    chunk_content: str
    page_number: Optional[int]
    slide_number: Optional[int]
    section_heading: Optional[str]
    chunk_index: int
    source_type: str
    final_rank: int
    reranker_score: float
    rrf_score: float
    dense_rank: Optional[int]
    keyword_rank: Optional[int]
    dense_similarity: Optional[float]
    keyword_score: Optional[float]
    segment_index: Optional[int]


class CrossEncoderReranker:
    """
    Thread-safe Singleton cross-encoder reranker leveraging cross-encoder/ms-marco-MiniLM-L-6-v2.
    Loads once per process and supports CPU/CUDA automatic placement.
    """

    _instance: Optional["CrossEncoderReranker"] = None
    _model: Optional[CrossEncoder] = None
    _lock = threading.Lock()

    def __init__(
        self,
        model_name: Optional[str] = None,
        device: Optional[str] = None,
        batch_size: Optional[int] = None,
        default_top_k: Optional[int] = None
    ):
        self.model_name = model_name or retrieval_config.reranker_model_name
        self.device_config = device or retrieval_config.reranker_device
        self.resolved_device = self._resolve_device(self.device_config)
        self.batch_size = batch_size or retrieval_config.reranker_batch_size
        self.default_top_k = default_top_k or retrieval_config.reranker_top_k

    @classmethod
    def get_instance(
        cls,
        model_name: Optional[str] = None,
        device: Optional[str] = None
    ) -> "CrossEncoderReranker":
        """Get or initialize thread-safe singleton instance."""
        if cls._instance is None:
            with cls._lock:
                if cls._instance is None:
                    cls._instance = cls(model_name, device)
        return cls._instance

    @staticmethod
    def _resolve_device(device_setting: str) -> str:
        """Resolve 'auto' to 'cuda' if GPU is available, else 'cpu'."""
        if device_setting.lower() == "auto":
            is_cuda = torch.cuda.is_available()
            resolved = "cuda" if is_cuda else "cpu"
            logger.info("Reranker device resolution: CUDA available=%s, selected device='%s'", is_cuda, resolved)
            return resolved
        return device_setting.lower()

    def get_model(self) -> CrossEncoder:
        """Lazily load the cross-encoder model once per process with thread safety."""
        if self._model is None:
            with self._lock:
                if self._model is None:
                    try:
                        logger.info("Loading cross-encoder model '%s' on device '%s'...", self.model_name, self.resolved_device)
                        self._model = CrossEncoder(
                            self.model_name,
                            max_length=512,
                            device=self.resolved_device
                        )
                        logger.info("Cross-encoder model '%s' loaded successfully.", self.model_name)
                    except Exception as e:
                        logger.error("Failed to load cross-encoder model '%s': %s", self.model_name, e, exc_info=True)
                        raise RerankerError(
                            f"Failed to load cross-encoder model '{self.model_name}': {e}",
                            {"model_name": self.model_name, "device": self.resolved_device}
                        )
        return self._model

    def rerank(
        self,
        query: str,
        candidates: List[HybridCandidate],
        top_k: Optional[int] = None,
        min_score: Optional[float] = None
    ) -> List[RerankedResult]:
        """
        Score hybrid candidates with the cross-encoder model and return top-K sorted results.

        Args:
            query: User's cleaned query string.
            candidates: Candidates produced by Reciprocal Rank Fusion.
            top_k: Number of highest-ranking results to return.
            min_score: Minimum cross-encoder logit cutoff score. Candidates below this threshold are omitted.

        Returns:
            List of RerankedResult sorted by descending reranker_score.
        """
        if not candidates:
            return []

        limit = top_k or self.default_top_k
        model = self.get_model()

        # Score top candidates (bounded to 12 candidates of 650 chars for sub-second CPU latency)
        candidates_to_score = candidates[:12]

        # Build query-document pairs
        pairs = []
        for cand in candidates_to_score:
            passage = cand.chunk_content.strip()[:650]
            pairs.append([query, passage])

        try:
            with torch.inference_mode():
                # CrossEncoder.predict handles batched inference
                scores = model.predict(
                    pairs,
                    batch_size=min(len(pairs), 16),
                    show_progress_bar=False,
                    convert_to_numpy=True
                )
        except Exception as e:
            logger.error("Cross-encoder inference failed: %s", e, exc_info=True)
            raise RerankerError(f"Cross-encoder inference failed: {e}")

        # Validate that all output scores are finite
        if not np.all(np.isfinite(scores)):
            raise RerankerError(
                "Cross-encoder produced non-finite relevance scores (NaN or Inf).",
                {"scores_count": len(scores)}
            )

        cutoff = min_score if min_score is not None else getattr(retrieval_config, "min_reranker_score", -4.5)

        # Pair scores with candidate objects, filtering out non-contextual noise below cutoff
        scored_candidates = []
        for cand, score in zip(candidates_to_score, scores):
            score_val = float(score)
            if score_val >= cutoff:
                scored_candidates.append({
                    "candidate": cand,
                    "reranker_score": score_val
                })

        # Graceful fallback: if no candidates passed cutoff but top candidates have reasonable similarity
        if not scored_candidates and len(candidates_to_score) > 0:
            best_idx = int(np.argmax(scores))
            best_score = float(scores[best_idx])
            if best_score >= -9.5:
                scored_candidates.append({
                    "candidate": candidates_to_score[best_idx],
                    "reranker_score": best_score
                })
                # If there is a second candidate close to the best, include it as well
                if len(candidates_to_score) > 1:
                    sorted_indices = np.argsort(scores)[::-1]
                    second_idx = int(sorted_indices[1])
                    if float(scores[second_idx]) >= -10.0:
                        scored_candidates.append({
                            "candidate": candidates_to_score[second_idx],
                            "reranker_score": float(scores[second_idx])
                        })

        if not scored_candidates:
            logger.info(
                "All %d candidates fell below cross-encoder cutoff %.2f (best candidate score was %.4f). Returning 0 results.",
                len(candidates), cutoff, float(np.max(scores)) if len(scores) > 0 else -99.0
            )
            return []

        # Deterministic sort: descending by reranker_score, tie-broken by rrf_score, chunk_id
        sorted_items = sorted(
            scored_candidates,
            key=lambda x: (
                -x["reranker_score"],
                -x["candidate"].rrf_score,
                str(x["candidate"].chunk_id)
            )
        )

        # Slice to top_k and assign final_rank
        top_items = sorted_items[:limit]

        results: List[RerankedResult] = []
        for rank_idx, item in enumerate(top_items, start=1):
            cand: HybridCandidate = item["candidate"]
            results.append(
                RerankedResult(
                    chunk_id=cand.chunk_id,
                    document_id=cand.document_id,
                    document_name=cand.document_name,
                    workspace_id=cand.workspace_id,
                    chunk_content=cand.chunk_content,
                    page_number=cand.page_number,
                    slide_number=cand.slide_number,
                    section_heading=cand.section_heading,
                    chunk_index=cand.chunk_index,
                    source_type=cand.source_type,
                    final_rank=rank_idx,
                    reranker_score=item["reranker_score"],
                    rrf_score=cand.rrf_score,
                    dense_rank=cand.dense_rank,
                    keyword_rank=cand.keyword_rank,
                    dense_similarity=cand.dense_similarity,
                    keyword_score=cand.keyword_score,
                    segment_index=cand.segment_index
                )
            )

        logger.info(
            "CrossEncoder reranked %d candidates to top %d results (best score: %.4f, lowest score: %.4f)",
            len(candidates), len(results),
            results[0].reranker_score if results else 0.0,
            results[-1].reranker_score if results else 0.0
        )
        return results
