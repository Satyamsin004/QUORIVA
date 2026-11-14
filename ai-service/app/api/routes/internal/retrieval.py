"""
CS Scholar AI - Internal Hybrid Retrieval API Route
Protected endpoint invoked by Spring Boot to perform scoped hybrid academic search.
"""
import logging
from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session

from app.database.connection import get_db
from app.api.routes.internal.security import verify_internal_service_key
from app.retrieval.schemas import InternalSearchRequest, InternalSearchResponse
from app.retrieval.service import RetrievalService
from app.retrieval.exceptions import (
    QueryValidationError,
    RetrievalError,
    DenseRetrievalError,
    KeywordRetrievalError,
    RerankerError,
)

logger = logging.getLogger(__name__)

router = APIRouter(
    prefix="/internal/v1/retrieval",
    tags=["internal-retrieval"],
    dependencies=[Depends(verify_internal_service_key)]
)

# Singleton retrieval service
_retrieval_service = RetrievalService()


@router.post(
    "/search",
    response_model=InternalSearchResponse,
    status_code=status.HTTP_200_OK,
    summary="Execute hybrid search (Dense + PostgreSQL FTS + RRF + Cross-Encoder Reranking)"
)
def execute_hybrid_search(
    request: InternalSearchRequest,
    db: Session = Depends(get_db)
) -> InternalSearchResponse:
    """
    Execute scoped hybrid search across a user's authorized workspace documents.
    """
    try:
        return _retrieval_service.search(db, request)
    except QueryValidationError as e:
        logger.warning("Search query validation failed: %s", e.message)
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=e.message
        )
    except (DenseRetrievalError, KeywordRetrievalError, RerankerError, RetrievalError) as e:
        logger.error("Hybrid retrieval pipeline error: %s", e.message, exc_info=True)
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Retrieval engine failed: {e.message}"
        )
    except Exception as e:
        logger.error("Unexpected error during hybrid search: %s", e, exc_info=True)
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="An unexpected error occurred during retrieval."
        )

