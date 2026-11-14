export interface SearchResultItem {
  chunkId: string;
  documentId: string;
  documentName: string;
  documentTitle?: string;
  workspaceId: string;
  chunkContent: string;
  content?: string;
  pageNumber?: number;
  slideNumber?: number;
  sectionHeading?: string;
  chunkIndex: number;
  sourceType: string;
  finalRank: number;
  rerankerScore: number;
  score?: number;
  rrfScore: number;
  denseRank?: number;
  keywordRank?: number;
  denseSimilarity?: number;
  keywordScore?: number;
  segmentIndex?: number;
}

export interface SearchResponse {
  query: string;
  workspaceId: string;
  totalResults: number;
  results: SearchResultItem[];
}
