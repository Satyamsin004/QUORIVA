import React, { useState, useEffect, useCallback } from 'react';
import { useAuth } from '@/context/AuthContext';
import { libraryService, SearchLibraryParams } from '@/services/libraryService';
import {
  LibraryResourceSummary,
  OptionItem,
  AcademicCategory,
  LibraryResourceType,
  AcademicLevel,
} from '@/types';
import {
  BookOpen,
  Search,
  Download,
  Plus,
  User,
  Loader2,
  FileText,
  FolderClock,
  ShieldAlert,
} from 'lucide-react';

interface LibraryDiscoveryPageProps {
  onSelectResource: (id: string) => void;
  onNavigate: (route: string) => void;
}

export const LibraryDiscoveryPage: React.FC<LibraryDiscoveryPageProps> = ({
  onSelectResource,
  onNavigate,
}) => {
  const { user } = useAuth();
  const [resources, setResources] = useState<LibraryResourceSummary[]>([]);
  const [categories, setCategories] = useState<OptionItem[]>([]);
  const [resourceTypes, setResourceTypes] = useState<OptionItem[]>([]);
  const [academicLevels, setAcademicLevels] = useState<OptionItem[]>([]);
  const [popularTags, setPopularTags] = useState<string[]>([]);

  // Search & filter states
  const [query, setQuery] = useState('');
  const [selectedCategory, setSelectedCategory] = useState<AcademicCategory | ''>('');
  const [selectedType, setSelectedType] = useState<LibraryResourceType | ''>('');
  const [selectedLevel, setSelectedLevel] = useState<AcademicLevel | ''>('');
  const [selectedTag, setSelectedTag] = useState<string>('');
  const [sortBy, setSortBy] = useState<'RECENT' | 'POPULAR' | 'TITLE'>('RECENT');
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [totalElements, setTotalElements] = useState(0);

  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const isModerator = user?.role === 'EDUCATOR' || user?.role === 'ADMIN';

  // Load taxonomies on mount
  useEffect(() => {
    const loadMetadata = async () => {
      try {
        const [cats, types, levels, tags] = await Promise.all([
          libraryService.getCategories(),
          libraryService.getResourceTypes(),
          libraryService.getAcademicLevels(),
          libraryService.getPopularTags(12),
        ]);
        setCategories(cats);
        setResourceTypes(types);
        setAcademicLevels(levels);
        setPopularTags(tags);
      } catch (err) {
        console.error('Failed to load library taxonomies', err);
      }
    };
    loadMetadata();
  }, []);

  const fetchResources = useCallback(async () => {
    try {
      setIsLoading(true);
      setError(null);
      const params: SearchLibraryParams = {
        q: query.trim() || undefined,
        category: selectedCategory ? selectedCategory : undefined,
        resourceType: selectedType ? selectedType : undefined,
        academicLevel: selectedLevel ? selectedLevel : undefined,
        tag: selectedTag ? selectedTag : undefined,
        sort: sortBy,
        page,
        size: 12,
      };
      const res = await libraryService.searchResources(params);
      setResources(res.content || []);
      setTotalPages(res.totalPages || res.page?.totalPages || 1);
      setTotalElements(res.totalElements ?? res.page?.totalElements ?? 0);
    } catch (err: any) {
      setError(err?.response?.data?.message || 'Failed to load library resources.');
    } finally {
      setIsLoading(false);
    }
  }, [query, selectedCategory, selectedType, selectedLevel, selectedTag, sortBy, page]);

  useEffect(() => {
    fetchResources();
  }, [fetchResources]);

  const handleSearchSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setPage(0);
    fetchResources();
  };

  const handleResetFilters = () => {
    setQuery('');
    setSelectedCategory('');
    setSelectedType('');
    setSelectedLevel('');
    setSelectedTag('');
    setSortBy('RECENT');
    setPage(0);
  };



  return (
    <div className="max-w-7xl mx-auto py-8 px-4 sm:px-6 space-y-6">
      {/* Top Banner & Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-4 border-b border-slate-800">
        <div>
          <div className="flex items-center gap-2 text-indigo-400 text-xs font-semibold uppercase tracking-wider mb-1">
            <BookOpen className="w-4 h-4" />
            <span>Academic Commons</span>
          </div>
          <h1 className="text-2xl sm:text-3xl font-bold text-white tracking-tight">
            Shared Academic Library
          </h1>
          <p className="text-xs sm:text-sm text-slate-400 mt-1 max-w-xl">
            Discover peer-reviewed lecture notes, textbooks, and open courseware with explicit copyright and educational rights declarations.
          </p>
        </div>

        <div className="flex flex-wrap items-center gap-2.5">
          <button
            onClick={() => onNavigate('library-my')}
            className="px-3.5 py-2 text-xs font-medium text-slate-300 hover:text-white bg-slate-900 border border-slate-800 hover:border-slate-700 rounded-xl transition inline-flex items-center gap-1.5"
          >
            <FolderClock className="w-3.5 h-3.5 text-slate-400" />
            <span>My Submissions</span>
          </button>

          {isModerator && (
            <button
              onClick={() => onNavigate('library-moderation')}
              className="px-3.5 py-2 text-xs font-semibold text-amber-300 bg-amber-500/10 border border-amber-500/20 hover:bg-amber-500/20 rounded-xl transition inline-flex items-center gap-1.5"
            >
              <ShieldAlert className="w-3.5 h-3.5" />
              <span>Moderation Queue</span>
            </button>
          )}

          <button
            onClick={() => onNavigate('library-submit')}
            className="px-4 py-2 text-xs font-semibold text-white bg-indigo-600 hover:bg-indigo-500 rounded-xl shadow-lg shadow-indigo-600/20 transition inline-flex items-center gap-1.5"
          >
            <Plus className="w-4 h-4" />
            <span>Contribute Resource</span>
          </button>
        </div>
      </div>

      {/* Search & Filter Controls */}
      <div className="bg-slate-900/90 border border-slate-800 rounded-2xl p-4 sm:p-5 space-y-4">
        {/* Search Bar */}
        <form onSubmit={handleSearchSubmit} className="flex gap-2">
          <div className="relative flex-1">
            <Search className="w-4 h-4 text-slate-400 absolute left-3.5 top-1/2 -translate-y-1/2" />
            <input
              type="text"
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              placeholder="Search by title, subject, attribution, or keywords..."
              className="w-full bg-slate-950 border border-slate-800 rounded-xl pl-10 pr-4 py-2.5 text-xs sm:text-sm text-white placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-indigo-500/50"
            />
          </div>
          <button
            type="submit"
            className="px-5 py-2.5 bg-indigo-600 hover:bg-indigo-500 text-white text-xs font-semibold rounded-xl transition"
          >
            Search
          </button>
        </form>

        {/* Filter Dropdowns */}
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-3 text-xs">
          <div>
            <label className="block text-[11px] font-semibold uppercase text-slate-400 mb-1">
              Category
            </label>
            <select
              value={selectedCategory}
              onChange={(e) => {
                setSelectedCategory(e.target.value as AcademicCategory);
                setPage(0);
              }}
              className="w-full bg-slate-950 border border-slate-800 rounded-xl px-3 py-2 text-white focus:outline-none focus:ring-1 focus:ring-indigo-500"
            >
              <option value="">All Academic Categories</option>
              {categories.map((c) => (
                <option key={c.value} value={c.value}>
                  {c.label}
                </option>
              ))}
            </select>
          </div>

          <div>
            <label className="block text-[11px] font-semibold uppercase text-slate-400 mb-1">
              Resource Type
            </label>
            <select
              value={selectedType}
              onChange={(e) => {
                setSelectedType(e.target.value as LibraryResourceType);
                setPage(0);
              }}
              className="w-full bg-slate-950 border border-slate-800 rounded-xl px-3 py-2 text-white focus:outline-none focus:ring-1 focus:ring-indigo-500"
            >
              <option value="">All Resource Types</option>
              {resourceTypes.map((t) => (
                <option key={t.value} value={t.value}>
                  {t.label}
                </option>
              ))}
            </select>
          </div>

          <div>
            <label className="block text-[11px] font-semibold uppercase text-slate-400 mb-1">
              Academic Level
            </label>
            <select
              value={selectedLevel}
              onChange={(e) => {
                setSelectedLevel(e.target.value as AcademicLevel);
                setPage(0);
              }}
              className="w-full bg-slate-950 border border-slate-800 rounded-xl px-3 py-2 text-white focus:outline-none focus:ring-1 focus:ring-indigo-500"
            >
              <option value="">All Academic Levels</option>
              {academicLevels.map((l) => (
                <option key={l.value} value={l.value}>
                  {l.label}
                </option>
              ))}
            </select>
          </div>

          <div>
            <label className="block text-[11px] font-semibold uppercase text-slate-400 mb-1">
              Sort By
            </label>
            <select
              value={sortBy}
              onChange={(e) => {
                setSortBy(e.target.value as any);
                setPage(0);
              }}
              className="w-full bg-slate-950 border border-slate-800 rounded-xl px-3 py-2 text-white focus:outline-none focus:ring-1 focus:ring-indigo-500"
            >
              <option value="RECENT">Most Recent</option>
              <option value="POPULAR">Most Downloaded</option>
              <option value="TITLE">Title (A-Z)</option>
            </select>
          </div>
        </div>

        {/* Popular Tags Pills & Reset Filter */}
        <div className="flex flex-wrap items-center justify-between gap-2 pt-2 border-t border-slate-800/80 text-xs">
          <div className="flex flex-wrap items-center gap-1.5">
            <span className="text-slate-500 text-[11px] mr-1">Popular Tags:</span>
            {popularTags.map((tag) => (
              <button
                key={tag}
                onClick={() => {
                  setSelectedTag(selectedTag === tag ? '' : tag);
                  setPage(0);
                }}
                className={`px-2 py-0.5 rounded-md text-[11px] transition ${
                  selectedTag === tag
                    ? 'bg-indigo-600 text-white font-semibold'
                    : 'bg-slate-800 text-slate-400 hover:text-white hover:bg-slate-700'
                }`}
              >
                #{tag}
              </button>
            ))}
          </div>

          {(query || selectedCategory || selectedType || selectedLevel || selectedTag) && (
            <button
              onClick={handleResetFilters}
              className="text-xs text-indigo-400 hover:text-indigo-300 font-medium underline"
            >
              Clear all filters
            </button>
          )}
        </div>
      </div>

      {/* Results Header */}
      <div className="flex items-center justify-between text-xs text-slate-400 px-1">
        <span>
          Showing <strong className="text-white">{resources.length}</strong> of{' '}
          <strong className="text-white">{totalElements}</strong> published resources
        </span>
      </div>

      {/* Content Area */}
      {isLoading ? (
        <div className="flex flex-col items-center justify-center py-20 text-slate-400">
          <Loader2 className="w-8 h-8 animate-spin text-indigo-500 mb-3" />
          <p className="text-xs">Searching academic library...</p>
        </div>
      ) : error ? (
        <div className="p-6 bg-rose-500/10 border border-rose-500/20 rounded-2xl text-center text-rose-300 text-xs">
          {error}
        </div>
      ) : resources.length === 0 ? (
        <div className="py-16 px-4 bg-slate-900/40 border border-slate-800 rounded-2xl text-center space-y-3">
          <div className="w-12 h-12 rounded-2xl bg-slate-800 flex items-center justify-center mx-auto text-slate-400">
            <FileText className="w-6 h-6" />
          </div>
          <h3 className="text-base font-semibold text-white">No Resources Found</h3>
          <p className="text-xs text-slate-400 max-w-sm mx-auto">
            No published academic resources matched your search criteria. Try clearing filters or be the first to contribute!
          </p>
          <div className="flex items-center justify-center gap-3 pt-2">
            <button
              onClick={handleResetFilters}
              className="px-4 py-2 text-xs font-medium text-slate-300 bg-slate-800 hover:bg-slate-700 rounded-xl transition"
            >
              Reset Filters
            </button>
            <button
              onClick={() => onNavigate('library-submit')}
              className="px-4 py-2 text-xs font-semibold text-white bg-indigo-600 hover:bg-indigo-500 rounded-xl transition"
            >
              Contribute Resource
            </button>
          </div>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4 sm:gap-5">
          {resources.map((item) => (
            <div
              key={item.id}
              onClick={() => onSelectResource(item.id)}
              className="group bg-slate-900 border border-slate-800 hover:border-indigo-500/50 rounded-2xl p-5 flex flex-col justify-between transition shadow-md hover:shadow-indigo-500/10 cursor-pointer"
            >
              <div className="space-y-3">
                <div className="flex items-start justify-between gap-2">
                  <span className="px-2 py-0.5 rounded text-[11px] font-semibold bg-indigo-500/10 text-indigo-400 border border-indigo-500/20">
                    {item.categoryDisplayName || item.category}
                  </span>
                  <span className="text-[11px] text-slate-500 flex items-center gap-1">
                    <Download className="w-3 h-3" />
                    {item.downloadsCount}
                  </span>
                </div>

                <h3 className="text-base font-bold text-white group-hover:text-indigo-400 transition line-clamp-2">
                  {item.title}
                </h3>

                <p className="text-xs text-slate-400 line-clamp-3 leading-relaxed">
                  {item.description}
                </p>

                {/* Tags */}
                {item.tags && item.tags.length > 0 && (
                  <div className="flex flex-wrap gap-1 pt-1">
                    {item.tags.slice(0, 3).map((t) => (
                      <span
                        key={t}
                        className="px-1.5 py-0.5 text-[10px] rounded bg-slate-800 text-slate-400"
                      >
                        #{t}
                      </span>
                    ))}
                    {item.tags.length > 3 && (
                      <span className="text-[10px] text-slate-500">+{item.tags.length - 3}</span>
                    )}
                  </div>
                )}
              </div>

              {/* Card Footer */}
              <div className="pt-4 mt-4 border-t border-slate-800/80 flex items-center justify-between text-[11px] text-slate-400">
                <div className="flex items-center gap-1.5 truncate max-w-[170px]">
                  <User className="w-3.5 h-3.5 text-slate-500 shrink-0" />
                  <span className="truncate">{item.contributorName}</span>
                </div>

                <span className="px-2 py-0.5 rounded text-[10px] font-medium bg-emerald-500/10 text-emerald-400 border border-emerald-500/20">
                  {item.licenseName}
                </span>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Pagination Bar */}
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
  );
};
