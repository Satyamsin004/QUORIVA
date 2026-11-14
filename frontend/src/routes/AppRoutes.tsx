import React, { useState, useEffect } from 'react';
import { useAuth } from '@/context/AuthContext';
import { AuthPage } from '@/pages/AuthPage';
import { HomePage } from '@/pages/HomePage';
import { DashboardPage } from '@/pages/DashboardPage';
import { DocumentsPage } from '@/pages/DocumentsPage';
import { SearchPage } from '@/pages/SearchPage';
import { TutorPage } from '@/pages/TutorPage';
import { LearningStudioPage } from '@/pages/LearningStudioPage';
import { SystemHealthPage } from '@/pages/SystemHealthPage';
import { AppShell, NavRoute } from '@/components/layout/AppShell';
import { Loader2, GraduationCap } from 'lucide-react';

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
        <AuthPage
          initialMode={currentHash === 'register' ? 'register' : 'login'}
          onNavigateLanding={() => {
            window.location.hash = '';
            setCurrentHash('');
          }}
        />
      );
    }

    // Default to the flagship Landing Page
    return (
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
    );
  }

  // Authenticated workspace shell
  return (
    <AppShell currentRoute={currentRoute} onRouteChange={handleRouteChange}>
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
    </AppShell>
  );
};
