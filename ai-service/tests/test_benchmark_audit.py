"""
Phase 12.1.1 Evidence Audit & Regression Verification Test Suite
Tests:
1. Canonical dataset counts and split integrity (80 cases: 60 ans, 20 unans; DEV=26, VAL=20, TEST=34).
2. Question uniqueness and zero cross-split near-duplicates (Jaccard < 0.70).
3. Grounding confusion matrix math, explicit denominators, and raw prediction alignment.
4. Retrieval metric edge cases (multiple chunks, missing chunks, ties, empty lists, short lists).
5. Statistical significance calculations, paired t-test, and confidence intervals.
6. Human review agreement, unweighted Cohen's Kappa, and pending status.
7. Learning Studio sample sizes and multi-tier distinction.
8. Machine-readable JSON and Markdown report consistency.
"""
import json
import math
import os
import re
from typing import List, Set, Dict, Any
from uuid import UUID, uuid4

import pytest
from scipy import stats

from app.evaluation.datasets.loader import BenchmarkDataLoader
from app.evaluation.datasets.schemas import DatasetSplit, ExpectedAnswerability, EvaluationCase, CorpusChunk
from app.evaluation.metrics.retrieval import (
    compute_recall_at_k,
    compute_precision_at_k,
    compute_mrr,
    compute_ndcg_at_k
)
from app.evaluation.metrics.generation import (
    ConfusionMatrix,
    compute_abstention_metrics,
    AbstentionMetricsReport
)
from app.evaluation.judges.human_review import (
    compute_cohens_kappa,
    HumanReviewWorkflow,
    HumanJudgmentLabel,
    HumanReviewConcordanceReport
)
from app.evaluation.metrics.learning import (
    compute_jaccard_similarity,
    evaluate_quiz_quality,
    evaluate_flashcard_quality,
    QuizQualityReport,
    FlashcardQualityReport
)
from app.rag.schemas import TutorAnswerResponse, GroundingStatus
from app.learning.schemas import (
    GenerateQuizInternalResponse,
    GeneratedQuestionDto,
    GeneratedOptionDto,
    GenerateFlashcardsInternalResponse,
    GeneratedFlashcardDto
)


# ==============================================================================
# 1. Dataset Integrity & Split Count Audit
# ==============================================================================

def test_canonical_dataset_exact_counts():
    loader = BenchmarkDataLoader()
    corpus = loader.load_corpus()
    dev_cases = loader.load_split(DatasetSplit.DEV)
    val_cases = loader.load_split(DatasetSplit.VAL)
    test_cases = loader.load_split(DatasetSplit.TEST)
    all_cases = loader.load_all_cases()

    # Exact counts
    assert len(corpus) == 31, "Corpus must contain exactly 31 curriculum chunks."
    assert len(dev_cases) == 26, "DEV split must contain exactly 26 cases."
    assert len(val_cases) == 20, "VAL split must contain exactly 20 cases."
    assert len(test_cases) == 34, "TEST split must contain exactly 34 cases (>= 30)."
    assert len(all_cases) == 80, "Canonical dataset must contain exactly 80 cases."

    # Answerability breakdown
    dev_ans = sum(1 for c in dev_cases if c.relevant_chunk_ids)
    dev_unans = sum(1 for c in dev_cases if not c.relevant_chunk_ids)
    assert dev_ans == 26 and dev_unans == 0, "DEV must have 26 answerable and 0 unanswerable."

    val_ans = sum(1 for c in val_cases if c.relevant_chunk_ids)
    val_unans = sum(1 for c in val_cases if not c.relevant_chunk_ids)
    assert val_ans == 14 and val_unans == 6, "VAL must have 14 answerable and 6 unanswerable."

    test_ans = sum(1 for c in test_cases if c.relevant_chunk_ids)
    test_unans = sum(1 for c in test_cases if not c.relevant_chunk_ids)
    assert test_ans == 20 and test_unans == 14, "TEST must have 20 answerable and 14 unanswerable."

    total_ans = sum(1 for c in all_cases if c.relevant_chunk_ids)
    total_unans = sum(1 for c in all_cases if not c.relevant_chunk_ids)
    assert total_ans == 60 and total_unans == 20, "Must have exactly 60 answerable and 20 unanswerable."


def test_unique_stable_ids_and_no_duplicate_questions():
    loader = BenchmarkDataLoader()
    all_cases = loader.load_all_cases()
    ids = [c.case_id for c in all_cases]
    assert len(set(ids)) == len(ids), "Every case must have a unique stable ID."

    # Check for near-duplicate questions across splits
    def tokenize(text):
        return set(re.findall(r'\b[a-zA-Z0-9]+\b', text.lower()))

    for i in range(len(all_cases)):
        t1 = tokenize(all_cases[i].question)
        for j in range(i + 1, len(all_cases)):
            # Same split duplicates or cross-split duplicates
            t2 = tokenize(all_cases[j].question)
            inter = len(t1 & t2)
            union = len(t1 | t2)
            jacc = inter / union if union > 0 else 0
            assert jacc < 0.70, (
                f"Questions {all_cases[i].case_id} and {all_cases[j].case_id} are near-duplicates (Jaccard={jacc:.3f})"
            )


def test_document_overlap_bounded_and_documented():
    loader = BenchmarkDataLoader()
    dev_cases = loader.load_split(DatasetSplit.DEV)
    val_cases = loader.load_split(DatasetSplit.VAL)
    test_cases = loader.load_split(DatasetSplit.TEST)

    dev_chunks = set(cid for c in dev_cases for cid in c.relevant_chunk_ids)
    val_chunks = set(cid for c in val_cases for cid in c.relevant_chunk_ids)
    test_chunks = set(cid for c in test_cases for cid in c.relevant_chunk_ids)

    # 31 total chunks in corpus
    assert len(dev_chunks) == 26
    assert len(val_chunks) == 15
    assert len(test_chunks) == 20
    assert len(dev_chunks & test_chunks) == 17
    assert len(val_chunks & test_chunks) == 10
    assert len(dev_chunks & val_chunks & test_chunks) == 7


# ==============================================================================
# 2. Confusion Matrix & Denominators Audit
# ==============================================================================

def test_confusion_matrix_explicit_denominators():
    cm = ConfusionMatrix(
        fully_ans_grounded=18,
        fully_ans_partial=0,
        fully_ans_abstained=0,
        partial_ans_grounded=0,
        partial_ans_partial=2,
        partial_ans_abstained=0,
        unans_grounded=0,
        unans_partial=0,
        unans_abstained=14
    )
    assert cm.total_fully_answerable == 18
    assert cm.total_partially_answerable == 2
    assert cm.total_unanswerable == 14
    assert cm.total_cases == 34
    assert cm.total_predicted_grounded == 18
    assert cm.total_predicted_partial == 2
    assert cm.total_predicted_abstained == 14

    # Test false abstention rate denominator
    false_abstention_rate = cm.fully_ans_abstained / cm.total_fully_answerable
    assert false_abstention_rate == 0.0

    # Test unsupported answer rate denominator
    unsupported_rate = (cm.unans_grounded + cm.unans_partial) / cm.total_unanswerable
    assert unsupported_rate == 0.0

    # Test partial grounding accuracy denominator
    partial_acc = cm.partial_ans_partial / cm.total_partially_answerable
    assert partial_acc == 1.0

    # Test correct abstention rate denominator
    correct_abstention_rate = cm.unans_abstained / cm.total_unanswerable
    assert correct_abstention_rate == 1.0


def test_partially_answerable_case_isolation():
    loader = BenchmarkDataLoader()
    test_cases = loader.load_split(DatasetSplit.TEST)

    partial_cases = [c for c in test_cases if c.answerability == ExpectedAnswerability.PARTIALLY_ANSWERABLE]
    assert len(partial_cases) == 2, "TEST split must contain exactly 2 partially answerable cases."
    partial_ids = {c.case_id for c in partial_cases}
    assert partial_ids == {"eval-test-ans-19", "eval-test-ans-20"}


# ==============================================================================
# 3. Retrieval Metrics Mathematical Rigor & Edge Cases
# ==============================================================================

def test_retrieval_metric_edge_cases():
    doc1, doc2, doc3, doc4 = uuid4(), uuid4(), uuid4(), uuid4()

    # 1. Multiple relevant chunks
    relevant = {doc1, doc2}
    retrieved = [doc3, doc1, doc4, doc2]
    # Recall@3: doc1 is in top 3 -> hits=1 -> recall = 1/2 = 0.5
    assert compute_recall_at_k(retrieved, relevant, k=3) == 0.5
    # Recall@4: doc1 and doc2 in top 4 -> hits=2 -> recall = 2/2 = 1.0
    assert compute_recall_at_k(retrieved, relevant, k=4) == 1.0
    # Precision@3: 1 hit in 3 retrieved -> 1/3
    assert pytest.approx(compute_precision_at_k(retrieved, relevant, k=3)) == 1.0 / 3.0
    # MRR: first hit at rank 2 -> 1/2 = 0.5
    assert compute_mrr(retrieved, relevant) == 0.5

    # 2. No relevant documents in candidate list
    unrelated = [uuid4(), uuid4(), uuid4()]
    assert compute_recall_at_k(unrelated, relevant, k=5) == 0.0
    assert compute_precision_at_k(unrelated, relevant, k=5) == 0.0
    assert compute_mrr(unrelated, relevant) == 0.0
    assert compute_ndcg_at_k(unrelated, relevant, k=5) == 0.0

    # 3. Candidate lists shorter than K
    short_list = [doc1]
    assert compute_recall_at_k(short_list, relevant, k=10) == 0.5
    assert compute_precision_at_k(short_list, relevant, k=10) == 1.0  # 1 hit in 1 retrieved
    assert compute_mrr(short_list, relevant) == 1.0

    # 4. Empty candidate list or empty relevant set
    assert compute_recall_at_k([], relevant, k=10) == 0.0
    assert compute_precision_at_k([], relevant, k=10) == 0.0
    assert compute_mrr([], relevant) == 0.0
    assert compute_ndcg_at_k([], relevant, k=10) == 0.0
    assert compute_recall_at_k(retrieved, set(), k=10) == 0.0
    assert compute_precision_at_k(retrieved, set(), k=10) == 0.0
    assert compute_mrr(retrieved, set()) == 0.0
    assert compute_ndcg_at_k(retrieved, set(), k=10) == 0.0


def test_ndcg_at_k_discount_and_ideal_calculation():
    d1, d2 = uuid4(), uuid4()
    relevant = {d1, d2}

    # Case A: Ideal ranking [d1, d2]
    # DCG = (2^1 - 1)/log2(2) + (2^1 - 1)/log2(3) = 1/1 + 1/1.58496 = 1 + 0.63093 = 1.63093
    # IDCG = 1.63093 -> nDCG = 1.0
    assert pytest.approx(compute_ndcg_at_k([d1, d2], relevant, k=5)) == 1.0

    # Case B: First relevant item at rank 2 [irrelevant, d1]
    # DCG = 0 + 1/log2(3) = 0.63093
    # IDCG for {d1} = 1/log2(2) = 1.0 -> nDCG = 0.63093
    assert pytest.approx(compute_ndcg_at_k([uuid4(), d1], {d1}, k=5), abs=1e-3) == 0.6309


# ==============================================================================
# 4. Statistical Significance & Paired Tests Audit
# ==============================================================================

def test_paired_ttest_and_confidence_interval_math():
    # 20 paired queries with small differences
    # Simulate Dense vs Cross-Encoder scores on 20 queries
    dense_scores = [1.0] * 16 + [0.8503, 0.5649, 0.9324, 1.0000]
    rerank_scores = [1.0] * 16 + [0.9197, 0.6177, 0.9177, 0.6309]

    diffs = [r - d for r, d in zip(rerank_scores, dense_scores)]
    mean_diff = sum(diffs) / len(diffs)
    t_stat, p_val = stats.ttest_rel(rerank_scores, dense_scores)

    # 95% Confidence Interval using t-distribution
    n = len(diffs)
    std_err = stats.sem(diffs)
    ci = stats.t.interval(0.95, df=n - 1, loc=mean_diff, scale=std_err)

    assert pytest.approx(mean_diff, abs=1e-4) == -0.0131
    assert p_val > 0.05, f"Difference must NOT be statistically significant (observed p={p_val})"
    assert ci[0] < 0.0 < ci[1], "Confidence interval must cross zero"


# ==============================================================================
# 5. Human Review Agreement Audit
# ==============================================================================

def test_cohens_kappa_unweighted_calculation():
    # Perfect agreement
    r1 = ["CORRECT", "CORRECT", "INCORRECT", "CORRECT_ABSTENTION"]
    r2 = ["CORRECT", "CORRECT", "INCORRECT", "CORRECT_ABSTENTION"]
    assert compute_cohens_kappa(r1, r2) == 1.0

    # Disagreement lower than chance
    r1 = ["CORRECT", "INCORRECT", "CORRECT", "INCORRECT"]
    r2 = ["INCORRECT", "CORRECT", "INCORRECT", "CORRECT"]
    assert compute_cohens_kappa(r1, r2) < 0.0

    # Non-trivial standard case
    r1 = ["CORRECT", "CORRECT", "INCORRECT", "CORRECT", "CORRECT"]
    r2 = ["CORRECT", "INCORRECT", "INCORRECT", "CORRECT", "CORRECT"]
    k = compute_cohens_kappa(r1, r2)
    assert 0.0 < k < 1.0


def test_human_review_status_is_pending_external():
    r1 = {"eval-1": HumanJudgmentLabel.CORRECT}
    r2 = {"eval-1": HumanJudgmentLabel.CORRECT}
    report = HumanReviewWorkflow.compute_inter_annotator_agreement(
        r1, r2, is_simulated=False
    )
    assert report.status == "PENDING_EXTERNAL_REVIEW"


# ==============================================================================
# 6. Learning Studio Multi-Tier Validation & Sample Sizes
# ==============================================================================

def test_learning_studio_multi_tier_separation():
    quiz = GenerateQuizInternalResponse(
        title="Test CS Quiz",
        difficultyLevel="EASY",
        modelName="test",
        provider="test",
        questions=[
            GeneratedQuestionDto(
                questionIndex=0,
                questionText="What is 2 + 2 in binary?",
                questionType="MULTIPLE_CHOICE",
                difficultyLevel="EASY",
                explanation="2 + 2 = 4, which is 100 in binary.",
                sourceReference="Binary Arithmetic, Page 1",
                options=[
                    GeneratedOptionDto(optionIndex=0, optionText="10", isCorrect=False),
                    GeneratedOptionDto(optionIndex=1, optionText="100", isCorrect=True),
                    GeneratedOptionDto(optionIndex=2, optionText="110", isCorrect=False),
                    GeneratedOptionDto(optionIndex=3, optionText="111", isCorrect=False)
                ]
            )
        ]
    )

    q_report = evaluate_quiz_quality(quiz)
    assert q_report.total_questions == 1
    assert q_report.single_correct_answer_rate == 1.0
    assert q_report.tier_1_schema_pass_rate == 1.0
    assert "Pedagogical calibration" in q_report.limitations_note


# ==============================================================================
# 7. Machine-Readable JSON & Markdown Consistency
# ==============================================================================

def test_json_and_markdown_reports_exist_and_consistent():
    json_path = os.path.join("docs", "rag_evaluation_results.json")
    md_path = os.path.join("docs", "RAG_EVALUATION.md")
    worksheet_path = os.path.join("docs", "HUMAN_REVIEW_WORKSHEET.md")

    assert os.path.exists(json_path), "JSON report must exist."
    assert os.path.exists(md_path), "Markdown report must exist."
    assert os.path.exists(worksheet_path), "Worksheet must exist."

    with open(json_path, "r", encoding="utf-8") as f:
        data = json.load(f)

    with open(md_path, "r", encoding="utf-8") as f:
        md_text = f.read()

    # Check key consistency
    assert data["dataset_splits"]["total_cases"] == 80
    assert data["dataset_splits"]["test"]["total"] == 34
    assert data["dataset_splits"]["test"]["answerable"] == 20
    assert data["dataset_splits"]["test"]["unanswerable"] == 14

    assert "80 academic cases" in md_text
    assert "Held-Out Test (TEST)" in md_text
    assert "0.9674" in md_text  # Dense nDCG@10
    assert "0.9543" in md_text  # Rerank nDCG@10
    assert "Pending Independent Expert Validation" in md_text


# ==============================================================================
# 8. Phase 12.1.2 Final Benchmark Sign-off Clarification Tests
# ==============================================================================

def test_phase_12_1_2_statistical_methodology_validation():
    json_path = os.path.join("docs", "rag_evaluation_results.json")
    with open(json_path, "r", encoding="utf-8") as f:
        data = json.load(f)

    stat = data["metadata"]["statistical_methodology"]
    assert stat["evaluated_sample_size"] == 20
    assert stat["degrees_of_freedom"] == 19
    assert stat["bonferroni_family_comparisons"] == 4
    assert stat["bonferroni_adjusted_alpha"] == 0.0125

    # Check paired comparisons summary
    comps = stat["paired_comparisons_summary"]
    for comp_name, metrics in comps.items():
        assert not metrics["statistically_significant"], f"{comp_name} must not claim statistical significance"
        assert metrics["p_value"] > 0.0125, f"{comp_name} p-value must exceed Bonferroni threshold"
        # All confidence intervals bracket zero
        assert metrics["unadjusted_ci_95"][0] < 0.0 < metrics["unadjusted_ci_95"][1]
        assert metrics["bonferroni_ci_98_75"][0] < 0.0 < metrics["bonferroni_ci_98_75"][1]


def test_phase_12_1_2_retrieval_metric_coexistence_proof():
    loader = BenchmarkDataLoader()
    test_cases = loader.load_split(DatasetSplit.TEST)
    ans_cases = [c for c in test_cases if c.relevant_chunk_ids]

    assert len(ans_cases) == 20

    single_targets = [c for c in ans_cases if len(c.relevant_chunk_ids) == 1]
    multi_targets = [c for c in ans_cases if len(c.relevant_chunk_ids) > 1]

    assert len(single_targets) == 17
    assert len(multi_targets) == 3

    multi_ids = {c.case_id for c in multi_targets}
    assert multi_ids == {"eval-test-ans-13", "eval-test-ans-14", "eval-test-ans-15"}

    # Mathematical Proof Calculation
    # Under 100% first-rank hit rate:
    # Single-target recall@1 = 1 / 1 = 1.0
    # Multi-target recall@1 = 1 / 2 = 0.5 (since capacity at K=1 is 1)
    # MRR = 1 / 1 = 1.0 for all 20 queries
    macro_recall_1 = (len(single_targets) * 1.0 + len(multi_targets) * 0.5) / len(ans_cases)
    macro_mrr = sum(1.0 for _ in ans_cases) / len(ans_cases)

    assert round(macro_recall_1, 4) == 0.9250
    assert round(macro_mrr, 4) == 1.0000


def test_phase_12_1_2_reproducibility_metadata_and_hashes():
    import hashlib

    json_path = os.path.join("docs", "rag_evaluation_results.json")
    with open(json_path, "r", encoding="utf-8") as f:
        data = json.load(f)

    repro = data["metadata"]["reproducibility"]
    assert repro["deterministic_seed"] == 42
    assert "python_version" in repro
    assert "scipy_version" in repro
    assert "numpy_version" in repro

    hashes = repro["dataset_file_hashes_sha256"]
    dataset_dir = os.path.join("ai-service", "app", "evaluation", "datasets", "data")

    for filename, expected_hash in hashes.items():
        fpath = os.path.join(dataset_dir, filename)
        assert os.path.exists(fpath), f"{fpath} must exist"
        with open(fpath, "rb") as f:
            computed_hash = hashlib.sha256(f.read()).hexdigest().lower()
        assert computed_hash == expected_hash.lower(), f"Hash mismatch for {filename}: {computed_hash} != {expected_hash}"


def test_phase_12_1_2_reranker_architectural_recommendation():
    json_path = os.path.join("docs", "rag_evaluation_results.json")
    with open(json_path, "r", encoding="utf-8") as f:
        data = json.load(f)

    rerank_rec = data["metadata"]["reranker_architectural_recommendation"]
    assert "NOT_STATISTICALLY_SIGNIFICANT" in rerank_rec["significance_status"]
    assert "feature flag" in rerank_rec["production_guidance"].lower()


def test_phase_12_1_2_raw_per_query_results_presence():
    json_path = os.path.join("docs", "rag_evaluation_results.json")
    with open(json_path, "r", encoding="utf-8") as f:
        data = json.load(f)

    assert "raw_per_query_results" in data
    raw = data["raw_per_query_results"]
    assert len(raw) == 20, "Must contain all 20 answerable test queries"

    for r in raw:
        assert "case_id" in r
        assert "subject" in r
        assert "topic" in r
        assert "question" in r
        assert "dense_ndcg_at_10" in r
        assert "reranked_ndcg_at_10" in r
        assert "ndcg_delta" in r


# ==============================================================================
# Phase 12.1.3 & Phase 12.1.4: Historical Benchmark Reconciliation & Final Closure
# ==============================================================================

def test_phase_12_1_3_activation_record_case_reconciliation():
    json_path = os.path.join("docs", "rag_evaluation_results.json")
    with open(json_path, "r", encoding="utf-8") as f:
        data = json.load(f)

    # 1. Verify raw per-query array contains eval-test-ans-01 with true delta -0.3691
    raw = data["raw_per_query_results"]
    ans_01 = next((r for r in raw if r["case_id"] == "eval-test-ans-01"), None)
    assert ans_01 is not None, "eval-test-ans-01 must be present in raw results"
    assert round(ans_01["dense_ndcg_at_10"], 4) == 1.0000
    assert round(ans_01["reranked_ndcg_at_10"], 4) == 0.6309
    assert round(ans_01["ndcg_delta"], 4) == -0.3691

    # 2. Verify sample-wide sums across all 20 queries:
    # Graded relevance sum across 20 queries = -0.2616 (exact: -0.26162697)
    total_delta_graded = sum(r["ndcg_delta"] for r in raw)
    assert pytest.approx(total_delta_graded, abs=1e-3) == -0.2616

    # Binary relevance sum across 20 queries = -0.2854 (exact: -0.28539662)
    total_delta_binary = sum(r["binary_relevance"]["ndcg_delta"] for r in raw)
    assert pytest.approx(total_delta_binary, abs=1e-3) == -0.2854

    # 3. Verify reconciliation metadata block
    recon = data["metadata"]["historical_reconciliation"]["activation_record_case_reconciliation"]
    assert recon["authoritative_per_query_delta"] == -0.3691
    assert recon["phase_12_1_2_prose_cited_delta"] == -0.2625
    assert "transcription slip" in recon["reconciliation_explanation"].lower()


def test_phase_12_1_4_four_non_zero_reranker_differences_reconciliation():
    json_path = os.path.join("docs", "rag_evaluation_results.json")
    with open(json_path, "r", encoding="utf-8") as f:
        data = json.load(f)

    raw = data["raw_per_query_results"]
    assert len(raw) == 20, "Must contain exactly 20 answerable held-out test queries"

    # Identify non-zero differences
    non_zero_graded = [r for r in raw if abs(r["ndcg_delta"]) > 1e-6]
    non_zero_binary = [r for r in raw if abs(r["binary_relevance"]["ndcg_delta"]) > 1e-6]

    assert len(non_zero_graded) == 4, "Exactly 4 queries have non-zero graded reranker differences"
    assert len(non_zero_binary) == 4, "Exactly 4 queries have non-zero binary reranker differences"

    # Verify the remaining 16 queries are strictly zero
    zero_queries = [r for r in raw if abs(r["ndcg_delta"]) <= 1e-6]
    assert len(zero_queries) == 16, "Remaining 16 queries must have delta exactly 0.0000"
    for r in zero_queries:
        assert r["ndcg_delta"] == 0.0
        assert r["binary_relevance"]["ndcg_delta"] == 0.0

    # Verify the four displayed graded deltas
    g_map = {r["case_id"]: r["ndcg_delta"] for r in non_zero_graded}
    assert pytest.approx(g_map["eval-test-ans-01"], abs=1e-4) == -0.3691
    assert pytest.approx(g_map["eval-test-ans-13"], abs=1e-4) == 0.0694
    assert pytest.approx(g_map["eval-test-ans-14"], abs=1e-4) == 0.0529
    assert pytest.approx(g_map["eval-test-ans-15"], abs=1e-4) == -0.0148

    # Sum of the four displayed graded deltas equals -0.2616
    assert pytest.approx(sum(g_map.values()), abs=1e-4) == -0.2616

    # Verify the four displayed binary deltas
    b_map = {r["case_id"]: r["binary_relevance"]["ndcg_delta"] for r in non_zero_binary}
    assert pytest.approx(b_map["eval-test-ans-01"], abs=1e-4) == -0.3691
    assert pytest.approx(b_map["eval-test-ans-13"], abs=1e-4) == 0.0694
    assert pytest.approx(b_map["eval-test-ans-14"], abs=1e-4) == 0.0412
    assert pytest.approx(b_map["eval-test-ans-15"], abs=1e-4) == -0.0269

    # Sum of the four displayed binary deltas equals -0.2854
    assert pytest.approx(sum(b_map.values()), abs=1e-4) == -0.2854


def test_phase_12_1_4_historical_reconciliation_metadata_and_lineage():
    json_path = os.path.join("docs", "rag_evaluation_results.json")
    with open(json_path, "r", encoding="utf-8") as f:
        data = json.load(f)

    recon = data["metadata"]["historical_reconciliation"]
    assert recon["authoritative_benchmark_version"] in ["0.12.1.4-closed", "0.12.1.5-closed"]

    stats_recon = recon["paired_statistical_reconciliation"]
    bin_stats = stats_recon["phase_12_1_1_binary_relevance"]
    grad_stats = stats_recon["phase_12_1_2_graded_relevance"]

    # Verify binary statistical values are correctly computed
    re_bin = bin_stats["Cross-Encoder vs Dense"]
    assert re_bin["paired_mean_delta"] == -0.0143
    assert re_bin["t_statistic"] == -0.7451
    assert re_bin["p_value"] == 0.4653
    assert re_bin["paired_sum_deltas"] == -0.2854

    # Verify graded statistical values are correctly computed
    re_grad = grad_stats["Cross-Encoder vs Dense"]
    assert re_grad["paired_mean_delta"] == -0.0131
    assert re_grad["t_statistic"] == -0.6800
    assert re_grad["p_value"] == 0.5047
    assert re_grad["paired_sum_deltas"] == -0.2616

    # Both binary and graded must demonstrate non-significance (p > 0.40)
    assert re_bin["p_value"] > 0.40
    assert re_grad["p_value"] > 0.40

    # Lineage must preserve all historical versions
    lineage = recon["version_lineage_and_superseded_status"]
    assert "Phase 12 (0.12.0)" in lineage
    assert "Phase 12.1 (0.12.1)" in lineage
    assert "Phase 12.1.1 (0.12.1.1)" in lineage
    assert "Phase 12.1.2 (0.12.1.2)" in lineage
    assert "Phase 12.1.3 (0.12.1.3-closed)" in lineage
    assert "Phase 12.1.4 (0.12.1.4-closed)" in lineage


def test_phase_12_1_4_paired_stats_regression_and_mathematical_identities():
    import numpy as np
    from scipy import stats

    json_path = os.path.join("docs", "rag_evaluation_results.json")
    with open(json_path, "r", encoding="utf-8") as f:
        data = json.load(f)

    raw = data["raw_per_query_results"]
    dense_scores = np.array([r["dense_ndcg_at_10"] for r in raw], dtype=np.float64)
    rerank_scores = np.array([r["reranked_ndcg_at_10"] for r in raw], dtype=np.float64)
    deltas = rerank_scores - dense_scores

    # 1. Displayed paired mean equals the mean of serialized per-query differences
    abl_rerank = data["ablation"][4]
    displayed_mean = abl_rerank["ndcg_delta_vs_dense"]
    calculated_mean = float(np.mean(deltas))
    assert pytest.approx(calculated_mean, abs=1e-4) == displayed_mean
    assert pytest.approx(calculated_mean, abs=1e-4) == -0.0131

    # 2. t-statistic equals mean divided by SEM
    std_d = float(np.std(deltas, ddof=1))
    sem = std_d / np.sqrt(len(deltas))
    calc_t = calculated_mean / sem
    t_res = stats.ttest_rel(rerank_scores, dense_scores)
    assert pytest.approx(calc_t, abs=1e-5) == float(t_res.statistic)
    assert pytest.approx(float(t_res.statistic), abs=1e-4) == -0.6800

    # 3. Reported p-value matches the calculated test
    reported_p = abl_rerank["p_value_ndcg"]
    assert pytest.approx(float(t_res.pvalue), abs=1e-4) == reported_p
    assert pytest.approx(float(t_res.pvalue), abs=1e-4) == 0.5047

    # 4. Sum of per-query differences equals aggregate paired sum within justified floating point tolerance
    unrounded_deltas = np.array([r["graded_relevance"]["unrounded_delta"] for r in raw], dtype=np.float64)
    unrounded_sum = float(np.sum(unrounded_deltas))
    displayed_sum = -0.2616
    assert abs(unrounded_sum - displayed_sum) < 0.0001
    assert abs(unrounded_sum - (len(raw) * float(np.mean(unrounded_deltas)))) < 1e-12
    assert abs(displayed_sum - (len(raw) * calculated_mean)) < 1e-4

    # 5. Verify binary relevance independent calculations
    dense_bin = np.array([r["binary_relevance"]["dense_ndcg_at_10"] for r in raw], dtype=np.float64)
    rerank_bin = np.array([r["binary_relevance"]["reranked_ndcg_at_10"] for r in raw], dtype=np.float64)
    bin_deltas = rerank_bin - dense_bin
    bin_mean = float(np.mean(bin_deltas))
    bin_std = float(np.std(bin_deltas, ddof=1))
    bin_sem = bin_std / np.sqrt(len(bin_deltas))
    bin_t = bin_mean / bin_sem
    bin_t_res = stats.ttest_rel(rerank_bin, dense_bin)

    assert pytest.approx(bin_mean, abs=1e-4) == -0.0143
    assert pytest.approx(bin_std, abs=1e-4) == 0.0856
    assert pytest.approx(bin_sem, abs=1e-4) == 0.0192
    assert pytest.approx(bin_t, abs=1e-4) == -0.7451
    assert pytest.approx(float(bin_t_res.pvalue), abs=1e-4) == 0.4653


def test_phase_12_1_5_bonferroni_critical_value_and_confidence_interval():
    import numpy as np
    from scipy import stats

    json_path = os.path.join("docs", "rag_evaluation_results.json")
    with open(json_path, "r", encoding="utf-8") as f:
        data = json.load(f)

    # 1. Verify Bonferroni critical value in metadata matches scipy.stats.t.ppf(0.99375, 19)
    df = 19
    expected_t_crit = float(stats.t.ppf(0.99375, df))
    reported_t_crit = data["metadata"]["statistical_methodology"]["bonferroni_t_critical"]
    assert pytest.approx(expected_t_crit, abs=1e-12) == reported_t_crit
    assert pytest.approx(reported_t_crit, abs=1e-4) == 2.7586

    # 2. Independently verify Binary Cross-Encoder Bonferroni CI against scipy.stats.t.ppf
    raw = data["raw_per_query_results"]
    dense_bin = np.array([r["binary_relevance"]["dense_ndcg_at_10"] for r in raw], dtype=np.float64)
    rerank_bin = np.array([r["binary_relevance"]["reranked_ndcg_at_10"] for r in raw], dtype=np.float64)
    deltas = rerank_bin - dense_bin

    mean_d = float(np.mean(deltas))
    sem = float(np.std(deltas, ddof=1) / np.sqrt(len(deltas)))

    margin = expected_t_crit * sem
    manual_ci_low = mean_d - margin
    manual_ci_high = mean_d + margin

    scipy_ci = stats.t.interval(0.9875, df=df, loc=mean_d, scale=sem)
    assert pytest.approx(manual_ci_low, abs=1e-12) == float(scipy_ci[0])
    assert pytest.approx(manual_ci_high, abs=1e-12) == float(scipy_ci[1])

    # Check reported 4dp values
    assert round(manual_ci_low, 4) == -0.0671
    assert round(manual_ci_high, 4) == 0.0386

    # Check serialized JSON binary reconciliation matches
    bin_recon = data["metadata"]["historical_reconciliation"]["paired_statistical_reconciliation"]["phase_12_1_1_binary_relevance"]["Cross-Encoder vs Dense"]
    assert bin_recon["bonferroni_ci"] == [-0.0671, 0.0386]

    # 3. Verify Graded Cross-Encoder Bonferroni CI as well
    abl_rerank = data["ablation"][4]
    assert abl_rerank["ci_bonferroni_ndcg"] == [-0.0661, 0.04]
    paired_summary = data["metadata"]["statistical_methodology"]["paired_comparisons_summary"]["Cross-Encoder vs Dense-Only"]
    assert paired_summary["bonferroni_ci_98_75"] == [-0.0661, 0.04]


def test_phase_12_1_5_aggregate_and_paired_mean_identity():
    import numpy as np

    json_path = os.path.join("docs", "rag_evaluation_results.json")
    with open(json_path, "r", encoding="utf-8") as f:
        data = json.load(f)

    raw = data["raw_per_query_results"]

    # 1. Graded relevance identity: mean(C - D) == mean(C) - mean(D)
    graded_dense = np.array([r["dense_ndcg_at_10"] for r in raw], dtype=np.float64)
    graded_rerank = np.array([r["reranked_ndcg_at_10"] for r in raw], dtype=np.float64)
    graded_deltas = graded_rerank - graded_dense

    mean_cand_graded = float(np.mean(graded_rerank))
    mean_dense_graded = float(np.mean(graded_dense))
    diff_of_means_graded = mean_cand_graded - mean_dense_graded
    mean_of_diffs_graded = float(np.mean(graded_deltas))

    # Must equal to within floating point precision
    assert abs(diff_of_means_graded - mean_of_diffs_graded) < 1e-12

    # 2. Binary relevance identity: mean(C - D) == mean(C) - mean(D)
    binary_dense = np.array([r["binary_relevance"]["dense_ndcg_at_10"] for r in raw], dtype=np.float64)
    binary_rerank = np.array([r["binary_relevance"]["reranked_ndcg_at_10"] for r in raw], dtype=np.float64)
    binary_deltas = binary_rerank - binary_dense

    mean_cand_bin = float(np.mean(binary_rerank))
    mean_dense_bin = float(np.mean(binary_dense))
    diff_of_means_bin = mean_cand_bin - mean_dense_bin
    mean_of_diffs_bin = float(np.mean(binary_deltas))

    assert abs(diff_of_means_bin - mean_of_diffs_bin) < 1e-12

    # 3. Full ablation runner check: test all 4 stages against dense baseline
    from app.evaluation.datasets.loader import BenchmarkDataLoader
    from app.evaluation.datasets.schemas import DatasetSplit
    from app.evaluation.retrieval.ablation import RetrievalAblationRunner
    from app.evaluation.metrics.retrieval import compute_ndcg_at_k

    loader = BenchmarkDataLoader()
    corpus = loader.load_corpus()
    cases = loader.load_split(DatasetSplit.TEST)
    eval_cases = [c for c in cases if len(c.relevant_chunk_ids) > 0]

    runner = RetrievalAblationRunner(corpus)
    _, all_preds = runner.run_ablation(cases)

    # Compute unrounded per-query scores for all stages
    dense_scores = np.array([
        compute_ndcg_at_k(all_preds["Dense Only"].get(c.case_id, []), set(c.relevant_chunk_ids), k=10, graded_relevance=getattr(c, "graded_relevance", None))
        for c in eval_cases
    ], dtype=np.float64)

    for stage_name, preds_dict in all_preds.items():
        if stage_name == "Dense Only":
            continue
        cand_scores = np.array([
            compute_ndcg_at_k(preds_dict.get(c.case_id, []), set(c.relevant_chunk_ids), k=10, graded_relevance=getattr(c, "graded_relevance", None))
            for c in eval_cases
        ], dtype=np.float64)

        # Mathematical identity: mean(C - D) == mean(C) - mean(D)
        cand_minus_dense = cand_scores - dense_scores
        assert abs(float(np.mean(cand_minus_dense)) - (float(np.mean(cand_scores)) - float(np.mean(dense_scores)))) < 1e-12


def test_phase_12_1_5_lineage_and_metadata():
    json_path = os.path.join("docs", "rag_evaluation_results.json")
    with open(json_path, "r", encoding="utf-8") as f:
        data = json.load(f)

    assert data["metadata"]["framework_version"] == "0.12.1.5-closed"
    recon = data["metadata"]["historical_reconciliation"]
    assert recon["authoritative_benchmark_version"] == "0.12.1.5-closed"

    lineage = recon["version_lineage_and_superseded_status"]
    assert "Phase 12 (0.12.0)" in lineage
    assert "Phase 12.1 (0.12.1)" in lineage
    assert "Phase 12.1.1 (0.12.1.1)" in lineage
    assert "Phase 12.1.2 (0.12.1.2)" in lineage
    assert "Phase 12.1.3 (0.12.1.3-closed)" in lineage
    assert "Phase 12.1.4 (0.12.1.4-closed)" in lineage
    assert "Phase 12.1.5 (0.12.1.5-closed)" in lineage
    assert "SUPERSEDED" in lineage["Phase 12.1.4 (0.12.1.4-closed)"]
    assert "AUTHORITATIVE_FINAL" in lineage["Phase 12.1.5 (0.12.1.5-closed)"]

