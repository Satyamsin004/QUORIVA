package com.csscholar.backend.learning.service;

import com.csscholar.backend.common.BadRequestException;
import com.csscholar.backend.common.ForbiddenException;
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
import com.csscholar.backend.learning.dto.internal.GenerateFlashcardsInternalRequest;
import com.csscholar.backend.learning.dto.internal.GenerateFlashcardsInternalResponse;
import com.csscholar.backend.learning.dto.internal.GeneratedFlashcardDto;
import com.csscholar.backend.learning.entity.*;
import com.csscholar.backend.learning.enums.LearningSourceType;
import com.csscholar.backend.learning.enums.ReviewState;
import com.csscholar.backend.learning.enums.SessionType;
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
public class FlashcardService {

    private static final Logger log = LoggerFactory.getLogger(FlashcardService.class);

    private final FlashcardDeckRepository deckRepository;
    private final FlashcardRepository cardRepository;
    private final FlashcardReviewStateRepository reviewStateRepository;
    private final FlashcardReviewHistoryRepository reviewHistoryRepository;
    private final LearningSessionRepository sessionRepository;
    private final UserRepository userRepository;
    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceService workspaceService;
    private final DocumentRepository documentRepository;
    private final DocumentChunkRepository chunkRepository;
    private final KnowledgeTopicRepository topicRepository;
    private final LibraryResourceRepository libraryResourceRepository;
    private final SpacedRepetitionService spacedRepetitionService;
    private final LearningClient learningClient;

    public FlashcardService(FlashcardDeckRepository deckRepository,
                            FlashcardRepository cardRepository,
                            FlashcardReviewStateRepository reviewStateRepository,
                            FlashcardReviewHistoryRepository reviewHistoryRepository,
                            LearningSessionRepository sessionRepository,
                            UserRepository userRepository,
                            WorkspaceRepository workspaceRepository,
                            WorkspaceService workspaceService,
                            DocumentRepository documentRepository,
                            DocumentChunkRepository chunkRepository,
                            KnowledgeTopicRepository topicRepository,
                            LibraryResourceRepository libraryResourceRepository,
                            SpacedRepetitionService spacedRepetitionService,
                            LearningClient learningClient) {
        this.deckRepository = deckRepository;
        this.cardRepository = cardRepository;
        this.reviewStateRepository = reviewStateRepository;
        this.reviewHistoryRepository = reviewHistoryRepository;
        this.sessionRepository = sessionRepository;
        this.userRepository = userRepository;
        this.workspaceRepository = workspaceRepository;
        this.workspaceService = workspaceService;
        this.documentRepository = documentRepository;
        this.chunkRepository = chunkRepository;
        this.topicRepository = topicRepository;
        this.libraryResourceRepository = libraryResourceRepository;
        this.spacedRepetitionService = spacedRepetitionService;
        this.learningClient = learningClient;
    }

    /**
     * Create an empty manual flashcard deck.
     */
    @Transactional
    public FlashcardDeckDto createManualDeck(UUID userId, CreateManualDeckRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        Workspace workspace = null;
        if (request.getWorkspaceId() != null) {
            workspaceService.getWorkspaceById(userId, request.getWorkspaceId());
            workspace = workspaceRepository.findById(request.getWorkspaceId()).orElse(null);
        }

        FlashcardDeck deck = new FlashcardDeck(
                user,
                workspace,
                request.getTitle().trim(),
                request.getDescription(),
                request.getSubjectName(),
                null,
                request.getTopicName(),
                LearningSourceType.MANUAL,
                null
        );

        FlashcardDeck saved = deckRepository.save(deck);
        return mapToDeckDto(saved, 0);
    }

    /**
     * Generate an AI-powered flashcard deck based on selected academic context.
     */
    @Transactional
    public FlashcardDeckDto generateDeck(UUID userId, GenerateFlashcardsRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        log.info("Generating flashcard deck for user: {}, count: {}", userId, request.getCardCount());

        String sourceTitle;
        String contextText;
        LearningSourceType sourceType;
        String sourceId = null;
        Workspace workspace = null;
        KnowledgeTopic topic = null;
        String subjectName = null;
        String topicName = null;

        // 0. Fallback to default user workspace if none specified
        if (request.getWorkspaceId() == null && request.getTopicId() == null && (request.getTopicSlug() == null || request.getTopicSlug().isBlank()) && request.getLibraryResourceId() == null) {
            List<Workspace> userWorkspaces = workspaceRepository.findByOwnerId(userId);
            if (!userWorkspaces.isEmpty()) {
                request.setWorkspaceId(userWorkspaces.get(0).getId());
            }
        }

        if (request.getWorkspaceId() != null) {
            workspaceService.getWorkspaceById(userId, request.getWorkspaceId());
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
                    sourceType = LearningSourceType.MANUAL;
                    sourceTitle = request.getConceptFocus();
                    sourceId = workspace.getId().toString();
                    contextText = "Academic Concept Focus: " + request.getConceptFocus() + "\n" +
                            "Generate atomic, high-impact active recall flashcards covering core definitions, formulas, and principles of " + request.getConceptFocus() + ".";
                    topicName = request.getConceptFocus();
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
                        if (sb.length() > 25000) break;
                    }
                    if (sb.length() > 25000) break;
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
                topicName = request.getConceptFocus() != null ? request.getConceptFocus() : sourceTitle;
            }

        } else if (request.getTopicId() != null || (request.getTopicSlug() != null && !request.getTopicSlug().isBlank())) {
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
            subjectName = topic.getSubject() != null ? topic.getSubject().getName() : null;
            topicName = topic.getName();

            StringBuilder sb = new StringBuilder();
            sb.append("Topic: ").append(topic.getName()).append("\n");
            sb.append("Summary: ").append(topic.getSummary()).append("\n\n");
            if (topic.getDescription() != null) {
                sb.append("Detailed Overview: ").append(topic.getDescription()).append("\n\n");
            }
            if (topic.getLearningObjectives() != null) {
                sb.append("Learning Objectives:\n");
                for (KnowledgeTopicLearningObjective obj : topic.getLearningObjectives()) {
                    sb.append("- ").append(obj.getObjective()).append("\n");
                }
            }
            contextText = sb.toString();

        } else if (request.getLibraryResourceId() != null) {
            LibraryResource resource = libraryResourceRepository.findById(request.getLibraryResourceId())
                    .orElseThrow(() -> new ResourceNotFoundException("Library resource not found"));
            if (resource.getStatus() != LibraryResourceStatus.PUBLISHED) {
                throw new BadRequestException("Only PUBLISHED library resources can be used for flashcard generation.");
            }
            sourceType = LearningSourceType.LIBRARY_RESOURCE;
            sourceTitle = resource.getTitle();
            sourceId = resource.getId().toString();
            subjectName = resource.getCategory().name();
            topicName = resource.getTitle();

            StringBuilder sb = new StringBuilder();
            sb.append("Library Resource: ").append(resource.getTitle()).append("\n");
            sb.append("Category: ").append(resource.getCategory().name()).append("\n");
            sb.append("Description: ").append(resource.getDescription()).append("\n");
            contextText = sb.toString();

        } else {
            throw new BadRequestException("A valid context source (workspace document, knowledge topic, or library resource) must be selected.");
        }

        GenerateFlashcardsInternalRequest internalRequest = new GenerateFlashcardsInternalRequest(
                userId,
                sourceType.name(),
                sourceTitle,
                contextText,
                request.getCardCount(),
                request.getConceptFocus()
        );

        GenerateFlashcardsInternalResponse aiResponse = learningClient.generateFlashcards(internalRequest);

        FlashcardDeck deck = new FlashcardDeck(
                user,
                workspace,
                aiResponse.getTitle() != null ? aiResponse.getTitle() : sourceTitle + " Flashcards",
                "Active recall deck generated from " + sourceTitle,
                subjectName,
                topic,
                topicName,
                sourceType,
                sourceId
        );

        FlashcardDeck savedDeck = deckRepository.save(deck);

        Instant now = Instant.now();
        for (GeneratedFlashcardDto cDto : aiResponse.getCards()) {
            Flashcard card = new Flashcard(
                    savedDeck,
                    user,
                    cDto.getFront(),
                    cDto.getBack(),
                    cDto.getTopicName(),
                    cDto.getSourceReference(),
                    cDto.getPageNumber(),
                    cDto.getSlideNumber()
            );
            Flashcard savedCard = cardRepository.save(card);

            // Initialize SM-2 review state
            FlashcardReviewState reviewState = new FlashcardReviewState(savedCard, user);
            reviewState.setNextDueAt(now);
            reviewStateRepository.save(reviewState);

            savedDeck.addCard(savedCard);
        }

        savedDeck.setTotalCards(savedDeck.getCards().size());
        FlashcardDeck finalDeck = deckRepository.save(savedDeck);

        return mapToDeckDto(finalDeck, finalDeck.getTotalCards());
    }

    /**
     * List all flashcard decks owned by user with real-time due counts.
     */
    @Transactional(readOnly = true)
    public List<FlashcardDeckDto> getDecksForUser(UUID userId) {
        Instant now = Instant.now();
        List<FlashcardDeck> decks = deckRepository.findByUserIdOrderByUpdatedAtDesc(userId);

        return decks.stream()
                .map(d -> {
                    long dueCount = reviewStateRepository.findDueCardsForUserAndDeck(userId, d.getId(), now).size();
                    return mapToDeckDto(d, (int) dueCount);
                })
                .collect(Collectors.toList());
    }

    /**
     * Get deck details by ID.
     */
    @Transactional(readOnly = true)
    public FlashcardDeckDto getDeck(UUID deckId, UUID userId) {
        FlashcardDeck deck = deckRepository.findByIdAndUserId(deckId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Flashcard deck not found with id: " + deckId));
        int dueCount = reviewStateRepository.findDueCardsForUserAndDeck(userId, deck.getId(), Instant.now()).size();
        return mapToDeckDto(deck, dueCount);
    }

    /**
     * Update deck metadata.
     */
    @Transactional
    public FlashcardDeckDto updateDeck(UUID deckId, UpdateDeckRequest request, UUID userId) {
        FlashcardDeck deck = deckRepository.findByIdAndUserId(deckId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Flashcard deck not found with id: " + deckId));

        if (request.getTitle() != null && !request.getTitle().isBlank()) {
            deck.setTitle(request.getTitle().trim());
        }
        if (request.getDescription() != null) {
            deck.setDescription(request.getDescription().trim());
        }
        if (request.getSubjectName() != null) {
            deck.setSubjectName(request.getSubjectName().trim());
        }
        if (request.getTopicName() != null) {
            deck.setTopicName(request.getTopicName().trim());
        }

        FlashcardDeck updated = deckRepository.save(deck);
        int dueCount = reviewStateRepository.findDueCardsForUserAndDeck(userId, updated.getId(), Instant.now()).size();
        return mapToDeckDto(updated, dueCount);
    }

    /**
     * Delete deck and all cards.
     */
    @Transactional
    public void deleteDeck(UUID deckId, UUID userId) {
        FlashcardDeck deck = deckRepository.findByIdAndUserId(deckId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Flashcard deck not found with id: " + deckId));
        deckRepository.delete(deck);
    }

    /**
     * List all cards in a deck.
     */
    @Transactional(readOnly = true)
    public List<FlashcardDto> getCardsInDeck(UUID deckId, UUID userId) {
        FlashcardDeck deck = deckRepository.findByIdAndUserId(deckId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Flashcard deck not found with id: " + deckId));

        List<Flashcard> cards = cardRepository.findByDeckIdOrderByCreatedAtAsc(deck.getId());
        return cards.stream().map(this::mapToCardDto).collect(Collectors.toList());
    }

    /**
     * Add a card to an existing deck manually.
     */
    @Transactional
    public FlashcardDto addCardToDeck(UUID deckId, CreateManualCardRequest request, UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        FlashcardDeck deck = deckRepository.findByIdAndUserId(deckId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Flashcard deck not found with id: " + deckId));

        Flashcard card = new Flashcard(
                deck,
                user,
                request.getFront().trim(),
                request.getBack().trim(),
                request.getTopicName(),
                request.getSourceReference(),
                request.getPageNumber(),
                request.getSlideNumber()
        );

        Flashcard savedCard = cardRepository.save(card);

        FlashcardReviewState state = new FlashcardReviewState(savedCard, user);
        state.setNextDueAt(Instant.now());
        reviewStateRepository.save(state);

        deck.setTotalCards((int) cardRepository.countByDeckId(deck.getId()));
        deckRepository.save(deck);

        return mapToCardDto(savedCard);
    }

    /**
     * Update an individual card.
     */
    @Transactional
    public FlashcardDto updateCard(UUID cardId, UpdateCardRequest request, UUID userId) {
        Flashcard card = cardRepository.findByIdAndUserId(cardId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Flashcard not found with id: " + cardId));

        if (request.getFront() != null && !request.getFront().isBlank()) {
            card.setFront(request.getFront().trim());
        }
        if (request.getBack() != null && !request.getBack().isBlank()) {
            card.setBack(request.getBack().trim());
        }
        if (request.getTopicName() != null) {
            card.setTopicName(request.getTopicName().trim());
        }
        if (request.getSourceReference() != null) {
            card.setSourceReference(request.getSourceReference().trim());
        }
        if (request.getPageNumber() != null) {
            card.setPageNumber(request.getPageNumber());
        }
        if (request.getSlideNumber() != null) {
            card.setSlideNumber(request.getSlideNumber());
        }

        Flashcard updated = cardRepository.save(card);
        return mapToCardDto(updated);
    }

    /**
     * Delete an individual card.
     */
    @Transactional
    public void deleteCard(UUID cardId, UUID userId) {
        Flashcard card = cardRepository.findByIdAndUserId(cardId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Flashcard not found with id: " + cardId));
        FlashcardDeck deck = card.getDeck();
        cardRepository.delete(card);

        deck.setTotalCards((int) cardRepository.countByDeckId(deck.getId()));
        deckRepository.save(deck);
    }

    /**
     * Retrieve all cards currently due for review for the user.
     */
    @Transactional(readOnly = true)
    public List<DueCardDto> getDueCards(UUID userId, UUID optionalDeckId) {
        Instant now = Instant.now();
        List<FlashcardReviewState> dueStates;

        if (optionalDeckId != null) {
            dueStates = reviewStateRepository.findDueCardsForUserAndDeck(userId, optionalDeckId, now);
        } else {
            dueStates = reviewStateRepository.findDueCardsForUser(userId, now);
        }

        return dueStates.stream()
                .map(s -> {
                    Flashcard c = s.getCard();
                    return new DueCardDto(
                            c.getId(),
                            c.getDeck().getId(),
                            c.getDeck().getTitle(),
                            c.getFront(),
                            c.getBack(),
                            c.getTopicName(),
                            c.getSourceReference(),
                            s.getState(),
                            s.getIntervalDays(),
                            s.getEaseFactor(),
                            s.getRepetitionCount()
                    );
                })
                .collect(Collectors.toList());
    }

    /**
     * Record review feedback and update SM-2 scheduling parameters.
     */
    @Transactional
    public ReviewSummaryDto recordReview(UUID userId, RecordReviewRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        Flashcard card = cardRepository.findByIdAndUserId(request.getCardId(), userId)
                .orElseThrow(() -> new ResourceNotFoundException("Flashcard not found with id: " + request.getCardId()));

        FlashcardReviewState currentState = reviewStateRepository.findByCardIdAndUserId(card.getId(), userId)
                .orElseGet(() -> new FlashcardReviewState(card, user));

        Instant now = Instant.now();
        SpacedRepetitionService.ReviewCalculationResult calc = spacedRepetitionService.calculate(
                currentState,
                request.getRating(),
                now
        );

        // Update Review State
        currentState.setRepetitionCount(calc.getRepetitionCount());
        currentState.setEaseFactor(calc.getEaseFactor());
        currentState.setIntervalDays(calc.getIntervalDays());
        currentState.setNextDueAt(calc.getNextDueAt());
        currentState.setState(calc.getNewState());
        currentState.setLastRating(request.getRating());
        currentState.setLastReviewedAt(now);
        reviewStateRepository.save(currentState);

        // Append to Review History
        FlashcardReviewHistory history = new FlashcardReviewHistory(
                card,
                card.getDeck(),
                user,
                request.getRating(),
                request.getReviewTimeSeconds() != null ? request.getReviewTimeSeconds() : 0,
                calc.getIntervalDays(),
                calc.getEaseFactor()
        );
        reviewHistoryRepository.save(history);

        // Record Learning Session for aggregate stats
        LearningSession session = new LearningSession(
                user,
                SessionType.FLASHCARD_REVIEW,
                card.getDeck().getId(),
                request.getReviewTimeSeconds() != null ? request.getReviewTimeSeconds() : 0,
                1,
                request.getRating() == com.csscholar.backend.learning.enums.ReviewRating.AGAIN ? 0.0 : 100.0
        );
        sessionRepository.save(session);

        return new ReviewSummaryDto(
                card.getId(),
                request.getRating(),
                calc.getIntervalDays(),
                calc.getNextDueAt(),
                calc.getEaseFactor(),
                calc.getRepetitionCount(),
                calc.getNewState()
        );
    }

    private FlashcardDeckDto mapToDeckDto(FlashcardDeck deck, int dueCount) {
        return new FlashcardDeckDto(
                deck.getId(),
                deck.getTitle(),
                deck.getDescription(),
                deck.getSubjectName(),
                deck.getTopicName(),
                deck.getSourceType(),
                deck.getTotalCards(),
                dueCount,
                deck.getCreatedAt(),
                deck.getUpdatedAt()
        );
    }

    private FlashcardDto mapToCardDto(Flashcard card) {
        FlashcardReviewState s = reviewStateRepository.findByCardId(card.getId()).orElse(null);
        return new FlashcardDto(
                card.getId(),
                card.getDeck().getId(),
                card.getFront(),
                card.getBack(),
                card.getTopicName(),
                card.getSourceReference(),
                card.getPageNumber(),
                card.getSlideNumber(),
                s != null ? s.getState() : ReviewState.NEW,
                s != null ? s.getRepetitionCount() : 0,
                s != null ? s.getEaseFactor() : 2.5,
                s != null ? s.getIntervalDays() : 0,
                s != null ? s.getNextDueAt() : card.getCreatedAt(),
                s != null ? s.getLastReviewedAt() : null,
                card.getCreatedAt()
        );
    }
}
