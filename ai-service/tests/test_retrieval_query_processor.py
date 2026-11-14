"""
Unit tests for Search Query Processor
"""
import pytest
from app.retrieval.query_processor import QueryProcessor
from app.retrieval.exceptions import QueryValidationError


def test_query_processor_success():
    processor = QueryProcessor(query_instruction="Instruction: ")
    clean, instructed = processor.process("  binary  search  tree  ")
    assert clean == "binary search tree"
    assert instructed == "Instruction: binary search tree"


def test_query_processor_empty_or_none():
    processor = QueryProcessor()
    with pytest.raises(QueryValidationError) as exc:
        processor.process("")
    assert "cannot be empty" in exc.value.message

    with pytest.raises(QueryValidationError) as exc:
        processor.process("   \t  \n  ")
    assert "cannot be empty" in exc.value.message

    with pytest.raises(QueryValidationError) as exc:
        processor.process(None)
    assert "cannot be empty" in exc.value.message


def test_query_processor_exceeds_max_length():
    processor = QueryProcessor(max_length=50)
    with pytest.raises(QueryValidationError) as exc:
        processor.process("A" * 51)
    assert "exceeds maximum length" in exc.value.message


def test_query_processor_whitespace_normalization():
    processor = QueryProcessor(query_instruction="Represent: ")
    clean, instructed = processor.process("Dijkstra's\n\talgorithm\r\nfor  shortest   path")
    assert clean == "Dijkstra's algorithm for shortest path"
    assert instructed == "Represent: Dijkstra's algorithm for shortest path"

