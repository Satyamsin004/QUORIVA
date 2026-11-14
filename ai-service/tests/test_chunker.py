"""
CS Scholar AI - Structure-Aware Document Chunker Unit Tests
"""
from app.ingestion.chunking.tokenizer import TiktokenTokenizer
from app.ingestion.chunking.chunker import StructureAwareChunker
from app.ingestion.models import ParsedDocument, ContentElement, SourceType, ExtractionMethod


def test_tokenizer_token_counting():
    tokenizer = TiktokenTokenizer()
    text = "Computer science is the study of computation, information, and automation."
    tokens = tokenizer.count_tokens(text)
    assert tokens > 5
    assert tokens < 25


def test_chunker_basic_chunking():
    chunker = StructureAwareChunker(target_chunk_size=50, chunk_overlap=10, min_chunk_size=10, max_chunk_size=100)
    parsed_doc = ParsedDocument(
        source_type=SourceType.PDF,
        extraction_method=ExtractionMethod.DIRECT_TEXT,
        total_pages=1,
        elements=[
            ContentElement(
                element_type="paragraph",
                content="Operating systems provide abstraction and resource management for computer hardware.",
                page_number=1,
                section_title="OS Overview"
            )
        ]
    )
    chunks = chunker.chunk_document(parsed_doc)
    assert len(chunks) == 1
    assert chunks[0].page_number == 1
    assert chunks[0].section_title == "OS Overview"
    assert chunks[0].source_type == "PDF"
    assert len(chunks[0].content_hash) == 64
    assert chunks[0].token_count > 0


def test_chunker_large_text_splitting_with_overlap():
    chunker = StructureAwareChunker(target_chunk_size=30, chunk_overlap=10, min_chunk_size=5, max_chunk_size=50)

    # Generate a long text with multiple paragraphs
    paragraphs = [
        f"Paragraph {i}: Virtual memory provides an illusion of dedicated memory space to processes. "
        f"Pages are mapped to physical frames using page tables."
        for i in range(10)
    ]
    long_text = "\n\n".join(paragraphs)

    parsed_doc = ParsedDocument(
        source_type=SourceType.PDF,
        extraction_method=ExtractionMethod.DIRECT_TEXT,
        total_pages=2,
        elements=[
            ContentElement(
                element_type="page_content",
                content=long_text,
                page_number=2,
                section_title="Virtual Memory"
            )
        ]
    )

    chunks = chunker.chunk_document(parsed_doc)
    assert len(chunks) > 1

    # Check indices are consecutive
    for idx, c in enumerate(chunks):
        assert c.chunk_index == idx
        assert c.page_number == 2
        assert c.section_title == "Virtual Memory"
        assert len(c.content_hash) == 64


def test_content_hash_consistency():
    chunker = StructureAwareChunker()
    doc1 = ParsedDocument(
        source_type=SourceType.DOCX,
        extraction_method=ExtractionMethod.DIRECT_TEXT,
        elements=[ContentElement(element_type="p", content="Deterministic hash verification text.")]
    )
    doc2 = ParsedDocument(
        source_type=SourceType.DOCX,
        extraction_method=ExtractionMethod.DIRECT_TEXT,
        elements=[ContentElement(element_type="p", content="Deterministic hash verification text.")]
    )
    chunks1 = chunker.chunk_document(doc1)
    chunks2 = chunker.chunk_document(doc2)
    assert chunks1[0].content_hash == chunks2[0].content_hash

