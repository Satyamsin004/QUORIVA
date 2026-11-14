import React, { useState } from 'react';
import {
  Sparkles,
  Bot,
  Search,
  BrainCircuit,
  FileText,
  Layers,
  BarChart3,
  CheckCircle2,
  ArrowRight,
  Zap,
  BookOpen,
  ChevronRight,
  Cpu,
} from 'lucide-react';

interface HomePageProps {
  onGetStarted?: () => void;
  onSignIn?: () => void;
}

export const HomePage: React.FC<HomePageProps> = ({ onGetStarted, onSignIn }) => {
  const [activePreviewIndex, setActivePreviewIndex] = useState(1);

  const handleStart = () => {
    if (onGetStarted) {
      onGetStarted();
    } else {
      window.location.hash = '#register';
    }
  };

  const handleLogin = () => {
    if (onSignIn) {
      onSignIn();
    } else {
      window.location.hash = '#login';
    }
  };

  const previewCards = [
    {
      id: 0,
      tag: 'WORKSPACE ENGINE',
      title: 'Private Study Workspaces',
      desc: 'Organize course slides, textbooks, and notes with strict data isolation.',
      badge: 'Private & Secure',
      highlight: '#FF2D8D',
      preview: {
        item1: 'Operating Systems (CS301)',
        item2: 'Distributed Systems Textbook',
        item3: '14 Indexed Sections • Workspace Ready',
      },
    },
    {
      id: 1,
      tag: 'AI ACADEMIC TUTOR',
      title: 'Grounded Concept Exploration',
      desc: 'Ask questions and receive verifiable, citation-backed answers derived directly from your materials.',
      badge: 'Exact Citations',
      highlight: '#C7F000',
      preview: {
        item1: 'Q: What is Amdahl\'s Law speedup limit?',
        item2: 'Verified Citation: BCS601-textbook • Page 44',
        item3: 'Grounding Status: 100% VERIFIED',
      },
    },
    {
      id: 2,
      tag: 'HYBRID RETRIEVAL',
      title: 'Dense + Keyword Search',
      desc: 'Combines semantic concepts with precise keyword matching, prioritized for academic accuracy.',
      badge: 'Contextual Reranking',
      highlight: '#FF2D8D',
      preview: {
        item1: 'Dense & Keyword Candidate Fusion',
        item2: 'Contextual Relevance Filtering',
        item3: 'Irrelevant Content Filtered Out',
      },
    },
    {
      id: 3,
      tag: 'LEARNING STUDIO',
      title: 'Automated Quizzes & Flashcards',
      desc: 'Generate multiple-choice problem sets and active-recall decks directly from lecture notes.',
      badge: 'Active Recall & Spaced Repetition',
      highlight: '#C7F000',
      preview: {
        item1: '5 Concept Assessment Questions',
        item2: 'Adaptive Difficulty: Medium',
        item3: 'Next Review Due: Tomorrow',
      },
    },
    {
      id: 4,
      tag: 'MASTERY TELEMETRY',
      title: 'Retention & Progress Analytics',
      desc: 'Track retention curves, quiz attempt scores, and topic mastery across course modules.',
      badge: 'Real-time Metrics',
      highlight: '#FF2D8D',
      preview: {
        item1: 'Average Quiz Score: 88.5%',
        item2: 'Review Cards Graduated: 34',
        item3: 'Daily Learning Streak: Active',
      },
    },
  ];

  const features = [
    {
      icon: Layers,
      title: 'Personal Study Workspaces',
      description: 'Create dedicated environments for each computer science course. Keep course slides, syllabus, and textbook chapters isolated and clean.',
      badge: 'Multi-Workspace',
    },
    {
      icon: FileText,
      title: 'Intelligent Document Ingestion',
      description: 'Automated document processing preserving headings, formulas, and slide boundaries for accurate retrieval.',
      badge: 'PDF • DOCX • PPTX',
    },
    {
      icon: Search,
      title: 'Hybrid Academic Search',
      description: 'Combines semantic understanding with exact keyword matching to deliver only the most relevant textbook passages and notes.',
      badge: 'Semantic & Keyword Search',
    },
    {
      icon: Bot,
      title: 'AI Tutor with Grounded Citations',
      description: 'Conversational academic assistant that quotes directly from your uploaded materials with verifiable page and slide provenance.',
      badge: 'Source-Grounded',
    },
    {
      icon: Sparkles,
      title: 'AI Practice Quiz Generation',
      description: 'Instantly create structured diagnostic quizzes with realistic options, step-by-step technical explanations, and score tracking.',
      badge: 'Active Recall',
    },
    {
      icon: BrainCircuit,
      title: 'Flashcards & Spaced Repetition',
      description: 'Built-in spaced repetition algorithm schedules card reviews at the optimal moment before forgetting occurs.',
      badge: 'Optimal Spaced Intervals',
    },
    {
      icon: BarChart3,
      title: 'Learning Analytics & Mastery',
      description: 'Detailed telemetry tracking attempt history, retention metrics, topic strengths, and review consistency over time.',
      badge: 'Verifiable Data',
    },
  ];

  const steps = [
    {
      step: '01',
      title: 'Upload your academic materials',
      description: 'Drag and drop your lecture slides, textbook PDFs, or course notes into your private study workspace for high-speed automated chunking and vector indexing.',
    },
    {
      step: '02',
      title: 'Ask questions and understand concepts',
      description: 'Interact with the AI Academic Tutor to dissect complex algorithms, derive formulas, and receive grounded answers backed by specific citations.',
    },
    {
      step: '03',
      title: 'Practice, revise and master your curriculum',
      description: 'Generate diagnostic quizzes, review flashcards with spaced repetition, and monitor your topic mastery on your personalized telemetry dashboard.',
    },
  ];

  return (
    <div className="min-h-screen bg-[#09090B] text-[#F5F5F5] font-sans selection:bg-[#FF2D8D] selection:text-white relative overflow-x-hidden">
      {/* Ambient Lighting Gradients */}
      <div className="absolute top-0 left-1/2 -translate-x-1/2 w-[1100px] h-[550px] bg-gradient-to-b from-[#FF2D8D]/15 via-[#C7F000]/10 to-transparent blur-[140px] pointer-events-none -z-10" />
      <div className="absolute top-[800px] right-0 w-[500px] h-[500px] bg-[#FF2D8D]/10 blur-[150px] pointer-events-none -z-10" />
      <div className="absolute top-[1600px] left-0 w-[500px] h-[500px] bg-[#C7F000]/10 blur-[150px] pointer-events-none -z-10" />

      {/* Floating Navigation Header (Inspired by Reference 5) */}
      <header className="sticky top-4 z-50 max-w-6xl mx-auto px-4 sm:px-6">
        <nav className="glass-panel-elevated rounded-full px-5 py-3 flex items-center justify-between border border-white/10 shadow-card-dark">
          {/* Logo */}
          <div className="flex items-center gap-2.5 cursor-pointer" onClick={() => window.scrollTo({ top: 0, behavior: 'smooth' })}>
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

          {/* Nav Links */}
          <div className="hidden md:flex items-center gap-7 text-xs font-medium text-[#A1A1AA]">
            <a href="#features" className="hover:text-white transition-colors">Features</a>
            <a href="#preview" className="hover:text-white transition-colors">Interface Preview</a>
            <a href="#how-it-works" className="hover:text-white transition-colors">How It Works</a>
            <a href="#studio" className="hover:text-white transition-colors">Learning Studio</a>
          </div>

          {/* Action CTAs */}
          <div className="flex items-center gap-3">
            <button
              onClick={handleLogin}
              className="text-xs font-semibold text-[#A1A1AA] hover:text-white px-3 py-1.5 transition-colors"
            >
              Sign In
            </button>
            <button
              onClick={handleStart}
              className="btn-chartreuse rounded-full px-4 sm:px-5 py-2 text-xs font-bold inline-flex items-center gap-1.5"
            >
              <span>Get Started</span>
              <ArrowRight className="w-3.5 h-3.5" />
            </button>
          </div>
        </nav>
      </header>

      {/* Hero Section (Inspired by Reference 2 & 5) */}
      <section className="pt-24 sm:pt-32 pb-20 px-4 sm:px-6 max-w-5xl mx-auto text-center space-y-8 relative">
        {/* Technology Pill Badge */}
        <div className="inline-flex items-center gap-2 px-3.5 py-1.5 rounded-full bg-[#171719] border border-white/10 text-xs font-medium text-[#A1A1AA] shadow-sm hover:border-white/20 transition">
          <span className="w-2 h-2 rounded-full bg-[#C7F000] animate-pulse" />
          <span className="text-white font-semibold">Personal AI Learning Space</span>
          <span className="text-white/30">•</span>
          <span className="text-[#FF2D8D] font-mono text-[11px]">Understand What You Study</span>
        </div>

        {/* Refined Cinematic Editorial Headline */}
        <div className="space-y-4">
          <h1 className="font-editorial text-5xl sm:text-7xl lg:text-8xl font-normal tracking-tight text-white leading-[1.08] max-w-4xl mx-auto">
            Understand more.{' '}
            <span className="italic font-normal bg-gradient-to-r from-white via-[#FF2D8D] to-[#C7F000] bg-clip-text text-transparent">
              Memorize less.
            </span>
          </h1>

          <p className="text-[#A1A1AA] text-sm sm:text-lg max-w-2xl mx-auto leading-relaxed font-normal">
            Bring your academic documents into one workspace. Ask questions, explore concepts, generate practice quizzes and revise with personalized flashcards.
          </p>
        </div>

        {/* Action Buttons */}
        <div className="pt-2 flex flex-col sm:flex-row items-center justify-center gap-4">
          <button
            onClick={handleStart}
            className="btn-chartreuse w-full sm:w-auto rounded-full px-8 py-3.5 text-sm font-bold inline-flex items-center justify-center gap-2 group"
          >
            <span>Get Started Free</span>
            <ArrowRight className="w-4 h-4 group-hover:translate-x-1 transition-transform" />
          </button>

          <a
            href="#preview"
            className="w-full sm:w-auto px-7 py-3.5 rounded-full bg-[#171719] hover:bg-[#202025] border border-white/10 hover:border-white/20 text-sm font-semibold text-white transition inline-flex items-center justify-center gap-2"
          >
            <span>Explore Platform</span>
            <ChevronRight className="w-4 h-4 text-[#A1A1AA]" />
          </a>
        </div>

        {/* Floating AI Orb (Inspired by Reference 1) */}
        <div className="pt-8 flex justify-center">
          <div className="glowing-orb w-24 h-24 sm:w-28 sm:h-28 animate-orb-float cursor-pointer hover:scale-105 transition-transform" />
        </div>
      </section>

      {/* Curved Product Preview Carousel (Directly referencing Screenshot 2 & 5) */}
      <section id="preview" className="py-16 px-4 sm:px-6 max-w-6xl mx-auto space-y-6">
        <div className="text-center space-y-2">
          <div className="inline-flex items-center gap-2 text-[#FF2D8D] text-xs font-bold uppercase tracking-wider">
            <Sparkles className="w-3.5 h-3.5" />
            <span>Interactive Platform Preview</span>
          </div>
          <h2 className="text-2xl sm:text-4xl font-bold tracking-tight text-white">
            Engineered for academic excellence
          </h2>
          <p className="text-xs sm:text-sm text-[#A1A1AA] max-w-lg mx-auto">
            Experience how QUORIVA transforms course materials into verified answers, structured practice, and spaced retention.
          </p>
        </div>

        {/* Preview Selector Pills */}
        <div className="flex items-center justify-center gap-2 flex-wrap pt-2">
          {previewCards.map((card, idx) => (
            <button
              key={card.id}
              onClick={() => setActivePreviewIndex(idx)}
              className={`px-3.5 py-1.5 rounded-full text-xs font-semibold transition-all ${
                activePreviewIndex === idx
                  ? 'bg-white text-[#09090B] shadow-sm'
                  : 'bg-[#171719] text-[#A1A1AA] hover:text-white border border-white/10'
              }`}
            >
              {card.title}
            </button>
          ))}
        </div>

        {/* Active Featured Preview Showcase */}
        {(() => {
          const active = previewCards[activePreviewIndex];
          return (
            <div className="bg-[#171719] border border-white/10 rounded-3xl p-6 sm:p-10 shadow-card-dark relative overflow-hidden group">
              <div className="absolute top-0 right-0 w-80 h-80 bg-[#FF2D8D]/10 rounded-full blur-3xl pointer-events-none" />
              <div className="absolute bottom-0 left-0 w-80 h-80 bg-[#C7F000]/10 rounded-full blur-3xl pointer-events-none" />

              <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 items-center relative z-10">
                {/* Left Description */}
                <div className="lg:col-span-5 space-y-4 text-left">
                  <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-[#202025] border border-white/10 text-xs font-mono font-medium text-[#C7F000]">
                    {active.tag}
                  </div>
                  <h3 className="text-2xl sm:text-3xl font-bold text-white tracking-tight">
                    {active.title}
                  </h3>
                  <p className="text-xs sm:text-sm text-[#A1A1AA] leading-relaxed">
                    {active.desc}
                  </p>

                  <div className="pt-2">
                    <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-semibold bg-[#FF2D8D]/10 text-[#FF2D8D] border border-[#FF2D8D]/20">
                      <Zap className="w-3 h-3" />
                      {active.badge}
                    </span>
                  </div>

                  <div className="pt-4">
                    <button
                      onClick={handleStart}
                      className="btn-chartreuse rounded-full px-5 py-2.5 text-xs font-bold inline-flex items-center gap-1.5"
                    >
                      <span>Try This Feature</span>
                      <ArrowRight className="w-3.5 h-3.5" />
                    </button>
                  </div>
                </div>

                {/* Right Interactive Card Mock */}
                <div className="lg:col-span-7 bg-[#09090B] border border-white/10 rounded-2xl p-6 space-y-4 shadow-2xl">
                  <div className="flex items-center justify-between border-b border-white/10 pb-3">
                    <div className="flex items-center gap-2">
                      <div className="w-3 h-3 rounded-full bg-rose-500/80" />
                      <div className="w-3 h-3 rounded-full bg-amber-500/80" />
                      <div className="w-3 h-3 rounded-full bg-emerald-500/80" />
                      <span className="text-xs font-mono text-[#A1A1AA] ml-2">quoriva://{active.tag.toLowerCase()}</span>
                    </div>
                    <span className="text-[11px] font-mono text-[#C7F000]">Active Session</span>
                  </div>

                  <div className="space-y-3 font-mono text-xs">
                    <div className="p-3.5 rounded-xl bg-[#171719] border border-white/10 text-white flex items-center justify-between">
                      <span>{active.preview.item1}</span>
                      <CheckCircle2 className="w-4 h-4 text-[#C7F000]" />
                    </div>
                    <div className="p-3.5 rounded-xl bg-[#171719] border border-white/10 text-[#A1A1AA]">
                      {active.preview.item2}
                    </div>
                    <div className="p-3.5 rounded-xl bg-[#202025] border border-white/10 text-[#FF2D8D] flex items-center gap-2">
                      <Sparkles className="w-4 h-4" />
                      <span>{active.preview.item3}</span>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          );
        })()}
      </section>

      {/* Features Section */}
      <section id="features" className="py-20 px-4 sm:px-6 max-w-6xl mx-auto space-y-12">
        <div className="text-center space-y-3">
          <div className="inline-flex items-center gap-2 text-[#C7F000] text-xs font-bold uppercase tracking-wider">
            <Cpu className="w-4 h-4" />
            <span>Core Capabilities</span>
          </div>
          <h2 className="text-3xl sm:text-5xl font-bold tracking-tight text-white">
            Built from scratch for rigorous academic study
          </h2>
          <p className="text-xs sm:text-sm text-[#A1A1AA] max-w-xl mx-auto">
            Every feature is purpose-built to preserve source integrity, eliminate hallucinations, and reinforce technical mastery.
          </p>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {features.map((feat, idx) => {
            const Icon = feat.icon;
            return (
              <div
                key={idx}
                className="bg-[#171719] border border-white/10 hover:border-white/20 rounded-2xl p-6 space-y-4 hover:translate-y-[-2px] transition shadow-card-dark relative group"
              >
                <div className="flex items-center justify-between">
                  <div className="w-10 h-10 rounded-xl bg-[#202025] border border-white/10 flex items-center justify-center text-[#FF2D8D] group-hover:scale-110 transition-transform">
                    <Icon className="w-5 h-5" />
                  </div>
                  <span className="text-[11px] font-mono text-[#A1A1AA] bg-[#202025] px-2.5 py-1 rounded-full border border-white/5">
                    {feat.badge}
                  </span>
                </div>

                <div className="space-y-1.5">
                  <h3 className="text-base font-semibold text-white tracking-tight">
                    {feat.title}
                  </h3>
                  <p className="text-xs text-[#A1A1AA] leading-relaxed">
                    {feat.description}
                  </p>
                </div>
              </div>
            );
          })}
        </div>
      </section>

      {/* How It Works (3 Steps) */}
      <section id="how-it-works" className="py-20 px-4 sm:px-6 max-w-6xl mx-auto border-t border-white/10 space-y-12">
        <div className="text-center space-y-3">
          <div className="inline-flex items-center gap-2 text-[#FF2D8D] text-xs font-bold uppercase tracking-wider">
            <BookOpen className="w-4 h-4" />
            <span>Workflow</span>
          </div>
          <h2 className="text-3xl sm:text-5xl font-bold tracking-tight text-white">
            Three simple steps to mastery
          </h2>
          <p className="text-xs sm:text-sm text-[#A1A1AA] max-w-md mx-auto">
            From lecture slides to verified exam readiness in minutes.
          </p>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-3 gap-8">
          {steps.map((s, idx) => (
            <div
              key={idx}
              className="bg-[#171719] border border-white/10 rounded-2xl p-6 sm:p-8 space-y-4 relative shadow-card-dark"
            >
              <div className="text-3xl font-extrabold font-mono text-[#C7F000]">
                {s.step}
              </div>
              <h3 className="text-lg font-bold text-white tracking-tight">
                {s.title}
              </h3>
              <p className="text-xs text-[#A1A1AA] leading-relaxed">
                {s.description}
              </p>
            </div>
          ))}
        </div>
      </section>

      {/* Final Call to Action Banner */}
      <section className="py-20 px-4 sm:px-6 max-w-5xl mx-auto text-center space-y-6">
        <div className="bg-gradient-to-b from-[#171719] to-[#09090B] border border-white/10 rounded-3xl p-10 sm:p-16 space-y-6 shadow-glow-subtle relative overflow-hidden">
          <div className="space-y-3">
            <h2 className="font-editorial text-4xl sm:text-6xl text-white font-normal tracking-tight">
              Your next learning breakthrough{' '}
              <span className="italic text-[#C7F000]">starts here.</span>
            </h2>
            <p className="text-xs sm:text-sm text-[#A1A1AA] max-w-md mx-auto">
              Join students mastering complex Computer Science concepts with privacy-first grounded AI.
            </p>
          </div>

          <div className="pt-2 flex flex-col sm:flex-row items-center justify-center gap-4">
            <button
              onClick={handleStart}
              className="btn-chartreuse rounded-full px-8 py-3.5 text-sm font-bold inline-flex items-center gap-2"
            >
              <span>Get Started Now</span>
              <ArrowRight className="w-4 h-4" />
            </button>
            <button
              onClick={handleLogin}
              className="px-6 py-3.5 rounded-full bg-[#202025] hover:bg-[#2A2A32] border border-white/10 text-xs font-semibold text-white transition"
            >
              Sign In to Your Workspace
            </button>
          </div>
        </div>
      </section>

      {/* Sleek Dark Footer */}
      <footer className="border-t border-white/10 py-12 px-4 sm:px-6 max-w-6xl mx-auto text-xs text-[#A1A1AA] flex flex-col sm:flex-row items-center justify-between gap-4">
        <div className="flex items-center gap-2">
          <div className="w-6 h-6 rounded-lg bg-gradient-to-tr from-[#FF2D8D] to-[#C7F000] p-0.5 flex items-center justify-center">
            <div className="w-full h-full bg-[#09090B] rounded-[6px] flex items-center justify-center font-bold text-[11px] text-white">
              Q
            </div>
          </div>
          <span className="font-semibold text-white">QUORIVA</span>
          <span>© {new Date().getFullYear()} All rights reserved.</span>
        </div>

        <div className="flex items-center gap-6">
          <a href="#features" className="hover:text-white transition-colors">Features</a>
          <a href="#how-it-works" className="hover:text-white transition-colors">Workflow</a>
          <span className="text-[#A1A1AA] text-xs">Understand what you study.</span>
        </div>
      </footer>
    </div>
  );
};
