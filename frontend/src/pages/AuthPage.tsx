import React, { useState } from 'react';
import { useAuth } from '@/context/AuthContext';
import {
  Mail,
  Lock,
  User,
  ArrowRight,
  Loader2,
  AlertCircle,
  Eye,
  EyeOff,
  CheckCircle2,
  ShieldCheck,
} from 'lucide-react';

interface AuthPageProps {
  initialMode?: 'login' | 'register';
  onNavigateLanding?: () => void;
}

export const AuthPage: React.FC<AuthPageProps> = ({ initialMode = 'login', onNavigateLanding }) => {
  const { login, register } = useAuth();

  const [mode, setMode] = useState<'login' | 'register'>(initialMode);
  const [fullName, setFullName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [rememberMe, setRememberMe] = useState(true);
  const [isLoading, setIsLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMessage(null);

    if (!email || !password) {
      setErrorMessage('Please provide your email and password.');
      return;
    }

    if (password.length < 8) {
      setErrorMessage('Password must be at least 8 characters in length.');
      return;
    }

    if (mode === 'register') {
      if (!fullName.trim()) {
        setErrorMessage('Full name is required.');
        return;
      }
      if (password !== confirmPassword) {
        setErrorMessage('Passwords do not match.');
        return;
      }
    }

    setIsLoading(true);

    try {
      if (mode === 'login') {
        await login({ email: email.trim(), password });
      } else {
        await register({ fullName: fullName.trim(), email: email.trim(), password });
      }
    } catch (err: unknown) {
      console.error('Authentication failure:', err);
      if (err instanceof Error) {
        setErrorMessage(err.message);
      } else {
        setErrorMessage(
          mode === 'login'
            ? 'Invalid email or password. Please verify your credentials.'
            : 'Registration failed. A student account with this email may already exist.'
        );
      }
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-[#09090B] text-[#F5F5F5] font-sans flex items-center justify-center p-4 sm:p-6 lg:p-8 relative overflow-hidden">
      {/* Ambient Lighting Gradients */}
      <div className="absolute top-1/4 -left-20 w-[500px] h-[500px] bg-[#FF2D8D]/15 rounded-full blur-[140px] pointer-events-none -z-10" />
      <div className="absolute bottom-1/4 -right-20 w-[500px] h-[500px] bg-[#C7F000]/15 rounded-full blur-[140px] pointer-events-none -z-10" />

      {/* Main Glassmorphism Auth Card */}
      <div className="w-full max-w-4xl grid grid-cols-1 lg:grid-cols-12 rounded-3xl overflow-hidden glass-panel-elevated border border-white/10 shadow-card-dark relative z-10">

        {/* Left Side: Editorial Brand Showcase (5 cols) */}
        <div className="lg:col-span-5 p-8 sm:p-10 bg-[#111114]/90 border-b lg:border-b-0 lg:border-r border-white/10 flex flex-col justify-between space-y-8">
          <div>
            {/* Logo */}
            <div
              className="flex items-center gap-2.5 cursor-pointer group"
              onClick={onNavigateLanding || (() => (window.location.hash = '#'))}
            >
              <div className="w-9 h-9 rounded-xl bg-gradient-to-tr from-[#FF2D8D] to-[#C7F000] p-0.5 flex items-center justify-center shadow-glow-pink">
                <div className="w-full h-full bg-[#09090B] rounded-[10px] flex items-center justify-center font-bold text-base text-white">
                  Q
                </div>
              </div>
              <div className="flex items-baseline gap-1.5">
                <span className="font-bold text-sm tracking-tight text-white">QUORIVA</span>
                <span className="text-[10px] font-extrabold text-[#09090B] bg-[#C7F000] px-1.5 py-0.2 rounded font-mono">AI</span>
              </div>
            </div>

            <div className="mt-10 space-y-3">
              <h2 className="font-editorial text-3xl sm:text-4xl text-white font-normal tracking-tight leading-tight">
                Understand what{' '}
                <span className="italic text-[#C7F000]">you study.</span>
              </h2>
              <p className="text-xs text-[#A1A1AA] leading-relaxed">
                Bring your lecture slides, textbooks, and notes into an isolated, source-grounded personal AI workspace.
              </p>
            </div>

            <div className="mt-8 space-y-3 text-xs text-[#A1A1AA]">
              <div className="flex items-center gap-2.5">
                <CheckCircle2 className="w-4 h-4 text-[#C7F000] shrink-0" />
                <span>Source-grounded answers with verified document citations</span>
              </div>
              <div className="flex items-center gap-2.5">
                <CheckCircle2 className="w-4 h-4 text-[#FF2D8D] shrink-0" />
                <span>Spaced repetition active-recall flashcard decks</span>
              </div>
              <div className="flex items-center gap-2.5">
                <CheckCircle2 className="w-4 h-4 text-[#C7F000] shrink-0" />
                <span>Personalized diagnostic practice quizzes</span>
              </div>
            </div>
          </div>

          <div className="pt-6 border-t border-white/5 flex items-center justify-between text-[11px] text-[#A1A1AA]">
            <span className="flex items-center gap-1.5">
              <ShieldCheck className="w-3.5 h-3.5 text-[#C7F000]" />
              HMAC Secured Session
            </span>
            <button
              onClick={onNavigateLanding || (() => (window.location.hash = '#'))}
              className="text-[#FF2D8D] hover:underline"
            >
              Back to Overview
            </button>
          </div>
        </div>

        {/* Right Side: Form (7 cols) */}
        <div className="lg:col-span-7 p-8 sm:p-10 flex flex-col justify-center space-y-6">
          {/* Header Switcher */}
          <div className="flex items-center justify-between">
            <div>
              <h3 className="text-xl font-bold text-white tracking-tight">
                {mode === 'login' ? 'Welcome back' : 'Create your account'}
              </h3>
              <p className="text-xs text-[#A1A1AA] mt-0.5">
                {mode === 'login'
                  ? 'Access your personal study workspaces and active discussions.'
                  : 'Get started with intelligent academic retrieval in seconds.'}
              </p>
            </div>

            {/* Mode Toggle Pills */}
            <div className="p-1 rounded-xl bg-[#171719] border border-white/10 flex items-center gap-1">
              <button
                type="button"
                onClick={() => {
                  setMode('login');
                  setErrorMessage(null);
                }}
                className={`px-3 py-1 text-xs font-semibold rounded-lg transition-all ${mode === 'login'
                    ? 'bg-[#FF2D8D] text-white shadow-glow-pink'
                    : 'text-[#A1A1AA] hover:text-white'
                  }`}
              >
                Sign In
              </button>
              <button
                type="button"
                onClick={() => {
                  setMode('register');
                  setErrorMessage(null);
                }}
                className={`px-3 py-1 text-xs font-semibold rounded-lg transition-all ${mode === 'register'
                    ? 'bg-[#FF2D8D] text-white shadow-glow-pink'
                    : 'text-[#A1A1AA] hover:text-white'
                  }`}
              >
                Sign Up
              </button>
            </div>
          </div>

          {/* Error Banner */}
          {errorMessage && (
            <div className="p-3.5 rounded-xl bg-rose-500/10 border border-rose-500/20 text-xs text-rose-300 flex items-center gap-2.5">
              <AlertCircle className="w-4 h-4 shrink-0 text-rose-400" />
              <span>{errorMessage}</span>
            </div>
          )}

          {/* Form */}
          <form onSubmit={handleSubmit} className="space-y-4 text-xs">
            {mode === 'register' && (
              <div className="space-y-1.5">
                <label className="text-xs font-medium text-[#F5F5F5]">Full Name</label>
                <div className="relative">
                  <User className="w-4 h-4 text-[#A1A1AA] absolute left-3.5 top-3.5" />
                  <input
                    type="text"
                    required
                    value={fullName}
                    onChange={(e) => setFullName(e.target.value)}
                    placeholder="e.g. Satyam Kumar"
                    className="w-full pl-10 pr-4 py-3 rounded-xl bg-[#171719] border border-white/10 text-white placeholder-[#A1A1AA]/50 focus:outline-none focus:border-[#FF2D8D] focus:ring-1 focus:ring-[#FF2D8D] transition text-xs"
                  />
                </div>
              </div>
            )}

            <div className="space-y-1.5">
              <label className="text-xs font-medium text-[#F5F5F5]">Email Address</label>
              <div className="relative">
                <Mail className="w-4 h-4 text-[#A1A1AA] absolute left-3.5 top-3.5" />
                <input
                  type="email"
                  required
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  placeholder="student@university.edu"
                  className="w-full pl-10 pr-4 py-3 rounded-xl bg-[#171719] border border-white/10 text-white placeholder-[#A1A1AA]/50 focus:outline-none focus:border-[#FF2D8D] focus:ring-1 focus:ring-[#FF2D8D] transition text-xs"
                />
              </div>
            </div>

            <div className="space-y-1.5">
              <label className="text-xs font-medium text-[#F5F5F5]">Password</label>
              <div className="relative">
                <Lock className="w-4 h-4 text-[#A1A1AA] absolute left-3.5 top-3.5" />
                <input
                  type={showPassword ? 'text' : 'password'}
                  required
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  placeholder="At least 8 characters"
                  className="w-full pl-10 pr-10 py-3 rounded-xl bg-[#171719] border border-white/10 text-white placeholder-[#A1A1AA]/50 focus:outline-none focus:border-[#FF2D8D] focus:ring-1 focus:ring-[#FF2D8D] transition text-xs"
                />
                <button
                  type="button"
                  onClick={() => setShowPassword(!showPassword)}
                  className="absolute right-3.5 top-3.5 text-[#A1A1AA] hover:text-white"
                >
                  {showPassword ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                </button>
              </div>
            </div>

            {mode === 'register' && (
              <div className="space-y-1.5">
                <label className="text-xs font-medium text-[#F5F5F5]">Confirm Password</label>
                <div className="relative">
                  <Lock className="w-4 h-4 text-[#A1A1AA] absolute left-3.5 top-3.5" />
                  <input
                    type={showPassword ? 'text' : 'password'}
                    required
                    value={confirmPassword}
                    onChange={(e) => setConfirmPassword(e.target.value)}
                    placeholder="Re-enter your password"
                    className="w-full pl-10 pr-4 py-3 rounded-xl bg-[#171719] border border-white/10 text-white placeholder-[#A1A1AA]/50 focus:outline-none focus:border-[#FF2D8D] focus:ring-1 focus:ring-[#FF2D8D] transition text-xs"
                  />
                </div>
              </div>
            )}

            {mode === 'login' && (
              <div className="flex items-center justify-between pt-1 text-xs">
                <label className="flex items-center gap-2 cursor-pointer text-[#A1A1AA] hover:text-white">
                  <input
                    type="checkbox"
                    checked={rememberMe}
                    onChange={(e) => setRememberMe(e.target.checked)}
                    className="w-3.5 h-3.5 rounded bg-[#171719] border-white/20 text-[#FF2D8D] focus:ring-0 focus:ring-offset-0"
                  />
                  <span>Remember me for 7 days</span>
                </label>
              </div>
            )}

            <div className="pt-3">
              <button
                type="submit"
                disabled={isLoading}
                className="btn-chartreuse w-full py-3.5 rounded-xl font-bold text-xs inline-flex items-center justify-center gap-2 disabled:opacity-50"
              >
                {isLoading ? (
                  <Loader2 className="w-4 h-4 animate-spin text-[#09090B]" />
                ) : (
                  <>
                    <span>{mode === 'login' ? 'Sign In to Workspace' : 'Create Student Account'}</span>
                    <ArrowRight className="w-4 h-4 text-[#09090B]" />
                  </>
                )}
              </button>
            </div>
          </form>

          {/* Mode Switcher Footer */}
          <div className="text-center pt-2 text-xs text-[#A1A1AA]">
            {mode === 'login' ? (
              <span>
                Don&apos;t have an account yet?{' '}
                <button
                  type="button"
                  onClick={() => {
                    setMode('register');
                    setErrorMessage(null);
                  }}
                  className="text-[#FF2D8D] font-semibold hover:underline"
                >
                  Sign up free
                </button>
              </span>
            ) : (
              <span>
                Already have an account?{' '}
                <button
                  type="button"
                  onClick={() => {
                    setMode('login');
                    setErrorMessage(null);
                  }}
                  className="text-[#FF2D8D] font-semibold hover:underline"
                >
                  Sign in here
                </button>
              </span>
            )}
          </div>
        </div>
      </div>
    </div>
  );
};
