"""
CS Scholar AI - Ingestion Data Models
"""
from enum import Enum
from typing import Dict, Any, List, Optional
from pydantic import BaseModel, Field


class SourceType(str, Enum):
    PDF = "PDF"
    DOCX = "DOCX"
    PPTX = "PPTX"


class ExtractionMethod(str, Enum):
    DIRECT_TEXT = "DIRECT_TEXT"
    OCR = "OCR"
    HYBRID = "HYBRID"


class ContentElement(BaseModel):
    """A granular structural unit of extracted content (heading, paragraph, table, slide, etc.)."""
    element_type: str = Field(..., description="Element type: heading, paragraph, table, code, slide_notes, etc.")
    content: str = Field(..., description="Normalized text representation of the element")
    page_number: Optional[int] = Field(None, description="1-indexed document page number")
    slide_number: Optional[int] = Field(None, description="1-indexed presentation slide number")
    section_title: Optional[str] = Field(None, description="Current enclosing section or heading title")
    metadata: Dict[str, Any] = Field(default_factory=dict, description="Custom metadata for this element")


class ParsedDocument(BaseModel):
    """Complete structured output from parsing a document file."""
    source_type: SourceType
    extraction_method: ExtractionMethod
    total_pages: Optional[int] = None
    total_slides: Optional[int] = None
    elements: List[ContentElement] = Field(default_factory=list)
    metadata: Dict[str, Any] = Field(default_factory=dict)


class DocumentChunkModel(BaseModel):
    """Structure-aware chunk ready for database persistence and subsequent indexing."""
    chunk_index: int
    processing_version: int = 1
    page_number: Optional[int] = None
    slide_number: Optional[int] = None
    section_title: Optional[str] = None
    content: str
    token_count: int
    source_type: str
    extraction_method: str
    content_hash: str
    metadata: Dict[str, Any] = Field(default_factory=dict)

