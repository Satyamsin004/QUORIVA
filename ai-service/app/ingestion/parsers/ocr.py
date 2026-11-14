"""
CS Scholar AI - Ingestion OCR Engine
High-performance optical character recognition using RapidOCR (ONNX) with PyTesseract fallback.
"""
import logging
from typing import Optional
import numpy as np
from PIL import Image

from app.core.config import settings

logger = logging.getLogger(__name__)

# Try RapidOCR
try:
    from rapidocr_onnxruntime import RapidOCR
    RAPID_OCR_AVAILABLE = True
except ImportError:
    RapidOCR = None
    RAPID_OCR_AVAILABLE = False

# Try PyTesseract
try:
    import pytesseract
    PYTESSERACT_AVAILABLE = True
except ImportError:
    pytesseract = None
    PYTESSERACT_AVAILABLE = False


class OCREngine:
    """Wrapper around RapidOCR and pytesseract with automatic engine selection."""

    _rapid_ocr_instance: Optional[object] = None
    _is_tesseract_installed: Optional[bool] = None

    @classmethod
    def is_available(cls) -> bool:
        """Check if OCR is enabled in config and at least one OCR engine is ready."""
        if not settings.ocr_enabled:
            return False

        if RAPID_OCR_AVAILABLE:
            return True

        if PYTESSERACT_AVAILABLE and pytesseract is not None:
            if cls._is_tesseract_installed is not None:
                return cls._is_tesseract_installed
            try:
                if settings.tesseract_cmd:
                    pytesseract.pytesseract.tesseract_cmd = settings.tesseract_cmd
                pytesseract.get_tesseract_version()
                cls._is_tesseract_installed = True
                return True
            except Exception:
                cls._is_tesseract_installed = False

        return False

    @classmethod
    def _get_rapid_ocr(cls):
        if cls._rapid_ocr_instance is None and RAPID_OCR_AVAILABLE and RapidOCR is not None:
            cls._rapid_ocr_instance = RapidOCR()
        return cls._rapid_ocr_instance

    @classmethod
    def extract_text_from_image(cls, image: Image.Image, lang: str = "eng") -> str:
        """
        Extract text from a PIL Image using RapidOCR with PyTesseract fallback.
        Returns extracted text, or empty string if OCR yields no text.
        """
        if not cls.is_available():
            return ""

        # 1. Try RapidOCR first (faster, no external dependencies, high accuracy)
        if RAPID_OCR_AVAILABLE:
            try:
                engine = cls._get_rapid_ocr()
                if engine is not None:
                    img_array = np.array(image.convert("RGB"))
                    results, _ = engine(img_array)
                    if results:
                        lines = [box_res[1] for box_res in results if len(box_res) >= 2 and box_res[1]]
                        text = "\n".join(lines).strip()
                        if text:
                            return text
            except Exception as e:
                logger.warning("RapidOCR extraction encountered error: %s", e)

        # 2. Fallback to pytesseract if available
        if PYTESSERACT_AVAILABLE and pytesseract is not None:
            try:
                text = pytesseract.image_to_string(image, lang=lang)
                return text.strip() if text else ""
            except Exception as e:
                logger.warning("PyTesseract extraction failed: %s", e)

        return ""
