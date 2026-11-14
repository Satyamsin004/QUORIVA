import React from 'react';
import { GraduationCap, Sparkles } from 'lucide-react';
import { APP_CONFIG } from '@/utils/constants';

export const Header: React.FC = () => {
  return (
    <header className="border-b border-slate-200 bg-white sticky top-0 z-50">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 h-16 flex items-center justify-between">
        <div className="flex items-center gap-3">
          <div className="w-10 h-10 rounded-xl bg-teal-600 flex items-center justify-center text-white shadow-md shadow-teal-500/20">
            <GraduationCap className="w-6 h-6" />
          </div>
          <div>
            <div className="flex items-center gap-2">
              <span className="font-bold text-lg text-slate-900 tracking-tight">
                {APP_CONFIG.name}
              </span>
              <span className="text-[10px] font-semibold uppercase tracking-wider px-2 py-0.5 bg-emerald-50 text-emerald-700 rounded-md border border-emerald-200">
                AI Learning
              </span>
            </div>
            <p className="text-xs text-slate-500 hidden sm:block">
              {APP_CONFIG.tagline}
            </p>
          </div>
        </div>
        <div className="flex items-center gap-4">
          <nav className="flex items-center gap-1 bg-slate-100 p-1 rounded-xl border border-slate-200">
            <a
              href="#health"
              className="px-3 py-1 text-xs font-medium rounded-lg text-slate-700 hover:text-slate-900 transition"
            >
              System Health
            </a>
            <a
              href="#tutor"
              className="px-3 py-1 text-xs font-semibold rounded-lg bg-indigo-600 text-white shadow-sm transition"
            >
              AI Academic Tutor
            </a>
          </nav>
          <div className="flex items-center gap-2 text-xs text-slate-500 font-mono bg-slate-100 px-3 py-1.5 rounded-lg border border-slate-200">
            <Sparkles className="w-3.5 h-3.5 text-emerald-600" />
            <span>AI Studio</span>
          </div>
        </div>
      </div>
    </header>
  );
};

