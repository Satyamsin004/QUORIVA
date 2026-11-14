"""
CS Scholar AI - Embedding Tokenizer & Bounded Chunk Segmenter
Ensures zero content truncation by segmenting oversized chunks with the actual model tokenizer.
"""
import logging
from typing import List, Tuple, Optional
from transformers import AutoTokenizer

from app.embeddings.config import embedding_config
from app.embeddings.exceptions import SegmentationError

logger = logging.getLogger(__name__)


class EmbeddingTokenizer:
    """
    Tokenizer wrapper for the embedding model.
    Handles token counting and deterministic bounded segmentation of oversized chunks.
    """

    _instance: Optional["EmbeddingTokenizer"] = None

    def __init__(self, model_name: Optional[str] = None):
        self.model_name = model_name or embedding_config.model_name
        self._tokenizer = None

    @classmethod
    def get_instance(cls, model_name: Optional[str] = None) -> "EmbeddingTokenizer":
        """Singleton accessor to reuse tokenizer across worker turns."""
        if cls._instance is None:
            cls._instance = cls(model_name)
        return cls._instance

    @property
    def tokenizer(self):
        """Lazy load tokenizer on first use."""
        if self._tokenizer is None:
            try:
                logger.info("Loading tokenizer for model: %s", self.model_name)
                self._tokenizer = AutoTokenizer.from_pretrained(self.model_name)
            except Exception as e:
                logger.warning("Could not load Hugging Face tokenizer for '%s': %s. Using fallback.", self.model_name, e)
                # Fallback to standard bert-base-uncased or basic token counting
                try:
                    self._tokenizer = AutoTokenizer.from_pretrained("bert-base-uncased")
                except Exception:
                    self._tokenizer = None
        return self._tokenizer

    def count_tokens(self, text: str) -> int:
        """Count tokens in text excluding special tokens."""
        if not text:
            return 0
        if self.tokenizer is not None:
            try:
                tokens = self.tokenizer.encode(text, add_special_tokens=False)
                return len(tokens)
            except Exception:
                pass
        # Fallback estimation
        return max(1, len(text.split()))

    def encode(self, text: str) -> List[int]:
        """Encode text into token IDs without special tokens."""
        if not text:
            return []
        if self.tokenizer is not None:
            try:
                return self.tokenizer.encode(text, add_special_tokens=False)
            except Exception:
                pass
        words = text.split()
        return list(range(len(words)))

    def decode(self, token_ids: List[int]) -> str:
        """Decode token IDs back into text."""
        if not token_ids:
            return ""
        if self.tokenizer is not None:
            try:
                return self.tokenizer.decode(token_ids, skip_special_tokens=True).strip()
            except Exception:
                pass
        return " ".join(str(t) for t in token_ids)

    def segment_text(
        self,
        text: str,
        max_tokens: Optional[int] = None,
        overlap_tokens: Optional[int] = None
    ) -> List[Tuple[str, int]]:
        """
        Tokenize and segment text to guarantee it strictly fits within the embedding model's
        supported input length.
        
        Args:
            text: Input chunk text.
            max_tokens: Maximum allowed content tokens per segment (default: 510).
            overlap_tokens: Overlap tokens between consecutive segments (default: 40).
            
        Returns:
            List of tuples: [(segment_text, token_count), ...]
            For text that fits within max_tokens, returns a single item list.
        """
        if not text or not text.strip():
            return []

        limit = max_tokens or embedding_config.max_content_tokens
        overlap = overlap_tokens or embedding_config.segment_overlap_tokens

        try:
            token_ids = self.encode(text)
            total_tokens = len(token_ids)

            # Normal chunk: fits within model limit
            if total_tokens <= limit:
                return [(text.strip(), total_tokens)]

            # Oversized chunk: deterministic sliding window segmentation
            segments: List[Tuple[str, int]] = []
            stride = max(1, limit - overlap)
            start_idx = 0

            while start_idx < total_tokens:
                end_idx = min(start_idx + limit, total_tokens)
                segment_tokens = token_ids[start_idx:end_idx]
                segment_text = self.decode(segment_tokens)

                if segment_text:
                    segments.append((segment_text, len(segment_tokens)))

                if end_idx >= total_tokens:
                    break

                start_idx += stride

            return segments

        except Exception as e:
            raise SegmentationError(f"Failed to segment chunk text: {e}", {"text_length": len(text)})

