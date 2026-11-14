"""
CS Scholar AI - Embedding Model Loader & Lifecycle Manager
Manages lazy singleton loading and device placement for BGE models.
"""
import logging
from typing import Optional
import torch
from sentence_transformers import SentenceTransformer

from app.embeddings.config import embedding_config
from app.embeddings.exceptions import ModelLoadError

logger = logging.getLogger(__name__)


class EmbeddingModel:
    """
    Singleton wrapper for the SentenceTransformer embedding model.
    Loads once per worker process and supports CPU and GPU execution.
    """

    _instance: Optional["EmbeddingModel"] = None
    _model: Optional[SentenceTransformer] = None

    def __init__(self, model_name: Optional[str] = None, device: Optional[str] = None):
        self.model_name = model_name or embedding_config.model_name
        self.device_config = device or embedding_config.device
        self.resolved_device = self._resolve_device(self.device_config)

    @classmethod
    def get_instance(cls, model_name: Optional[str] = None, device: Optional[str] = None) -> "EmbeddingModel":
        """Get or initialize singleton instance."""
        if cls._instance is None:
            cls._instance = cls(model_name, device)
        return cls._instance

    @staticmethod
    def _resolve_device(device_setting: str) -> str:
        """Resolve 'auto' to 'cuda' if available, otherwise 'cpu'."""
        if device_setting.lower() == "auto":
            is_cuda = torch.cuda.is_available()
            resolved = "cuda" if is_cuda else "cpu"
            logger.info("Auto device resolution: CUDA available=%s, selected device='%s'", is_cuda, resolved)
            return resolved
        return device_setting.lower()

    def get_model(self) -> SentenceTransformer:
        """
        Lazily load the SentenceTransformer embedding model on the resolved device.
        Reuses the model instance across calls within the same process.
        """
        if self._model is None:
            try:
                logger.info(
                    "Loading embedding model '%s' on device '%s'...",
                    self.model_name, self.resolved_device
                )
                self._model = SentenceTransformer(self.model_name, device=self.resolved_device)
                # Ensure max sequence length matches config
                self._model.max_seq_length = embedding_config.max_sequence_length
                logger.info("Embedding model '%s' successfully loaded.", self.model_name)
            except Exception as e:
                logger.error("Failed to load embedding model '%s': %s", self.model_name, e, exc_info=True)
                raise ModelLoadError(
                    f"Failed to load embedding model '{self.model_name}': {e}",
                    {"model_name": self.model_name, "device": self.resolved_device}
                )
        return self._model

