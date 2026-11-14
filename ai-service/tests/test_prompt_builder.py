"""
CS Scholar AI - Unit Tests for Prompt Builder
Tests system prompt persona, injection defense barriers, and conversation history inclusion.
"""
from uuid import uuid4
from app.rag.prompt_builder import PromptBuilder
from app.rag.schemas import AssembledContext, RetrievedContextChunk


def test_prompt_builder_structure():
    builder = PromptBuilder(history_max_messages=4)
    chunk = RetrievedContextChunk(
        source_id="S1",
        chunk_id=uuid4(),
        document_id=uuid4(),
        document_title="OS.pdf",
        content="Virtual memory maps pages to page frames.",
        token_count=10,
        page_number=14,
        slide_number=None,
        section_heading="Paging",
        relevance_score=0.92,
        rank=1
    )
    context = AssembledContext(
        chunks=[chunk],
        formatted_text="[S1] Document: OS.pdf | Page: 14\nVirtual memory maps pages to page frames.",
        total_tokens=10,
        source_map={"S1": chunk}
    )

    history = [
        {"role": "user", "content": "What is memory?"},
        {"role": "assistant", "content": "Memory stores instructions and data."},
    ]

    messages = builder.build_messages(
        query="Explain paging.",
        context=context,
        history=history
    )

    assert len(messages) == 4
    # System turn
    assert messages[0].role == "system"
    assert "Academic Tutor" in messages[0].content
    assert "INJECTION DEFENSE BARRIER" in messages[0].content

    # History turns
    assert messages[1].role == "user"
    assert messages[1].content == "What is memory?"
    assert messages[2].role == "assistant"
    assert messages[2].content == "Memory stores instructions and data."

    # User query turn with academic context tags
    assert messages[3].role == "user"
    assert "<academic_context>" in messages[3].content
    assert "</academic_context>" in messages[3].content
    assert "Virtual memory maps pages" in messages[3].content
    assert "Student Question:\nExplain paging." in messages[3].content

