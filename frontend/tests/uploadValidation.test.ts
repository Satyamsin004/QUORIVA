import { test, describe } from 'node:test';
import assert from 'node:assert';

const MAX_FILE_SIZE_BYTES = 50 * 1024 * 1024; // 50MB
const ALLOWED_EXTENSIONS = ['.pdf', '.docx', '.pptx'];
const ALLOWED_MIME_TYPES = [
  'application/pdf',
  'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
  'application/vnd.openxmlformats-officedocument.presentationml.presentation',
  'application/msword',
  'application/vnd.ms-powerpoint',
];

function validateUploadFile(fileName: string, mimeType: string, fileSize: number): { valid: boolean; error?: string } {
  const ext = fileName.substring(fileName.lastIndexOf('.')).toLowerCase();
  const isExtensionAllowed = ALLOWED_EXTENSIONS.includes(ext);
  const isMimeAllowed = ALLOWED_MIME_TYPES.includes(mimeType);

  if (!isExtensionAllowed && !isMimeAllowed) {
    return {
      valid: false,
      error: `Unsupported format "${ext}". Please upload an academic PDF, DOCX, or PPTX file.`,
    };
  }

  if (fileSize > MAX_FILE_SIZE_BYTES) {
    return {
      valid: false,
      error: `File size exceeds the 50 MB limit.`,
    };
  }

  return { valid: true };
}

describe('Document Upload Validation', () => {
  test('accepts valid PDF within 50MB limit', () => {
    const res = validateUploadFile('cs189_lecture01.pdf', 'application/pdf', 5 * 1024 * 1024);
    assert.strictEqual(res.valid, true);
    assert.strictEqual(res.error, undefined);
  });

  test('accepts valid DOCX file', () => {
    const res = validateUploadFile(
      'algorithms_syllabus.docx',
      'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
      1024 * 500
    );
    assert.strictEqual(res.valid, true);
  });

  test('accepts valid PPTX file', () => {
    const res = validateUploadFile(
      'distributed_systems_week3.pptx',
      'application/vnd.openxmlformats-officedocument.presentationml.presentation',
      12 * 1024 * 1024
    );
    assert.strictEqual(res.valid, true);
  });

  test('rejects unsupported file format (.exe)', () => {
    const res = validateUploadFile('installer.exe', 'application/x-msdownload', 1024 * 1024);
    assert.strictEqual(res.valid, false);
    assert.match(res.error || '', /Unsupported format/);
  });

  test('rejects unsupported file format (.mp4)', () => {
    const res = validateUploadFile('lecture_recording.mp4', 'video/mp4', 20 * 1024 * 1024);
    assert.strictEqual(res.valid, false);
    assert.match(res.error || '', /Unsupported format/);
  });

  test('rejects file larger than 50MB', () => {
    const res = validateUploadFile('large_textbook.pdf', 'application/pdf', 51 * 1024 * 1024);
    assert.strictEqual(res.valid, false);
    assert.match(res.error || '', /exceeds the 50 MB limit/);
  });
});

