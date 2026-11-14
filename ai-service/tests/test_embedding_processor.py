"""
CS Scholar AI - Embedding Processor Unit Tests
"""
import uuid
from unittest.mock import MagicMock
import numpy as np
import pytest

from app.embeddings.processor import EmbeddingProcessor
from app.database.models import DocumentChunk


def test_process_document_empty_chunks():
    mock_repo = MagicMock()
    mock_repo.get_document_chunks.return_value = []

    processor = EmbeddingProcessor(repository=mock_repo)
    result = processor.process_document_embeddings(document_id=uuid.uuid4())

    assert result["status"] == "COMPLETED"
    assert result["chunks_processed"] == 0
    assert result["embeddings_generated"] == 0
    mock_repo.update_document_embedding_status.assert_called_with(mock_repo.get_document_chunks.call_args[0][0], "COMPLETED")


def test_process_document_with_chunks_skips_unchanged():
    doc_id = uuid.uuid4()
    chunk1_id = uuid.uuid4()
    chunk1 = DocumentChunk(
        id=chunk1_id,
        document_id=doc_id,
        chunk_index=0,
        content="Stable chunk content that has already been embedded.",
        token_count=10
    )

    mock_tokenizer = MagicMock()
    mock_tokenizer.segment_text.return_value = [(chunk1.content, 10)]

    mock_generator = MagicMock()

    mock_repo = MagicMock()
    mock_repo.get_document_chunks.return_value = [chunk1]

    # Mock existing hashes matching the segment hash
    seg_hash = EmbeddingProcessor._compute_hash(chunk1.content)
    mock_repo.get_existing_embeddings_hashes.return_value = {(chunk1_id, 0): seg_hash}

    processor = EmbeddingProcessor(
        tokenizer=mock_tokenizer,
        generator=mock_generator,
        repository=mock_repo
    )

    result = processor.process_document_embeddings(document_id=doc_id, force_reembed=False)

    assert result["status"] == "COMPLETED"
    assert result["chunks_processed"] == 1
    assert result["embeddings_generated"] == 0
    assert result["skipped_embeddings"] == 1
    # Generator was never called because chunk was unchanged!
    mock_generator.generate_embeddings.assert_not_called()


def test_process_document_generates_when_new_or_forced():
    doc_id = uuid.uuid4()
    chunk1_id = uuid.uuid4()
    chunk1 = DocumentChunk(
        id=chunk1_id,
        document_id=doc_id,
        chunk_index=0,
        content="New chunk requiring vector embedding calculation.",
        token_count=8
    )

    mock_tokenizer = MagicMock()
    mock_tokenizer.segment_text.return_value = [(chunk1.content, 8)]

    dummy_vector = np.ones(768, dtype=np.float32) / np.sqrt(768)
    mock_generator = MagicMock()
    mock_generator.generate_embeddings.return_value = [dummy_vector]

    mock_repo = MagicMock()
    mock_repo.get_document_chunks.return_value = [chunk1]
    mock_repo.get_existing_embeddings_hashes.return_value = {}  # No existing embeddings
    mock_repo.save_embeddings.return_value = 1

    processor = EmbeddingProcessor(
        tokenizer=mock_tokenizer,
        generator=mock_generator,
        repository=mock_repo
    )

    result = processor.process_document_embeddings(document_id=doc_id)

    assert result["status"] == "COMPLETED"
    assert result["chunks_processed"] == 1
    assert result["embeddings_generated"] == 1
    mock_generator.generate_embeddings.assert_called_once()
    mock_repo.save_embeddings.assert_called_once()

