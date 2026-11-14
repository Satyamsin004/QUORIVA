import React, { createContext, useContext, useState, useEffect, useCallback, ReactNode } from 'react';
import { Workspace } from '@/types';
import { workspaceService } from '@/services/workspaceService';
import { useAuth } from './AuthContext';

const ACTIVE_WORKSPACE_KEY = 'cs_scholar_active_workspace_id';

interface WorkspaceContextType {
  workspaces: Workspace[];
  currentWorkspace: Workspace | null;
  isLoadingWorkspaces: boolean;
  setCurrentWorkspace: (workspace: Workspace) => void;
  createWorkspace: (name: string, description?: string) => Promise<Workspace>;
  updateWorkspace: (id: string, name?: string, description?: string) => Promise<Workspace>;
  deleteWorkspace: (id: string) => Promise<void>;
  refreshWorkspaces: () => Promise<void>;
}

const WorkspaceContext = createContext<WorkspaceContextType | undefined>(undefined);

export const WorkspaceProvider: React.FC<{ children: ReactNode }> = ({ children }) => {
  const { isAuthenticated, user } = useAuth();
  const [workspaces, setWorkspaces] = useState<Workspace[]>([]);
  const [currentWorkspace, setCurrentWorkspaceState] = useState<Workspace | null>(null);
  const [isLoadingWorkspaces, setIsLoadingWorkspaces] = useState<boolean>(false);

  const setCurrentWorkspace = useCallback((workspace: Workspace) => {
    setCurrentWorkspaceState(workspace);
    localStorage.setItem(ACTIVE_WORKSPACE_KEY, workspace.id);
  }, []);

  const refreshWorkspaces = useCallback(async () => {
    if (!isAuthenticated) {
      setWorkspaces([]);
      setCurrentWorkspaceState(null);
      return;
    }

    setIsLoadingWorkspaces(true);
    try {
      let list = await workspaceService.listWorkspaces();

      // If user has no workspaces (e.g. edge case or brand new), create default one
      if (list.length === 0) {
        const defaultWs = await workspaceService.createWorkspace(
          `${user?.fullName || 'Student'}'s Workspace`,
          'Default private academic workspace'
        );
        list = [defaultWs];
      }

      setWorkspaces(list);

      // Restore active workspace if exists
      const savedId = localStorage.getItem(ACTIVE_WORKSPACE_KEY);
      const matched = list.find((w) => w.id === savedId);
      if (matched) {
        setCurrentWorkspaceState(matched);
      } else {
        setCurrentWorkspace(list[0]);
      }
    } catch (err) {
      console.error('Failed to load workspaces:', err);
    } finally {
      setIsLoadingWorkspaces(false);
    }
  }, [isAuthenticated, user?.fullName, setCurrentWorkspace]);

  useEffect(() => {
    refreshWorkspaces();
  }, [refreshWorkspaces]);

  const createWorkspace = async (name: string, description?: string): Promise<Workspace> => {
    const created = await workspaceService.createWorkspace(name, description);
    setWorkspaces((prev) => [created, ...prev]);
    setCurrentWorkspace(created);
    return created;
  };

  const updateWorkspace = async (
    id: string,
    name?: string,
    description?: string
  ): Promise<Workspace> => {
    const updated = await workspaceService.updateWorkspace(id, name, description);
    setWorkspaces((prev) => prev.map((w) => (w.id === id ? updated : w)));
    if (currentWorkspace?.id === id) {
      setCurrentWorkspace(updated);
    }
    return updated;
  };

  const deleteWorkspace = async (id: string): Promise<void> => {
    await workspaceService.deleteWorkspace(id);
    const remaining = workspaces.filter((w) => w.id !== id);
    setWorkspaces(remaining);
    if (currentWorkspace?.id === id) {
      if (remaining.length > 0) {
        setCurrentWorkspace(remaining[0]);
      } else {
        setCurrentWorkspaceState(null);
        localStorage.removeItem(ACTIVE_WORKSPACE_KEY);
      }
    }
  };

  return (
    <WorkspaceContext.Provider
      value={{
        workspaces,
        currentWorkspace,
        isLoadingWorkspaces,
        setCurrentWorkspace,
        createWorkspace,
        updateWorkspace,
        deleteWorkspace,
        refreshWorkspaces,
      }}
    >
      {children}
    </WorkspaceContext.Provider>
  );
};

export const useWorkspace = (): WorkspaceContextType => {
  const context = useContext(WorkspaceContext);
  if (!context) {
    throw new Error('useWorkspace must be used within a WorkspaceProvider');
  }
  return context;
};

