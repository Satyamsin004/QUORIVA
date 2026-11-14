"""
CS Scholar AI - Phase 12 Retrieval, Citation, Generation & Learning Studio Metrics Tests
Verifies mathematical soundness, edge case handling, and precision of all evaluation metrics.
"""
import pytest
from uuid import uuid4
from app.evaluation.metrics.retrieval import (
    compute_recall_at_k,
    compute_precision_at_k,
    compute_mrr,
    compute_ndcg_at_k
)
from app.evaluation.metrics.citations import (
    extract_claims,
    extract_citation_tags,
    evaluate_citations_and_claims,
    CitationEvaluationReport
)
from app.evaluation.metrics.generation import (
    compute_fact_coverage,
    evaluate_answer_quality,
    compute_abstention_metrics,
    AnswerQualityResult,
    AbstentionMetricsReport,
    ConfusionMatrix
)
from app.evaluation.metrics.learning import (
    evaluate_quiz_quality,
    evaluate_flashcard_quality,
    compute_jaccard_similarity,
    QuizQualityReport,
    FlashcardQualityReport
)
from app.evaluation.judges.human_review import compute_cohens_kappa
from app.evaluation.datasets.schemas import (
    EvaluationCase,
    DifficultyLevel,
    QuestionCategory,
    ExpectedAnswerability,
    DatasetSplit
)
from app.rag.schemas import (
    AssembledContext,
    RetrievedContextChunk,
    TutorAnswerResponse,
    GroundingStatus,
    VerifiedCitation
)
from app.learning.schemas import (
    GenerateQuizInternalResponse,
    GeneratedQuestionDto,
    GeneratedOptionDto,
    GenerateFlashcardsInternalResponse,
    GeneratedFlashcardDto
)


class TestRetrievalMetrics:
    """Tests Recall@K, Precision@K, MRR, and nDCG@K across standard and edge-case ranking lists."""

    def test_recall_and_precision_at_k(self):
        c1, c2, c3, c4 = uuid4(), uuid4(), uuid4(), uuid4()
        relevant = [c1, c2]

        # Case 1: First item is relevant
        retrieved = [c1, c3, c4]
        assert compute_recall_at_k(retrieved, relevant, k=1) == 0.5
        assert compute_precision_at_k(retrieved, relevant, k=1) == 1.0

        # Case 2: Top 3 contains both c1 and c2
        retrieved = [c1, c2, c3]
        assert compute_recall_at_k(retrieved, relevant, k=3) == 1.0
        assert compute_precision_at_k(retrieved, relevant, k=3) == pytest.approx(2 / 3)

        # Case 3: Empty relevant or retrieved
        assert compute_recall_at_k([], relevant, k=5) == 0.0
        assert compute_precision_at_k([], relevant, k=5) == 0.0
        assert compute_recall_at_k(retrieved, [], k=5) == 0.0
        assert compute_precision_at_k(retrieved, [], k=5) == 0.0

    def test_mrr_first_relevant_rank(self):
        c1, c2, c3 = uuid4(), uuid4(), uuid4()
        relevant = [c1]

        # Rank 1 -> RR = 1.0
        assert compute_mrr([c1, c2, c3], relevant) == 1.0
        # Rank 2 -> RR = 0.5
        assert compute_mrr([c2, c1, c3], relevant) == 0.5
        # Rank 3 -> RR = 1/3
        assert compute_mrr([c2, c3, c1], relevant) == pytest.approx(1.0 / 3.0)
        # Not found -> RR = 0.0
        assert compute_mrr([c2, c3], relevant) == 0.0

    def test_ndcg_at_k_binary_and_graded(self):
        c1, c2, c3, c4 = uuid4(), uuid4(), uuid4(), uuid4()
        graded = {c1: 3, c2: 2, c3: 1}

        # Perfect ordering: c1 (rank 1), c2 (rank 2), c3 (rank 3)
        assert compute_ndcg_at_k([c1, c2, c3], graded_relevance=graded, k=3) == 1.0

        # Sub-optimal ordering: c3 (rank 1), c2 (rank 2), c1 (rank 3)
        sub_ndcg = compute_ndcg_at_k([c3, c2, c1], graded_relevance=graded, k=3)
        assert 0.0 < sub_ndcg < 1.0

        # Binary fallback when graded_relevance is None
        binary_ndcg = compute_ndcg_at_k([c1, c4], relevant_ids=[c1], k=2)
        assert binary_ndcg == 1.0

        # Non-retrieved relevant items
        assert compute_ndcg_at_k([c4], relevant_ids=[c1], k=1) == 0.0


class TestCitationAndGroundingMetrics:
    """Tests claim-level citation verification distinguishing validity, correctness, and faithfulness."""

    def test_extract_claims_and_tags(self):
        text = (
            "Recursion relies on the call stack [S1]. Each invocation allocates a frame [S1]. "
            "Stack overflows happen without a base case [S2]."
        )
        claims = extract_claims(text)
        assert len(claims) == 3
        assert "call stack [S1]" in claims[0]

        tags = extract_citation_tags(claims[0])
        assert tags == ["S1"]
        tags_multi = extract_citation_tags("Combined concept [S1] [S2].")
        assert tags_multi == ["S1", "S2"]

    def test_claim_level_faithfulness_and_hallucination(self):
        c1_id = uuid4()
        chunk = RetrievedContextChunk(
            source_id="S1",
            chunk_id=c1_id,
            document_id=uuid4(),
            document_title="Operating Systems",
            content="Deadlock requires mutual exclusion, hold and wait, no preemption, and circular wait.",
            token_count=18,
            page_number=1,
            section_heading="Coffman Conditions",
            relevance_score=0.99,
            rank=1
        )
        ctx = AssembledContext(
            chunks=[chunk],
            formatted_text="[S1] " + chunk.content,
            total_tokens=18,
            source_map={"S1": chunk}
        )

        # Supported claim
        answer = "A deadlock occurs when mutual exclusion, hold and wait, and circular wait conditions hold [S1]."
        rep: CitationEvaluationReport = evaluate_citations_and_claims(answer, ctx)
        assert rep.total_claims == 1
        assert rep.supported_claims == 1
        assert rep.claim_faithfulness == 1.0
        assert rep.source_id_validity_rate == 1.0
        assert rep.citation_precision == 1.0
        assert rep.hallucination_rate == 0.0

        # Hallucinated source ID ([S99] does not exist)
        answer_bad_source = "Deadlock requires circular wait [S99]."
        rep_bad = evaluate_citations_and_claims(answer_bad_source, ctx)
        assert rep_bad.source_id_validity_rate == 0.0
        assert rep_bad.claim_faithfulness == 0.0
        assert rep_bad.hallucination_rate == 1.0

        # Valid source ID but ungrounded claim content
        answer_unsupported = "Virtual memory paging utilizes segment registers and quantum teleportation [S1]."
        rep_unsupported = evaluate_citations_and_claims(answer_unsupported, ctx)
        assert rep_unsupported.source_id_validity_rate == 1.0
        assert rep_unsupported.citation_precision == 0.0
        assert rep_unsupported.claim_faithfulness == 0.0

    def test_abstention_claims_without_citations_are_faithful(self):
        ctx = AssembledContext(chunks=[], formatted_text="", total_tokens=0, source_map={})
        answer = "Based on the provided documents, there is insufficient evidence to answer this question."
        rep = evaluate_citations_and_claims(answer, ctx)
        assert rep.claim_faithfulness == 1.0
        assert rep.hallucination_rate == 0.0
        assert rep.citation_coverage == 1.0


class TestGenerationAndAbstentionMetrics:
    """Tests fact coverage, answer quality, and abstention reporting."""

    def test_compute_fact_coverage(self):
        key_facts = [
            "QuickSort has an average time complexity of O(N log N)",
            "Worst-case occurs when the pivot partition is completely unbalanced",
            "Randomized pivot selection avoids the quadratic worst case"
        ]
        answer = (
            "QuickSort achieves an average time complexity of O(N log N). "
            "However, when the chosen pivot is unbalanced, performance degrades to worst-case."
        )
        cov, matched, missing = compute_fact_coverage(answer, key_facts)
        assert cov == pytest.approx(2 / 3)
        assert len(matched) == 2
        assert len(missing) == 1

    def test_evaluate_answer_quality_unanswerable(self):
        case = EvaluationCase(
            case_id="case-unans-01",
            subject="Algorithms",
            topic="Quantum Computing",
            question="What is the decoherence rate of superconducting qubits?",
            expected_answer="The provided documents do not contain information regarding qubit decoherence.",
            key_facts=["Superconducting qubit decoherence is absent from corpus"],
            relevant_chunk_ids=[],
            graded_relevance={},
            difficulty=DifficultyLevel.ADVANCED,
            category=QuestionCategory.INSUFFICIENT_EVIDENCE,
            answerability=ExpectedAnswerability.UNANSWERABLE,
            split=DatasetSplit.DEV
        )

        # Correct abstention
        res_ok: AnswerQualityResult = evaluate_answer_quality(
            "Based on the provided documents, there is insufficient evidence to answer this question.",
            case
        )
        assert res_ok.correctness_score == 1.0
        assert res_ok.relevance_score == 1.0
        assert res_ok.completeness_score == 1.0

        # False hallucinated response to unanswerable question
        res_fail: AnswerQualityResult = evaluate_answer_quality(
            "The decoherence rate is approximately 10 microseconds under cryogenic shielding.",
            case
        )
        assert res_fail.correctness_score == 0.0

    def test_abstention_benchmark_reporting(self):
        unans_case = EvaluationCase(
            case_id="u1",
            subject="Databases",
            topic="Storage",
            question="Unknown question",
            expected_answer="Insufficient evidence",
            key_facts=["Fact absent"],
            relevant_chunk_ids=[],
            graded_relevance={},
            difficulty=DifficultyLevel.BEGINNER,
            category=QuestionCategory.INSUFFICIENT_EVIDENCE,
            answerability=ExpectedAnswerability.UNANSWERABLE,
            split=DatasetSplit.DEV
        )
        ans_case = EvaluationCase(
            case_id="a1",
            subject="Databases",
            topic="Indexes",
            question="What is a B-Tree?",
            expected_answer="Self balancing search tree",
            key_facts=["Self balancing search tree"],
            relevant_chunk_ids=[uuid4()],
            graded_relevance={},
            difficulty=DifficultyLevel.BEGINNER,
            category=QuestionCategory.DIRECT_FACTUAL,
            answerability=ExpectedAnswerability.ANSWERABLE,
            split=DatasetSplit.DEV
        )

        resp_abstained = TutorAnswerResponse(
            answer="Based on the provided documents, there is insufficient evidence.",
            grounding_status=GroundingStatus.INSUFFICIENT_EVIDENCE,
            insufficient_evidence=True,
            citations=[],
            provider="test",
            model_name="test"
        )
        resp_grounded = TutorAnswerResponse(
            answer="A B-Tree is a self balancing search tree [S1].",
            grounding_status=GroundingStatus.GROUNDED,
            insufficient_evidence=False,
            citations=[],
            provider="test",
            model_name="test"
        )

        report: AbstentionMetricsReport = compute_abstention_metrics([
            (unans_case, resp_abstained),
            (ans_case, resp_grounded)
        ])
        assert report.correct_abstention_rate == 1.0
        assert report.false_abstention_rate == 0.0
        assert report.unsupported_answer_rate == 0.0

    def test_quiz_quality_evaluation(self):
        quiz = GenerateQuizInternalResponse(
            title="Database Concurrency Quiz",
            difficulty_level="INTERMEDIATE",
            questions=[
                GeneratedQuestionDto(
                    question_index=0,
                    question_text="Which ACID property prevents dirty reads between concurrent transactions?",
                    explanation="Isolation ensures that intermediate states of concurrent transactions are invisible.",
                    topic_name="ACID Properties",
                    options=[
                        GeneratedOptionDto(option_index=0, option_text="Atomicity", is_correct=False),
                        GeneratedOptionDto(option_index=1, option_text="Isolation", is_correct=True),
                        GeneratedOptionDto(option_index=2, option_text="Durability", is_correct=False),
                        GeneratedOptionDto(option_index=3, option_text="Consistency", is_correct=False)
                    ]
                )
            ],
            provider="test-provider",
            model_name="test-model"
        )

        report: QuizQualityReport = evaluate_quiz_quality(quiz)
        assert report.total_questions == 1
        assert report.single_correct_answer_rate == 1.0
        assert report.distractor_plausibility_rate == 1.0
        assert report.question_clarity_score == 1.0

    def test_flashcard_quality_evaluation(self):
        flashcards = GenerateFlashcardsInternalResponse(
            title="Computer Networks Flashcards",
            cards=[
                GeneratedFlashcardDto(
                    front="What packets are exchanged in a standard TCP three-way connection handshake?",
                    back="SYN, SYN-ACK, and ACK establish bidirectional sequence synchronization.",
                    topic_name="TCP Handshake"
                )
            ],
            provider="test-provider",
            model_name="test-model"
        )

        report: FlashcardQualityReport = evaluate_flashcard_quality(flashcards)
        assert report.total_cards == 1
        assert report.conciseness_score == 1.0
        assert report.one_concept_per_card_rate == 1.0

    def test_abstention_confusion_matrix_3x3(self):
        c_ans = EvaluationCase(
            case_id="c-ans",
            subject="Test",
            topic="Test",
            question="Answerable?",
            expected_answer="Yes",
            relevant_chunk_ids=[uuid4()],
            answerability=ExpectedAnswerability.ANSWERABLE
        )
        c_part = EvaluationCase(
            case_id="c-part",
            subject="Test",
            topic="Test",
            question="Partially answerable?",
            expected_answer="Partially",
            relevant_chunk_ids=[uuid4()],
            answerability=ExpectedAnswerability.PARTIALLY_ANSWERABLE
        )
        c_unans = EvaluationCase(
            case_id="c-unans",
            subject="Test",
            topic="Test",
            question="Unanswerable?",
            expected_answer="None",
            relevant_chunk_ids=[],
            answerability=ExpectedAnswerability.UNANSWERABLE
        )

        resp_grounded = TutorAnswerResponse(
            answer="Fully grounded [S1].",
            grounding_status=GroundingStatus.GROUNDED,
            insufficient_evidence=False,
            citations=[],
            provider="test-provider",
            model_name="test-model"
        )
        resp_partial = TutorAnswerResponse(
            answer="Partially grounded [S1].",
            grounding_status=GroundingStatus.PARTIALLY_GROUNDED,
            insufficient_evidence=False,
            citations=[],
            provider="test-provider",
            model_name="test-model"
        )
        resp_abstained = TutorAnswerResponse(
            answer="Based on the documents, there is insufficient evidence.",
            grounding_status=GroundingStatus.INSUFFICIENT_EVIDENCE,
            insufficient_evidence=True,
            citations=[],
            provider="test-provider",
            model_name="test-model"
        )

        pairs = [
            (c_ans, resp_grounded),     # Fully Answerable -> Grounded
            (c_part, resp_partial),    # Partially Answerable -> Partially Grounded
            (c_unans, resp_abstained)  # Unanswerable -> Abstained
        ]

        report = compute_abstention_metrics(pairs)
        cm = report.confusion_matrix
        assert cm is not None
        assert cm.fully_ans_grounded == 1
        assert cm.fully_ans_partial == 0
        assert cm.fully_ans_abstained == 0

        assert cm.partial_ans_grounded == 0
        assert cm.partial_ans_partial == 1
        assert cm.partial_ans_abstained == 0

        assert cm.unans_grounded == 0
        assert cm.unans_partial == 0
        assert cm.unans_abstained == 1

        assert report.correct_abstention_rate == 1.0
        assert report.false_abstention_rate == 0.0
        assert report.unsupported_answer_rate == 0.0
        assert report.partial_grounding_accuracy == 1.0
        assert report.grounding_accuracy == 1.0

    def test_quiz_and_flashcard_jaccard_duplicate_detection(self):
        # Two very similar questions (Jaccard token overlap > 0.70)
        q1 = GeneratedQuestionDto(
            question_index=0,
            question_text="What is the worst-case time complexity of binary search tree operations?",
            explanation="Unbalanced BST degenerates to linear time search.",
            options=[
                GeneratedOptionDto(option_index=0, option_text="O(1)", is_correct=False),
                GeneratedOptionDto(option_index=1, option_text="O(log N)", is_correct=False),
                GeneratedOptionDto(option_index=2, option_text="O(N)", is_correct=True),
                GeneratedOptionDto(option_index=3, option_text="O(N^2)", is_correct=False)
            ]
        )
        q2 = GeneratedQuestionDto(
            question_index=1,
            question_text="What is the worst case time complexity of a binary search tree operation?",
            explanation="Unbalanced BST degenerates to linear time search.",
            options=[
                GeneratedOptionDto(option_index=0, option_text="O(1)", is_correct=False),
                GeneratedOptionDto(option_index=1, option_text="O(log N)", is_correct=False),
                GeneratedOptionDto(option_index=2, option_text="O(N)", is_correct=True),
                GeneratedOptionDto(option_index=3, option_text="O(N^2)", is_correct=False)
            ]
        )
        quiz = GenerateQuizInternalResponse(
            title="Duplicate Test Quiz",
            difficulty_level="MEDIUM",
            provider="test-provider",
            model_name="test-model",
            questions=[q1, q2]
        )
        report = evaluate_quiz_quality(quiz)
        assert report.total_questions == 2
        assert report.duplicate_question_rate == 0.5  # 1 duplicate detected out of 2 questions

        # Flashcard Jaccard duplicate test
        f1 = GeneratedFlashcardDto(front="What is the CAP theorem in distributed systems?", back="Consistency, Availability, Partition Tolerance.")
        f2 = GeneratedFlashcardDto(front="What is the CAP theorem for distributed system?", back="Consistency, Availability, Partition Tolerance.")
        cards = GenerateFlashcardsInternalResponse(
            title="Duplicate Test Flashcards",
            provider="test-provider",
            model_name="test-model",
            cards=[f1, f2]
        )
        fc_report = evaluate_flashcard_quality(cards)
        assert fc_report.total_cards == 2
        assert fc_report.duplicate_card_rate == 0.5

    def test_cohens_kappa_inter_annotator_agreement(self):
        # Identical ratings -> Kappa = 1.0
        r1 = ["CORRECT", "CORRECT", "INCORRECT", "CORRECT_ABSTENTION"]
        r2 = ["CORRECT", "CORRECT", "INCORRECT", "CORRECT_ABSTENTION"]
        assert compute_cohens_kappa(r1, r2) == 1.0

        # Partial disagreement
        r3 = ["CORRECT", "INCORRECT", "INCORRECT", "CORRECT_ABSTENTION"]
        kappa = compute_cohens_kappa(r1, r3)
        assert 0.0 < kappa < 1.0
