import { test, describe } from 'node:test';
import assert from 'node:assert';
import {
  RightsType,
  LibraryResourceStatus,
  AcademicCategory,
  LibraryResourceType,
} from '../src/types/library';

// Helper validator functions reflecting Phase 8 business rules
function validateRightsDeclaration(
  rightsType?: RightsType,
  licenseName?: string,
  warrantyAgreed?: boolean,
  sourceAttribution?: string
): { valid: boolean; error?: string } {
  if (!rightsType) {
    return { valid: false, error: 'A valid rights declaration is mandatory.' };
  }
  if (!licenseName || !licenseName.trim()) {
    return { valid: false, error: 'License specification is mandatory.' };
  }
  if (!warrantyAgreed) {
    return { valid: false, error: 'Contributor must confirm the copyright warranty declaration.' };
  }
  if (rightsType === 'OTHER' && (!sourceAttribution || !sourceAttribution.trim())) {
    return { valid: false, error: 'Source attribution and explanation are mandatory when rights type is OTHER.' };
  }
  return { valid: true };
}

function validateLibraryStorageKeyIsolation(storageKey: string): { isolated: boolean; error?: string } {
  if (!storageKey.startsWith('library/')) {
    return {
      isolated: false,
      error: 'Library assets must be stored under dedicated "library/" prefix to guarantee tenancy isolation.',
    };
  }
  if (storageKey.startsWith('workspaces/')) {
    return {
      isolated: false,
      error: 'Private workspace document storage keys must NEVER be reused for shared library resources.',
    };
  }
  return { isolated: true };
}

function canUserModerate(userRole: string): boolean {
  return userRole === 'EDUCATOR' || userRole === 'ADMIN';
}

function canUserEditSubmission(status: LibraryResourceStatus, isContributor: boolean): boolean {
  if (!isContributor) return false;
  return status === 'DRAFT' || status === 'REJECTED';
}

function canUserDownload(status: LibraryResourceStatus, isContributor: boolean, isModerator: boolean): boolean {
  if (status === 'PUBLISHED') return true;
  return isContributor || isModerator;
}

function composeSearchLibraryParams(
  query?: string,
  category?: AcademicCategory,
  resourceType?: LibraryResourceType,
  tag?: string,
  sort: 'RECENT' | 'POPULAR' | 'TITLE' = 'RECENT'
): Record<string, string> {
  const params: Record<string, string> = { sort };
  if (query && query.trim()) params.q = query.trim();
  if (category) params.category = category;
  if (resourceType) params.resourceType = resourceType;
  if (tag && tag.trim()) params.tag = tag.trim().toLowerCase();
  return params;
}

describe('Phase 8: Shared Academic Library Logic', () => {
  describe('Rights Declaration & Copyright Licensing', () => {
    test('accepts valid original work with CC BY 4.0 license and agreed warranty', () => {
      const res = validateRightsDeclaration('ORIGINAL_WORK', 'CC BY 4.0', true);
      assert.strictEqual(res.valid, true);
      assert.strictEqual(res.error, undefined);
    });

    test('accepts valid public domain resource with CC0 license', () => {
      const res = validateRightsDeclaration('PUBLIC_DOMAIN', 'CC0 1.0 Universal', true);
      assert.strictEqual(res.valid, true);
    });

    test('rejects submission when rights type is omitted', () => {
      const res = validateRightsDeclaration(undefined, 'MIT License', true);
      assert.strictEqual(res.valid, false);
      assert.match(res.error || '', /rights declaration is mandatory/i);
    });

    test('rejects submission when license name is empty or whitespace', () => {
      const res = validateRightsDeclaration('CREATIVE_COMMONS', '   ', true);
      assert.strictEqual(res.valid, false);
      assert.match(res.error || '', /license specification is mandatory/i);
    });

    test('rejects submission when contributor does not agree to warranty', () => {
      const res = validateRightsDeclaration('ORIGINAL_WORK', 'CC BY 4.0', false);
      assert.strictEqual(res.valid, false);
      assert.match(res.error || '', /copyright warranty/i);
    });

    test('rejects OTHER rights type without source attribution', () => {
      const res = validateRightsDeclaration('OTHER', 'Custom Permission', true, '');
      assert.strictEqual(res.valid, false);
      assert.match(res.error || '', /source attribution/i);
    });

    test('accepts OTHER rights type with valid source attribution', () => {
      const res = validateRightsDeclaration(
        'OTHER',
        'Custom Permission',
        true,
        'Donated by Prof. Alan Turing with written permission'
      );
      assert.strictEqual(res.valid, true);
    });
  });

  describe('Strict Tenancy & Storage Key Isolation', () => {
    test('approves dedicated library S3 storage key', () => {
      const key = 'library/550e8400-e29b-41d4-a716-446655440000/concurrency_notes.pdf';
      const res = validateLibraryStorageKeyIsolation(key);
      assert.strictEqual(res.isolated, true);
    });

    test('strictly rejects workspace storage keys to prevent cross-tenant document leakage', () => {
      const leakedKey = 'workspaces/12345/docs/private_exam.pdf';
      const res = validateLibraryStorageKeyIsolation(leakedKey);
      assert.strictEqual(res.isolated, false);
      assert.match(res.error || '', /dedicated "library\/" prefix/i);
    });

    test('rejects arbitrary non-library root prefix', () => {
      const key = 'uploads/test.pdf';
      const res = validateLibraryStorageKeyIsolation(key);
      assert.strictEqual(res.isolated, false);
    });
  });

  describe('Role-Based Moderation & Access Control', () => {
    test('allows educators to moderate', () => {
      assert.strictEqual(canUserModerate('EDUCATOR'), true);
    });

    test('allows admins to moderate', () => {
      assert.strictEqual(canUserModerate('ADMIN'), true);
    });

    test('strictly denies students from moderation actions', () => {
      assert.strictEqual(canUserModerate('STUDENT'), false);
    });

    test('strictly denies researchers from moderation actions', () => {
      assert.strictEqual(canUserModerate('RESEARCHER'), false);
    });

    test('contributor can edit draft and rejected submissions', () => {
      assert.strictEqual(canUserEditSubmission('DRAFT', true), true);
      assert.strictEqual(canUserEditSubmission('REJECTED', true), true);
      assert.strictEqual(canUserEditSubmission('PENDING_REVIEW', true), false);
      assert.strictEqual(canUserEditSubmission('PUBLISHED', true), false);
      assert.strictEqual(canUserEditSubmission('DRAFT', false), false);
    });

    test('published resource can be downloaded by anyone', () => {
      assert.strictEqual(canUserDownload('PUBLISHED', false, false), true);
    });

    test('unpublished resource can only be downloaded by contributor or moderator', () => {
      assert.strictEqual(canUserDownload('PENDING_REVIEW', false, false), false);
      assert.strictEqual(canUserDownload('PENDING_REVIEW', true, false), true);
      assert.strictEqual(canUserDownload('PENDING_REVIEW', false, true), true);
      assert.strictEqual(canUserDownload('DRAFT', false, false), false);
    });
  });

  describe('Search & Discovery Parameter Composition', () => {
    test('composes default search parameters with RECENT sort', () => {
      const params = composeSearchLibraryParams();
      assert.strictEqual(params.sort, 'RECENT');
      assert.strictEqual(params.q, undefined);
    });

    test('composes full search filter query with category, tag, and custom sort', () => {
      const params = composeSearchLibraryParams(
        'distributed systems',
        'CLOUD_COMPUTING',
        'LECTURE_NOTES',
        'Raft',
        'POPULAR'
      );
      assert.strictEqual(params.q, 'distributed systems');
      assert.strictEqual(params.category, 'CLOUD_COMPUTING');
      assert.strictEqual(params.resourceType, 'LECTURE_NOTES');
      assert.strictEqual(params.tag, 'raft');
      assert.strictEqual(params.sort, 'POPULAR');
    });
  });
});

