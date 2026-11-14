"""
CS Scholar AI - Phase 12 Retrieval Evaluation Metrics
Calculates Recall@K, Precision@K, Mean Reciprocal Rank (MRR), and nDCG@K.
Handles multiple relevant chunks, graded relevance, and deterministic ranking tie-breaks.
"""
import math
from typing import List, Set, Dict, Any, Optional
from uuid import UUID


def compute_recall_at_k(retrieved_ids: List[Any], relevant_ids: Set[Any], k: int) -> float:
    """
    Calculate Recall@K: fraction of relevant items retrieved in top K.
    """
    if not relevant_ids or k <= 0:
        return 0.0

    top_k = set(retrieved_ids[:k])
    hits = len(top_k.intersection(relevant_ids))
    return float(hits / len(relevant_ids))


def compute_precision_at_k(retrieved_ids: List[Any], relevant_ids: Set[Any], k: int) -> float:
    """
    Calculate Precision@K: fraction of retrieved items in top K that are relevant.
    """
    if not retrieved_ids or k <= 0:
        return 0.0

    top_k = retrieved_ids[:k]
    hits = sum(1 for item in top_k if item in relevant_ids)
    return float(hits / len(top_k))


def compute_mrr(retrieved_ids: List[Any], relevant_ids: Set[Any]) -> float:
    """
    Calculate Reciprocal Rank (RR): reciprocal of the 1-based rank of the first relevant item.
    """
    if not relevant_ids or not retrieved_ids:
        return 0.0

    for idx, item in enumerate(retrieved_ids, start=1):
        if item in relevant_ids:
            return 1.0 / idx
    return 0.0


def compute_ndcg_at_k(
    retrieved_ids: List[Any],
    relevant_ids: Optional[Set[Any]] = None,
    k: int = 10,
    graded_relevance: Optional[Dict[Any, int]] = None
) -> float:
    """
    Calculate Normalized Discounted Cumulative Gain (nDCG@K).
    Supports graded relevance (e.g. 0 to 3) or falls back to binary relevance.

    DCG@K = sum_{i=1}^K (2^{rel_i} - 1) / log2(i + 1)
    """
    if graded_relevance and not relevant_ids:
        relevant_ids = {k_id for k_id, g in graded_relevance.items() if g > 0}

    if not relevant_ids or not retrieved_ids or k <= 0:
        return 0.0

    # Determine gains for retrieved items
    gains = []
    for item in retrieved_ids[:k]:
        item_str = str(item)
        if graded_relevance and item in graded_relevance:
            rel = float(graded_relevance[item])
        elif graded_relevance and item_str in graded_relevance:
            rel = float(graded_relevance[item_str])
        elif item in relevant_ids:
            rel = 1.0
        else:
            rel = 0.0
        gains.append(rel)

    # Compute DCG@K
    dcg = 0.0
    for idx, rel in enumerate(gains, start=1):
        if rel > 0:
            dcg += (math.pow(2.0, rel) - 1.0) / math.log2(idx + 1)

    # Compute Ideal DCG@K
    if graded_relevance:
        ideal_rels = sorted(
            [float(g) for k_id, g in graded_relevance.items() if g > 0],
            reverse=True
        )
    else:
        ideal_rels = [1.0] * len(relevant_ids)

    ideal_rels = ideal_rels[:k]
    idcg = 0.0
    for idx, rel in enumerate(ideal_rels, start=1):
        idcg += (math.pow(2.0, rel) - 1.0) / math.log2(idx + 1)

    if idcg == 0.0:
        return 0.0

    return float(dcg / idcg)

