from pydantic_settings import BaseSettings, SettingsConfigDict
from pydantic import Field
from typing import List


class Settings(BaseSettings):
    model_config = SettingsConfigDict(
        env_file=".env",
        env_file_encoding="utf-8",
        case_sensitive=False,
        extra="ignore"
    )

    project_name: str = "CS Scholar AI Engine"
    app_env: str = "development"
    api_v1_prefix: str = "/api/v1"
    fastapi_host: str = "0.0.0.0"
    fastapi_port: int = 8000
    fastapi_reload: bool = True
    log_level: str = "INFO"

    # Internal Authentication
    internal_service_key: str = Field(
        default="cs_scholar_internal_service_secret_key_change_in_production",
        description="Shared secret for internal Spring Boot <-> AI Service communication"
    )

    # CORS
    cors_origins: List[str] = [
        "http://localhost:5173",
        "http://localhost:3000",
        "http://localhost:8080",
    ]

    # PostgreSQL Database
    postgres_host: str = "localhost"
    postgres_port: int = 5432
    postgres_db: str = "cs_scholar_db"
    postgres_user: str = "postgres"
    postgres_password: str = "postgres_dev_password"

    @property
    def postgres_url(self) -> str:
        return f"postgresql://{self.postgres_user}:{self.postgres_password}@{self.postgres_host}:{self.postgres_port}/{self.postgres_db}"

    # Redis & Celery
    redis_host: str = "localhost"
    redis_port: int = 6379
    redis_password: str = ""
    celery_broker_url: str = "redis://localhost:6379/0"
    celery_result_backend: str = "redis://localhost:6379/1"

    # S3 / MinIO Object Storage
    s3_endpoint: str = "http://localhost:9000"
    s3_region: str = "us-east-1"
    s3_access_key: str = "minioadmin"
    s3_secret_key: str = "minioadmin"
    s3_bucket: str = "cs-scholar-documents"
    s3_path_style_access: bool = True

    # Ingestion & Chunking
    target_chunk_size: int = 400
    chunk_overlap: int = 60
    min_chunk_size: int = 50
    max_chunk_size: int = 480
    ocr_enabled: bool = True
    max_ocr_pages: int = 250
    max_pdf_pages: int = 2500
    tesseract_cmd: str = r"C:\Program Files\Tesseract-OCR\tesseract.exe"

    # Embedding Model & pgvector (Phase 4)
    embedding_model_name: str = "BAAI/bge-base-en-v1.5"
    embedding_model_version: str = "bge-base-en-v1.5"
    embedding_dimension: int = 768
    embedding_batch_size: int = 32
    embedding_device: str = "auto"
    embedding_max_sequence_length: int = 512

    # Retrieval & Reranker Configuration (Phase 5)
    reranker_model_name: str = "cross-encoder/ms-marco-MiniLM-L-6-v2"
    reranker_device: str = "auto"
    reranker_batch_size: int = 16
    reranker_top_k: int = 10
    dense_candidate_count: int = 40
    keyword_candidate_count: int = 40
    hybrid_candidate_count: int = 40
    rrf_k: int = 60
    query_instruction: str = "Represent this sentence for searching relevant passages: "

    # AI Model Providers & Academic Tutor RAG (Phase 6)
    llm_provider: str = Field(default="groq", description="Default LLM provider ('groq' or 'gemini')")
    groq_api_key: str = Field(default="", description="Groq API Key")
    groq_model_name: str = Field(default="openai/gpt-oss-120b", description="Default Groq LLM model")
    gemini_api_key: str = Field(default="", description="Gemini API Key")
    gemini_model_name: str = Field(default="gemini-1.5-flash", description="Default Gemini LLM model")
    llm_temperature: float = Field(default=0.2, description="Sampling temperature for tutor answers")
    llm_max_output_tokens: int = Field(default=2048, description="Max generation tokens")
    rag_max_context_tokens: int = Field(default=4000, description="Max token budget for retrieved context")
    rag_max_retrieved_chunks: int = Field(default=8, description="Max chunks fed into prompt")
    rag_history_max_messages: int = Field(default=8, description="Max recent conversation messages")


settings = Settings()
