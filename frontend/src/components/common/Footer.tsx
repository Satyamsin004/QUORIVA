import React from 'react';

export const Footer: React.FC = () => {
  return (
    <footer className="border-t border-white/10 bg-[#09090B] py-6 mt-auto">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 flex flex-col sm:flex-row items-center justify-between gap-4 text-xs text-[#A1A1AA]">
        <div className="flex items-center gap-2">
          <span className="font-semibold text-white tracking-wider">QUORIVA</span>
          <span>•</span>
          <span>© {new Date().getFullYear()} All rights reserved.</span>
        </div>
        <div className="text-[11px] text-[#A1A1AA]">
          Understand what you study. Your personal AI learning space.
        </div>
      </div>
    </footer>
  );
};
