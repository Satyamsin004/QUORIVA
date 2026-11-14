"""
CS Scholar AI - Ingestion Parser Factory
"""
from typing import List
from app.ingestion.base import BaseParser
from app.ingestion.parsers.pdf_parser import PDFParser
from app.ingestion.parsers.docx_parser import DOCXParser
from app.ingestion.parsers.pptx_parser import PPTXParser
from app.ingestion.exceptions import UnsupportedFormatError


class ParserFactory:
    """Factory for selecting and instantiating document format parsers."""

    def __init__(self):
        self._parsers: List[BaseParser] = [
            PDFParser(),
            DOCXParser(),
            PPTXParser()
        ]

    def get_parser(self, file_type: str, filename_or_path: str = "") -> BaseParser:
        """
        Resolve and return the appropriate parser for the given file type or path.
        
        Args:
            file_type: Document file type string (e.g., 'PDF', 'DOCX', 'PPTX')
            filename_or_path: Optional filename or file path to check extension
            
        Returns:
            BaseParser implementation
            
        Raises:
            UnsupportedFormatError: If no parser matches the requested format.
        """
        for parser in self._parsers:
            if parser.can_parse(file_type, filename_or_path):
                return parser

        raise UnsupportedFormatError(
            f"Unsupported document format: '{file_type}' (path: '{filename_or_path}'). "
            f"Supported formats: PDF, DOCX, PPTX.",
            {"file_type": file_type, "filename": filename_or_path}
        )

