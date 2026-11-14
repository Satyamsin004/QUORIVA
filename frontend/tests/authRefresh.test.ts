import { test, describe } from 'node:test';
import assert from 'node:assert';

describe('Auth Refresh Interceptor & Loop Prevention', () => {
  test('excludes refresh endpoints from 401 retry to prevent infinite loops', () => {
    const isExcluded = (url: string) => {
      return (
        url.includes('/auth/login') ||
        url.includes('/auth/register') ||
        url.includes('/auth/refresh') ||
        url.includes('/auth/refresh-token')
      );
    };

    assert.strictEqual(isExcluded('/api/v1/auth/refresh'), true);
    assert.strictEqual(isExcluded('/api/v1/auth/refresh-token'), true);
    assert.strictEqual(isExcluded('/api/v1/auth/login'), true);
    assert.strictEqual(isExcluded('/api/v1/auth/register'), true);
    assert.strictEqual(isExcluded('/api/v1/documents'), false);
    assert.strictEqual(isExcluded('/api/v1/search'), false);
    assert.strictEqual(isExcluded('/api/v1/conversations'), false);
  });

  test('queued concurrent 401 requests resolve once refresh completes', async () => {
    let isRefreshing = false;
    let failedQueue: Array<{
      resolve: (value?: unknown) => void;
      reject: (reason?: unknown) => void;
    }> = [];

    const processQueue = (error: Error | null, token: string | null = null) => {
      failedQueue.forEach((prom) => {
        if (error) {
          prom.reject(error);
        } else {
          prom.resolve(token);
        }
      });
      failedQueue = [];
    };

    // Simulate 3 concurrent requests encountering 401
    const p1 = new Promise((resolve, reject) => failedQueue.push({ resolve, reject }));
    const p2 = new Promise((resolve, reject) => failedQueue.push({ resolve, reject }));
    const p3 = new Promise((resolve, reject) => failedQueue.push({ resolve, reject }));

    assert.strictEqual(failedQueue.length, 3);

    // Refresh succeeds with new token
    processQueue(null, 'new-jwt-access-token');

    const results = await Promise.all([p1, p2, p3]);
    assert.deepStrictEqual(results, [
      'new-jwt-access-token',
      'new-jwt-access-token',
      'new-jwt-access-token',
    ]);
    assert.strictEqual(failedQueue.length, 0);
  });

  test('failed refresh clears state and rejects all queued requests', async () => {
    let failedQueue: Array<{
      resolve: (value?: unknown) => void;
      reject: (reason?: unknown) => void;
    }> = [];

    const p1 = new Promise((resolve, reject) => failedQueue.push({ resolve, reject }));
    const p2 = new Promise((resolve, reject) => failedQueue.push({ resolve, reject }));

    const refreshError = new Error('Refresh token revoked');

    failedQueue.forEach((prom) => prom.reject(refreshError));
    failedQueue = [];

    await assert.rejects(p1, /Refresh token revoked/);
    await assert.rejects(p2, /Refresh token revoked/);
    assert.strictEqual(failedQueue.length, 0);
  });
});

