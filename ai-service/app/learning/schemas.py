"""
CS Scholar AI - Learning Studio Pydantic Schemas & Data Transfer Objects
Defines request and response schemas for AI quiz and flashcard generation.
"""
from typing import List, Optional
from uuid import UUID
from pydantic import BaseModel, Field, ConfigDict


class GeneratedOptionDto(BaseModel):
    """Option for a multiple-choice or true/false question."""
    model_config = ConfigDict(populate_by_name=True, serialize_by_alias=True)

    option_index: int = Field(..., alias="optionIndex", ge=0)
    option_text: str = Field(..., alias="optionText", min_length=1)
    is_correct: bool = Field(..., alias="isCorrect")


class GeneratedQuestionDto(BaseModel):
    """Structured question generated from academic material."""
    model_config = ConfigDict(populate_by_name=True, serialize_by_alias=True)

    question_index: int = Field(..., alias="questionIndex", ge=0)
    question_text: str = Field(..., alias="questionText", min_length=5)
    question_type: str = Field(default="MULTIPLE_CHOICE", alias="questionType")
    difficulty_level: str = Field(default="MEDIUM", alias="difficultyLevel")
    explanation: str = Field(..., min_length=10)
    source_reference: Optional[str] = Field(default=None, alias="sourceReference")
    page_number: Optional[int] = Field(default=None, alias="pageNumber")
    slide_number: Optional[int] = Field(default=None, alias="slideNumber")
    topic_name: Optional[str] = Field(default=None, alias="topicName")
    options: List[GeneratedOptionDto] = Field(default_factory=list)


class GenerateQuizInternalRequest(BaseModel):
    """Internal request from Spring Boot to generate a quiz."""
    model_config = ConfigDict(populate_by_name=True)

    user_id: UUID = Field(..., alias="userId")
    source_type: str = Field(..., alias="sourceType")
    source_title: str = Field(..., alias="sourceTitle")
    context_text: str = Field(..., alias="contextText", min_length=10)
    difficulty_level: str = Field(default="MEDIUM", alias="difficultyLevel")
    question_count: int = Field(default=5, alias="questionCount", ge=1, le=20)
    question_types: List[str] = Field(default_factory=lambda: ["MULTIPLE_CHOICE"], alias="questionTypes")
    concept_focus: Optional[str] = Field(default=None, alias="conceptFocus")
    provider: Optional[str] = Field(default=None)
    model_name: Optional[str] = Field(default=None, alias="modelName")


class GenerateQuizInternalResponse(BaseModel):
    """Internal response returned to Spring Boot containing generated quiz questions."""
    model_config = ConfigDict(populate_by_name=True, serialize_by_alias=True)

    title: str
    difficulty_level: str = Field(..., alias="difficultyLevel")
    questions: List[GeneratedQuestionDto]
    provider: str
    model_name: str = Field(..., alias="modelName")
    input_tokens: int = Field(default=0, alias="inputTokens")
    output_tokens: int = Field(default=0, alias="outputTokens")
    latency_ms: int = Field(default=0, alias="latencyMs")


class GeneratedFlashcardDto(BaseModel):
    """Structured flashcard item."""
    model_config = ConfigDict(populate_by_name=True, serialize_by_alias=True)

    front: str = Field(..., min_length=3)
    back: str = Field(..., min_length=3)
    topic_name: Optional[str] = Field(default=None, alias="topicName")
    source_reference: Optional[str] = Field(default=None, alias="sourceReference")
    page_number: Optional[int] = Field(default=None, alias="pageNumber")
    slide_number: Optional[int] = Field(default=None, alias="slideNumber")


class GenerateFlashcardsInternalRequest(BaseModel):
    """Internal request from Spring Boot to generate flashcards."""
    model_config = ConfigDict(populate_by_name=True)

    user_id: UUID = Field(..., alias="userId")
    source_type: str = Field(..., alias="sourceType")
    source_title: str = Field(..., alias="sourceTitle")
    context_text: str = Field(..., alias="contextText", min_length=10)
    card_count: int = Field(default=5, alias="cardCount", ge=1, le=25)
    concept_focus: Optional[str] = Field(default=None, alias="conceptFocus")
    provider: Optional[str] = Field(default=None)
    model_name: Optional[str] = Field(default=None, alias="modelName")


class GenerateFlashcardsInternalResponse(BaseModel):
    """Internal response returned to Spring Boot containing generated flashcards."""
    model_config = ConfigDict(populate_by_name=True, serialize_by_alias=True)

    title: str
    cards: List[GeneratedFlashcardDto]
    provider: str
    model_name: str = Field(..., alias="modelName")
    input_tokens: int = Field(default=0, alias="inputTokens")
    output_tokens: int = Field(default=0, alias="outputTokens")
    latency_ms: int = Field(default=0, alias="latencyMs")

