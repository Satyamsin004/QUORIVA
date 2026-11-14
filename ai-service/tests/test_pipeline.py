"""
CS Scholar AI - Document Ingestion Pipeline Integration Tests
"""
import uuid
import pytest
import pymupdf
from sqlalchemy import text

from app.database.connection import get_db_session
from app.database.models import Document, DocumentChunk, ProcessingJob
from app.database.storage import storage_client
from app.ingestion.pipeline import IngestionPipeline


@pytest.fixture
def sample_pdf_in_s3():
    # 1. Create a PDF in memory
    doc = pymupdf.open()
    page1 = doc.new_page()
    page1.insert_text((50, 72), "Chapter 1: Relational Algebra & Database Systems.\nSelection and projection are fundamental.")
    page2 = doc.new_page()
    page2.insert_text((50, 72), "Chapter 2: Query Optimization.\nDynamic programming is used in System R optimizers.")
    pdf_bytes = doc.tobytes()
    doc.close()

    # 2. Upload to MinIO/S3
    storage_key = f"test-integration/{uuid.uuid4()}/database_systems.pdf"
    storage_client.client.put_object(
        Bucket=storage_client.bucket,
        Key=storage_key,
        Body=pdf_bytes,
        ContentType="application/pdf"
    )

    # 3. Create Document record in PostgreSQL
    doc_id = uuid.uuid4()
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
                VALUES (:uid, 'Pipeline Test User', :email, 'hash', 'STUDENT')
            """), {"uid": owner_id, "email": f"test_{owner_id}@example.com"})
            db.execute(text("""
                INSERT INTO workspaces (id, name, owner_id)
                VALUES (:ws_id, 'Pipeline Test Workspace', :uid)
            """), {"ws_id": ws_id, "uid": owner_id})
            db.commit()

        doc_record = Document(
            id=doc_id,
            owner_id=owner_id,
            workspace_id=ws_id,
            title="Database Systems Test",
            original_filename="database_systems.pdf",
            storage_key=storage_key,
            file_type="PDF",
            mime_type="application/pdf",
            file_size=len(pdf_bytes),
            processing_status="PENDING_PROCESSING"
        )
        db.add(doc_record)
        db.commit()

    yield {
        "document_id": doc_id,
        "storage_key": storage_key,
        "file_type": "PDF",
        "doc_record": doc_record
    }

    # Teardown
    try:
        storage_client.client.delete_object(Bucket=storage_client.bucket, Key=storage_key)
    except Exception:
        pass

    with get_db_session() as db:
        db.query(DocumentChunk).filter(DocumentChunk.document_id == doc_id).delete()
        db.query(ProcessingJob).filter(ProcessingJob.document_id == doc_id).delete()
        db.query(Document).filter(Document.id == doc_id).delete()
        if created_ws_id:
            db.execute(text("DELETE FROM workspaces WHERE id = :ws_id"), {"ws_id": created_ws_id})
        if created_user_id:
            db.execute(text("DELETE FROM users WHERE id = :uid"), {"uid": created_user_id})
        db.commit()


def test_end_to_end_ingestion_pipeline(sample_pdf_in_s3):
    pipeline = IngestionPipeline()
    doc_id = sample_pdf_in_s3["document_id"]
    storage_key = sample_pdf_in_s3["storage_key"]
    file_type = sample_pdf_in_s3["file_type"]

    result = pipeline.process_document(
        document_id=doc_id,
        storage_key=storage_key,
        file_type=file_type,
        processing_version=1
    )

    assert result["status"] == "PROCESSED"
    assert result["chunks_created"] >= 2
    assert result["total_tokens"] > 0
    assert result["page_count"] == 2

    # Verify persistence in database
    with get_db_session() as db:
        doc = db.query(Document).filter(Document.id == doc_id).first()
        assert doc.processing_status == "PROCESSED"
        assert doc.page_count == 2

        chunks = db.query(DocumentChunk).filter(
            DocumentChunk.document_id == doc_id,
            DocumentChunk.processing_version == 1
        ).order_by(DocumentChunk.chunk_index.asc()).all()

        assert len(chunks) == result["chunks_created"]
        assert chunks[0].page_number == 1
        assert chunks[1].page_number == 2
        assert len(chunks[0].content_hash) == 64
        assert chunks[0].source_type == "PDF"
        assert chunks[0].extraction_method == "DIRECT_TEXT"
