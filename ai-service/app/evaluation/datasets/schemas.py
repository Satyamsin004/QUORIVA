"""
CS Scholar AI - Phase 12 Evaluation Dataset Schemas
Defines structured schemas for academic evaluation cases, corpus chunks, splits, and categories.
"""
from enum import Enum
from typing import List, Dict, Optional, Set
from uuid import UUID
from pydantic import BaseModel, Field


class QuestionCategory(str, Enum):
    DIRECT_FACTUAL = "DIRECT_FACTUAL"
    CONCEPTUAL = "CONCEPTUAL"
    MULTI_HOP = "MULTI_HOP"
    COMPARISON = "COMPARISON"
    PREREQUISITE_RELATIONSHIP = "PREREQUISITE_RELATIONSHIP"
    MULTI_CHUNK = "MULTI_CHUNK"
    INSUFFICIENT_EVIDENCE = "INSUFFICIENT_EVIDENCE"
    DISTRACTOR = "DISTRACTOR"
    CONFLICTING_EVIDENCE = "CONFLICTING_EVIDENCE"


class DifficultyLevel(str, Enum):
    BEGINNER = "BEGINNER"
    INTERMEDIATE = "INTERMEDIATE"
    ADVANCED = "ADVANCED"


class ExpectedAnswerability(str, Enum):
    ANSWERABLE = "ANSWERABLE"
    PARTIALLY_ANSWERABLE = "PARTIALLY_ANSWERABLE"
    UNANSWERABLE = "UNANSWERABLE"


class DatasetSplit(str, Enum):
    DEV = "DEV"
    VAL = "VAL"
    TEST = "TEST"


class CorpusChunk(BaseModel):
    chunk_id: UUID
    document_id: UUID
    subject: str
    topic: str
    title: str
    content: str
    token_count: int
    page_number: Optional[int] = None
    slide_number: Optional[int] = None
    section_heading: Optional[str] = None


class EvaluationCase(BaseModel):
    case_id: str
    subject: str
    topic: str
    question: str
    expected_answer: str
    key_facts: List[str] = Field(
        default_factory=list,
        description="Atomic factual claims required in a complete, faithful answer"
    )
    relevant_doc_id: Optional[UUID] = None
    relevant_chunk_ids: List[UUID] = Field(
        default_factory=list,
        description="Corpus chunk IDs containing authoritative evidence"
    )
    graded_relevance: Dict[str, int] = Field(
        default_factory=dict,
        description="Chunk UUID string to graded relevance score (3=definitive, 2=strong support, 1=background, 0=irrelevant)"
    )
    expected_evidence_excerpts: List[str] = Field(
        default_factory=list,
        description="Key text fragments expected to be cited"
    )
    difficulty: DifficultyLevel = DifficultyLevel.INTERMEDIATE
    category: QuestionCategory = QuestionCategory.DIRECT_FACTUAL
    answerability: ExpectedAnswerability = ExpectedAnswerability.ANSWERABLE
    split: DatasetSplit = DatasetSplit.DEV
    is_multi_source: bool = False
    abstention_rationale: Optional[str] = Field(
        default=None,
        description="Defensible explanation of why evidence is absent or insufficient in the corpus for unanswerable cases"
    )

