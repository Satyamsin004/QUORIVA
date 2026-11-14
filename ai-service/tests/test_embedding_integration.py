"""
CS Scholar AI - Embedding & pgvector Database Integration Tests
"""
import uuid
import numpy as np
import pytest
from sqlalchemy import text

from app.database.connection import get_db_session
from app.database.models import Document, DocumentChunk, ChunkEmbedding, EmbeddingJob
from app.embeddings.processor import EmbeddingProcessor
from app.embeddings.generator import EmbeddingGenerator
from app.embeddings.config import embedding_config


@pytest.fixture
def sample_chunk_in_db():
    doc_id = uuid.uuid4()
    chunk_id = uuid.uuid4()
    created_user_id = None
    created_ws_id = None

    with get_db_session() as db:
        user_row = db.execute(text("SELECT id FROM users LIMIT 1")).fetchone()
        ws_row = db.execute(text("SELECT id FROM workspaces LIMIT 1")).fetchone()

        if user_row and ws_row:
            owner_id = user_row[0]
            ws_id = ws_row[0]
        else:
            owner_id = uuid.uuid4()
            ws_id = uuid.uuid4()
            created_user_id = owner_id
            created_ws_id = ws_id
            db.execute(text("""
                INSERT INTO users (id, full_name, email, password_hash, role)
                VALUES (:uid, 'Embedding Test User', :email, 'hash', 'STUDENT')
            """), {"uid": owner_id, "email": f"test_{owner_id}@example.com"})
            db.execute(text("""
                INSERT INTO workspaces (id, name, owner_id)
                VALUES (:ws_id, 'Embedding Test Workspace', :uid)
            """), {"ws_id": ws_id, "uid": owner_id})
            db.commit()

        doc = Document(
            id=doc_id,
            owner_id=owner_id,
            workspace_id=ws_id,
            title="Vector Index Test",
            original_filename="vector_test.pdf",
            storage_key=f"test-integration/{doc_id}/vector_test.pdf",
            file_type="PDF",
            mime_type="application/pdf",
            file_size=2048,
            processing_status="PROCESSED",
            embedding_status="NOT_STARTED"
        )
        db.add(doc)

        chunk = DocumentChunk(
            id=chunk_id,
            document_id=doc_id,
            chunk_index=0,
            processing_version=1,
            page_number=1,
            section_title="Vector Search",
            content="Vector indexing enables fast nearest-neighbor lookups using cosine distance.",
            token_count=12,
            source_type="PDF",
            extraction_method="DIRECT_TEXT",
            content_hash="abc123hash",
            extra_metadata={}
        )
        db.add(chunk)
        db.commit()

    yield {"document_id": doc_id, "chunk_id": chunk_id}

    # Teardown
    with get_db_session() as db:
        db.query(ChunkEmbedding).filter(ChunkEmbedding.document_id == doc_id).delete()
        db.query(EmbeddingJob).filter(EmbeddingJob.document_id == doc_id).delete()
        db.query(DocumentChunk).filter(DocumentChunk.document_id == doc_id).delete()
        db.query(Document).filter(Document.id == doc_id).delete()
        if created_ws_id:
            db.execute(text("DELETE FROM workspaces WHERE id = :ws_id"), {"ws_id": created_ws_id})
        if created_user_id:
            db.execute(text("DELETE FROM users WHERE id = :uid"), {"uid": created_user_id})
        db.commit()


def test_embedding_pgvector_persistence(sample_chunk_in_db):
    doc_id = sample_chunk_in_db["document_id"]
    chunk_id = sample_chunk_in_db["chunk_id"]

    # Use generator with deterministic synthetic normalized 768-dim vector
    dummy_vec = np.zeros(768, dtype=np.float32)
    dummy_vec[0] = 1.0  # Unit vector
    mock_generator = EmbeddingGenerator(expected_dimension=768)
    mock_generator.generate_embeddings = lambda texts: [dummy_vec]

    processor = EmbeddingProcessor(generator=mock_generator)
    result = processor.process_document_embeddings(document_id=doc_id)

    assert result["status"] == "COMPLETED"
    assert result["embeddings_generated"] == 1

    # Verify directly via PostgreSQL pgvector
    with get_db_session() as db:
        doc = db.query(Document).filter(Document.id == doc_id).first()
        assert doc.embedding_status == "COMPLETED"

        emb_records = db.query(ChunkEmbedding).filter(ChunkEmbedding.chunk_id == chunk_id).all()
        assert len(emb_records) == 1
        record = emb_records[0]
        assert record.segment_index == 0
        assert record.model_name == embedding_config.model_name
        assert record.model_version == embedding_config.model_version
        assert len(record.embedding) == 768
        assert record.embedding[0] == 1.0
        assert record.embedding[1] == 0.0

        # Test native pgvector cosine distance operator (<=>) in SQL
        res = db.execute(
            text("SELECT embedding <=> CAST(:query_vec AS vector) AS cosine_dist FROM chunk_embeddings WHERE id = :emb_id"),
            {"query_vec": str(dummy_vec.tolist()), "emb_id": str(record.id)}
        ).fetchone()

        assert res is not None
        # Distance between identical unit vectors should be approx 0.0
        assert abs(float(res[0])) < 1e-5

