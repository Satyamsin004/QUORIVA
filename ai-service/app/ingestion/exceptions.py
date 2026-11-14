"""
CS Scholar AI - Ingestion Exceptions
"""


class IngestionError(Exception):
    """Base exception for all document ingestion engine errors."""
    def __init__(self, message: str, details: dict = None):
        super().__init__(message)
        self.message = message
        self.details = details or {}


class UnsupportedFormatError(IngestionError):
    """Raised when an unsupported file format or extension is encountered."""
    pass


class ParserError(IngestionError):
    """Raised when parsing document contents fails."""
    pass


class PageLimitExceededError(IngestionError):
    """Raised when a document exceeds configured maximum page/slide limits."""
    pass


class EmptyDocumentError(IngestionError):
    """Raised when a document contains no extractable content or pages."""
    pass


class StorageFetchError(IngestionError):
    """Raised when fetching a document from S3/MinIO storage fails."""
    pass


class OCRError(IngestionError):
    """Raised when OCR execution fails on an image or page."""
    pass

