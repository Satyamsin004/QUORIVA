"""
CS Scholar AI - Phase 12 Evaluation Dataset Loader
Provides loaders, filtering, and validation for benchmark corpus and evaluation cases.
"""
import json
import os
from typing import List, Dict, Optional, Set
from uuid import UUID
from app.evaluation.datasets.schemas import (
    CorpusChunk,
    EvaluationCase,
    DatasetSplit,
    QuestionCategory,
    DifficultyLevel
)

DATA_DIR = os.path.join(os.path.dirname(__file__), "data")


class BenchmarkDataLoader:
    """Loads and filters benchmark evaluation datasets and corpus chunks."""

    def __init__(self, data_dir: Optional[str] = None):
        self.data_dir = data_dir or DATA_DIR
        self._corpus: Optional[List[CorpusChunk]] = None
        self._corpus_map: Optional[Dict[UUID, CorpusChunk]] = None

    def load_corpus(self) -> List[CorpusChunk]:
        """Load all authoritative corpus chunks."""
        if self._corpus is None:
            corpus_path = os.path.join(self.data_dir, "corpus.json")
            if not os.path.exists(corpus_path):
                raise FileNotFoundError(f"Benchmark corpus file not found at: {corpus_path}")

            with open(corpus_path, "r", encoding="utf-8") as f:
                raw_data = json.load(f)
            self._corpus = [CorpusChunk(**item) for item in raw_data]
            self._corpus_map = {chunk.chunk_id: chunk for chunk in self._corpus}

        return self._corpus

    def get_chunk(self, chunk_id: UUID) -> Optional[CorpusChunk]:
        """Retrieve a corpus chunk by its UUID."""
        if self._corpus_map is None:
            self.load_corpus()
        return self._corpus_map.get(chunk_id)

    def load_split(self, split: DatasetSplit) -> List[EvaluationCase]:
        """Load a specific evaluation dataset split (DEV, VAL, or TEST)."""
        filename_map = {
            DatasetSplit.DEV: "dev_set.json",
            DatasetSplit.VAL: "val_set.json",
            DatasetSplit.TEST: "test_set.json"
        }
        filename = filename_map[split]
        filepath = os.path.join(self.data_dir, filename)

        if not os.path.exists(filepath):
            raise FileNotFoundError(f"Benchmark split file not found at: {filepath}")

        with open(filepath, "r", encoding="utf-8") as f:
            raw_data = json.load(f)

        return [EvaluationCase(**item) for item in raw_data]

    def load_all_cases(self) -> List[EvaluationCase]:
        """Load all evaluation cases across all splits."""
        all_cases = []
        for split in [DatasetSplit.DEV, DatasetSplit.VAL, DatasetSplit.TEST]:
            all_cases.extend(self.load_split(split))
        return all_cases

    def filter_cases(
        self,
        cases: List[EvaluationCase],
        subject: Optional[str] = None,
        category: Optional[QuestionCategory] = None,
        difficulty: Optional[DifficultyLevel] = None,
        is_multi_source: Optional[bool] = None
    ) -> List[EvaluationCase]:
        """Filter evaluation cases by criteria."""
        filtered = cases
        if subject:
            filtered = [c for c in filtered if c.subject.lower() == subject.lower()]
        if category:
            filtered = [c for c in filtered if c.category == category]
        if difficulty:
            filtered = [c for c in filtered if c.difficulty == difficulty]
        if is_multi_source is not None:
            filtered = [c for c in filtered if c.is_multi_source == is_multi_source]
        return filtered

