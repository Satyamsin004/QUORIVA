import React, { useState, useEffect } from 'react';
import { Eye, Loader2, Maximize2, X } from 'lucide-react';
import { apiClient } from '@/services/apiClient';

interface Props {
  documentId?: string;
  pageNumber?: number;
  documentTitle?: string;
}

export const PdfPagePreview: React.FC<Props> = ({
  documentId,
  pageNumber = 1,
  documentTitle = 'Document',
}) => {
  const [imageUrl, setImageUrl] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [hasError, setHasError] = useState<boolean>(false);
  const [isZoomed, setIsZoomed] = useState<boolean>(false);

  useEffect(() => {
    if (!documentId) {
      setIsLoading(false);
      setHasError(true);
      return;
    }

    let isMounted = true;
    let objectUrlToRevoke: string | null = null;
    setIsLoading(true);
    setHasError(false);

    apiClient
      .get(`/documents/${documentId}/pages/${pageNumber}/preview`, {
        responseType: 'blob',
      })
      .then((res: { data: BlobPart }) => {
        if (!isMounted) return;
        const blob = new Blob([res.data], { type: 'image/png' });
        const url = URL.createObjectURL(blob);
        objectUrlToRevoke = url;
        setImageUrl(url);
        setIsLoading(false);
      })
      .catch((err: unknown) => {
        if (!isMounted) return;
        console.warn('Page preview failed to load:', err);
        setHasError(true);
        setIsLoading(false);
      });

    return () => {
      isMounted = false;
      if (objectUrlToRevoke) {
        URL.revokeObjectURL(objectUrlToRevoke);
      }
    };
  }, [documentId, pageNumber]);

  if (hasError && !isLoading) {
    return (
      <div className="rounded-xl overflow-hidden border border-white/5 bg-[#09090B]/60 my-2 px-3 py-2 flex items-center justify-between text-[11px] text-[#A1A1AA]">
        <span className="flex items-center gap-1.5 text-zinc-400">
          <Eye className="w-3.5 h-3.5 text-[#FF2D8D]" />
          Source Document Page {pageNumber}
        </span>
        <button
          type="button"
          onClick={() => {
            setHasError(false);
            setIsLoading(true);
            apiClient
              .get(`/documents/${documentId}/pages/${pageNumber}/preview`, {
                responseType: 'blob',
              })
              .then((res: { data: BlobPart }) => {
                const blob = new Blob([res.data], { type: 'image/png' });
                const url = URL.createObjectURL(blob);
                setImageUrl(url);
                setIsLoading(false);
              })
              .catch(() => {
                setHasError(true);
                setIsLoading(false);
              });
          }}
          className="text-[#C7F000] hover:underline"
        >
          Load Page Image
        </button>
      </div>
    );
  }

  return (
    <div className="rounded-xl overflow-hidden border border-white/10 bg-[#09090B] my-3">
      {/* Bar Header */}
      <div className="flex items-center justify-between px-3 py-1.5 bg-[#171719] border-b border-white/10 text-[11px] text-[#A1A1AA]">
        <span className="flex items-center gap-1.5 font-semibold text-white">
          <Eye className="w-3.5 h-3.5 text-[#C7F000]" />
          Verified Source Page {pageNumber}
        </span>
        {imageUrl && (
          <button
            type="button"
            onClick={() => setIsZoomed(true)}
            className="flex items-center gap-1 text-[#FF2D8D] hover:text-white transition text-[11px] font-medium"
          >
            <Maximize2 className="w-3 h-3" />
            Expand Page
          </button>
        )}
      </div>

      {/* Image Preview Box */}
      <div className="p-2 flex items-center justify-center bg-zinc-950/70 min-h-[140px] max-h-[360px] overflow-y-auto relative">
        {isLoading ? (
          <div className="flex flex-col items-center justify-center py-6 text-[#A1A1AA] gap-2">
            <Loader2 className="w-5 h-5 animate-spin text-[#C7F000]" />
            <span className="text-[11px]">Rendering original PDF page...</span>
          </div>
        ) : imageUrl ? (
          <img
            src={imageUrl}
            alt={`Page ${pageNumber} of ${documentTitle}`}
            onClick={() => setIsZoomed(true)}
            className="rounded shadow-md max-w-full h-auto object-contain cursor-zoom-in hover:opacity-95 transition"
          />
        ) : null}
      </div>

      {/* Modal Zoom View */}
      {isZoomed && imageUrl && (
        <div
          className="fixed inset-0 z-[100] bg-black/90 backdrop-blur-md flex flex-col items-center justify-center p-4"
          onClick={() => setIsZoomed(false)}
        >
          <div
            className="max-w-4xl w-full max-h-[90vh] bg-[#171719] border border-white/10 rounded-2xl flex flex-col overflow-hidden shadow-2xl"
            onClick={(e) => e.stopPropagation()}
          >
            <div className="flex items-center justify-between px-5 py-3 border-b border-white/10 bg-[#09090B]">
              <span className="text-xs font-semibold text-white">
                {documentTitle} — Page {pageNumber}
              </span>
              <button
                type="button"
                onClick={() => setIsZoomed(false)}
                className="p-1 rounded-lg hover:bg-white/10 text-white transition"
              >
                <X className="w-5 h-5" />
              </button>
            </div>
            <div className="flex-1 overflow-auto p-4 flex justify-center bg-zinc-950">
              <img
                src={imageUrl}
                alt={`Zoomed Page ${pageNumber}`}
                className="max-w-full max-h-[80vh] object-contain rounded"
              />
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
