import React, { useState, useEffect } from 'react';
import {
  Network,
  LayoutGrid,
  Compass,
  Search,
  Layers,
  Clock,
  AlertCircle,
  Loader2,
  RefreshCw,
  Milestone,
} from 'lucide-react';
import { knowledgeService } from '@/services/knowledgeService';
import { KnowledgeGraphCanvas } from '@/components/knowledge/KnowledgeGraphCanvas';
import { TopicDetailDrawer } from '@/components/knowledge/TopicDetailDrawer';
import { LearningPathDrawer } from '@/components/knowledge/LearningPathDrawer';
import {
  AcademicSubject,
  KnowledgeTopicSummary,
  KnowledgeGraph,
  LearningPath,
  TopicDifficulty,
} from '@/types';

interface KnowledgePageProps {
  onNavigateToTutor?: (prompt: string) => void;
  onSelectResource?: (resourceId: string) => void;
}

type ViewMode = 'graph' | 'grid' | 'paths';

const DIFFICULTY_BADGES: Record<TopicDifficulty, { badge: string; text: string }> = {
  BEGINNER: {
    badge: 'bg-emerald-500/10 text-emerald-400 border-emerald-500/30',
    text: 'Beginner',
  },
  INTERMEDIATE: {
    badge: 'bg-blue-500/10 text-blue-400 border-blue-500/30',
    text: 'Intermediate',
  },
  ADVANCED: {
    badge: 'bg-purple-500/10 text-purple-400 border-purple-500/30',
    text: 'Advanced',
  },
};

export const KnowledgePage: React.FC<KnowledgePageProps> = ({
  onNavigateToTutor,
  onSelectResource,
}) => {
  // Navigation & View mode
  const [viewMode, setViewMode] = useState<ViewMode>('graph');

  // Subjects & Filter State
  const [subjects, setSubjects] = useState<AcademicSubject[]>([]);
  const [selectedSubjectId, setSelectedSubjectId] = useState<string>('all');
  const [searchQuery, setSearchQuery] = useState<string>('');
  const [selectedDifficulty, setSelectedDifficulty] = useState<TopicDifficulty | 'ALL'>('ALL');
  const [maxGraphNodes, setMaxGraphNodes] = useState<number>(45);

  // Data States
  const [graphData, setGraphData] = useState<KnowledgeGraph | null>(null);
  const [topics, setTopics] = useState<KnowledgeTopicSummary[]>([]);
  const [curatedPaths, setCuratedPaths] = useState<LearningPath[]>([]);

  // Loading & Error States
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);

  // Drawer States
  const [detailSlug, setDetailSlug] = useState<string | null>(null);
  const [pathSlug, setPathSlug] = useState<string | null>(null);
  const [isDetailOpen, setIsDetailOpen] = useState<boolean>(false);
  const [isPathOpen, setIsPathOpen] = useState<boolean>(false);

  // Load Subjects on Mount
  useEffect(() => {
    knowledgeService
      .getSubjects()
      .then((data) => setSubjects(data))
      .catch((err) => console.error('Failed to load subjects:', err));
  }, []);

  // Fetch Graph or Topics when filters change
  const refreshData = async () => {
    setIsLoading(true);
    setError(null);
    try {
      const subjectFilter = selectedSubjectId !== 'all' ? selectedSubjectId : undefined;

      if (viewMode === 'graph') {
        const graph = await knowledgeService.getFullGraph(subjectFilter, maxGraphNodes);
        setGraphData(graph);
      } else if (viewMode === 'grid') {
        const res = await knowledgeService.searchTopics({
          subjectId: subjectFilter,
          q: searchQuery.trim() || undefined,
          difficulty: selectedDifficulty !== 'ALL' ? selectedDifficulty : undefined,
          size: 60,
        });
        setTopics(res.content || []);
      } else if (viewMode === 'paths') {
        const paths = await knowledgeService.getCuratedLearningPaths();
        setCuratedPaths(paths);
      }
    } catch (err: any) {
      console.error('Failed to fetch knowledge data:', err);
      setError(err.response?.data?.message || 'Failed to load knowledge graph data.');
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    refreshData();
  }, [viewMode, selectedSubjectId, selectedDifficulty, maxGraphNodes]);

  // Handle Search for Grid view (debounced)
  useEffect(() => {
    if (viewMode !== 'grid') return;
    const timeout = setTimeout(() => {
      refreshData();
    }, 300);
    return () => clearTimeout(timeout);
  }, [searchQuery]);

  // Drawer Handlers
  const handleOpenDetail = (slug: string) => {
    setDetailSlug(slug);
    setIsDetailOpen(true);
  };

  const handleOpenPath = (slug: string) => {
    setPathSlug(slug);
    setIsPathOpen(true);
  };

  // Expand neighborhood around a specific topic
  const handleExpandNeighborhood = async (slug: string) => {
    setIsLoading(true);
    try {
      const neighborhood = await knowledgeService.getTopicNeighborhoodGraph(slug, 1);
      setGraphData(neighborhood);
      setViewMode('graph');
    } catch (err: any) {
      console.error('Failed to expand neighborhood:', err);
    } finally {
      setIsLoading(false);
    }
  };

  // Tutor Bridge
  const handleStudyWithTutor = (topicName: string, subjectName: string) => {
    const prompt = `Explain the foundational computer science concepts of "${topicName}" (${subjectName}). Cover the theoretical intuition, core mechanisms or algorithms, space/time complexities and engineering trade-offs, common practical use cases, and how it connects to its prerequisites.`;
    if (onNavigateToTutor) {
      onNavigateToTutor(prompt);
    }
  };

  return (
    <div className="min-h-full flex flex-col bg-slate-950 text-slate-100">
      {/* Header Bar */}
      <div className="border-b border-slate-800 bg-slate-900/60 backdrop-blur px-6 py-6">
        <div className="max-w-7xl mx-auto flex flex-col md:flex-row md:items-center justify-between gap-4">
          <div>
            <div className="flex items-center gap-2 mb-1">
              <span className="p-1.5 rounded-lg bg-indigo-600/20 text-indigo-400 border border-indigo-500/30">
                <Network className="w-5 h-5" />
              </span>
              <h1 className="text-2xl font-bold tracking-tight text-white">
                CS Topic Discovery & Knowledge Graph
              </h1>
            </div>
            <p className="text-xs text-slate-400">
              Explore interconnected Computer Science concepts, navigate prerequisites, and discover learning pathways.
            </p>
          </div>

          {/* View Mode Switcher */}
          <div className="flex items-center bg-slate-950/80 border border-slate-800 rounded-lg p-1">
            <button
              onClick={() => setViewMode('graph')}
              className={`flex items-center gap-2 px-3.5 py-1.5 rounded-md text-xs font-medium transition ${
                viewMode === 'graph'
                  ? 'bg-primary-600 text-white shadow-sm'
                  : 'text-slate-400 hover:text-white'
              }`}
            >
              <Network className="w-3.5 h-3.5" />
              Interactive Graph
            </button>
            <button
              onClick={() => setViewMode('grid')}
              className={`flex items-center gap-2 px-3.5 py-1.5 rounded-md text-xs font-medium transition ${
                viewMode === 'grid'
                  ? 'bg-primary-600 text-white shadow-sm'
                  : 'text-slate-400 hover:text-white'
              }`}
            >
              <LayoutGrid className="w-3.5 h-3.5" />
              Topic Grid
            </button>
            <button
              onClick={() => setViewMode('paths')}
              className={`flex items-center gap-2 px-3.5 py-1.5 rounded-md text-xs font-medium transition ${
                viewMode === 'paths'
                  ? 'bg-primary-600 text-white shadow-sm'
                  : 'text-slate-400 hover:text-white'
              }`}
            >
              <Compass className="w-3.5 h-3.5" />
              Curated Roadmaps
            </button>
          </div>
        </div>
      </div>

      {/* Main Content Area */}
      <div className="flex-1 max-w-7xl w-full mx-auto p-6 flex flex-col space-y-6">
        {/* Filter Controls Bar */}
        <div className="flex flex-col lg:flex-row items-stretch lg:items-center justify-between gap-4 bg-slate-900/40 border border-slate-800/80 p-4 rounded-xl">
          {/* Subject Tabs */}
          <div className="flex items-center gap-1.5 overflow-x-auto pb-1 lg:pb-0 scrollbar-thin">
            <button
              onClick={() => setSelectedSubjectId('all')}
              className={`px-3 py-1.5 rounded-lg text-xs font-medium whitespace-nowrap transition border ${
                selectedSubjectId === 'all'
                  ? 'bg-primary-600 border-primary-500 text-white shadow'
                  : 'bg-slate-800/50 border-slate-700/60 text-slate-400 hover:text-slate-200 hover:bg-slate-800'
              }`}
            >
              All Subjects
            </button>
            {subjects.map((sub) => (
              <button
                key={sub.id}
                onClick={() => setSelectedSubjectId(sub.id)}
                className={`px-3 py-1.5 rounded-lg text-xs font-medium whitespace-nowrap transition border ${
                  selectedSubjectId === sub.id
                    ? 'bg-primary-600 border-primary-500 text-white shadow'
                    : 'bg-slate-800/50 border-slate-700/60 text-slate-400 hover:text-slate-200 hover:bg-slate-800'
                }`}
                title={sub.description}
              >
                {sub.name}
              </button>
            ))}
          </div>

          {/* Search & Difficulty Filter */}
          <div className="flex items-center gap-3">
            {viewMode === 'grid' && (
              <div className="relative flex-1 sm:w-64">
                <Search className="w-4 h-4 absolute left-3 top-2.5 text-slate-400" />
                <input
                  type="text"
                  placeholder="Search topics..."
                  value={searchQuery}
                  onChange={(e) => setSearchQuery(e.target.value)}
                  className="w-full bg-slate-950 border border-slate-800 rounded-lg pl-9 pr-3 py-1.5 text-xs text-white placeholder-slate-500 focus:outline-none focus:border-primary-500"
                />
              </div>
            )}

            <div className="flex items-center gap-1.5">
              <span className="text-xs text-slate-500 hidden sm:inline">Difficulty:</span>
              <select
                value={selectedDifficulty}
                onChange={(e) => setSelectedDifficulty(e.target.value as any)}
                className="bg-slate-950 border border-slate-800 rounded-lg px-2.5 py-1.5 text-xs text-slate-300 focus:outline-none focus:border-primary-500"
              >
                <option value="ALL">All Levels</option>
                <option value="BEGINNER">Beginner</option>
                <option value="INTERMEDIATE">Intermediate</option>
                <option value="ADVANCED">Advanced</option>
              </select>
            </div>

            {viewMode === 'graph' && (
              <div className="flex items-center gap-2">
                <span className="text-xs text-slate-500 hidden sm:inline">Nodes:</span>
                <select
                  value={maxGraphNodes}
                  onChange={(e) => setMaxGraphNodes(Number(e.target.value))}
                  className="bg-slate-950 border border-slate-800 rounded-lg px-2.5 py-1.5 text-xs text-slate-300 focus:outline-none focus:border-primary-500"
                >
                  <option value={30}>30</option>
                  <option value={45}>45</option>
                  <option value={60}>60</option>
                </select>
                <button
                  onClick={refreshData}
                  title="Reset to Full Graph"
                  className="p-1.5 text-slate-400 hover:text-white hover:bg-slate-800 border border-slate-800 rounded-lg transition"
                >
                  <RefreshCw className="w-4 h-4" />
                </button>
              </div>
            )}
          </div>
        </div>

        {/* Loading Spinner */}
        {isLoading && (
          <div className="py-24 flex flex-col items-center justify-center text-slate-400">
            <Loader2 className="w-9 h-9 animate-spin text-primary-400 mb-3" />
            <p className="text-sm font-medium">Loading knowledge network...</p>
          </div>
        )}

        {/* Error Alert */}
        {error && !isLoading && (
          <div className="p-4 rounded-xl bg-red-950/40 border border-red-800 text-red-300 flex items-start gap-3">
            <AlertCircle className="w-5 h-5 flex-shrink-0 mt-0.5" />
            <p className="text-sm">{error}</p>
          </div>
        )}

        {/* VIEW MODE: GRAPH */}
        {viewMode === 'graph' && !isLoading && graphData && (
          <div className="space-y-3">
            <div className="flex items-center justify-between text-xs text-slate-400 px-1">
              <span>
                Displaying <strong>{graphData.totalNodes} topics</strong> and{' '}
                <strong>{graphData.totalEdges} prerequisite/related connections</strong>
                {graphData.centerTopicName && (
                  <span className="text-amber-400 ml-2 font-medium">
                    (Neighborhood around: {graphData.centerTopicName})
                  </span>
                )}
              </span>
              <span className="text-slate-500">
                Tip: Click node for details · Double-click to expand neighborhood
              </span>
            </div>

            <KnowledgeGraphCanvas
              nodes={graphData.nodes}
              edges={graphData.edges}
              onSelectTopic={handleOpenDetail}
              onExpandTopic={handleExpandNeighborhood}
              selectedTopicSlug={detailSlug}
            />
          </div>
        )}

        {/* VIEW MODE: TOPIC GRID */}
        {viewMode === 'grid' && !isLoading && (
          <div>
            {topics.length === 0 ? (
              <div className="py-20 text-center text-slate-500 bg-slate-900/30 rounded-xl border border-slate-800">
                <Layers className="w-10 h-10 mx-auto text-slate-600 mb-3" />
                <p className="text-sm font-medium">No topics found matching your filters.</p>
                <p className="text-xs text-slate-600 mt-1">Try clearing your search query or selecting All Subjects.</p>
              </div>
            ) : (
              <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
                {topics.map((topic) => {
                  const diffStyle =
                    DIFFICULTY_BADGES[topic.difficultyLevel] || DIFFICULTY_BADGES.BEGINNER;

                  return (
                    <div
                      key={topic.id}
                      className="bg-slate-900/70 border border-slate-800 hover:border-slate-700/80 rounded-xl p-5 flex flex-col justify-between group transition shadow-sm"
                    >
                      <div>
                        <div className="flex items-center justify-between gap-2 mb-2">
                          <span className="text-[11px] font-semibold text-slate-400">
                            {topic.subjectCode || topic.subjectName}
                          </span>
                          <span
                            className={`text-[10px] px-2 py-0.5 rounded border ${diffStyle.badge}`}
                          >
                            {diffStyle.text}
                          </span>
                        </div>

                        <h3 className="text-base font-bold text-white group-hover:text-primary-400 transition mb-1.5">
                          {topic.name}
                        </h3>

                        <p className="text-xs text-slate-400 line-clamp-3 leading-relaxed mb-4">
                          {topic.summary}
                        </p>
                      </div>

                      <div>
                        <div className="flex items-center gap-3 text-[11px] text-slate-400 mb-3 pt-3 border-t border-slate-800/80">
                          <span className="flex items-center gap-1">
                            <Clock className="w-3 h-3 text-slate-400" />
                            {topic.estimatedMinutes}m
                          </span>
                          <span>&bull;</span>
                          <span>Prereqs: {topic.prerequisiteCount}</span>
                          <span>&bull;</span>
                          <span>Subtopics: {topic.subtopicCount}</span>
                        </div>

                        <div className="flex items-center gap-2">
                          <button
                            onClick={() => handleOpenDetail(topic.slug)}
                            className="flex-1 px-3 py-1.5 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-200 text-xs font-medium transition"
                          >
                            Explore Topic
                          </button>
                          <button
                            onClick={() => handleOpenPath(topic.slug)}
                            className="px-3 py-1.5 rounded-lg bg-primary-600/20 hover:bg-primary-600/30 text-primary-300 border border-primary-500/30 text-xs font-medium flex items-center gap-1 transition"
                            title="Generate Prerequisite Learning Path"
                          >
                            <Compass className="w-3.5 h-3.5" />
                            Path
                          </button>
                        </div>
                      </div>
                    </div>
                  );
                })}
              </div>
            )}
          </div>
        )}

        {/* VIEW MODE: CURATED PATHS */}
        {viewMode === 'paths' && !isLoading && (
          <div className="space-y-4">
            <div className="text-xs text-slate-400">
              Curated milestone roadmaps computed via prerequisite graph topological sequencing.
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
              {curatedPaths.map((path) => (
                <div
                  key={path.targetTopicSlug}
                  className="bg-slate-900/80 border border-slate-800 hover:border-slate-700 rounded-xl p-6 flex flex-col justify-between shadow-lg"
                >
                  <div>
                    <div className="flex items-center gap-2 mb-3">
                      <span className="p-2 rounded-lg bg-primary-950 border border-primary-800 text-primary-400">
                        <Milestone className="w-5 h-5" />
                      </span>
                      <div>
                        <div className="text-[11px] font-semibold uppercase tracking-wider text-primary-400">
                          Milestone Track
                        </div>
                        <h3 className="text-lg font-bold text-white">{path.targetTopicName}</h3>
                      </div>
                    </div>

                    <div className="flex items-center gap-4 text-xs text-slate-400 mb-4 bg-slate-950/60 p-2.5 rounded-lg border border-slate-800/80">
                      <span>
                        <strong>{path.totalSteps}</strong> Steps
                      </span>
                      <span>&bull;</span>
                      <span>
                        ~{Math.round(path.estimatedTotalMinutes / 60 * 10) / 10} hours
                      </span>
                    </div>

                    {/* Prerequisite Steps Snapshot */}
                    <div className="space-y-2 mb-6">
                      <div className="text-[11px] font-semibold text-slate-500 uppercase tracking-wider">
                        Foundations Included
                      </div>
                      {path.steps.slice(0, 4).map((step) => (
                        <div
                          key={step.stepNumber}
                          className="flex items-center gap-2 text-xs text-slate-300"
                        >
                          <span className="w-4 h-4 rounded-full bg-slate-800 text-slate-400 text-[10px] font-semibold flex items-center justify-center flex-shrink-0">
                            {step.stepNumber}
                          </span>
                          <span className="truncate">{step.name}</span>
                        </div>
                      ))}
                      {path.steps.length > 4 && (
                        <div className="text-[11px] text-slate-500 pl-6">
                          + {path.steps.length - 4} more foundational topics
                        </div>
                      )}
                    </div>
                  </div>

                  <button
                    onClick={() => handleOpenPath(path.targetTopicSlug)}
                    className="w-full flex items-center justify-center gap-2 px-4 py-2.5 rounded-lg bg-primary-600 hover:bg-primary-500 text-white text-xs font-semibold transition shadow"
                  >
                    <Compass className="w-4 h-4" />
                    Open Full Learning Path
                  </button>
                </div>
              ))}
            </div>
          </div>
        )}
      </div>

      {/* Topic Detail Drawer */}
      <TopicDetailDrawer
        topicSlug={detailSlug}
        isOpen={isDetailOpen}
        onClose={() => {
          setIsDetailOpen(false);
          setDetailSlug(null);
        }}
        onSelectTopic={(slug) => {
          setDetailSlug(slug);
        }}
        onViewLearningPath={(slug) => {
          setPathSlug(slug);
          setIsPathOpen(true);
        }}
        onStudyWithTutor={handleStudyWithTutor}
        onOpenResource={onSelectResource}
      />

      {/* Learning Path Drawer */}
      <LearningPathDrawer
        topicSlug={pathSlug}
        isOpen={isPathOpen}
        onClose={() => {
          setIsPathOpen(false);
          setPathSlug(null);
        }}
        onSelectTopic={(slug) => {
          setDetailSlug(slug);
          setIsDetailOpen(true);
        }}
        onStudyWithTutor={handleStudyWithTutor}
      />
    </div>
  );
};
