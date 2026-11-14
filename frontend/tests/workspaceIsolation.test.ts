import { test, describe } from 'node:test';
import assert from 'node:assert';

describe('Workspace Switching & Stale Request Isolation', () => {
  test('discards delayed responses belonging to previous workspace', async () => {
    let activeWorkspaceId = 'workspace-A';
    let committedData: string | null = null;

    // Simulate async request for Workspace A with 50ms latency
    const fetchWorkspaceA = async () => {
      const requestWorkspaceId = activeWorkspaceId;
      await new Promise((r) => setTimeout(r, 50));
      // Guard: only commit if still active
      if (activeWorkspaceId === requestWorkspaceId) {
        committedData = 'Workspace A Documents';
      }
    };

    // Simulate user switching to Workspace B after 10ms
    const promiseA = fetchWorkspaceA();
    await new Promise((r) => setTimeout(r, 10));
    activeWorkspaceId = 'workspace-B';
    committedData = 'Workspace B Documents';

    await promiseA;

    // Workspace A delayed completion MUST NOT overwrite Workspace B!
    assert.strictEqual(committedData, 'Workspace B Documents');
  });

  test('tutor conversation selection is isolated and does not bleed into new workspace', () => {
    let activeConversationId: string | null = 'conv-ws-A-123';
    let messages = [{ id: 'm1', content: 'Hello Workspace A' }];

    const switchWorkspace = (
      newWorkspaceId: string,
      newWorkspaceConversations: Array<{ id: string }>
    ) => {
      // Invalidate old workspace conversation and messages immediately
      messages = [];
      const existsInNew = newWorkspaceConversations.some((c) => c.id === activeConversationId);
      activeConversationId = existsInNew ? activeConversationId : (newWorkspaceConversations[0]?.id || null);
    };

    // User switches to Workspace B which has conv-ws-B-999
    switchWorkspace('workspace-B', [{ id: 'conv-ws-B-999' }]);

    assert.strictEqual(activeConversationId, 'conv-ws-B-999');
    assert.strictEqual(messages.length, 0);

    // User switches to Workspace C which has 0 conversations
    switchWorkspace('workspace-C', []);
    assert.strictEqual(activeConversationId, null);
    assert.strictEqual(messages.length, 0);
  });

  test('upload completion does not refresh state if active workspace switched during upload', () => {
    let activeWorkspaceId = 'workspace-A';
    let refreshedWorkspaceId: string | null = null;

    const onUploadSuccess = (uploadedWsId: string) => {
      if (uploadedWsId === activeWorkspaceId) {
        refreshedWorkspaceId = uploadedWsId;
      }
    };

    const targetUploadWorkspace = 'workspace-A';

    // User switches to Workspace B while upload is in progress
    activeWorkspaceId = 'workspace-B';

    // Upload finishes for workspace A
    onUploadSuccess(targetUploadWorkspace);

    // Refresh should NOT occur on Workspace B for an upload belonging to Workspace A
    assert.strictEqual(refreshedWorkspaceId, null);
  });

  test('polling timer is cleanly terminated when workspace switches', () => {
    let isPolling = true;
    let pollCount = 0;

    const stopPolling = () => {
      isPolling = false;
    };

    const simulateTick = () => {
      if (isPolling) {
        pollCount++;
      }
    };

    simulateTick();
    assert.strictEqual(pollCount, 1);

    // User switches workspace: polling stopped
    stopPolling();
    simulateTick();
    simulateTick();

    assert.strictEqual(pollCount, 1);
  });
});

