import React, { useState } from 'react';
import { ConfirmDialog } from '@/components/common/ConfirmDialog';
import { DocumentItem } from '@/types/document';
import { documentService } from '@/services/documentService';

interface DocumentDeleteDialogProps {
  document: DocumentItem | null;
  isOpen: boolean;
  onClose: () => void;
  onSuccess: () => void;
}

export const DocumentDeleteDialog: React.FC<DocumentDeleteDialogProps> = ({
  document,
  isOpen,
  onClose,
  onSuccess,
}) => {
  const [isDeleting, setIsDeleting] = useState(false);

  if (!document) return null;

  const displayName = document.originalFilename || document.originalFileName || document.title;

  const handleConfirm = async () => {
    setIsDeleting(true);
    try {
      await documentService.deleteDocument(document.id);
      onSuccess();
      onClose();
    } catch (err: unknown) {
      console.error('Delete failed:', err);
      alert('Failed to delete document. Please try again.');
    } finally {
      setIsDeleting(false);
    }
  };

  return (
    <ConfirmDialog
      isOpen={isOpen}
      onClose={() => !isDeleting && onClose()}
      onConfirm={handleConfirm}
      title="Delete Academic Document"
      message={`Are you sure you want to delete "${displayName}"? This will permanently remove the document, its extracted text, and its vector embeddings from your workspace.`}
      confirmLabel="Delete Document"
      cancelLabel="Cancel"
      isDestructive={true}
      isLoading={isDeleting}
    />
  );
};
