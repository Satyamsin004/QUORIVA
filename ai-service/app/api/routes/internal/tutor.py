"""
CS Scholar AI - Internal Academic Tutor API Route
Protected endpoint invoked by Spring Boot to perform grounded RAG generation with citations.
"""
import logging
from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session

from app.database.connection import get_db
from app.api.routes.internal.security import verify_internal_service_key
from app.rag.schemas import InternalTutorChatRequest, TutorAnswerResponse
from app.rag.service import RAGService
from app.rag.exceptions import RAGError
from app.providers.exceptions import (
    LLMError,
    ProviderUnavailableError,
    RateLimitError,
    InvalidAPIKeyError,
    LLMTimeoutError
)

logger = logging.getLogger(__name__)

router = APIRouter(
    prefix="/internal/v1/tutor",
    tags=["internal-tutor"],
    dependencies=[Depends(verify_internal_service_key)]
)

_rag_service = RAGService()


@router.post(
    "/answer",
    response_model=TutorAnswerResponse,
    status_code=status.HTTP_200_OK,
    summary="Generate source-grounded academic tutor answer"
)
def generate_tutor_answer(
    request: InternalTutorChatRequest,
    db: Session = Depends(get_db)
) -> TutorAnswerResponse:
    """
    Execute end-to-end RAG pipeline:
    Hybrid retrieval -> Context assembly -> Prompt orchestration -> LLM generation -> Citation validation.
    """
    try:
        return _rag_service.answer(db, request)
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
    except (RAGError, LLMError) as e:
        logger.error("RAG pipeline error: %s", e, exc_info=True)
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Tutor engine error: {str(e)}"
        )
    except Exception as e:
        logger.error("Unexpected error during tutor answer generation: %s", e, exc_info=True)
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="An unexpected error occurred while generating the tutor response."
        )

