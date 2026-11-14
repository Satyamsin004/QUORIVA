"""
CS Scholar AI - LLM Provider Exceptions
Standard exception hierarchy for AI providers (Groq, Gemini, etc.).
"""


class LLMError(Exception):
    """Base exception for all LLM provider errors."""
    pass


class ProviderUnavailableError(LLMError):
    """Raised when the requested provider service is unreachable or misconfigured."""
    pass


class RateLimitError(LLMError):
    """Raised when provider rate limits are exceeded (HTTP 429)."""
    pass


class InvalidAPIKeyError(LLMError):
    """Raised when authentication against the provider fails (HTTP 401/403)."""
    pass


class LLMTimeoutError(LLMError):
    """Raised when an LLM provider request times out."""
    pass


class MalformedResponseError(LLMError):
    """Raised when an LLM output cannot be parsed into the required structured format."""
    pass

