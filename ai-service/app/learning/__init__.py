"""
CS Scholar AI - Learning Studio Package
"""
from app.learning.schemas import (
    GenerateQuizInternalRequest,
    GenerateQuizInternalResponse,
    GenerateFlashcardsInternalRequest,
    GenerateFlashcardsInternalResponse,
    GeneratedQuestionDto,
    GeneratedOptionDto,
    GeneratedFlashcardDto,
)
from app.learning.service import LearningGenerationService

__all__ = [
    "GenerateQuizInternalRequest",
    "GenerateQuizInternalResponse",
    "GenerateFlashcardsInternalRequest",
    "GenerateFlashcardsInternalResponse",
    "GeneratedQuestionDto",
    "GeneratedOptionDto",
    "GeneratedFlashcardDto",
    "LearningGenerationService",
]

