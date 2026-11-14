import React, { useState, useEffect } from 'react';
import { useAuth } from '@/context/AuthContext';
import { libraryService } from '@/services/libraryService';
import { LibraryResourceDetail, ModerationLog } from '@/types';
import { ReportResourceModal } from '@/components/library/ReportResourceModal';
import {
  Download,
  ShieldCheck,
  AlertTriangle,
  FileText,
  User,
  Calendar,
  Tag,
  ArrowLeft,
  Loader2,
  CheckCircle,
  XCircle,
  PauseCircle,
  RotateCcw,
  Clock,
  ShieldAlert,
} from 'lucide-react';

interface LibraryResourceDetailPageProps {
  resourceId: string;
  onBack: () => void;
  onEdit?: (resourceId: string) => void;
}

export const LibraryResourceDetailPage: React.FC<LibraryResourceDetailPageProps> = ({
  resourceId,
  onBack,
  onEdit,
}) => {
  const { user } = useAuth();
  const [resource, setResource] = useState<LibraryResourceDetail | null>(null);
  const [auditLogs, setAuditLogs] = useState<ModerationLog[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [isDownloading, setIsDownloading] = useState(false);
  const [isReportModalOpen, setIsReportModalOpen] = useState(false);

  // Moderation action modal states
  const [isRejectModalOpen, setIsRejectModalOpen] = useState(false);
  const [isSuspendModalOpen, setIsSuspendModalOpen] = useState(false);
  const [actionReason, setActionReason] = useState('');
  const [isActionSubmitting, setIsActionSubmitting] = useState(false);

  const isModerator = user?.role === 'EDUCATOR' || user?.role === 'ADMIN';

  const loadResource = async () => {
    try {
      setIsLoading(true);
      setError(null);
      const data = await libraryService.getResourceDetail(resourceId);
      setResource(data);

      if (isModerator || data.canModerate) {
        try {
          const logs = await libraryService.getResourceModerationLogs(resourceId);
          setAuditLogs(logs);
        } catch (ignored) {}
      }
    } catch (err: any) {
      setError(err?.response?.data?.message || 'Failed to load library resource details.');
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    loadResource();
  }, [resourceId]);

  const handleDownload = async () => {
    if (!resource) return;
    try {
      setIsDownloading(true);
      const res = await libraryService.downloadResource(resource.id);
      if (res?.downloadUrl) {
        window.open(res.downloadUrl, '_blank');
        // Refresh detail to get updated download counter
        setResource((prev) => prev ? { ...prev, downloadsCount: prev.downloadsCount + 1 } : null);
      }
    } catch (err: any) {
      alert(err?.response?.data?.message || 'Unable to download resource.');
    } finally {
      setIsDownloading(false);
    }
  };

  const handleApprove = async () => {
    if (!resource || !confirm('Are you sure you want to approve and publish this resource?')) return;
    try {
      setIsActionSubmitting(true);
      await libraryService.approveResource(resource.id, { reason: 'Meets academic standards' });
      await loadResource();
    } catch (err: any) {
      alert(err?.response?.data?.message || 'Failed to approve resource.');
    } finally {
      setIsActionSubmitting(false);
    }
  };

  const handleRejectSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!resource || !actionReason.trim()) return;
    try {
      setIsActionSubmitting(true);
      await libraryService.rejectResource(resource.id, { reason: actionReason.trim() });
      setIsRejectModalOpen(false);
      setActionReason('');
      await loadResource();
    } catch (err: any) {
      alert(err?.response?.data?.message || 'Failed to reject resource.');
    } finally {
      setIsActionSubmitting(false);
    }
  };

  const handleSuspendSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!resource || !actionReason.trim()) return;
    try {
      setIsActionSubmitting(true);
      await libraryService.suspendResource(resource.id, { reason: actionReason.trim() });
      setIsSuspendModalOpen(false);
      setActionReason('');
      await loadResource();
    } catch (err: any) {
      alert(err?.response?.data?.message || 'Failed to suspend resource.');
    } finally {
      setIsActionSubmitting(false);
    }
  };

  const handleRestore = async () => {
    if (!resource || !confirm('Restore this suspended resource back to the public library?')) return;
    try {
      setIsActionSubmitting(true);
      await libraryService.restoreResource(resource.id, { reason: 'Restored by moderator' });
      await loadResource();
    } catch (err: any) {
      alert(err?.response?.data?.message || 'Failed to restore resource.');
    } finally {
      setIsActionSubmitting(false);
    }
  };

  const formatFileSize = (bytes?: number) => {
    if (!bytes) return 'Unknown size';
    if (bytes < 1024) return `${bytes} B`;
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
    return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
  };

  if (isLoading) {
    return (
      <div className="flex flex-col items-center justify-center py-24 text-slate-400">
        <Loader2 className="w-8 h-8 animate-spin text-indigo-500 mb-3" />
        <p className="text-sm">Loading resource details...</p>
      </div>
    );
  }

  if (error || !resource) {
    return (
      <div className="max-w-4xl mx-auto py-12 px-4">
        <div className="bg-rose-500/10 border border-rose-500/20 rounded-2xl p-6 text-center">
          <AlertTriangle className="w-10 h-10 text-rose-400 mx-auto mb-3" />
          <h3 className="text-base font-semibold text-white mb-1">Resource Not Accessible</h3>
          <p className="text-xs text-rose-300 mb-4">{error || 'Resource not found'}</p>
          <button
            onClick={onBack}
            className="px-4 py-2 bg-slate-800 hover:bg-slate-700 text-white text-xs font-semibold rounded-xl inline-flex items-center gap-2"
          >
            <ArrowLeft className="w-4 h-4" /> Back to Library
          </button>
        </div>
      </div>
    );
  }

  const getStatusBadge = () => {
    switch (resource.status) {
      case 'PUBLISHED':
        return (
          <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-semibold bg-emerald-500/10 text-emerald-400 border border-emerald-500/20">
            <CheckCircle className="w-3.5 h-3.5" /> Published
          </span>
        );
      case 'PENDING_REVIEW':
        return (
          <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-semibold bg-amber-500/10 text-amber-400 border border-amber-500/20">
            <Clock className="w-3.5 h-3.5" /> Pending Review
          </span>
        );
      case 'REJECTED':
        return (
          <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-semibold bg-rose-500/10 text-rose-400 border border-rose-500/20">
            <XCircle className="w-3.5 h-3.5" /> Rejected
          </span>
        );
      case 'SUSPENDED':
        return (
          <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-semibold bg-orange-500/10 text-orange-400 border border-orange-500/20">
            <PauseCircle className="w-3.5 h-3.5" /> Suspended
          </span>
        );
      default:
        return (
          <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-semibold bg-slate-500/10 text-slate-400 border border-slate-500/20">
            Draft
          </span>
        );
    }
  };

  return (
    <div className="max-w-5xl mx-auto py-8 px-4 sm:px-6 space-y-6">
      {/* Top back navigation */}
      <div className="flex items-center justify-between">
        <button
          onClick={onBack}
          className="inline-flex items-center gap-2 text-xs font-semibold text-slate-400 hover:text-white transition"
        >
          <ArrowLeft className="w-4 h-4" /> Back to Library
        </button>
        <div className="flex items-center gap-2">
          {getStatusBadge()}
          <button
            onClick={() => setIsReportModalOpen(true)}
            className="px-3 py-1.5 text-xs font-medium text-slate-400 hover:text-rose-400 bg-slate-900 border border-slate-800 rounded-xl hover:border-rose-500/30 transition inline-flex items-center gap-1.5"
          >
            <ShieldAlert className="w-3.5 h-3.5" /> Report Issue
          </button>
        </div>
      </div>

      {/* Rejection / Suspension Notice */}
      {resource.rejectionReason && (
        <div className="p-4 bg-rose-500/10 border border-rose-500/20 rounded-2xl flex items-start gap-3">
          <AlertTriangle className="w-5 h-5 text-rose-400 shrink-0 mt-0.5" />
          <div className="text-xs">
            <p className="font-semibold text-rose-300">
              {resource.status === 'REJECTED' ? 'Editorial Feedback / Reason for Rejection' : 'Suspension Notice'}
            </p>
            <p className="text-rose-200 mt-1 whitespace-pre-wrap">{resource.rejectionReason}</p>
          </div>
        </div>
      )}

      {/* Main Resource Card */}
      <div className="bg-slate-900 border border-slate-800 rounded-2xl p-6 sm:p-8 space-y-6">
        <div>
          <div className="flex flex-wrap items-center gap-2 mb-3">
            <span className="px-2.5 py-1 rounded-lg text-xs font-semibold bg-indigo-500/10 text-indigo-400 border border-indigo-500/20">
              {resource.categoryDisplayName || resource.category}
            </span>
            <span className="px-2.5 py-1 rounded-lg text-xs font-medium bg-slate-800 text-slate-300 border border-slate-700">
              {resource.resourceTypeDisplayName || resource.resourceType}
            </span>
            <span className="px-2.5 py-1 rounded-lg text-xs font-medium bg-slate-800 text-slate-400 border border-slate-700">
              {resource.academicLevelDisplayName || resource.academicLevel}
            </span>
          </div>

          <h1 className="text-2xl sm:text-3xl font-bold text-white tracking-tight">{resource.title}</h1>

          <div className="flex flex-wrap items-center gap-4 mt-4 text-xs text-slate-400 border-b border-slate-800/80 pb-4">
            <div className="flex items-center gap-1.5">
              <User className="w-4 h-4 text-slate-500" />
              <span>Contributed by <strong className="text-slate-300">{resource.contributorName}</strong></span>
            </div>
            <div className="flex items-center gap-1.5">
              <Calendar className="w-4 h-4 text-slate-500" />
              <span>
                {resource.publishedAt
                  ? `Published on ${new Date(resource.publishedAt).toLocaleDateString()}`
                  : `Created on ${new Date(resource.createdAt).toLocaleDateString()}`}
              </span>
            </div>
            <div className="flex items-center gap-1.5">
              <Download className="w-4 h-4 text-slate-500" />
              <span>{resource.downloadsCount} downloads</span>
            </div>
          </div>
        </div>

        {/* Description */}
        <div>
          <h3 className="text-xs font-semibold uppercase tracking-wider text-slate-400 mb-2">
            Resource Description
          </h3>
          <p className="text-sm text-slate-300 leading-relaxed whitespace-pre-wrap">
            {resource.description}
          </p>
        </div>

        {/* Tags */}
        {resource.tags && resource.tags.length > 0 && (
          <div className="flex flex-wrap items-center gap-2 pt-2">
            <Tag className="w-3.5 h-3.5 text-slate-500" />
            {resource.tags.map((t) => (
              <span
                key={t}
                className="px-2.5 py-1 text-xs rounded-lg bg-slate-800/80 text-slate-300 border border-slate-700/60"
              >
                #{t}
              </span>
            ))}
          </div>
        )}

        {/* Rights Declaration & Licensing Card (Crucial for Phase 8) */}
        <div className="p-5 bg-slate-950/70 border border-slate-800 rounded-xl space-y-3">
          <div className="flex items-center gap-2.5 text-emerald-400">
            <ShieldCheck className="w-5 h-5" />
            <h4 className="text-sm font-semibold">Rights Declaration & Academic Licensing</h4>
          </div>
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 text-xs pt-1">
            <div>
              <span className="text-slate-500 block mb-0.5">Rights Category</span>
              <span className="font-semibold text-white">
                {resource.rightsTypeDisplayName || resource.rightsType}
              </span>
            </div>
            <div>
              <span className="text-slate-500 block mb-0.5">License Specification</span>
              <span className="font-mono font-medium text-emerald-400 bg-emerald-500/10 px-2 py-0.5 rounded border border-emerald-500/20">
                {resource.licenseName}
              </span>
            </div>
            {resource.sourceAttribution && (
              <div className="sm:col-span-2">
                <span className="text-slate-500 block mb-0.5">Attribution & Origin Details</span>
                <span className="text-slate-300 italic">
                  "{resource.sourceAttribution}"
                </span>
              </div>
            )}
          </div>
          <p className="text-[11px] text-slate-500 pt-2 border-t border-slate-800/80">
            This resource was submitted under verified educational sharing rights. Downloaded copies are subject to the specified license terms.
          </p>
        </div>

        {/* File Details & Download Action */}
        <div className="flex flex-col sm:flex-row sm:items-center justify-between p-4 bg-slate-800/40 border border-slate-800 rounded-xl gap-4">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-xl bg-indigo-500/10 border border-indigo-500/20 flex items-center justify-center text-indigo-400 shrink-0">
              <FileText className="w-5 h-5" />
            </div>
            <div className="text-xs">
              <p className="font-semibold text-white truncate max-w-[280px] sm:max-w-md">{resource.fileName}</p>
              <p className="text-slate-400">
                {resource.fileType} • {formatFileSize(resource.fileSize)}
                {resource.pageCount ? ` • ${resource.pageCount} pages` : ''}
              </p>
            </div>
          </div>

          <div className="flex items-center gap-2">
            {resource.canDownload && (
              <button
                onClick={handleDownload}
                disabled={isDownloading}
                className="w-full sm:w-auto px-5 py-2.5 bg-indigo-600 hover:bg-indigo-500 text-white text-xs font-semibold rounded-xl flex items-center justify-center gap-2 shadow-lg shadow-indigo-600/20 transition disabled:opacity-50"
              >
                {isDownloading ? <Loader2 className="w-4 h-4 animate-spin" /> : <Download className="w-4 h-4" />}
                <span>Download Resource</span>
              </button>
            )}

            {resource.canEdit && onEdit && (
              <button
                onClick={() => onEdit(resource.id)}
                className="w-full sm:w-auto px-4 py-2.5 bg-slate-800 hover:bg-slate-700 text-white text-xs font-semibold rounded-xl transition"
              >
                Edit Submission
              </button>
            )}
          </div>
        </div>

        {/* Moderator Actions Toolbar (Visible only to authorized educators/admins) */}
        {isModerator && (
          <div className="pt-6 border-t border-slate-800 space-y-3">
            <div className="flex items-center justify-between">
              <h4 className="text-xs font-bold uppercase tracking-wider text-amber-400 flex items-center gap-1.5">
                <ShieldAlert className="w-4 h-4" /> Moderator Controls
              </h4>
              <span className="text-[11px] text-slate-500">Authorized: {user?.role}</span>
            </div>

            <div className="flex flex-wrap items-center gap-2">
              {resource.status === 'PENDING_REVIEW' && (
                <>
                  <button
                    onClick={handleApprove}
                    disabled={isActionSubmitting}
                    className="px-3.5 py-2 bg-emerald-600 hover:bg-emerald-500 text-white text-xs font-semibold rounded-xl flex items-center gap-1.5 transition"
                  >
                    <CheckCircle className="w-3.5 h-3.5" /> Approve & Publish
                  </button>
                  <button
                    onClick={() => setIsRejectModalOpen(true)}
                    disabled={isActionSubmitting}
                    className="px-3.5 py-2 bg-rose-600 hover:bg-rose-500 text-white text-xs font-semibold rounded-xl flex items-center gap-1.5 transition"
                  >
                    <XCircle className="w-3.5 h-3.5" /> Reject with Reason
                  </button>
                </>
              )}

              {resource.status === 'PUBLISHED' && (
                <button
                  onClick={() => setIsSuspendModalOpen(true)}
                  disabled={isActionSubmitting}
                  className="px-3.5 py-2 bg-orange-600 hover:bg-orange-500 text-white text-xs font-semibold rounded-xl flex items-center gap-1.5 transition"
                >
                  <PauseCircle className="w-3.5 h-3.5" /> Suspend Resource
                </button>
              )}

              {resource.status === 'SUSPENDED' && (
                <button
                  onClick={handleRestore}
                  disabled={isActionSubmitting}
                  className="px-3.5 py-2 bg-emerald-600 hover:bg-emerald-500 text-white text-xs font-semibold rounded-xl flex items-center gap-1.5 transition"
                >
                  <RotateCcw className="w-3.5 h-3.5" /> Restore to Library
                </button>
              )}
            </div>
          </div>
        )}

        {/* Audit Trail Section */}
        {auditLogs.length > 0 && (
          <div className="pt-6 border-t border-slate-800 space-y-3">
            <h4 className="text-xs font-bold uppercase tracking-wider text-slate-400">
              Moderation Audit History
            </h4>
            <div className="divide-y divide-slate-800 text-xs">
              {auditLogs.map((log) => (
                <div key={log.id} className="py-2.5 flex items-start justify-between">
                  <div>
                    <div className="flex items-center gap-2">
                      <span className="font-semibold text-white">{log.action}</span>
                      <span className="text-slate-500 text-[11px]">
                        ({log.previousStatus} → {log.newStatus})
                      </span>
                    </div>
                    {log.reason && <p className="text-slate-400 text-xs mt-0.5">{log.reason}</p>}
                    <span className="text-[11px] text-slate-500">Moderated by {log.moderatorName}</span>
                  </div>
                  <span className="text-[11px] text-slate-500">
                    {new Date(log.createdAt).toLocaleString()}
                  </span>
                </div>
              ))}
            </div>
          </div>
        )}
      </div>

      {/* Reject Modal */}
      {isRejectModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/70 backdrop-blur-sm">
          <div className="bg-slate-900 border border-slate-800 rounded-2xl p-6 max-w-md w-full">
            <h3 className="text-base font-semibold text-white mb-2">Reject Submission</h3>
            <p className="text-xs text-slate-400 mb-4">
              Please provide clear, constructive feedback on why this submission cannot be published. The contributor will be able to revise and resubmit.
            </p>
            <form onSubmit={handleRejectSubmit}>
              <textarea
                value={actionReason}
                onChange={(e) => setActionReason(e.target.value)}
                rows={4}
                required
                placeholder="Reason for rejection (e.g., license is missing, poor scan quality, inaccurate attribution)..."
                className="w-full bg-slate-950 border border-slate-800 rounded-xl p-3 text-xs text-white placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-rose-500/50"
              />
              <div className="flex justify-end gap-2 mt-4">
                <button
                  type="button"
                  onClick={() => setIsRejectModalOpen(false)}
                  className="px-3.5 py-1.5 text-xs text-slate-300 hover:text-white"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={isActionSubmitting || !actionReason.trim()}
                  className="px-4 py-1.5 bg-rose-600 hover:bg-rose-500 text-white text-xs font-semibold rounded-xl disabled:opacity-50"
                >
                  Confirm Rejection
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Suspend Modal */}
      {isSuspendModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/70 backdrop-blur-sm">
          <div className="bg-slate-900 border border-slate-800 rounded-2xl p-6 max-w-md w-full">
            <h3 className="text-base font-semibold text-white mb-2">Suspend Published Resource</h3>
            <p className="text-xs text-slate-400 mb-4">
              Enter the reason for suspending this published resource (e.g. copyright takedown inquiry or inaccurate metadata).
            </p>
            <form onSubmit={handleSuspendSubmit}>
              <textarea
                value={actionReason}
                onChange={(e) => setActionReason(e.target.value)}
                rows={4}
                required
                placeholder="Reason for suspension..."
                className="w-full bg-slate-950 border border-slate-800 rounded-xl p-3 text-xs text-white placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-orange-500/50"
              />
              <div className="flex justify-end gap-2 mt-4">
                <button
                  type="button"
                  onClick={() => setIsSuspendModalOpen(false)}
                  className="px-3.5 py-1.5 text-xs text-slate-300 hover:text-white"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={isActionSubmitting || !actionReason.trim()}
                  className="px-4 py-1.5 bg-orange-600 hover:bg-orange-500 text-white text-xs font-semibold rounded-xl disabled:opacity-50"
                >
                  Confirm Suspension
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Report Modal */}
      <ReportResourceModal
        isOpen={isReportModalOpen}
        resourceId={resource.id}
        resourceTitle={resource.title}
        onClose={() => setIsReportModalOpen(false)}
      />
    </div>
  );
};
