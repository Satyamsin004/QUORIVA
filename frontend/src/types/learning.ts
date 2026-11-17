export type LearningDifficultyLevel = 'EASY' | 'MEDIUM' | 'HARD';
export type QuestionType = 'MULTIPLE_CHOICE' | 'TRUE_FALSE';
export type AttemptStatus = 'IN_PROGRESS' | 'SUBMITTED' | 'ABANDONED';
export type ReviewRating = 'AGAIN' | 'HARD' | 'GOOD' | 'EASY';
export type ReviewState = 'NEW' | 'LEARNING' | 'REVIEW' | 'GRADUATED';
export type LearningSourceType = 'WORKSPACE_DOCUMENTS' | 'KNOWLEDGE_TOPIC' | 'LIBRARY_RESOURCE' | 'CUSTOM' | 'MANUAL' | 'TARGETED_REVISION';
export type SessionType = 'QUIZ_ATTEMPT' | 'FLASHCARD_REVIEW';

export interface TargetMaterialRequest {
  documentId: string;
  startPage?: number;
  endPage?: number;
}

export interface TargetMaterialSummary {
  documentId: string;
  title: string;
  startPage: number;
  endPage: number;
  pageCount: number;
}

export interface SubjectMaterial {
  documentId: string;
  title: string;
  originalFilename: string;
  fileType: string;
  fileSize: number;
  pageCount: number;
  subject: string;
  processingStatus: string;
  uploadedAt: string;
}

// Quiz Practice Types (Answers strictly omitted for security)
export interface QuizOptionPractice {
  id?: string;
  optionId?: string;
  optionIndex: number;
  optionText: string;
}

export interface QuizQuestionPractice {
  id?: string;
  questionId?: string;
  questionIndex: number;
  questionText: string;
  questionType: QuestionType;
  difficultyLevel?: LearningDifficultyLevel;
  sourceReference?: string;
  pageNumber?: number;
  slideNumber?: number;
  topicName?: string;
  options: QuizOptionPractice[];
}

export interface QuizPractice {
  id?: string;
  quizId?: string;
  title: string;
  description?: string;
  sourceType?: LearningSourceType;
  sourceId?: string;
  subjectName?: string;
  topicName?: string;
  difficulty?: LearningDifficultyLevel;
  difficultyLevel?: LearningDifficultyLevel;
  totalQuestions: number;
  timeLimitMinutes?: number;
  questions: QuizQuestionPractice[];
}

export interface QuizSummary {
  id: string;
  title: string;
  description?: string;
  sourceType: LearningSourceType;
  subjectName?: string;
  targetMaterials?: string;
  topicName?: string;
  difficulty: LearningDifficultyLevel;
  totalQuestions: number;
  createdAt: string;
  bestScore?: number;
  totalAttempts: number;
}

export interface QuizAttemptAnswerItem {
  questionId: string;
  selectedOptionId?: string;
  selectedText?: string;
  markedForReview?: boolean;
}

export interface QuizAttemptResponse {
  id?: string;
  attemptId?: string;
  quizId: string;
  quizTitle?: string;
  status: AttemptStatus;
  score?: number;
  maxScore?: number;
  percentage?: number;
  totalQuestions?: number;
  correctAnswersCount?: number;
  startedAt: string;
  submittedAt?: string;
  durationSeconds?: number;
  timeSpentSeconds?: number;
  selectedAnswers?: Record<string, number>;
}

// Quiz Review Types (Answers and explanations revealed after submission)
export interface QuizOptionReview {
  id?: string;
  optionId?: string;
  optionIndex: number;
  optionText: string;
  isCorrect: boolean;
}

export interface QuizQuestionReview {
  id?: string;
  questionId?: string;
  questionIndex: number;
  questionText: string;
  questionType: QuestionType;
  explanation?: string;
  sourceReference?: string;
  pageNumber?: number;
  slideNumber?: number;
  topicName?: string;
  selectedOptionIndex?: number;
  correctOptionIndex?: number;
  selectedOptionId?: string;
  correctOptionId?: string;
  isCorrect?: boolean;
  isUserCorrect?: boolean;
  options: QuizOptionReview[];
}

export interface QuizAttemptReview {
  id?: string;
  attemptId: string;
  quizId: string;
  quizTitle?: string;
  status?: AttemptStatus;
  score: number;
  maxScore?: number;
  percentage?: number;
  totalQuestions?: number;
  correctAnswersCount?: number;
  timeSpentSeconds?: number;
  startedAt: string;
  submittedAt?: string;
  durationSeconds?: number;
  questions: QuizQuestionReview[];
}

// Flashcard Types
export interface FlashcardDeck {
  id: string;
  title: string;
  description?: string;
  subjectName?: string;
  topicName?: string;
  sourceType: LearningSourceType;
  totalCards: number;
  dueCardsCount: number;
  createdAt: string;
  updatedAt: string;
}

export interface Flashcard {
  id: string;
  deckId: string;
  front: string;
  back: string;
  question?: string;
  answer?: string;
  sourceCitation?: string;
  sourceReference?: string;
  pageNumber?: number;
  slideNumber?: number;
  state?: ReviewState;
  reviewState?: ReviewState;
  repetitionCount: number;
  intervalDays: number;
  nextDueAt: string;
  lastRating?: ReviewRating;
  lastReviewedAt?: string;
}

export interface DueCard {
  id?: string;
  cardId?: string;
  deckId: string;
  deckTitle: string;
  front: string;
  back: string;
  question?: string;
  answer?: string;
  sourceCitation?: string;
  sourceReference?: string;
  state?: ReviewState;
  reviewState?: ReviewState;
  repetitionCount: number;
  intervalDays: number;
}

export interface ReviewSummary {
  cardId: string;
  rating: ReviewRating;
  scheduledDays?: number;
  intervalDays?: number;
  nextDueAt: string;
  easeFactor: number;
  repetitionCount: number;
  state?: ReviewState;
  newState?: ReviewState;
}

// Analytics Types
export interface LearningAnalyticsOverview {
  totalQuizAttempts: number;
  totalQuizzesTaken?: number;
  averageQuizScore: number;
  totalDecks: number;
  totalFlashcardDecks?: number;
  totalCards: number;
  totalFlashcards?: number;
  cardsDueToday: number;
  cardsReviewedCount: number;
  cardsReviewedToday?: number;
  cardsReviewedTotal?: number;
  cardsInNewState?: number;
  cardsInLearningState?: number;
  cardsInReviewState?: number;
  cardsInGraduatedState?: number;
  recentQuizAttempts?: any[];
  recentSessions?: any[];
  newCardsCount?: number;
  learningCardsCount?: number;
  reviewCardsCount?: number;
  graduatedCardsCount?: number;
  totalLearningTimeSeconds?: number;
}

export interface TopicPerformance {
  topicName: string;
  totalQuizzes: number;
  averageScore: number;
  totalCards: number;
  cardsMastered: number;
}

export interface DailyActivity {
  date: string;
  quizzesTaken: number;
  cardsReviewed: number;
  studyTimeMinutes: number;
  quizAttempts?: number;
  flashcardReviews?: number;
  totalMinutes?: number;
}

// Requests
export interface GenerateQuizRequest {
  title?: string;
  sourceType: LearningSourceType;
  sourceWorkspaceId?: string;
  workspaceId?: string;
  sourceDocumentIds?: string[];
  documentIds?: string[];
  conceptFocus?: string;
  topicId?: string;
  resourceId?: string;
  subjectName?: string;
  topicName?: string;
  difficulty?: LearningDifficultyLevel;
  difficultyLevel?: LearningDifficultyLevel;
  questionCount?: number;
  targetMaterials?: TargetMaterialRequest[];
}

export interface GenerateFlashcardsRequest {
  deckTitle?: string;
  sourceType: LearningSourceType;
  sourceWorkspaceId?: string;
  workspaceId?: string;
  sourceDocumentIds?: string[];
  documentIds?: string[];
  conceptFocus?: string;
  topicId?: string;
  resourceId?: string;
  subjectName?: string;
  topicName?: string;
  cardCount?: number;
}

export interface CreateManualDeckRequest {
  title: string;
  description?: string;
  subjectName?: string;
  topicName?: string;
}

export interface UpdateDeckRequest {
  title?: string;
  description?: string;
  subjectName?: string;
  topicName?: string;
}

export interface CreateManualCardRequest {
  question: string;
  answer: string;
  sourceCitation?: string;
}

export interface UpdateCardRequest {
  question?: string;
  answer?: string;
  sourceCitation?: string;
}

export interface SaveAnswersRequest {
  answers: QuizAttemptAnswerItem[];
  timeSpentSeconds?: number;
}

export interface RecordReviewRequest {
  cardId: string;
  rating: ReviewRating;
  reviewTimeSeconds?: number;
}
