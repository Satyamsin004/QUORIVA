import React, { useState, useRef, DragEvent, ChangeEvent } from 'react';
import { Modal } from '@/components/common/Modal';
import { documentService } from '@/services/documentService';
import { UploadCloud, File, AlertCircle, CheckCircle, Loader2 } from 'lucide-react';

interface DocumentUploadDialogProps {
  isOpen: boolean;
  onClose: () => void;
  workspaceId: string;
  defaultSubject?: string;
  onSuccess: (uploadedWorkspaceId?: string) => void;
}

const MAX_FILE_SIZE_BYTES = 50 * 1024 * 1024; // 50MB
const ALLOWED_EXTENSIONS = ['.pdf', '.docx', '.pptx'];
const ALLOWED_MIME_TYPES = [
  'application/pdf',
  'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
  'application/vnd.openxmlformats-officedocument.presentationml.presentation',
  'application/msword',
  'application/vnd.ms-powerpoint',
];

export const DocumentUploadDialog: React.FC<DocumentUploadDialogProps> = ({
  isOpen,
  onClose,
  workspaceId,
  defaultSubject = 'Java',
  onSuccess,
}) => {
  const [file, setFile] = useState<File | null>(null);
  const [subject, setSubject] = useState(defaultSubject);
  const [isNewSubject, setIsNewSubject] = useState(false);
  const [customSubject, setCustomSubject] = useState('');
  const [isDragging, setIsDragging] = useState(false);
  const [validationError, setValidationError] = useState<string | null>(null);
  const [isUploading, setIsUploading] = useState(false);
  const [uploadProgress, setUploadProgress] = useState(0);
  const [statusMessage, setStatusMessage] = useState<string | null>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);

  React.useEffect(() => {
    if (defaultSubject) {
      setSubject(defaultSubject);
    }
  }, [defaultSubject, isOpen]);

  const resetState = () => {
    setFile(null);
    setSubject(defaultSubject || 'Java');
    setIsNewSubject(false);
    setCustomSubject('');
    setIsDragging(false);
    setValidationError(null);
    setIsUploading(false);
    setUploadProgress(0);
    setStatusMessage(null);
    if (fileInputRef.current) {
      fileInputRef.current.value = '';
    }
  };

  const handleClose = () => {
    if (!isUploading) {
      resetState();
      onClose();
    }
  };

  const validateFile = (selectedFile: File): boolean => {
    setValidationError(null);

    const ext = selectedFile.name.substring(selectedFile.name.lastIndexOf('.')).toLowerCase();
    const isExtensionAllowed = ALLOWED_EXTENSIONS.includes(ext);
    const isMimeAllowed = ALLOWED_MIME_TYPES.includes(selectedFile.type);

    if (!isExtensionAllowed && !isMimeAllowed) {
      setValidationError(
        `Unsupported format "${ext}". Please upload an academic PDF, DOCX, or PPTX file.`
      );
      return false;
    }

    if (selectedFile.size > MAX_FILE_SIZE_BYTES) {
      setValidationError(
        `File size (${(selectedFile.size / (1024 * 1024)).toFixed(1)} MB) exceeds the 50 MB limit.`
      );
      return false;
    }

    return true;
  };

  const handleDragOver = (e: DragEvent<HTMLDivElement>) => {
    e.preventDefault();
    setIsDragging(true);
  };

  const handleDragLeave = (e: DragEvent<HTMLDivElement>) => {
    e.preventDefault();
    setIsDragging(false);
  };

  const handleDrop = (e: DragEvent<HTMLDivElement>) => {
    e.preventDefault();
    setIsDragging(false);

    if (e.dataTransfer.files && e.dataTransfer.files.length > 0) {
      const droppedFile = e.dataTransfer.files[0];
      if (validateFile(droppedFile)) {
        setFile(droppedFile);
      }
    }
  };

  const handleFileChange = (e: ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files.length > 0) {
      const selectedFile = e.target.files[0];
      if (validateFile(selectedFile)) {
        setFile(selectedFile);
      }
    }
  };

  const handleUpload = async () => {
    if (!file || !workspaceId) return;

    const targetWorkspaceId = workspaceId;

    setIsUploading(true);
    setValidationError(null);
    setUploadProgress(5);
    setStatusMessage('Requesting secure direct upload authorization...');

    try {
      // Step 1: Request presigned upload URL from Spring Boot
      const contentType = file.type || 'application/octet-stream';
      const finalSubject = isNewSubject ? (customSubject.trim() || 'General') : subject;
      const uploadIntent = await documentService.getUploadUrl({
        workspaceId: targetWorkspaceId,
        filename: file.name,
        contentType,
        fileSize: file.size,
        subject: finalSubject,
      });

      setStatusMessage('Uploading academic material directly to object storage...');
      setUploadProgress(15);

      // Step 2: Directly upload file to S3/MinIO via presigned PUT URL
      await documentService.uploadFileToS3(uploadIntent.uploadUrl, file, (progress) => {
        const mapped = Math.round(15 + progress * 0.7);
        setUploadProgress(mapped);
      });

      // Step 3: Complete upload registration in Spring Boot
      setStatusMessage('Registering document and enqueuing ingestion pipeline...');
      setUploadProgress(90);

      await documentService.completeUpload({
        documentId: uploadIntent.documentId,
        uploadSuccessful: true,
        fileSize: file.size,
      });

      setUploadProgress(100);
      setStatusMessage('Document uploaded successfully!');

      setTimeout(() => {
        resetState();
        onClose();
        onSuccess(targetWorkspaceId);
      }, 700);
    } catch (err: unknown) {
      console.error('Upload failure:', err);
      const msg =
        err instanceof Error
          ? err.message
          : 'Failed to complete document upload. Please try again.';
      setValidationError(msg);
      setIsUploading(false);
      setUploadProgress(0);
      setStatusMessage(null);
    }
  };

  const formatFileSize = (bytes: number): string => {
    if (bytes < 1024) return `${bytes} B`;
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
    return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
  };

  return (
    <Modal isOpen={isOpen} onClose={handleClose} title="Upload Academic Material" maxWidth="lg">
      <div className="space-y-5 text-[#F5F5F5]">
        {/* Drag & drop upload area */}
        <div
          onDragOver={handleDragOver}
          onDragLeave={handleDragLeave}
          onDrop={handleDrop}
          onClick={() => !isUploading && fileInputRef.current?.click()}
          className={`border-2 border-dashed rounded-3xl p-8 text-center transition cursor-pointer flex flex-col items-center justify-center ${
            isDragging
              ? 'border-[#FF2D8D] bg-[#FF2D8D]/10'
              : 'border-white/10 hover:border-[#FF2D8D]/40 bg-[#202025]/50 hover:bg-[#202025]'
          } ${isUploading ? 'pointer-events-none opacity-60' : ''}`}
        >
          <input
            type="file"
            ref={fileInputRef}
            onChange={handleFileChange}
            accept=".pdf,.docx,.pptx,application/pdf,application/vnd.openxmlformats-officedocument.wordprocessingml.document,application/vnd.openxmlformats-officedocument.presentationml.presentation"
            className="hidden"
          />

          <div className="w-12 h-12 rounded-2xl bg-[#FF2D8D]/10 border border-[#FF2D8D]/30 flex items-center justify-center text-[#FF2D8D] mb-3 shadow-sm">
            <UploadCloud className="w-6 h-6" />
          </div>

          <h4 className="text-sm font-semibold text-white">
            Click to upload or drag and drop
          </h4>
          <p className="text-xs text-[#A1A1AA] mt-1">
            Supported formats: PDF, DOCX, PPTX (up to 50 MB)
          </p>
        </div>

        {/* Selected file preview */}
        {file && !validationError && (
          <div className="flex items-center justify-between p-3.5 bg-[#202025] rounded-2xl border border-white/10">
            <div className="flex items-center gap-3 overflow-hidden">
              <div className="p-2.5 rounded-xl bg-[#FF2D8D]/10 text-[#FF2D8D] shrink-0 border border-[#FF2D8D]/20">
                <File className="w-5 h-5" />
              </div>
              <div className="min-w-0">
                <p className="text-sm font-medium text-white truncate">{file.name}</p>
                <p className="text-xs text-[#A1A1AA]">{formatFileSize(file.size)}</p>
              </div>
            </div>
            {!isUploading && (
              <button
                type="button"
                onClick={() => setFile(null)}
                className="text-xs text-[#A1A1AA] hover:text-white font-medium px-2.5 py-1 rounded-lg hover:bg-white/5 transition"
              >
                Change
              </button>
            )}
          </div>
        )}

        {/* Subject / Category Selection */}
        <div className="space-y-2 pt-1">
          <label className="block text-xs font-semibold text-zinc-300">
            Subject / Course Category
          </label>
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-2">
            <select
              value={isNewSubject ? '__new__' : subject}
              onChange={(e) => {
                if (e.target.value === '__new__') {
                  setIsNewSubject(true);
                } else {
                  setIsNewSubject(false);
                  setSubject(e.target.value);
                }
              }}
              disabled={isUploading}
              aria-label="Subject or category selection"
              className="bg-[#202025] border border-white/10 text-white rounded-xl px-3 py-2 text-xs focus:outline-none focus:border-[#FF2D8D]"
            >
              <option value="Java">☕ Java</option>
              <option value="Python">🐍 Python</option>
              <option value="JavaScript">JavaScript</option>
              <option value="Theory of Computation (TOC)">📐 Theory of Computation (TOC)</option>
              <option value="Operating Systems">💻 Operating Systems</option>
              <option value="Computer Networks">🌐 Computer Networks</option>
              <option value="Database Systems (DBMS)">🗄️ Database Systems (DBMS)</option>
              <option value="Data Structures & Algorithms">⚡ Data Structures & Algorithms</option>
              <option value="Distributed Systems">☁️ Distributed Systems</option>
              <option value="General">📚 General / Elective</option>
              <option value="__new__">+ Create New Category...</option>
            </select>

            {isNewSubject && (
              <input
                type="text"
                placeholder="Enter subject name (e.g. TOC)"
                value={customSubject}
                onChange={(e) => setCustomSubject(e.target.value)}
                disabled={isUploading}
                aria-label="Custom subject name"
                className="bg-[#202025] border border-[#FF2D8D]/50 text-white rounded-xl px-3 py-2 text-xs focus:outline-none focus:border-[#FF2D8D]"
                autoFocus
              />
            )}
          </div>
          <p className="text-[11px] text-[#A1A1AA]">
            All materials for this subject will be organized together for targeted practice and revisions.
          </p>
        </div>

        {/* Validation error notice */}
        {validationError && (
          <div className="flex items-start gap-2.5 p-3.5 rounded-2xl bg-rose-500/10 border border-rose-500/20 text-rose-300 text-xs">
            <AlertCircle className="w-4 h-4 shrink-0 mt-0.5" />
            <span className="leading-relaxed">{validationError}</span>
          </div>
        )}

        {/* Upload progress & status */}
        {isUploading && (
          <div className="space-y-2 pt-1">
            <div className="flex items-center justify-between text-xs font-medium text-[#A1A1AA]">
              <span className="flex items-center gap-1.5 text-[#C7F000]">
                <Loader2 className="w-3.5 h-3.5 animate-spin" />
                {statusMessage}
              </span>
              <span className="font-mono text-white">{uploadProgress}%</span>
            </div>
            <div className="w-full bg-[#202025] rounded-full h-2 overflow-hidden border border-white/5">
              <div
                className="bg-[#C7F000] h-2 rounded-full transition-all duration-300"
                style={{ width: `${uploadProgress}%` }}
              />
            </div>
          </div>
        )}

        {/* Actions */}
        <div className="flex items-center justify-end gap-3 pt-3 border-t border-white/10">
          <button
            type="button"
            onClick={handleClose}
            disabled={isUploading}
            className="px-4 py-2 text-xs font-medium text-[#A1A1AA] hover:text-white hover:bg-white/5 rounded-xl transition disabled:opacity-50"
          >
            Cancel
          </button>
          <button
            type="button"
            onClick={handleUpload}
            disabled={!file || isUploading}
            className="btn-chartreuse rounded-full px-6 py-2.5 inline-flex items-center gap-2 text-xs font-bold whitespace-nowrap shadow-md transition"
          >
            {isUploading ? (
              <>
                <Loader2 className="w-4 h-4 animate-spin text-black" />
                <span>Uploading...</span>
              </>
            ) : (
              <>
                <CheckCircle className="w-4 h-4 text-black" />
                <span>Upload Material</span>
              </>
            )}
          </button>
        </div>
      </div>
    </Modal>
  );
};
