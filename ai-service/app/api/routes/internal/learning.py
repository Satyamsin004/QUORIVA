"""
CS Scholar AI - Internal Learning Studio API Route
Protected endpoints invoked by Spring Boot to generate academic quizzes and flashcards.
"""
import logging
from fastapi import APIRouter, Depends, HTTPException, status

from app.api.routes.internal.security import verify_internal_service_key
from app.learning.schemas import (
    GenerateQuizInternalRequest,
    GenerateQuizInternalResponse,
    GenerateFlashcardsInternalRequest,
    GenerateFlashcardsInternalResponse,
)
from app.learning.service import LearningGenerationService
from app.providers.exceptions import (
    LLMError,
    ProviderUnavailableError,
    RateLimitError,
    InvalidAPIKeyError,
    LLMTimeoutError,
)

logger = logging.getLogger(__name__)

router = APIRouter(
    prefix="/internal/v1/learning",
    tags=["internal-learning"],
    dependencies=[Depends(verify_internal_service_key)]
)

_learning_service = LearningGenerationService()


@router.post(
    "/quizzes/generate",
    response_model=GenerateQuizInternalResponse,
    status_code=status.HTTP_200_OK,
    summary="Generate source-grounded academic quiz"
)
def generate_quiz(request: GenerateQuizInternalRequest) -> GenerateQuizInternalResponse:
    """Generate structured quiz questions with plausible distractors, explanations, and groundings."""
    try:
        return _learning_service.generate_quiz(request)
    except (InvalidAPIKeyError, ProviderUnavailableError) as e:
        logger.error("LLM Provider configuration or authentication failure: %s", e)
        raise HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            detail=str(e)
        )
    except RateLimitError as e:
        logger.warning("LLM Provider rate limit exceeded: %s", e)
        raise HTTPException(
            status_code=status.HTTP_429_TOO_MANY_REQUESTS,
            detail="LLM provider rate limit exceeded. Please try again shortly."
        )
    except LLMTimeoutError as e:
        logger.error("LLM Provider request timed out: %s", e)
        raise HTTPException(
            status_code=status.HTTP_504_GATEWAY_TIMEOUT,
            detail="LLM generation timed out."
        )
    except LLMError as e:
        logger.error("LLM Generation error during quiz generation: %s", e, exc_info=True)
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Quiz generator error: {str(e)}"
        )
    except Exception as e:
        logger.error("Unexpected error during quiz generation: %s", e, exc_info=True)
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="An unexpected error occurred while generating the quiz."
        )


@router.post(
    "/flashcards/generate",
    response_model=GenerateFlashcardsInternalResponse,
    status_code=status.HTTP_200_OK,
    summary="Generate source-grounded active-recall flashcards"
)
def generate_flashcards(request: GenerateFlashcardsInternalRequest) -> GenerateFlashcardsInternalResponse:
    """Generate concise, atomic flashcards with source provenance and technical accuracy."""
    try:
        return _learning_service.generate_flashcards(request)
    except (InvalidAPIKeyError, ProviderUnavailableError) as e:
        logger.error("LLM Provider configuration or authentication failure: %s", e)
        raise HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            detail=str(e)
        )
    except RateLimitError as e:
        logger.warning("LLM Provider rate limit exceeded: %s", e)
        raise HTTPException(
            status_code=status.HTTP_429_TOO_MANY_REQUESTS,
            detail="LLM provider rate limit exceeded. Please try again shortly."
        )
    except LLMTimeoutError as e:
        logger.error("LLM Provider request timed out: %s", e)
        raise HTTPException(
            status_code=status.HTTP_504_GATEWAY_TIMEOUT,
            detail="LLM generation timed out."
        )
    except LLMError as e:
        logger.error("LLM Generation error during flashcard generation: %s", e, exc_info=True)
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Flashcard generator error: {str(e)}"
        )
    except Exception as e:
        logger.error("Unexpected error during flashcard generation: %s", e, exc_info=True)
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="An unexpected error occurred while generating flashcards."
        )

