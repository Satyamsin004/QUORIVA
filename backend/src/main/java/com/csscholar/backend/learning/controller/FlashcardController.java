package com.csscholar.backend.learning.controller;

import com.csscholar.backend.auth.UserPrincipal;
import com.csscholar.backend.common.ApiResponse;
import com.csscholar.backend.learning.dto.*;
import com.csscholar.backend.learning.service.FlashcardService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/learning")
public class FlashcardController {

    private final FlashcardService flashcardService;

    public FlashcardController(FlashcardService flashcardService) {
        this.flashcardService = flashcardService;
    }

    @PostMapping("/decks")
    public ResponseEntity<ApiResponse<FlashcardDeckDto>> createManualDeck(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateManualDeckRequest request
    ) {
        FlashcardDeckDto deck = flashcardService.createManualDeck(principal.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Flashcard deck created successfully.", deck));
    }

    @PostMapping("/decks/generate")
    public ResponseEntity<ApiResponse<FlashcardDeckDto>> generateDeck(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody GenerateFlashcardsRequest request
    ) {
        FlashcardDeckDto deck = flashcardService.generateDeck(principal.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Flashcard deck generated successfully.", deck));
    }

    @GetMapping("/decks")
    public ResponseEntity<ApiResponse<List<FlashcardDeckDto>>> listDecks(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        List<FlashcardDeckDto> decks = flashcardService.getDecksForUser(principal.getId());
        return ResponseEntity.ok(ApiResponse.success("Flashcard decks retrieved successfully.", decks));
    }

    @GetMapping("/decks/{deckId}")
    public ResponseEntity<ApiResponse<FlashcardDeckDto>> getDeck(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID deckId
    ) {
        FlashcardDeckDto deck = flashcardService.getDeck(deckId, principal.getId());
        return ResponseEntity.ok(ApiResponse.success("Flashcard deck retrieved successfully.", deck));
    }

    @PatchMapping("/decks/{deckId}")
    public ResponseEntity<ApiResponse<FlashcardDeckDto>> updateDeck(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID deckId,
            @RequestBody UpdateDeckRequest request
    ) {
        FlashcardDeckDto updated = flashcardService.updateDeck(deckId, request, principal.getId());
        return ResponseEntity.ok(ApiResponse.success("Flashcard deck updated successfully.", updated));
    }

    @DeleteMapping("/decks/{deckId}")
    public ResponseEntity<ApiResponse<Void>> deleteDeck(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID deckId
    ) {
        flashcardService.deleteDeck(deckId, principal.getId());
        return ResponseEntity.ok(ApiResponse.success("Flashcard deck deleted successfully.", null));
    }

    @GetMapping("/decks/{deckId}/cards")
    public ResponseEntity<ApiResponse<List<FlashcardDto>>> listCards(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID deckId
    ) {
        List<FlashcardDto> cards = flashcardService.getCardsInDeck(deckId, principal.getId());
        return ResponseEntity.ok(ApiResponse.success("Flashcards retrieved successfully.", cards));
    }

    @PostMapping("/decks/{deckId}/cards")
    public ResponseEntity<ApiResponse<FlashcardDto>> addCard(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID deckId,
            @Valid @RequestBody CreateManualCardRequest request
    ) {
        FlashcardDto card = flashcardService.addCardToDeck(deckId, request, principal.getId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Flashcard added successfully.", card));
    }

    @PatchMapping("/cards/{cardId}")
    public ResponseEntity<ApiResponse<FlashcardDto>> updateCard(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID cardId,
            @RequestBody UpdateCardRequest request
    ) {
        FlashcardDto updated = flashcardService.updateCard(cardId, request, principal.getId());
        return ResponseEntity.ok(ApiResponse.success("Flashcard updated successfully.", updated));
    }

    @DeleteMapping("/cards/{cardId}")
    public ResponseEntity<ApiResponse<Void>> deleteCard(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID cardId
    ) {
        flashcardService.deleteCard(cardId, principal.getId());
        return ResponseEntity.ok(ApiResponse.success("Flashcard deleted successfully.", null));
    }

    @GetMapping("/reviews/due")
    public ResponseEntity<ApiResponse<List<DueCardDto>>> getDueCards(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) UUID deckId
    ) {
        List<DueCardDto> dueCards = flashcardService.getDueCards(principal.getId(), deckId);
        return ResponseEntity.ok(ApiResponse.success("Due cards retrieved successfully.", dueCards));
    }

    @PostMapping("/reviews")
    public ResponseEntity<ApiResponse<ReviewSummaryDto>> recordReview(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody RecordReviewRequest request
    ) {
        ReviewSummaryDto summary = flashcardService.recordReview(principal.getId(), request);
        return ResponseEntity.ok(ApiResponse.success("Review recorded successfully.", summary));
    }
}
