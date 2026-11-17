"""
CS Scholar AI - Learning Studio AI Generation Service
Orchestrates LLM generation for academic quizzes and active-recall flashcard decks.
"""
import logging
import time
from typing import Optional, List, Dict, Any

from app.learning.schemas import (
    GenerateQuizInternalRequest,
    GenerateQuizInternalResponse,
    GeneratedQuestionDto,
    GeneratedOptionDto,
    GenerateFlashcardsInternalRequest,
    GenerateFlashcardsInternalResponse,
    GeneratedFlashcardDto,
)
from app.learning.prompt_builder import LearningPromptBuilder
from app.providers.factory import LLMProviderFactory
from app.providers.schemas import LLMGenerationConfig
from app.providers.exceptions import MalformedResponseError, LLMError

logger = logging.getLogger(__name__)


class LearningGenerationService:
    """Service handling Quiz and Flashcard generation with schema validation and deduplication."""

    def __init__(self, prompt_builder: Optional[LearningPromptBuilder] = None):
        self.prompt_builder = prompt_builder or LearningPromptBuilder()

    def generate_quiz(self, request: GenerateQuizInternalRequest) -> GenerateQuizInternalResponse:
        """Generate academic assessment questions from grounded context."""
        start_time = time.time()
        logger.info(
            "Generating quiz: user=%s, source='%s', count=%d, difficulty=%s",
            request.user_id, request.source_title, request.question_count, request.difficulty_level
        )

        messages = self.prompt_builder.build_quiz_prompt(request)
        provider = LLMProviderFactory.get_provider(
            provider_name=request.provider,
            model_name=request.model_name
        )
        gen_config = LLMGenerationConfig(
            temperature=0.3,
            max_output_tokens=3500,
            json_mode=True
        )

        llm_response = provider.generate(
            messages=messages,
            config=gen_config,
            model_name=request.model_name
        )

        parsed = llm_response.parsed_json or {}
        raw_questions = parsed.get("questions", [])
        if not raw_questions and isinstance(parsed, list):
            raw_questions = parsed

        validated_questions: List[GeneratedQuestionDto] = []
        seen_questions = set()

        for idx, q_data in enumerate(raw_questions):
            if not isinstance(q_data, dict):
                continue

            q_text = str(q_data.get("question_text", "")).strip()
            if not q_text or q_text.lower() in seen_questions:
                continue

            q_type = str(q_data.get("question_type", "MULTIPLE_CHOICE")).upper()
            if q_type not in ["MULTIPLE_CHOICE", "TRUE_FALSE"]:
                q_type = "MULTIPLE_CHOICE"

            explanation = str(q_data.get("explanation", "")).strip()
            if not explanation:
                explanation = "Based on the provided Computer Science curriculum materials."

            # Process options with deduplication and strict validation
            raw_options = q_data.get("options", [])
            validated_options: List[GeneratedOptionDto] = []
            seen_options = set()
            correct_count = 0

            if isinstance(raw_options, list):
                for opt_idx, opt_data in enumerate(raw_options):
                    if isinstance(opt_data, dict):
                        opt_text = str(opt_data.get("option_text", "")).strip()
                        is_corr = bool(opt_data.get("is_correct", False))
                        if opt_text and opt_text.lower() not in seen_options:
                            seen_options.add(opt_text.lower())
                            if is_corr:
                                correct_count += 1
                            validated_options.append(
                                GeneratedOptionDto(
                                    optionIndex=len(validated_options),
                                    optionText=opt_text,
                                    isCorrect=is_corr
                                )
                            )

            # Fallback if options are malformed
            if q_type == "TRUE_FALSE":
                if len(validated_options) < 2:
                    validated_options = [
                        GeneratedOptionDto(optionIndex=0, optionText="True", isCorrect=True),
                        GeneratedOptionDto(optionIndex=1, optionText="False", isCorrect=False),
                    ]
                    correct_count = 1
                else:
                    # Enforce exactly one is_correct
                    has_true = any(o.is_correct for o in validated_options)
                    if not has_true:
                        validated_options[0].is_correct = True
            elif q_type == "MULTIPLE_CHOICE":
                if len(validated_options) < 2:
                    continue
                # Ensure exactly one option is marked correct
                if correct_count != 1 and validated_options:
                    # If multiple were marked correct, keep only the first true
                    marked_one = False
                    for o in validated_options:
                        if o.is_correct and not marked_one:
                            marked_one = True
                        else:
                            o.is_correct = False
                    if not marked_one:
                        validated_options[0].is_correct = True

            seen_questions.add(q_text.lower())
            validated_questions.append(
                GeneratedQuestionDto(
                    questionIndex=len(validated_questions),
                    questionText=q_text,
                    questionType=q_type,
                    difficultyLevel=request.difficulty_level,
                    explanation=explanation,
                    sourceReference=q_data.get("source_reference") or request.source_title,
                    pageNumber=q_data.get("page_number"),
                    slideNumber=q_data.get("slide_number"),
                    topicName=q_data.get("topic_name") or request.concept_focus,
                    options=validated_options
                )
            )

            if len(validated_questions) >= request.question_count:
                break

        # Fallback if zero questions could be extracted
        if not validated_questions:
            logger.warning("No questions extracted from LLM response. Generating fallback question.")
            validated_questions.append(
                GeneratedQuestionDto(
                    questionIndex=0,
                    questionText=f"Which core concept is prominently examined in {request.source_title}?",
                    questionType="MULTIPLE_CHOICE",
                    difficultyLevel=request.difficulty_level,
                    explanation=f"This foundational concept is introduced directly in {request.source_title}.",
                    sourceReference=request.source_title,
                    options=[
                        GeneratedOptionDto(optionIndex=0, optionText=request.concept_focus or request.source_title, isCorrect=True),
                        GeneratedOptionDto(optionIndex=1, optionText="Unrelated hardware register failure", isCorrect=False),
                        GeneratedOptionDto(optionIndex=2, optionText="Arbitrary network timeout", isCorrect=False),
                        GeneratedOptionDto(optionIndex=3, optionText="Unrelated memory address translation", isCorrect=False),
                    ]
                )
            )

        latency_ms = int((time.time() - start_time) * 1000)
        quiz_title = parsed.get("title") or f"{request.source_title} Quiz"

        return GenerateQuizInternalResponse(
            title=quiz_title,
            difficultyLevel=request.difficulty_level,
            questions=validated_questions,
            provider=llm_response.provider,
            modelName=llm_response.model_name,
            inputTokens=llm_response.input_tokens,
            outputTokens=llm_response.output_tokens,
            latencyMs=latency_ms
        )

    def generate_flashcards(self, request: GenerateFlashcardsInternalRequest) -> GenerateFlashcardsInternalResponse:
        """Generate high-yield active-recall flashcards from grounded context."""
        start_time = time.time()
        logger.info(
            "Generating flashcards: user=%s, source='%s', count=%d",
            request.user_id, request.source_title, request.card_count
        )

        messages = self.prompt_builder.build_flashcards_prompt(request)
        provider = LLMProviderFactory.get_provider(
            provider_name=request.provider,
            model_name=request.model_name
        )
        gen_config = LLMGenerationConfig(
            temperature=0.3,
            max_output_tokens=3000,
            json_mode=True
        )

        llm_response = provider.generate(
            messages=messages,
            config=gen_config,
            model_name=request.model_name
        )

        parsed = llm_response.parsed_json or {}
        raw_cards = parsed.get("cards", [])
        if not raw_cards and isinstance(parsed, list):
            raw_cards = parsed

        validated_cards: List[GeneratedFlashcardDto] = []
        seen_fronts = set()

        for card_data in raw_cards:
            if not isinstance(card_data, dict):
                continue

            front = str(card_data.get("front", "")).strip()
            back = str(card_data.get("back", "")).strip()

            if not front or not back or front.lower() in seen_fronts:
                continue

            seen_fronts.add(front.lower())
            validated_cards.append(
                GeneratedFlashcardDto(
                    front=front,
                    back=back,
                    topicName=card_data.get("topic_name") or request.concept_focus or request.source_title,
                    sourceReference=card_data.get("source_reference") or request.source_title,
                    pageNumber=card_data.get("page_number"),
                    slideNumber=card_data.get("slide_number")
                )
            )

            if len(validated_cards) >= request.card_count:
                break

        # Fallback if zero cards could be parsed
        if not validated_cards:
            logger.warning("No cards extracted from LLM response. Creating fallback card.")
            validated_cards.append(
                GeneratedFlashcardDto(
                    front=f"What is the key principle of {request.source_title}?",
                    back=f"{request.source_title} provides core Computer Science functionality within this domain.",
                    topicName=request.concept_focus or request.source_title,
                    sourceReference=request.source_title
                )
            )

        latency_ms = int((time.time() - start_time) * 1000)
        deck_title = parsed.get("title") or f"{request.source_title} Flashcards"

        return GenerateFlashcardsInternalResponse(
            title=deck_title,
            cards=validated_cards,
            provider=llm_response.provider,
            modelName=llm_response.model_name,
            inputTokens=llm_response.input_tokens,
            outputTokens=llm_response.output_tokens,
            latencyMs=latency_ms
        )

