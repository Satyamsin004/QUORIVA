"""
CS Scholar AI - Retrieval Evaluation Metrics
Computes Recall@K, Mean Reciprocal Rank (MRR), and Normalized Discounted Cumulative Gain (nDCG@K).
"""
import math
from typing import List, Set, Any
from uuid import UUID


def compute_recall_at_k(retrieved_ids: List[Any], relevant_ids: Set[Any], k: int) -> float:
    """
    Calculate Recall@K: fraction of relevant items retrieved in top K.
    """
    if not relevant_ids:
        return 0.0

    top_k = set(retrieved_ids[:k])
    hits = len(top_k.intersection(relevant_ids))
    return float(hits / len(relevant_ids))


def compute_mrr(retrieved_ids: List[Any], relevant_ids: Set[Any]) -> float:
    """
    Calculate Reciprocal Rank (RR): reciprocal of the 1-based rank of the first relevant item.
    """
    if not relevant_ids:
        return 0.0

    for idx, item in enumerate(retrieved_ids, start=1):
        if item in relevant_ids:
            return 1.0 / idx
    return 0.0


def compute_ndcg_at_k(retrieved_ids: List[Any], relevant_ids: Set[Any], k: int) -> float:
    """
    Calculate Normalized Discounted Cumulative Gain (nDCG@K) with binary relevance.
    """
    if not relevant_ids:
        return 0.0

    # Compute DCG@K
    dcg = 0.0
    for idx, item in enumerate(retrieved_ids[:k], start=1):
        rel = 1.0 if item in relevant_ids else 0.0
        dcg += rel / math.log2(idx + 1)

    # Compute Ideal DCG@K (all relevant items ranked at the top)
    idcg = 0.0
    ideal_hits = min(len(relevant_ids), k)
    for idx in range(1, ideal_hits + 1):
        idcg += 1.0 / math.log2(idx + 1)

    if idcg == 0.0:
        return 0.0

    return float(dcg / idcg)

