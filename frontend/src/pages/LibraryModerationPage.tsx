import React, { useState, useEffect } from 'react';
import { useAuth } from '@/context/AuthContext';
import { libraryService } from '@/services/libraryService';
import {
  LibraryResourceSummary,
  LibraryReport,
  ModerationLog,
  ReportStatus,
} from '@/types';
import {
  ShieldAlert,
  ArrowLeft,
  CheckCircle,
  Clock,
  AlertTriangle,
  History,
  ShieldCheck,
  Loader2,
  Check,
  X,
} from 'lucide-react';

interface LibraryModerationPageProps {
  onBack: () => void;
  onSelectResource: (id: string) => void;
}

export const LibraryModerationPage: React.FC<LibraryModerationPageProps> = ({
  onBack,
  onSelectResource,
}) => {
  const { user } = useAuth();
  const [activeTab, setActiveTab] = useState<'queue' | 'reports' | 'audit'>('queue');

  // Queue state
  const [queueItems, setQueueItems] = useState<LibraryResourceSummary[]>([]);
  const [isQueueLoading, setIsQueueLoading] = useState(true);

  // Reports state
  const [reports, setReports] = useState<LibraryReport[]>([]);
  const [isReportsLoading, setIsReportsLoading] = useState(false);
  const [reportFilter, setReportFilter] = useState<ReportStatus>('PENDING');

  // Audit logs state
  const [auditLogs, setAuditLogs] = useState<ModerationLog[]>([]);
  const [isAuditLoading, setIsAuditLoading] = useState(false);

  // Action modals
  const [selectedResourceId, setSelectedResourceId] = useState<string | null>(null);
  const [selectedResourceTitle, setSelectedResourceTitle] = useState<string>('');
  const [isRejectModalOpen, setIsRejectModalOpen] = useState(false);
  const [rejectionReason, setRejectionReason] = useState('');

  // Report resolution modal
  const [selectedReportId, setSelectedReportId] = useState<string | null>(null);
  const [isResolveModalOpen, setIsResolveModalOpen] = useState(false);
  const [resolveAction, setResolveAction] = useState<ReportStatus>('RESOLVED');
  const [resolutionNotes, setResolutionNotes] = useState('');

  const [isActionSubmitting, setIsActionSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const isModerator = user?.role === 'EDUCATOR' || user?.role === 'ADMIN';

  const loadQueue = async () => {
    try {
      setIsQueueLoading(true);
      setError(null);
      const res = await libraryService.getModerationQueue(0, 50);
      setQueueItems(res.content || []);
    } catch (err: any) {
      setError(err?.response?.data?.message || 'Failed to load moderation queue.');
    } finally {
      setIsQueueLoading(false);
    }
  };

  const loadReports = async () => {
    try {
      setIsReportsLoading(true);
      setError(null);
      const res = await libraryService.getModerationReports(reportFilter, 0, 50);
      setReports(res.content || []);
    } catch (err: any) {
      setError(err?.response?.data?.message || 'Failed to load reports.');
    } finally {
      setIsReportsLoading(false);
    }
  };

  const loadAuditLogs = async () => {
    try {
      setIsAuditLoading(true);
      setError(null);
      const res = await libraryService.getGlobalModerationLogs(0, 50);
      setAuditLogs(res.content || []);
    } catch (err: any) {
      setError(err?.response?.data?.message || 'Failed to load moderation history.');
    } finally {
      setIsAuditLoading(false);
    }
  };

  useEffect(() => {
    if (activeTab === 'queue') loadQueue();
    else if (activeTab === 'reports') loadReports();
    else if (activeTab === 'audit') loadAuditLogs();
  }, [activeTab, reportFilter]);

  const handleApprove = async (id: string, title: string) => {
    if (!confirm(`Approve and publish "${title}" to the academic library?`)) return;
    try {
      setIsActionSubmitting(true);
      await libraryService.approveResource(id, { reason: 'Academic quality and licensing verified.' });
      await loadQueue();
    } catch (err: any) {
      alert(err?.response?.data?.message || 'Failed to approve resource.');
    } finally {
      setIsActionSubmitting(false);
    }
  };

  const openRejectModal = (id: string, title: string) => {
    setSelectedResourceId(id);
    setSelectedResourceTitle(title);
    setRejectionReason('');
    setIsRejectModalOpen(true);
  };

  const handleRejectSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedResourceId || !rejectionReason.trim()) return;
    try {
      setIsActionSubmitting(true);
      await libraryService.rejectResource(selectedResourceId, {
        reason: rejectionReason.trim(),
      });
      setIsRejectModalOpen(false);
      await loadQueue();
    } catch (err: any) {
      alert(err?.response?.data?.message || 'Failed to reject resource.');
    } finally {
      setIsActionSubmitting(false);
    }
  };

  const openResolveModal = (reportId: string) => {
    setSelectedReportId(reportId);
    setResolutionNotes('');
    setResolveAction('RESOLVED');
    setIsResolveModalOpen(true);
  };

  const handleResolveSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedReportId || !resolutionNotes.trim()) return;
    try {
      setIsActionSubmitting(true);
      await libraryService.resolveReport(selectedReportId, {
        action: resolveAction,
        resolutionNotes: resolutionNotes.trim(),
      });
      setIsResolveModalOpen(false);
      await loadReports();
    } catch (err: any) {
      alert(err?.response?.data?.message || 'Failed to record report resolution.');
    } finally {
      setIsActionSubmitting(false);
    }
  };

  if (!isModerator) {
    return (
      <div className="max-w-3xl mx-auto py-16 px-4 text-center">
        <div className="w-12 h-12 rounded-2xl bg-rose-500/10 text-rose-400 flex items-center justify-center mx-auto mb-3">
          <ShieldAlert className="w-6 h-6" />
        </div>
        <h2 className="text-lg font-bold text-white mb-1">Access Restricted</h2>
        <p className="text-xs text-slate-400 mb-4">
          Library moderation requires educator or administrator privileges.
        </p>
        <button
          onClick={onBack}
          className="px-4 py-2 bg-slate-800 text-white text-xs font-semibold rounded-xl"
        >
          Return to Library
        </button>
      </div>
    );
  }

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
            <ShieldAlert className="w-6 h-6 text-amber-400" /> Academic Moderation & Editorial Review
          </h1>
          <p className="text-xs text-slate-400 mt-1">
            Review submitted resources, enforce academic rights and licensing standards, and resolve reports.
          </p>
        </div>

        {/* Tab switcher */}
        <div className="flex items-center bg-slate-900 border border-slate-800 p-1 rounded-xl text-xs">
          <button
            onClick={() => setActiveTab('queue')}
            className={`px-3 py-1.5 rounded-lg font-medium transition flex items-center gap-1.5 ${
              activeTab === 'queue' ? 'bg-indigo-600 text-white font-semibold' : 'text-slate-400 hover:text-white'
            }`}
          >
            <Clock className="w-3.5 h-3.5" /> Pending Queue ({queueItems.length})
          </button>
          <button
            onClick={() => setActiveTab('reports')}
            className={`px-3 py-1.5 rounded-lg font-medium transition flex items-center gap-1.5 ${
              activeTab === 'reports' ? 'bg-indigo-600 text-white font-semibold' : 'text-slate-400 hover:text-white'
            }`}
          >
            <AlertTriangle className="w-3.5 h-3.5" /> User Reports
          </button>
          <button
            onClick={() => setActiveTab('audit')}
            className={`px-3 py-1.5 rounded-lg font-medium transition flex items-center gap-1.5 ${
              activeTab === 'audit' ? 'bg-indigo-600 text-white font-semibold' : 'text-slate-400 hover:text-white'
            }`}
          >
            <History className="w-3.5 h-3.5" /> Audit History
          </button>
        </div>
      </div>

      {error && (
        <div className="p-4 bg-rose-500/10 border border-rose-500/20 rounded-2xl text-xs text-rose-300">
          {error}
        </div>
      )}

      {/* TAB 1: PENDING QUEUE */}
      {activeTab === 'queue' && (
        <div>
          {isQueueLoading ? (
            <div className="flex flex-col items-center justify-center py-20 text-slate-400">
              <Loader2 className="w-8 h-8 animate-spin text-amber-400 mb-3" />
              <p className="text-xs">Loading pending review items...</p>
            </div>
          ) : queueItems.length === 0 ? (
            <div className="py-16 bg-slate-900/40 border border-slate-800 rounded-2xl text-center space-y-2">
              <CheckCircle className="w-10 h-10 text-emerald-400 mx-auto" />
              <h3 className="text-base font-semibold text-white">Queue is Clear!</h3>
              <p className="text-xs text-slate-400">All student submissions have been reviewed and moderated.</p>
            </div>
          ) : (
            <div className="space-y-4">
              {queueItems.map((item) => (
                <div
                  key={item.id}
                  className="bg-slate-900 border border-slate-800 rounded-2xl p-5 space-y-4"
                >
                  <div className="flex flex-col sm:flex-row sm:items-start justify-between gap-4">
                    <div className="space-y-1.5 flex-1">
                      <div className="flex flex-wrap items-center gap-2">
                        <span className="px-2 py-0.5 rounded text-[11px] font-semibold bg-amber-500/10 text-amber-400 border border-amber-500/20">
                          Pending Review
                        </span>
                        <span className="text-[11px] font-semibold text-indigo-400 bg-indigo-500/10 px-2 py-0.5 rounded">
                          {item.categoryDisplayName || item.category}
                        </span>
                        <span className="text-[11px] text-slate-400 bg-slate-800 px-2 py-0.5 rounded">
                          {item.resourceTypeDisplayName || item.resourceType}
                        </span>
                      </div>

                      <h3
                        onClick={() => onSelectResource(item.id)}
                        className="text-base font-bold text-white hover:text-indigo-400 transition cursor-pointer"
                      >
                        {item.title}
                      </h3>

                      <p className="text-xs text-slate-300 leading-relaxed line-clamp-3">
                        {item.description}
                      </p>

                      {/* Rights & Licensing info */}
                      <div className="flex flex-wrap items-center gap-3 pt-2 text-xs">
                        <div className="flex items-center gap-1.5 text-emerald-400">
                          <ShieldCheck className="w-3.5 h-3.5" />
                          <span>Rights: <strong>{item.rightsTypeDisplayName || item.rightsType}</strong></span>
                        </div>
                        <div className="text-slate-400">
                          License: <span className="font-mono text-emerald-300 bg-slate-950 px-1.5 py-0.5 rounded">{item.licenseName}</span>
                        </div>
                        <div className="text-slate-400">
                          File: {item.fileName} ({item.fileType})
                        </div>
                        <div className="text-slate-400">
                          Contributor: <strong className="text-slate-300">{item.contributorName}</strong>
                        </div>
                      </div>
                    </div>

                    {/* Action buttons */}
                    <div className="flex sm:flex-col gap-2 shrink-0 pt-2 sm:pt-0">
                      <button
                        onClick={() => handleApprove(item.id, item.title)}
                        disabled={isActionSubmitting}
                        className="px-3.5 py-2 bg-emerald-600 hover:bg-emerald-500 text-white text-xs font-semibold rounded-xl inline-flex items-center justify-center gap-1.5 transition"
                      >
                        <Check className="w-3.5 h-3.5" /> Approve
                      </button>

                      <button
                        onClick={() => openRejectModal(item.id, item.title)}
                        disabled={isActionSubmitting}
                        className="px-3.5 py-2 bg-rose-600 hover:bg-rose-500 text-white text-xs font-semibold rounded-xl inline-flex items-center justify-center gap-1.5 transition"
                      >
                        <X className="w-3.5 h-3.5" /> Reject
                      </button>

                      <button
                        onClick={() => onSelectResource(item.id)}
                        className="px-3.5 py-1.5 bg-slate-800 hover:bg-slate-700 text-slate-300 text-xs font-medium rounded-xl inline-flex items-center justify-center gap-1.5 transition"
                      >
                        Inspect Details
                      </button>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      )}

      {/* TAB 2: USER REPORTS */}
      {activeTab === 'reports' && (
        <div className="space-y-4">
          <div className="flex items-center gap-2 pb-2">
            {(['PENDING', 'RESOLVED', 'DISMISSED'] as ReportStatus[]).map((st) => (
              <button
                key={st}
                onClick={() => setReportFilter(st)}
                className={`px-3 py-1.5 rounded-xl text-xs font-medium transition ${
                  reportFilter === st
                    ? 'bg-indigo-600 text-white font-semibold'
                    : 'bg-slate-900 text-slate-400 hover:text-white border border-slate-800'
                }`}
              >
                {st}
              </button>
            ))}
          </div>

          {isReportsLoading ? (
            <div className="flex flex-col items-center justify-center py-20 text-slate-400">
              <Loader2 className="w-8 h-8 animate-spin text-amber-400 mb-3" />
              <p className="text-xs">Loading user reports...</p>
            </div>
          ) : reports.length === 0 ? (
            <div className="py-16 bg-slate-900/40 border border-slate-800 rounded-2xl text-center">
              <p className="text-xs text-slate-400">No {reportFilter.toLowerCase()} reports found.</p>
            </div>
          ) : (
            <div className="space-y-3">
              {reports.map((rep) => (
                <div
                  key={rep.id}
                  className="bg-slate-900 border border-slate-800 rounded-2xl p-4 sm:p-5 flex flex-col sm:flex-row sm:items-start justify-between gap-4"
                >
                  <div className="space-y-2 flex-1">
                    <div className="flex flex-wrap items-center gap-2">
                      <span className="px-2 py-0.5 rounded text-[11px] font-semibold bg-rose-500/10 text-rose-400 border border-rose-500/20">
                        {rep.reasonCategoryDisplayName || rep.reasonCategory}
                      </span>
                      <span className="text-[11px] text-slate-400 bg-slate-800 px-2 py-0.5 rounded">
                        Status: {rep.status}
                      </span>
                    </div>

                    <h4
                      onClick={() => onSelectResource(rep.resourceId)}
                      className="text-sm font-bold text-white hover:text-indigo-400 transition cursor-pointer"
                    >
                      Reported Resource: {rep.resourceTitle}
                    </h4>

                    <p className="text-xs text-slate-300 italic bg-slate-950 p-2.5 rounded-xl border border-slate-800">
                      "{rep.explanation}"
                    </p>

                    <div className="text-[11px] text-slate-500">
                      Reported by {rep.reporterName} on {new Date(rep.createdAt).toLocaleString()}
                    </div>

                    {rep.resolutionNotes && (
                      <div className="text-xs text-emerald-400 pt-1">
                        <strong>Resolution Note:</strong> {rep.resolutionNotes} (by {rep.resolvedByName})
                      </div>
                    )}
                  </div>

                  {rep.status === 'PENDING' && (
                    <div className="flex sm:flex-col gap-2 shrink-0">
                      <button
                        onClick={() => openResolveModal(rep.id)}
                        className="px-3.5 py-1.5 bg-indigo-600 hover:bg-indigo-500 text-white text-xs font-semibold rounded-xl transition"
                      >
                        Resolve Report
                      </button>
                      <button
                        onClick={() => onSelectResource(rep.resourceId)}
                        className="px-3.5 py-1.5 bg-slate-800 hover:bg-slate-700 text-slate-300 text-xs font-medium rounded-xl transition"
                      >
                        Inspect Resource
                      </button>
                    </div>
                  )}
                </div>
              ))}
            </div>
          )}
        </div>
      )}

      {/* TAB 3: AUDIT HISTORY */}
      {activeTab === 'audit' && (
        <div>
          {isAuditLoading ? (
            <div className="flex flex-col items-center justify-center py-20 text-slate-400">
              <Loader2 className="w-8 h-8 animate-spin text-amber-400 mb-3" />
              <p className="text-xs">Loading audit logs...</p>
            </div>
          ) : auditLogs.length === 0 ? (
            <div className="py-16 bg-slate-900/40 border border-slate-800 rounded-2xl text-center">
              <p className="text-xs text-slate-400">No moderation audit history found.</p>
            </div>
          ) : (
            <div className="bg-slate-900 border border-slate-800 rounded-2xl divide-y divide-slate-800 overflow-hidden text-xs">
              {auditLogs.map((log) => (
                <div key={log.id} className="p-4 flex flex-col sm:flex-row sm:items-center justify-between gap-3">
                  <div className="space-y-1">
                    <div className="flex items-center gap-2">
                      <span className="font-bold text-white uppercase text-[11px] px-2 py-0.5 rounded bg-slate-800">
                        {log.action}
                      </span>
                      <span className="text-slate-300 font-medium">{log.resourceTitle}</span>
                      <span className="text-slate-500 text-[11px]">
                        ({log.previousStatus} → {log.newStatus})
                      </span>
                    </div>
                    {log.reason && (
                      <p className="text-slate-400 text-xs mt-0.5">
                        <span className="text-slate-500">Reason:</span> {log.reason}
                      </p>
                    )}
                    <div className="text-[11px] text-slate-500">
                      Moderator: <strong className="text-slate-400">{log.moderatorName}</strong>
                    </div>
                  </div>

                  <span className="text-[11px] text-slate-500 shrink-0">
                    {new Date(log.createdAt).toLocaleString()}
                  </span>
                </div>
              ))}
            </div>
          )}
        </div>
      )}

      {/* Rejection Modal */}
      {isRejectModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/70 backdrop-blur-sm">
          <div className="bg-slate-900 border border-slate-800 rounded-2xl p-6 max-w-md w-full">
            <h3 className="text-base font-semibold text-white mb-2">Reject Resource</h3>
            <p className="text-xs text-slate-400 mb-4">
              Enter constructive feedback for "{selectedResourceTitle}". The contributor can revise and resubmit.
            </p>
            <form onSubmit={handleRejectSubmit}>
              <textarea
                value={rejectionReason}
                onChange={(e) => setRejectionReason(e.target.value)}
                rows={4}
                required
                placeholder="Detailed reason for rejection..."
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
                  disabled={isActionSubmitting || !rejectionReason.trim()}
                  className="px-4 py-1.5 bg-rose-600 hover:bg-rose-500 text-white text-xs font-semibold rounded-xl disabled:opacity-50"
                >
                  Confirm Rejection
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Resolve Report Modal */}
      {isResolveModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/70 backdrop-blur-sm">
          <div className="bg-slate-900 border border-slate-800 rounded-2xl p-6 max-w-md w-full">
            <h3 className="text-base font-semibold text-white mb-2">Resolve User Report</h3>
            <form onSubmit={handleResolveSubmit} className="space-y-4">
              <div>
                <label className="block text-xs font-semibold uppercase text-slate-400 mb-1">
                  Action
                </label>
                <select
                  value={resolveAction}
                  onChange={(e) => setResolveAction(e.target.value as ReportStatus)}
                  className="w-full bg-slate-950 border border-slate-800 rounded-xl px-3 py-2 text-xs text-white"
                >
                  <option value="RESOLVED">Resolve (Action Taken / Confirmed)</option>
                  <option value="DISMISSED">Dismiss (False Report / No Issue)</option>
                </select>
              </div>

              <div>
                <label className="block text-xs font-semibold uppercase text-slate-400 mb-1">
                  Resolution Notes *
                </label>
                <textarea
                  value={resolutionNotes}
                  onChange={(e) => setResolutionNotes(e.target.value)}
                  rows={3}
                  required
                  placeholder="Explain what steps were taken or why this was dismissed..."
                  className="w-full bg-slate-950 border border-slate-800 rounded-xl p-3 text-xs text-white placeholder-slate-500"
                />
              </div>

              <div className="flex justify-end gap-2 pt-2">
                <button
                  type="button"
                  onClick={() => setIsResolveModalOpen(false)}
                  className="px-3.5 py-1.5 text-xs text-slate-300 hover:text-white"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={isActionSubmitting || !resolutionNotes.trim()}
                  className="px-4 py-1.5 bg-indigo-600 hover:bg-indigo-500 text-white text-xs font-semibold rounded-xl disabled:opacity-50"
                >
                  Submit Resolution
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
