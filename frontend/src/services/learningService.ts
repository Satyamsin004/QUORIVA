import { apiClient } from './apiClient';
import {
  ApiResponse,
  QuizSummary,
  QuizPractice,
  QuizAttemptResponse,
  QuizAttemptReview,
  FlashcardDeck,
  Flashcard,
  DueCard,
  ReviewSummary,
  LearningAnalyticsOverview,
  TopicPerformance,
  DailyActivity,
  GenerateQuizRequest,
  GenerateFlashcardsRequest,
  CreateManualDeckRequest,
  UpdateDeckRequest,
  CreateManualCardRequest,
  UpdateCardRequest,
  SaveAnswersRequest,
  RecordReviewRequest,
} from '@/types';

export const learningService = {
  // Quizzes
  async generateQuiz(data: GenerateQuizRequest): Promise<QuizSummary> {
    const res = await apiClient.post<ApiResponse<QuizSummary>>('/learning/quizzes/generate', data, {
      timeout: 120000,
    });
    return res.data.data;
  },

  async listQuizzes(): Promise<QuizSummary[]> {
    const res = await apiClient.get<ApiResponse<QuizSummary[]>>('/learning/quizzes');
    return res.data.data;
  },

  async getQuizPractice(quizId: string): Promise<QuizPractice> {
    const res = await apiClient.get<ApiResponse<QuizPractice>>(`/learning/quizzes/${quizId}/practice`);
    return res.data.data;
  },

  async deleteQuiz(quizId: string): Promise<void> {
    await apiClient.delete(`/learning/quizzes/${quizId}`);
  },

  // Attempts
  async startAttempt(quizId: string): Promise<QuizAttemptResponse> {
    const res = await apiClient.post<ApiResponse<QuizAttemptResponse>>(`/learning/quizzes/${quizId}/attempts`);
    return res.data.data;
  },

  async getAttempt(attemptId: string): Promise<QuizAttemptResponse> {
    const res = await apiClient.get<ApiResponse<QuizAttemptResponse>>(`/learning/attempts/${attemptId}`);
    return res.data.data;
  },

  async saveAnswers(attemptId: string, data: SaveAnswersRequest): Promise<QuizAttemptResponse> {
    const res = await apiClient.put<ApiResponse<QuizAttemptResponse>>(`/learning/attempts/${attemptId}/answers`, data);
    return res.data.data;
  },

  async submitAttempt(attemptId: string, data?: SaveAnswersRequest): Promise<QuizAttemptResponse> {
    const res = await apiClient.post<ApiResponse<QuizAttemptResponse>>(`/learning/attempts/${attemptId}/submit`, data || {});
    return res.data.data;
  },

  async getAttemptReview(attemptId: string): Promise<QuizAttemptReview> {
    const res = await apiClient.get<ApiResponse<QuizAttemptReview>>(`/learning/attempts/${attemptId}/review`);
    return res.data.data;
  },

  async listQuizAttempts(quizId: string): Promise<QuizAttemptResponse[]> {
    const res = await apiClient.get<ApiResponse<QuizAttemptResponse[]>>(`/learning/quizzes/${quizId}/attempts`);
    return res.data.data;
  },

  // Decks & Flashcards
  async createManualDeck(data: CreateManualDeckRequest): Promise<FlashcardDeck> {
    const res = await apiClient.post<ApiResponse<FlashcardDeck>>('/learning/decks/manual', data);
    return res.data.data;
  },

  async generateDeck(data: GenerateFlashcardsRequest): Promise<FlashcardDeck> {
    const res = await apiClient.post<ApiResponse<FlashcardDeck>>('/learning/decks/generate', data, {
      timeout: 120000,
    });
    return res.data.data;
  },

  async listDecks(): Promise<FlashcardDeck[]> {
    const res = await apiClient.get<ApiResponse<FlashcardDeck[]>>('/learning/decks');
    return res.data.data;
  },

  async getDeck(deckId: string): Promise<FlashcardDeck> {
    const res = await apiClient.get<ApiResponse<FlashcardDeck>>(`/learning/decks/${deckId}`);
    return res.data.data;
  },

  async updateDeck(deckId: string, data: UpdateDeckRequest): Promise<FlashcardDeck> {
    const res = await apiClient.put<ApiResponse<FlashcardDeck>>(`/learning/decks/${deckId}`, data);
    return res.data.data;
  },

  async deleteDeck(deckId: string): Promise<void> {
    await apiClient.delete(`/learning/decks/${deckId}`);
  },

  async listCards(deckId: string): Promise<Flashcard[]> {
    const res = await apiClient.get<ApiResponse<Flashcard[]>>(`/learning/decks/${deckId}/cards`);
    return res.data.data;
  },

  async createCard(deckId: string, data: CreateManualCardRequest): Promise<Flashcard> {
    const res = await apiClient.post<ApiResponse<Flashcard>>(`/learning/decks/${deckId}/cards`, data);
    return res.data.data;
  },

  async updateCard(cardId: string, data: UpdateCardRequest): Promise<Flashcard> {
    const res = await apiClient.put<ApiResponse<Flashcard>>(`/learning/cards/${cardId}`, data);
    return res.data.data;
  },

  async deleteCard(cardId: string): Promise<void> {
    await apiClient.delete(`/learning/cards/${cardId}`);
  },

  async getDueCards(deckId?: string): Promise<DueCard[]> {
    const params = deckId ? { deckId } : {};
    const res = await apiClient.get<ApiResponse<DueCard[]>>('/learning/reviews/due', { params });
    return res.data.data;
  },

  async recordReview(data: RecordReviewRequest): Promise<ReviewSummary> {
    const res = await apiClient.post<ApiResponse<ReviewSummary>>('/learning/reviews', data);
    return res.data.data;
  },

  // Analytics
  async getAnalyticsOverview(): Promise<LearningAnalyticsOverview> {
    const res = await apiClient.get<ApiResponse<LearningAnalyticsOverview>>('/learning/analytics/overview');
    return res.data.data;
  },

  async getTopicPerformance(): Promise<TopicPerformance[]> {
    const res = await apiClient.get<ApiResponse<TopicPerformance[]>>('/learning/analytics/topics');
    return res.data.data;
  },

  async getActivityHistory(days: number = 14): Promise<DailyActivity[]> {
    const res = await apiClient.get<ApiResponse<DailyActivity[]>>('/learning/analytics/activity', {
      params: { days },
    });
    return res.data.data;
  },
};

