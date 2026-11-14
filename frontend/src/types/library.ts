export type AcademicCategory =
  | 'DATA_STRUCTURES'
  | 'ALGORITHMS'
  | 'OPERATING_SYSTEMS'
  | 'DATABASE_SYSTEMS'
  | 'COMPUTER_NETWORKS'
  | 'COMPUTER_ORGANIZATION'
  | 'SOFTWARE_ENGINEERING'
  | 'ARTIFICIAL_INTELLIGENCE'
  | 'MACHINE_LEARNING'
  | 'COMPILER_DESIGN'
  | 'THEORY_OF_COMPUTATION'
  | 'MATHEMATICS_FOR_CS'
  | 'CYBERSECURITY'
  | 'CLOUD_COMPUTING'
  | 'WEB_TECHNOLOGIES'
  | 'OTHER';

export type LibraryResourceType =
  | 'LECTURE_NOTES'
  | 'TEXTBOOK'
  | 'PRESENTATION'
  | 'LAB_MANUAL'
  | 'ASSIGNMENT'
  | 'PREVIOUS_EXAM_PAPER'
  | 'RESEARCH_PAPER'
  | 'TUTORIAL'
  | 'REFERENCE_GUIDE'
  | 'OTHER';

export type AcademicLevel =
  | 'UNDERGRADUATE_LOWER'
  | 'UNDERGRADUATE_UPPER'
  | 'GRADUATE'
  | 'DOCTORAL'
  | 'ALL_LEVELS';

export type RightsType =
  | 'ORIGINAL_WORK'
  | 'PUBLIC_DOMAIN'
  | 'CREATIVE_COMMONS'
  | 'INSTITUTIONAL_AUTHORIZED'
  | 'OPEN_ACCESS'
  | 'OTHER';

export type LibraryResourceStatus =
  | 'DRAFT'
  | 'PENDING_REVIEW'
  | 'PUBLISHED'
  | 'REJECTED'
  | 'SUSPENDED'
  | 'REMOVED';

export type ModerationAction =
  | 'SUBMITTED'
  | 'APPROVED'
  | 'REJECTED'
  | 'SUSPENDED'
  | 'RESTORED'
  | 'REMOVED';

export type ReportReasonCategory =
  | 'COPYRIGHT_INFRINGEMENT'
  | 'INAPPROPRIATE_CONTENT'
  | 'INCORRECT_ATTRIBUTION'
  | 'DEFECTIVE_CORRUPT'
  | 'SPAM_MISLEADING'
  | 'OTHER';

export type ReportStatus = 'PENDING' | 'RESOLVED' | 'DISMISSED';

export interface LibraryResourceSummary {
  id: string;
  title: string;
  description: string;
  category: AcademicCategory;
  categoryDisplayName?: string;
  resourceType: LibraryResourceType;
  resourceTypeDisplayName?: string;
  academicLevel: AcademicLevel;
  academicLevelDisplayName?: string;
  language: string;
  contributorId: string;
  contributorName: string;
  fileName: string;
  fileType: string;
  fileSize: number;
  pageCount?: number;
  rightsType: RightsType;
  rightsTypeDisplayName?: string;
  licenseName: string;
  status: LibraryResourceStatus;
  downloadsCount: number;
  tags: string[];
  createdAt: string;
  publishedAt?: string;
}

export interface LibraryResourceDetail extends LibraryResourceSummary {
  contributorEmail?: string;
  mimeType: string;
  checksumSha256?: string;
  sourceAttribution?: string;
  rejectionReason?: string;
  updatedAt: string;
  canDownload: boolean;
  canEdit: boolean;
  canModerate: boolean;
}

export interface LibraryUploadSlotRequest {
  fileName: string;
  fileType: string;
  fileSize: number;
  contentType: string;
}

export interface LibraryUploadSlotResponse {
  resourceId: string;
  storageKey: string;
  uploadUrl: string;
  expiresInSeconds: number;
}

export interface CreateLibrarySubmissionPayload {
  id?: string;
  title: string;
  description: string;
  category: AcademicCategory;
  resourceType: LibraryResourceType;
  academicLevel?: AcademicLevel;
  language?: string;
  storageKey: string;
  fileName: string;
  fileType: string;
  mimeType: string;
  fileSize: number;
  pageCount?: number;
  rightsType: RightsType;
  licenseName: string;
  sourceAttribution?: string;
  warrantyAgreed: boolean;
  tags?: string[];
  submitForReview?: boolean;
}

export interface UpdateLibrarySubmissionPayload {
  title?: string;
  description?: string;
  category?: AcademicCategory;
  resourceType?: LibraryResourceType;
  academicLevel?: AcademicLevel;
  language?: string;
  rightsType?: RightsType;
  licenseName?: string;
  sourceAttribution?: string;
  tags?: string[];
}

export interface ModerationActionPayload {
  reason?: string;
  notes?: string;
}

export interface ModerationLog {
  id: string;
  resourceId: string;
  resourceTitle: string;
  moderatorId: string;
  moderatorName: string;
  action: ModerationAction;
  previousStatus: LibraryResourceStatus;
  newStatus: LibraryResourceStatus;
  reason?: string;
  notes?: string;
  createdAt: string;
}

export interface CreateReportPayload {
  reasonCategory: ReportReasonCategory;
  explanation: string;
}

export interface ReportResolutionPayload {
  action: ReportStatus;
  resolutionNotes: string;
}

export interface LibraryReport {
  id: string;
  resourceId: string;
  resourceTitle: string;
  reporterId: string;
  reporterName: string;
  reasonCategory: ReportReasonCategory;
  reasonCategoryDisplayName?: string;
  explanation: string;
  status: ReportStatus;
  resolutionNotes?: string;
  resolvedById?: string;
  resolvedByName?: string;
  createdAt: string;
  resolvedAt?: string;
}

export interface LibraryDownloadUrlResponse {
  downloadUrl: string;
  fileName: string;
  expiresInSeconds: number;
}

export interface OptionItem {
  value: string;
  label: string;
}
