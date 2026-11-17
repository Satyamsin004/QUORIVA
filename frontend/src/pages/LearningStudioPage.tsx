import React, { useState, useEffect } from 'react';
import {
  GraduationCap,
  Sparkles,
  BrainCircuit,
  Layers,
  Clock,
  CheckCircle2,
  XCircle,
  RotateCw,
  Plus,
  Play,
  Award,
  BarChart3,
  Trash2,
  ArrowRight,
  ArrowLeft,
  AlertTriangle,
  Loader2,
  Check,
  Flag,
  BookmarkCheck,
} from 'lucide-react';
import { learningService } from '@/services/learningService';
import { documentService } from '@/services/documentService';
import { useWorkspace } from '@/context/WorkspaceContext';
import {
  DocumentItem,
  QuizSummary,
  QuizPractice,
  QuizAttemptResponse,
  QuizAttemptReview,
  FlashcardDeck,
  Flashcard,
  DueCard,
  LearningAnalyticsOverview,
  TopicPerformance,
  DailyActivity,
  LearningDifficultyLevel,
  LearningSourceType,
  ReviewRating,
  GenerateQuizRequest,
  TargetMaterialRequest,
  GenerateFlashcardsRequest,
  CreateManualDeckRequest,
  CreateManualCardRequest,
} from '@/types';

type StudioTab = 'dashboard' | 'quizzes' | 'flashcards' | 'analytics';

export const LearningStudioPage: React.FC = () => {
  const { currentWorkspace } = useWorkspace();

  // Navigation state
  const [activeTab, setActiveTab] = useState<StudioTab>('dashboard');

  // Loading & Error state
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [actionLoading, setActionLoading] = useState<boolean>(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  // Data states
  const [overview, setOverview] = useState<LearningAnalyticsOverview | null>(null);
  const [topicPerformance, setTopicPerformance] = useState<TopicPerformance[]>([]);
  const [activityHistory, setActivityHistory] = useState<DailyActivity[]>([]);
  const [quizzes, setQuizzes] = useState<QuizSummary[]>([]);
  const [decks, setDecks] = useState<FlashcardDeck[]>([]);

  // Workspace documents for subject-wise revision
  const [workspaceDocs, setWorkspaceDocs] = useState<DocumentItem[]>([]);
  const [selectedSubject, setSelectedSubject] = useState<string>('ALL');
  const [selectedDocIds, setSelectedDocIds] = useState<string[]>([]);
  const [docPageRanges, setDocPageRanges] = useState<Record<string, { isAllPages: boolean; startPage: number; endPage: number }>>({});

  // Active Quiz Practice State
  const [activeQuizPractice, setActiveQuizPractice] = useState<QuizPractice | null>(null);
  const [activeAttempt, setActiveAttempt] = useState<QuizAttemptResponse | null>(null);
  const [currentQuestionIndex, setCurrentQuestionIndex] = useState<number>(0);
  const [selectedAnswers, setSelectedAnswers] = useState<Record<string, { optionId: string; optionIndex: number; text: string }>>({});
  const [flaggedQuestions, setFlaggedQuestions] = useState<Set<string>>(new Set());
  const [elapsedPracticeSeconds, setElapsedPracticeSeconds] = useState<number>(0);
  const [isSubmitModalOpen, setIsSubmitModalOpen] = useState<boolean>(false);

  // Active Quiz Review State (post-submission)
  const [attemptReview, setAttemptReview] = useState<QuizAttemptReview | null>(null);

  // Active Flashcard Review State
  const [activeDeckForReview, setActiveDeckForReview] = useState<FlashcardDeck | null>(null);
  const [reviewCards, setReviewCards] = useState<DueCard[]>([]);
  const [currentCardIndex, setCurrentCardIndex] = useState<number>(0);
  const [isCardFlipped, setIsCardFlipped] = useState<boolean>(false);
  const [reviewFinished, setReviewFinished] = useState<boolean>(false);
  const [reviewSessionStats, setReviewSessionStats] = useState<{ again: number; hard: number; good: number; easy: number }>({
    again: 0,
    hard: 0,
    good: 0,
    easy: 0,
  });

  // Selected Deck drilldown
  const [selectedDeck, setSelectedDeck] = useState<FlashcardDeck | null>(null);
  const [deckCards, setDeckCards] = useState<Flashcard[]>([]);

  // Modals state
  const [isGenerateQuizModalOpen, setIsGenerateQuizModalOpen] = useState<boolean>(false);
  const [isGenerateCardsModalOpen, setIsGenerateCardsModalOpen] = useState<boolean>(false);
  const [isCreateDeckModalOpen, setIsCreateDeckModalOpen] = useState<boolean>(false);
  const [isAddCardModalOpen, setIsAddCardModalOpen] = useState<boolean>(false);

  // Form states
  const [quizForm, setQuizForm] = useState<{
    sourceType: LearningSourceType;
    conceptFocus: string;
    topicSlug: string;
    difficultyLevel: LearningDifficultyLevel;
    questionCount: number;
    title: string;
  }>({
    sourceType: 'CUSTOM',
    conceptFocus: 'Operating Systems Scheduling',
    topicSlug: '',
    difficultyLevel: 'MEDIUM',
    questionCount: 5,
    title: '',
  });

  const [cardGenForm, setCardGenForm] = useState<{
    sourceType: LearningSourceType;
    conceptFocus: string;
    topicSlug: string;
    cardCount: number;
    deckTitle: string;
  }>({
    sourceType: 'CUSTOM',
    conceptFocus: 'Algorithms and Data Structures',
    topicSlug: '',
    cardCount: 5,
    deckTitle: '',
  });

  const [newDeckForm, setNewDeckForm] = useState<CreateManualDeckRequest>({
    title: '',
    description: '',
    subjectName: 'Computer Science',
    topicName: '',
  });

  const [newCardForm, setNewCardForm] = useState<CreateManualCardRequest>({
    question: '',
    answer: '',
    sourceCitation: '',
  });

  // Load initial studio data
  const loadStudioData = async () => {
    setIsLoading(true);
    setErrorMessage(null);
    try {
      const [ovData, tpData, actData, qzData, dkData] = await Promise.all([
        learningService.getAnalyticsOverview().catch(() => null),
        learningService.getTopicPerformance().catch(() => []),
        learningService.getActivityHistory(14).catch(() => []),
        learningService.listQuizzes().catch(() => []),
        learningService.listDecks().catch(() => []),
      ]);
      setOverview(
        ovData || {
          totalQuizAttempts: 0,
          averageQuizScore: 0,
          totalDecks: 0,
          totalCards: 0,
          cardsReviewedCount: 0,
          totalFlashcardDecks: 0,
          totalFlashcards: 0,
          cardsDueToday: 0,
          cardsReviewedTotal: 0,
          cardsInNewState: 0,
          cardsInLearningState: 0,
          cardsInReviewState: 0,
          cardsInGraduatedState: 0,
          recentQuizAttempts: [],
          recentSessions: [],
        }
      );
      setTopicPerformance(tpData || []);
      setActivityHistory(actData || []);
      setQuizzes(qzData || []);
      setDecks(dkData || []);
    } catch (err: any) {
      setErrorMessage(err.response?.data?.message || err.message || 'Failed to load Learning Studio data');
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    loadStudioData();
    if (currentWorkspace?.id) {
      documentService
        .listDocuments({ workspaceId: currentWorkspace.id, size: 100 })
        .then((res) => {
          const docs = res.content || [];
          setWorkspaceDocs(docs);
          // Do not silently include materials which still have no readable content.
          setSelectedDocIds(docs.filter((d) => d.processingStatus === 'PROCESSED').map((d) => d.id));
        })
        .catch(() => setWorkspaceDocs([]));
    }
  }, [currentWorkspace?.id]);

  const availableSubjects = Array.from(
    new Set(workspaceDocs.map((d) => d.subject || 'General').filter(Boolean))
  );

  const filteredDocs =
    selectedSubject === 'ALL'
      ? workspaceDocs
      : workspaceDocs.filter((d) => (d.subject || 'General') === selectedSubject);

  const readyDocs = filteredDocs.filter((d) => d.processingStatus === 'PROCESSED');

  // Timer for active quiz practice
  useEffect(() => {
    let interval: any;
    if (activeQuizPractice && !attemptReview) {
      interval = setInterval(() => {
        setElapsedPracticeSeconds((prev) => prev + 1);
      }, 1000);
    }
    return () => clearInterval(interval);
  }, [activeQuizPractice, attemptReview]);

  // Helpers to normalize question and option IDs across backend models
  const getQId = (q: any): string => q?.id || q?.questionId || '';
  const getOptId = (opt: any): string => opt?.id || opt?.optionId || '';

  // Quiz Practice Handlers
  const handleStartQuizPractice = async (quizId: string) => {
    setActionLoading(true);
    setErrorMessage(null);
    try {
      const [practiceDto, attemptDto] = await Promise.all([
        learningService.getQuizPractice(quizId),
        learningService.startAttempt(quizId),
      ]);
      setActiveQuizPractice(practiceDto);
      setActiveAttempt(attemptDto);
      setCurrentQuestionIndex(0);
      setSelectedAnswers({});
      setFlaggedQuestions(new Set());
      setElapsedPracticeSeconds(0);
      setAttemptReview(null);
    } catch (err: any) {
      setErrorMessage(err.response?.data?.message || err.message || 'Failed to start quiz');
    } finally {
      setActionLoading(false);
    }
  };

  const handleSelectOption = (questionId: string, optionId: string, optionIndex: number, text: string) => {
    if (!questionId) return;
    setSelectedAnswers((prev) => ({
      ...prev,
      [questionId]: { optionId, optionIndex, text },
    }));
  };

  const toggleFlagQuestion = (questionId: string) => {
    if (!questionId) return;
    setFlaggedQuestions((prev) => {
      const next = new Set(prev);
      if (next.has(questionId)) next.delete(questionId);
      else next.add(questionId);
      return next;
    });
  };

  const handleSubmitQuiz = async () => {
    if (!activeAttempt || !activeQuizPractice) return;
    const attemptId = (activeAttempt as any).id || activeAttempt.attemptId;
    if (!attemptId) return;

    setActionLoading(true);
    setErrorMessage(null);
    try {
      const formattedAnswers = Object.entries(selectedAnswers)
        .filter(([qId]) => qId && qId !== 'undefined' && qId !== 'null' && qId.trim() !== '')
        .map(([qId, ans]) => ({
          questionId: qId,
          selectedOptionId: ans.optionId && ans.optionId !== 'undefined' && ans.optionId !== 'null' && ans.optionId.length === 36 ? ans.optionId : undefined,
          selectedText: ans.text,
          markedForReview: flaggedQuestions.has(qId),
        }));

      // Submit attempt with answers and recorded practice time
      await learningService.submitAttempt(attemptId, {
        answers: formattedAnswers,
        timeSpentSeconds: elapsedPracticeSeconds,
      });

      const review = await learningService.getAttemptReview(attemptId);
      setAttemptReview(review);
      setIsSubmitModalOpen(false);
      // Reload studio stats
      loadStudioData();
    } catch (err: any) {
      setErrorMessage(err.response?.data?.message || err.message || 'Failed to submit quiz attempt');
    } finally {
      setActionLoading(false);
    }
  };

  // Flashcard Review Handlers
  const handleStartReview = async (deck?: FlashcardDeck) => {
    setActionLoading(true);
    setErrorMessage(null);
    try {
      const cards = await learningService.getDueCards(deck?.id);
      if (cards.length === 0) {
        setErrorMessage(deck ? `No cards due for review in "${deck.title}". Great job!` : 'No cards due for review today across all decks!');
        setActionLoading(false);
        return;
      }
      setActiveDeckForReview(deck || null);
      setReviewCards(cards);
      setCurrentCardIndex(0);
      setIsCardFlipped(false);
      setReviewFinished(false);
      setReviewSessionStats({ again: 0, hard: 0, good: 0, easy: 0 });
    } catch (err: any) {
      setErrorMessage(err.response?.data?.message || err.message || 'Failed to start card review');
    } finally {
      setActionLoading(false);
    }
  };

  const handleRateCard = async (rating: ReviewRating) => {
    if (reviewCards.length === 0 || currentCardIndex >= reviewCards.length) return;
    const card = reviewCards[currentCardIndex];

    try {
      await learningService.recordReview({
        cardId: card.cardId || (card as any).id || '',
        rating,
        reviewTimeSeconds: 5,
      });

      setReviewSessionStats((prev) => ({
        ...prev,
        [rating.toLowerCase()]: prev[rating.toLowerCase() as keyof typeof prev] + 1,
      }));

      if (currentCardIndex + 1 < reviewCards.length) {
        setCurrentCardIndex((prev) => prev + 1);
        setIsCardFlipped(false);
      } else {
        setReviewFinished(true);
        loadStudioData();
      }
    } catch (err: any) {
      setErrorMessage(err.response?.data?.message || err.message || 'Failed to record card review');
    }
  };

  // Deck Drilldown Handlers
  const handleOpenDeck = async (deck: FlashcardDeck) => {
    setSelectedDeck(deck);
    try {
      const cards = await learningService.listCards(deck.id);
      setDeckCards(cards);
    } catch (err: any) {
      setErrorMessage(err.response?.data?.message || 'Failed to load deck cards');
    }
  };

  const handleAddManualCard = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedDeck) return;
    setActionLoading(true);
    try {
      await learningService.createCard(selectedDeck.id, newCardForm);
      setIsAddCardModalOpen(false);
      setNewCardForm({ question: '', answer: '', sourceCitation: '' });
      handleOpenDeck(selectedDeck);
      loadStudioData();
    } catch (err: any) {
      setErrorMessage(err.response?.data?.message || 'Failed to add card');
    } finally {
      setActionLoading(false);
    }
  };

  const handleDeleteCard = async (cardId: string) => {
    if (!confirm('Are you sure you want to delete this card?')) return;
    try {
      await learningService.deleteCard(cardId);
      if (selectedDeck) handleOpenDeck(selectedDeck);
      loadStudioData();
    } catch (err: any) {
      setErrorMessage(err.response?.data?.message || 'Failed to delete card');
    }
  };

  // AI Generation Handlers
  const handleGenerateQuiz = async (e: React.FormEvent) => {
    e.preventDefault();
    setActionLoading(true);
    setErrorMessage(null);
    try {
      const isDocScoped = selectedDocIds.length > 0;
      const topicLabel = selectedSubject !== 'ALL' ? selectedSubject : quizForm.conceptFocus;

      // Build target materials with validated page ranges
      const targetMaterials: TargetMaterialRequest[] = [];
      for (const docId of selectedDocIds) {
        const doc = workspaceDocs.find((d) => d.id === docId);
        const total = doc?.pageCount || 1;
        const range = docPageRanges[docId] || { isAllPages: true, startPage: 1, endPage: total };

        if (!range.isAllPages) {
          const start = Number(range.startPage) || 1;
          const end = Number(range.endPage) || total;
          if (start < 1 || end > total || start > end) {
            setErrorMessage(`Invalid page range for "${doc?.title || 'Material'}": Must be between 1 and ${total}, and start page cannot exceed end page.`);
            setActionLoading(false);
            return;
          }
          targetMaterials.push({
            documentId: docId,
            startPage: start,
            endPage: end,
          });
        } else {
          targetMaterials.push({
            documentId: docId,
            startPage: 1,
            endPage: total,
          });
        }
      }

      const isTargetedRevision = targetMaterials.length > 0;
      const req: GenerateQuizRequest = {
        title: quizForm.title || `${topicLabel} ${isTargetedRevision ? 'Revision ' : ''}Quiz`,
        sourceType: isTargetedRevision ? 'TARGETED_REVISION' : (isDocScoped ? 'WORKSPACE_DOCUMENTS' : 'CUSTOM'),
        sourceWorkspaceId: currentWorkspace?.id,
        workspaceId: currentWorkspace?.id,
        documentIds: isDocScoped ? selectedDocIds : undefined,
        targetMaterials: isTargetedRevision ? targetMaterials : undefined,
        subjectName: selectedSubject !== 'ALL' ? selectedSubject : undefined,
        conceptFocus: quizForm.conceptFocus || topicLabel,
        difficulty: quizForm.difficultyLevel,
        difficultyLevel: quizForm.difficultyLevel,
        questionCount: quizForm.questionCount,
      };
      await learningService.generateQuiz(req);
      setIsGenerateQuizModalOpen(false);
      loadStudioData();
      setActiveTab('quizzes');
    } catch (err: any) {
      setErrorMessage(err.response?.data?.message || err.message || 'Quiz generation failed');
    } finally {
      setActionLoading(false);
    }
  };

  const handleGenerateCards = async (e: React.FormEvent) => {
    e.preventDefault();
    setActionLoading(true);
    setErrorMessage(null);
    try {
      const isDocScoped = selectedDocIds.length > 0;
      const topicLabel = selectedSubject !== 'ALL' ? selectedSubject : cardGenForm.conceptFocus;
      const req: GenerateFlashcardsRequest = {
        deckTitle: cardGenForm.deckTitle || `${topicLabel} Deck`,
        sourceType: isDocScoped ? 'WORKSPACE_DOCUMENTS' : 'CUSTOM',
        sourceWorkspaceId: currentWorkspace?.id,
        workspaceId: currentWorkspace?.id,
        documentIds: isDocScoped ? selectedDocIds : undefined,
        conceptFocus: cardGenForm.conceptFocus || topicLabel,
        cardCount: cardGenForm.cardCount,
      };
      await learningService.generateDeck(req);
      setIsGenerateCardsModalOpen(false);
      loadStudioData();
      setActiveTab('flashcards');
    } catch (err: any) {
      setErrorMessage(err.response?.data?.message || err.message || 'Flashcard generation failed');
    } finally {
      setActionLoading(false);
    }
  };

  const handleCreateManualDeck = async (e: React.FormEvent) => {
    e.preventDefault();
    setActionLoading(true);
    try {
      await learningService.createManualDeck(newDeckForm);
      setIsCreateDeckModalOpen(false);
      setNewDeckForm({ title: '', description: '', subjectName: 'Computer Science', topicName: '' });
      loadStudioData();
      setActiveTab('flashcards');
    } catch (err: any) {
      setErrorMessage(err.response?.data?.message || 'Failed to create deck');
    } finally {
      setActionLoading(false);
    }
  };

  // -------------------------------------------------------------
  // RENDER: Active Quiz Practice View
  // -------------------------------------------------------------
  if (activeQuizPractice && !attemptReview) {
    const totalQ = activeQuizPractice.questions.length;
    const currentQ = activeQuizPractice.questions[currentQuestionIndex];
    const currentQId = getQId(currentQ);
    const isFlagged = currentQId ? flaggedQuestions.has(currentQId) : false;
    const answeredCount = Object.keys(selectedAnswers).filter((k) => k && k !== 'undefined' && k !== 'null').length;

    return (
      <div className="max-w-4xl mx-auto px-4 py-8 space-y-6 text-[#F5F5F5]">
        {/* Practice Top Bar */}
        <div className="flex items-center justify-between bg-[#171719] border border-white/10 rounded-2xl p-5 shadow-xl">
          <div className="flex items-center gap-3.5">
            <button
              onClick={() => {
                if (confirm('Leave quiz practice? Your progress so far will be saved in your attempt.')) {
                  setActiveQuizPractice(null);
                }
              }}
              className="text-[#A1A1AA] hover:text-white text-xs font-semibold flex items-center gap-1.5 transition"
            >
              <ArrowLeft className="w-4 h-4" /> Exit
            </button>
            <div className="h-4 w-px bg-white/10" />
            <div>
              <h2 className="font-semibold text-white text-base truncate max-w-md">{activeQuizPractice.title}</h2>
              <p className="text-xs text-[#A1A1AA]">
                Question {currentQuestionIndex + 1} of {totalQ}
              </p>
            </div>
          </div>

          <div className="flex items-center gap-4">
            <div className="flex items-center gap-1.5 px-3 py-1.5 bg-[#202025] rounded-xl text-xs font-mono text-[#C7F000] border border-white/10">
              <Clock className="w-3.5 h-3.5" />
              <span>
                {Math.floor(elapsedPracticeSeconds / 60)}:
                {(elapsedPracticeSeconds % 60).toString().padStart(2, '0')}
              </span>
            </div>

            <button
              onClick={() => setIsSubmitModalOpen(true)}
              className="btn-chartreuse"
            >
              Submit Quiz
            </button>
          </div>
        </div>

        {/* Question Progress Dots */}
        <div className="flex items-center gap-2 overflow-x-auto pb-1">
          {activeQuizPractice.questions.map((q, idx) => {
            const qId = getQId(q) || `q-${idx}`;
            const answered = !!selectedAnswers[qId];
            const flagged = flaggedQuestions.has(qId);
            const isCurrent = idx === currentQuestionIndex;

            return (
              <button
                key={qId}
                onClick={() => setCurrentQuestionIndex(idx)}
                className={`w-9 h-9 rounded-xl text-xs font-semibold transition-all flex items-center justify-center relative ${
                  isCurrent
                    ? 'bg-[#FF2D8D] text-white ring-2 ring-[#FF2D8D]/50 shadow-md shadow-[#FF2D8D]/30'
                    : answered
                    ? 'bg-[#C7F000]/15 text-[#C7F000] border border-[#C7F000]/30'
                    : 'bg-[#171719] text-[#A1A1AA] border border-white/10 hover:bg-[#202025]'
                }`}
              >
                {idx + 1}
                {flagged && (
                  <span className="absolute -top-1 -right-1 w-2.5 h-2.5 bg-amber-400 rounded-full" />
                )}
              </button>
            );
          })}
        </div>

        {/* Question Body */}
        {currentQ && (
          <div className="bg-[#171719] border border-white/10 rounded-3xl p-8 space-y-6 shadow-2xl">
            <div className="flex items-start justify-between gap-4">
              <div className="space-y-1.5">
                <span className="px-3 py-1 rounded-full text-xs font-semibold bg-[#FF2D8D]/15 text-[#FF2D8D] border border-[#FF2D8D]/30 font-mono">
                  {currentQ.questionType === 'MULTIPLE_CHOICE' ? 'Multiple Choice' : 'True / False'}
                </span>
                <h3 className="text-xl font-serif text-white leading-relaxed pt-2 font-normal">
                  {currentQ.questionText}
                </h3>
              </div>

              <button
                onClick={() => toggleFlagQuestion(currentQId)}
                className={`p-2.5 rounded-xl border transition ${
                  isFlagged
                    ? 'bg-amber-500/10 text-amber-400 border-amber-500/30'
                    : 'text-[#A1A1AA] hover:text-white border-white/10 hover:bg-white/5'
                }`}
                title={isFlagged ? 'Remove flag' : 'Mark for review'}
              >
                <Flag className="w-4 h-4" />
              </button>
            </div>

            {/* Options */}
            <div className="space-y-3 pt-2">
              {currentQ.options.map((opt) => {
                const optId = getOptId(opt);
                const currentAnswer = selectedAnswers[currentQId];
                const isSelected = !!currentAnswer && (
                  currentAnswer.optionIndex === opt.optionIndex ||
                  (!!optId && currentAnswer.optionId === optId)
                );

                return (
                  <button
                    key={optId || opt.optionIndex}
                    onClick={() => handleSelectOption(currentQId, optId, opt.optionIndex, opt.optionText)}
                    className={`w-full text-left p-4.5 rounded-2xl border transition-all flex items-start gap-4 ${
                      isSelected
                        ? 'bg-[#FF2D8D]/15 border-[#FF2D8D] text-white shadow-lg ring-1 ring-[#FF2D8D]/50'
                        : 'bg-[#202025] border-white/10 text-zinc-300 hover:bg-[#2A2A32] hover:border-white/20'
                    }`}
                  >
                    <div
                      className={`w-5 h-5 rounded-full border flex items-center justify-center mt-0.5 shrink-0 transition-colors ${
                        isSelected ? 'border-[#FF2D8D] bg-[#FF2D8D] text-white' : 'border-zinc-600'
                      }`}
                    >
                      {isSelected && <Check className="w-3 h-3 stroke-[3]" />}
                    </div>
                    <span className="text-sm leading-relaxed">{opt.optionText}</span>
                  </button>
                );
              })}
            </div>

            {/* Navigation buttons */}
            <div className="flex items-center justify-between pt-5 border-t border-white/10">
              <button
                disabled={currentQuestionIndex === 0}
                onClick={() => setCurrentQuestionIndex((prev) => prev - 1)}
                className="px-4 py-2 rounded-xl border border-white/10 text-[#A1A1AA] hover:text-white hover:bg-white/5 disabled:opacity-40 disabled:pointer-events-none text-xs font-semibold flex items-center gap-1.5 transition"
              >
                <ArrowLeft className="w-4 h-4" /> Previous
              </button>

              {currentQuestionIndex + 1 < totalQ ? (
                <button
                  onClick={() => setCurrentQuestionIndex((prev) => prev + 1)}
                  className="px-5 py-2.5 rounded-xl bg-[#202025] hover:bg-[#2A2A32] text-white text-xs font-semibold flex items-center gap-1.5 transition border border-white/10 hover:border-[#FF2D8D]/40"
                >
                  Next <ArrowRight className="w-4 h-4" />
                </button>
              ) : (
                <button
                  onClick={() => setIsSubmitModalOpen(true)}
                  className="btn-chartreuse"
                >
                  Review & Submit
                </button>
              )}
            </div>
          </div>
        )}

        {/* Submit Confirmation Modal */}
        {isSubmitModalOpen && (
          <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/70 backdrop-blur-sm p-4">
            <div className="bg-slate-900 border border-slate-800 rounded-2xl max-w-md w-full p-6 space-y-5 shadow-2xl">
              <div className="flex items-center gap-3 text-emerald-400">
                <CheckCircle2 className="w-6 h-6" />
                <h3 className="text-lg font-semibold text-white">Submit Quiz</h3>
              </div>

              <div className="space-y-3 text-sm text-slate-300">
                <p>Are you sure you want to finish this quiz attempt? Your answers will be graded immediately.</p>
                <div className="bg-slate-950 p-3 rounded-lg border border-slate-800 space-y-1.5 text-xs font-mono">
                  <div className="flex justify-between">
                    <span className="text-slate-400">Answered Questions:</span>
                    <span className="text-emerald-400 font-semibold">{answeredCount} of {totalQ}</span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-slate-400">Unanswered Questions:</span>
                    <span className="text-amber-400 font-semibold">{totalQ - answeredCount}</span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-slate-400">Flagged for Review:</span>
                    <span className="text-indigo-400 font-semibold">{flaggedQuestions.size}</span>
                  </div>
                </div>
              </div>

              <div className="flex items-center justify-end gap-3 pt-2">
                <button
                  onClick={() => setIsSubmitModalOpen(false)}
                  className="px-4 py-2 text-sm text-slate-400 hover:text-white rounded-lg hover:bg-slate-800 transition-colors"
                >
                  Keep Working
                </button>
                <button
                  onClick={handleSubmitQuiz}
                  disabled={actionLoading}
                  className="px-5 py-2 bg-emerald-600 hover:bg-emerald-500 text-white text-sm font-medium rounded-lg flex items-center gap-2 transition-colors disabled:opacity-50"
                >
                  {actionLoading ? <Loader2 className="w-4 h-4 animate-spin" /> : null}
                  Confirm & Grade
                </button>
              </div>
            </div>
          </div>
        )}
      </div>
    );
  }

  // -------------------------------------------------------------
  // RENDER: Post-Submission Quiz Review View
  // -------------------------------------------------------------
  if (attemptReview) {
    const totalQuestions = attemptReview.totalQuestions || attemptReview.maxScore || attemptReview.questions?.length || 1;
    const correctCount = (attemptReview as any).correctAnswersCount ?? (attemptReview as any).correctCount ?? attemptReview.score ?? 0;
    const calculatedPercentage = totalQuestions > 0 ? Math.round((Number(correctCount) / Number(totalQuestions)) * 100) : 0;
    const percentage = attemptReview.percentage !== undefined && attemptReview.percentage !== null
      ? Math.round(Number(attemptReview.percentage))
      : (attemptReview.score !== undefined && attemptReview.score !== null && attemptReview.score <= 100 && attemptReview.score > totalQuestions
          ? Math.round(Number(attemptReview.score))
          : calculatedPercentage);
    const isPassing = percentage >= 70;

    return (
      <div className="max-w-4xl mx-auto px-4 py-8 space-y-6">
        {/* Score Banner */}
        <div className={`p-6 rounded-2xl border shadow-lg ${
          isPassing
            ? 'bg-emerald-950/30 border-emerald-500/40'
            : 'bg-amber-950/30 border-amber-500/40'
        }`}>
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
            <div className="flex items-center gap-4">
              <div className={`w-14 h-14 rounded-2xl flex items-center justify-center shrink-0 shadow-md ${
                isPassing ? 'bg-emerald-600 text-white' : 'bg-amber-600 text-white'
              }`}>
                <Award className="w-8 h-8" />
              </div>
              <div>
                <span className={`text-xs font-semibold uppercase tracking-wider ${
                  isPassing ? 'text-emerald-400' : 'text-amber-400'
                }`}>
                  {isPassing ? 'Mastery Achieved' : 'Needs Review'}
                </span>
                <h2 className="text-2xl font-bold text-white">{percentage}% Score</h2>
                <p className="text-xs text-slate-300">
                  {correctCount} of {totalQuestions} questions answered correctly
                </p>
              </div>
            </div>

            <div className="flex items-center gap-3">
              <button
                onClick={() => {
                  setAttemptReview(null);
                  setActiveQuizPractice(null);
                  setActiveTab('quizzes');
                }}
                className="px-4 py-2 rounded-xl bg-slate-800 hover:bg-slate-700 text-white text-sm font-medium transition-colors"
              >
                Back to Quizzes
              </button>
              <button
                onClick={() => handleStartQuizPractice(attemptReview.quizId || (attemptReview as any).quiz?.id)}
                className="px-4 py-2 rounded-xl bg-indigo-600 hover:bg-indigo-500 text-white text-sm font-medium flex items-center gap-1.5 transition-colors shadow-sm"
              >
                <RotateCw className="w-4 h-4" /> Retake Quiz
              </button>
            </div>
          </div>
        </div>

        {/* Detailed Question Review List */}
        <div className="space-y-4">
          <h3 className="text-lg font-semibold text-white">Question Breakdown & Explanations</h3>
          {attemptReview.questions.map((q, idx) => {
            const qKey = q.questionId || (q as any).id || `rev-q-${idx}`;
            const isUserCorrect = (q as any).isCorrect !== undefined ? (q as any).isCorrect : q.isUserCorrect;

            return (
              <div
                key={qKey}
                className="bg-slate-900 border border-slate-800 rounded-xl p-5 space-y-4 shadow-sm"
              >
                <div className="flex items-start justify-between gap-4">
                  <div className="flex items-center gap-2">
                    <span className="text-xs font-mono text-slate-400">Q{idx + 1}.</span>
                    <span className="text-sm font-medium text-white">{q.questionText}</span>
                  </div>

                  <span className={`px-2.5 py-0.5 rounded-full text-xs font-medium shrink-0 flex items-center gap-1 ${
                    isUserCorrect
                      ? 'bg-emerald-950/80 text-emerald-300 border border-emerald-700/60'
                      : 'bg-rose-950/80 text-rose-300 border border-rose-700/60'
                  }`}>
                    {isUserCorrect ? (
                      <>
                        <CheckCircle2 className="w-3.5 h-3.5" /> Correct
                      </>
                    ) : (
                      <>
                        <XCircle className="w-3.5 h-3.5" /> Incorrect
                      </>
                    )}
                  </span>
                </div>

                {/* Options List with Answer Key Highlighting */}
                <div className="space-y-2">
                  {q.options.map((opt) => {
                    const optId = (opt as any).id || (opt as any).optionId;
                    const isUserChoice = (optId && optId === q.selectedOptionId) || opt.optionIndex === q.selectedOptionIndex;
                    const isCorrectAnswer = Boolean(opt.isCorrect || (opt as any).correct || (optId && optId === q.correctOptionId));

                    let optionBorder = 'border-slate-800/80 bg-slate-950/40 text-slate-400';
                    if (isCorrectAnswer) {
                      optionBorder = 'border-emerald-500/80 bg-emerald-950/30 text-emerald-200 ring-1 ring-emerald-500/40';
                    } else if (isUserChoice && !isCorrectAnswer) {
                      optionBorder = 'border-rose-500/80 bg-rose-950/30 text-rose-200 ring-1 ring-rose-500/40';
                    }

                    return (
                      <div
                        key={optId || opt.optionIndex}
                        className={`p-3 rounded-lg border text-sm flex items-start gap-2.5 ${optionBorder}`}
                      >
                        <div className="mt-0.5 shrink-0">
                          {isCorrectAnswer ? (
                            <CheckCircle2 className="w-4 h-4 text-emerald-400" />
                          ) : isUserChoice ? (
                            <XCircle className="w-4 h-4 text-rose-400" />
                          ) : (
                            <div className="w-4 h-4 rounded-full border border-slate-700" />
                          )}
                        </div>
                        <span className="leading-relaxed">{opt.optionText}</span>
                        {isUserChoice && (
                          <span className="ml-auto text-xs px-2 py-0.5 rounded bg-slate-800 text-slate-300 shrink-0 font-medium">
                            Your Answer
                          </span>
                        )}
                      </div>
                    );
                  })}
                </div>

                {/* Academic Explanation Callout */}
                {q.explanation && (
                  <div className="p-3.5 rounded-xl bg-indigo-950/30 border border-indigo-900/60 text-xs text-indigo-200 space-y-1">
                    <div className="font-semibold text-indigo-300 flex items-center gap-1.5">
                      <Sparkles className="w-3.5 h-3.5" /> Explanation
                    </div>
                    <p className="leading-relaxed text-slate-300">{q.explanation}</p>
                  </div>
                )}
              </div>
            );
          })}
        </div>
      </div>
    );
  }

  // -------------------------------------------------------------
  // RENDER: Active Flashcard Review View (SM-2 Spaced Repetition)
  // -------------------------------------------------------------
  if (reviewCards.length > 0 && !reviewFinished) {
    const currentCard = reviewCards[currentCardIndex];

    return (
      <div className="max-w-2xl mx-auto px-4 py-8 space-y-6">
        {/* Top bar */}
        <div className="flex items-center justify-between text-sm text-slate-400">
          <div className="flex items-center gap-2">
            <button
              onClick={() => {
                if (confirm('Exit card review session?')) {
                  setReviewCards([]);
                }
              }}
              className="hover:text-white flex items-center gap-1 transition-colors"
            >
              <ArrowLeft className="w-4 h-4" /> End Review
            </button>
            {activeDeckForReview && (
              <span className="text-xs text-indigo-400 font-medium">({activeDeckForReview.title})</span>
            )}
          </div>
          <span className="font-mono text-xs text-[#A1A1AA]">
            Card {currentCardIndex + 1} of {reviewCards.length}
          </span>
        </div>

        {/* 3D Flip Flashcard */}
        <div
          onClick={() => setIsCardFlipped(!isCardFlipped)}
          className="min-h-[320px] w-full rounded-3xl bg-gradient-to-br from-[#171719] to-[#111114] border border-white/10 p-8 flex flex-col justify-between cursor-pointer hover:border-[#FF2D8D]/50 transition-all shadow-2xl group relative select-none"
        >
          <div className="flex items-center justify-between">
            <span className="text-xs font-semibold uppercase tracking-wider text-[#FF2D8D] flex items-center gap-1.5 font-mono">
              <BrainCircuit className="w-4 h-4" />
              {isCardFlipped ? 'Answer' : 'Question (Prompt)'}
            </span>
            <span className="text-xs text-[#A1A1AA] group-hover:text-white transition">
              Click to flip
            </span>
          </div>

          <div className="py-8 text-center">
            <p className="text-xl md:text-2xl font-serif text-white leading-relaxed font-normal">
              {isCardFlipped
                ? currentCard.back || currentCard.answer
                : currentCard.front || currentCard.question}
            </p>
          </div>

          <div className="text-xs text-[#A1A1AA] flex items-center justify-between">
            <span>{currentCard.sourceCitation || currentCard.sourceReference || 'Academic Material'}</span>
            <span className="text-[#C7F000] font-mono text-[11px]">
              {isCardFlipped ? 'Rate recall below' : 'Press Space to flip'}
            </span>
          </div>
        </div>

        {/* SM-2 Rating Controls (Revealed after flip) */}
        {isCardFlipped ? (
          <div className="space-y-3 pt-2">
            <p className="text-xs text-center text-[#A1A1AA]">
              How easily did you recall this concept?
            </p>
            <div className="grid grid-cols-4 gap-3">
              <button
                onClick={() => handleRateCard('AGAIN')}
                className="p-3.5 rounded-2xl bg-rose-500/10 border border-rose-500/30 hover:bg-rose-500/20 text-rose-300 text-xs font-medium flex flex-col items-center gap-1 transition"
              >
                <span className="font-bold text-sm">Again</span>
                <span className="text-[10px] text-rose-400">&lt; 1 day</span>
              </button>

              <button
                onClick={() => handleRateCard('HARD')}
                className="p-3.5 rounded-2xl bg-amber-500/10 border border-amber-500/30 hover:bg-amber-500/20 text-amber-300 text-xs font-medium flex flex-col items-center gap-1 transition"
              >
                <span className="font-bold text-sm">Hard</span>
                <span className="text-[10px] text-amber-400">Modest</span>
              </button>

              <button
                onClick={() => handleRateCard('GOOD')}
                className="p-3.5 rounded-2xl bg-[#C7F000]/10 border border-[#C7F000]/30 hover:bg-[#C7F000]/20 text-[#C7F000] text-xs font-medium flex flex-col items-center gap-1 transition"
              >
                <span className="font-bold text-sm">Good</span>
                <span className="text-[10px] text-[#C7F000]">SM-2 Int.</span>
              </button>

              <button
                onClick={() => handleRateCard('EASY')}
                className="p-3.5 rounded-2xl bg-[#FF2D8D]/10 border border-[#FF2D8D]/30 hover:bg-[#FF2D8D]/20 text-[#FF2D8D] text-xs font-medium flex flex-col items-center gap-1 transition"
              >
                <span className="font-bold text-sm">Easy</span>
                <span className="text-[10px] text-[#FF2D8D]">Bonus Int.</span>
              </button>
            </div>
          </div>
        ) : (
          <div className="text-center pt-4">
            <button
              onClick={() => setIsCardFlipped(true)}
              className="btn-chartreuse"
            >
              Reveal Answer
            </button>
          </div>
        )}
      </div>
    );
  }

  // -------------------------------------------------------------
  // RENDER: Card Review Session Completion
  // -------------------------------------------------------------
  if (reviewFinished) {
    return (
      <div className="max-w-md mx-auto px-4 py-16 text-center space-y-6">
        <div className="w-16 h-16 rounded-2xl bg-emerald-600/20 border border-emerald-500/30 text-emerald-400 flex items-center justify-center mx-auto shadow-lg">
          <CheckCircle2 className="w-8 h-8" />
        </div>

        <div className="space-y-2">
          <h2 className="text-2xl font-bold text-white">Review Session Complete!</h2>
          <p className="text-sm text-slate-400">
            You reviewed {reviewCards.length} cards. Your SuperMemo-2 intervals have been updated.
          </p>
        </div>

        <div className="bg-slate-900 border border-slate-800 rounded-xl p-4 grid grid-cols-4 gap-2 text-center text-xs">
          <div>
            <div className="font-bold text-rose-400 text-base">{reviewSessionStats.again}</div>
            <div className="text-slate-400 text-[10px]">Again</div>
          </div>
          <div>
            <div className="font-bold text-amber-400 text-base">{reviewSessionStats.hard}</div>
            <div className="text-slate-400 text-[10px]">Hard</div>
          </div>
          <div>
            <div className="font-bold text-emerald-400 text-base">{reviewSessionStats.good}</div>
            <div className="text-slate-400 text-[10px]">Good</div>
          </div>
          <div>
            <div className="font-bold text-indigo-400 text-base">{reviewSessionStats.easy}</div>
            <div className="text-slate-400 text-[10px]">Easy</div>
          </div>
        </div>

        <button
          onClick={() => {
            setReviewCards([]);
            setReviewFinished(false);
            setActiveTab('flashcards');
          }}
          className="px-6 py-2.5 rounded-xl bg-indigo-600 hover:bg-indigo-500 text-white text-sm font-medium transition-colors shadow-sm"
        >
          Return to Flashcards
        </button>
      </div>
    );
  }

  // -------------------------------------------------------------
  // RENDER: Main Learning Studio Shell (Dashboard, Quizzes, Flashcards, Analytics)
  // -------------------------------------------------------------
  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-8 text-[#F5F5F5]">
      {/* Header */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 border-b border-white/10 pb-6">
        <div>
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-2xl bg-[#FF2D8D]/10 border border-[#FF2D8D]/30 flex items-center justify-center text-[#FF2D8D] shadow-sm">
              <BrainCircuit className="w-5 h-5" />
            </div>
            <div>
              <h1 className="text-2xl sm:text-3xl font-serif text-white tracking-tight font-normal">
                Interactive Learning Studio
              </h1>
              <p className="text-xs sm:text-sm text-[#A1A1AA] mt-0.5">
                AI quizzes, active recall flashcards, and SuperMemo-2 spaced repetition
              </p>
            </div>
          </div>
        </div>

        <div className="flex items-center gap-2.5">
          <button
            onClick={() => setIsGenerateQuizModalOpen(true)}
            className="btn-chartreuse rounded-full px-4 py-2 inline-flex items-center gap-2"
          >
            <Sparkles className="w-4 h-4 text-black" />
            <span>Generate Quiz</span>
          </button>
          <button
            onClick={() => setIsGenerateCardsModalOpen(true)}
            className="px-4 py-2.5 bg-[#202025] hover:bg-[#2A2A32] text-white rounded-full text-xs sm:text-sm font-medium flex items-center gap-2 transition-all border border-white/10 hover:border-[#FF2D8D]/40"
          >
            <Layers className="w-4 h-4 text-[#FF2D8D]" />
            <span>Generate Flashcards</span>
          </button>
        </div>
      </div>

      {/* Error alert */}
      {errorMessage && (
        <div className="p-4 rounded-2xl bg-rose-500/10 border border-rose-500/20 text-xs text-rose-300 flex items-center justify-between">
          <div className="flex items-center gap-2">
            <AlertTriangle className="w-4 h-4 shrink-0 text-rose-400" />
            <span>{errorMessage}</span>
          </div>
          <button
            onClick={() => setErrorMessage(null)}
            className="text-xs text-rose-400 hover:text-white underline"
          >
            Dismiss
          </button>
        </div>
      )}

      {/* Tabs Bar */}
      <div className="flex items-center gap-3 border-b border-white/10 overflow-x-auto">
        <button
          onClick={() => setActiveTab('dashboard')}
          className={`px-4 py-3 text-xs sm:text-sm font-medium border-b-2 transition-colors flex items-center gap-2 whitespace-nowrap ${
            activeTab === 'dashboard'
              ? 'border-[#FF2D8D] text-[#FF2D8D] font-semibold'
              : 'border-transparent text-[#A1A1AA] hover:text-white'
          }`}
        >
          <BarChart3 className="w-4 h-4" /> Studio Dashboard
        </button>

        <button
          onClick={() => setActiveTab('quizzes')}
          className={`px-4 py-3 text-xs sm:text-sm font-medium border-b-2 transition-colors flex items-center gap-2 whitespace-nowrap ${
            activeTab === 'quizzes'
              ? 'border-[#FF2D8D] text-[#FF2D8D] font-semibold'
              : 'border-transparent text-[#A1A1AA] hover:text-white'
          }`}
        >
          <GraduationCap className="w-4 h-4" /> Quizzes ({quizzes.length})
        </button>

        <button
          onClick={() => setActiveTab('flashcards')}
          className={`px-4 py-3 text-xs sm:text-sm font-medium border-b-2 transition-colors flex items-center gap-2 whitespace-nowrap ${
            activeTab === 'flashcards'
              ? 'border-[#FF2D8D] text-[#FF2D8D] font-semibold'
              : 'border-transparent text-[#A1A1AA] hover:text-white'
          }`}
        >
          <Layers className="w-4 h-4" /> Flashcard Decks ({decks.length})
        </button>

        <button
          onClick={() => setActiveTab('analytics')}
          className={`px-4 py-3 text-xs sm:text-sm font-medium border-b-2 transition-colors flex items-center gap-2 whitespace-nowrap ${
            activeTab === 'analytics'
              ? 'border-[#FF2D8D] text-[#FF2D8D] font-semibold'
              : 'border-transparent text-[#A1A1AA] hover:text-white'
          }`}
        >
          <BookmarkCheck className="w-4 h-4" /> Mastery & Analytics
        </button>
      </div>

      {isLoading ? (
        <div className="py-24 flex flex-col items-center justify-center text-slate-400 gap-3">
          <Loader2 className="w-8 h-8 animate-spin text-indigo-500" />
          <span className="text-sm">Loading your Learning Studio workspace...</span>
        </div>
      ) : (
        <>
          {/* ------------------------------------------------------------- */}
          {/* TAB 1: DASHBOARD */}
          {/* ------------------------------------------------------------- */}
      {activeTab === 'dashboard' && overview && (
        <div className="space-y-6">
          {/* Due Cards Callout Banner */}
          {overview.cardsDueToday > 0 && (
            <div className="p-6 rounded-3xl bg-gradient-to-r from-[#171719] via-[#111114] to-[#09090B] border border-[#FF2D8D]/30 flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4 shadow-xl">
              <div className="flex items-center gap-4">
                <div className="w-12 h-12 rounded-2xl bg-[#FF2D8D]/15 text-[#FF2D8D] border border-[#FF2D8D]/30 flex items-center justify-center shrink-0 shadow-md">
                  <Clock className="w-6 h-6" />
                </div>
                <div>
                  <h3 className="text-base font-semibold text-white">
                    {overview.cardsDueToday} {overview.cardsDueToday === 1 ? 'card' : 'cards'} due for review today
                  </h3>
                  <p className="text-xs text-[#A1A1AA] mt-0.5">
                    Maintain optimal active recall retention with SuperMemo-2 spaced repetition.
                  </p>
                </div>
              </div>
              <button
                onClick={() => handleStartReview()}
                className="btn-chartreuse shrink-0"
              >
                Review Now
              </button>
            </div>
          )}

          {/* Metric Cards */}
          <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
            <div className="bg-[#171719] border border-white/10 rounded-2xl p-5 space-y-1 shadow-lg">
              <span className="text-[11px] font-semibold text-[#A1A1AA] uppercase tracking-wider">Quizzes Completed</span>
              <div className="text-2xl font-bold text-white">{overview.totalQuizAttempts}</div>
              <span className="text-[11px] text-[#C7F000] font-mono">
                Avg Score: {overview.averageQuizScore}%
              </span>
            </div>

            <div className="bg-[#171719] border border-white/10 rounded-2xl p-5 space-y-1 shadow-lg">
              <span className="text-[11px] font-semibold text-[#A1A1AA] uppercase tracking-wider">Flashcard Decks</span>
              <div className="text-2xl font-bold text-white">{overview.totalDecks}</div>
              <span className="text-[11px] text-[#FF2D8D] font-mono">
                {overview.totalCards} Total Cards
              </span>
            </div>

            <div className="bg-[#171719] border border-white/10 rounded-2xl p-5 space-y-1 shadow-lg">
              <span className="text-[11px] font-semibold text-[#A1A1AA] uppercase tracking-wider">Cards Due Today</span>
              <div className="text-2xl font-bold text-amber-400">{overview.cardsDueToday}</div>
              <span className="text-[11px] text-[#A1A1AA] font-mono">
                {overview.cardsReviewedCount} Reviewed
              </span>
            </div>

            <div className="bg-[#171719] border border-white/10 rounded-2xl p-5 space-y-1 shadow-lg">
              <span className="text-[11px] font-semibold text-[#A1A1AA] uppercase tracking-wider">Graduated Cards</span>
              <div className="text-2xl font-bold text-[#C7F000]">{overview.graduatedCardsCount}</div>
              <span className="text-[11px] text-[#A1A1AA] font-mono">
                {overview.learningCardsCount} Learning
              </span>
            </div>
          </div>

          {/* Quick Access Grid: Recent Quizzes & Decks */}
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            {/* Recent Quizzes */}
            <div className="bg-[#171719] border border-white/10 rounded-3xl p-6 space-y-4 shadow-xl">
              <div className="flex items-center justify-between">
                <h3 className="font-semibold text-white text-sm flex items-center gap-2">
                  <GraduationCap className="w-4 h-4 text-[#FF2D8D]" /> Recent Quizzes
                </h3>
                <button
                  onClick={() => setActiveTab('quizzes')}
                  className="text-xs text-[#FF2D8D] hover:underline font-medium"
                >
                  View All
                </button>
              </div>

              {quizzes.length === 0 ? (
                <div className="py-8 text-center text-xs text-[#A1A1AA]">
                  No quizzes generated yet. Click &quot;Generate Quiz&quot; to create one.
                </div>
              ) : (
                <div className="space-y-2.5">
                  {quizzes.slice(0, 3).map((q) => (
                    <div
                      key={q.id}
                      className="p-3.5 rounded-2xl bg-[#202025] border border-white/5 flex items-center justify-between gap-3 hover:border-white/15 transition"
                    >
                      <div className="min-w-0">
                        <div className="font-medium text-sm text-white truncate">{q.title}</div>
                        <div className="text-xs text-[#A1A1AA] flex items-center gap-2 mt-0.5">
                          <span>{q.totalQuestions} Questions</span>
                          <span>•</span>
                          <span className="text-[#FF2D8D] font-mono text-[11px]">{q.difficulty}</span>
                        </div>
                      </div>
                      <button
                        onClick={() => handleStartQuizPractice(q.id)}
                        className="px-3 py-1.5 rounded-full bg-[#FF2D8D]/15 hover:bg-[#FF2D8D] text-[#FF2D8D] hover:text-white text-xs font-semibold transition-all border border-[#FF2D8D]/30"
                      >
                        Practice
                      </button>
                    </div>
                  ))}
                </div>
              )}
            </div>

            {/* Flashcard Decks */}
            <div className="bg-[#171719] border border-white/10 rounded-3xl p-6 space-y-4 shadow-xl">
              <div className="flex items-center justify-between">
                <h3 className="font-semibold text-white text-sm flex items-center gap-2">
                  <Layers className="w-4 h-4 text-[#C7F000]" /> Active Decks
                </h3>
                <button
                  onClick={() => setActiveTab('flashcards')}
                  className="text-xs text-[#C7F000] hover:underline font-medium"
                >
                  View All
                </button>
              </div>

              {decks.length === 0 ? (
                <div className="py-8 text-center text-xs text-[#A1A1AA]">
                  No flashcard decks yet. Create or generate a deck to start spaced repetition.
                </div>
              ) : (
                <div className="space-y-2.5">
                  {decks.slice(0, 3).map((d) => (
                    <div
                      key={d.id}
                      className="p-3.5 rounded-2xl bg-[#202025] border border-white/5 flex items-center justify-between gap-3 hover:border-white/15 transition"
                    >
                      <div className="min-w-0">
                        <div className="font-medium text-sm text-white truncate">{d.title}</div>
                        <div className="text-xs text-[#A1A1AA] flex items-center gap-2 mt-0.5">
                          <span>{d.totalCards} Cards</span>
                          {d.dueCardsCount > 0 && (
                            <>
                              <span>•</span>
                              <span className="text-amber-400 font-semibold">{d.dueCardsCount} due</span>
                            </>
                          )}
                        </div>
                      </div>
                      <button
                        onClick={() => handleStartReview(d)}
                        className="px-3.5 py-1.5 rounded-full bg-[#202025] hover:bg-white/10 text-white text-xs font-medium transition-colors border border-white/10"
                      >
                        Review
                      </button>
                    </div>
                  ))}
                </div>
              )}
            </div>
          </div>
        </div>
      )}

      {/* ------------------------------------------------------------- */}
      {/* TAB 2: QUIZZES */}
      {/* ------------------------------------------------------------- */}
      {activeTab === 'quizzes' && (
        <div className="space-y-6">
          <div className="flex items-center justify-between">
            <h2 className="text-lg font-serif text-white font-normal">Active Practice Quizzes</h2>
            <button
              onClick={() => setIsGenerateQuizModalOpen(true)}
              className="btn-chartreuse rounded-full px-4 py-2 inline-flex items-center gap-2"
            >
              <Plus className="w-4 h-4 text-black" />
              <span>Create New Quiz</span>
            </button>
          </div>

          {quizzes.length === 0 ? (
            <div className="text-center py-16 bg-[#171719] border border-white/10 rounded-3xl space-y-3 shadow-xl">
              <GraduationCap className="w-12 h-12 text-[#FF2D8D] mx-auto opacity-70" />
              <h3 className="text-base font-semibold text-white">No quizzes generated yet</h3>
              <p className="text-xs text-[#A1A1AA] max-w-sm mx-auto">
                Generate practice questions from your workspace documents, lecture notes, or key computer science concepts.
              </p>
              <button
                onClick={() => setIsGenerateQuizModalOpen(true)}
                className="mt-2 btn-chartreuse rounded-full px-5 py-2.5 inline-flex items-center gap-1.5"
              >
                <span>Generate First Quiz</span>
              </button>
            </div>
          ) : (
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5">
              {quizzes.map((quiz) => (
                <div
                  key={quiz.id}
                  className="bg-[#171719] border border-white/10 rounded-3xl p-6 flex flex-col justify-between space-y-4 shadow-xl hover:border-[#FF2D8D]/40 transition-all group"
                >
                  <div className="space-y-2.5">
                    <div className="flex items-center justify-between">
                      <span className="px-2.5 py-0.5 rounded-full text-[11px] font-semibold bg-[#FF2D8D]/15 text-[#FF2D8D] border border-[#FF2D8D]/30 font-mono">
                        {quiz.difficulty}
                      </span>
                      <span className="text-xs text-[#A1A1AA] font-mono">
                        {quiz.totalQuestions} Questions
                      </span>
                    </div>

                    <h3 className="font-semibold text-white text-base leading-snug line-clamp-2 group-hover:text-[#FF2D8D] transition">
                      {quiz.title}
                    </h3>

                    {quiz.description && (
                      <p className="text-xs text-[#A1A1AA] line-clamp-2">{quiz.description}</p>
                    )}

                    {quiz.targetMaterials && (
                      <div className="pt-1">
                        <div className="flex items-center gap-1.5 text-[10px] text-[#C7F000] font-mono bg-[#C7F000]/10 px-2 py-1 rounded-lg border border-[#C7F000]/20">
                          <BookmarkCheck className="w-3 h-3 text-[#C7F000]" />
                          <span className="truncate">
                            Scope: {(() => {
                              try {
                                const mats = JSON.parse(quiz.targetMaterials);
                                return mats.map((m: any) => `${m.title} (pp. ${m.startPage}-${m.endPage})`).join(', ');
                              } catch {
                                return 'Targeted Scope';
                              }
                            })()}
                          </span>
                        </div>
                      </div>
                    )}
                  </div>

                  <div className="pt-3 border-t border-white/10 flex items-center justify-between text-xs text-[#A1A1AA]">
                    <div>
                      {quiz.bestScore !== undefined && quiz.bestScore !== null ? (
                        <span className="text-[#C7F000] font-mono font-medium">
                          Best: {quiz.bestScore}%
                        </span>
                      ) : (
                        <span className="text-zinc-500">Not attempted</span>
                      )}
                    </div>

                    <div className="flex items-center gap-2">
                      {quiz.targetMaterials && (
                        <button
                          onClick={() => {
                            try {
                              const raw = quiz.targetMaterials;
                              if (!raw) return;
                              const mats = typeof raw === 'string' ? JSON.parse(raw) : raw;
                              const docIds = mats.map((m: any) => m.documentId);
                              setSelectedDocIds(docIds);
                              if (quiz.subjectName) {
                                setSelectedSubject(quiz.subjectName);
                              }
                              const newRanges: Record<string, any> = {};
                              mats.forEach((m: any) => {
                                newRanges[m.documentId] = {
                                  isAllPages: m.startPage === 1 && m.endPage === m.pageCount,
                                  startPage: m.startPage,
                                  endPage: m.endPage,
                                };
                              });
                              setDocPageRanges(newRanges);
                              setIsGenerateQuizModalOpen(true);
                            } catch {
                              setIsGenerateQuizModalOpen(true);
                            }
                          }}
                          className="px-2.5 py-1.5 bg-[#FF2D8D]/15 hover:bg-[#FF2D8D]/25 text-[#FF2D8D] font-medium rounded-full transition-colors flex items-center gap-1 border border-[#FF2D8D]/30 text-xs"
                          title="Revise with this exact material & page scope again"
                        >
                          <RotateCw className="w-3 h-3" /> Revise Again
                        </button>
                      )}
                      <button
                        onClick={async () => {
                          if (confirm(`Delete quiz "${quiz.title}"?`)) {
                            await learningService.deleteQuiz(quiz.id);
                            loadStudioData();
                          }
                        }}
                        className="p-1.5 text-zinc-500 hover:text-rose-400 rounded-lg hover:bg-white/5 transition"
                        title="Delete Quiz"
                      >
                        <Trash2 className="w-4 h-4" />
                      </button>
                      <button
                        onClick={() => handleStartQuizPractice(quiz.id)}
                        className="px-3.5 py-1.5 bg-[#C7F000] hover:bg-[#D4F52B] text-black font-semibold rounded-full transition-colors flex items-center gap-1.5 shadow-sm text-xs"
                      >
                        <Play className="w-3 h-3 fill-black text-black" /> Practice
                      </button>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      )}

      {/* ------------------------------------------------------------- */}
      {/* TAB 3: FLASHCARDS */}
      {/* ------------------------------------------------------------- */}
      {activeTab === 'flashcards' && (
        <div className="space-y-6">
          {/* Deck Detail drilldown view */}
          {selectedDeck ? (
            <div className="space-y-6">
              <div className="flex items-center justify-between">
                <button
                  onClick={() => setSelectedDeck(null)}
                  className="text-[#A1A1AA] hover:text-white text-xs font-semibold flex items-center gap-1.5 transition"
                >
                  <ArrowLeft className="w-4 h-4" /> Back to All Decks
                </button>
                <button
                  onClick={() => setIsAddCardModalOpen(true)}
                  className="btn-chartreuse inline-flex items-center gap-1.5"
                >
                  <Plus className="w-4 h-4 text-black" />
                  <span>Add Card</span>
                </button>
              </div>

              <div className="bg-[#171719] border border-white/10 rounded-3xl p-6 space-y-2 shadow-xl">
                <h2 className="text-xl font-serif text-white font-normal">{selectedDeck.title}</h2>
                <p className="text-xs text-[#A1A1AA]">{selectedDeck.description}</p>
                <div className="pt-2 flex items-center gap-4 text-xs font-mono text-[#A1A1AA]">
                  <span>Total Cards: {deckCards.length}</span>
                  <span>Subject: {selectedDeck.subjectName || 'Computer Science'}</span>
                </div>
              </div>

              {/* Cards table */}
              <div className="bg-[#171719] border border-white/10 rounded-3xl overflow-hidden shadow-xl">
                <div className="p-5 border-b border-white/10 font-medium text-white text-sm bg-[#111114]">
                  Cards in Deck ({deckCards.length})
                </div>
                {deckCards.length === 0 ? (
                  <div className="p-8 text-center text-xs text-[#A1A1AA]">
                    No cards in this deck yet. Click &quot;Add Card&quot; to create one.
                  </div>
                ) : (
                  <div className="divide-y divide-white/5">
                    {deckCards.map((c) => (
                      <div key={c.id} className="p-5 flex items-start justify-between gap-4 hover:bg-white/[0.02] transition">
                        <div className="space-y-1.5 text-sm max-w-2xl">
                          <div className="font-semibold text-white">{c.front}</div>
                          <div className="text-[#A1A1AA] text-xs leading-relaxed">{c.back}</div>
                          {c.sourceCitation && (
                            <div className="text-[11px] text-[#FF2D8D] font-mono pt-1">
                              Ref: {c.sourceCitation}
                            </div>
                          )}
                        </div>

                        <div className="flex items-center gap-3 shrink-0">
                          <span className="px-2.5 py-1 rounded-full text-[10px] font-mono bg-[#202025] border border-white/10 text-zinc-300">
                            {c.reviewState} ({c.intervalDays}d)
                          </span>
                          <button
                            onClick={() => handleDeleteCard(c.id)}
                            className="text-zinc-500 hover:text-rose-400 p-1 transition-colors"
                          >
                            <Trash2 className="w-4 h-4" />
                          </button>
                        </div>
                      </div>
                    ))}
                  </div>
                )}
              </div>
            </div>
          ) : (
            // Deck list
            <div className="space-y-6">
              <div className="flex items-center justify-between">
                <h2 className="text-lg font-serif text-white font-normal">Active Recall Decks</h2>
                <div className="flex items-center gap-2.5">
                  <button
                    onClick={() => setIsCreateDeckModalOpen(true)}
                    className="px-4 py-2 bg-[#202025] hover:bg-[#2A2A32] text-white rounded-full text-xs font-semibold border border-white/10 hover:border-white/20 transition"
                  >
                    Manual Deck
                  </button>
                  <button
                    onClick={() => setIsGenerateCardsModalOpen(true)}
                    className="btn-chartreuse inline-flex items-center gap-1.5"
                  >
                    <Sparkles className="w-4 h-4 text-black" />
                    <span>AI Generate</span>
                  </button>
                </div>
              </div>

              {decks.length === 0 ? (
                <div className="text-center py-16 bg-[#171719] border border-white/10 rounded-3xl space-y-3 shadow-xl">
                  <Layers className="w-12 h-12 text-[#C7F000] mx-auto opacity-70" />
                  <h3 className="text-base font-semibold text-white">No flashcard decks yet</h3>
                  <p className="text-xs text-[#A1A1AA] max-w-sm mx-auto">
                    Active recall and spaced repetition accelerate retention for exams and technical interviews.
                  </p>
                </div>
              ) : (
                <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5">
                  {decks.map((deck) => (
                    <div
                      key={deck.id}
                      className="bg-[#171719] border border-white/10 rounded-3xl p-6 flex flex-col justify-between space-y-4 shadow-xl hover:border-[#C7F000]/40 transition-all group"
                    >
                      <div className="space-y-2.5">
                        <div className="flex items-center justify-between">
                          <span className="text-xs text-[#C7F000] font-mono font-semibold">
                            {deck.subjectName || 'Computer Science'}
                          </span>
                          {deck.dueCardsCount > 0 ? (
                            <span className="px-2.5 py-0.5 rounded-full text-[10px] font-semibold bg-amber-500/10 text-amber-300 border border-amber-500/30">
                              {deck.dueCardsCount} due
                            </span>
                          ) : (
                            <span className="text-[11px] text-[#A1A1AA]">Caught up</span>
                          )}
                        </div>

                        <h3 className="font-semibold text-white text-base line-clamp-1 group-hover:text-[#C7F000] transition">
                          {deck.title}
                        </h3>

                        {deck.description && (
                          <p className="text-xs text-[#A1A1AA] line-clamp-2">{deck.description}</p>
                        )}
                      </div>

                      <div className="pt-3 border-t border-white/10 flex items-center justify-between text-xs text-[#A1A1AA]">
                        <span>{deck.totalCards} Cards</span>

                        <div className="flex items-center gap-2">
                          <button
                            onClick={() => handleOpenDeck(deck)}
                            className="px-3 py-1 text-[#A1A1AA] hover:text-white rounded-full hover:bg-white/5 transition-colors"
                          >
                            Manage
                          </button>
                          <button
                            onClick={() => handleStartReview(deck)}
                            className="px-4 py-1.5 bg-[#C7F000] hover:bg-[#D4F52B] text-black font-semibold rounded-full transition-colors text-xs"
                          >
                            Review
                          </button>
                        </div>
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>
          )}
        </div>
      )}

      {/* ------------------------------------------------------------- */}
      {/* TAB 4: MASTERY & ANALYTICS */}
      {/* ------------------------------------------------------------- */}
      {activeTab === 'analytics' && (
        <div className="space-y-8">
          {/* Topic Performance Table */}
          <div className="bg-[#171719] border border-white/10 rounded-3xl overflow-hidden shadow-xl">
            <div className="p-6 border-b border-white/10 bg-[#111114]">
              <h3 className="font-serif text-white text-base font-normal">Topic Accuracy & Concept Mastery</h3>
              <p className="text-xs text-[#A1A1AA] mt-0.5">
                Derived directly from persisted quiz attempts and graduated spaced repetition cards.
              </p>
            </div>

            {topicPerformance.length === 0 ? (
              <div className="p-8 text-center text-xs text-[#A1A1AA]">
                No topic activity data recorded yet. Take quizzes to view your accuracy breakdown.
              </div>
            ) : (
              <div className="divide-y divide-white/5">
                {topicPerformance.map((tp) => (
                  <div key={tp.topicName} className="p-5 flex flex-col sm:flex-row sm:items-center justify-between gap-3 hover:bg-white/[0.02] transition">
                    <div className="space-y-1">
                      <div className="font-semibold text-white text-sm">{tp.topicName}</div>
                      <div className="text-xs text-[#A1A1AA]">
                        {tp.totalQuizzes} quizzes completed • {tp.cardsMastered} of {tp.totalCards} cards graduated
                      </div>
                    </div>

                    <div className="flex items-center gap-4">
                      <div className="w-32 bg-[#202025] rounded-full h-2 overflow-hidden border border-white/5">
                        <div
                          className="bg-[#C7F000] h-full rounded-full transition-all"
                          style={{ width: `${Math.min(100, tp.averageScore)}%` }}
                        />
                      </div>
                      <span className="font-mono text-sm font-semibold text-[#C7F000] w-12 text-right">
                        {tp.averageScore}%
                      </span>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>

          {/* Daily Activity Timeline */}
          <div className="bg-[#171719] border border-white/10 rounded-3xl overflow-hidden shadow-xl">
            <div className="p-6 border-b border-white/10 bg-[#111114]">
              <h3 className="font-serif text-white text-base font-normal">Daily Study Activity</h3>
              <p className="text-xs text-[#A1A1AA] mt-0.5">
                Your learning consistency across quizzes and active recall sessions.
              </p>
            </div>

            {activityHistory.length === 0 ? (
              <div className="p-8 text-center text-xs text-[#A1A1AA]">
                No study sessions logged yet.
              </div>
            ) : (
              <div className="overflow-x-auto">
                <table className="w-full text-left text-xs">
                  <thead className="bg-[#202025]/50 text-[#A1A1AA] border-b border-white/10">
                    <tr>
                      <th className="p-4">Date</th>
                      <th className="p-4">Quizzes Taken</th>
                      <th className="p-4">Cards Reviewed</th>
                      <th className="p-4">Time Spent (min)</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-white/5 font-mono text-white">
                    {activityHistory.map((act) => (
                      <tr key={act.date} className="hover:bg-white/[0.02] transition">
                        <td className="p-4 text-[#A1A1AA]">{act.date}</td>
                        <td className="p-4 text-[#FF2D8D] font-semibold">{act.quizzesTaken ?? (act as any).quizAttempts ?? 0}</td>
                        <td className="p-4 text-[#C7F000] font-semibold">{act.cardsReviewed ?? (act as any).flashcardReviews ?? 0}</td>
                        <td className="p-4 text-zinc-300">{act.studyTimeMinutes ?? (act as any).totalMinutes ?? 0} min</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </div>
        </div>
      )}
        </>
      )}

      {/* ------------------------------------------------------------- */}
      {/* MODAL 1: Generate AI Quiz */}
      {/* ------------------------------------------------------------- */}
      {isGenerateQuizModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/80 backdrop-blur-md p-4">
          <div className="bg-[#171719] border border-white/10 rounded-3xl max-w-lg w-full p-6 space-y-5 shadow-2xl text-[#F5F5F5]">
            <div className="flex items-center justify-between border-b border-white/10 pb-4">
              <h3 className="text-base font-semibold text-white flex items-center gap-2">
                <Sparkles className="w-4 h-4 text-[#FF2D8D]" /> Generate AI Practice Quiz
              </h3>
              <button
                onClick={() => setIsGenerateQuizModalOpen(false)}
                className="text-[#A1A1AA] hover:text-white text-xs"
              >
                Cancel
              </button>
            </div>

            <form onSubmit={handleGenerateQuiz} className="space-y-4 text-xs">
              <div>
                <label className="block text-[#A1A1AA] font-medium mb-1">Subject / Category</label>
                <select
                  value={selectedSubject}
                  onChange={(e) => {
                    const subj = e.target.value;
                    setSelectedSubject(subj);
                    if (subj === 'ALL') {
                      setSelectedDocIds(workspaceDocs.filter((d) => d.processingStatus === 'PROCESSED').map((d) => d.id));
                    } else {
                      setSelectedDocIds(
                        workspaceDocs
                          .filter((d) => (d.subject || 'General') === subj && d.processingStatus === 'PROCESSED')
                          .map((d) => d.id)
                      );
                    }
                  }}
                  className="w-full bg-[#202025] border border-white/10 rounded-xl px-3.5 py-2.5 text-white focus:outline-none focus:border-[#FF2D8D]"
                >
                  <option value="ALL">All Subjects ({workspaceDocs.length} materials)</option>
                  {availableSubjects.map((subj) => (
                    <option key={subj} value={subj}>
                      {subj} ({workspaceDocs.filter((d) => (d.subject || 'General') === subj).length} materials)
                    </option>
                  ))}
                </select>
              </div>

              {/* Material Selection Checkboxes */}
              <div>
                <div className="flex items-center justify-between mb-1.5">
                  <label className="text-[#A1A1AA] font-medium">Select Materials to Include</label>
                  <div className="flex gap-2 text-[11px]">
                    <button
                      type="button"
                      onClick={() => setSelectedDocIds(readyDocs.map((d) => d.id))}
                      className="text-[#C7F000] hover:underline"
                    >
                      Select All
                    </button>
                    <button
                      type="button"
                      onClick={() => setSelectedDocIds([])}
                      className="text-[#A1A1AA] hover:text-white"
                    >
                      Clear
                    </button>
                  </div>
                </div>
                <div className="max-h-56 overflow-y-auto space-y-2 p-2 bg-[#09090B] rounded-xl border border-white/10">
                  {filteredDocs.length === 0 ? (
                    <p className="text-[#A1A1AA] text-center py-2 text-[11px]">
                      No documents found for this subject. Upload notes to your workspace.
                    </p>
                  ) : (
                    filteredDocs.map((doc) => {
                      const isChecked = selectedDocIds.includes(doc.id);
                      const isReady = doc.processingStatus === 'PROCESSED';
                      const totalPgs = doc.pageCount && doc.pageCount > 0 ? doc.pageCount : 1;
                      const range = docPageRanges[doc.id] || { isAllPages: true, startPage: 1, endPage: totalPgs };
                      const hasRangeWarning = !range.isAllPages && (range.startPage < 1 || range.endPage > totalPgs || range.startPage > range.endPage);

                      return (
                        <div
                          key={doc.id}
                          className={`rounded-xl border transition text-[11px] overflow-hidden ${
                            isChecked
                              ? 'bg-[#1C1C22] text-white border-[#FF2D8D]/40'
                              : isReady ? 'bg-[#121215] text-[#A1A1AA] border-white/5' : 'bg-black/30 text-zinc-600 border-white/5'
                          }`}
                        >
                          <label className="flex items-center gap-2.5 p-2.5 cursor-pointer">
                            <input
                              type="checkbox"
                              checked={isChecked}
                              disabled={!isReady}
                              onChange={() => {
                                if (isChecked) {
                                  setSelectedDocIds(selectedDocIds.filter((id) => id !== doc.id));
                                } else {
                                  setSelectedDocIds([...selectedDocIds, doc.id]);
                                  if (!docPageRanges[doc.id]) {
                                    setDocPageRanges((prev) => ({
                                      ...prev,
                                      [doc.id]: { isAllPages: true, startPage: 1, endPage: totalPgs },
                                    }));
                                  }
                                }
                              }}
                              className="accent-[#FF2D8D] rounded"
                            />
                            <span className="truncate flex-1 font-medium text-white">
                              {doc.title || doc.originalFilename}
                            </span>
                            <span className="text-[10px] px-1.5 py-0.5 rounded bg-white/5 text-[#C7F000] font-mono">
                              {totalPgs} pgs
                            </span>
                            <span className="text-[10px] px-1.5 py-0.5 rounded bg-white/5 text-zinc-400 font-mono">
                              {isReady ? (doc.subject || 'General') : 'Processing'}
                            </span>
                          </label>

                          {/* Targeted Page Range Controls */}
                          {isChecked && isReady && (
                            <div className="px-3 pb-3 pt-1 border-t border-white/5 bg-[#17171C] space-y-2">
                              <div className="flex items-center gap-3 text-[11px]">
                                <label className="flex items-center gap-1.5 cursor-pointer text-zinc-300">
                                  <input
                                    type="radio"
                                    name={`range-mode-${doc.id}`}
                                    checked={range.isAllPages}
                                    onChange={() =>
                                      setDocPageRanges((prev) => ({
                                        ...prev,
                                        [doc.id]: { ...range, isAllPages: true, startPage: 1, endPage: totalPgs },
                                      }))
                                    }
                                    className="accent-[#FF2D8D]"
                                  />
                                  <span>Entire Document (Pages 1–{totalPgs})</span>
                                </label>
                                <label className="flex items-center gap-1.5 cursor-pointer text-zinc-300">
                                  <input
                                    type="radio"
                                    name={`range-mode-${doc.id}`}
                                    checked={!range.isAllPages}
                                    onChange={() =>
                                      setDocPageRanges((prev) => ({
                                        ...prev,
                                        [doc.id]: { ...range, isAllPages: false },
                                      }))
                                    }
                                    className="accent-[#FF2D8D]"
                                  />
                                  <span className="text-[#FF2D8D] font-medium">Custom Page Range</span>
                                </label>
                              </div>

                              {!range.isAllPages && (
                                <div className="space-y-1">
                                  <div className="flex items-center gap-2">
                                    <span className="text-zinc-400 text-[10px]">Revise From:</span>
                                    <input
                                      type="number"
                                      min={1}
                                      max={totalPgs}
                                      value={range.startPage}
                                      onChange={(e) => {
                                        const val = parseInt(e.target.value) || 1;
                                        setDocPageRanges((prev) => ({
                                          ...prev,
                                          [doc.id]: { ...range, startPage: val },
                                        }));
                                      }}
                                      className="w-16 bg-[#09090B] border border-white/10 rounded-lg px-2 py-1 text-center text-white text-[11px] focus:outline-none focus:border-[#FF2D8D]"
                                    />
                                    <span className="text-zinc-400 text-[10px]">To:</span>
                                    <input
                                      type="number"
                                      min={1}
                                      max={totalPgs}
                                      value={range.endPage}
                                      onChange={(e) => {
                                        const val = parseInt(e.target.value) || totalPgs;
                                        setDocPageRanges((prev) => ({
                                          ...prev,
                                          [doc.id]: { ...range, endPage: val },
                                        }));
                                      }}
                                      className="w-16 bg-[#09090B] border border-white/10 rounded-lg px-2 py-1 text-center text-white text-[11px] focus:outline-none focus:border-[#FF2D8D]"
                                    />
                                    <span className="text-zinc-500 text-[10px]">of {totalPgs} pages</span>
                                  </div>

                                  {hasRangeWarning && (
                                    <p className="text-[10px] text-rose-400 font-medium">
                                      * Page range must be within 1 to {totalPgs}, and start page cannot exceed end page.
                                    </p>
                                  )}
                                </div>
                              )}
                            </div>
                          )}
                        </div>
                      );
                    })
                  )}
                </div>
                <p className="text-[11px] text-[#A1A1AA] mt-1">
                  {selectedDocIds.length} of {readyDocs.length} ready materials selected.
                </p>
              </div>

              <div>
                <label className="block text-[#A1A1AA] font-medium mb-1">Concept Focus (Optional)</label>
                <input
                  type="text"
                  placeholder="e.g. Memory Management, or leave blank for full subject coverage"
                  value={quizForm.conceptFocus}
                  onChange={(e) => setQuizForm({ ...quizForm, conceptFocus: e.target.value })}
                  className="w-full bg-[#202025] border border-white/10 rounded-xl px-3.5 py-2.5 text-white focus:outline-none focus:border-[#FF2D8D]"
                />
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-[#A1A1AA] font-medium mb-1">Difficulty</label>
                  <select
                    value={quizForm.difficultyLevel}
                    onChange={(e) => setQuizForm({ ...quizForm, difficultyLevel: e.target.value as any })}
                    className="w-full bg-[#202025] border border-white/10 rounded-xl px-3.5 py-2.5 text-white focus:outline-none focus:border-[#FF2D8D]"
                  >
                    <option value="EASY">Easy</option>
                    <option value="MEDIUM">Medium</option>
                    <option value="HARD">Hard</option>
                  </select>
                </div>

                <div>
                  <label className="block text-[#A1A1AA] font-medium mb-1">Question Count</label>
                  <select
                    value={quizForm.questionCount}
                    onChange={(e) => setQuizForm({ ...quizForm, questionCount: parseInt(e.target.value) })}
                    className="w-full bg-[#202025] border border-white/10 rounded-xl px-3.5 py-2.5 text-white focus:outline-none focus:border-[#FF2D8D]"
                  >
                    <option value="3">3 Questions</option>
                    <option value="5">5 Questions</option>
                    <option value="10">10 Questions</option>
                  </select>
                </div>
              </div>

              <div className="pt-3 border-t border-white/10 flex justify-end gap-2.5">
                <button
                  type="button"
                  onClick={() => setIsGenerateQuizModalOpen(false)}
                  className="px-4 py-2 text-[#A1A1AA] hover:text-white rounded-xl"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={actionLoading}
                  className="btn-chartreuse inline-flex items-center gap-1.5"
                >
                  {actionLoading ? <Loader2 className="w-4 h-4 animate-spin text-black" /> : null}
                  <span>Generate Quiz</span>
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* ------------------------------------------------------------- */}
      {/* MODAL 2: Generate AI Flashcards */}
      {/* ------------------------------------------------------------- */}
      {isGenerateCardsModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/80 backdrop-blur-md p-4">
          <div className="bg-[#171719] border border-white/10 rounded-3xl max-w-lg w-full p-6 space-y-5 shadow-2xl text-[#F5F5F5]">
            <div className="flex items-center justify-between border-b border-white/10 pb-4">
              <h3 className="text-base font-semibold text-white flex items-center gap-2">
                <Sparkles className="w-4 h-4 text-[#FF2D8D]" /> Generate Flashcard Deck
              </h3>
              <button
                onClick={() => setIsGenerateCardsModalOpen(false)}
                className="text-[#A1A1AA] hover:text-white text-xs"
              >
                Cancel
              </button>
            </div>

            <form onSubmit={handleGenerateCards} className="space-y-4 text-xs">
              <div>
                <label className="block text-[#A1A1AA] font-medium mb-1">Subject / Category</label>
                <select
                  value={selectedSubject}
                  onChange={(e) => {
                    const subj = e.target.value;
                    setSelectedSubject(subj);
                    if (subj === 'ALL') {
                      setSelectedDocIds(workspaceDocs.filter((d) => d.processingStatus === 'PROCESSED').map((d) => d.id));
                    } else {
                      setSelectedDocIds(
                        workspaceDocs
                          .filter((d) => (d.subject || 'General') === subj && d.processingStatus === 'PROCESSED')
                          .map((d) => d.id)
                      );
                    }
                  }}
                  className="w-full bg-[#202025] border border-white/10 rounded-xl px-3.5 py-2.5 text-white focus:outline-none focus:border-[#FF2D8D]"
                >
                  <option value="ALL">All Subjects ({workspaceDocs.length} materials)</option>
                  {availableSubjects.map((subj) => (
                    <option key={subj} value={subj}>
                      {subj} ({workspaceDocs.filter((d) => (d.subject || 'General') === subj).length} materials)
                    </option>
                  ))}
                </select>
              </div>

              {/* Material Selection Checkboxes */}
              <div>
                <div className="flex items-center justify-between mb-1.5">
                  <label className="text-[#A1A1AA] font-medium">Select Materials to Include</label>
                  <div className="flex gap-2 text-[11px]">
                    <button
                      type="button"
                      onClick={() => setSelectedDocIds(readyDocs.map((d) => d.id))}
                      className="text-[#C7F000] hover:underline"
                    >
                      Select All
                    </button>
                    <button
                      type="button"
                      onClick={() => setSelectedDocIds([])}
                      className="text-[#A1A1AA] hover:text-white"
                    >
                      Clear
                    </button>
                  </div>
                </div>
                <div className="max-h-56 overflow-y-auto space-y-2 p-2 bg-[#09090B] rounded-xl border border-white/10">
                  {filteredDocs.length === 0 ? (
                    <p className="text-[#A1A1AA] text-center py-2 text-[11px]">
                      No documents found for this subject. Upload notes to your workspace.
                    </p>
                  ) : (
                    filteredDocs.map((doc) => {
                      const isChecked = selectedDocIds.includes(doc.id);
                      const isReady = doc.processingStatus === 'PROCESSED';
                      const totalPgs = doc.pageCount && doc.pageCount > 0 ? doc.pageCount : 1;
                      const range = docPageRanges[doc.id] || { isAllPages: true, startPage: 1, endPage: totalPgs };
                      const hasRangeWarning = !range.isAllPages && (range.startPage < 1 || range.endPage > totalPgs || range.startPage > range.endPage);

                      return (
                        <div
                          key={doc.id}
                          className={`rounded-xl border transition text-[11px] overflow-hidden ${
                            isChecked
                              ? 'bg-[#1C1C22] text-white border-[#FF2D8D]/40'
                              : isReady ? 'bg-[#121215] text-[#A1A1AA] border-white/5' : 'bg-black/30 text-zinc-600 border-white/5'
                          }`}
                        >
                          <label className="flex items-center gap-2.5 p-2.5 cursor-pointer">
                            <input
                              type="checkbox"
                              checked={isChecked}
                              disabled={!isReady}
                              onChange={() => {
                                if (isChecked) {
                                  setSelectedDocIds(selectedDocIds.filter((id) => id !== doc.id));
                                } else {
                                  setSelectedDocIds([...selectedDocIds, doc.id]);
                                  if (!docPageRanges[doc.id]) {
                                    setDocPageRanges((prev) => ({
                                      ...prev,
                                      [doc.id]: { isAllPages: true, startPage: 1, endPage: totalPgs },
                                    }));
                                  }
                                }
                              }}
                              className="accent-[#FF2D8D] rounded"
                            />
                            <span className="truncate flex-1 font-medium text-white">
                              {doc.title || doc.originalFilename}
                            </span>
                            <span className="text-[10px] px-1.5 py-0.5 rounded bg-white/5 text-[#C7F000] font-mono">
                              {totalPgs} pgs
                            </span>
                            <span className="text-[10px] px-1.5 py-0.5 rounded bg-white/5 text-zinc-400 font-mono">
                              {isReady ? (doc.subject || 'General') : 'Processing'}
                            </span>
                          </label>

                          {/* Targeted Page Range Controls */}
                          {isChecked && isReady && (
                            <div className="px-3 pb-3 pt-1 border-t border-white/5 bg-[#17171C] space-y-2">
                              <div className="flex items-center gap-3 text-[11px]">
                                <label className="flex items-center gap-1.5 cursor-pointer text-zinc-300">
                                  <input
                                    type="radio"
                                    name={`range-mode-${doc.id}`}
                                    checked={range.isAllPages}
                                    onChange={() =>
                                      setDocPageRanges((prev) => ({
                                        ...prev,
                                        [doc.id]: { ...range, isAllPages: true, startPage: 1, endPage: totalPgs },
                                      }))
                                    }
                                    className="accent-[#FF2D8D]"
                                  />
                                  <span>Entire Document (Pages 1–{totalPgs})</span>
                                </label>
                                <label className="flex items-center gap-1.5 cursor-pointer text-zinc-300">
                                  <input
                                    type="radio"
                                    name={`range-mode-${doc.id}`}
                                    checked={!range.isAllPages}
                                    onChange={() =>
                                      setDocPageRanges((prev) => ({
                                        ...prev,
                                        [doc.id]: { ...range, isAllPages: false },
                                      }))
                                    }
                                    className="accent-[#FF2D8D]"
                                  />
                                  <span className="text-[#FF2D8D] font-medium">Custom Page Range</span>
                                </label>
                              </div>

                              {!range.isAllPages && (
                                <div className="space-y-1">
                                  <div className="flex items-center gap-2">
                                    <span className="text-zinc-400 text-[10px]">Revise From:</span>
                                    <input
                                      type="number"
                                      min={1}
                                      max={totalPgs}
                                      value={range.startPage}
                                      onChange={(e) => {
                                        const val = parseInt(e.target.value) || 1;
                                        setDocPageRanges((prev) => ({
                                          ...prev,
                                          [doc.id]: { ...range, startPage: val },
                                        }));
                                      }}
                                      className="w-16 bg-[#09090B] border border-white/10 rounded-lg px-2 py-1 text-center text-white text-[11px] focus:outline-none focus:border-[#FF2D8D]"
                                    />
                                    <span className="text-zinc-400 text-[10px]">To:</span>
                                    <input
                                      type="number"
                                      min={1}
                                      max={totalPgs}
                                      value={range.endPage}
                                      onChange={(e) => {
                                        const val = parseInt(e.target.value) || totalPgs;
                                        setDocPageRanges((prev) => ({
                                          ...prev,
                                          [doc.id]: { ...range, endPage: val },
                                        }));
                                      }}
                                      className="w-16 bg-[#09090B] border border-white/10 rounded-lg px-2 py-1 text-center text-white text-[11px] focus:outline-none focus:border-[#FF2D8D]"
                                    />
                                    <span className="text-zinc-500 text-[10px]">of {totalPgs} pages</span>
                                  </div>

                                  {hasRangeWarning && (
                                    <p className="text-[10px] text-rose-400 font-medium">
                                      * Page range must be within 1 to {totalPgs}, and start page cannot exceed end page.
                                    </p>
                                  )}
                                </div>
                              )}
                            </div>
                          )}
                        </div>
                      );
                    })
                  )}
                </div>
                <p className="text-[11px] text-[#A1A1AA] mt-1">
                  {selectedDocIds.length} of {readyDocs.length} ready materials selected.
                </p>
              </div>

              <div>
                <label className="block text-[#A1A1AA] font-medium mb-1">Concept / Focus (Optional)</label>
                <input
                  type="text"
                  placeholder="e.g. Asymptotic Complexity and Big-O, or leave blank for full coverage"
                  value={cardGenForm.conceptFocus}
                  onChange={(e) => setCardGenForm({ ...cardGenForm, conceptFocus: e.target.value })}
                  className="w-full bg-[#202025] border border-white/10 rounded-xl px-3.5 py-2.5 text-white focus:outline-none focus:border-[#FF2D8D]"
                />
              </div>

              <div>
                <label className="block text-[#A1A1AA] font-medium mb-1">Card Count</label>
                <select
                  value={cardGenForm.cardCount}
                  onChange={(e) => setCardGenForm({ ...cardGenForm, cardCount: parseInt(e.target.value) })}
                  className="w-full bg-[#202025] border border-white/10 rounded-xl px-3.5 py-2.5 text-white focus:outline-none focus:border-[#FF2D8D]"
                >
                  <option value="5">5 Cards</option>
                  <option value="10">10 Cards</option>
                  <option value="15">15 Cards</option>
                </select>
              </div>

              <div className="pt-3 border-t border-white/10 flex justify-end gap-2.5">
                <button
                  type="button"
                  onClick={() => setIsGenerateCardsModalOpen(false)}
                  className="px-4 py-2 text-[#A1A1AA] hover:text-white rounded-xl"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={actionLoading}
                  className="btn-chartreuse inline-flex items-center gap-1.5"
                >
                  {actionLoading ? <Loader2 className="w-4 h-4 animate-spin text-black" /> : null}
                  <span>Generate Deck</span>
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* ------------------------------------------------------------- */}
      {/* MODAL 3: Create Manual Deck */}
      {/* ------------------------------------------------------------- */}
      {isCreateDeckModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/80 backdrop-blur-md p-4">
          <div className="bg-[#171719] border border-white/10 rounded-3xl max-w-md w-full p-6 space-y-4 shadow-2xl text-[#F5F5F5]">
            <h3 className="text-base font-semibold text-white">Create Flashcard Deck</h3>
            <form onSubmit={handleCreateManualDeck} className="space-y-3.5 text-xs">
              <div>
                <label className="block text-[#A1A1AA] font-medium mb-1">Deck Title</label>
                <input
                  type="text"
                  required
                  placeholder="e.g. Computer Networks Fundamentals"
                  value={newDeckForm.title}
                  onChange={(e) => setNewDeckForm({ ...newDeckForm, title: e.target.value })}
                  className="w-full bg-[#202025] border border-white/10 rounded-xl px-3 py-2 text-white focus:outline-none focus:border-[#FF2D8D]"
                />
              </div>

              <div>
                <label className="block text-[#A1A1AA] font-medium mb-1">Description</label>
                <textarea
                  rows={2}
                  placeholder="Deck overview..."
                  value={newDeckForm.description || ''}
                  onChange={(e) => setNewDeckForm({ ...newDeckForm, description: e.target.value })}
                  className="w-full bg-[#202025] border border-white/10 rounded-xl px-3 py-2 text-white focus:outline-none focus:border-[#FF2D8D]"
                />
              </div>

              <div className="pt-2 flex justify-end gap-2.5">
                <button
                  type="button"
                  onClick={() => setIsCreateDeckModalOpen(false)}
                  className="px-3 py-1.5 text-[#A1A1AA] hover:text-white"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={actionLoading}
                  className="btn-chartreuse px-4 py-2"
                >
                  <span>Create Deck</span>
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* ------------------------------------------------------------- */}
      {/* MODAL 4: Add Manual Card */}
      {/* ------------------------------------------------------------- */}
      {isAddCardModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/80 backdrop-blur-md p-4">
          <div className="bg-[#171719] border border-white/10 rounded-3xl max-w-md w-full p-6 space-y-4 shadow-2xl text-[#F5F5F5]">
            <h3 className="text-base font-semibold text-white">Add Card to Deck</h3>
            <form onSubmit={handleAddManualCard} className="space-y-3.5 text-xs">
              <div>
                <label className="block text-[#A1A1AA] font-medium mb-1">Front (Question / Prompt)</label>
                <textarea
                  required
                  rows={2}
                  placeholder="What is Dijkstra's algorithm used for?"
                  value={newCardForm.question}
                  onChange={(e) => setNewCardForm({ ...newCardForm, question: e.target.value })}
                  className="w-full bg-[#202025] border border-white/10 rounded-xl px-3 py-2 text-white focus:outline-none focus:border-[#FF2D8D]"
                />
              </div>

              <div>
                <label className="block text-[#A1A1AA] font-medium mb-1">Back (Answer)</label>
                <textarea
                  required
                  rows={3}
                  placeholder="Single-source shortest path algorithm on graphs with non-negative edge weights."
                  value={newCardForm.answer}
                  onChange={(e) => setNewCardForm({ ...newCardForm, answer: e.target.value })}
                  className="w-full bg-[#202025] border border-white/10 rounded-xl px-3 py-2 text-white focus:outline-none focus:border-[#FF2D8D]"
                />
              </div>

              <div>
                <label className="block text-[#A1A1AA] font-medium mb-1">Source Citation (Optional)</label>
                <input
                  type="text"
                  placeholder="e.g. Algorithms Textbook, Chapter 24"
                  value={newCardForm.sourceCitation || ''}
                  onChange={(e) => setNewCardForm({ ...newCardForm, sourceCitation: e.target.value })}
                  className="w-full bg-[#202025] border border-white/10 rounded-xl px-3 py-2 text-white focus:outline-none focus:border-[#FF2D8D]"
                />
              </div>

              <div className="pt-2 flex justify-end gap-2.5">
                <button
                  type="button"
                  onClick={() => setIsAddCardModalOpen(false)}
                  className="px-3 py-1.5 text-[#A1A1AA] hover:text-white"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={actionLoading}
                  className="btn-chartreuse px-4 py-2"
                >
                  <span>Add Card</span>
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
