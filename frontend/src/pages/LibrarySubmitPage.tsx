import React, { useState, useEffect } from 'react';
import { libraryService } from '@/services/libraryService';
import {
  OptionItem,
  AcademicCategory,
  LibraryResourceType,
  AcademicLevel,
  RightsType,
  CreateLibrarySubmissionPayload,
  UpdateLibrarySubmissionPayload,
} from '@/types';
import {
  Upload,
  FileText,
  ShieldCheck,
  CheckCircle2,
  AlertTriangle,
  ArrowLeft,
  Loader2,
  X,
} from 'lucide-react';

interface LibrarySubmitPageProps {
  onBack: () => void;
  onSuccess: (resourceId: string) => void;
  editResourceId?: string | null;
}

const RIGHTS_OPTIONS: { value: RightsType; label: string; desc: string }[] = [
  {
    value: 'ORIGINAL_WORK',
    label: 'Original Work',
    desc: 'You are the author/creator of this material and own full copyright to share it.',
  },
  {
    value: 'PUBLIC_DOMAIN',
    label: 'Public Domain (CC0 / Expired)',
    desc: 'The material is dedicated to the public domain or copyright has expired.',
  },
  {
    value: 'CREATIVE_COMMONS',
    label: 'Creative Commons Licensed',
    desc: 'The work was published under a Creative Commons license permitting redistribution.',
  },
  {
    value: 'INSTITUTIONAL_AUTHORIZED',
    label: 'Institutionally Authorized',
    desc: 'Your university or institution authorized this material for educational sharing.',
  },
  {
    value: 'OPEN_ACCESS',
    label: 'Open Access Publication',
    desc: 'Academic paper or survey published under an open access scholarly license.',
  },
  {
    value: 'OTHER',
    label: 'Other Permitted Educational Use',
    desc: 'Other legal permission granted by the copyright owner.',
  },
];

const COMMON_LICENSES = [
  'CC BY 4.0',
  'CC BY-SA 4.0',
  'CC BY-NC 4.0',
  'CC0 1.0 Universal',
  'MIT License',
  'Apache 2.0',
  'Academic Free License (AFL 3.0)',
  'Open Educational Resource (OER) Permission',
];

export const LibrarySubmitPage: React.FC<LibrarySubmitPageProps> = ({
  onBack,
  onSuccess,
  editResourceId,
}) => {
  const [categories, setCategories] = useState<OptionItem[]>([]);
  const [resourceTypes, setResourceTypes] = useState<OptionItem[]>([]);
  const [academicLevels, setAcademicLevels] = useState<OptionItem[]>([]);

  // File Upload State
  const [uploadProgress, setUploadProgress] = useState(0);
  const [isUploading, setIsUploading] = useState(false);
  const [uploadedStorageKey, setUploadedStorageKey] = useState<string | null>(null);
  const [uploadedFileName, setUploadedFileName] = useState<string>('');
  const [uploadedFileType, setUploadedFileType] = useState<string>('PDF');
  const [uploadedMimeType, setUploadedMimeType] = useState<string>('application/pdf');
  const [uploadedFileSize, setUploadedFileSize] = useState<number>(0);
  const [allocatedResourceId, setAllocatedResourceId] = useState<string | null>(null);

  // Form Fields
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [category, setCategory] = useState<AcademicCategory>('DATA_STRUCTURES');
  const [resourceType, setResourceType] = useState<LibraryResourceType>('LECTURE_NOTES');
  const [academicLevel, setAcademicLevel] = useState<AcademicLevel>('ALL_LEVELS');
  const [language, setLanguage] = useState('en');

  // Rights & Licensing Fields
  const [rightsType, setRightsType] = useState<RightsType>('ORIGINAL_WORK');
  const [licenseName, setLicenseName] = useState('CC BY 4.0');
  const [customLicense, setCustomLicense] = useState('');
  const [sourceAttribution, setSourceAttribution] = useState('');
  const [rightsWarrantyAgreed, setRightsWarrantyAgreed] = useState(false);

  // Tags
  const [tagInput, setTagInput] = useState('');
  const [tags, setTags] = useState<string[]>([]);

  const [isLoadingMetadata, setIsLoadingMetadata] = useState(true);
  const [isSaving, setIsSaving] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  useEffect(() => {
    const loadTaxonomies = async () => {
      try {
        const [cats, types, levels] = await Promise.all([
          libraryService.getCategories(),
          libraryService.getResourceTypes(),
          libraryService.getAcademicLevels(),
        ]);
        setCategories(cats);
        setResourceTypes(types);
        setAcademicLevels(levels);

        if (editResourceId) {
          const existing = await libraryService.getResourceDetail(editResourceId);
          setTitle(existing.title);
          setDescription(existing.description);
          setCategory(existing.category);
          setResourceType(existing.resourceType);
          setAcademicLevel(existing.academicLevel);
          setLanguage(existing.language || 'en');
          setRightsType(existing.rightsType);
          setLicenseName(existing.licenseName);
          setSourceAttribution(existing.sourceAttribution || '');
          setTags(existing.tags || []);
          setAllocatedResourceId(existing.id);
          setUploadedFileName(existing.fileName);
          setUploadedFileType(existing.fileType);
          setUploadedFileSize(existing.fileSize);
          setUploadedStorageKey('library/' + existing.id + '/' + existing.fileName);
          setRightsWarrantyAgreed(true);
        }
      } catch (err: any) {
        setErrorMessage(err?.response?.data?.message || 'Failed to initialize submission form.');
      } finally {
        setIsLoadingMetadata(false);
      }
    };
    loadTaxonomies();
  }, [editResourceId]);

  const handleFileChange = async (e: React.ChangeEvent<HTMLInputElement>) => {
    if (!e.target.files || e.target.files.length === 0) return;
    const file = e.target.files[0];

    // Auto populate title if empty
    if (!title) {
      const cleanName = file.name.replace(/\.[^/.]+$/, '').replace(/[_-]/g, ' ');
      setTitle(cleanName);
    }

    try {
      setIsUploading(true);
      setErrorMessage(null);
      setUploadProgress(10);

      const fileExtension = file.name.split('.').pop()?.toUpperCase() || 'PDF';
      const slot = await libraryService.requestUploadSlot({
        fileName: file.name,
        fileType: fileExtension,
        fileSize: file.size,
        contentType: file.type || 'application/pdf',
      });

      setAllocatedResourceId(slot.resourceId);
      setUploadedStorageKey(slot.storageKey);
      setUploadedFileName(file.name);
      setUploadedFileType(fileExtension);
      setUploadedMimeType(file.type || 'application/pdf');
      setUploadedFileSize(file.size);

      setUploadProgress(40);
      await libraryService.uploadFileToPresignedUrl(
        slot.uploadUrl,
        file,
        file.type || 'application/pdf',
        (pct) => setUploadProgress(40 + Math.round(pct * 0.55))
      );
      setUploadProgress(100);
    } catch (err: any) {
      setErrorMessage(err?.response?.data?.message || 'Failed to upload resource file to S3.');
      setUploadProgress(0);
    } finally {
      setIsUploading(false);
    }
  };

  const handleAddTag = () => {
    const trimmed = tagInput.trim().toLowerCase().replace(/[^a-z0-9_-]/g, '');
    if (trimmed && !tags.includes(trimmed) && tags.length < 8) {
      setTags([...tags, trimmed]);
      setTagInput('');
    }
  };

  const handleRemoveTag = (tagToRemove: string) => {
    setTags(tags.filter((t) => t !== tagToRemove));
  };

  const handleSubmit = async (submitForReview: boolean) => {
    if (!editResourceId && !uploadedStorageKey) {
      setErrorMessage('Please select and upload a resource file before submitting.');
      return;
    }

    if (!title.trim()) {
      setErrorMessage('Resource title is required.');
      return;
    }
    if (!description.trim()) {
      setErrorMessage('Resource description is required.');
      return;
    }
    if (!rightsWarrantyAgreed) {
      setErrorMessage('You must verify and agree to the rights and copyright declaration warranty.');
      return;
    }

    if (rightsType === 'OTHER' && !sourceAttribution.trim()) {
      setErrorMessage("Source attribution and permission details are required when Rights Type is 'OTHER'.");
      return;
    }

    const finalLicense = licenseName === 'Other' ? customLicense.trim() : licenseName.trim();
    if (!finalLicense) {
      setErrorMessage('Please specify the academic license name.');
      return;
    }

    try {
      setIsSaving(true);
      setErrorMessage(null);

      if (editResourceId) {
        const updatePayload: UpdateLibrarySubmissionPayload = {
          title: title.trim(),
          description: description.trim(),
          category,
          resourceType,
          academicLevel,
          language,
          rightsType,
          licenseName: finalLicense,
          sourceAttribution: sourceAttribution.trim() || undefined,
          tags,
        };
        const updated = await libraryService.updateSubmission(editResourceId, updatePayload);
        if (submitForReview) {
          await libraryService.submitForReview(editResourceId);
        }
        onSuccess(updated.id);
      } else {
        const createPayload: CreateLibrarySubmissionPayload = {
          id: allocatedResourceId || undefined,
          title: title.trim(),
          description: description.trim(),
          category,
          resourceType,
          academicLevel,
          language,
          storageKey: uploadedStorageKey!,
          fileName: uploadedFileName,
          fileType: uploadedFileType,
          mimeType: uploadedMimeType,
          fileSize: uploadedFileSize,
          rightsType,
          licenseName: finalLicense,
          sourceAttribution: sourceAttribution.trim() || undefined,
          warrantyAgreed: rightsWarrantyAgreed,
          tags,
          submitForReview,
        };
        const created = await libraryService.createSubmission(createPayload);
        onSuccess(created.id);
      }
    } catch (err: any) {
      setErrorMessage(err?.response?.data?.message || 'Failed to save resource submission.');
    } finally {
      setIsSaving(false);
    }
  };

  if (isLoadingMetadata) {
    return (
      <div className="flex flex-col items-center justify-center py-24 text-slate-400">
        <Loader2 className="w-8 h-8 animate-spin text-indigo-500 mb-3" />
        <p className="text-sm">Preparing submission form...</p>
      </div>
    );
  }

  return (
    <div className="max-w-4xl mx-auto py-8 px-4 sm:px-6 space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between pb-4 border-b border-slate-800">
        <button
          onClick={onBack}
          className="inline-flex items-center gap-2 text-xs font-semibold text-slate-400 hover:text-white transition"
        >
          <ArrowLeft className="w-4 h-4" /> Back to Library
        </button>
        <span className="text-xs font-medium text-slate-400">
          {editResourceId ? 'Edit Submission' : 'Contribute Academic Resource'}
        </span>
      </div>

      {errorMessage && (
        <div className="p-4 bg-rose-500/10 border border-rose-500/20 rounded-2xl flex items-center gap-3 text-xs text-rose-300">
          <AlertTriangle className="w-5 h-5 text-rose-400 shrink-0" />
          <span>{errorMessage}</span>
        </div>
      )}

      <div className="bg-slate-900 border border-slate-800 rounded-2xl p-6 sm:p-8 space-y-8">
        {/* Step 1: File Upload */}
        <div>
          <h3 className="text-sm font-bold uppercase tracking-wider text-indigo-400 mb-3 flex items-center gap-2">
            <Upload className="w-4 h-4" /> 1. Resource Document Asset
          </h3>

          {!uploadedStorageKey ? (
            <div className="border-2 border-dashed border-slate-700 hover:border-indigo-500/60 rounded-2xl p-6 text-center cursor-pointer transition bg-slate-950/40">
              <input
                type="file"
                id="file-upload"
                onChange={handleFileChange}
                accept=".pdf,.docx,.pptx,.txt"
                className="hidden"
                disabled={isUploading}
              />
              <label htmlFor="file-upload" className="cursor-pointer block">
                <div className="w-12 h-12 rounded-xl bg-indigo-500/10 flex items-center justify-center text-indigo-400 mx-auto mb-3">
                  {isUploading ? (
                    <Loader2 className="w-6 h-6 animate-spin" />
                  ) : (
                    <Upload className="w-6 h-6" />
                  )}
                </div>
                <p className="text-sm font-semibold text-white">
                  {isUploading ? 'Uploading to isolated library storage...' : 'Click to select document'}
                </p>
                <p className="text-xs text-slate-400 mt-1">PDF, DOCX, PPTX, or TXT up to 50MB</p>
              </label>

              {isUploading && (
                <div className="w-full bg-slate-800 rounded-full h-1.5 mt-4 overflow-hidden">
                  <div
                    className="bg-indigo-600 h-1.5 rounded-full transition-all duration-300"
                    style={{ width: `${uploadProgress}%` }}
                  />
                </div>
              )}
            </div>
          ) : (
            <div className="flex items-center justify-between p-4 bg-slate-950 border border-emerald-500/30 rounded-xl">
              <div className="flex items-center gap-3">
                <div className="w-10 h-10 rounded-xl bg-emerald-500/10 text-emerald-400 flex items-center justify-center">
                  <CheckCircle2 className="w-5 h-5" />
                </div>
                <div className="text-xs">
                  <p className="font-semibold text-white">{uploadedFileName}</p>
                  <p className="text-slate-400">
                    {uploadedFileType} • {(uploadedFileSize / 1024).toFixed(1)} KB • Stored in library repository
                  </p>
                </div>
              </div>

              {!editResourceId && (
                <button
                  type="button"
                  onClick={() => {
                    setUploadedStorageKey(null);
                  }}
                  className="text-xs text-slate-400 hover:text-rose-400 p-1"
                >
                  Replace File
                </button>
              )}
            </div>
          )}
        </div>

        {/* Step 2: Metadata */}
        <div className="space-y-4 pt-4 border-t border-slate-800">
          <h3 className="text-sm font-bold uppercase tracking-wider text-indigo-400 mb-3 flex items-center gap-2">
            <FileText className="w-4 h-4" /> 2. Academic Metadata
          </h3>

          <div>
            <label className="block text-xs font-semibold uppercase text-slate-400 mb-1">
              Resource Title *
            </label>
            <input
              type="text"
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              placeholder="e.g. Advanced Operating Systems: Memory Virtualization"
              className="w-full bg-slate-950 border border-slate-800 rounded-xl px-4 py-2.5 text-sm text-white placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-indigo-500/50"
              required
            />
          </div>

          <div>
            <label className="block text-xs font-semibold uppercase text-slate-400 mb-1">
              Detailed Description *
            </label>
            <textarea
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              rows={4}
              placeholder="Provide a clear description of syllabus topics, lecture weeks, or key algorithmic concepts..."
              className="w-full bg-slate-950 border border-slate-800 rounded-xl p-3 text-sm text-white placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-indigo-500/50"
              required
            />
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
            <div>
              <label className="block text-xs font-semibold uppercase text-slate-400 mb-1">
                Category *
              </label>
              <select
                value={category}
                onChange={(e) => setCategory(e.target.value as AcademicCategory)}
                className="w-full bg-slate-950 border border-slate-800 rounded-xl px-3 py-2 text-xs text-white focus:outline-none focus:ring-1 focus:ring-indigo-500"
              >
                {categories.map((c) => (
                  <option key={c.value} value={c.value}>
                    {c.label}
                  </option>
                ))}
              </select>
            </div>

            <div>
              <label className="block text-xs font-semibold uppercase text-slate-400 mb-1">
                Resource Type *
              </label>
              <select
                value={resourceType}
                onChange={(e) => setResourceType(e.target.value as LibraryResourceType)}
                className="w-full bg-slate-950 border border-slate-800 rounded-xl px-3 py-2 text-xs text-white focus:outline-none focus:ring-1 focus:ring-indigo-500"
              >
                {resourceTypes.map((t) => (
                  <option key={t.value} value={t.value}>
                    {t.label}
                  </option>
                ))}
              </select>
            </div>

            <div>
              <label className="block text-xs font-semibold uppercase text-slate-400 mb-1">
                Academic Level
              </label>
              <select
                value={academicLevel}
                onChange={(e) => setAcademicLevel(e.target.value as AcademicLevel)}
                className="w-full bg-slate-950 border border-slate-800 rounded-xl px-3 py-2 text-xs text-white focus:outline-none focus:ring-1 focus:ring-indigo-500"
              >
                {academicLevels.map((l) => (
                  <option key={l.value} value={l.value}>
                    {l.label}
                  </option>
                ))}
              </select>
            </div>
          </div>

          {/* Tags Input */}
          <div>
            <label className="block text-xs font-semibold uppercase text-slate-400 mb-1">
              Topic Tags (up to 8)
            </label>
            <div className="flex gap-2">
              <input
                type="text"
                value={tagInput}
                onChange={(e) => setTagInput(e.target.value)}
                onKeyDown={(e) => {
                  if (e.key === 'Enter') {
                    e.preventDefault();
                    handleAddTag();
                  }
                }}
                placeholder="Add tag and press Enter..."
                className="flex-1 bg-slate-950 border border-slate-800 rounded-xl px-3.5 py-2 text-xs text-white placeholder-slate-500 focus:outline-none focus:ring-1 focus:ring-indigo-500"
              />
              <button
                type="button"
                onClick={handleAddTag}
                className="px-3.5 py-2 bg-slate-800 hover:bg-slate-700 text-white text-xs font-semibold rounded-xl"
              >
                Add
              </button>
            </div>
            {tags.length > 0 && (
              <div className="flex flex-wrap gap-1.5 mt-2">
                {tags.map((t) => (
                  <span
                    key={t}
                    className="inline-flex items-center gap-1 px-2.5 py-1 rounded-lg text-xs bg-slate-800 text-slate-300 border border-slate-700"
                  >
                    #{t}
                    <button
                      type="button"
                      onClick={() => handleRemoveTag(t)}
                      className="hover:text-rose-400 ml-1"
                    >
                      <X className="w-3 h-3" />
                    </button>
                  </span>
                ))}
              </div>
            )}
          </div>
        </div>

        {/* Step 3: Rights Declaration & Licensing (Mandatory) */}
        <div className="space-y-4 pt-4 border-t border-slate-800">
          <h3 className="text-sm font-bold uppercase tracking-wider text-emerald-400 mb-3 flex items-center gap-2">
            <ShieldCheck className="w-4 h-4" /> 3. Rights Declaration & Copyright Licensing
          </h3>

          <div>
            <label className="block text-xs font-semibold uppercase text-slate-400 mb-1.5">
              Rights Classification *
            </label>
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 text-xs">
              {RIGHTS_OPTIONS.map((opt) => (
                <div
                  key={opt.value}
                  onClick={() => setRightsType(opt.value)}
                  className={`p-3 rounded-xl border cursor-pointer transition ${
                    rightsType === opt.value
                      ? 'bg-emerald-500/10 border-emerald-500/50 text-white ring-1 ring-emerald-500'
                      : 'bg-slate-950/70 border-slate-800 text-slate-400 hover:border-slate-700'
                  }`}
                >
                  <p className="font-semibold text-white mb-0.5">{opt.label}</p>
                  <p className="text-[11px] text-slate-400 leading-snug">{opt.desc}</p>
                </div>
              ))}
            </div>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <label className="block text-xs font-semibold uppercase text-slate-400 mb-1">
                License Specification *
              </label>
              <select
                value={licenseName}
                onChange={(e) => setLicenseName(e.target.value)}
                className="w-full bg-slate-950 border border-slate-800 rounded-xl px-3 py-2 text-xs text-white focus:outline-none focus:ring-1 focus:ring-indigo-500"
              >
                {COMMON_LICENSES.map((lic) => (
                  <option key={lic} value={lic}>
                    {lic}
                  </option>
                ))}
                <option value="Other">Other Custom License</option>
              </select>
            </div>

            {licenseName === 'Other' && (
              <div>
                <label className="block text-xs font-semibold uppercase text-slate-400 mb-1">
                  Custom License Name *
                </label>
                <input
                  type="text"
                  value={customLicense}
                  onChange={(e) => setCustomLicense(e.target.value)}
                  placeholder="e.g. University Department Educational Permit"
                  className="w-full bg-slate-950 border border-slate-800 rounded-xl px-3.5 py-2 text-xs text-white focus:outline-none focus:ring-1 focus:ring-indigo-500"
                  required
                />
              </div>
            )}
          </div>

          <div>
            <label className="block text-xs font-semibold uppercase text-slate-400 mb-1">
              Source Attribution & Acknowledgement
            </label>
            <input
              type="text"
              value={sourceAttribution}
              onChange={(e) => setSourceAttribution(e.target.value)}
              placeholder="e.g. Lecture delivered by Prof. John Smith, Fall 2025, University CS Dept"
              className="w-full bg-slate-950 border border-slate-800 rounded-xl px-3.5 py-2 text-xs text-white placeholder-slate-500 focus:outline-none focus:ring-1 focus:ring-indigo-500"
            />
          </div>

          {/* Legal Warranty Checkbox */}
          <div className="p-4 bg-slate-950 border border-slate-800 rounded-xl">
            <label className="flex items-start gap-3 cursor-pointer">
              <input
                type="checkbox"
                checked={rightsWarrantyAgreed}
                onChange={(e) => setRightsWarrantyAgreed(e.target.checked)}
                className="mt-1 rounded bg-slate-900 border-slate-700 text-indigo-600 focus:ring-indigo-500"
              />
              <span className="text-xs text-slate-300 leading-relaxed">
                <strong className="text-white block mb-0.5">Contributor Warranty & Declaration:</strong>
                I declare that this academic material is either my original work, in the public domain, licensed under Creative Commons, or authorized for educational redistribution. I understand that submissions are subject to academic editorial review and that infringing or improper uploads may be suspended or removed.
              </span>
            </label>
          </div>
        </div>

        {/* Step 4: Submission Actions */}
        <div className="pt-4 border-t border-slate-800 flex flex-col sm:flex-row items-center justify-end gap-3">
          <button
            type="button"
            onClick={onBack}
            disabled={isSaving}
            className="w-full sm:w-auto px-4 py-2.5 text-xs font-medium text-slate-300 hover:text-white bg-slate-800 hover:bg-slate-700 rounded-xl transition"
          >
            Cancel
          </button>

          <button
            type="button"
            onClick={() => handleSubmit(false)}
            disabled={isSaving || isUploading}
            className="w-full sm:w-auto px-4 py-2.5 text-xs font-semibold text-slate-200 bg-slate-800 hover:bg-slate-700 border border-slate-700 rounded-xl transition"
          >
            Save as Draft
          </button>

          <button
            type="button"
            onClick={() => handleSubmit(true)}
            disabled={isSaving || isUploading || !rightsWarrantyAgreed}
            className="w-full sm:w-auto px-5 py-2.5 text-xs font-semibold text-white bg-indigo-600 hover:bg-indigo-500 rounded-xl flex items-center justify-center gap-2 shadow-lg shadow-indigo-600/20 transition disabled:opacity-50"
          >
            {isSaving && <Loader2 className="w-3.5 h-3.5 animate-spin" />}
            <span>Submit for Editorial Review</span>
          </button>
        </div>
      </div>
    </div>
  );
};
