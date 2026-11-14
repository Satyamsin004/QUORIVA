"""
CS Scholar AI - Search Query Processor
Validates user search input and formats queries with model-recommended instructions.
"""
import re
import logging
from typing import Tuple, Optional

from app.retrieval.config import retrieval_config
from app.retrieval.exceptions import QueryValidationError

logger = logging.getLogger(__name__)


class QueryProcessor:
    """
    Cleans, validates, and prepares search queries for dense and keyword retrieval.
    """

    def __init__(self, query_instruction: Optional[str] = None, max_length: int = 1000):
        self.query_instruction = (
            query_instruction if query_instruction is not None else retrieval_config.query_instruction
        )
        self.max_length = max_length

    def process(self, raw_query: str) -> Tuple[str, str]:
        """
        Validate and format the search query.

        Args:
            raw_query: Raw user query string.

        Returns:
            Tuple of (clean_query, instructed_query):
              - clean_query: trimmed, validated query for keyword search & cross-encoder.
              - instructed_query: query prepended with BGE search instruction for dense embedding.

        Raises:
            QueryValidationError: If query is empty, whitespace-only, or exceeds max length.
        """
        if not raw_query or not isinstance(raw_query, str):
            raise QueryValidationError("Search query cannot be empty or null.")

        # Normalize unicode whitespace
        cleaned = re.sub(r"\s+", " ", raw_query).strip()

        if not cleaned:
            raise QueryValidationError("Search query cannot be empty or contain only whitespace.")

        if len(cleaned) > self.max_length:
            raise QueryValidationError(
                f"Search query exceeds maximum length of {self.max_length} characters.",
                {"length": len(cleaned), "max_length": self.max_length}
            )

        instructed_query = f"{self.query_instruction}{cleaned}"
        return cleaned, instructed_query

