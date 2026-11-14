"""
CS Scholar AI - Ingestion Base Parser Interface
"""
from abc import ABC, abstractmethod
from typing import Dict, Any, Optional
from app.ingestion.models import ParsedDocument


class BaseParser(ABC):
    """Abstract base class for all document format parsers."""

    @abstractmethod
    def can_parse(self, file_type: str, filename_or_path: str) -> bool:
        """Return True if this parser can process the given file type or file path."""
        pass

    @abstractmethod
    def parse(self, file_path: str, metadata: Optional[Dict[str, Any]] = None) -> ParsedDocument:
        """
        Parse the target document file and return a structured ParsedDocument.
        
        Args:
            file_path: Absolute path to the local document file.
            metadata: Optional additional metadata associated with the document.
            
        Returns:
            ParsedDocument containing extracted structured elements and metadata.
            
        Raises:
            ParserError: If parsing fails.
            PageLimitExceededError: If the document exceeds page limits.
            EmptyDocumentError: If the document has no extractable content.
        """
        pass

