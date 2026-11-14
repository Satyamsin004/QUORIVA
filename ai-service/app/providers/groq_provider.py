"""
CS Scholar AI - Groq LLM Provider Integration
Executes low-latency inference using Groq's high-speed LPU infrastructure.
"""
import logging
import time
from typing import List, Optional

try:
    from groq import Groq, APIConnectionError, RateLimitError as GroqRateLimitError, AuthenticationError, APITimeoutError
except ImportError:
    Groq = None

from app.core.config import settings
from app.providers.base import BaseLLMProvider
from app.providers.schemas import LLMMessage, LLMGenerationConfig, LLMResponse
from app.providers.exceptions import (
    ProviderUnavailableError,
    RateLimitError,
    InvalidAPIKeyError,
    LLMTimeoutError,
    MalformedResponseError
)

logger = logging.getLogger(__name__)


class GroqProvider(BaseLLMProvider):
    """Groq API provider implementation supporting structured JSON output."""

    def __init__(self, api_key: Optional[str] = None, default_model: Optional[str] = None):
        self.api_key = api_key if api_key is not None else settings.groq_api_key
        self._default_model = default_model or settings.groq_model_name
        self._client: Optional[Groq] = None

    @property
    def provider_name(self) -> str:
        return "groq"

    @property
    def default_model(self) -> str:
        return self._default_model

    def _get_client(self) -> Groq:
        if Groq is None:
            raise ProviderUnavailableError("The 'groq' Python package is not installed.")
        if not self.api_key or self.api_key.startswith("gsk_placeholder"):
            raise InvalidAPIKeyError("Groq API key is not configured. Set GROQ_API_KEY in environment.")
        if self._client is None:
            self._client = Groq(api_key=self.api_key)
        return self._client

    def generate(
        self,
        messages: List[LLMMessage],
        config: Optional[LLMGenerationConfig] = None,
        model_name: Optional[str] = None
    ) -> LLMResponse:
        client = self._get_client()
        cfg = config or LLMGenerationConfig()
        target_model = model_name or self.default_model

        groq_messages = [{"role": m.role, "content": m.content} for m in messages]

        kwargs = {
            "model": target_model,
            "messages": groq_messages,
            "temperature": cfg.temperature,
            "max_tokens": cfg.max_output_tokens,
        }
        if cfg.json_mode:
            kwargs["response_format"] = {"type": "json_object"}

        start_time = time.perf_counter()
        try:
            logger.info("Calling Groq API model=%s (messages=%d, json_mode=%s)", target_model, len(messages), cfg.json_mode)
            chat_completion = client.chat.completions.create(**kwargs)
            latency_ms = int((time.perf_counter() - start_time) * 1000)

            choice = chat_completion.choices[0]
            raw_content = choice.message.content or ""

            input_tokens = None
            output_tokens = None
            if hasattr(chat_completion, "usage") and chat_completion.usage:
                input_tokens = chat_completion.usage.prompt_tokens
                output_tokens = chat_completion.usage.completion_tokens

            parsed_json = None
            if cfg.json_mode:
                parsed_json = self.extract_json(raw_content)

            return LLMResponse(
                content=raw_content,
                parsed_json=parsed_json,
                provider=self.provider_name,
                model_name=target_model,
                input_tokens=input_tokens,
                output_tokens=output_tokens,
                latency_ms=latency_ms
            )

        except AuthenticationError as e:
            logger.error("Groq authentication failure: %s", e)
            raise InvalidAPIKeyError(f"Groq API authentication failed: {e}") from e
        except GroqRateLimitError as e:
            logger.warning("Groq rate limit encountered: %s", e)
            raise RateLimitError(f"Groq rate limit exceeded: {e}") from e
        except APITimeoutError as e:
            logger.error("Groq request timed out: %s", e)
            raise LLMTimeoutError(f"Groq request timed out: {e}") from e
        except APIConnectionError as e:
            logger.error("Groq API connection failure: %s", e)
            raise ProviderUnavailableError(f"Groq connection error: {e}") from e
        except MalformedResponseError:
            raise
        except Exception as e:
            err_str = str(e)
            if ("model_not_found" in err_str or "does not exist" in err_str) and target_model != "openai/gpt-oss-120b":
                logger.warning("Groq model %s not found, falling back to openai/gpt-oss-120b: %s", target_model, e)
                return self.generate(messages, config, model_name="openai/gpt-oss-120b")
            elif "json_validate_failed" in err_str and cfg.json_mode:
                logger.warning("Groq JSON mode strict validation failed. Retrying without response_format constraint: %s", e)
                try:
                    kwargs.pop("response_format", None)
                    chat_completion = client.chat.completions.create(**kwargs)
                    raw_content = chat_completion.choices[0].message.content or ""
                    parsed_json = self.extract_json(raw_content)
                    return LLMResponse(
                        content=raw_content,
                        parsed_json=parsed_json,
                        provider=self.provider_name,
                        model_name=target_model,
                        latency_ms=int((time.perf_counter() - start_time) * 1000)
                    )
                except Exception as retry_err:
                    logger.error("Groq fallback generation error: %s", retry_err)
                    raise ProviderUnavailableError(f"Groq generation error: {retry_err}") from retry_err

            logger.error("Unexpected error in Groq provider: %s", e)
            raise ProviderUnavailableError(f"Groq generation error: {e}") from e
