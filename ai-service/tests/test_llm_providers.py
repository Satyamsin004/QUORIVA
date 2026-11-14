"""
CS Scholar AI - Unit Tests for LLM Providers
Tests BaseLLMProvider JSON extraction, GroqProvider, GeminiProvider, and LLMProviderFactory.
"""
from unittest.mock import MagicMock, patch
import pytest

from app.providers.base import BaseLLMProvider
from app.providers.schemas import LLMMessage, LLMGenerationConfig
from app.providers.exceptions import (
    MalformedResponseError,
    ProviderUnavailableError,
    InvalidAPIKeyError,
    RateLimitError,
    LLMTimeoutError
)
from app.providers.groq_provider import GroqProvider
from app.providers.gemini_provider import GeminiProvider
from app.providers.factory import LLMProviderFactory


def test_base_llm_extract_json_clean():
    raw = '{"answer": "Paging is a memory management scheme.", "insufficient_evidence": false, "cited_sources": ["S1"]}'
    res = BaseLLMProvider.extract_json(raw)
    assert res["answer"] == "Paging is a memory management scheme."
    assert res["insufficient_evidence"] is False
    assert res["cited_sources"] == ["S1"]


def test_base_llm_extract_json_with_markdown_fence():
    raw = """```json
    {
      "answer": "Virtual memory separates user logical memory from physical memory [S1].",
      "insufficient_evidence": false,
      "cited_sources": ["S1"]
    }
    ```"""
    res = BaseLLMProvider.extract_json(raw)
    assert "Virtual memory" in res["answer"]
    assert res["cited_sources"] == ["S1"]


def test_base_llm_extract_json_embedded_braces():
    raw = "Here is the response:\n{\"answer\": \"Context switch saves CPU state.\", \"cited_sources\": []}\nThank you."
    res = BaseLLMProvider.extract_json(raw)
    assert res["answer"] == "Context switch saves CPU state."


def test_base_llm_extract_json_malformed():
    raw = "This is not json at all."
    with pytest.raises(MalformedResponseError):
        BaseLLMProvider.extract_json(raw)


def test_groq_provider_missing_key():
    provider = GroqProvider(api_key="")
    with pytest.raises(InvalidAPIKeyError):
        provider.generate([LLMMessage(role="user", content="Hello")])


def test_groq_provider_success():
    provider = GroqProvider(api_key="mock_key")
    mock_client = MagicMock()
    mock_completion = MagicMock()
    mock_choice = MagicMock()
    mock_choice.message.content = '{"answer": "Thread is a lightweight process [S1].", "insufficient_evidence": false, "cited_sources": ["S1"]}'
    mock_completion.choices = [mock_choice]
    mock_completion.usage.prompt_tokens = 50
    mock_completion.usage.completion_tokens = 25
    mock_client.chat.completions.create.return_value = mock_completion

    with patch.object(provider, "_get_client", return_value=mock_client):
        resp = provider.generate([LLMMessage(role="user", content="What is a thread?")])
        assert resp.provider == "groq"
        assert resp.input_tokens == 50
        assert resp.output_tokens == 25
        assert resp.parsed_json is not None
        assert resp.parsed_json["cited_sources"] == ["S1"]


def test_gemini_provider_missing_key():
    provider = GeminiProvider(api_key="")
    with pytest.raises(InvalidAPIKeyError):
        provider.generate([LLMMessage(role="user", content="Hello")])


def test_gemini_provider_success():
    provider = GeminiProvider(api_key="mock_gemini_key")
    mock_res = MagicMock()
    mock_res.status_code = 200
    mock_res.json.return_value = {
        "candidates": [
            {
                "content": {
                    "parts": [
                        {
                            "text": '{"answer": "Semaphore is a synchronization tool [S2].", "insufficient_evidence": false, "cited_sources": ["S2"]}'
                        }
                    ]
                }
            }
        ],
        "usageMetadata": {
            "promptTokenCount": 60,
            "candidatesTokenCount": 30
        }
    }

    with patch("httpx.Client.post", return_value=mock_res):
        resp = provider.generate([LLMMessage(role="user", content="What is a semaphore?")])
        assert resp.provider == "gemini"
        assert resp.input_tokens == 60
        assert resp.output_tokens == 30
        assert resp.parsed_json["cited_sources"] == ["S2"]


def test_llm_provider_factory():
    LLMProviderFactory.reset()
    groq_p = LLMProviderFactory.get_provider("groq")
    assert groq_p.provider_name == "groq"

    gemini_p = LLMProviderFactory.get_provider("gemini")
    assert gemini_p.provider_name == "gemini"

    with pytest.raises(ProviderUnavailableError):
        LLMProviderFactory.get_provider("unsupported_provider")
