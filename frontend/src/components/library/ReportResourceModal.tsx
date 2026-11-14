import React, { useState } from 'react';
import { libraryService } from '@/services/libraryService';
import { ReportReasonCategory } from '@/types';
import { AlertTriangle, X, Loader2, CheckCircle2 } from 'lucide-react';

interface ReportResourceModalProps {
  isOpen: boolean;
  resourceId: string;
  resourceTitle: string;
  onClose: () => void;
  onSuccess?: () => void;
}

const REPORT_REASONS: { value: ReportReasonCategory; label: string; desc: string }[] = [
  {
    value: 'COPYRIGHT_INFRINGEMENT',
    label: 'Copyright Infringement',
    desc: 'Uploaded without permission or violates author copyright.',
  },
  {
    value: 'INAPPROPRIATE_CONTENT',
    label: 'Inappropriate Content',
    desc: 'Offensive, harmful, or non-academic material.',
  },
  {
    value: 'INCORRECT_ATTRIBUTION',
    label: 'Incorrect Attribution / Plagiarism',
    desc: 'Misattributed authorship or missing required license attribution.',
  },
  {
    value: 'DEFECTIVE_CORRUPT',
    label: 'Corrupted / Defective File',
    desc: 'The file cannot be opened or is missing critical pages/content.',
  },
  {
    value: 'SPAM_MISLEADING',
    label: 'Spam or Misleading',
    desc: 'Misleading title, promotional material, or irrelevant content.',
  },
  {
    value: 'OTHER',
    label: 'Other Academic Concern',
    desc: 'Other academic integrity or quality issue.',
  },
];

export const ReportResourceModal: React.FC<ReportResourceModalProps> = ({
  isOpen,
  resourceId,
  resourceTitle,
  onClose,
  onSuccess,
}) => {
  const [reasonCategory, setReasonCategory] = useState<ReportReasonCategory>('COPYRIGHT_INFRINGEMENT');
  const [explanation, setExplanation] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [isSubmitted, setIsSubmitted] = useState(false);

  if (!isOpen) return null;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!explanation.trim()) {
      setError('Please provide a brief explanation for the report.');
      return;
    }

    try {
      setIsSubmitting(true);
      setError(null);
      await libraryService.submitReport(resourceId, {
        reasonCategory,
        explanation: explanation.trim(),
      });
      setIsSubmitted(true);
      setTimeout(() => {
        setIsSubmitted(false);
        setExplanation('');
        onClose();
        if (onSuccess) onSuccess();
      }, 1600);
    } catch (err: any) {
      setError(err?.response?.data?.message || 'Failed to submit report. Please try again.');
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/70 backdrop-blur-sm animate-in fade-in duration-200">
      <div className="relative w-full max-w-lg bg-slate-900 border border-slate-800 rounded-2xl shadow-2xl p-6 overflow-hidden">
        <div className="flex items-center justify-between pb-4 border-b border-slate-800">
          <div className="flex items-center gap-3">
            <div className="w-9 h-9 rounded-xl bg-amber-500/10 border border-amber-500/20 flex items-center justify-center text-amber-400">
              <AlertTriangle className="w-5 h-5" />
            </div>
            <div>
              <h3 className="text-lg font-semibold text-white">Report Library Resource</h3>
              <p className="text-xs text-slate-400 truncate max-w-[280px]">{resourceTitle}</p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="text-slate-400 hover:text-white p-1 rounded-lg hover:bg-slate-800 transition"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {isSubmitted ? (
          <div className="py-8 flex flex-col items-center text-center">
            <div className="w-12 h-12 rounded-full bg-emerald-500/10 border border-emerald-500/20 flex items-center justify-center text-emerald-400 mb-3">
              <CheckCircle2 className="w-6 h-6" />
            </div>
            <h4 className="text-base font-semibold text-white mb-1">Report Received</h4>
            <p className="text-xs text-slate-400 max-w-xs">
              Thank you for helping maintain library integrity. Academic moderators will review this submission.
            </p>
          </div>
        ) : (
          <form onSubmit={handleSubmit} className="mt-4 space-y-4">
            {error && (
              <div className="p-3 bg-rose-500/10 border border-rose-500/20 rounded-xl text-xs text-rose-400">
                {error}
              </div>
            )}

            <div>
              <label className="block text-xs font-semibold uppercase tracking-wider text-slate-400 mb-1.5">
                Reason for Report
              </label>
              <select
                value={reasonCategory}
                onChange={(e) => setReasonCategory(e.target.value as ReportReasonCategory)}
                className="w-full bg-slate-950 border border-slate-800 rounded-xl px-3.5 py-2.5 text-sm text-white focus:outline-none focus:ring-2 focus:ring-amber-500/50"
              >
                {REPORT_REASONS.map((r) => (
                  <option key={r.value} value={r.value}>
                    {r.label}
                  </option>
                ))}
              </select>
              <p className="text-xs text-slate-500 mt-1">
                {REPORT_REASONS.find((r) => r.value === reasonCategory)?.desc}
              </p>
            </div>

            <div>
              <label className="block text-xs font-semibold uppercase tracking-wider text-slate-400 mb-1.5">
                Detailed Explanation
              </label>
              <textarea
                value={explanation}
                onChange={(e) => setExplanation(e.target.value)}
                rows={4}
                maxLength={2000}
                placeholder="Describe the issue, specific pages, or copyright owner details..."
                className="w-full bg-slate-950 border border-slate-800 rounded-xl p-3 text-sm text-white placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-amber-500/50"
                required
              />
              <div className="text-right text-[11px] text-slate-500 mt-1">
                {explanation.length} / 2000
              </div>
            </div>

            <div className="flex items-center justify-end gap-3 pt-3 border-t border-slate-800">
              <button
                type="button"
                onClick={onClose}
                disabled={isSubmitting}
                className="px-4 py-2 text-xs font-medium text-slate-300 hover:text-white bg-slate-800/80 hover:bg-slate-800 rounded-xl transition"
              >
                Cancel
              </button>
              <button
                type="submit"
                disabled={isSubmitting}
                className="px-4 py-2 text-xs font-semibold text-white bg-amber-600 hover:bg-amber-500 rounded-xl flex items-center gap-2 transition disabled:opacity-50"
              >
                {isSubmitting && <Loader2 className="w-3.5 h-3.5 animate-spin" />}
                <span>Submit Report</span>
              </button>
            </div>
          </form>
        )}
      </div>
    </div>
  );
};

