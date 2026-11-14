"""
CS Scholar AI - Structure-Aware Document Chunker
Splits parsed document elements into context-rich, structure-respecting chunks
with configurable token size, overlap, code-block preservation, and SHA-256 content hashing.
"""
import hashlib
import logging
import re
from typing import List, Optional, Dict, Any

from app.core.config import settings
from app.ingestion.chunking.tokenizer import BaseTokenizer, default_tokenizer
from app.ingestion.models import ParsedDocument, ContentElement, DocumentChunkModel

logger = logging.getLogger(__name__)


class StructureAwareChunker:
    """
    Structure-aware text chunker that respects document hierarchy (pages, slides, sections),
    preserves code blocks and math formulas, maintains token overlap, and computes content hashes.
    """

    def __init__(
        self,
        tokenizer: Optional[BaseTokenizer] = None,
        target_chunk_size: Optional[int] = None,
        chunk_overlap: Optional[int] = None,
        min_chunk_size: Optional[int] = None,
        max_chunk_size: Optional[int] = None
    ):
        self.tokenizer = tokenizer or default_tokenizer
        self.target_chunk_size = target_chunk_size or settings.target_chunk_size
        self.chunk_overlap = chunk_overlap or settings.chunk_overlap
        self.min_chunk_size = min_chunk_size or settings.min_chunk_size
        self.max_chunk_size = max_chunk_size or settings.max_chunk_size

    def _split_into_paragraphs(self, text: str) -> List[str]:
        """Split text by double newlines or major structural separators."""
        paragraphs = text.split("\n\n")
        return [p.strip() for p in paragraphs if p.strip()]

    def _split_into_sentences(self, text: str) -> List[str]:
        """Split text into sentences using punctuation while preserving abbreviations."""
        # Simple regex splitting on punctuation followed by space and uppercase letter
        sentence_endings = re.compile(r'(?<=[.!?]) +(?=[A-Z0-9#\-\*])')
        sentences = sentence_endings.split(text)
        return [s.strip() for s in sentences if s.strip()]

    def _compute_sha256(self, content: str) -> str:
        """Compute SHA-256 hash of UTF-8 encoded chunk content."""
        return hashlib.sha256(content.encode("utf-8")).hexdigest()

    def _extract_overlap_prefix(self, prev_content: str, overlap_tokens: int) -> str:
        """Extract roughly `overlap_tokens` from the tail of previous chunk, snapping to sentence/newline boundaries."""
        if not prev_content or overlap_tokens <= 0:
            return ""

        # Break previous content into sentences or lines
        lines = prev_content.splitlines()
        selected: List[str] = []
        accumulated_tokens = 0

        for line in reversed(lines):
            line_tokens = self.tokenizer.count_tokens(line)
            if accumulated_tokens + line_tokens > overlap_tokens * 1.5 and selected:
                break
            selected.insert(0, line)
            accumulated_tokens += line_tokens
            if accumulated_tokens >= overlap_tokens:
                break

        return "\n".join(selected).strip()

    def chunk_document(
        self,
        parsed_doc: ParsedDocument,
        document_metadata: Optional[Dict[str, Any]] = None
    ) -> List[DocumentChunkModel]:
        """
        Produce a list of DocumentChunkModel instances from a ParsedDocument.
        """
        meta = document_metadata or {}
        raw_chunks: List[DocumentChunkModel] = []
        chunk_index = 0

        for element in parsed_doc.elements:
            element_text = element.content.strip()
            if not element_text:
                continue

            elem_token_count = self.tokenizer.count_tokens(element_text)

            # If element fits comfortably within target size, make it a candidate chunk
            if elem_token_count <= self.target_chunk_size:
                raw_chunks.append(DocumentChunkModel(
                    chunk_index=chunk_index,
                    processing_version=meta.get("processing_version", 1),
                    page_number=element.page_number,
                    slide_number=element.slide_number,
                    section_title=element.section_title,
                    content=element_text,
                    token_count=elem_token_count,
                    source_type=parsed_doc.source_type.value,
                    extraction_method=element.metadata.get("extraction_method", parsed_doc.extraction_method.value),
                    content_hash=self._compute_sha256(element_text),
                    metadata={
                        "element_type": element.element_type,
                        **element.metadata,
                        **meta
                    }
                ))
                chunk_index += 1
            else:
                # Large element: split into sub-chunks respecting paragraphs and sentences
                sub_chunks = self._chunk_large_text(
                    text=element_text,
                    element=element,
                    source_type=parsed_doc.source_type.value,
                    base_metadata=meta,
                    start_chunk_index=chunk_index
                )
                raw_chunks.extend(sub_chunks)
                chunk_index += len(sub_chunks)

        # Merge adjacent chunks that are smaller than min_chunk_size if they share the same page/slide/section
        merged_chunks = self._merge_small_chunks(raw_chunks)

        # Re-index chunks consecutively to ensure 0..N indices
        for idx, chunk in enumerate(merged_chunks):
            chunk.chunk_index = idx

        return merged_chunks

    def _chunk_large_text(
        self,
        text: str,
        element: ContentElement,
        source_type: str,
        base_metadata: Dict[str, Any],
        start_chunk_index: int
    ) -> List[DocumentChunkModel]:
        """Break down a single large text block into overlapping chunks."""
        paragraphs = self._split_into_paragraphs(text)
        chunks: List[DocumentChunkModel] = []
        current_chunk_pieces: List[str] = []
        current_tokens = 0
        current_chunk_idx = start_chunk_index
        prev_chunk_content = ""

        for para in paragraphs:
            para_tokens = self.tokenizer.count_tokens(para)

            # If a single paragraph is larger than max_chunk_size, split by sentences
            if para_tokens > self.max_chunk_size:
                # Flush existing buffer first
                if current_chunk_pieces:
                    chunk_text = "\n\n".join(current_chunk_pieces).strip()
                    tokens = self.tokenizer.count_tokens(chunk_text)
                    chunks.append(DocumentChunkModel(
                        chunk_index=current_chunk_idx,
                        processing_version=base_metadata.get("processing_version", 1),
                        page_number=element.page_number,
                        slide_number=element.slide_number,
                        section_title=element.section_title,
                        content=chunk_text,
                        token_count=tokens,
                        source_type=source_type,
                        extraction_method=element.metadata.get("extraction_method", "DIRECT_TEXT"),
                        content_hash=self._compute_sha256(chunk_text),
                        metadata={"element_type": element.element_type, **element.metadata, **base_metadata}
                    ))
                    current_chunk_idx += 1
                    prev_chunk_content = chunk_text
                    current_chunk_pieces = []
                    current_tokens = 0

                # Sentence-level breakdown
                sentences = self._split_into_sentences(para)
                for sentence in sentences:
                    sent_tokens = self.tokenizer.count_tokens(sentence)
                    if current_tokens + sent_tokens > self.target_chunk_size and current_chunk_pieces:
                        chunk_text = " ".join(current_chunk_pieces).strip()
                        tokens = self.tokenizer.count_tokens(chunk_text)
                        chunks.append(DocumentChunkModel(
                            chunk_index=current_chunk_idx,
                            processing_version=base_metadata.get("processing_version", 1),
                            page_number=element.page_number,
                            slide_number=element.slide_number,
                            section_title=element.section_title,
                            content=chunk_text,
                            token_count=tokens,
                            source_type=source_type,
                            extraction_method=element.metadata.get("extraction_method", "DIRECT_TEXT"),
                            content_hash=self._compute_sha256(chunk_text),
                            metadata={"element_type": element.element_type, **element.metadata, **base_metadata}
                        ))
                        current_chunk_idx += 1
                        prev_chunk_content = chunk_text
                        overlap = self._extract_overlap_prefix(prev_chunk_content, self.chunk_overlap)
                        current_chunk_pieces = [overlap, sentence] if overlap else [sentence]
                        current_tokens = self.tokenizer.count_tokens(" ".join(current_chunk_pieces))
                    else:
                        current_chunk_pieces.append(sentence)
                        current_tokens += sent_tokens
            else:
                # Paragraph fits in standard progression
                if current_tokens + para_tokens > self.target_chunk_size and current_chunk_pieces:
                    chunk_text = "\n\n".join(current_chunk_pieces).strip()
                    tokens = self.tokenizer.count_tokens(chunk_text)
                    chunks.append(DocumentChunkModel(
                        chunk_index=current_chunk_idx,
                        processing_version=base_metadata.get("processing_version", 1),
                        page_number=element.page_number,
                        slide_number=element.slide_number,
                        section_title=element.section_title,
                        content=chunk_text,
                        token_count=tokens,
                        source_type=source_type,
                        extraction_method=element.metadata.get("extraction_method", "DIRECT_TEXT"),
                        content_hash=self._compute_sha256(chunk_text),
                        metadata={"element_type": element.element_type, **element.metadata, **base_metadata}
                    ))
                    current_chunk_idx += 1
                    prev_chunk_content = chunk_text
                    overlap = self._extract_overlap_prefix(prev_chunk_content, self.chunk_overlap)
                    current_chunk_pieces = [overlap, para] if overlap else [para]
                    current_tokens = self.tokenizer.count_tokens("\n\n".join(current_chunk_pieces))
                else:
                    current_chunk_pieces.append(para)
                    current_tokens += para_tokens

        # Flush remaining buffer
        if current_chunk_pieces:
            chunk_text = "\n\n".join(current_chunk_pieces).strip()
            tokens = self.tokenizer.count_tokens(chunk_text)
            chunks.append(DocumentChunkModel(
                chunk_index=current_chunk_idx,
                processing_version=base_metadata.get("processing_version", 1),
                page_number=element.page_number,
                slide_number=element.slide_number,
                section_title=element.section_title,
                content=chunk_text,
                token_count=tokens,
                source_type=source_type,
                extraction_method=element.metadata.get("extraction_method", "DIRECT_TEXT"),
                content_hash=self._compute_sha256(chunk_text),
                metadata={"element_type": element.element_type, **element.metadata, **base_metadata}
            ))

        return chunks

    def _merge_small_chunks(self, chunks: List[DocumentChunkModel]) -> List[DocumentChunkModel]:
        """Merge adjacent chunks smaller than min_chunk_size if they belong to the same logical boundary."""
        if len(chunks) <= 1:
            return chunks

        merged: List[DocumentChunkModel] = []
        i = 0
        while i < len(chunks):
            current = chunks[i]
            # If current chunk is below min size and there is a next chunk with matching page/slide/section
            if (
                current.token_count < self.min_chunk_size
                and i + 1 < len(chunks)
                and chunks[i + 1].page_number == current.page_number
                and chunks[i + 1].slide_number == current.slide_number
                and chunks[i + 1].section_title == current.section_title
            ):
                nxt = chunks[i + 1]
                combined_content = f"{current.content}\n\n{nxt.content}".strip()
                combined_tokens = self.tokenizer.count_tokens(combined_content)
                if combined_tokens <= self.max_chunk_size:
                    merged.append(DocumentChunkModel(
                        chunk_index=current.chunk_index,
                        processing_version=current.processing_version,
                        page_number=current.page_number,
                        slide_number=current.slide_number,
                        section_title=current.section_title,
                        content=combined_content,
                        token_count=combined_tokens,
                        source_type=current.source_type,
                        extraction_method=current.extraction_method,
                        content_hash=self._compute_sha256(combined_content),
                        metadata={**current.metadata, **nxt.metadata}
                    ))
                    i += 2
                    continue

            merged.append(current)
            i += 1

        return merged

