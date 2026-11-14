"""
CS Scholar AI - Google Gemini LLM Provider Integration
Uses HTTP REST API with structured JSON mode and system instruction support.
Eliminates binary C-extension dependencies (such as grpc) to ensure maximum cross-platform compatibility.
"""
import logging
import time
from typing import List, Optional
import httpx

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


class GeminiProvider(BaseLLMProvider):
    """Google Gemini provider implementation using REST API."""

    BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models"

    def __init__(self, api_key: Optional[str] = None, default_model: Optional[str] = None):
        self.api_key = api_key if api_key is not None else settings.gemini_api_key
        self._default_model = default_model or settings.gemini_model_name

    @property
    def provider_name(self) -> str:
        return "gemini"

    @property
    def default_model(self) -> str:
        return self._default_model

    def generate(
        self,
        messages: List[LLMMessage],
        config: Optional[LLMGenerationConfig] = None,
        model_name: Optional[str] = None
    ) -> LLMResponse:
        if not self.api_key or self.api_key.startswith("placeholder"):
            raise InvalidAPIKeyError("Gemini API key is not configured. Set GEMINI_API_KEY in environment.")

        cfg = config or LLMGenerationConfig()
        target_model = model_name or self.default_model

        # Build payload
        payload = {}

        # 1. System instruction
        system_msgs = [m.content for m in messages if m.role.lower() == "system"]
        if system_msgs:
            payload["system_instruction"] = {
                "parts": [{"text": "\n\n".join(system_msgs)}]
            }

        # 2. Contents (history + current question)
        contents = []
        for m in messages:
            if m.role.lower() == "system":
                continue
            role = "user" if m.role.lower() == "user" else "model"
            contents.append({
                "role": role,
                "parts": [{"text": m.content}]
            })
        payload["contents"] = contents

        # 3. Generation configuration
        gen_config = {
            "temperature": cfg.temperature,
            "maxOutputTokens": cfg.max_output_tokens,
        }
        if cfg.json_mode:
            gen_config["responseMimeType"] = "application/json"
        payload["generationConfig"] = gen_config

        url = f"{self.BASE_URL}/{target_model}:generateContent?key={self.api_key}"

        start_time = time.perf_counter()
        try:
            logger.info("Calling Gemini REST API model=%s (contents=%d, json_mode=%s)", target_model, len(contents), cfg.json_mode)
            with httpx.Client(timeout=60.0) as client:
                res = client.post(url, json=payload)

            latency_ms = int((time.perf_counter() - start_time) * 1000)

            if res.status_code in (401, 403):
                raise InvalidAPIKeyError(f"Gemini API authentication failed (status {res.status_code}): {res.text}")
            elif res.status_code == 429:
                raise RateLimitError(f"Gemini API rate limit exceeded: {res.text}")
            elif res.status_code != 200:
                raise ProviderUnavailableError(f"Gemini API error (status {res.status_code}): {res.text}")

            res_json = res.json()
            candidates = res_json.get("candidates", [])
            if not candidates:
                raise MalformedResponseError(f"Gemini returned empty candidate list: {res.text}")

            content_obj = candidates[0].get("content", {})
            parts = content_obj.get("parts", [])
            raw_content = parts[0].get("text", "") if parts else ""

            # Token usage metadata
            usage = res_json.get("usageMetadata", {})
            input_tokens = usage.get("promptTokenCount")
            output_tokens = usage.get("candidatesTokenCount")

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

        except httpx.TimeoutException as e:
            logger.error("Gemini request timed out: %s", e)
            raise LLMTimeoutError(f"Gemini request timed out: {e}") from e
        except (InvalidAPIKeyError, RateLimitError, ProviderUnavailableError, MalformedResponseError):
            raise
        except Exception as e:
            logger.error("Unexpected error in Gemini provider: %s", e)
            raise ProviderUnavailableError(f"Gemini generation error: {e}") from e
