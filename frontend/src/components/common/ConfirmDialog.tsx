import React from 'react';
import { Modal } from './Modal';
import { AlertTriangle, Loader2 } from 'lucide-react';

interface ConfirmDialogProps {
  isOpen: boolean;
  onClose: () => void;
  onConfirm: () => void;
  title: string;
  message: string;
  confirmLabel?: string;
  cancelLabel?: string;
  isDestructive?: boolean;
  isLoading?: boolean;
}

export const ConfirmDialog: React.FC<ConfirmDialogProps> = ({
  isOpen,
  onClose,
  onConfirm,
  title,
  message,
  confirmLabel = 'Confirm',
  cancelLabel = 'Cancel',
  isDestructive = false,
  isLoading = false,
}) => {
  return (
    <Modal isOpen={isOpen} onClose={onClose} title={title} maxWidth="sm">
      <div className="space-y-4 text-[#F5F5F5]">
        <div className="flex items-start gap-3">
          <div
            className={`p-2.5 rounded-xl shrink-0 border ${
              isDestructive
                ? 'bg-rose-500/10 border-rose-500/20 text-rose-400'
                : 'bg-[#FF2D8D]/10 border-[#FF2D8D]/20 text-[#FF2D8D]'
            }`}
          >
            <AlertTriangle className="w-5 h-5" />
          </div>
          <p className="text-xs text-[#A1A1AA] leading-relaxed pt-0.5">{message}</p>
        </div>

        <div className="flex justify-end gap-3 pt-3 border-t border-white/10">
          <button
            type="button"
            onClick={onClose}
            disabled={isLoading}
            className="px-4 py-2 text-xs font-medium text-[#A1A1AA] hover:text-white hover:bg-white/5 rounded-xl transition disabled:opacity-50"
          >
            {cancelLabel}
          </button>
          <button
            type="button"
            onClick={onConfirm}
            disabled={isLoading}
            className={`inline-flex items-center gap-2 px-4 py-2 text-xs font-semibold rounded-xl text-white shadow-lg transition disabled:opacity-50 ${
              isDestructive
                ? 'bg-rose-600 hover:bg-rose-500 shadow-rose-600/30'
                : 'bg-[#FF2D8D] hover:bg-[#FF479D] shadow-[#FF2D8D]/30'
            }`}
          >
            {isLoading && <Loader2 className="w-3.5 h-3.5 animate-spin" />}
            <span>{confirmLabel}</span>
          </button>
        </div>
      </div>
    </Modal>
  );
};
