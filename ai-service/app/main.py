from contextlib import asynccontextmanager
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from app.core.config import settings
from app.core.logging import logger
from app.api.routes import api_router
from app.api.routes.health import router as health_router
from app.api.routes.internal.ingestion import router as ingestion_router
from app.api.routes.internal.embedding import router as embedding_router
from app.api.routes.internal.retrieval import router as retrieval_router
from app.api.routes.internal.tutor import router as tutor_router
from app.api.routes.internal.learning import router as learning_router


@asynccontextmanager
async def lifespan(app: FastAPI):
    logger.info("Initializing CS Scholar AI Service [%s]", settings.app_env)
    import threading
    def _warmup():
        try:
            from app.embeddings.model import EmbeddingModel
            EmbeddingModel.get_instance().get_model()
            logger.info("Embedding model pre-warmed successfully.")
            from app.retrieval.reranker import CrossEncoderReranker
            CrossEncoderReranker.get_instance().get_model()
            logger.info("Cross-Encoder reranker pre-warmed successfully.")
            logger.info("Background model warmup complete.")
        except Exception as e:
            logger.warning("Background model warmup error: %s", e)
    threading.Thread(target=_warmup, daemon=True).start()
    yield
    logger.info("Shutting down CS Scholar AI Service")


app = FastAPI(
    title=settings.project_name,
    version="0.6.0-phase6",
    description="CS Scholar AI - Academic Knowledge Discovery & Learning Engine",
    lifespan=lifespan,
    docs_url="/docs",
    redoc_url="/redoc",
)

# CORS Middleware
app.add_middleware(
    CORSMiddleware,
    allow_origins=settings.cors_origins,
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Root-level health endpoint (GET /health)
app.include_router(health_router)

# Versioned API routes (GET /api/v1/health, etc.)
app.include_router(api_router, prefix=settings.api_v1_prefix)

# Internal Service API routes
app.include_router(ingestion_router)
app.include_router(embedding_router)
app.include_router(retrieval_router)
app.include_router(tutor_router)
app.include_router(learning_router)
