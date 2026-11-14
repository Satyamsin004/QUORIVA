import React, { useEffect, useState } from 'react';
import {
  X,
  Compass,
  Clock,
  ArrowRight,
  Sparkles,
  AlertCircle,
  Loader2,
  Milestone,
} from 'lucide-react';
import { knowledgeService } from '@/services/knowledgeService';
import { LearningPath, TopicDifficulty } from '@/types';

interface LearningPathDrawerProps {
  topicSlug: string | null;
  isOpen: boolean;
  onClose: () => void;
  onSelectTopic: (slug: string) => void;
  onStudyWithTutor: (topicName: string, subjectName: string) => void;
}

const DIFFICULTY_STYLES: Record<TopicDifficulty, { badge: string; text: string }> = {
  BEGINNER: {
    badge: 'bg-emerald-500/10 text-emerald-400 border-emerald-500/30',
    text: 'text-emerald-400',
  },
  INTERMEDIATE: {
    badge: 'bg-blue-500/10 text-blue-400 border-blue-500/30',
    text: 'text-blue-400',
  },
  ADVANCED: {
    badge: 'bg-purple-500/10 text-purple-400 border-purple-500/30',
    text: 'text-purple-400',
  },
};

export const LearningPathDrawer: React.FC<LearningPathDrawerProps> = ({
  topicSlug,
  isOpen,
  onClose,
  onSelectTopic,
  onStudyWithTutor,
}) => {
  const [learningPath, setLearningPath] = useState<LearningPath | null>(null);
  const [loading, setLoading] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!isOpen || !topicSlug) {
      setLearningPath(null);
      return;
    }

    let isMounted = true;
    setLoading(true);
    setError(null);

    knowledgeService
      .getLearningPath(topicSlug)
      .then((data) => {
        if (isMounted) {
          setLearningPath(data);
          setLoading(false);
        }
      })
      .catch((err) => {
        if (isMounted) {
          setError(err.response?.data?.message || 'Failed to generate learning path.');
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
          <div>
            <div className="flex items-center gap-2 mb-1.5">
              <span className="text-xs font-semibold px-2 py-0.5 rounded bg-primary-950/80 text-primary-300 border border-primary-800 flex items-center gap-1">
                <Compass className="w-3 h-3" /> Learning Roadmap
              </span>
              <span className="text-xs px-2 py-0.5 rounded bg-slate-800 text-slate-400 border border-slate-700">
                Topological Prerequisite Order
              </span>
            </div>
            <h2 className="text-xl font-bold text-white tracking-tight">
              {loading ? 'Generating Path...' : learningPath?.targetTopicName || 'Learning Path'}
            </h2>
            {learningPath && (
              <div className="flex items-center gap-4 mt-2 text-xs text-slate-400">
                <span className="flex items-center gap-1">
                  <Milestone className="w-3.5 h-3.5 text-primary-400" />
                  {learningPath.totalSteps} {learningPath.totalSteps === 1 ? 'Step' : 'Steps'}
                </span>
                <span className="flex items-center gap-1">
                  <Clock className="w-3.5 h-3.5 text-amber-400" />
                  {Math.round(learningPath.estimatedTotalMinutes / 60 * 10) / 10} hrs ({learningPath.estimatedTotalMinutes} mins)
                </span>
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

        {/* Drawer Body */}
        <div className="flex-1 overflow-y-auto p-6 space-y-6">
          {loading && (
            <div className="py-20 flex flex-col items-center justify-center text-slate-400">
              <Loader2 className="w-8 h-8 animate-spin text-primary-400 mb-3" />
              <p className="text-sm">Calculating prerequisite DAG & topological ordering...</p>
            </div>
          )}

          {error && (
            <div className="p-4 rounded-lg bg-red-950/40 border border-red-800 text-red-300 flex items-start gap-3">
              <AlertCircle className="w-5 h-5 flex-shrink-0 mt-0.5" />
              <p className="text-sm">{error}</p>
            </div>
          )}

          {learningPath && !loading && (
            <div className="space-y-6">
              {/* Introduction Card */}
              <div className="bg-slate-800/40 border border-slate-800 p-4 rounded-xl text-xs text-slate-300">
                {learningPath.hasPrerequisites ? (
                  <p>
                    Follow this deterministic sequence of <strong className="text-white">{learningPath.totalSteps} steps</strong> to build solid foundations before tackling <strong className="text-primary-300">{learningPath.targetTopicName}</strong>.
                  </p>
                ) : (
                  <p>
                    <strong className="text-white">{learningPath.targetTopicName}</strong> is a foundational topic with no prerequisites! You can start learning it immediately.
                  </p>
                )}
              </div>

              {/* Steps Vertical Timeline */}
              <div className="relative pl-6 border-l-2 border-slate-800 space-y-6">
                {learningPath.steps.map((step) => {
                  const isFinal = step.isTargetTopic || step.targetTopic;
                  const diffStyle = DIFFICULTY_STYLES[step.difficultyLevel] || DIFFICULTY_STYLES.BEGINNER;

                  return (
                    <div key={step.stepNumber} className="relative group">
                      {/* Timeline Node Icon */}
                      <div
                        className={`absolute -left-[31px] top-1 w-6 h-6 rounded-full border-2 flex items-center justify-center text-[10px] font-bold ${
                          isFinal
                            ? 'bg-amber-500 border-amber-300 text-slate-950'
                            : 'bg-slate-900 border-primary-500 text-primary-400'
                        }`}
                      >
                        {step.stepNumber}
                      </div>

                      {/* Step Card */}
                      <div
                        className={`p-4 rounded-xl border transition ${
                          isFinal
                            ? 'bg-gradient-to-br from-slate-900 via-amber-950/20 to-slate-900 border-amber-500/40 shadow-lg'
                            : 'bg-slate-900/90 border-slate-800 hover:border-slate-700'
                        }`}
                      >
                        <div className="flex items-center justify-between gap-2 mb-2">
                          <div className="flex items-center gap-2">
                            <span className="text-[11px] font-semibold text-slate-400">
                              {step.subjectName}
                            </span>
                            <span
                              className={`text-[10px] px-2 py-0.5 rounded border ${diffStyle.badge}`}
                            >
                              {step.difficultyLevel}
                            </span>
                          </div>
                          <span className="text-xs text-slate-400 flex items-center gap-1">
                            <Clock className="w-3 h-3 text-slate-400" />
                            {step.estimatedMinutes}m
                          </span>
                        </div>

                        <h4 className="text-sm font-semibold text-white mb-1 flex items-center gap-2">
                          {step.name}
                          {isFinal && (
                            <span className="text-[10px] px-2 py-0.5 rounded bg-amber-500/20 text-amber-300 border border-amber-500/40 font-normal">
                              Goal Target
                            </span>
                          )}
                        </h4>

                        <p className="text-xs text-slate-400 mb-3">{step.summary}</p>

                        <div className="flex items-center justify-between pt-2 border-t border-slate-800/80">
                          <button
                            onClick={() => onSelectTopic(step.slug)}
                            className="text-xs text-primary-400 hover:text-primary-300 flex items-center gap-1 transition"
                          >
                            Explore Topic <ArrowRight className="w-3 h-3" />
                          </button>
                          <button
                            onClick={() => onStudyWithTutor(step.name, step.subjectName)}
                            className="text-xs text-indigo-400 hover:text-indigo-300 flex items-center gap-1 transition"
                          >
                            <Sparkles className="w-3 h-3 text-amber-300" />
                            Study Step
                          </button>
                        </div>
                      </div>
                    </div>
                  );
                })}
              </div>

              {/* Bottom Tutor Call to Action */}
              <div className="p-4 rounded-xl bg-gradient-to-r from-primary-950/60 to-indigo-950/60 border border-primary-800/50 flex items-center justify-between">
                <div>
                  <h4 className="text-xs font-semibold text-white">Study with AI Tutor</h4>
                  <p className="text-[11px] text-slate-300 mt-0.5">
                    Launch an interactive tutoring session focused on this learning roadmap.
                  </p>
                </div>
                <button
                  onClick={() => onStudyWithTutor(learningPath.targetTopicName, 'CS Knowledge Path')}
                  className="px-3.5 py-2 rounded-lg bg-primary-600 hover:bg-primary-500 text-white text-xs font-medium flex items-center gap-1.5 transition shadow"
                >
                  <Sparkles className="w-3.5 h-3.5 text-amber-300" />
                  Start Session
                </button>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};
