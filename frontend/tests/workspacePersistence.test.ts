import { test, describe } from 'node:test';
import assert from 'node:assert';
import { Workspace } from '../src/types/workspace';

const WORKSPACE_STORAGE_KEY = 'cs_scholar_active_workspace_id';

class MockLocalStorage {
  private store: Record<string, string> = {};

  getItem(key: string): string | null {
    return this.store[key] || null;
  }

  setItem(key: string, value: string): void {
    this.store[key] = value;
  }

  removeItem(key: string): void {
    delete this.store[key];
  }

  clear(): void {
    this.store = {};
  }
}

function resolveActiveWorkspace(
  storage: MockLocalStorage,
  workspaces: Workspace[]
): Workspace | null {
  if (workspaces.length === 0) return null;

  const savedId = storage.getItem(WORKSPACE_STORAGE_KEY);
  if (savedId) {
    const found = workspaces.find((w) => w.id === savedId);
    if (found) return found;
  }

  // Fallback to first available workspace
  storage.setItem(WORKSPACE_STORAGE_KEY, workspaces[0].id);
  return workspaces[0];
}

describe('Workspace Resolution and Persistence', () => {
  const dummyWorkspaces: Workspace[] = [
    {
      id: 'ws-1111',
      name: 'Algorithms & Data Structures',
      description: 'CS2040S notes',
      isDefault: true,
      createdAt: '2026-11-12T00:00:00Z',
      updatedAt: '2026-11-12T00:00:00Z',
    },
    {
      id: 'ws-2222',
      name: 'Operating Systems',
      description: 'CS2106 concurrency & kernels',
      isDefault: false,
      createdAt: '2026-11-12T00:00:00Z',
      updatedAt: '2026-11-12T00:00:00Z',
    },
  ];

  test('restores saved workspace ID from localStorage when valid', () => {
    const storage = new MockLocalStorage();
    storage.setItem(WORKSPACE_STORAGE_KEY, 'ws-2222');

    const active = resolveActiveWorkspace(storage, dummyWorkspaces);
    assert.ok(active);
    assert.strictEqual(active.id, 'ws-2222');
    assert.strictEqual(active.name, 'Operating Systems');
  });

  test('falls back to first workspace and updates storage if saved ID does not exist', () => {
    const storage = new MockLocalStorage();
    storage.setItem(WORKSPACE_STORAGE_KEY, 'non-existent-id');

    const active = resolveActiveWorkspace(storage, dummyWorkspaces);
    assert.ok(active);
    assert.strictEqual(active.id, 'ws-1111');
    assert.strictEqual(storage.getItem(WORKSPACE_STORAGE_KEY), 'ws-1111');
  });

  test('returns null when workspace list is empty', () => {
    const storage = new MockLocalStorage();
    const active = resolveActiveWorkspace(storage, []);
    assert.strictEqual(active, null);
  });
});

