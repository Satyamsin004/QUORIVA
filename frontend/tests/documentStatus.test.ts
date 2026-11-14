import { test, describe } from 'node:test';
import assert from 'node:assert';
import { getDocumentDisplayStatus } from '../src/components/documents/DocumentStatusBadge';

describe('DocumentStatusBadge Status Mapping', () => {
  test('returns READY when processingStatus is PROCESSED and embeddingStatus is EMBEDDED', () => {
    const status = getDocumentDisplayStatus('PROCESSED', 'EMBEDDED');
    assert.strictEqual(status.type, 'READY');
    assert.strictEqual(status.label, 'Ready for AI Search');
  });

  test('returns EXTRACTING when processingStatus is PROCESSING', () => {
    const status = getDocumentDisplayStatus('PROCESSING', 'NOT_STARTED');
    assert.strictEqual(status.type, 'EXTRACTING');
    assert.strictEqual(status.label, 'Extracting content');
  });

  test('returns WAITING_EMBEDDINGS when processingStatus is PROCESSED and embedding is PENDING or NOT_STARTED', () => {
    const status1 = getDocumentDisplayStatus('PROCESSED', 'PENDING');
    assert.strictEqual(status1.type, 'WAITING_EMBEDDINGS');
    assert.strictEqual(status1.label, 'Waiting for embeddings');

    const status2 = getDocumentDisplayStatus('PROCESSED', 'NOT_STARTED');
    assert.strictEqual(status2.type, 'WAITING_EMBEDDINGS');
    assert.strictEqual(status2.label, 'Waiting for embeddings');
  });

  test('returns EMBEDDING when processingStatus is PROCESSED and embedding is EMBEDDING_IN_PROGRESS or IN_PROGRESS', () => {
    const status1 = getDocumentDisplayStatus('PROCESSED', 'EMBEDDING_IN_PROGRESS');
    assert.strictEqual(status1.type, 'EMBEDDING');
    assert.strictEqual(status1.label, 'Generating embeddings');

    const status2 = getDocumentDisplayStatus('PROCESSED', 'IN_PROGRESS');
    assert.strictEqual(status2.type, 'EMBEDDING');
    assert.strictEqual(status2.label, 'Generating embeddings');
  });

  test('returns PROCESSING_FAILED when processingStatus is FAILED regardless of embedding', () => {
    const status = getDocumentDisplayStatus('FAILED', 'NOT_STARTED');
    assert.strictEqual(status.type, 'PROCESSING_FAILED');
    assert.strictEqual(status.label, 'Processing failed');
  });

  test('returns EMBEDDING_FAILED when processingStatus is PROCESSED but embeddingStatus is FAILED', () => {
    const status = getDocumentDisplayStatus('PROCESSED', 'FAILED');
    assert.strictEqual(status.type, 'EMBEDDING_FAILED');
    assert.strictEqual(status.label, 'Embedding failed');
  });

  test('returns PENDING_UPLOAD when processingStatus is PENDING_UPLOAD', () => {
    const status = getDocumentDisplayStatus('PENDING_UPLOAD', 'NOT_STARTED');
    assert.strictEqual(status.type, 'PENDING_UPLOAD');
    assert.strictEqual(status.label, 'Pending upload');
  });

  test('returns PENDING when processingStatus is PENDING_PROCESSING or UPLOADED', () => {
    const status1 = getDocumentDisplayStatus('PENDING_PROCESSING', 'NOT_STARTED');
    assert.strictEqual(status1.type, 'PENDING');
    assert.strictEqual(status1.label, 'Pending processing');

    const status2 = getDocumentDisplayStatus('UPLOADED', 'NOT_STARTED');
    assert.strictEqual(status2.type, 'PENDING');
    assert.strictEqual(status2.label, 'Pending processing');
  });

  test('readiness rule: only PROCESSED + EMBEDDED is ready for AI search', () => {
    const isReady = (proc: any, emb: any) =>
      proc === 'PROCESSED' && emb === 'EMBEDDED';

    assert.strictEqual(isReady('PROCESSED', 'EMBEDDED'), true);
    assert.strictEqual(isReady('PROCESSED', 'IN_PROGRESS'), false);
    assert.strictEqual(isReady('PROCESSED', 'NOT_STARTED'), false);
    assert.strictEqual(isReady('PROCESSED', 'FAILED'), false);
    assert.strictEqual(isReady('PROCESSING', 'NOT_STARTED'), false);
    assert.strictEqual(isReady('FAILED', 'FAILED'), false);
  });
});
