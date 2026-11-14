"""
CS Scholar AI - LLM Provider Factory
Resolves configured provider instances with fallback management.
"""
import logging
from typing import Optional, Dict

from app.core.config import settings
from app.providers.base import BaseLLMProvider
from app.providers.groq_provider import GroqProvider
from app.providers.gemini_provider import GeminiProvider
from app.providers.exceptions import ProviderUnavailableError

logger = logging.getLogger(__name__)


class LLMProviderFactory:
    """Factory for resolving and instantiating LLM providers."""

    _instances: Dict[str, BaseLLMProvider] = {}

    @classmethod
    def get_provider(
        cls,
        provider_name: Optional[str] = None,
        model_name: Optional[str] = None
    ) -> BaseLLMProvider:
        name = (provider_name or settings.llm_provider).lower().strip()

        if name == "groq":
            if "groq" not in cls._instances:
                cls._instances["groq"] = GroqProvider()
            provider = cls._instances["groq"]
        elif name in ("gemini", "google"):
            if "gemini" not in cls._instances:
                cls._instances["gemini"] = GeminiProvider()
            provider = cls._instances["gemini"]
        else:
            raise ProviderUnavailableError(f"Unsupported LLM provider requested: '{name}'. Supported: 'groq', 'gemini'.")

        return provider

    @classmethod
    def reset(cls) -> None:
        """Clear cached instances (useful in tests)."""
        cls._instances.clear()

