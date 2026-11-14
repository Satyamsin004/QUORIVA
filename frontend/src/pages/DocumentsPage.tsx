import React, { useState, useEffect, useCallback, useRef, useMemo } from 'react';
import { useWorkspace } from '@/context/WorkspaceContext';
import { documentService } from '@/services/documentService';
import { DocumentItem } from '@/types/document';
import { DocumentStatusBadge } from '@/components/documents/DocumentStatusBadge';
import { DocumentUploadDialog } from '@/components/documents/DocumentUploadDialog';
import { DocumentDetailsModal } from '@/components/documents/DocumentDetailsModal';
import { DocumentDeleteDialog } from '@/components/documents/DocumentDeleteDialog';
import {
  FileText,
  Search,
  Upload,
  Download,
  Trash2,
  Eye,
  RefreshCw,
  Loader2,
  Filter,
  AlertCircle,
  FolderOpen,
  Sparkles,
  Layers,
  PlusCircle,
  Tag,
} from 'lucide-react';

const getSubjectIcon = (subjectName: string): string => {
  const s = (subjectName || '').toLowerCase();
  if (s.includes('java') || s.includes('spring')) return '☕';
  if (s.includes('sql') || s.includes('dbms') || s.includes('database')) return '🗄️';
  if (s.includes('python') || s.includes('py')) return '🐍';
  if (s.includes('operating') || s.includes('os') || s.includes('linux')) return '💻';
  if (s.includes('toc') || s.includes('automata') || s.includes('computation')) return '📐';
  if (s.includes('network') || s.includes('cn') || s.includes('distributed')) return '🌐';
  if (s.includes('algorithm') || s.includes('dsa') || s.includes('data structure')) return '⚡';
  return '📚';
};

export const DocumentsPage: React.FC = () => {
  const { currentWorkspace } = useWorkspace();

  const [documents, setDocuments] = useState<DocumentItem[]>([]);
  const [page, setPage] = useState(0);
  const [pageSize] = useState(50);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // Filters
  const [searchQuery, setSearchQuery] = useState('');
  const [typeFilter, setTypeFilter] = useState<string>('ALL');
  const [statusFilter, setStatusFilter] = useState<string>('ALL');
  const [selectedCategory, setSelectedCategory] = useState<string>('ALL');

  // Modals
  const [isUploadOpen, setIsUploadOpen] = useState(false);
  const [uploadSubject, setUploadSubject] = useState('Java');
  const [selectedDetailsDoc, setSelectedDetailsDoc] = useState<DocumentItem | null>(null);
  const [selectedDeleteDoc, setSelectedDeleteDoc] = useState<DocumentItem | null>(null);

  // Workspace tracking ref
  const activeWorkspaceIdRef = useRef<string | null>(null);
  const pollingTimerRef = useRef<NodeJS.Timeout | null>(null);

  const stopPolling = useCallback(() => {
    if (pollingTimerRef.current) {
      clearInterval(pollingTimerRef.current);
      pollingTimerRef.current = null;
    }
  }, []);

  const fetchDocuments = useCallback(
    async (silent = false) => {
      const targetWorkspaceId = currentWorkspace?.id;
      if (!targetWorkspaceId) return;

      if (!silent) setIsLoading(true);
      setError(null);

      try {
        const response = await documentService.listDocuments({
          workspaceId: targetWorkspaceId,
          page,
          size: pageSize,
        });

        if (activeWorkspaceIdRef.current === targetWorkspaceId) {
          setDocuments(response.content || []);
        }
      } catch (err: unknown) {
        if (activeWorkspaceIdRef.current === targetWorkspaceId) {
          console.error('Failed to fetch documents:', err);
          setError('Failed to load course materials for this workspace.');
        }
      } finally {
        if (activeWorkspaceIdRef.current === targetWorkspaceId && !silent) {
          setIsLoading(false);
        }
      }
    },
    [currentWorkspace?.id, page, pageSize]
  );

  useEffect(() => {
    const currentId = currentWorkspace?.id || null;
    activeWorkspaceIdRef.current = currentId;

    stopPolling();
    setDocuments([]);
    setPage(0);

    if (currentId) {
      fetchDocuments(false);
    }

    return () => {
      stopPolling();
    };
  }, [currentWorkspace?.id, fetchDocuments, stopPolling]);

  // Determine if polling is needed for documents currently being processed
  const hasInProgressDocuments = documents.some(
    (d) =>
      d.processingStatus === 'PROCESSING' ||
      d.processingStatus === 'PENDING_PROCESSING' ||
      d.processingStatus === 'UPLOADED' ||
      (d.processingStatus === 'PROCESSED' &&
        d.embeddingStatus !== 'EMBEDDED' &&
        d.embeddingStatus !== 'COMPLETED' &&
        d.embeddingStatus !== 'READY' &&
        d.embeddingStatus !== 'PARTIALLY_READY' &&
        d.embeddingStatus !== 'FAILED')
  );

  useEffect(() => {
    if (hasInProgressDocuments && currentWorkspace?.id) {
      if (!pollingTimerRef.current) {
        pollingTimerRef.current = setInterval(() => {
          fetchDocuments(true);
        }, 4000);
      }
    } else {
      stopPolling();
    }

    return () => {
      stopPolling();
    };
  }, [hasInProgressDocuments, currentWorkspace?.id, fetchDocuments, stopPolling]);

  // Subject Statistics Grouping across all documents in workspace
  const categoryStats = useMemo(() => {
    const stats: Record<string, { count: number; pages: number; readyCount: number; docs: DocumentItem[] }> = {};
    documents.forEach((doc) => {
      const cat = (doc.subject && doc.subject.trim()) || 'General';
      if (!stats[cat]) {
        stats[cat] = { count: 0, pages: 0, readyCount: 0, docs: [] };
      }
      stats[cat].count += 1;
      stats[cat].pages += doc.pageCount || 0;
      if (
        doc.processingStatus === 'PROCESSED' &&
        (doc.embeddingStatus === 'EMBEDDED' ||
          doc.embeddingStatus === 'COMPLETED' ||
          doc.embeddingStatus === 'READY' ||
          doc.embeddingStatus === 'PARTIALLY_READY')
      ) {
        stats[cat].readyCount += 1;
      }
      stats[cat].docs.push(doc);
    });
    return stats;
  }, [documents]);

  const uniqueCategories = useMemo(() => Object.keys(categoryStats).sort(), [categoryStats]);

  // Client-side filtering
  const filteredDocuments = useMemo(() => {
    return documents.filter((doc) => {
      const displayName = doc.originalFilename || doc.originalFileName || doc.title;
      const titleMatch =
        !searchQuery.trim() ||
        displayName.toLowerCase().includes(searchQuery.trim().toLowerCase());

      const typeMatch =
        typeFilter === 'ALL' ||
        doc.fileType.toUpperCase() === typeFilter.toUpperCase();

      let statusMatch = true;
      if (statusFilter === 'READY') {
        statusMatch =
          doc.processingStatus === 'PROCESSED' &&
          (doc.embeddingStatus === 'EMBEDDED' || doc.embeddingStatus === 'COMPLETED');
      } else if (statusFilter === 'EXTRACTING') {
        statusMatch = doc.processingStatus === 'PROCESSING';
      } else if (statusFilter === 'WAITING_EMBEDDINGS') {
        statusMatch =
          doc.processingStatus === 'PROCESSED' &&
          (doc.embeddingStatus === 'PENDING' ||
            doc.embeddingStatus === 'NOT_STARTED' ||
            doc.embeddingStatus === 'NOT_EMBEDDED');
      } else if (statusFilter === 'EMBEDDING') {
        statusMatch =
          doc.processingStatus === 'PROCESSED' &&
          (doc.embeddingStatus === 'IN_PROGRESS' ||
            doc.embeddingStatus === 'EMBEDDING_IN_PROGRESS');
      } else if (statusFilter === 'FAILED') {
        statusMatch =
          doc.processingStatus === 'FAILED' || doc.embeddingStatus === 'FAILED';
      }

      const categoryMatch =
        selectedCategory === 'ALL' ||
        ((doc.subject && doc.subject.trim()) || 'General').toLowerCase() === selectedCategory.toLowerCase();

      return titleMatch && typeMatch && statusMatch && categoryMatch;
    });
  }, [documents, searchQuery, typeFilter, statusFilter, selectedCategory]);

  // Group filtered documents by subject category
  const filteredGroups = useMemo(() => {
    const groups: Record<string, DocumentItem[]> = {};
    filteredDocuments.forEach((doc) => {
      const cat = (doc.subject && doc.subject.trim()) || 'General';
      if (!groups[cat]) {
        groups[cat] = [];
      }
      groups[cat].push(doc);
    });
    return groups;
  }, [filteredDocuments]);

  const formatFileSize = (bytes: number): string => {
    if (bytes < 1024) return `${bytes} B`;
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
    return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
  };

  const handleDownload = async (doc: DocumentItem) => {
    try {
      const res = await documentService.getDownloadUrl(doc.id);
      if (res && res.downloadUrl) {
        window.open(res.downloadUrl, '_blank', 'noopener,noreferrer');
      }
    } catch (err: unknown) {
      console.error('Download failed:', err);
      alert('Unable to generate download URL. Please try again.');
    }
  };

  const handleOpenUploadForCategory = (cat: string) => {
    setUploadSubject(cat === 'ALL' ? 'Java' : cat);
    setIsUploadOpen(true);
  };

  return (
    <div className="p-6 sm:p-8 space-y-6 max-w-7xl mx-auto text-[#F5F5F5]">
      {/* Top Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-4 border-b border-white/10">
        <div>
          <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-[#FF2D8D]/10 border border-[#FF2D8D]/30 text-[#FF2D8D] text-xs font-semibold mb-2">
            <Sparkles className="w-3.5 h-3.5" />
            <span>Workspace: {currentWorkspace?.name || 'Personal Workspace'}</span>
          </div>
          <h1 className="text-2xl sm:text-3xl font-serif text-white tracking-tight font-normal">
            Academic Materials & Subject Categories
          </h1>
          <p className="text-xs sm:text-sm text-[#A1A1AA] mt-1 font-light">
            Organized subject collections with automated parsing, vector embeddings, and page verification.
          </p>
        </div>

        <div className="flex items-center gap-3">
          <button
            type="button"
            onClick={() => fetchDocuments(false)}
            disabled={isLoading}
            className="p-2.5 rounded-xl border border-white/10 bg-[#171719] text-[#A1A1AA] hover:text-white hover:bg-[#202025] transition"
            title="Refresh materials list"
          >
            <RefreshCw className={`w-4 h-4 ${isLoading ? 'animate-spin' : ''}`} />
          </button>
          <button
            type="button"
            onClick={() => handleOpenUploadForCategory(selectedCategory)}
            className="btn-chartreuse rounded-full px-5 py-2.5 inline-flex items-center gap-2 text-xs font-bold whitespace-nowrap shadow-md transition shrink-0"
          >
            <Upload className="w-4 h-4 shrink-0 text-black" />
            <span>Upload Material</span>
          </button>
        </div>
      </div>

      {/* Subject Category Cards Grid */}
      <div className="space-y-3">
        <div className="flex items-center justify-between text-xs font-semibold uppercase tracking-wider text-[#A1A1AA]">
          <span className="flex items-center gap-2">
            <Layers className="w-4 h-4 text-[#FF2D8D]" />
            Subject Collections ({uniqueCategories.length})
          </span>
          {selectedCategory !== 'ALL' && (
            <button
              type="button"
              onClick={() => setSelectedCategory('ALL')}
              className="text-[#FF2D8D] hover:underline normal-case text-xs font-medium"
            >
              Clear Filter (Show All)
            </button>
          )}
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
          {/* All Materials Card */}
          <div
            onClick={() => setSelectedCategory('ALL')}
            className={`p-4 rounded-2xl border transition cursor-pointer flex flex-col justify-between ${
              selectedCategory === 'ALL'
                ? 'border-[#FF2D8D] bg-[#202025] ring-1 ring-[#FF2D8D]/40'
                : 'border-white/10 bg-[#171719] hover:border-white/20 hover:bg-[#202025]/60'
            }`}
          >
            <div className="flex items-start justify-between">
              <span className="text-2xl p-2 rounded-xl bg-white/5 border border-white/10">📚</span>
              <span className="text-xs font-mono font-bold px-2 py-0.5 rounded-full bg-white/10 text-white">
                {documents.length} Docs
              </span>
            </div>
            <div className="mt-3">
              <h3 className="text-sm font-bold text-white">All Subjects</h3>
              <p className="text-[11px] text-[#A1A1AA] mt-0.5">
                Total workspace repository
              </p>
            </div>
            <div className="mt-3 pt-3 border-t border-white/5 flex items-center justify-between text-[11px] text-zinc-400">
              <span>{categoryStats['General'] ? Object.keys(categoryStats).length : uniqueCategories.length} Categories</span>
              <span className="text-[#C7F000] font-medium font-mono">
                {documents.reduce((acc, d) => acc + (d.pageCount || 0), 0)} pgs
              </span>
            </div>
          </div>

          {/* Individual Subject Cards */}
          {uniqueCategories.map((cat) => {
            const data = categoryStats[cat];
            const isSelected = selectedCategory.toLowerCase() === cat.toLowerCase();
            const icon = getSubjectIcon(cat);
            return (
              <div
                key={cat}
                onClick={() => setSelectedCategory(isSelected ? 'ALL' : cat)}
                className={`p-4 rounded-2xl border transition cursor-pointer flex flex-col justify-between group ${
                  isSelected
                    ? 'border-[#C7F000] bg-[#202025] ring-1 ring-[#C7F000]/40 shadow-lg'
                    : 'border-white/10 bg-[#171719] hover:border-white/20 hover:bg-[#202025]/60'
                }`}
              >
                <div className="flex items-start justify-between">
                  <span className="text-2xl p-2 rounded-xl bg-white/5 border border-white/10">
                    {icon}
                  </span>
                  <span className="text-xs font-mono font-bold px-2.5 py-0.5 rounded-full bg-[#FF2D8D]/15 text-[#FF2D8D] border border-[#FF2D8D]/30">
                    {data.count} {data.count === 1 ? 'Material' : 'Materials'}
                  </span>
                </div>
                <div className="mt-3">
                  <h3 className="text-sm font-bold text-white group-hover:text-[#C7F000] transition truncate">
                    {cat}
                  </h3>
                  <p className="text-[11px] text-[#A1A1AA] mt-0.5">
                    {data.readyCount} / {data.count} Ready for AI Search
                  </p>
                </div>
                <div className="mt-3 pt-3 border-t border-white/5 flex items-center justify-between text-[11px] text-zinc-400">
                  <span className="font-mono text-[#C7F000]">{data.pages} Total Pages</span>
                  <button
                    type="button"
                    onClick={(e) => {
                      e.stopPropagation();
                      handleOpenUploadForCategory(cat);
                    }}
                    className="text-[#FF2D8D] hover:underline font-medium text-[11px] flex items-center gap-1"
                  >
                    + Add
                  </button>
                </div>
              </div>
            );
          })}
        </div>
      </div>

      {/* Filter and Search Bar */}
      <div className="bg-[#171719] p-4 rounded-2xl border border-white/10 shadow-lg flex flex-col md:flex-row items-stretch md:items-center justify-between gap-3">
        {/* Search Input */}
        <div className="relative flex-1">
          <Search className="w-4 h-4 text-[#A1A1AA] absolute left-3.5 top-3" />
          <input
            type="text"
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            placeholder="Search documents by title or filename..."
            className="w-full pl-10 pr-4 py-2 rounded-xl border border-white/10 bg-[#202025] text-white placeholder-[#A1A1AA] text-xs focus:outline-none focus:border-[#FF2D8D] transition"
          />
        </div>

        {/* Filters */}
        <div className="flex items-center gap-2 sm:gap-3 flex-wrap">
          {/* Subject Filter Dropdown */}
          <div className="flex items-center gap-1.5 text-xs text-[#A1A1AA]">
            <Tag className="w-3.5 h-3.5 text-[#FF2D8D]" />
            <select
              value={selectedCategory}
              onChange={(e) => setSelectedCategory(e.target.value)}
              className="py-2 px-3 rounded-xl border border-white/10 bg-[#202025] text-xs font-medium text-white focus:outline-none focus:border-[#FF2D8D] cursor-pointer"
            >
              <option value="ALL">All Categories</option>
              {uniqueCategories.map((cat) => (
                <option key={cat} value={cat}>
                  {cat} ({categoryStats[cat]?.count || 0})
                </option>
              ))}
            </select>
          </div>

          {/* File Type Filter */}
          <div className="flex items-center gap-1.5 text-xs text-[#A1A1AA]">
            <Filter className="w-3.5 h-3.5 text-[#C7F000]" />
            <select
              value={typeFilter}
              onChange={(e) => setTypeFilter(e.target.value)}
              className="py-2 px-3 rounded-xl border border-white/10 bg-[#202025] text-xs font-medium text-white focus:outline-none focus:border-[#FF2D8D] cursor-pointer"
            >
              <option value="ALL">All Formats</option>
              <option value="PDF">PDF</option>
              <option value="DOCX">DOCX</option>
              <option value="PPTX">PPTX</option>
            </select>
          </div>

          {/* Processing Status Filter */}
          <div className="flex items-center gap-1.5 text-xs text-[#A1A1AA]">
            <select
              value={statusFilter}
              onChange={(e) => setStatusFilter(e.target.value)}
              className="py-2 px-3 rounded-xl border border-white/10 bg-[#202025] text-xs font-medium text-white focus:outline-none focus:border-[#FF2D8D] cursor-pointer"
            >
              <option value="ALL">All States</option>
              <option value="READY">Ready for AI Search</option>
              <option value="EXTRACTING">Extracting content</option>
              <option value="WAITING_EMBEDDINGS">Waiting embeddings</option>
              <option value="EMBEDDING">Embedding active</option>
              <option value="FAILED">Failed</option>
            </select>
          </div>
        </div>
      </div>

      {/* Active Polling Status Notice */}
      {hasInProgressDocuments && (
        <div className="flex items-center gap-2.5 p-3.5 rounded-2xl bg-[#FF2D8D]/10 border border-[#FF2D8D]/30 text-xs text-[#FF2D8D]">
          <Loader2 className="w-4 h-4 text-[#FF2D8D] animate-spin shrink-0" />
          <span>
            Asynchronous ingestion pipeline active: Polling document extraction and vector embedding status...
          </span>
        </div>
      )}

      {/* Error Message */}
      {error && (
        <div className="p-4 rounded-2xl bg-rose-500/10 border border-rose-500/30 text-rose-300 text-xs flex items-center gap-2">
          <AlertCircle className="w-4 h-4 shrink-0" />
          <span>{error}</span>
        </div>
      )}

      {/* Grouped Category Materials Cards */}
      {isLoading && documents.length === 0 ? (
        <div className="bg-[#171719] rounded-2xl border border-white/10 p-20 flex flex-col items-center justify-center text-[#A1A1AA]">
          <Loader2 className="w-8 h-8 animate-spin text-[#FF2D8D] mb-3" />
          <p className="text-xs font-medium">Loading workspace documents...</p>
        </div>
      ) : filteredDocuments.length === 0 ? (
        <div className="bg-[#171719] rounded-2xl border border-white/10 py-16 px-6 text-center">
          <div className="w-14 h-14 rounded-2xl bg-white/5 border border-white/10 text-[#FF2D8D] flex items-center justify-center mx-auto mb-3 shadow-sm">
            <FolderOpen className="w-7 h-7" />
          </div>
          <h3 className="text-base font-semibold text-white">No academic materials found</h3>
          <p className="text-xs text-[#A1A1AA] mt-1 max-w-sm mx-auto">
            {searchQuery || typeFilter !== 'ALL' || statusFilter !== 'ALL' || selectedCategory !== 'ALL'
              ? 'No documents match your active filters. Try clearing search criteria or selecting All Categories.'
              : 'This workspace has no documents uploaded yet. Upload your lecture slides or syllabus to start learning.'}
          </p>
          <button
            type="button"
            onClick={() => handleOpenUploadForCategory(selectedCategory)}
            className="btn-chartreuse mt-4 inline-flex items-center gap-2 rounded-full px-5 py-2.5 text-xs font-bold whitespace-nowrap shadow-md transition"
          >
            <Upload className="w-4 h-4 text-black" />
            <span>Upload Academic Material</span>
          </button>
        </div>
      ) : (
        <div className="space-y-6">
          {Object.entries(filteredGroups).map(([subjectName, docs]) => {
            const icon = getSubjectIcon(subjectName);
            const totalSubjectPages = docs.reduce((acc, d) => acc + (d.pageCount || 0), 0);

            return (
              <div
                key={subjectName}
                className="bg-[#171719] rounded-2xl border border-white/10 shadow-xl overflow-hidden"
              >
                {/* Category Card Header */}
                <div className="px-6 py-4 bg-[#202025]/80 border-b border-white/10 flex flex-col sm:flex-row sm:items-center justify-between gap-3">
                  <div className="flex items-center gap-3">
                    <span className="text-2xl p-2 rounded-xl bg-white/5 border border-white/10 shrink-0">
                      {icon}
                    </span>
                    <div>
                      <div className="flex items-center gap-2">
                        <h2 className="text-base font-bold text-white tracking-tight">
                          {subjectName}
                        </h2>
                        <span className="px-2 py-0.5 rounded-full bg-[#FF2D8D]/20 text-[#FF2D8D] border border-[#FF2D8D]/30 text-[11px] font-semibold font-mono">
                          {docs.length} {docs.length === 1 ? 'file' : 'files'}
                        </span>
                      </div>
                      <p className="text-[11px] text-[#A1A1AA] mt-0.5">
                        {totalSubjectPages} Total pages indexed & ready for grounded search
                      </p>
                    </div>
                  </div>

                  <button
                    type="button"
                    onClick={() => handleOpenUploadForCategory(subjectName)}
                    className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-xl border border-white/10 bg-[#171719] text-xs font-medium text-white hover:border-[#C7F000] hover:text-[#C7F000] transition self-start sm:self-auto"
                  >
                    <PlusCircle className="w-3.5 h-3.5" />
                    <span>Add to {subjectName}</span>
                  </button>
                </div>

                {/* Material Rows Table */}
                <div className="overflow-x-auto">
                  <table className="w-full text-left text-xs border-collapse">
                    <thead>
                      <tr className="border-b border-white/5 bg-[#171719] text-[#A1A1AA] font-semibold uppercase tracking-wider">
                        <th className="py-3 px-6">Document Title</th>
                        <th className="py-3 px-4">Format</th>
                        <th className="py-3 px-4">Size</th>
                        <th className="py-3 px-4">Pages / Slides</th>
                        <th className="py-3 px-4">Uploaded</th>
                        <th className="py-3 px-4">AI Search Status</th>
                        <th className="py-3 px-6 text-right">Actions</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-white/5 text-[#F5F5F5]">
                      {docs.map((doc) => {
                        const displayName = doc.originalFilename || doc.originalFileName || doc.title;
                        return (
                          <tr
                            key={doc.id}
                            className="hover:bg-white/[0.02] transition group"
                          >
                            {/* Title */}
                            <td className="py-4 px-6 font-medium text-white">
                              <div className="flex items-center gap-3">
                                <div className="p-2 rounded-xl bg-white/5 text-[#FF2D8D] border border-white/10 shrink-0">
                                  <FileText className="w-4 h-4" />
                                </div>
                                <div className="min-w-0 max-w-xs sm:max-w-md truncate">
                                  <p className="font-semibold text-white truncate">
                                    {displayName}
                                  </p>
                                  <span className="text-[11px] text-[#A1A1AA] font-mono">
                                    ID: {doc.id.slice(0, 8)}...
                                  </span>
                                </div>
                              </div>
                            </td>

                            {/* Format */}
                            <td className="py-4 px-4 font-mono font-semibold uppercase text-zinc-300">
                              <span className="px-2 py-0.5 rounded-md bg-white/5 border border-white/10 text-[11px]">
                                {doc.fileType}
                              </span>
                            </td>

                            {/* Size */}
                            <td className="py-4 px-4 text-[#A1A1AA] font-medium">
                              {formatFileSize(doc.fileSize || 0)}
                            </td>

                            {/* Pages */}
                            <td className="py-4 px-4 text-[#A1A1AA] font-medium">
                              {doc.pageCount && doc.pageCount > 0 ? (
                                <span>{doc.pageCount} pgs</span>
                              ) : (
                                <span className="text-zinc-500 italic">Processing</span>
                              )}
                            </td>

                            {/* Uploaded */}
                            <td className="py-4 px-4 text-[#A1A1AA]">
                              {new Date(doc.createdAt).toLocaleDateString(undefined, {
                                month: 'short',
                                day: 'numeric',
                                year: 'numeric',
                              })}
                            </td>

                            {/* Status Badge */}
                            <td className="py-4 px-4">
                              <DocumentStatusBadge
                                processingStatus={doc.processingStatus}
                                embeddingStatus={doc.embeddingStatus}
                                size="sm"
                              />
                            </td>

                            {/* Actions */}
                            <td className="py-4 px-6 text-right">
                              <div className="flex items-center justify-end gap-1.5">
                                <button
                                  type="button"
                                  onClick={() => setSelectedDetailsDoc(doc)}
                                  className="p-1.5 text-[#A1A1AA] hover:text-[#FF2D8D] hover:bg-white/5 rounded-lg transition"
                                  title="Inspect Details"
                                >
                                  <Eye className="w-4 h-4" />
                                </button>
                                <button
                                  type="button"
                                  onClick={() => handleDownload(doc)}
                                  className="p-1.5 text-[#A1A1AA] hover:text-[#C7F000] hover:bg-white/5 rounded-lg transition"
                                  title="Download Original"
                                >
                                  <Download className="w-4 h-4" />
                                </button>
                                <button
                                  type="button"
                                  onClick={() => setSelectedDeleteDoc(doc)}
                                  className="p-1.5 text-[#A1A1AA] hover:text-rose-400 hover:bg-white/5 rounded-lg transition"
                                  title="Delete Document"
                                >
                                  <Trash2 className="w-4 h-4" />
                                </button>
                              </div>
                            </td>
                          </tr>
                        );
                      })}
                    </tbody>
                  </table>
                </div>
              </div>
            );
          })}
        </div>
      )}

      {/* Upload Dialog */}
      {currentWorkspace && (
        <DocumentUploadDialog
          isOpen={isUploadOpen}
          defaultSubject={uploadSubject}
          onClose={() => setIsUploadOpen(false)}
          workspaceId={currentWorkspace.id}
          onSuccess={(uploadedWsId) => {
            if (!uploadedWsId || uploadedWsId === currentWorkspace?.id) {
              fetchDocuments(false);
            }
          }}
        />
      )}

      {/* Details Modal */}
      <DocumentDetailsModal
        document={selectedDetailsDoc}
        isOpen={!!selectedDetailsDoc}
        onClose={() => setSelectedDetailsDoc(null)}
      />

      {/* Delete Confirmation Dialog */}
      <DocumentDeleteDialog
        document={selectedDeleteDoc}
        isOpen={!!selectedDeleteDoc}
        onClose={() => setSelectedDeleteDoc(null)}
        onSuccess={() => fetchDocuments(false)}
      />
    </div>
  );
};