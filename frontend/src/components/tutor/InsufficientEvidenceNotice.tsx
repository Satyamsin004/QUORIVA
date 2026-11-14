import React from 'react';
import { AlertCircle, FilePlus2 } from 'lucide-react';

interface Props {
  className?: string;
}

export const InsufficientEvidenceNotice: React.FC<Props> = ({ className = '' }) => {
  return (
    <div className={`rounded-lg border border-amber-500/30 bg-amber-500/10 p-4 text-amber-200 ${className}`}>
      <div className="flex items-start gap-3">
        <AlertCircle className="mt-0.5 h-5 w-5 flex-shrink-0 text-amber-400" />
        <div className="space-y-1 text-sm">
          <p className="font-semibold text-amber-300">Insufficient Evidence in Workspace Materials</p>
          <p className="text-amber-200/90 leading-relaxed">
            The tutor could not verify this answer against your uploaded documents. To receive grounded answers with page-level citations, please upload relevant lecture slides, notes, or textbook chapters to this workspace.
          </p>
          <div className="flex items-center gap-2 pt-1 text-xs text-amber-400">
            <FilePlus2 className="h-4 w-4" />
            <span>Upload documents in Document Management to expand the tutor's verified knowledge.</span>
          </div>
        </div>
      </div>
    </div>
  );
};

