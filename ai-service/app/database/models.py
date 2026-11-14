"""
CS Scholar AI - Database SQLAlchemy Models
Maps to PostgreSQL tables managed by Spring Boot Flyway migrations.
"""
import uuid
from sqlalchemy import (
    Column,
    String,
    Integer,
    BigInteger,
    Text,
    DateTime,
    ForeignKey,
    UniqueConstraint
)
from sqlalchemy.dialects.postgresql import UUID, JSONB, TSVECTOR
from sqlalchemy.sql import func
from pgvector.sqlalchemy import Vector
from app.database.connection import Base


class Document(Base):
    __tablename__ = "documents"

    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    owner_id = Column(UUID(as_uuid=True), nullable=False)
    workspace_id = Column(UUID(as_uuid=True), nullable=False)
    title = Column(String(255), nullable=False)
    original_filename = Column(String(255), nullable=False)
    storage_key = Column(String(512), nullable=False, unique=True)
    file_type = Column(String(50), nullable=False)
    mime_type = Column(String(100), nullable=False)
    file_size = Column(BigInteger, nullable=False)
    checksum_sha256 = Column(String(64), nullable=True)
    page_count = Column(Integer, nullable=True)
    subject = Column(String(100), nullable=True)
    description = Column(Text, nullable=True)
    processing_status = Column(String(50), nullable=False, default="PENDING_UPLOAD")
    embedding_status = Column(String(50), nullable=False, default="NOT_STARTED")
    visibility = Column(String(50), nullable=False, default="PRIVATE")
    created_at = Column(DateTime(timezone=True), nullable=False, server_default=func.now())
    updated_at = Column(DateTime(timezone=True), nullable=False, server_default=func.now())
    uploaded_at = Column(DateTime(timezone=True), nullable=True)
    deleted_at = Column(DateTime(timezone=True), nullable=True)


class DocumentChunk(Base):
    __tablename__ = "document_chunks"

    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    document_id = Column(UUID(as_uuid=True), ForeignKey("documents.id", ondelete="CASCADE"), nullable=False)
    chunk_index = Column(Integer, nullable=False)
    processing_version = Column(Integer, nullable=False, default=1)
    page_number = Column(Integer, nullable=True)
    slide_number = Column(Integer, nullable=True)
    section_title = Column(String(255), nullable=True)
    content = Column(Text, nullable=False)
    token_count = Column(Integer, nullable=False)
    source_type = Column(String(50), nullable=False)
    extraction_method = Column(String(50), nullable=False)
    content_hash = Column(String(64), nullable=False)
    # Use "metadata" column in SQL, named extra_metadata in python to avoid collision with Base.metadata
    extra_metadata = Column("metadata", JSONB, nullable=False, default=dict)
    created_at = Column(DateTime(timezone=True), nullable=False, server_default=func.now())

    __table_args__ = (
        UniqueConstraint("document_id", "processing_version", "chunk_index", name="uq_document_chunk"),
    )


class ProcessingJob(Base):
    __tablename__ = "processing_jobs"

    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    document_id = Column(UUID(as_uuid=True), ForeignKey("documents.id", ondelete="CASCADE"), nullable=False)
    status = Column(String(50), nullable=False, default="QUEUED")
    attempts = Column(Integer, nullable=False, default=0)
    max_attempts = Column(Integer, nullable=False, default=3)
    error_message = Column(Text, nullable=True)
    task_id = Column(String(255), nullable=True)
    processing_version = Column(Integer, nullable=False, default=1)
    started_at = Column(DateTime(timezone=True), nullable=True)
    completed_at = Column(DateTime(timezone=True), nullable=True)
    created_at = Column(DateTime(timezone=True), nullable=False, server_default=func.now())
    updated_at = Column(DateTime(timezone=True), nullable=False, server_default=func.now())


class ChunkEmbedding(Base):
    __tablename__ = "chunk_embeddings"

    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    chunk_id = Column(UUID(as_uuid=True), ForeignKey("document_chunks.id", ondelete="CASCADE"), nullable=False)
    document_id = Column(UUID(as_uuid=True), ForeignKey("documents.id", ondelete="CASCADE"), nullable=False)
    segment_index = Column(Integer, nullable=False, default=0)
    embedding = Column(Vector(768), nullable=False)
    model_name = Column(String(150), nullable=False)
    model_version = Column(String(100), nullable=False)
    content_hash = Column(String(64), nullable=False)
    token_count = Column(Integer, nullable=False)
    created_at = Column(DateTime(timezone=True), nullable=False, server_default=func.now())

    __table_args__ = (
        UniqueConstraint("chunk_id", "model_name", "model_version", "segment_index", name="uq_chunk_embedding"),
    )


class EmbeddingJob(Base):
    __tablename__ = "embedding_jobs"

    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    document_id = Column(UUID(as_uuid=True), ForeignKey("documents.id", ondelete="CASCADE"), nullable=False)
    job_type = Column(String(50), nullable=False, default="EMBEDDING_GENERATION")
    status = Column(String(50), nullable=False, default="QUEUED")
    model_name = Column(String(150), nullable=False)
    model_version = Column(String(100), nullable=False)
    processing_version = Column(Integer, nullable=False, default=1)
    chunks_total = Column(Integer, nullable=False, default=0)
    chunks_processed = Column(Integer, nullable=False, default=0)
    embeddings_generated = Column(Integer, nullable=False, default=0)
    attempts = Column(Integer, nullable=False, default=0)
    max_attempts = Column(Integer, nullable=False, default=3)
    error_message = Column(Text, nullable=True)
    task_id = Column(String(255), nullable=True)
    started_at = Column(DateTime(timezone=True), nullable=True)
    completed_at = Column(DateTime(timezone=True), nullable=True)
    created_at = Column(DateTime(timezone=True), nullable=False, server_default=func.now())
    updated_at = Column(DateTime(timezone=True), nullable=False, server_default=func.now())
