"""
Integration tests for Hybrid Retrieval against real PostgreSQL container:
Verifies pgvector cosine distance, PostgreSQL tsvector full-text search, and workspace isolation.
"""
import uuid
import numpy as np
import pytest
from sqlalchemy import text

from app.database.connection import SessionLocal
from app.database.models import Document, DocumentChunk, ChunkEmbedding
from app.retrieval.dense_retriever import DenseRetriever
from app.retrieval.keyword_retriever import KeywordRetriever
from app.retrieval.rrf import ReciprocalRankFusion
from app.retrieval.reranker import CrossEncoderReranker
from app.retrieval.service import RetrievalService
from app.retrieval.schemas import InternalSearchRequest


@pytest.fixture
def db_session():
    """Provides a transactional database session rolled back after testing."""
    session = SessionLocal()
    yield session
    session.rollback()
    session.close()


def test_real_postgres_hybrid_retrieval_and_workspace_isolation(db_session):
    """
    Seed test documents and chunks in Workspace 1 and Workspace 2,
    then verify full-text search, dense vector retrieval, and strict workspace boundary isolation.
    """
    test_user_id = uuid.uuid4()
    ws_1 = uuid.uuid4()
    ws_2 = uuid.uuid4()

    # Create real user and workspaces to satisfy foreign keys
    db_session.execute(text("""
        INSERT INTO users (id, full_name, email, password_hash)
        VALUES (:uid, 'Retrieval Test User', :email, 'hash')
    """), {"uid": test_user_id, "email": f"test_{test_user_id}@example.com"})

    db_session.execute(text("""
        INSERT INTO workspaces (id, name, owner_id)
        VALUES (:ws1, 'Workspace 1', :uid), (:ws2, 'Workspace 2', :uid)
    """), {"ws1": ws_1, "ws2": ws_2, "uid": test_user_id})
    db_session.flush()

    try:
        # Create Document in Workspace 1
        doc_1 = Document(
            id=uuid.uuid4(),
            owner_id=test_user_id,
            workspace_id=ws_1,
            title="Data Structures & Algorithms",
            original_filename="dsa.pdf",
            storage_key=f"workspaces/{ws_1}/documents/dsa.pdf",
            file_type="PDF",
            mime_type="application/pdf",
            file_size=1024,
            processing_status="PROCESSED",
            embedding_status="EMBEDDED"
        )
        db_session.add(doc_1)

        # Create Document in Workspace 2 (isolated)
        doc_2 = Document(
            id=uuid.uuid4(),
            owner_id=test_user_id,
            workspace_id=ws_2,
            title="Private OS Notes",
            original_filename="os.pdf",
            storage_key=f"workspaces/{ws_2}/documents/os.pdf",
            file_type="PDF",
            mime_type="application/pdf",
            file_size=1024,
            processing_status="PROCESSED",
            embedding_status="EMBEDDED"
        )
        db_session.add(doc_2)
        db_session.flush()

        # Create Chunk in Workspace 1 with technical content
        chunk_1 = DocumentChunk(
            id=uuid.uuid4(),
            document_id=doc_1.id,
            chunk_index=0,
            processing_version=1,
            page_number=42,
            slide_number=None,
            section_title="Binary Search Trees",
            content="A binary search tree provides average O(log n) search time complexity by maintaining node order.",
            token_count=18,
            source_type="PDF",
            extraction_method="DIRECT_TEXT",
            content_hash="hash_bst_chunk_1",
            extra_metadata={}
        )
        db_session.add(chunk_1)

        # Create Chunk in Workspace 2 with similar technical words
        chunk_2 = DocumentChunk(
            id=uuid.uuid4(),
            document_id=doc_2.id,
            chunk_index=0,
            processing_version=1,
            page_number=10,
            slide_number=None,
            section_title="OS Process Trees",
            content="Process hierarchies in operating systems form a tree structure similar to a binary search tree.",
            token_count=16,
            source_type="PDF",
            extraction_method="DIRECT_TEXT",
            content_hash="hash_os_chunk_2",
            extra_metadata={}
        )
        db_session.add(chunk_2)
        db_session.flush()

        # Add normalized 768-dim embeddings for both chunks
        rng = np.random.RandomState(42)
        vec1 = rng.randn(768).astype(np.float32)
        vec1 /= np.linalg.norm(vec1)

        vec2 = rng.randn(768).astype(np.float32)
        vec2 /= np.linalg.norm(vec2)

        emb_1 = ChunkEmbedding(
            id=uuid.uuid4(),
            chunk_id=chunk_1.id,
            document_id=doc_1.id,
            segment_index=0,
            embedding=vec1.tolist(),
            model_name="BAAI/bge-base-en-v1.5",
            model_version="bge-base-en-v1.5",
            content_hash="hash_bst_chunk_1",
            token_count=18
        )
        db_session.add(emb_1)

        emb_2 = ChunkEmbedding(
            id=uuid.uuid4(),
            chunk_id=chunk_2.id,
            document_id=doc_2.id,
            segment_index=0,
            embedding=vec2.tolist(),
            model_name="BAAI/bge-base-en-v1.5",
            model_version="bge-base-en-v1.5",
            content_hash="hash_os_chunk_2",
            token_count=16
        )
        db_session.add(emb_2)
        db_session.flush()

        # 1. Test PostgreSQL Full-Text Keyword Search in Workspace 1
        kw_retriever = KeywordRetriever()
        results_ws1 = kw_retriever.search(db_session, workspace_id=ws_1, query_text="binary search tree", candidate_count=10)
        assert len(results_ws1) == 1
        assert results_ws1[0].chunk_id == chunk_1.id
        assert results_ws1[0].document_name == "Data Structures & Algorithms"
        assert results_ws1[0].page_number == 42
        assert results_ws1[0].keyword_score > 0.0

        # 2. Test Workspace Isolation: Workspace 1 search MUST NEVER return chunk from Workspace 2
        for r in results_ws1:
            assert r.chunk_id != chunk_2.id
            assert r.workspace_id == ws_1

        # 3. Test Dense Retrieval with exact query vector in Workspace 1
        dense_retriever = DenseRetriever()
        dense_results_ws1 = dense_retriever.search(db_session, workspace_id=ws_1, query_vector=vec1, candidate_count=10)
        assert len(dense_results_ws1) == 1
        assert dense_results_ws1[0].chunk_id == chunk_1.id
        assert dense_results_ws1[0].cosine_distance < 1e-4  # Exact vector match distance ≈ 0
        assert dense_results_ws1[0].dense_similarity > 0.999

        # Verify dense retrieval does not leak Workspace 2 chunk
        for r in dense_results_ws1:
            assert r.chunk_id != chunk_2.id

        # 4. Test Workspace 2 isolation
        dense_results_ws2 = dense_retriever.search(db_session, workspace_id=ws_2, query_vector=vec1, candidate_count=10)
        assert len(dense_results_ws2) == 1
        assert dense_results_ws2[0].chunk_id == chunk_2.id
        assert dense_results_ws2[0].workspace_id == ws_2

    finally:
        # Cascade delete workspaces and test user
        db_session.execute(text("DELETE FROM workspaces WHERE id IN (:ws1, :ws2)"), {"ws1": ws_1, "ws2": ws_2})
        db_session.execute(text("DELETE FROM users WHERE id = :uid"), {"uid": test_user_id})
        db_session.commit()
