"""
CS Scholar AI - Ingestion DOCX Parser
Structure-aware Word document parser using python-docx.
Extracts headings, paragraphs, lists, and formatted tables with hierarchical section tracking.
"""
import logging
from typing import Dict, Any, Optional, List
import docx
from docx.table import Table
from docx.text.paragraph import Paragraph

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


class DOCXParser(BaseParser):
    """DOCX parser powered by python-docx."""

    def can_parse(self, file_type: str, filename_or_path: str) -> bool:
        norm_type = file_type.upper().strip() if file_type else ""
        norm_path = filename_or_path.lower().strip() if filename_or_path else ""
        return norm_type == "DOCX" or norm_path.endswith(".docx")

    def _format_table_as_markdown(self, table: Table) -> str:
        """Convert a docx Table into a clean markdown table string."""
        rows_data = []
        for row in table.rows:
            row_cells = [TextNormalizer.clean(cell.text).replace("\n", " ").strip() for cell in row.cells]
            rows_data.append(row_cells)

        if not rows_data:
            return ""

        # Find max columns
        max_cols = max(len(r) for r in rows_data)
        if max_cols == 0:
            return ""

        lines = []
        # Header row
        header = rows_data[0] + [""] * (max_cols - len(rows_data[0]))
        lines.append("| " + " | ".join(header) + " |")
        lines.append("| " + " | ".join(["---"] * max_cols) + " |")

        # Data rows
        for row in rows_data[1:]:
            padded_row = row + [""] * (max_cols - len(row))
            lines.append("| " + " | ".join(padded_row) + " |")

        return "\n".join(lines)

    def parse(self, file_path: str, metadata: Optional[Dict[str, Any]] = None) -> ParsedDocument:
        meta = metadata or {}
        try:
            doc = docx.Document(file_path)
        except Exception as e:
            raise ParserError(f"Failed to open DOCX document: {e}", {"path": file_path})

        elements: List[ContentElement] = []
        current_section = meta.get("title") or "Document"
        section_hierarchy: List[str] = [current_section]

        # Iterate through paragraphs and tables in document body
        # doc.element.body contains paragraph and table elements in order
        for child in doc.element.body:
            tag_name = child.tag.split("}")[-1] if "}" in child.tag else child.tag

            if tag_name == "p":
                # Paragraph
                p = Paragraph(child, doc)
                raw_text = p.text
                style_name = p.style.name.lower() if p.style and p.style.name else ""

                if not raw_text.strip():
                    continue

                if "heading 1" in style_name:
                    current_section = raw_text.strip()
                    section_hierarchy = [current_section]
                    elements.append(ContentElement(
                        element_type="heading",
                        content=f"# {current_section}",
                        section_title=current_section,
                        metadata={"heading_level": 1, "style": style_name}
                    ))
                elif "heading 2" in style_name:
                    sub_title = raw_text.strip()
                    current_section = f"{section_hierarchy[0]} > {sub_title}" if section_hierarchy else sub_title
                    elements.append(ContentElement(
                        element_type="heading",
                        content=f"## {sub_title}",
                        section_title=current_section,
                        metadata={"heading_level": 2, "style": style_name}
                    ))
                elif "heading 3" in style_name:
                    sub_title = raw_text.strip()
                    elements.append(ContentElement(
                        element_type="heading",
                        content=f"### {sub_title}",
                        section_title=current_section,
                        metadata={"heading_level": 3, "style": style_name}
                    ))
                elif "list" in style_name or style_name.startswith("bullet"):
                    cleaned = TextNormalizer.clean(raw_text)
                    if cleaned:
                        elements.append(ContentElement(
                            element_type="list_item",
                            content=f"- {cleaned}",
                            section_title=current_section,
                            metadata={"style": style_name}
                        ))
                else:
                    cleaned = TextNormalizer.clean(raw_text)
                    if cleaned:
                        elements.append(ContentElement(
                            element_type="paragraph",
                            content=cleaned,
                            section_title=current_section,
                            metadata={"style": style_name}
                        ))

            elif tag_name == "tbl":
                # Table
                table = Table(child, doc)
                tbl_md = self._format_table_as_markdown(table)
                if tbl_md.strip():
                    elements.append(ContentElement(
                        element_type="table",
                        content=tbl_md,
                        section_title=current_section,
                        metadata={"row_count": len(table.rows), "col_count": len(table.columns)}
                    ))

        if not elements:
            raise EmptyDocumentError("The DOCX document contains no extractable text or tables.", {"path": file_path})

        return ParsedDocument(
            source_type=SourceType.DOCX,
            extraction_method=ExtractionMethod.DIRECT_TEXT,
            elements=elements,
            metadata={
                "element_count": len(elements),
                **meta
            }
        )

