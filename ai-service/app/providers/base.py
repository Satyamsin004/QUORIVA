"""
CS Scholar AI - Abstract LLM Provider Base
Defines the uniform interface for all language model providers.
"""
from abc import ABC, abstractmethod
import json
import re
from typing import List, Optional, Dict, Any

from app.providers.schemas import LLMMessage, LLMGenerationConfig, LLMResponse
from app.providers.exceptions import MalformedResponseError


class BaseLLMProvider(ABC):
    """Abstract interface that all LLM provider integrations must implement."""

    @property
    @abstractmethod
    def provider_name(self) -> str:
        """Returns the canonical provider name (e.g. 'groq', 'gemini')."""
        pass

    @property
    @abstractmethod
    def default_model(self) -> str:
        """Returns default model name for this provider."""
        pass

    @abstractmethod
    def generate(
        self,
        messages: List[LLMMessage],
        config: Optional[LLMGenerationConfig] = None,
        model_name: Optional[str] = None
    ) -> LLMResponse:
        """
        Execute synchronous chat completion and return uniform LLMResponse.
        """
        pass

    @staticmethod
    def extract_json(raw_text: str) -> Dict[str, Any]:
        """
        Helper to parse JSON from an LLM string, handling optional markdown code blocks.
        """
        cleaned = raw_text.strip()
        # Remove markdown code block fences if present
        if cleaned.startswith("```"):
            cleaned = re.sub(r"^```(?:json)?\s*", "", cleaned, flags=re.IGNORECASE)
            cleaned = re.sub(r"\s*```$", "", cleaned)
            cleaned = cleaned.strip()

        # Try standard parse
        try:
            return json.loads(cleaned)
        except json.JSONDecodeError:
            pass

        # Try searching for outermost curly braces
        match = re.search(r"(\{.*\})", cleaned, re.DOTALL)
        if match:
            try:
                return json.loads(match.group(1))
            except json.JSONDecodeError:
                pass

        raise MalformedResponseError(f"Failed to parse valid JSON from LLM output: {raw_text[:200]}...")

