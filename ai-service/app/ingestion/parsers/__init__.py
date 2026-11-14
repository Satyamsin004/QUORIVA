"""
CS Scholar AI - Parsers Package
"""
from app.ingestion.parsers.pdf_parser import PDFParser
from app.ingestion.parsers.docx_parser import DOCXParser
from app.ingestion.parsers.pptx_parser import PPTXParser
from app.ingestion.parsers.ocr import OCREngine

__all__ = ["PDFParser", "DOCXParser", "PPTXParser", "OCREngine"]

