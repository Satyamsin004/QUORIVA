import React, { useEffect, useState } from 'react';
import {
  X,
  BookOpen,
  Compass,
  ArrowRight,
  CheckCircle2,
  Clock,
  Sparkles,
  ExternalLink,
  Tag,
  AlertCircle,
  Loader2,
  FolderGit2,
} from 'lucide-react';
import { knowledgeService } from '@/services/knowledgeService';
import { KnowledgeTopicDetail, TopicDifficulty } from '@/types';

interface TopicDetailDrawerProps {
  topicSlug: string | null;
  isOpen: boolean;
  onClose: () => void;
  onSelectTopic: (slug: string) => void;
  onViewLearningPath: (slug: string) => void;
  onStudyWithTutor: (topicName: string, subjectName: string) => void;
  onOpenResource?: (resourceId: string) => void;
}

const DIFFICULTY_STYLES: Record<TopicDifficulty, { badge: string; label: string }> = {
  BEGINNER: {
    badge: 'bg-emerald-500/10 text-emerald-400 border-emerald-500/30',
    label: 'Beginner',
  },
  INTERMEDIATE: {
    badge: 'bg-blue-500/10 text-blue-400 border-blue-500/30',
    label: 'Intermediate',
  },
  ADVANCED: {
    badge: 'bg-purple-500/10 text-purple-400 border-purple-500/30',
    label: 'Advanced',
  },
};

export const TopicDetailDrawer: React.FC<TopicDetailDrawerProps> = ({
  topicSlug,
  isOpen,
  onClose,
  onSelectTopic,
  onViewLearningPath,
  onStudyWithTutor,
  onOpenResource,
}) => {
  const [topic, setTopic] = useState<KnowledgeTopicDetail | null>(null);
  const [loading, setLoading] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!isOpen || !topicSlug) {
      setTopic(null);
      return;
    }

    let isMounted = true;
    setLoading(true);
    setError(null);

    knowledgeService
      .getTopicDetail(topicSlug)
      .then((data) => {
        if (isMounted) {
          setTopic(data);
          setLoading(false);
        }
      })
      .catch((err) => {
        if (isMounted) {
          setError(err.response?.data?.message || 'Failed to load topic details.');
          setLoading(false);
        }
      });

    return () => {
      isMounted = false;
    };
  }, [isOpen, topicSlug]);

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 overflow-hidden bg-slate-950/60 backdrop-blur-sm flex justify-end transition-opacity">
      <div className="w-full max-w-xl bg-slate-900 border-l border-slate-800 shadow-2xl h-full flex flex-col text-slate-100">
        {/* Drawer Header */}
        <div className="p-6 border-b border-slate-800 flex items-start justify-between bg-slate-900/80 sticky top-0 z-10">
          <div className="pr-4">
            <div className="flex items-center gap-2 mb-1.5 flex-wrap">
              {topic && (
                <>
                  <span className="text-xs font-semibold px-2 py-0.5 rounded bg-slate-800 text-slate-300 border border-slate-700">
                    {topic.subjectCode || topic.subjectName}
                  </span>
                  <span
                    className={`text-xs font-medium px-2 py-0.5 rounded border ${
                      DIFFICULTY_STYLES[topic.difficultyLevel]?.badge || ''
                    }`}
                  >
                    {DIFFICULTY_STYLES[topic.difficultyLevel]?.label || topic.difficultyLevel}
                  </span>
                  <span className="text-xs px-2 py-0.5 rounded bg-slate-800/60 text-slate-400 border border-slate-700/50">
                    {topic.topicType}
                  </span>
                </>
              )}
            </div>
            <h2 className="text-xl font-bold text-white tracking-tight">
              {loading ? 'Loading Topic...' : topic?.name || 'Topic Details'}
            </h2>
            {topic?.estimatedMinutes && (
              <div className="flex items-center gap-1.5 mt-1.5 text-xs text-slate-400">
                <Clock className="w-3.5 h-3.5 text-slate-400" />
                <span>Estimated Study Time: {topic.estimatedMinutes} mins</span>
              </div>
            )}
          </div>
          <button
            onClick={onClose}
            className="p-1.5 text-slate-400 hover:text-white hover:bg-slate-800 rounded-lg transition"
            title="Close Drawer"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Drawer Content */}
        <div className="flex-1 overflow-y-auto p-6 space-y-6">
          {loading && (
            <div className="py-20 flex flex-col items-center justify-center text-slate-400">
              <Loader2 className="w-8 h-8 animate-spin text-primary-400 mb-3" />
              <p className="text-sm">Retrieving topic knowledge...</p>
            </div>
          )}

          {error && (
            <div className="p-4 rounded-lg bg-red-950/40 border border-red-800 text-red-300 flex items-start gap-3">
              <AlertCircle className="w-5 h-5 flex-shrink-0 mt-0.5" />
              <p className="text-sm">{error}</p>
            </div>
          )}

          {topic && !loading && (
            <>
              {/* Primary Actions */}
              <div className="flex items-center gap-3">
                <button
                  onClick={() => onViewLearningPath(topic.slug)}
                  className="flex-1 flex items-center justify-center gap-2 px-4 py-2.5 rounded-lg bg-primary-600 hover:bg-primary-500 text-white font-medium text-sm transition shadow-sm"
                >
                  <Compass className="w-4 h-4" />
                  View Learning Path
                </button>
                <button
                  onClick={() => onStudyWithTutor(topic.name, topic.subjectName)}
                  className="flex-1 flex items-center justify-center gap-2 px-4 py-2.5 rounded-lg bg-indigo-600/90 hover:bg-indigo-500 text-white font-medium text-sm transition shadow-sm"
                >
                  <Sparkles className="w-4 h-4 text-amber-300" />
                  Ask AI Tutor
                </button>
              </div>

              {/* Summary */}
              <div>
                <h3 className="text-xs font-semibold uppercase tracking-wider text-slate-400 mb-2">
                  Summary
                </h3>
                <p className="text-sm text-slate-300 leading-relaxed bg-slate-800/40 p-3.5 rounded-lg border border-slate-800">
                  {topic.summary}
                </p>
              </div>

              {/* Detailed Overview */}
              {topic.detailedDescription && (
                <div>
                  <h3 className="text-xs font-semibold uppercase tracking-wider text-slate-400 mb-2">
                    Detailed Overview
                  </h3>
                  <div className="text-sm text-slate-300 space-y-2 leading-relaxed">
                    {topic.detailedDescription.split('\n\n').map((para, i) => (
                      <p key={i}>{para}</p>
                    ))}
                  </div>
                </div>
              )}

              {/* Learning Objectives */}
              {topic.learningObjectives && topic.learningObjectives.length > 0 && (
                <div>
                  <h3 className="text-xs font-semibold uppercase tracking-wider text-slate-400 mb-2.5">
                    Learning Objectives
                  </h3>
                  <ul className="space-y-2">
                    {topic.learningObjectives.map((obj, i) => (
                      <li
                        key={i}
                        className="flex items-start gap-2.5 text-xs text-slate-300 bg-slate-800/30 p-2.5 rounded-md border border-slate-800"
                      >
                        <CheckCircle2 className="w-4 h-4 text-emerald-400 flex-shrink-0 mt-0.5" />
                        <span>{obj}</span>
                      </li>
                    ))}
                  </ul>
                </div>
              )}

              {/* Prerequisites Section */}
              <div>
                <h3 className="text-xs font-semibold uppercase tracking-wider text-slate-400 mb-2">
                  Prerequisites ({topic.prerequisites?.length || 0})
                </h3>
                {topic.prerequisites && topic.prerequisites.length > 0 ? (
                  <div className="space-y-2">
                    {topic.prerequisites.map((req) => (
                      <div
                        key={req.relationshipId}
                        onClick={() => onSelectTopic(req.slug)}
                        className="group flex items-center justify-between p-3 rounded-lg bg-slate-800/50 hover:bg-slate-800 border border-slate-800 hover:border-slate-700 cursor-pointer transition"
                      >
                        <div>
                          <div className="text-xs font-medium text-white group-hover:text-primary-400 transition">
                            {req.name}
                          </div>
                          <div className="text-[11px] text-slate-400 mt-0.5">
                            {req.description || 'Foundational prerequisite'}
                          </div>
                        </div>
                        <ArrowRight className="w-4 h-4 text-slate-500 group-hover:text-primary-400 transition transform group-hover:translate-x-0.5" />
                      </div>
                    ))}
                  </div>
                ) : (
                  <p className="text-xs text-slate-500 italic bg-slate-800/20 p-2.5 rounded border border-slate-800/60">
                    No prerequisites required. This is a foundational concept.
                  </p>
                )}
              </div>

              {/* Unlocked Concepts */}
              {topic.unlockedTopics && topic.unlockedTopics.length > 0 && (
                <div>
                  <h3 className="text-xs font-semibold uppercase tracking-wider text-slate-400 mb-2">
                    Concepts Unlocked After This ({topic.unlockedTopics.length})
                  </h3>
                  <div className="space-y-2">
                    {topic.unlockedTopics.map((unlocked) => (
                      <div
                        key={unlocked.relationshipId}
                        onClick={() => onSelectTopic(unlocked.slug)}
                        className="group flex items-center justify-between p-3 rounded-lg bg-slate-800/50 hover:bg-slate-800 border border-slate-800 hover:border-slate-700 cursor-pointer transition"
                      >
                        <div>
                          <div className="text-xs font-medium text-white group-hover:text-primary-400 transition">
                            {unlocked.name}
                          </div>
                          <div className="text-[11px] text-slate-400 mt-0.5">
                            {unlocked.description || 'Enabled by mastering this topic'}
                          </div>
                        </div>
                        <ArrowRight className="w-4 h-4 text-slate-500 group-hover:text-primary-400 transition transform group-hover:translate-x-0.5" />
                      </div>
                    ))}
                  </div>
                </div>
              )}

              {/* Related Concepts */}
              {topic.relatedTopics && topic.relatedTopics.length > 0 && (
                <div>
                  <h3 className="text-xs font-semibold uppercase tracking-wider text-slate-400 mb-2">
                    Related Concepts
                  </h3>
                  <div className="flex flex-wrap gap-2">
                    {topic.relatedTopics.map((rel) => (
                      <button
                        key={rel.relationshipId}
                        onClick={() => onSelectTopic(rel.slug)}
                        className="px-2.5 py-1.5 rounded-lg bg-slate-800 hover:bg-slate-700 border border-slate-700 text-xs text-slate-300 hover:text-white transition flex items-center gap-1.5"
                      >
                        <FolderGit2 className="w-3 h-3 text-slate-400" />
                        {rel.name}
                      </button>
                    ))}
                  </div>
                </div>
              )}

              {/* Subtopics */}
              {topic.subtopics && topic.subtopics.length > 0 && (
                <div>
                  <h3 className="text-xs font-semibold uppercase tracking-wider text-slate-400 mb-2">
                    Subtopics ({topic.subtopics.length})
                  </h3>
                  <div className="grid grid-cols-1 gap-2">
                    {topic.subtopics.map((sub) => (
                      <div
                        key={sub.id}
                        onClick={() => onSelectTopic(sub.slug)}
                        className="p-2.5 rounded-lg bg-slate-800/40 hover:bg-slate-800 border border-slate-800 cursor-pointer text-xs flex items-center justify-between"
                      >
                        <span className="font-medium text-slate-200">{sub.name}</span>
                        <span className="text-[10px] text-slate-400">{sub.estimatedMinutes}m</span>
                      </div>
                    ))}
                  </div>
                </div>
              )}

              {/* Curated Library Resources */}
              {topic.resources && topic.resources.length > 0 && (
                <div>
                  <h3 className="text-xs font-semibold uppercase tracking-wider text-slate-400 mb-2 flex items-center gap-1.5">
                    <BookOpen className="w-3.5 h-3.5 text-primary-400" />
                    Published Library Resources ({topic.resources.length})
                  </h3>
                  <div className="space-y-2">
                    {topic.resources.map((res) => (
                      <div
                        key={res.resourceId}
                        onClick={() => onOpenResource && onOpenResource(res.resourceId)}
                        className="p-3 rounded-lg bg-slate-800/60 hover:bg-slate-800 border border-slate-700/80 cursor-pointer flex items-center justify-between group transition"
                      >
                        <div>
                          <div className="text-xs font-medium text-white group-hover:text-primary-400 transition">
                            {res.title}
                          </div>
                          <div className="text-[10px] text-slate-400 mt-0.5 flex items-center gap-2">
                            <span>{res.category}</span>
                            <span>&bull;</span>
                            <span>{res.resourceType}</span>
                          </div>
                        </div>
                        <ExternalLink className="w-4 h-4 text-slate-400 group-hover:text-primary-400 transition" />
                      </div>
                    ))}
                  </div>
                </div>
              )}

              {/* Tags */}
              {topic.tags && topic.tags.length > 0 && (
                <div>
                  <h3 className="text-xs font-semibold uppercase tracking-wider text-slate-400 mb-2">
                    Keywords & Tags
                  </h3>
                  <div className="flex flex-wrap gap-1.5">
                    {topic.tags.map((tag, i) => (
                      <span
                        key={i}
                        className="inline-flex items-center gap-1 text-[11px] px-2 py-0.5 rounded bg-slate-800 text-slate-400 border border-slate-700/60"
                      >
                        <Tag className="w-2.5 h-2.5" />
                        {tag}
                      </span>
                    ))}
                  </div>
                </div>
              )}
            </>
          )}
        </div>
      </div>
    </div>
  );
};
