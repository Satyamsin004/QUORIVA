"""
CS Scholar AI - Unit and Integration Tests for Learning Studio Services
Tests Quiz and Flashcard generation, schema validation, prompt building, deduplication, and internal API routes.
"""
from unittest.mock import MagicMock, patch
from uuid import uuid4
import pytest
from fastapi.testclient import TestClient

from app.main import app
from app.learning.schemas import (
    GenerateQuizInternalRequest,
    GenerateFlashcardsInternalRequest,
    GeneratedQuestionDto,
    GeneratedOptionDto,
    GeneratedFlashcardDto,
)
from app.learning.prompt_builder import LearningPromptBuilder
from app.learning.service import LearningGenerationService
from app.providers.schemas import LLMResponse


@pytest.fixture
def test_client():
    return TestClient(app)


def test_quiz_prompt_builder():
    builder = LearningPromptBuilder()
    req = GenerateQuizInternalRequest(
        userId=uuid4(),
        sourceType="WORKSPACE_DOCUMENTS",
        sourceTitle="Virtual Memory Notes",
        contextText="Paging divides memory into fixed-sized blocks called frames.",
        difficultyLevel="MEDIUM",
        questionCount=3,
        questionTypes=["MULTIPLE_CHOICE", "TRUE_FALSE"],
        conceptFocus="Page Replacement Algorithms"
    )

    messages = builder.build_quiz_prompt(req)
    assert len(messages) == 2
    assert messages[0].role == "system"
    assert "Computer Science Professor" in messages[0].content
    assert messages[1].role == "user"
    assert "<academic_context>" in messages[1].content
    assert "Paging divides memory into fixed-sized blocks" in messages[1].content
    assert "Page Replacement Algorithms" in messages[1].content


def test_flashcard_prompt_builder():
    builder = LearningPromptBuilder()
    req = GenerateFlashcardsInternalRequest(
        userId=uuid4(),
        sourceType="KNOWLEDGE_TOPIC",
        sourceTitle="Binary Search Trees",
        contextText="A BST is a rooted binary tree whose internal nodes each store a key.",
        cardCount=4,
        conceptFocus="Inorder Traversal"
    )

    messages = builder.build_flashcards_prompt(req)
    assert len(messages) == 2
    assert messages[0].role == "system"
    assert "active recall and spaced repetition" in messages[0].content
    assert "<academic_context>" in messages[1].content
    assert "Inorder Traversal" in messages[1].content


@patch("app.learning.service.LLMProviderFactory.get_provider")
def test_learning_generation_service_generate_quiz(mock_get_provider):
    mock_provider = MagicMock()
    mock_provider.generate.return_value = LLMResponse(
        content='{"title": "OS Quiz", "questions": []}',
        parsed_json={
            "title": "OS Memory Quiz",
            "questions": [
                {
                    "question_text": "What is the primary function of the TLB in virtual memory?",
                    "question_type": "MULTIPLE_CHOICE",
                    "explanation": "The TLB caches recently accessed virtual-to-physical address translations.",
                    "source_reference": "Virtual Memory Notes",
                    "options": [
                        {"option_text": "Caches virtual-to-physical page table entries", "is_correct": True},
                        {"option_text": "Stores dirty disk pages", "is_correct": False},
                        {"option_text": "Executes context switches", "is_correct": False},
                        {"option_text": "Performs ALU operations", "is_correct": False}
                    ]
                },
                {
                    "question_text": "Paging suffers from internal fragmentation.",
                    "question_type": "TRUE_FALSE",
                    "explanation": "True because the last frame allocated may not be completely filled.",
                    "options": [
                        {"option_text": "True", "is_correct": True},
                        {"option_text": "False", "is_correct": False}
                    ]
                }
            ]
        },
        provider="mock-provider",
        model_name="mock-model",
        input_tokens=150,
        output_tokens=220
    )
    mock_get_provider.return_value = mock_provider

    service = LearningGenerationService()
    req = GenerateQuizInternalRequest(
        userId=uuid4(),
        sourceType="WORKSPACE_DOCUMENTS",
        sourceTitle="OS Memory",
        contextText="TLB stands for Translation Lookaside Buffer.",
        difficultyLevel="HARD",
        questionCount=2
    )

    res = service.generate_quiz(req)
    assert res.title == "OS Memory Quiz"
    assert res.difficulty_level == "HARD"
    assert len(res.questions) == 2

    # Verify first question
    q1 = res.questions[0]
    assert q1.question_index == 0
    assert "TLB" in q1.question_text
    assert q1.question_type == "MULTIPLE_CHOICE"
    assert len(q1.options) == 4
    assert sum(1 for o in q1.options if o.is_correct) == 1

    # Verify second question (True/False)
    q2 = res.questions[1]
    assert q2.question_index == 1
    assert q2.question_type == "TRUE_FALSE"
    assert len(q2.options) == 2
    assert q2.options[0].option_text == "True"
    assert q2.options[0].is_correct is True


@patch("app.learning.service.LLMProviderFactory.get_provider")
def test_learning_generation_service_generate_flashcards(mock_get_provider):
    mock_provider = MagicMock()
    mock_provider.generate.return_value = LLMResponse(
        content='{"title": "DSA Deck", "cards": []}',
        parsed_json={
            "title": "Binary Trees Active Recall",
            "cards": [
                {
                    "front": "What is the height of a balanced binary tree with N nodes?",
                    "back": "O(log N)",
                    "topic_name": "Trees",
                    "source_reference": "DSA Chapter 4"
                },
                {
                    "front": "What traversal visits root, left, right in a binary tree?",
                    "back": "Pre-order traversal",
                    "topic_name": "Trees",
                    "source_reference": "DSA Chapter 4"
                }
            ]
        },
        provider="mock-provider",
        model_name="mock-model",
        input_tokens=100,
        output_tokens=150
    )
    mock_get_provider.return_value = mock_provider

    service = LearningGenerationService()
    req = GenerateFlashcardsInternalRequest(
        userId=uuid4(),
        sourceType="KNOWLEDGE_TOPIC",
        sourceTitle="Binary Trees",
        contextText="Binary tree height is O(log N) when balanced.",
        cardCount=2
    )

    res = service.generate_flashcards(req)
    assert res.title == "Binary Trees Active Recall"
    assert len(res.cards) == 2
    assert res.cards[0].front == "What is the height of a balanced binary tree with N nodes?"
    assert res.cards[0].back == "O(log N)"
    assert res.cards[1].front == "What traversal visits root, left, right in a binary tree?"
    assert res.cards[1].back == "Pre-order traversal"


def test_learning_internal_routes_security(test_client):
    # Without internal service key -> 401 or 422
    quiz_payload = {
        "userId": str(uuid4()),
        "sourceType": "WORKSPACE_DOCUMENTS",
        "sourceTitle": "Security Test",
        "contextText": "Context information for testing security.",
        "difficultyLevel": "MEDIUM",
        "questionCount": 2
    }

    res = test_client.post("/internal/v1/learning/quizzes/generate", json=quiz_payload)
    assert res.status_code in [401, 422]

    # With invalid key -> 403 Forbidden
    res = test_client.post(
        "/internal/v1/learning/quizzes/generate",
        json=quiz_payload,
        headers={"X-Internal-Service-Key": "wrong-key"}
    )
    assert res.status_code == 403

