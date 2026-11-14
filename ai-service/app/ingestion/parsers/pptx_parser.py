"""
CS Scholar AI - Ingestion PPTX Parser
Structure-aware PowerPoint parser using python-pptx.
Extracts slide titles, body shapes, tables, and speaker notes in presentation order.
"""
import logging
from typing import Dict, Any, Optional, List
import pptx
from pptx.shapes.autoshape import Shape

from app.ingestion.base import BaseParser
from app.ingestion.exceptions import ParserError, EmptyDocumentError
from app.ingestion.models import (
    ParsedDocument,
    ContentElement,
    SourceType,
    ExtractionMethod
)
from app.ingestion.normalizer import TextNormalizer

logger = logging.getLogger(__name__)


class PPTXParser(BaseParser):
    """PPTX parser powered by python-pptx."""

    def can_parse(self, file_type: str, filename_or_path: str) -> bool:
        norm_type = file_type.upper().strip() if file_type else ""
        norm_path = filename_or_path.lower().strip() if filename_or_path else ""
        return norm_type == "PPTX" or norm_path.endswith(".pptx")

    def _format_table_as_markdown(self, table) -> str:
        """Convert a pptx Table into a clean markdown table string."""
        rows_data = []
        for row in table.rows:
            row_cells = [TextNormalizer.clean(cell.text).replace("\n", " ").strip() for cell in row.cells]
            rows_data.append(row_cells)

        if not rows_data:
            return ""

        max_cols = max(len(r) for r in rows_data)
        if max_cols == 0:
            return ""

        lines = []
        header = rows_data[0] + [""] * (max_cols - len(rows_data[0]))
        lines.append("| " + " | ".join(header) + " |")
        lines.append("| " + " | ".join(["---"] * max_cols) + " |")

        for row in rows_data[1:]:
            padded_row = row + [""] * (max_cols - len(row))
            lines.append("| " + " | ".join(padded_row) + " |")

        return "\n".join(lines)

    def parse(self, file_path: str, metadata: Optional[Dict[str, Any]] = None) -> ParsedDocument:
        meta = metadata or {}
        try:
            prs = pptx.Presentation(file_path)
        except Exception as e:
            raise ParserError(f"Failed to open PPTX presentation: {e}", {"path": file_path})

        total_slides = len(prs.slides)
        if total_slides == 0:
            raise EmptyDocumentError("The PPTX presentation contains 0 slides.", {"path": file_path})

        elements: List[ContentElement] = []

        for slide_idx, slide in enumerate(prs.slides):
            slide_number = slide_idx + 1
            slide_title = ""

            # Check if slide has a title shape
            if slide.shapes.title and slide.shapes.title.has_text_frame:
                slide_title = slide.shapes.title.text_frame.text.strip()

            effective_title = slide_title or f"Slide {slide_number}"

            slide_text_pieces: List[str] = []
            if slide_title:
                slide_text_pieces.append(f"## {slide_title}")

            # Extract shapes in order
            for shape in slide.shapes:
                if shape == slide.shapes.title:
                    continue  # already captured

                if shape.has_text_frame:
                    text = shape.text_frame.text.strip()
                    if text:
                        cleaned = TextNormalizer.clean(text)
                        if cleaned:
                            slide_text_pieces.append(cleaned)
                elif shape.has_table:
                    tbl_md = self._format_table_as_markdown(shape.table)
                    if tbl_md.strip():
                        slide_text_pieces.append(tbl_md)

            # Extract speaker notes if available
            speaker_notes = ""
            try:
                if slide.has_notes_slide and slide.notes_slide.notes_text_frame:
                    notes_text = slide.notes_slide.notes_text_frame.text.strip()
                    if notes_text:
                        cleaned_notes = TextNormalizer.clean(notes_text)
                        if cleaned_notes:
                            speaker_notes = cleaned_notes
                            slide_text_pieces.append(f"**Presenter Notes:**\n{cleaned_notes}")
            except Exception as notes_err:
                logger.debug("Could not read notes on slide %d: %s", slide_number, notes_err)

            combined_slide_content = "\n\n".join(slide_text_pieces).strip()

            if combined_slide_content:
                elements.append(ContentElement(
                    element_type="slide_content",
                    content=combined_slide_content,
                    slide_number=slide_number,
                    section_title=effective_title,
                    metadata={
                        "slide_title": slide_title,
                        "has_notes": bool(speaker_notes),
                        "shape_count": len(slide.shapes)
                    }
                ))

        if not elements:
            raise EmptyDocumentError("The PPTX presentation contains no extractable text or tables.", {"path": file_path})

        return ParsedDocument(
            source_type=SourceType.PPTX,
            extraction_method=ExtractionMethod.DIRECT_TEXT,
            total_slides=total_slides,
            elements=elements,
            metadata={
                "total_slides": total_slides,
                **meta
            }
        )

