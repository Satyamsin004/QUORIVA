from fastapi import APIRouter
from datetime import datetime, timezone
from app.core.config import settings
from app.core.logging import logger

router = APIRouter()


@router.get("/health", summary="Health Check")
async def health_check():
    """
    Standard health check endpoint for monitoring, orchestrators, and clients.
    """
    logger.info("Health check endpoint accessed")
    return {
        "status": "healthy",
        "service": settings.project_name,
        "environment": settings.app_env,
        "timestamp": datetime.now(timezone.utc).isoformat(),
        "version": "0.1.0-phase0",
    }

