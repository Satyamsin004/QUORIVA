package com.csscholar.backend.learning.controller;

import com.csscholar.backend.auth.UserPrincipal;
import com.csscholar.backend.common.ApiResponse;
import com.csscholar.backend.learning.dto.*;
import com.csscholar.backend.learning.service.QuizService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/learning")
public class QuizController {

    private final QuizService quizService;

    public QuizController(QuizService quizService) {
        this.quizService = quizService;
    }

    @PostMapping("/quizzes/generate")
    public ResponseEntity<ApiResponse<QuizSummaryDto>> generateQuiz(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody GenerateQuizRequest request
    ) {
        QuizSummaryDto quiz = quizService.generateQuiz(principal.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Quiz generated successfully.", quiz));
    }

    @GetMapping("/quizzes")
    public ResponseEntity<ApiResponse<List<QuizSummaryDto>>> listQuizzes(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        List<QuizSummaryDto> quizzes = quizService.getQuizzesForUser(principal.getId());
        return ResponseEntity.ok(ApiResponse.success("Quizzes retrieved successfully.", quizzes));
    }

    @GetMapping({"/quizzes/{quizId}", "/quizzes/{quizId}/practice"})
    public ResponseEntity<ApiResponse<QuizPracticeDto>> getQuizPractice(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID quizId
    ) {
        QuizPracticeDto quiz = quizService.getQuizPractice(quizId, principal.getId());
        return ResponseEntity.ok(ApiResponse.success("Quiz practice view retrieved successfully.", quiz));
    }

    @DeleteMapping("/quizzes/{quizId}")
    public ResponseEntity<ApiResponse<Void>> deleteQuiz(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID quizId
    ) {
        quizService.deleteQuiz(quizId, principal.getId());
        return ResponseEntity.ok(ApiResponse.success("Quiz deleted successfully.", null));
    }

    @PostMapping("/quizzes/{quizId}/attempts")
    public ResponseEntity<ApiResponse<QuizAttemptResponseDto>> startAttempt(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID quizId
    ) {
        QuizAttemptResponseDto attempt = quizService.startAttempt(quizId, principal.getId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Quiz attempt started.", attempt));
    }

    @GetMapping("/attempts/{attemptId}")
    public ResponseEntity<ApiResponse<QuizAttemptResponseDto>> getAttempt(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID attemptId
    ) {
        QuizAttemptResponseDto attempt = quizService.getAttempt(attemptId, principal.getId());
        return ResponseEntity.ok(ApiResponse.success("Quiz attempt retrieved.", attempt));
    }

    @PutMapping("/attempts/{attemptId}/answers")
    public ResponseEntity<ApiResponse<QuizAttemptResponseDto>> saveAnswers(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID attemptId,
            @Valid @RequestBody SaveAnswersRequest request
    ) {
        QuizAttemptResponseDto attempt = quizService.saveAnswers(attemptId, request, principal.getId());
        return ResponseEntity.ok(ApiResponse.success("Answers saved.", attempt));
    }

    @PostMapping("/attempts/{attemptId}/submit")
    public ResponseEntity<ApiResponse<QuizAttemptResponseDto>> submitAttempt(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID attemptId,
            @RequestBody(required = false) SaveAnswersRequest request
    ) {
        QuizAttemptResponseDto attempt = quizService.submitAttempt(attemptId, request, principal.getId());
        return ResponseEntity.ok(ApiResponse.success("Quiz attempt submitted successfully.", attempt));
    }

    @GetMapping("/attempts/{attemptId}/review")
    public ResponseEntity<ApiResponse<QuizAttemptReviewDto>> getAttemptReview(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID attemptId
    ) {
        QuizAttemptReviewDto review = quizService.getAttemptReview(attemptId, principal.getId());
        return ResponseEntity.ok(ApiResponse.success("Quiz attempt review retrieved.", review));
    }
}
