import React, { useState, useEffect, Suspense, lazy } from 'react';
import { useAuth } from '@/context/AuthContext';
import { AppShell, NavRoute } from '@/components/layout/AppShell';
import { Loader2, GraduationCap } from 'lucide-react';

// Route-level code splitting via lazy loading
const AuthPage = lazy(() => import('@/pages/AuthPage').then((m) => ({ default: m.AuthPage })));
const HomePage = lazy(() => import('@/pages/HomePage').then((m) => ({ default: m.HomePage })));
const DashboardPage = lazy(() => import('@/pages/DashboardPage').then((m) => ({ default: m.DashboardPage })));
const DocumentsPage = lazy(() => import('@/pages/DocumentsPage').then((m) => ({ default: m.DocumentsPage })));
const SearchPage = lazy(() => import('@/pages/SearchPage').then((m) => ({ default: m.SearchPage })));
const TutorPage = lazy(() => import('@/pages/TutorPage').then((m) => ({ default: m.TutorPage })));
const LearningStudioPage = lazy(() => import('@/pages/LearningStudioPage').then((m) => ({ default: m.LearningStudioPage })));
const SystemHealthPage = lazy(() => import('@/pages/SystemHealthPage').then((m) => ({ default: m.SystemHealthPage })));

const RouteLoadingFallback: React.FC = () => (
  <div className="w-full h-full min-h-[400px] flex flex-col items-center justify-center p-8 text-neutral-400">
    <div className="w-10 h-10 rounded-xl bg-neutral-900 border border-neutral-800 flex items-center justify-center mb-3">
      <Loader2 className="w-5 h-5 animate-spin text-[#C7F000]" />
    </div>
    <span className="text-xs font-mono tracking-wider uppercase">Loading Workspace Module...</span>
  </div>
);

const VALID_ROUTES: NavRoute[] = [
  'dashboard',
  'documents',
  'search',
  'tutor',
  'learning',
  'health',
];

export const AppRoutes: React.FC = () => {
  const { isAuthenticated, isLoading } = useAuth();

  const getHash = (): string => window.location.hash.replace('#', '').split('?')[0];

  const getInitialRoute = (): NavRoute => {
    const raw = getHash();
    if (VALID_ROUTES.includes(raw as NavRoute)) {
      return raw as NavRoute;
    }
    return 'dashboard';
  };

  const [currentRoute, setCurrentRoute] = useState<NavRoute>(getInitialRoute);
  const [currentHash, setCurrentHash] = useState<string>(getHash);
  const [tutorPrefillPrompt, setTutorPrefillPrompt] = useState<string | null>(null);
  const [targetConversationId, setTargetConversationId] = useState<string | null>(null);

  // Sync route on hashchange
  useEffect(() => {
    const handleHashChange = () => {
      const raw = getHash();
      setCurrentHash(raw);
      if (VALID_ROUTES.includes(raw as NavRoute)) {
        setCurrentRoute(raw as NavRoute);
      }
    };

    window.addEventListener('hashchange', handleHashChange);
    return () => window.removeEventListener('hashchange', handleHashChange);
  }, []);

  const handleRouteChange = (route: NavRoute) => {
    setCurrentRoute(route);
    window.location.hash = `#${route}`;
  };

  const handleNavigateToTutorWithPrompt = (prompt: string) => {
    setTutorPrefillPrompt(prompt);
    handleRouteChange('tutor');
  };

  const handleSelectConversationFromDashboard = (conversationId: string) => {
    setTargetConversationId(conversationId);
    handleRouteChange('tutor');
  };

  // Loading state while checking persistent authentication
  if (isLoading) {
    return (
      <div className="min-h-screen bg-[#09090B] flex flex-col items-center justify-center text-white">
        <div className="w-12 h-12 rounded-2xl bg-gradient-to-tr from-[#FF2D8D] to-[#C7F000] p-0.5 flex items-center justify-center mb-4 shadow-glow-pink">
          <div className="w-full h-full bg-[#09090B] rounded-2xl flex items-center justify-center">
            <GraduationCap className="w-6 h-6 text-white" />
          </div>
        </div>
        <div className="flex items-center gap-2 text-[#A1A1AA] text-xs font-mono">
          <Loader2 className="w-4 h-4 animate-spin text-[#C7F000]" />
          <span>Restoring academic session...</span>
        </div>
      </div>
    );
  }

  // Unauthenticated view
  if (!isAuthenticated) {
    if (currentHash === 'login' || currentHash === 'register') {
      return (
        <Suspense fallback={<RouteLoadingFallback />}>
          <AuthPage
            initialMode={currentHash === 'register' ? 'register' : 'login'}
            onNavigateLanding={() => {
              window.location.hash = '';
              setCurrentHash('');
            }}
          />
        </Suspense>
      );
    }

    // Default to the flagship Landing Page
    return (
      <Suspense fallback={<RouteLoadingFallback />}>
        <HomePage
          onGetStarted={() => {
            window.location.hash = '#register';
            setCurrentHash('register');
          }}
          onSignIn={() => {
            window.location.hash = '#login';
            setCurrentHash('login');
          }}
        />
      </Suspense>
    );
  }

  // Authenticated workspace shell
  return (
    <AppShell currentRoute={currentRoute} onRouteChange={handleRouteChange}>
      <Suspense fallback={<RouteLoadingFallback />}>
        {currentRoute === 'dashboard' && (
          <DashboardPage
            onNavigate={(target) => handleRouteChange(target)}
            onSelectConversation={handleSelectConversationFromDashboard}
          />
        )}
        {currentRoute === 'documents' && <DocumentsPage />}
        {currentRoute === 'search' && (
          <SearchPage onNavigateToTutor={handleNavigateToTutorWithPrompt} />
        )}
        {currentRoute === 'tutor' && (
          <TutorPage
            initialPrompt={tutorPrefillPrompt}
            targetConversationId={targetConversationId}
          />
        )}
        {currentRoute === 'learning' && <LearningStudioPage />}
        {currentRoute === 'health' && <SystemHealthPage />}
      </Suspense>
    </AppShell>
  );
};
