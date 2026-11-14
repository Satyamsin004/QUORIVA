import React, { useState, useEffect, useRef } from 'react';
import { useWorkspace } from '@/context/WorkspaceContext';
import { searchService } from '@/services/searchService';
import { SearchResultItem } from '@/types/search';
import { SearchSourceDrawer } from '@/components/search/SearchSourceDrawer';
import {
  Search,
  BookOpen,
  FileText,
  Bot,
  Loader2,
  ArrowRight,
  CheckCircle,
  HelpCircle,
} from 'lucide-react';

interface SearchPageProps {
  onNavigateToTutor: (prefillPrompt: string) => void;
}

const EXAMPLE_QUERIES = [
  'Dijkstra shortest path algorithm time complexity',
  'B-Tree vs LSM-Tree indexing in databases',
  'ACID transaction isolation levels and two-phase locking',
  'TCP congestion control: AIMD and Slow Start',
];

export const SearchPage: React.FC<SearchPageProps> = ({ onNavigateToTutor }) => {
  const { currentWorkspace } = useWorkspace();

  const [query, setQuery] = useState('');
  const [results, setResults] = useState<SearchResultItem[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [hasSearched, setHasSearched] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [selectedResult, setSelectedResult] = useState<SearchResultItem | null>(null);

  const activeWorkspaceIdRef = useRef<string | null>(null);

  useEffect(() => {
    const targetId = currentWorkspace?.id || null;
    activeWorkspaceIdRef.current = targetId;
    setResults([]);
    setQuery('');
    setHasSearched(false);
    setSelectedResult(null);
    setErrorMessage(null);
  }, [currentWorkspace?.id]);

  const handleSearch = async (searchQuery?: string) => {
    const targetWorkspaceId = currentWorkspace?.id;
    const q = searchQuery !== undefined ? searchQuery : query;
    if (!q.trim() || !targetWorkspaceId) return;

    setIsLoading(true);
    setErrorMessage(null);
    setHasSearched(true);
    if (searchQuery !== undefined) {
      setQuery(searchQuery);
    }

    try {
      const response = await searchService.search(q.trim(), targetWorkspaceId, 10);

      if (activeWorkspaceIdRef.current === targetWorkspaceId) {
        const rawResults = response.results || [];
        const sigmoid = (z: number) => 1 / (1 + Math.exp(-z));
        const filtered = rawResults.filter((item: SearchResultItem) => {
          const rawScore = item.rerankerScore ?? item.score;
          if (rawScore === undefined || rawScore === null) return false;
          const normalized = rawScore >= 0 && rawScore <= 1 ? rawScore : sigmoid(rawScore);
          return normalized >= 0.25;
        });
        setResults(filtered);
      }
    } catch (err: unknown) {
      if (activeWorkspaceIdRef.current === targetWorkspaceId) {
        console.error('Academic search error:', err);
        setErrorMessage('Failed to execute academic search. Please verify service health.');
        setResults([]);
      }
    } finally {
      if (activeWorkspaceIdRef.current === targetWorkspaceId) {
        setIsLoading(false);
      }
    }
  };

  const handleKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key === 'Enter') {
      handleSearch();
    }
  };

  const handleAskTutor = (item: SearchResultItem) => {
    const docTitle = item.documentName || item.documentTitle || 'Academic Document';
    const prompt = item.sectionHeading
      ? `Explain the concepts of "${item.sectionHeading}" from "${docTitle}" in detail.`
      : `Explain the key topics covered in "${docTitle}".`;
    onNavigateToTutor(prompt);
  };

  return (
    <div className="p-6 sm:p-8 space-y-6 max-w-5xl mx-auto text-[#F5F5F5]">
      {/* Search Header */}
      <div className="pb-4 border-b border-white/10">
        <h1 className="text-2xl sm:text-3xl font-serif text-white tracking-tight font-normal">
          Academic Knowledge Search
        </h1>
        <p className="text-xs sm:text-sm text-[#A1A1AA] mt-1 font-light">
          Search across all lecture slides, textbooks, and notes in{' '}
          <span className="font-semibold text-white">
            {currentWorkspace?.name || 'Personal Workspace'}
          </span>
          .
        </p>
      </div>

      {/* Search Input Box */}
      <div className="bg-[#171719] p-3 sm:p-4 rounded-3xl border border-white/10 shadow-xl space-y-3">
        <div className="flex items-center gap-2">
          <div className="relative flex-1">
            <Search className="w-5 h-5 text-[#A1A1AA] absolute left-3.5 top-3.5" />
            <input
              type="text"
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              onKeyDown={handleKeyDown}
              placeholder="Search concepts, algorithms, theorems, or slide topics..."
              className="w-full pl-11 pr-4 py-3 rounded-2xl border border-white/10 bg-[#202025] text-sm text-white placeholder-[#A1A1AA] focus:outline-none focus:border-[#FF2D8D] transition"
            />
          </div>
          <button
            type="button"
            onClick={() => handleSearch()}
            disabled={isLoading || !query.trim()}
            className="btn-chartreuse inline-flex items-center gap-2 h-12 px-6"
          >
            {isLoading ? (
              <Loader2 className="w-4 h-4 animate-spin text-black" />
            ) : (
              <Search className="w-4 h-4 text-black" />
            )}
            <span>Search</span>
          </button>
        </div>

        {/* Quick Suggestion Chips */}
        <div className="flex items-center gap-2 flex-wrap pt-1 text-xs">
          <span className="text-[#A1A1AA] font-medium">Try asking:</span>
          {EXAMPLE_QUERIES.map((example, idx) => (
            <button
              key={idx}
              type="button"
              onClick={() => handleSearch(example)}
              className="px-3 py-1 rounded-full bg-[#202025] hover:bg-[#FF2D8D]/15 text-[#A1A1AA] hover:text-[#FF2D8D] border border-white/10 hover:border-[#FF2D8D]/30 transition font-medium text-[11px]"
            >
              {example}
            </button>
          ))}
        </div>
      </div>

      {/* Error Message */}
      {errorMessage && (
        <div className="p-4 rounded-2xl bg-rose-500/10 border border-rose-500/20 text-rose-300 text-xs">
          {errorMessage}
        </div>
      )}

      {/* Results Header */}
      {hasSearched && !isLoading && (
        <div className="flex items-center justify-between text-xs text-[#A1A1AA] px-1">
          <span>
            Found <span className="font-semibold text-white">{results.length}</span> verified academic passages
          </span>
          <span className="text-[11px] text-[#A1A1AA]">Ranked by contextual relevance</span>
        </div>
      )}

      {/* Results List */}
      <div className="space-y-4">
        {isLoading ? (
          <div className="py-20 flex flex-col items-center justify-center text-[#A1A1AA]">
            <Loader2 className="w-8 h-8 animate-spin text-[#FF2D8D] mb-3" />
            <p className="text-xs font-medium">Searching workspace documents...</p>
          </div>
        ) : !hasSearched ? (
          <div className="py-16 text-center bg-[#171719] rounded-3xl border border-white/10 p-8 shadow-xl">
            <div className="w-14 h-14 rounded-2xl bg-white/5 border border-white/10 text-[#C7F000] flex items-center justify-center mx-auto mb-3">
              <BookOpen className="w-7 h-7" />
            </div>
            <h3 className="text-lg font-serif text-white font-normal">
              Grounded Academic Retrieval
            </h3>
            <p className="text-xs text-[#A1A1AA] mt-1.5 max-w-md mx-auto leading-relaxed">
              Every search combines semantic meaning with keyword matching to return the most relevant textbook passages and lecture notes.
            </p>
          </div>
        ) : results.length === 0 ? (
          <div className="py-16 text-center bg-[#171719] rounded-3xl border border-white/10 p-8 shadow-xl">
            <div className="w-14 h-14 rounded-2xl bg-amber-500/10 border border-amber-500/20 text-amber-400 flex items-center justify-center mx-auto mb-3">
              <HelpCircle className="w-7 h-7" />
            </div>
            <h3 className="text-lg font-serif text-white font-normal">No matching passages found</h3>
            <p className="text-xs text-[#A1A1AA] mt-1.5 max-w-sm mx-auto">
              No relevant contextual matches found in your uploaded materials. Only passages with verified relevance to your query are displayed.
            </p>
          </div>
        ) : (
          results.map((item, idx) => {
            const sigmoid = (z: number) => 1 / (1 + Math.exp(-z));
            const rawScore = item.rerankerScore ?? item.score;
            const normalized = rawScore !== undefined && rawScore !== null
              ? (rawScore >= 0 && rawScore <= 1 ? rawScore : sigmoid(rawScore))
              : 0;
            const scorePercent = Math.min(100, Math.max(1, Math.round(normalized * 100)));
            const docTitle = item.documentName || item.documentTitle || 'Academic Document';
            const textContent = item.chunkContent || item.content || '';
            const pageVal = item.pageNumber || item.slideNumber;

            return (
              <div
                key={item.chunkId || idx}
                className="bg-[#171719] p-6 rounded-3xl border border-white/10 hover:border-[#FF2D8D]/40 transition shadow-xl space-y-4 group"
              >
                {/* Result Item Header */}
                <div className="flex items-start justify-between gap-3">
                  <div className="flex items-center gap-3 min-w-0">
                    <div className="p-2.5 rounded-xl bg-white/5 border border-white/10 text-[#FF2D8D] shrink-0">
                      <FileText className="w-4 h-4" />
                    </div>
                    <div className="min-w-0">
                      <h4 className="text-sm font-semibold text-white truncate">
                        {docTitle}
                      </h4>
                      <div className="flex items-center gap-2 text-xs text-[#A1A1AA] mt-0.5">
                        {pageVal && (
                          <span className="font-medium text-zinc-300">
                            Page / Slide {pageVal}
                          </span>
                        )}
                        {item.sectionHeading && (
                          <>
                            <span>•</span>
                            <span className="truncate max-w-xs">{item.sectionHeading}</span>
                          </>
                        )}
                      </div>
                    </div>
                  </div>

                  {/* Reranker Score Badge */}
                  <div className="shrink-0 text-right">
                    <span className="inline-flex items-center gap-1 px-3 py-1 rounded-full text-xs font-semibold bg-[#C7F000]/10 text-[#C7F000] border border-[#C7F000]/30">
                      <CheckCircle className="w-3.5 h-3.5" />
                      {scorePercent}% Relevance
                    </span>
                  </div>
                </div>

                {/* Excerpt Snippet */}
                <div className="p-4 rounded-2xl bg-[#09090B] border border-white/5 text-xs text-zinc-300 leading-relaxed font-serif line-clamp-4">
                  {textContent}
                </div>

                {/* Actions */}
                <div className="flex items-center justify-between pt-2 border-t border-white/10 text-xs">
                  <button
                    type="button"
                    onClick={() => setSelectedResult(item)}
                    className="text-[#FF2D8D] hover:text-[#FF479D] font-semibold inline-flex items-center gap-1 transition"
                  >
                    <span>Inspect Full Passage</span>
                    <ArrowRight className="w-3.5 h-3.5" />
                  </button>

                  <button
                    type="button"
                    onClick={() => handleAskTutor(item)}
                    className="inline-flex items-center gap-1.5 px-3.5 py-1.5 rounded-full bg-[#202025] hover:bg-[#2A2A32] text-white border border-white/10 hover:border-[#FF2D8D]/40 transition text-xs font-medium"
                  >
                    <Bot className="w-3.5 h-3.5 text-[#FF2D8D]" />
                    <span>Ask AI Tutor about this topic</span>
                  </button>
                </div>
              </div>
            );
          })
        )}
      </div>

      {/* Inspect Source Drawer */}
      <SearchSourceDrawer
        isOpen={!!selectedResult}
        onClose={() => setSelectedResult(null)}
        result={selectedResult}
        onAskTutor={(prefill) => {
          onNavigateToTutor(prefill);
        }}
      />
    </div>
  );
};
