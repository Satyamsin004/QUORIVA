"""
CS Scholar AI - Internal API Routes
"""
from app.api.routes.internal.ingestion import router as ingestion_router
from app.api.routes.internal.embedding import router as embedding_router
from app.api.routes.internal.retrieval import router as retrieval_router
from app.api.routes.internal.tutor import router as tutor_router

__all__ = ["ingestion_router", "embedding_router", "retrieval_router", "tutor_router"]

