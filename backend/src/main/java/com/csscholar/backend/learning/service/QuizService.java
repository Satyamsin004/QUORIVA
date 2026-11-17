package com.csscholar.backend.learning.service;

import com.csscholar.backend.common.BadRequestException;
import com.csscholar.backend.common.ForbiddenException;
import com.csscholar.backend.common.LearningException;
import com.csscholar.backend.common.ResourceNotFoundException;
import com.csscholar.backend.document.entity.Document;
import com.csscholar.backend.document.entity.DocumentChunk;
import com.csscholar.backend.document.repository.DocumentChunkRepository;
import com.csscholar.backend.document.repository.DocumentRepository;
import com.csscholar.backend.knowledge.entity.KnowledgeTopic;
import com.csscholar.backend.knowledge.entity.KnowledgeTopicLearningObjective;
import com.csscholar.backend.knowledge.repository.KnowledgeTopicRepository;
import com.csscholar.backend.learning.client.LearningClient;
import com.csscholar.backend.learning.dto.*;
import com.csscholar.backend.learning.dto.internal.*;
import com.csscholar.backend.learning.entity.*;
import com.csscholar.backend.learning.enums.*;
import com.csscholar.backend.learning.repository.*;
import com.csscholar.backend.library.entity.LibraryResource;
import com.csscholar.backend.library.enums.LibraryResourceStatus;
import com.csscholar.backend.library.repository.LibraryResourceRepository;
import com.csscholar.backend.user.User;
import com.csscholar.backend.user.UserRepository;
import com.csscholar.backend.workspace.Workspace;
import com.csscholar.backend.workspace.WorkspaceRepository;
import com.csscholar.backend.workspace.WorkspaceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class QuizService {

    private static final Logger log = LoggerFactory.getLogger(QuizService.class);

    private final QuizRepository quizRepository;
    private final QuizQuestionRepository questionRepository;
    private final QuizQuestionOptionRepository optionRepository;
    private final QuizAttemptRepository attemptRepository;
    private final QuizAttemptAnswerRepository attemptAnswerRepository;
    private final LearningSessionRepository sessionRepository;
    private final UserRepository userRepository;
    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceService workspaceService;
    private final DocumentRepository documentRepository;
    private final DocumentChunkRepository chunkRepository;
    private final KnowledgeTopicRepository topicRepository;
    private final LibraryResourceRepository libraryResourceRepository;
    private final LearningClient learningClient;

    public QuizService(QuizRepository quizRepository,
                       QuizQuestionRepository questionRepository,
                       QuizQuestionOptionRepository optionRepository,
                       QuizAttemptRepository attemptRepository,
                       QuizAttemptAnswerRepository attemptAnswerRepository,
                       LearningSessionRepository sessionRepository,
                       UserRepository userRepository,
                       WorkspaceRepository workspaceRepository,
                       WorkspaceService workspaceService,
                       DocumentRepository documentRepository,
                       DocumentChunkRepository chunkRepository,
                       KnowledgeTopicRepository topicRepository,
                       LibraryResourceRepository libraryResourceRepository,
                       LearningClient learningClient) {
        this.quizRepository = quizRepository;
        this.questionRepository = questionRepository;
        this.optionRepository = optionRepository;
        this.attemptRepository = attemptRepository;
        this.attemptAnswerRepository = attemptAnswerRepository;
        this.sessionRepository = sessionRepository;
        this.userRepository = userRepository;
        this.workspaceRepository = workspaceRepository;
        this.workspaceService = workspaceService;
        this.documentRepository = documentRepository;
        this.chunkRepository = chunkRepository;
        this.topicRepository = topicRepository;
        this.libraryResourceRepository = libraryResourceRepository;
        this.learningClient = learningClient;
    }

    /**
     * Generate an AI-powered quiz based on selected academic context.
     */
    @Transactional
    public QuizSummaryDto generateQuiz(UUID userId, GenerateQuizRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        log.info("Generating quiz for user: {}, difficulty: {}, questionCount: {}",
                userId, request.getDifficultyLevel(), request.getQuestionCount());

        String sourceTitle;
        String contextText;
        LearningSourceType sourceType;
        String sourceId = null;
        Workspace workspace = null;
        KnowledgeTopic topic = null;

        // 0. Fallback to default user workspace if none specified
        if (request.getWorkspaceId() == null && request.getTopicId() == null && (request.getTopicSlug() == null || request.getTopicSlug().isBlank()) && request.getLibraryResourceId() == null) {
            List<Workspace> userWorkspaces = workspaceRepository.findByOwnerId(userId);
            if (!userWorkspaces.isEmpty()) {
                request.setWorkspaceId(userWorkspaces.get(0).getId());
            }
        }

        // 1. Determine Context Source
        String targetMaterialsJson = null;

        if (request.getTargetMaterials() != null && !request.getTargetMaterials().isEmpty()) {
            // Targeted Subject Revision flow
            sourceType = LearningSourceType.TARGETED_REVISION;
            workspaceService.getWorkspaceById(user.getId(), request.getWorkspaceId());
            workspace = workspaceRepository.findById(request.getWorkspaceId())
                    .orElseThrow(() -> new ResourceNotFoundException("Workspace not found"));

            String subject = request.getSubjectName() != null ? request.getSubjectName().trim() : "Targeted Revision";
            sourceTitle = subject + " Targeted Revision";

            List<TargetMaterialSummaryDto> summaryList = new ArrayList<>();
            StringBuilder sb = new StringBuilder();

            for (TargetMaterialRequest mat : request.getTargetMaterials()) {
                Document doc = documentRepository.findActiveByIdWithDetails(mat.getDocumentId())
                        .orElseThrow(() -> new ResourceNotFoundException("Document not found: " + mat.getDocumentId()));

                if (!doc.getWorkspace().getId().equals(workspace.getId())) {
                    throw new ForbiddenException("Document " + doc.getTitle() + " does not belong to the selected workspace.");
                }
                if (!"PROCESSED".equalsIgnoreCase(doc.getProcessingStatus().name())) {
                    throw new BadRequestException("Document '" + doc.getTitle() + "' is still processing and cannot be used for revision yet.");
                }

                int totalPages = doc.getPageCount() != null && doc.getPageCount() > 0 ? doc.getPageCount() : 1;
                int startPage = (mat.getStartPage() != null && mat.getStartPage() > 0) ? mat.getStartPage() : 1;
                int endPage = (mat.getEndPage() != null && mat.getEndPage() > 0) ? mat.getEndPage() : totalPages;

                if (startPage > endPage) {
                    throw new BadRequestException(String.format("Invalid page range for '%s': Start page (%d) cannot be greater than end page (%d).",
                            doc.getTitle(), startPage, endPage));
                }
                if (endPage > totalPages) {
                    throw new BadRequestException(String.format("Invalid page range for '%s': End page (%d) exceeds document total pages (%d).",
                            doc.getTitle(), endPage, totalPages));
                }

                summaryList.add(new TargetMaterialSummaryDto(doc.getId(), doc.getTitle(), startPage, endPage, totalPages));

                // Fetch chunks strictly within the targeted page range
                List<DocumentChunk> chunks = chunkRepository.findByDocumentIdAndPageNumberBetweenOrderByPageNumberAscChunkIndexAsc(
                        doc.getId(), startPage, endPage
                );

                if (chunks.isEmpty() && startPage == 1 && endPage >= totalPages) {
                    chunks = chunkRepository.findByDocumentIdAndProcessingVersionOrderByChunkIndexAsc(doc.getId(), 1);
                }

                if (chunks.isEmpty()) {
                    throw new BadRequestException(String.format("No readable content found in pages %d-%d of '%s'. Please select a valid page range.",
                            startPage, endPage, doc.getTitle()));
                }

                sb.append("\n=== Document: ").append(doc.getTitle())
                        .append(" (Pages ").append(startPage).append("-").append(endPage).append(") ===\n");

                for (DocumentChunk chunk : chunks) {
                    sb.append("[Document: ").append(doc.getTitle())
                            .append(", Page: ").append(chunk.getPageNumber() != null ? chunk.getPageNumber() : startPage)
                            .append("]\n")
                            .append(chunk.getContent()).append("\n\n");
                    if (sb.length() > 8000) break;
                }
                if (sb.length() > 8000) break;
            }

            contextText = sb.toString();
            sourceId = summaryList.get(0).getDocumentId().toString();

            try {
                targetMaterialsJson = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(summaryList);
            } catch (Exception e) {
                log.warn("Could not serialize target materials JSON: {}", e.getMessage());
            }

        } else if (request.getWorkspaceId() != null) {
            // Workspace Documents
            workspaceService.getWorkspaceById(user.getId(), request.getWorkspaceId());
            workspace = workspaceRepository.findById(request.getWorkspaceId())
                    .orElseThrow(() -> new ResourceNotFoundException("Workspace not found"));
            sourceType = LearningSourceType.WORKSPACE_DOCUMENTS;

            List<Document> docsToUse = new ArrayList<>();
            if (request.getDocumentIds() != null && !request.getDocumentIds().isEmpty()) {
                for (UUID docId : request.getDocumentIds()) {
                    Document doc = documentRepository.findActiveByIdWithDetails(docId)
                            .orElseThrow(() -> new ResourceNotFoundException("Document not found: " + docId));
                    if (!doc.getWorkspace().getId().equals(workspace.getId())) {
                        throw new ForbiddenException("Document " + docId + " does not belong to the selected workspace.");
                    }
                    if (!"PROCESSED".equals(doc.getProcessingStatus().name())) {
                        throw new BadRequestException("Document " + doc.getTitle() + " is still processing and cannot be used for a quiz yet.");
                    }
                    docsToUse.add(doc);
                }
            } else {
                List<Document> activeDocs = documentRepository.findByWorkspaceIdAndDeletedAtIsNull(workspace.getId());
                for (Document doc : activeDocs) {
                    if ("PROCESSED".equals(doc.getProcessingStatus())) {
                        docsToUse.add(doc);
                    }
                }
            }

            if (docsToUse.isEmpty()) {
                if (request.getConceptFocus() != null && !request.getConceptFocus().isBlank()) {
                    sourceType = LearningSourceType.CUSTOM;
                    sourceTitle = request.getConceptFocus();
                    sourceId = workspace.getId().toString();
                    contextText = "Academic Concept Focus: " + request.getConceptFocus() + "\n" +
                            "Generate rigorous, accurate Computer Science assessment questions and practice problems focusing specifically on " + request.getConceptFocus() + ".";
                } else {
                    throw new BadRequestException("No processed documents found in this workspace. Upload course materials or provide a concept focus.");
                }
            } else {
                StringBuilder sb = new StringBuilder();
                for (Document doc : docsToUse) {
                    List<DocumentChunk> chunks = chunkRepository.findByDocumentIdAndProcessingVersionOrderByChunkIndexAsc(doc.getId(), 1);
                    sb.append("--- Document: ").append(doc.getTitle()).append(" ---\n");
                    for (DocumentChunk chunk : chunks) {
                        sb.append(chunk.getContent()).append("\n\n");
                        if (sb.length() > 8000) break; // Bounded context budget for LLM TPM limits
                    }
                    if (sb.length() > 8000) break;
                }

                contextText = sb.toString();
                if (contextText.isBlank()) {
                    if (request.getConceptFocus() != null && !request.getConceptFocus().isBlank()) {
                        contextText = "Academic Concept Focus: " + request.getConceptFocus();
                    } else {
                        throw new BadRequestException("Selected documents have not been processed into readable chunks yet.");
                    }
                }
                sourceTitle = docsToUse.size() == 1 ? docsToUse.get(0).getTitle() : workspace.getName();
                sourceId = docsToUse.get(0).getId().toString();
            }

        } else if (request.getTopicId() != null || (request.getTopicSlug() != null && !request.getTopicSlug().isBlank())) {
            // Knowledge Graph Topic
            if (request.getTopicId() != null) {
                topic = topicRepository.findById(request.getTopicId())
                        .orElseThrow(() -> new ResourceNotFoundException("Knowledge topic not found"));
            } else {
                topic = topicRepository.findBySlug(request.getTopicSlug().trim())
                        .orElseThrow(() -> new ResourceNotFoundException("Knowledge topic not found: " + request.getTopicSlug()));
            }

            sourceType = LearningSourceType.KNOWLEDGE_TOPIC;
            sourceTitle = topic.getName();
            sourceId = topic.getId().toString();

            StringBuilder sb = new StringBuilder();
            sb.append("Topic: ").append(topic.getName()).append("\n");
            sb.append("Subject: ").append(topic.getSubject() != null ? topic.getSubject().getName() : "Computer Science").append("\n");
            sb.append("Summary: ").append(topic.getSummary()).append("\n\n");
            if (topic.getDescription() != null) {
                sb.append("Detailed Overview: ").append(topic.getDescription()).append("\n\n");
            }
            if (topic.getLearningObjectives() != null && !topic.getLearningObjectives().isEmpty()) {
                sb.append("Key Learning Objectives:\n");
                for (KnowledgeTopicLearningObjective obj : topic.getLearningObjectives()) {
                    sb.append("- ").append(obj.getObjective()).append("\n");
                }
            }
            contextText = sb.toString();

        } else if (request.getLibraryResourceId() != null) {
            // Published Shared Library Resource
            LibraryResource resource = libraryResourceRepository.findById(request.getLibraryResourceId())
                    .orElseThrow(() -> new ResourceNotFoundException("Library resource not found"));
            if (resource.getStatus() != LibraryResourceStatus.PUBLISHED) {
                throw new BadRequestException("Only PUBLISHED library resources can be used for quiz generation.");
            }
            sourceType = LearningSourceType.LIBRARY_RESOURCE;
            sourceTitle = resource.getTitle();
            sourceId = resource.getId().toString();

            StringBuilder sb = new StringBuilder();
            sb.append("Library Resource: ").append(resource.getTitle()).append("\n");
            sb.append("Category: ").append(resource.getCategory().name()).append("\n");
            sb.append("Description: ").append(resource.getDescription()).append("\n");
            contextText = sb.toString();

        } else {
            throw new BadRequestException("A valid context source (workspace document, knowledge topic, or library resource) must be selected.");
        }

        // 2. Build Internal Request & Call AI Service
        List<String> questionTypeNames = request.getQuestionTypes() != null
                ? request.getQuestionTypes().stream().map(Enum::name).collect(Collectors.toList())
                : List.of("MULTIPLE_CHOICE");

        GenerateQuizInternalRequest internalRequest = new GenerateQuizInternalRequest(
                user.getId(),
                sourceType.name(),
                sourceTitle,
                contextText,
                request.getDifficultyLevel().name(),
                request.getQuestionCount(),
                questionTypeNames,
                request.getConceptFocus()
        );

        GenerateQuizInternalResponse aiResponse = learningClient.generateQuiz(internalRequest);

        // 3. Persist Quiz Entity
        Quiz quiz = new Quiz(
                user,
                workspace,
                aiResponse.getTitle() != null ? aiResponse.getTitle() : sourceTitle + " Quiz",
                "Generated " + request.getDifficultyLevel() + " quiz based on " + sourceTitle,
                sourceType,
                sourceId,
                topic,
                request.getDifficultyLevel(),
                request.getTimeLimitMinutes()
        );

        if (sourceType == LearningSourceType.TARGETED_REVISION) {
            quiz.setSubjectName(request.getSubjectName() != null ? request.getSubjectName().trim() : "Targeted Revision");
            quiz.setTargetMaterials(targetMaterialsJson);
        }

        for (GeneratedQuestionDto qDto : aiResponse.getQuestions()) {
            QuestionType qType;
            try {
                qType = QuestionType.valueOf(qDto.getQuestionType().toUpperCase());
            } catch (Exception e) {
                qType = QuestionType.MULTIPLE_CHOICE;
            }

            QuizQuestion question = new QuizQuestion(
                    quiz,
                    qDto.getQuestionIndex(),
                    qDto.getQuestionText(),
                    qType,
                    request.getDifficultyLevel(),
                    qDto.getExplanation(),
                    qDto.getSourceReference(),
                    qDto.getPageNumber(),
                    qDto.getSlideNumber(),
                    qDto.getTopicName()
            );

            for (GeneratedOptionDto optDto : qDto.getOptions()) {
                QuizQuestionOption option = new QuizQuestionOption(
                        question,
                        optDto.getOptionIndex(),
                        optDto.getOptionText(),
                        optDto.isCorrect()
                );
                question.addOption(option);
            }

            quiz.addQuestion(question);
        }

        Quiz savedQuiz = quizRepository.save(quiz);
        return mapToSummary(savedQuiz);
    }

    /**
     * List all quizzes generated by the user.
     */
    @Transactional(readOnly = true)
    public List<QuizSummaryDto> getQuizzesForUser(UUID userId) {
        return quizRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::mapToSummary)
                .collect(Collectors.toList());
    }

    /**
     * Get revision history for a subject.
     */
    @Transactional(readOnly = true)
    public List<QuizSummaryDto> getRevisionHistory(UUID userId, UUID workspaceId, String subjectName) {
        if (workspaceId != null) {
            return quizRepository.findByUserIdAndWorkspaceIdAndSubjectNameOrderByCreatedAtDesc(userId, workspaceId, subjectName)
                    .stream()
                    .map(this::mapToSummary)
                    .collect(Collectors.toList());
        }
        return quizRepository.findByUserIdAndSubjectNameOrderByCreatedAtDesc(userId, subjectName)
                .stream()
                .map(this::mapToSummary)
                .collect(Collectors.toList());
    }

    /**
     * List all materials for a given subject to populate the Targeted Revision scope selector.
     */
    @Transactional(readOnly = true)
    public List<SubjectMaterialDto> getSubjectMaterials(UUID userId, UUID workspaceId, String subjectName) {
        workspaceService.getWorkspaceById(userId, workspaceId);
        List<Document> docs = documentRepository.findByWorkspaceIdAndSubjectAndDeletedAtIsNull(workspaceId, subjectName);

        return docs.stream()
                .map(doc -> new SubjectMaterialDto(
                        doc.getId(),
                        doc.getTitle(),
                        doc.getOriginalFilename(),
                        doc.getFileType() != null ? doc.getFileType().name() : "PDF",
                        doc.getFileSize(),
                        doc.getPageCount() != null ? doc.getPageCount() : 1,
                        doc.getSubject(),
                        doc.getProcessingStatus() != null ? doc.getProcessingStatus().name() : "PENDING",
                        doc.getUploadedAt() != null ? doc.getUploadedAt() : doc.getCreatedAt()
                ))
                .collect(Collectors.toList());
    }

    /**
     * Get quiz practice view without revealing answers or explanations.
     */
    @Transactional(readOnly = true)
    public QuizPracticeDto getQuizPractice(UUID quizId, UUID userId) {
        Quiz quiz = quizRepository.findByIdAndUserId(quizId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Quiz not found with id: " + quizId));

        List<QuizQuestionPracticeDto> questionDtos = quiz.getQuestions().stream()
                .map(q -> {
                    List<QuizOptionPracticeDto> optionDtos = q.getOptions().stream()
                            .map(o -> new QuizOptionPracticeDto(o.getId(), o.getOptionIndex(), o.getOptionText()))
                            .collect(Collectors.toList());

                    return new QuizQuestionPracticeDto(
                            q.getId(),
                            q.getQuestionIndex(),
                            q.getQuestionText(),
                            q.getQuestionType(),
                            q.getDifficultyLevel(),
                            q.getSourceReference(),
                            q.getPageNumber(),
                            q.getSlideNumber(),
                            q.getTopicName(),
                            optionDtos
                    );
                })
                .collect(Collectors.toList());

        return new QuizPracticeDto(
                quiz.getId(),
                quiz.getTitle(),
                quiz.getDescription(),
                quiz.getSourceType(),
                quiz.getSourceId(),
                quiz.getDifficultyLevel(),
                quiz.getTotalQuestions(),
                quiz.getTimeLimitMinutes(),
                questionDtos,
                quiz.getCreatedAt()
        );
    }

    /**
     * Delete a quiz and its questions.
     */
    @Transactional
    public void deleteQuiz(UUID quizId, UUID userId) {
        Quiz quiz = quizRepository.findByIdAndUserId(quizId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Quiz not found with id: " + quizId));
        quizRepository.delete(quiz);
    }

    /**
     * Start a new quiz attempt or resume existing in-progress attempt.
     */
    @Transactional
    public QuizAttemptResponseDto startAttempt(UUID quizId, UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        Quiz quiz = quizRepository.findByIdAndUserId(quizId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Quiz not found with id: " + quizId));

        // Reuse existing IN_PROGRESS attempt if available to prevent duplicate dangling attempts
        Optional<QuizAttempt> existingAttempt = attemptRepository.findByQuizIdAndUserIdOrderByStartedAtDesc(quizId, userId)
                .stream()
                .filter(a -> a.getStatus() == AttemptStatus.IN_PROGRESS)
                .findFirst();

        if (existingAttempt.isPresent()) {
            return mapToAttemptResponse(existingAttempt.get());
        }

        QuizAttempt attempt = new QuizAttempt(quiz, user, quiz.getTotalQuestions());
        QuizAttempt saved = attemptRepository.save(attempt);

        return mapToAttemptResponse(saved);
    }

    /**
     * Retrieve an attempt.
     */
    @Transactional(readOnly = true)
    public QuizAttemptResponseDto getAttempt(UUID attemptId, UUID userId) {
        QuizAttempt attempt = attemptRepository.findByIdAndUserId(attemptId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Quiz attempt not found with id: " + attemptId));
        return mapToAttemptResponse(attempt);
    }

    /**
     * Save in-progress answers without evaluating correctness.
     */
    @Transactional
    public QuizAttemptResponseDto saveAnswers(UUID attemptId, SaveAnswersRequest request, UUID userId) {
        QuizAttempt attempt = attemptRepository.findByIdAndUserId(attemptId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Quiz attempt not found with id: " + attemptId));

        if (attempt.getStatus() != AttemptStatus.IN_PROGRESS) {
            throw new BadRequestException("Cannot modify answers for a submitted attempt.");
        }

        if (request.getTimeSpentSeconds() != null) {
            attempt.setTimeSpentSeconds(request.getTimeSpentSeconds());
        }

        saveAnswersInternal(attempt, request.getAnswers());
        attemptRepository.save(attempt);

        return mapToAttemptResponse(attempt);
    }

    /**
     * Submit attempt, perform server-side evaluation, score, and persist learning session.
     */
    @Transactional
    public QuizAttemptResponseDto submitAttempt(UUID attemptId, SaveAnswersRequest request, UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        QuizAttempt attempt = attemptRepository.findByIdAndUserId(attemptId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Quiz attempt not found with id: " + attemptId));

        if (attempt.getStatus() == AttemptStatus.SUBMITTED) {
            throw new BadRequestException("Quiz attempt has already been submitted.");
        }

        if (request != null) {
            if (request.getTimeSpentSeconds() != null) {
                attempt.setTimeSpentSeconds(request.getTimeSpentSeconds());
            }
            if (request.getAnswers() != null && !request.getAnswers().isEmpty()) {
                saveAnswersInternal(attempt, request.getAnswers());
            }
        }

        // Server-Side Answer Scoring
        List<QuizAttemptAnswer> answers = attemptAnswerRepository.findByAttemptId(attempt.getId());
        int correctCount = 0;

        for (QuizAttemptAnswer ans : answers) {
            if (ans.getSelectedOption() != null) {
                boolean isCorrect = ans.getSelectedOption().isCorrect();
                ans.setIsCorrect(isCorrect);
                if (isCorrect) correctCount++;
            } else {
                ans.setIsCorrect(false);
            }
            attemptAnswerRepository.save(ans);
        }

        attempt.setCorrectAnswersCount(correctCount);
        double score = attempt.getTotalQuestions() > 0
                ? ((double) correctCount / attempt.getTotalQuestions()) * 100.0
                : 0.0;
        score = Math.round(score * 10.0) / 10.0;
        attempt.setScore(score);
        attempt.setStatus(AttemptStatus.SUBMITTED);
        attempt.setSubmittedAt(Instant.now());

        QuizAttempt savedAttempt = attemptRepository.save(attempt);

        // Record Learning Session for Analytics
        LearningSession session = new LearningSession(
                user,
                SessionType.QUIZ_ATTEMPT,
                savedAttempt.getId(),
                savedAttempt.getTimeSpentSeconds() != null ? savedAttempt.getTimeSpentSeconds() : 0,
                savedAttempt.getTotalQuestions(),
                score
        );
        sessionRepository.save(session);

        return mapToAttemptResponse(savedAttempt);
    }

    /**
     * Full attempt review with question explanations, correct answers, and student responses.
     */
    @Transactional(readOnly = true)
    public QuizAttemptReviewDto getAttemptReview(UUID attemptId, UUID userId) {
        QuizAttempt attempt = attemptRepository.findByIdAndUserId(attemptId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Quiz attempt not found with id: " + attemptId));

        if (attempt.getStatus() != AttemptStatus.SUBMITTED) {
            throw new BadRequestException("Review is only available after submitting the quiz attempt.");
        }

        Map<UUID, QuizAttemptAnswer> answerMap = attemptAnswerRepository.findByAttemptId(attempt.getId())
                .stream()
                .collect(Collectors.toMap(a -> a.getQuestion().getId(), a -> a, (k1, k2) -> k1));

        List<QuizQuestionReviewDto> questionReviews = attempt.getQuiz().getQuestions().stream()
                .map(q -> {
                    QuizAttemptAnswer userAns = answerMap.get(q.getId());
                    UUID selectedOptId = userAns != null && userAns.getSelectedOption() != null
                            ? userAns.getSelectedOption().getId()
                            : null;
                    Boolean isCorrect = userAns != null ? userAns.getIsCorrect() : false;

                    UUID correctOptId = q.getOptions().stream()
                            .filter(QuizQuestionOption::isCorrect)
                            .map(QuizQuestionOption::getId)
                            .findFirst()
                            .orElse(null);

                    List<QuizOptionReviewDto> optionReviews = q.getOptions().stream()
                            .map(o -> new QuizOptionReviewDto(o.getId(), o.getOptionIndex(), o.getOptionText(), o.isCorrect()))
                            .collect(Collectors.toList());

                    return new QuizQuestionReviewDto(
                            q.getId(),
                            q.getQuestionIndex(),
                            q.getQuestionText(),
                            q.getQuestionType(),
                            q.getExplanation(),
                            q.getSourceReference(),
                            q.getPageNumber(),
                            q.getSlideNumber(),
                            q.getTopicName(),
                            isCorrect,
                            selectedOptId,
                            correctOptId,
                            optionReviews
                    );
                })
                .collect(Collectors.toList());

        return new QuizAttemptReviewDto(
                attempt.getId(),
                attempt.getQuiz().getId(),
                attempt.getQuiz().getTitle(),
                attempt.getScore(),
                attempt.getTotalQuestions(),
                attempt.getCorrectAnswersCount(),
                attempt.getTimeSpentSeconds(),
                attempt.getStartedAt(),
                attempt.getSubmittedAt(),
                questionReviews
        );
    }

    private void saveAnswersInternal(QuizAttempt attempt, List<SaveAnswersRequest.AnswerItemDto> answers) {
        if (answers == null) return;

        for (SaveAnswersRequest.AnswerItemDto item : answers) {
            QuizQuestion question = questionRepository.findById(item.getQuestionId())
                    .orElseThrow(() -> new ResourceNotFoundException("Question not found: " + item.getQuestionId()));

            QuizQuestionOption option = null;
            if (item.getSelectedOptionId() != null) {
                option = optionRepository.findById(item.getSelectedOptionId())
                        .orElseThrow(() -> new ResourceNotFoundException("Option not found: " + item.getSelectedOptionId()));
            }

            Optional<QuizAttemptAnswer> existing = attemptAnswerRepository.findByAttemptIdAndQuestionId(attempt.getId(), question.getId());
            if (existing.isPresent()) {
                QuizAttemptAnswer ans = existing.get();
                ans.setSelectedOption(option);
                ans.setSelectedText(item.getSelectedText());
                ans.setMarkedForReview(item.isMarkedForReview());
                ans.setAnsweredAt(Instant.now());
                attemptAnswerRepository.save(ans);
            } else {
                QuizAttemptAnswer ans = new QuizAttemptAnswer(
                        attempt,
                        question,
                        option,
                        item.getSelectedText(),
                        item.isMarkedForReview()
                );
                attemptAnswerRepository.save(ans);
            }
        }
    }

    private QuizSummaryDto mapToSummary(Quiz quiz) {
        return new QuizSummaryDto(
                quiz.getId(),
                quiz.getTitle(),
                quiz.getDescription(),
                quiz.getSourceType(),
                quiz.getSourceId(),
                quiz.getDifficultyLevel(),
                quiz.getTotalQuestions(),
                quiz.getTimeLimitMinutes(),
                quiz.getSubjectName(),
                quiz.getTargetMaterials(),
                quiz.getCreatedAt()
        );
    }

    private QuizAttemptResponseDto mapToAttemptResponse(QuizAttempt attempt) {
        List<QuizAttemptAnswer> saved = attemptAnswerRepository.findByAttemptId(attempt.getId());
        List<QuizAttemptResponseDto.SavedAnswerDto> answerDtos = saved.stream()
                .map(a -> new QuizAttemptResponseDto.SavedAnswerDto(
                        a.getQuestion().getId(),
                        a.getSelectedOption() != null ? a.getSelectedOption().getId() : null,
                        a.getSelectedText(),
                        a.isMarkedForReview()
                ))
                .collect(Collectors.toList());

        return new QuizAttemptResponseDto(
                attempt.getId(),
                attempt.getQuiz().getId(),
                attempt.getQuiz().getTitle(),
                attempt.getStatus(),
                attempt.getScore(),
                attempt.getTotalQuestions(),
                attempt.getCorrectAnswersCount(),
                attempt.getTimeSpentSeconds(),
                attempt.getStartedAt(),
                attempt.getSubmittedAt(),
                answerDtos
        );
    }
}
