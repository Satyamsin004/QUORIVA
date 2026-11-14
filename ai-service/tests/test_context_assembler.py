"""
CS Scholar AI - Unit Tests for Context Assembler
Tests token budgeting, source ID assignment, and metadata formatting.
"""
from uuid import uuid4
from app.rag.context_assembler import ContextAssembler
from app.retrieval.schemas import SearchResultItem


def create_search_item(title: str, content: str, page: int = 1, rank: int = 1) -> SearchResultItem:
    return SearchResultItem(
        chunk_id=uuid4(),
        document_id=uuid4(),
        document_name=title,
        workspace_id=uuid4(),
        chunk_content=content,
        page_number=page,
        slide_number=None,
        section_heading="Memory Management",
        chunk_index=0,
        source_type="pdf",
        final_rank=rank,
        reranker_score=0.95 - (rank * 0.05),
        rrf_score=0.03,
        dense_rank=rank,
        keyword_rank=rank,
        dense_similarity=0.88,
        keyword_score=1.2,
        segment_index=0
    )


def test_context_assembler_basic():
    assembler = ContextAssembler(max_context_tokens=1000, max_chunks=5)
    items = [
        create_search_item("OS Silberschatz.pdf", "Paging avoids external fragmentation.", 10, 1),
        create_search_item("OS Tanenbaum.pdf", "Segmentation provides modular memory views.", 25, 2),
    ]

    context = assembler.assemble(items)

    assert len(context.chunks) == 2
    assert "S1" in context.source_map
    assert "S2" in context.source_map
    assert context.source_map["S1"].document_title == "OS Silberschatz.pdf"
    assert context.source_map["S1"].page_number == 10
    assert "[S1] Document: OS Silberschatz.pdf | Page: 10" in context.formatted_text
    assert "[S2] Document: OS Tanenbaum.pdf | Page: 25" in context.formatted_text
    assert context.total_tokens > 0


def test_context_assembler_budget_limit():
    # Set tiny token budget
    assembler = ContextAssembler(max_context_tokens=15, max_chunks=5)
    items = [
        create_search_item("Doc1.pdf", "This is a short chunk of text for testing.", 1, 1),
        create_search_item("Doc2.pdf", "Another long chunk of text that exceeds the limit.", 2, 2),
        create_search_item("Doc3.pdf", "Third chunk that should definitely be excluded.", 3, 3),
    ]

    context = assembler.assemble(items)
    # Should stop after first or second chunk to respect max_context_tokens
    assert len(context.chunks) < 3

