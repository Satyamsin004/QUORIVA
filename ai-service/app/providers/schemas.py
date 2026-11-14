"""
CS Scholar AI - LLM Provider Schemas
Data models for LLM requests, generation configurations, and uniform responses.
"""
from typing import Optional, Dict, Any
from pydantic import BaseModel, Field


class LLMMessage(BaseModel):
    role: str = Field(description="Role: 'system', 'user', or 'assistant'")
    content: str = Field(description="Text message body")


class LLMGenerationConfig(BaseModel):
    temperature: float = Field(default=0.2, ge=0.0, le=2.0)
    max_output_tokens: int = Field(default=2048, ge=1, le=8192)
    json_mode: bool = Field(default=True, description="Enforce JSON structured response")


class LLMResponse(BaseModel):
    content: str
    parsed_json: Optional[Dict[str, Any]] = None
    provider: str
    model_name: str
    input_tokens: Optional[int] = None
    output_tokens: Optional[int] = None
    latency_ms: int = 0

