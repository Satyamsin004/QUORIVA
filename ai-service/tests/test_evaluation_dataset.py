"""
CS Scholar AI - Phase 12 Benchmark Dataset Loader & Schema Integrity Tests
Verifies dataset splits, schema validation, 13 CS discipline coverage, and chunk linkage.
"""
import pytest
from app.evaluation.datasets.schemas import (
    CorpusChunk,
    EvaluationCase,
    QuestionCategory,
    DifficultyLevel,
    ExpectedAnswerability,
    DatasetSplit
)
from app.evaluation.datasets.loader import BenchmarkDataLoader
from app.evaluation.datasets.curated_data import generate_curated_benchmark


class TestBenchmarkDataset:
    """Tests the integrity and distribution of the evaluation corpus and evaluation cases."""

    def test_curated_benchmark_generation(self):
        corpus, cases = generate_curated_benchmark()

        assert len(corpus) >= 30, "Corpus should have at least 30 authoritative curriculum chunks"
        assert len(cases) == 80, f"Benchmark should contain exactly 80 canonical evaluation cases, found {len(cases)}"

        # Verify all 13 CS disciplines are covered in the corpus
        corpus_subjects = {c.subject for c in corpus}
        assert len(corpus_subjects) == 13, f"Expected 13 CS disciplines, found: {len(corpus_subjects)}"

        # Verify all 13 CS disciplines are covered in evaluation cases
        case_subjects = {c.subject for c in cases}
        assert len(case_subjects) == 13, f"Expected 13 CS disciplines in cases, found: {len(case_subjects)}"

        # Verify overall canonical balance: 60 answerable + 20 unanswerable = 80 total
        ans_cases = [c for c in cases if c.answerability in (ExpectedAnswerability.ANSWERABLE, ExpectedAnswerability.PARTIALLY_ANSWERABLE)]
        unans_cases = [c for c in cases if c.answerability == ExpectedAnswerability.UNANSWERABLE]
        assert len(ans_cases) == 60, f"Expected 60 answerable/partial cases, found {len(ans_cases)}"
        assert len(unans_cases) == 20, f"Expected 20 unanswerable cases, found {len(unans_cases)}"
        assert len(ans_cases) + len(unans_cases) == 80

    def test_split_partitions(self):
        _, cases = generate_curated_benchmark()

        dev_cases = [c for c in cases if c.split == DatasetSplit.DEV]
        val_cases = [c for c in cases if c.split == DatasetSplit.VAL]
        test_cases = [c for c in cases if c.split == DatasetSplit.TEST]

        assert len(dev_cases) == 26, f"DEV split should contain 26 cases, found {len(dev_cases)}"
        assert len(val_cases) == 20, f"VAL split should contain 20 cases, found {len(val_cases)}"
        assert len(test_cases) == 34, f"TEST split should contain 34 cases (>= 30), found {len(test_cases)}"
        assert len(dev_cases) + len(val_cases) + len(test_cases) == 80

        # Verify held-out TEST split balance
        test_ans = [c for c in test_cases if c.answerability in (ExpectedAnswerability.ANSWERABLE, ExpectedAnswerability.PARTIALLY_ANSWERABLE)]
        test_unans = [c for c in test_cases if c.answerability == ExpectedAnswerability.UNANSWERABLE]
        assert len(test_ans) == 20, f"TEST split should contain 20 answerable cases, found {len(test_ans)}"
        assert len(test_unans) == 14, f"TEST split should contain 14 unanswerable cases, found {len(test_unans)}"

        # Ensure no duplicate case IDs across splits
        all_ids = [c.case_id for c in cases]
        assert len(all_ids) == len(set(all_ids)), "All case IDs must be unique across all splits"

    def test_unanswerable_and_insufficient_evidence_distribution(self):
        _, cases = generate_curated_benchmark()

        unans_cases = [c for c in cases if c.answerability == ExpectedAnswerability.UNANSWERABLE]
        assert len(unans_cases) >= 10, "Should include at least 10 unanswerable/insufficient evidence stress cases"
        # Held-out TEST split hosts dedicated unanswerable stress cases
        test_unans = [c for c in unans_cases if c.split == DatasetSplit.TEST]
        assert len(test_unans) >= 10, "Held-out TEST split must contain unanswerable evaluation cases"

    def test_chunk_referential_integrity(self):
        corpus, cases = generate_curated_benchmark()
        corpus_chunk_ids = {c.chunk_id for c in corpus}

        # Verify relevant_chunk_ids in answerable cases point to valid corpus chunks
        for case in cases:
            if case.answerability == ExpectedAnswerability.ANSWERABLE:
                assert len(case.relevant_chunk_ids) > 0, f"Answerable case {case.case_id} must have relevant chunks"
                for cid in case.relevant_chunk_ids:
                    assert cid in corpus_chunk_ids, f"Case {case.case_id} references missing chunk {cid}"

    def test_data_loader_filtering(self):
        loader = BenchmarkDataLoader()
        dev_cases = loader.load_split(DatasetSplit.DEV)
        val_cases = loader.load_split(DatasetSplit.VAL)
        test_cases = loader.load_split(DatasetSplit.TEST)
        all_cases = loader.load_all_cases()

        assert len(dev_cases) > 0
        assert len(val_cases) > 0
        assert len(test_cases) > 0
        assert len(all_cases) == len(dev_cases) + len(val_cases) + len(test_cases)

        # Subject filtering
        algo_cases = loader.filter_cases(all_cases, subject="Data Structures and Algorithms")
        assert len(algo_cases) > 0
        for c in algo_cases:
            assert c.subject == "Data Structures and Algorithms"

        # Difficulty filtering
        adv_cases = loader.filter_cases(all_cases, difficulty=DifficultyLevel.ADVANCED)
        assert len(adv_cases) > 0
        for c in adv_cases:
            assert c.difficulty == DifficultyLevel.ADVANCED
