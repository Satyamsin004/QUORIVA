import React, { useState, useEffect } from 'react';
import { libraryService } from '@/services/libraryService';
import { LibraryResourceSummary } from '@/types';
import {
  FolderClock,
  Plus,
  ArrowLeft,
  CheckCircle,
  Clock,
  XCircle,
  PauseCircle,
  FileText,
  Edit,
  Trash2,
  Send,
  RotateCcw,
  Loader2,
  ExternalLink,
} from 'lucide-react';

interface LibraryMySubmissionsPageProps {
  onBack: () => void;
  onNavigate: (route: string) => void;
  onSelectResource: (id: string) => void;
  onEditResource: (id: string) => void;
}

export const LibraryMySubmissionsPage: React.FC<LibraryMySubmissionsPageProps> = ({
  onBack,
  onNavigate,
  onSelectResource,
  onEditResource,
}) => {
  const [submissions, setSubmissions] = useState<LibraryResourceSummary[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [actionLoadingId, setActionLoadingId] = useState<string | null>(null);

  const fetchMySubmissions = async () => {
    try {
      setIsLoading(true);
      setError(null);
      const res = await libraryService.getMySubmissions(page, 10);
      setSubmissions(res.content || []);
      setTotalPages(res.totalPages || res.page?.totalPages || 1);
    } catch (err: any) {
      setError(err?.response?.data?.message || 'Failed to load your submissions.');
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchMySubmissions();
  }, [page]);

  const handleSubmitForReview = async (id: string) => {
    try {
      setActionLoadingId(id);
      await libraryService.submitForReview(id);
      await fetchMySubmissions();
    } catch (err: any) {
      alert(err?.response?.data?.message || 'Failed to submit resource for review.');
    } finally {
      setActionLoadingId(null);
    }
  };

  const handleWithdraw = async (id: string) => {
    if (!confirm('Withdraw this submission back to draft status?')) return;
    try {
      setActionLoadingId(id);
      await libraryService.withdrawSubmission(id);
      await fetchMySubmissions();
    } catch (err: any) {
      alert(err?.response?.data?.message || 'Failed to withdraw submission.');
    } finally {
      setActionLoadingId(null);
    }
  };

  const handleDelete = async (id: string) => {
    if (!confirm('Are you sure you want to permanently delete this submission?')) return;
    try {
      setActionLoadingId(id);
      await libraryService.deleteSubmission(id);
      await fetchMySubmissions();
    } catch (err: any) {
      alert(err?.response?.data?.message || 'Failed to delete submission.');
    } finally {
      setActionLoadingId(null);
    }
  };

  const renderStatusBadge = (status: string) => {
    switch (status) {
      case 'PUBLISHED':
        return (
          <span className="inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full text-xs font-semibold bg-emerald-500/10 text-emerald-400 border border-emerald-500/20">
            <CheckCircle className="w-3.5 h-3.5" /> Published
          </span>
        );
      case 'PENDING_REVIEW':
        return (
          <span className="inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full text-xs font-semibold bg-amber-500/10 text-amber-400 border border-amber-500/20">
            <Clock className="w-3.5 h-3.5" /> In Review
          </span>
        );
      case 'REJECTED':
        return (
          <span className="inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full text-xs font-semibold bg-rose-500/10 text-rose-400 border border-rose-500/20">
            <XCircle className="w-3.5 h-3.5" /> Revisions Needed
          </span>
        );
      case 'SUSPENDED':
        return (
          <span className="inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full text-xs font-semibold bg-orange-500/10 text-orange-400 border border-orange-500/20">
            <PauseCircle className="w-3.5 h-3.5" /> Suspended
          </span>
        );
      default:
        return (
          <span className="inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full text-xs font-semibold bg-slate-500/10 text-slate-400 border border-slate-500/20">
            Draft
          </span>
        );
    }
  };

  return (
    <div className="max-w-6xl mx-auto py-8 px-4 sm:px-6 space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-4 border-b border-slate-800">
        <div>
          <button
            onClick={onBack}
            className="inline-flex items-center gap-2 text-xs font-semibold text-slate-400 hover:text-white transition mb-2"
          >
            <ArrowLeft className="w-4 h-4" /> Back to Library
          </button>
          <h1 className="text-2xl font-bold text-white tracking-tight flex items-center gap-2.5">
            <FolderClock className="w-6 h-6 text-indigo-400" /> My Academic Submissions
          </h1>
          <p className="text-xs text-slate-400 mt-1">
            Track status, update drafts, address editorial review comments, and manage your contributions.
          </p>
        </div>

        <button
          onClick={() => onNavigate('library-submit')}
          className="px-4 py-2 text-xs font-semibold text-white bg-indigo-600 hover:bg-indigo-500 rounded-xl transition inline-flex items-center gap-1.5 shadow-lg shadow-indigo-600/20"
        >
          <Plus className="w-4 h-4" /> New Contribution
        </button>
      </div>

      {error && (
        <div className="p-4 bg-rose-500/10 border border-rose-500/20 rounded-2xl text-xs text-rose-300">
          {error}
        </div>
      )}

      {isLoading ? (
        <div className="flex flex-col items-center justify-center py-24 text-slate-400">
          <Loader2 className="w-8 h-8 animate-spin text-indigo-500 mb-3" />
          <p className="text-xs">Loading submissions...</p>
        </div>
      ) : submissions.length === 0 ? (
        <div className="py-16 px-4 bg-slate-900/40 border border-slate-800 rounded-2xl text-center space-y-3">
          <div className="w-12 h-12 rounded-2xl bg-slate-800 flex items-center justify-center mx-auto text-slate-400">
            <FileText className="w-6 h-6" />
          </div>
          <h3 className="text-base font-semibold text-white">No Submissions Yet</h3>
          <p className="text-xs text-slate-400 max-w-sm mx-auto">
            You haven't contributed any academic resources to the library yet. Share lecture notes or study guides to help fellow students.
          </p>
          <button
            onClick={() => onNavigate('library-submit')}
            className="px-4 py-2 text-xs font-semibold text-white bg-indigo-600 hover:bg-indigo-500 rounded-xl transition inline-flex items-center gap-1.5"
          >
            <Plus className="w-4 h-4" /> Contribute First Resource
          </button>
        </div>
      ) : (
        <div className="space-y-4">
          {submissions.map((sub) => (
            <div
              key={sub.id}
              className="bg-slate-900 border border-slate-800 rounded-2xl p-5 space-y-4 transition hover:border-slate-700"
            >
              <div className="flex flex-col sm:flex-row sm:items-start justify-between gap-3">
                <div className="space-y-1.5 flex-1">
                  <div className="flex flex-wrap items-center gap-2">
                    {renderStatusBadge(sub.status)}
                    <span className="text-[11px] font-semibold text-indigo-400 bg-indigo-500/10 px-2 py-0.5 rounded border border-indigo-500/20">
                      {sub.categoryDisplayName || sub.category}
                    </span>
                    <span className="text-[11px] text-slate-400 bg-slate-800 px-2 py-0.5 rounded">
                      {sub.resourceTypeDisplayName || sub.resourceType}
                    </span>
                  </div>

                  <h3
                    onClick={() => onSelectResource(sub.id)}
                    className="text-base font-bold text-white hover:text-indigo-400 transition cursor-pointer"
                  >
                    {sub.title}
                  </h3>

                  <p className="text-xs text-slate-400 line-clamp-2">{sub.description}</p>
                </div>

                {/* Status action buttons */}
                <div className="flex flex-wrap items-center gap-2 shrink-0 pt-2 sm:pt-0">
                  {sub.status === 'DRAFT' && (
                    <>
                      <button
                        onClick={() => handleSubmitForReview(sub.id)}
                        disabled={actionLoadingId === sub.id}
                        className="px-3 py-1.5 bg-indigo-600 hover:bg-indigo-500 text-white text-xs font-semibold rounded-xl inline-flex items-center gap-1.5 transition"
                      >
                        <Send className="w-3.5 h-3.5" /> Submit for Review
                      </button>
                      <button
                        onClick={() => onEditResource(sub.id)}
                        className="px-3 py-1.5 bg-slate-800 hover:bg-slate-700 text-slate-200 text-xs font-medium rounded-xl inline-flex items-center gap-1.5 transition"
                      >
                        <Edit className="w-3.5 h-3.5" /> Edit
                      </button>
                      <button
                        onClick={() => handleDelete(sub.id)}
                        disabled={actionLoadingId === sub.id}
                        className="p-1.5 text-slate-400 hover:text-rose-400 hover:bg-rose-500/10 rounded-lg transition"
                        title="Delete draft"
                      >
                        <Trash2 className="w-4 h-4" />
                      </button>
                    </>
                  )}

                  {sub.status === 'PENDING_REVIEW' && (
                    <>
                      <button
                        onClick={() => onSelectResource(sub.id)}
                        className="px-3 py-1.5 bg-slate-800 hover:bg-slate-700 text-slate-200 text-xs font-medium rounded-xl transition"
                      >
                        View Submission
                      </button>
                      <button
                        onClick={() => handleWithdraw(sub.id)}
                        disabled={actionLoadingId === sub.id}
                        className="px-3 py-1.5 bg-slate-800 hover:bg-slate-700 text-amber-300 text-xs font-medium rounded-xl inline-flex items-center gap-1.5 transition"
                      >
                        <RotateCcw className="w-3.5 h-3.5" /> Withdraw to Draft
                      </button>
                    </>
                  )}

                  {sub.status === 'REJECTED' && (
                    <>
                      <button
                        onClick={() => onEditResource(sub.id)}
                        className="px-3 py-1.5 bg-amber-600 hover:bg-amber-500 text-white text-xs font-semibold rounded-xl inline-flex items-center gap-1.5 transition"
                      >
                        <Edit className="w-3.5 h-3.5" /> Revise & Resubmit
                      </button>
                      <button
                        onClick={() => onSelectResource(sub.id)}
                        className="px-3 py-1.5 bg-slate-800 hover:bg-slate-700 text-slate-200 text-xs font-medium rounded-xl transition"
                      >
                        View Feedback
                      </button>
                      <button
                        onClick={() => handleDelete(sub.id)}
                        disabled={actionLoadingId === sub.id}
                        className="p-1.5 text-slate-400 hover:text-rose-400 hover:bg-rose-500/10 rounded-lg transition"
                      >
                        <Trash2 className="w-4 h-4" />
                      </button>
                    </>
                  )}

                  {sub.status === 'PUBLISHED' && (
                    <button
                      onClick={() => onSelectResource(sub.id)}
                      className="px-3.5 py-1.5 bg-slate-800 hover:bg-slate-700 text-white text-xs font-medium rounded-xl inline-flex items-center gap-1.5 transition"
                    >
                      <ExternalLink className="w-3.5 h-3.5" /> View in Library
                    </button>
                  )}
                </div>
              </div>

              {/* Sub-details */}
              <div className="flex flex-wrap items-center gap-4 text-[11px] text-slate-500 pt-2 border-t border-slate-800/80">
                <span>File: {sub.fileName} ({sub.fileType})</span>
                <span>License: {sub.licenseName}</span>
                <span>Rights: {sub.rightsTypeDisplayName || sub.rightsType}</span>
                <span>Submitted: {new Date(sub.createdAt).toLocaleDateString()}</span>
                {sub.downloadsCount > 0 && <span>Downloads: {sub.downloadsCount}</span>}
              </div>
            </div>
          ))}

          {/* Pagination */}
          {totalPages > 1 && (
            <div className="flex items-center justify-center gap-2 pt-4">
              <button
                onClick={() => setPage((p) => Math.max(0, p - 1))}
                disabled={page === 0}
                className="px-3 py-1.5 bg-slate-900 border border-slate-800 rounded-xl text-xs text-slate-300 disabled:opacity-40"
              >
                Previous
              </button>
              <span className="text-xs text-slate-400 px-2">
                Page {page + 1} of {totalPages}
              </span>
              <button
                onClick={() => setPage((p) => Math.min(totalPages - 1, p + 1))}
                disabled={page >= totalPages - 1}
                className="px-3 py-1.5 bg-slate-900 border border-slate-800 rounded-xl text-xs text-slate-300 disabled:opacity-40"
              >
                Next
              </button>
            </div>
          )}
        </div>
      )}
    </div>
  );
};
