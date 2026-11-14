"""
CS Scholar AI - Ingestion Tokenizer
High-performance token counting and encoding using tiktoken with graceful fallback.
"""
import logging
from abc import ABC, abstractmethod
from typing import List

logger = logging.getLogger(__name__)

try:
    import tiktoken
    TIKTOKEN_AVAILABLE = True
except ImportError:
    tiktoken = None
    TIKTOKEN_AVAILABLE = False


class BaseTokenizer(ABC):
    """Abstract tokenizer interface."""

    @abstractmethod
    def count_tokens(self, text: str) -> int:
        pass

    @abstractmethod
    def encode(self, text: str) -> List[int]:
        pass

    @abstractmethod
    def decode(self, tokens: List[int]) -> str:
        pass


class TiktokenTokenizer(BaseTokenizer):
    """Tiktoken-based tokenizer using cl100k_base encoding."""

    def __init__(self, encoding_name: str = "cl100k_base"):
        self.encoding_name = encoding_name
        self._encoder = None
        if TIKTOKEN_AVAILABLE:
            try:
                self._encoder = tiktoken.get_encoding(encoding_name)
            except Exception as e:
                logger.warning("Failed to load tiktoken encoding '%s': %s. Falling back to approximation.", encoding_name, e)

    def count_tokens(self, text: str) -> int:
        if not text:
            return 0
        if self._encoder is not None:
            try:
                return len(self._encoder.encode(text, disallowed_special=()))
            except Exception:
                pass
        # Fallback estimation: average 4 characters per token
        return max(1, len(text) // 4)

    def encode(self, text: str) -> List[int]:
        if not text:
            return []
        if self._encoder is not None:
            try:
                return self._encoder.encode(text, disallowed_special=())
            except Exception:
                pass
        # Fallback: pseudo-tokens by word boundaries
        words = text.split()
        return list(range(len(words)))

    def decode(self, tokens: List[int]) -> str:
        if not tokens:
            return ""
        if self._encoder is not None:
            try:
                return self._encoder.decode(tokens)
            except Exception:
                pass
        return ""


# Shared default tokenizer instance
default_tokenizer = TiktokenTokenizer()

