import React, { useState, useEffect, useCallback, useRef } from 'react';
import { useAuth } from '@/context/AuthContext';
import { useWorkspace } from '@/context/WorkspaceContext';
import { documentService } from '@/services/documentService';
import { chatService } from '@/services/chatService';
import { learningService } from '@/services/learningService';
import { DocumentItem } from '@/types/document';
import { Conversation } from '@/types/chat';
import { LearningAnalyticsOverview, DailyActivity } from '@/types';
import { DocumentStatusBadge } from '@/components/documents/DocumentStatusBadge';
import { DocumentUploadDialog } from '@/components/documents/DocumentUploadDialog';
import {
  FileText,
  Search,
  Bot,
  Upload,
  CheckCircle2,
  Clock,
  AlertTriangle,
  Download,
  MessageSquare,
  Sparkles,
  BookOpen,
  ChevronRight,
  TrendingUp,
  Brain,
  Zap,
} from 'lucide-react';

interface DashboardPageProps {
  onNavigate: (route: 'documents' | 'search' | 'tutor' | 'learning') => void;
  onSelectConversation?: (conversationId: string) => void;
}

export const DashboardPage: React.FC<DashboardPageProps> = ({
  onNavigate,
  onSelectConversation,
}) => {
  const { user } = useAuth();
  const { currentWorkspace } = useWorkspace();

  const [documents, setDocuments] = useState<DocumentItem[]>([]);
  const [conversations, setConversations] = useState<Conversation[]>([]);
  const [analytics, setAnalytics] = useState<LearningAnalyticsOverview | null>(null);
  const [activity, setActivity] = useState<DailyActivity[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [isUploadOpen, setIsUploadOpen] = useState(false);

  // Stale request guard ref
  const activeWorkspaceIdRef = useRef<string | null>(null);

  const loadDashboardData = useCallback(async () => {
    const targetWorkspaceId = currentWorkspace?.id;
    if (!targetWorkspaceId) return;
    setIsLoading(true);

    try {
      const [docsRes, convsRes, analyticsRes, activityRes] = await Promise.allSettled([
        documentService.listDocuments({
          workspaceId: targetWorkspaceId,
          page: 0,
          size: 50,
        }),
        chatService.listConversations(targetWorkspaceId, 0, 5),
        learningService.getAnalyticsOverview(),
        learningService.getActivityHistory(7),
      ]);

      if (activeWorkspaceIdRef.current === targetWorkspaceId) {
        if (docsRes.status === 'fulfilled') {
          setDocuments(docsRes.value.content || []);
        }
        if (convsRes.status === 'fulfilled') {
          setConversations(convsRes.value.content || []);
        }
        if (analyticsRes.status === 'fulfilled') {
          setAnalytics(analyticsRes.value);
        }
        if (activityRes.status === 'fulfilled') {
          setActivity(activityRes.value || []);
        }
      }
    } catch (err: unknown) {
      if (activeWorkspaceIdRef.current === targetWorkspaceId) {
        console.error('Failed to load dashboard data:', err);
      }
    } finally {
      if (activeWorkspaceIdRef.current === targetWorkspaceId) {
        setIsLoading(false);
      }
    }
  }, [currentWorkspace?.id]);

  useEffect(() => {
    const currentId = currentWorkspace?.id || null;
    activeWorkspaceIdRef.current = currentId;
    setDocuments([]);
    setConversations([]);
    setIsLoading(true);

    if (currentId) {
      loadDashboardData();
    }
  }, [currentWorkspace?.id, loadDashboardData]);

  // Metric aggregates
  const totalDocs = documents.length;
  const readyDocs = documents.filter(
    (d) =>
      d.processingStatus === 'PROCESSED' &&
      (d.embeddingStatus === 'EMBEDDED' || d.embeddingStatus === 'COMPLETED')
  ).length;
  const inProgressDocs = documents.filter(
    (d) =>
      d.processingStatus === 'PROCESSING' ||
      d.processingStatus === 'PENDING_PROCESSING' ||
      d.processingStatus === 'UPLOADED' ||
      (d.processingStatus === 'PROCESSED' &&
        d.embeddingStatus !== 'EMBEDDED' &&
        d.embeddingStatus !== 'COMPLETED' &&
        d.embeddingStatus !== 'FAILED')
  ).length;
  const failedDocs = documents.filter(
    (d) => d.processingStatus === 'FAILED' || d.embeddingStatus === 'FAILED'
  ).length;

  const recentDocuments = documents.slice(0, 5);

  const handleDownload = async (doc: DocumentItem) => {
    try {
      const res = await documentService.getDownloadUrl(doc.id);
      if (res && res.downloadUrl) {
        window.open(res.downloadUrl, '_blank', 'noopener,noreferrer');
      }
    } catch (err: unknown) {
      console.error('Download error:', err);
    }
  };

  const formatFileSize = (bytes: number): string => {
    if (bytes < 1024) return `${bytes} B`;
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
    return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
  };

  // Readiness ratio for SVG ring
  const readinessPercent = totalDocs > 0 ? Math.round((readyDocs / totalDocs) * 100) : 0;
  const ringCircumference = 2 * Math.PI * 40;
  const ringOffset = ringCircumference - (readinessPercent / 100) * ringCircumference;

  return (
    <div className="p-6 sm:p-8 space-y-8 max-w-7xl mx-auto text-[#F5F5F5]">
      {/* Welcome Hero Banner */}
      <div className="relative overflow-hidden rounded-3xl bg-gradient-to-br from-[#171719] via-[#111114] to-[#09090B] border border-white/10 p-8 sm:p-10 shadow-2xl">
        <div className="absolute top-0 right-0 w-96 h-96 bg-[#FF2D8D]/10 rounded-full blur-[100px] pointer-events-none" />
        <div className="absolute bottom-0 left-1/3 w-80 h-80 bg-[#C7F000]/10 rounded-full blur-[100px] pointer-events-none" />

        <div className="relative z-10 max-w-3xl">
          <div className="inline-flex items-center gap-2 px-3.5 py-1.5 rounded-full bg-[#FF2D8D]/10 border border-[#FF2D8D]/30 text-[#FF2D8D] text-xs font-semibold mb-4">
            <Sparkles className="w-3.5 h-3.5" />
            <span>Workspace: {currentWorkspace?.name || 'Personal Workspace'}</span>
          </div>

          <h1 className="text-3xl sm:text-4xl lg:text-5xl font-serif tracking-tight text-white font-normal">
            Welcome back, <span className="text-[#F5F5F5] font-semibold">{user?.fullName || 'Student'}</span>
          </h1>

          <p className="text-sm sm:text-base text-[#A1A1AA] mt-3 leading-relaxed max-w-2xl font-light">
            Your personal study workspace is ready. Search verified passages, consult your QUORIVA AI Tutor with exact citations, or practice with quizzes and flashcards.
          </p>

          <div className="flex flex-wrap items-center gap-3.5 mt-7">
            <button
              type="button"
              onClick={() => setIsUploadOpen(true)}
              className="btn-chartreuse inline-flex items-center gap-2"
            >
              <Upload className="w-4 h-4 text-black" />
              <span>Upload Material</span>
            </button>

            <button
              type="button"
              onClick={() => onNavigate('search')}
              className="inline-flex items-center gap-2 px-5 py-2.5 rounded-full bg-[#202025] hover:bg-[#2A2A32] text-white font-medium text-xs border border-white/10 transition shadow-sm"
            >
              <Search className="w-4 h-4 text-[#C7F000]" />
              <span>Academic Search</span>
            </button>

            <button
              type="button"
              onClick={() => onNavigate('tutor')}
              className="inline-flex items-center gap-2 px-5 py-2.5 rounded-full bg-[#202025] hover:bg-[#2A2A32] text-white font-medium text-xs border border-white/10 transition shadow-sm"
            >
              <Bot className="w-4 h-4 text-[#FF2D8D]" />
              <span>Ask AI Tutor</span>
            </button>

            <button
              type="button"
              onClick={() => onNavigate('learning')}
              className="inline-flex items-center gap-2 px-5 py-2.5 rounded-full bg-[#FF2D8D]/10 hover:bg-[#FF2D8D]/20 text-[#FF2D8D] font-medium text-xs border border-[#FF2D8D]/30 transition shadow-sm"
            >
              <Brain className="w-4 h-4 text-[#FF2D8D]" />
              <span>Learning Studio</span>
            </button>
          </div>
        </div>
      </div>

      {/* 4 Quick Stat Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        {/* Total Documents */}
        <div className="bg-[#171719] p-5 rounded-2xl border border-white/10 shadow-lg flex items-center justify-between group hover:border-white/20 transition">
          <div>
            <p className="text-[11px] font-semibold text-[#A1A1AA] uppercase tracking-wider">
              Total Materials
            </p>
            <p className="text-2xl font-bold text-white mt-1">
              {isLoading ? '...' : totalDocs}
            </p>
            <p className="text-[11px] text-[#A1A1AA] mt-0.5">Across this workspace</p>
          </div>
          <div className="w-12 h-12 rounded-xl bg-white/5 border border-white/10 text-white flex items-center justify-center">
            <FileText className="w-5 h-5 text-white" />
          </div>
        </div>

        {/* Ready for AI Search */}
        <div className="bg-[#171719] p-5 rounded-2xl border border-white/10 shadow-lg flex items-center justify-between group hover:border-[#C7F000]/40 transition">
          <div>
            <p className="text-[11px] font-semibold text-[#A1A1AA] uppercase tracking-wider">
              Ready for AI Search
            </p>
            <p className="text-2xl font-bold text-[#C7F000] mt-1">
              {isLoading ? '...' : readyDocs}
            </p>
            <p className="text-[11px] text-[#A1A1AA] mt-0.5">Dense & sparse indexed</p>
          </div>
          <div className="w-12 h-12 rounded-xl bg-[#C7F000]/10 border border-[#C7F000]/20 text-[#C7F000] flex items-center justify-center">
            <CheckCircle2 className="w-5 h-5 text-[#C7F000]" />
          </div>
        </div>

        {/* Ingestion in Progress */}
        <div className="bg-[#171719] p-5 rounded-2xl border border-white/10 shadow-lg flex items-center justify-between group hover:border-[#FF2D8D]/40 transition">
          <div>
            <p className="text-[11px] font-semibold text-[#A1A1AA] uppercase tracking-wider">
              Processing Pipeline
            </p>
            <p className="text-2xl font-bold text-[#FF2D8D] mt-1">
              {isLoading ? '...' : inProgressDocs}
            </p>
            <p className="text-[11px] text-[#A1A1AA] mt-0.5">Extracting & embedding</p>
          </div>
          <div className="w-12 h-12 rounded-xl bg-[#FF2D8D]/10 border border-[#FF2D8D]/20 text-[#FF2D8D] flex items-center justify-center">
            <Clock className="w-5 h-5 text-[#FF2D8D]" />
          </div>
        </div>

        {/* Failed Pipeline */}
        <div className="bg-[#171719] p-5 rounded-2xl border border-white/10 shadow-lg flex items-center justify-between group hover:border-rose-500/40 transition">
          <div>
            <p className="text-[11px] font-semibold text-[#A1A1AA] uppercase tracking-wider">
              Failed Pipeline
            </p>
            <p className="text-2xl font-bold text-rose-400 mt-1">
              {isLoading ? '...' : failedDocs}
            </p>
            <p className="text-[11px] text-[#A1A1AA] mt-0.5">Requires re-upload</p>
          </div>
          <div className="w-12 h-12 rounded-xl bg-rose-500/10 border border-rose-500/20 text-rose-400 flex items-center justify-center">
            <AlertTriangle className="w-5 h-5 text-rose-400" />
          </div>
        </div>
      </div>

      {/* Interactive Charts Section (Real Backend Data) */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Knowledge Base Readiness Donut */}
        <div className="bg-[#171719] rounded-2xl border border-white/10 p-6 flex flex-col justify-between shadow-xl">
          <div className="flex items-center justify-between">
            <div>
              <h3 className="text-sm font-semibold text-white">Knowledge Readiness</h3>
              <p className="text-xs text-[#A1A1AA]">Material vector indexing ratio</p>
            </div>
            <span className="p-2 rounded-xl bg-white/5 border border-white/10 text-[#C7F000]">
              <Zap className="w-4 h-4" />
            </span>
          </div>

          <div className="my-6 flex items-center justify-center">
            <div className="relative w-36 h-36 flex items-center justify-center">
              <svg className="w-full h-full transform -rotate-90" viewBox="0 0 100 100">
                <circle
                  cx="50"
                  cy="50"
                  r="40"
                  fill="transparent"
                  stroke="#202025"
                  strokeWidth="10"
                />
                <circle
                  cx="50"
                  cy="50"
                  r="40"
                  fill="transparent"
                  stroke="#C7F000"
                  strokeWidth="10"
                  strokeDasharray={ringCircumference}
                  strokeDashoffset={ringOffset}
                  strokeLinecap="round"
                  className="transition-all duration-1000 ease-out"
                />
              </svg>
              <div className="absolute inset-0 flex flex-col items-center justify-center text-center">
                <span className="text-2xl font-bold text-white">{readinessPercent}%</span>
                <span className="text-[10px] text-[#A1A1AA] font-mono uppercase">Indexed</span>
              </div>
            </div>
          </div>

          <div className="grid grid-cols-3 gap-2 text-center pt-3 border-t border-white/10 text-xs">
            <div>
              <p className="text-white font-semibold">{readyDocs}</p>
              <p className="text-[10px] text-[#C7F000]">Active</p>
            </div>
            <div>
              <p className="text-white font-semibold">{inProgressDocs}</p>
              <p className="text-[10px] text-[#FF2D8D]">Queue</p>
            </div>
            <div>
              <p className="text-white font-semibold">{failedDocs}</p>
              <p className="text-[10px] text-rose-400">Failed</p>
            </div>
          </div>
        </div>

        {/* 7-Day Learning Activity Bar Chart */}
        <div className="lg:col-span-2 bg-[#171719] rounded-2xl border border-white/10 p-6 flex flex-col justify-between shadow-xl">
          <div className="flex items-center justify-between mb-4">
            <div>
              <h3 className="text-sm font-semibold text-white">Study Activity (Past 7 Days)</h3>
              <p className="text-xs text-[#A1A1AA]">
                Quizzes attempted & flashcard revisions recorded
              </p>
            </div>
            <div className="flex items-center gap-3 text-xs">
              <span className="flex items-center gap-1.5 text-xs text-[#A1A1AA]">
                <span className="w-2.5 h-2.5 rounded-sm bg-[#FF2D8D]" /> Quizzes
              </span>
              <span className="flex items-center gap-1.5 text-xs text-[#A1A1AA]">
                <span className="w-2.5 h-2.5 rounded-sm bg-[#C7F000]" /> Cards
              </span>
            </div>
          </div>

          {/* Activity Bar Chart Render */}
          {activity.length === 0 ? (
            <div className="h-44 flex flex-col items-center justify-center text-center p-6 border border-dashed border-white/10 rounded-xl bg-[#202025]/50">
              <TrendingUp className="w-8 h-8 text-[#A1A1AA] mb-2 opacity-60" />
              <p className="text-xs font-medium text-white">No activity logged in this period</p>
              <p className="text-[11px] text-[#A1A1AA] mt-0.5">
                Practice quizzes or review flashcards in Learning Studio to track retention.
              </p>
              <button
                type="button"
                onClick={() => onNavigate('learning')}
                className="mt-3 text-xs font-semibold text-[#FF2D8D] hover:underline"
              >
                Go to Learning Studio →
              </button>
            </div>
          ) : (
            <div className="h-44 flex items-end gap-3 sm:gap-6 pt-6 pb-2 px-2">
              {activity.slice(-7).map((day, idx) => {
                const totalActivity = (day.quizzesTaken || 0) + (day.cardsReviewed || 0);
                const maxVal = Math.max(
                  ...activity.map((a) => (a.quizzesTaken || 0) + (a.cardsReviewed || 0)),
                  5
                );
                const heightPercent = Math.min(Math.round((totalActivity / maxVal) * 100), 100);
                const dateLabel = new Date(day.date).toLocaleDateString(undefined, {
                  weekday: 'short',
                });

                return (
                  <div key={idx} className="flex-1 flex flex-col items-center gap-2 h-full justify-end group">
                    <div className="w-full flex flex-col items-center justify-end h-full">
                      <div
                        className="w-full max-w-[28px] rounded-t-md transition-all duration-500 flex flex-col justify-end overflow-hidden"
                        style={{ height: `${Math.max(heightPercent, 6)}%` }}
                        title={`${day.date}: ${day.quizzesTaken || 0} quizzes, ${day.cardsReviewed || 0} cards`}
                      >
                        <div
                          className="w-full bg-[#FF2D8D]"
                          style={{
                            height: `${totalActivity > 0
                                ? ((day.quizzesTaken || 0) / totalActivity) * 100
                                : 50
                              }%`,
                          }}
                        />
                        <div
                          className="w-full bg-[#C7F000]"
                          style={{
                            height: `${totalActivity > 0
                                ? ((day.cardsReviewed || 0) / totalActivity) * 100
                                : 50
                              }%`,
                          }}
                        />
                      </div>
                    </div>
                    <span className="text-[10px] text-[#A1A1AA] font-mono group-hover:text-white transition">
                      {dateLabel}
                    </span>
                  </div>
                );
              })}
            </div>
          )}

          <div className="flex items-center justify-between pt-3 border-t border-white/10 text-xs text-[#A1A1AA]">
            <span>Due Today: <strong className="text-white">{analytics?.cardsDueToday || 0} cards</strong></span>
            <span>Mastered Cards: <strong className="text-[#C7F000]">{analytics?.graduatedCardsCount || 0}</strong></span>
            <span>Avg Quiz Score: <strong className="text-[#FF2D8D]">{analytics?.averageQuizScore ? `${Math.round(analytics.averageQuizScore)}%` : 'N/A'}</strong></span>
          </div>
        </div>
      </div>

      {/* Main Grid: Recent Documents & Recent Discussions */}
      {totalDocs === 0 && !isLoading ? (
        /* Empty State Onboarding Guide */
        <div className="bg-[#171719] p-8 sm:p-12 rounded-3xl border border-white/10 text-center max-w-2xl mx-auto shadow-2xl">
          <div className="w-16 h-16 rounded-2xl bg-[#FF2D8D]/10 border border-[#FF2D8D]/30 text-[#FF2D8D] flex items-center justify-center mx-auto mb-4">
            <BookOpen className="w-8 h-8" />
          </div>
          <h3 className="text-xl font-serif text-white font-normal">
            Start Learning in your Study Workspace
          </h3>
          <p className="text-xs sm:text-sm text-[#A1A1AA] mt-1.5 max-w-md mx-auto">
            You don&apos;t have any academic materials in this workspace yet. Get started in three easy steps:
          </p>

          <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 my-8 text-left">
            <div className="p-4 rounded-2xl bg-[#202025] border border-white/10">
              <div className="w-7 h-7 rounded-lg bg-[#C7F000] text-black font-bold text-xs flex items-center justify-center mb-2">
                1
              </div>
              <h4 className="text-xs font-bold text-white">Upload Files</h4>
              <p className="text-[11px] text-[#A1A1AA] mt-1">
                Upload lecture slides, chapters, or syllabus (PDF, DOCX, PPTX).
              </p>
            </div>

            <div className="p-4 rounded-2xl bg-[#202025] border border-white/10">
              <div className="w-7 h-7 rounded-lg bg-[#FF2D8D] text-white font-bold text-xs flex items-center justify-center mb-2">
                2
              </div>
              <h4 className="text-xs font-bold text-white">AI Ingestion</h4>
              <p className="text-[11px] text-[#A1A1AA] mt-1">
                Content is parsed and embedded automatically with dense vectors.
              </p>
            </div>

            <div className="p-4 rounded-2xl bg-[#202025] border border-white/10">
              <div className="w-7 h-7 rounded-lg bg-white/20 text-white font-bold text-xs flex items-center justify-center mb-2">
                3
              </div>
              <h4 className="text-xs font-bold text-white">Grounded Chat</h4>
              <p className="text-[11px] text-[#A1A1AA] mt-1">
                Ask questions and receive verifiable answers with exact page citations.
              </p>
            </div>
          </div>

          <button
            type="button"
            onClick={() => setIsUploadOpen(true)}
            className="btn-chartreuse inline-flex items-center gap-2"
          >
            <Upload className="w-4 h-4 text-black" />
            <span>Upload Your First Course Material</span>
          </button>
        </div>
      ) : (
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          {/* Left 2 Cols: Recent Documents */}
          <div className="lg:col-span-2 bg-[#171719] rounded-2xl border border-white/10 shadow-xl overflow-hidden">
            <div className="p-5 border-b border-white/10 flex items-center justify-between">
              <div>
                <h3 className="text-sm font-semibold text-white">Recent Materials</h3>
                <p className="text-xs text-[#A1A1AA]">Recently uploaded to this workspace</p>
              </div>
              <button
                type="button"
                onClick={() => onNavigate('documents')}
                className="text-xs font-semibold text-[#FF2D8D] hover:text-[#FF479D] inline-flex items-center gap-1 transition"
              >
                <span>View all ({totalDocs})</span>
                <ChevronRight className="w-3.5 h-3.5" />
              </button>
            </div>

            <div className="divide-y divide-white/5">
              {recentDocuments.map((doc) => (
                <div
                  key={doc.id}
                  className="p-4 flex items-center justify-between gap-4 hover:bg-white/[0.02] transition"
                >
                  <div className="flex items-center gap-3.5 min-w-0">
                    <div className="p-2.5 rounded-xl bg-white/5 text-[#FF2D8D] shrink-0 border border-white/10">
                      <FileText className="w-4 h-4" />
                    </div>
                    <div className="min-w-0">
                      <p className="text-xs font-semibold text-white truncate">
                        {doc.originalFilename || doc.originalFileName || doc.title}
                      </p>
                      <div className="flex items-center gap-2 text-[11px] text-[#A1A1AA] mt-0.5">
                        <span className="font-mono uppercase">{doc.fileType}</span>
                        <span>•</span>
                        <span>{formatFileSize(doc.fileSize || 0)}</span>
                        {doc.pageCount && doc.pageCount > 0 && (
                          <>
                            <span>•</span>
                            <span>{doc.pageCount} pgs</span>
                          </>
                        )}
                      </div>
                    </div>
                  </div>

                  <div className="flex items-center gap-3 shrink-0">
                    <DocumentStatusBadge
                      processingStatus={doc.processingStatus}
                      embeddingStatus={doc.embeddingStatus}
                      size="sm"
                    />
                    <button
                      type="button"
                      onClick={() => handleDownload(doc)}
                      className="p-1.5 text-[#A1A1AA] hover:text-[#C7F000] hover:bg-[#C7F000]/10 rounded-lg transition"
                      title="Download Original"
                    >
                      <Download className="w-4 h-4" />
                    </button>
                  </div>
                </div>
              ))}
            </div>
          </div>

          {/* Right Col: Recent Tutor Discussions */}
          <div className="bg-[#171719] rounded-2xl border border-white/10 shadow-xl overflow-hidden flex flex-col">
            <div className="p-5 border-b border-white/10 flex items-center justify-between">
              <div>
                <h3 className="text-sm font-semibold text-white">AI Tutor Discussions</h3>
                <p className="text-xs text-[#A1A1AA]">Conversations in this workspace</p>
              </div>
              <button
                type="button"
                onClick={() => onNavigate('tutor')}
                className="text-xs font-semibold text-[#FF2D8D] hover:text-[#FF479D] inline-flex items-center gap-1 transition"
              >
                <span>New</span>
                <ChevronRight className="w-3.5 h-3.5" />
              </button>
            </div>

            <div className="flex-1 divide-y divide-white/5">
              {conversations.length === 0 ? (
                <div className="p-8 text-center text-[#A1A1AA]">
                  <MessageSquare className="w-8 h-8 mx-auto mb-2 opacity-40 text-white" />
                  <p className="text-xs font-medium text-white">No discussions yet</p>
                  <button
                    type="button"
                    onClick={() => onNavigate('tutor')}
                    className="mt-3 text-xs font-semibold text-[#C7F000] hover:underline"
                  >
                    Start your first academic discussion →
                  </button>
                </div>
              ) : (
                conversations.map((conv) => (
                  <button
                    key={conv.id}
                    type="button"
                    onClick={() => {
                      if (onSelectConversation) {
                        onSelectConversation(conv.id);
                      }
                      onNavigate('tutor');
                    }}
                    className="w-full text-left p-4 hover:bg-white/[0.02] transition flex items-start gap-3 group"
                  >
                    <div className="p-2 rounded-xl bg-white/5 text-[#A1A1AA] group-hover:text-[#FF2D8D] border border-white/10 transition shrink-0">
                      <MessageSquare className="w-4 h-4" />
                    </div>
                    <div className="min-w-0 flex-1">
                      <p className="text-xs font-semibold text-white truncate group-hover:text-[#FF2D8D] transition">
                        {conv.title}
                      </p>
                      <p className="text-[11px] text-[#A1A1AA] mt-0.5">
                        {new Date(conv.lastMessageAt || conv.createdAt).toLocaleDateString(
                          undefined,
                          { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' }
                        )}
                      </p>
                    </div>
                    <ChevronRight className="w-4 h-4 text-zinc-600 group-hover:text-[#FF2D8D] transition shrink-0 mt-1" />
                  </button>
                ))
              )}
            </div>
          </div>
        </div>
      )}

      {/* Upload Dialog */}
      {currentWorkspace && (
        <DocumentUploadDialog
          isOpen={isUploadOpen}
          onClose={() => setIsUploadOpen(false)}
          workspaceId={currentWorkspace.id}
          onSuccess={(uploadedWsId) => {
            if (!uploadedWsId || uploadedWsId === currentWorkspace?.id) {
              loadDashboardData();
            }
          }}
        />
      )}
    </div>
  );
};
