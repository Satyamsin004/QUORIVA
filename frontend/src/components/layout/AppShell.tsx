import React, { useState } from 'react';
import { useAuth } from '@/context/AuthContext';
import { useWorkspace } from '@/context/WorkspaceContext';
import { CreateWorkspaceModal } from '@/components/workspace/CreateWorkspaceModal';
import { UserProfileModal } from '@/components/workspace/UserProfileModal';
import { DocumentUploadDialog } from '@/components/documents/DocumentUploadDialog';
import {
  LayoutDashboard,
  FileText,
  Search,
  Bot,
  Activity,
  Plus,
  ChevronDown,
  LogOut,
  Upload,
  Menu,
  X,
  Sparkles,
  BrainCircuit,
} from 'lucide-react';

export type NavRoute =
  | 'dashboard'
  | 'documents'
  | 'search'
  | 'tutor'
  | 'learning'
  | 'health';

interface AppShellProps {
  currentRoute: NavRoute;
  onRouteChange: (route: NavRoute) => void;
  children: React.ReactNode;
  onRefreshDocuments?: () => void;
}

export const AppShell: React.FC<AppShellProps> = ({
  currentRoute,
  onRouteChange,
  children,
  onRefreshDocuments,
}) => {
  const { user, logout } = useAuth();
  const { workspaces, currentWorkspace, setCurrentWorkspace } = useWorkspace();

  const [isMobileMenuOpen, setIsMobileMenuOpen] = useState(false);
  const [isWorkspaceDropdownOpen, setIsWorkspaceDropdownOpen] = useState(false);
  const [isCreateWorkspaceOpen, setIsCreateWorkspaceOpen] = useState(false);
  const [isProfileModalOpen, setIsProfileModalOpen] = useState(false);
  const [isUploadDialogOpen, setIsUploadDialogOpen] = useState(false);

  const navItems = [
    { id: 'dashboard' as NavRoute, label: 'Dashboard', icon: LayoutDashboard },
    { id: 'documents' as NavRoute, label: 'My Documents', icon: FileText },
    { id: 'search' as NavRoute, label: 'Academic Search', icon: Search },
    { id: 'tutor' as NavRoute, label: 'AI Academic Tutor', icon: Bot },
    { id: 'learning' as NavRoute, label: 'Learning Studio', icon: BrainCircuit },
    { id: 'health' as NavRoute, label: 'System Health', icon: Activity },
  ];

  const handleNavClick = (route: NavRoute) => {
    onRouteChange(route);
    setIsMobileMenuOpen(false);
  };

  const getInitials = (name?: string) => {
    if (!name) return 'CS';
    return name
      .split(' ')
      .map((part) => part[0])
      .slice(0, 2)
      .join('')
      .toUpperCase();
  };

  return (
    <div className="min-h-screen bg-[#09090B] text-[#F5F5F5] flex flex-col lg:flex-row font-sans selection:bg-[#FF2D8D] selection:text-white">
      {/* Mobile Top Header */}
      <header className="lg:hidden bg-[#111114] text-white border-b border-white/10 px-4 py-3 flex items-center justify-between sticky top-0 z-40">
        <div className="flex items-center gap-2.5">
          <div className="w-8 h-8 rounded-full bg-gradient-to-tr from-[#FF2D8D] to-[#C7F000] p-0.5 flex items-center justify-center shadow-glow-pink">
            <div className="w-full h-full bg-[#09090B] rounded-full flex items-center justify-center">
              <span className="font-extrabold text-xs text-white font-mono">Q</span>
            </div>
          </div>
          <span className="font-bold text-sm tracking-tight text-white">QUORIVA</span>
        </div>
        <button
          type="button"
          onClick={() => setIsMobileMenuOpen(!isMobileMenuOpen)}
          className="p-1.5 text-[#A1A1AA] hover:text-white rounded-lg focus:outline-none"
        >
          {isMobileMenuOpen ? <X className="w-6 h-6" /> : <Menu className="w-6 h-6" />}
        </button>
      </header>

      {/* Left Sidebar */}
      <aside
        className={`fixed inset-y-0 left-0 z-40 w-72 bg-[#111114] text-[#F5F5F5] border-r border-white/10 flex flex-col transition-transform duration-200 ease-in-out lg:static lg:translate-x-0 ${
          isMobileMenuOpen ? 'translate-x-0' : '-translate-x-full'
        }`}
      >
        {/* Brand Header */}
        <div className="p-5 border-b border-white/10 flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-2xl bg-gradient-to-tr from-[#FF2D8D] to-[#C7F000] p-0.5 flex items-center justify-center shadow-glow-pink">
              <div className="w-full h-full bg-[#09090B] rounded-2xl flex items-center justify-center">
                <span className="font-extrabold text-sm text-white font-mono">Q</span>
              </div>
            </div>
            <div>
              <div className="flex items-center gap-1.5">
                <span className="font-bold text-base text-white tracking-tight">
                  QUORIVA
                </span>
                <span className="text-[10px] px-1.5 py-0.5 rounded font-extrabold font-mono bg-[#C7F000] text-[#09090B]">
                  AI
                </span>
              </div>
              <p className="text-[11px] text-[#A1A1AA]">Understand what you study.</p>
            </div>
          </div>
        </div>

        {/* Workspace Switcher */}
        <div className="p-4 border-b border-white/10">
          <label className="block text-[10px] font-bold text-[#A1A1AA] uppercase tracking-wider mb-1.5 font-mono">
            Active Study Workspace
          </label>
          <div className="relative">
            <button
              type="button"
              onClick={() => setIsWorkspaceDropdownOpen(!isWorkspaceDropdownOpen)}
              className="w-full flex items-center justify-between px-3.5 py-2.5 rounded-xl bg-[#171719] hover:bg-[#202025] border border-white/10 text-left transition text-xs text-white"
            >
              <span className="truncate font-medium">
                {currentWorkspace?.name || 'Personal Workspace'}
              </span>
              <ChevronDown className="w-3.5 h-3.5 text-[#A1A1AA] shrink-0 ml-2" />
            </button>

            {/* Dropdown Menu */}
            {isWorkspaceDropdownOpen && (
              <div className="absolute top-full left-0 right-0 mt-1.5 bg-[#171719] border border-white/10 rounded-xl shadow-card-dark py-1 z-50 text-xs">
                <div className="max-h-48 overflow-y-auto divide-y divide-white/5">
                  {workspaces.map((ws) => (
                    <button
                      key={ws.id}
                      type="button"
                      onClick={() => {
                        setCurrentWorkspace(ws);
                        setIsWorkspaceDropdownOpen(false);
                      }}
                      className={`w-full text-left px-3.5 py-2.5 transition flex items-center justify-between ${
                        ws.id === currentWorkspace?.id
                          ? 'bg-[#FF2D8D]/15 text-[#FF2D8D] font-semibold'
                          : 'text-[#A1A1AA] hover:bg-[#202025] hover:text-white'
                      }`}
                    >
                      <span className="truncate">{ws.name}</span>
                      {ws.id === currentWorkspace?.id && (
                        <span className="w-1.5 h-1.5 rounded-full bg-[#FF2D8D] shrink-0 ml-2 shadow-glow-pink" />
                      )}
                    </button>
                  ))}
                </div>
                <div className="p-1 border-t border-white/5">
                  <button
                    type="button"
                    onClick={() => {
                      setIsWorkspaceDropdownOpen(false);
                      setIsCreateWorkspaceOpen(true);
                    }}
                    className="w-full text-left px-3 py-2 text-[#C7F000] hover:bg-[#C7F000]/10 rounded-lg transition flex items-center gap-2 font-medium"
                  >
                    <Plus className="w-3.5 h-3.5" />
                    <span>Create New Workspace</span>
                  </button>
                </div>
              </div>
            )}
          </div>
        </div>

        {/* Navigation Items */}
        <nav className="flex-1 p-3 space-y-1.5 overflow-y-auto">
          {navItems.map((item) => {
            const Icon = item.icon;
            const isActive = currentRoute === item.id;
            return (
              <button
                key={item.id}
                type="button"
                onClick={() => handleNavClick(item.id)}
                className={`w-full flex items-center gap-3 px-3.5 py-2.5 rounded-xl text-xs font-semibold transition ${
                  isActive
                    ? 'bg-[#FF2D8D] text-white shadow-glow-pink'
                    : 'text-[#A1A1AA] hover:text-white hover:bg-[#171719]'
                }`}
              >
                <Icon className={`w-4 h-4 ${isActive ? 'text-white' : 'text-[#A1A1AA]'}`} />
                <span>{item.label}</span>
              </button>
            );
          })}
        </nav>

        {/* Quick Upload Action */}
        <div className="p-3 border-t border-white/10">
          <button
            type="button"
            onClick={() => setIsUploadDialogOpen(true)}
            className="btn-chartreuse w-full flex items-center justify-center gap-2 py-2.5 px-5 rounded-xl font-bold text-xs whitespace-nowrap transition shadow-sm"
          >
            <Upload className="w-4 h-4 shrink-0" />
            <span>Upload Material</span>
          </button>
        </div>

        {/* User Account Menu */}
        <div className="p-4 border-t border-white/10 bg-[#09090B]/60">
          <div className="flex items-center justify-between gap-3">
            <button
              type="button"
              onClick={() => setIsProfileModalOpen(true)}
              className="flex items-center gap-3 min-w-0 text-left hover:opacity-90 transition group"
            >
              <div className="w-9 h-9 rounded-xl bg-[#171719] text-[#FF2D8D] border border-white/10 flex items-center justify-center font-bold text-xs shrink-0 group-hover:border-[#FF2D8D]/50 shadow-sm">
                {getInitials(user?.fullName)}
              </div>
              <div className="min-w-0">
                <p className="text-xs font-semibold text-white truncate">
                  {user?.fullName || 'Student'}
                </p>
                <p className="text-[11px] text-[#A1A1AA] truncate">{user?.email}</p>
              </div>
            </button>
            <button
              type="button"
              onClick={logout}
              title="Logout"
              className="p-2 text-[#A1A1AA] hover:text-rose-400 hover:bg-rose-500/10 rounded-lg transition shrink-0"
            >
              <LogOut className="w-4 h-4" />
            </button>
          </div>
        </div>
      </aside>

      {/* Main Content Area */}
      <div className="flex-1 flex flex-col min-h-screen overflow-hidden bg-[#09090B]">
        {/* Top Header Bar for Desktop */}
        <header className="hidden lg:flex bg-[#111114]/80 backdrop-blur-md border-b border-white/10 px-8 py-3.5 items-center justify-between sticky top-0 z-30">
          <div className="flex items-center gap-3">
            <h1 className="text-base font-bold text-white capitalize">
              {currentRoute === 'health'
                ? 'System Diagnostics'
                : currentRoute === 'tutor'
                ? 'QUORIVA AI Tutor'
                : currentRoute === 'search'
                ? 'Academic Search'
                : currentRoute === 'documents'
                ? 'My Documents'
                : currentRoute === 'learning'
                ? 'Learning Studio'
                : 'Workspace Dashboard'}
            </h1>
            <span className="text-[11px] px-2.5 py-0.5 rounded-full bg-[#171719] text-[#C7F000] font-mono border border-white/10">
              {currentWorkspace?.name || 'Personal Workspace'}
            </span>
          </div>

          <div className="flex items-center gap-3">
            {currentRoute !== 'tutor' && (
              <button
                type="button"
                onClick={() => onRouteChange('tutor')}
                className="inline-flex items-center gap-1.5 px-3.5 py-1.5 rounded-xl bg-[#171719] text-white hover:border-[#FF2D8D]/50 border border-white/10 text-xs font-semibold transition"
              >
                <Sparkles className="w-3.5 h-3.5 text-[#FF2D8D]" />
                Ask AI Tutor
              </button>
            )}
            <button
              type="button"
              onClick={() => setIsUploadDialogOpen(true)}
              className="btn-chartreuse rounded-full px-5 py-2.5 inline-flex items-center gap-2 text-xs font-bold whitespace-nowrap transition shrink-0 shadow-md"
            >
              <Upload className="w-4 h-4 shrink-0" />
              <span>Upload Material</span>
            </button>
          </div>
        </header>

        {/* View Content */}
        <main className="flex-1 overflow-y-auto">
          {children}
        </main>
      </div>

      {/* Modals & Dialogs */}
      {currentWorkspace && (
        <DocumentUploadDialog
          isOpen={isUploadDialogOpen}
          onClose={() => setIsUploadDialogOpen(false)}
          workspaceId={currentWorkspace.id}
          onSuccess={(uploadedWsId) => {
            if (onRefreshDocuments && (!uploadedWsId || uploadedWsId === currentWorkspace?.id)) {
              onRefreshDocuments();
            }
            if (currentRoute !== 'documents') {
              onRouteChange('documents');
            }
          }}
        />
      )}

      <CreateWorkspaceModal
        isOpen={isCreateWorkspaceOpen}
        onClose={() => setIsCreateWorkspaceOpen(false)}
      />

      <UserProfileModal
        isOpen={isProfileModalOpen}
        onClose={() => setIsProfileModalOpen(false)}
      />
    </div>
  );
};
