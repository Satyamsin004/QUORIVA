"""
Unit tests for Reciprocal Rank Fusion (RRF)
"""
import uuid
import pytest
from app.retrieval.rrf import ReciprocalRankFusion, HybridCandidate
from app.retrieval.dense_retriever import DenseCandidate
from app.retrieval.keyword_retriever import KeywordCandidate


def make_dense_candidate(chunk_id, rank, dist=0.1):
    return DenseCandidate(
        chunk_id=chunk_id,
        document_id=uuid.uuid4(),
        document_name="Doc1",
        workspace_id=uuid.uuid4(),
        chunk_content=f"Content for {chunk_id}",
        page_number=1,
        slide_number=None,
        section_heading="Section A",
        chunk_index=0,
        source_type="PDF",
        dense_rank=rank,
        cosine_distance=dist,
        dense_similarity=1.0 - dist,
        segment_index=0
    )


def make_keyword_candidate(chunk_id, rank, score=0.5):
    return KeywordCandidate(
        chunk_id=chunk_id,
        document_id=uuid.uuid4(),
        document_name="Doc1",
        workspace_id=uuid.uuid4(),
        chunk_content=f"Content for {chunk_id}",
        page_number=1,
        slide_number=None,
        section_heading="Section A",
        chunk_index=0,
        source_type="PDF",
        keyword_rank=rank,
        keyword_score=score
    )


def test_rrf_scoring_and_deduplication():
    rrf = ReciprocalRankFusion(default_k=60, default_limit=10)

    chunk_a = uuid.uuid4()
    chunk_b = uuid.uuid4()
    chunk_c = uuid.uuid4()

    # chunk_a is in both dense (rank 1) and keyword (rank 2)
    # chunk_b is dense only (rank 2)
    # chunk_c is keyword only (rank 1)
    dense = [make_dense_candidate(chunk_a, rank=1), make_dense_candidate(chunk_b, rank=2)]
    keyword = [make_keyword_candidate(chunk_c, rank=1), make_keyword_candidate(chunk_a, rank=2)]

    results = rrf.fuse(dense, keyword, k=60, limit=10)

    assert len(results) == 3

    # Expected scores:
    # chunk_a: 1/(60+1) + 1/(60+2) = 1/61 + 1/62 ≈ 0.016393 + 0.016129 = 0.032522
    # chunk_c: 1/(60+1) = 1/61 ≈ 0.016393
    # chunk_b: 1/(60+2) = 1/62 ≈ 0.016129

    assert results[0].chunk_id == chunk_a
    assert pytest.approx(results[0].rrf_score, rel=1e-4) == (1.0 / 61.0 + 1.0 / 62.0)
    assert results[0].dense_rank == 1
    assert results[0].keyword_rank == 2

    assert results[1].chunk_id == chunk_c
    assert pytest.approx(results[1].rrf_score, rel=1e-4) == (1.0 / 61.0)
    assert results[1].dense_rank is None
    assert results[1].keyword_rank == 1

    assert results[2].chunk_id == chunk_b
    assert pytest.approx(results[2].rrf_score, rel=1e-4) == (1.0 / 62.0)
    assert results[2].dense_rank == 2
    assert results[2].keyword_rank is None


def test_rrf_limit_and_empty():
    rrf = ReciprocalRankFusion()

    # Empty inputs
    assert rrf.fuse([], []) == []

    # Dense only
    chunk_1 = uuid.uuid4()
    dense_only = [make_dense_candidate(chunk_1, 1)]
    results = rrf.fuse(dense_only, [])
    assert len(results) == 1
    assert results[0].chunk_id == chunk_1
    assert results[0].dense_rank == 1
    assert results[0].keyword_rank is None

    # Limit enforced
    cands = [make_dense_candidate(uuid.uuid4(), i) for i in range(1, 20)]
    limited = rrf.fuse(cands, [], limit=5)
    assert len(limited) == 5

