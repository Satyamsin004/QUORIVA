"""
CS Scholar AI - Background Workers Package
"""
from app.workers.celery_app import celery_app
from app.workers.tasks import process_document_task
from app.workers.embedding_tasks import generate_document_embeddings

__all__ = ["celery_app", "process_document_task", "generate_document_embeddings"]
