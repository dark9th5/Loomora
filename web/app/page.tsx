'use client';

import React from 'react';
import Link from 'next/link';
import { useTheme } from '@/components/ThemeContext';
import {
  Mic,
  ShieldCheck,
  Zap,
  Sliders,
  Sparkles,
  Download,
  Lock,
  CheckCircle,
  HelpCircle,
  ArrowRight,
  FileAudio,
  Activity,
  Layers,
} from 'lucide-react';
import { env } from '@/lib/env';

export default function HomePage() {
  const { t } = useTheme();

  return (
    <div className="space-y-24 pb-20">
      {/* 1. HERO SECTION */}
      <section className="relative pt-16 pb-20 overflow-hidden">
        <div className="absolute top-1/4 left-1/2 -translate-x-1/2 -translate-y-1/2 w-[600px] h-[600px] bg-loomora-primary/20 rounded-full blur-[140px] pointer-events-none" />

        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 relative z-10 text-center space-y-8">
          <div className="inline-flex items-center space-x-2 px-4 py-2 rounded-full glass border border-loomora-primary/30 text-xs font-semibold text-loomora-container shadow-inner">
            <Sparkles className="w-3.5 h-3.5 text-loomora-secondary" />
            <span>{t.hero.tagline}</span>
          </div>

          <h1 className="text-4xl sm:text-6xl lg:text-7xl font-extrabold tracking-tight text-white light:text-slate-900 leading-[1.1]">
            Voice Recording Made <br />
            <span className="text-transparent bg-clip-text bg-gradient-to-r from-loomora-secondary via-loomora-container to-loomora-primary">
              Private, Intelligent &amp; Reliable
            </span>
          </h1>

          <p className="max-w-3xl mx-auto text-lg sm:text-xl text-slate-300 light:text-slate-600 font-normal leading-relaxed">
            {t.hero.subtitle}
          </p>

          <div className="flex flex-col sm:flex-row items-center justify-center gap-4 pt-4">
            <Link
              href="/download"
              className="w-full sm:w-auto inline-flex items-center justify-center space-x-2 px-8 py-4 rounded-full bg-loomora-primary text-white font-bold text-base hover:bg-loomora-primary/90 transition-all shadow-xl shadow-loomora-primary/30"
            >
              <Download className="w-5 h-5" />
              <span>{t.hero.downloadBtn}</span>
            </Link>

            <Link
              href="/pro"
              className="w-full sm:w-auto inline-flex items-center justify-center space-x-2 px-8 py-4 rounded-full glass text-white light:text-slate-800 font-bold text-base hover:bg-white/10 light:hover:bg-black/5 transition-all"
            >
              <ShieldCheck className="w-5 h-5 text-loomora-secondary" />
              <span>{t.pricing.buyCta}</span>
            </Link>
          </div>

          <p className="text-xs text-slate-400 light:text-slate-500 font-medium flex items-center justify-center space-x-1.5">
            <Lock className="w-3.5 h-3.5 text-loomora-secondary" />
            <span>{t.hero.privacyNote}</span>
          </p>
        </div>
      </section>

      {/* 2. PRODUCT MOCKUP SCREENSHOTS SECTION */}
      <section className="max-w-6xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="glass rounded-3xl p-4 sm:p-8 border border-white/10 shadow-2xl relative overflow-hidden">
          <div className="flex items-center space-x-2 mb-6 border-b border-white/10 pb-4">
            <div className="w-3 h-3 rounded-full bg-red-500/80" />
            <div className="w-3 h-3 rounded-full bg-yellow-500/80" />
            <div className="w-3 h-3 rounded-full bg-green-500/80" />
            <span className="text-xs font-mono text-slate-400 ml-2">Loomora Android App — Compose Material 3 Shell</span>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
            {/* Screen 1: Active Recorder */}
            <div className="bg-slate-900 light:bg-slate-100 rounded-2xl p-6 border border-white/10 flex flex-col justify-between h-[420px]">
              <div>
                <div className="flex items-center justify-between text-xs text-slate-400 mb-4">
                  <span className="font-semibold text-loomora-secondary">RECORDER</span>
                  <span className="bg-red-500/20 text-red-400 px-2 py-0.5 rounded-full font-bold">● REC 02:45</span>
                </div>
                <h3 className="text-lg font-bold text-white light:text-slate-900">Meeting Strategy Session</h3>
                <p className="text-xs text-slate-400 mt-1">44.1kHz • 128kbps stereo • AAC/M4A</p>

                {/* Simulated Live Waveform */}
                <div className="mt-8 flex items-center justify-center space-x-1.5 h-24 bg-slate-950/60 rounded-xl p-4">
                  {[30, 60, 90, 45, 80, 100, 70, 40, 85, 95, 60, 35, 75, 90, 50].map((h, idx) => (
                    <div
                      key={idx}
                      className="w-1.5 bg-loomora-primary rounded-full animate-pulse"
                      style={{ height: `${h}%`, animationDelay: `${idx * 0.1}s` }}
                    />
                  ))}
                </div>
              </div>

              <div className="flex items-center justify-around pt-4 border-t border-white/10">
                <div className="w-10 h-10 rounded-full bg-slate-800 flex items-center justify-center text-slate-300">
                  <Activity className="w-5 h-5" />
                </div>
                <div className="w-14 h-14 rounded-full bg-red-600 flex items-center justify-center text-white shadow-lg shadow-red-600/40">
                  <Mic className="w-7 h-7" />
                </div>
                <div className="w-10 h-10 rounded-full bg-slate-800 flex items-center justify-center text-slate-300">
                  <Layers className="w-5 h-5" />
                </div>
              </div>
            </div>

            {/* Screen 2: Non-Destructive Audio Editor */}
            <div className="bg-slate-900 light:bg-slate-100 rounded-2xl p-6 border border-white/10 flex flex-col justify-between h-[420px]">
              <div>
                <div className="flex items-center justify-between text-xs text-slate-400 mb-4">
                  <span className="font-semibold text-loomora-secondary">AUDIO EDITOR</span>
                  <span className="bg-loomora-primary/20 text-loomora-container px-2 py-0.5 rounded-full font-bold">NON-DESTRUCTIVE</span>
                </div>
                <h3 className="text-lg font-bold text-white light:text-slate-900">Trim &amp; Speech Clarity</h3>
                <p className="text-xs text-slate-400 mt-1">Preserves original raw audio file intact</p>

                <div className="mt-6 space-y-4">
                  <div className="bg-slate-950/60 p-3 rounded-xl border border-white/5 space-y-2">
                    <div className="flex justify-between text-xs text-slate-400">
                      <span>Selection Start</span>
                      <span className="font-mono text-white">00:15.00</span>
                    </div>
                    <div className="flex justify-between text-xs text-slate-400">
                      <span>Selection End</span>
                      <span className="font-mono text-white">01:30.00</span>
                    </div>
                  </div>

                  <div className="flex items-center justify-between bg-loomora-primary/10 p-3 rounded-xl border border-loomora-primary/20">
                    <span className="text-xs font-semibold text-white">Speech Clarity Filter</span>
                    <div className="w-8 h-4 bg-loomora-primary rounded-full flex items-center justify-end px-0.5">
                      <div className="w-3 h-3 bg-white rounded-full" />
                    </div>
                  </div>
                </div>
              </div>

              <div className="pt-4 border-t border-white/10 flex space-x-2">
                <button className="flex-1 py-2 rounded-xl bg-loomora-primary text-white text-xs font-bold">Export Edited File</button>
              </div>
            </div>

            {/* Screen 3: AI Smart Insights */}
            <div className="bg-slate-900 light:bg-slate-100 rounded-2xl p-6 border border-white/10 flex flex-col justify-between h-[420px]">
              <div>
                <div className="flex items-center justify-between text-xs text-slate-400 mb-4">
                  <span className="font-semibold text-loomora-secondary">SMART INSIGHTS</span>
                  <span className="bg-green-500/20 text-green-400 px-2 py-0.5 rounded-full font-bold">READY</span>
                </div>
                <h3 className="text-lg font-bold text-white light:text-slate-900">Structured Notes &amp; Tasks</h3>
                <p className="text-xs text-slate-400 mt-1">Evidence-linked transcript analysis</p>

                <div className="mt-6 space-y-3">
                  <div className="bg-slate-950/60 p-3 rounded-xl border border-white/5">
                    <span className="text-[10px] font-bold uppercase text-loomora-secondary">Key Decisions</span>
                    <p className="text-xs text-slate-300 mt-1">Approved product architecture for Q3 release.</p>
                  </div>
                  <div className="bg-slate-950/60 p-3 rounded-xl border border-white/5">
                    <span className="text-[10px] font-bold uppercase text-loomora-secondary">Action Item</span>
                    <p className="text-xs text-slate-300 mt-1">Finalize security compliance report by Friday.</p>
                  </div>
                </div>
              </div>

              <div className="pt-4 border-t border-white/10 flex justify-between items-center text-xs text-slate-400">
                <span>3 Free Trial Uses Left</span>
                <span className="text-loomora-secondary font-bold">View Pro</span>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* 3. CORE FEATURES GRID */}
      <section className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 space-y-12">
        <div className="text-center space-y-4">
          <h2 className="text-3xl sm:text-5xl font-bold text-white light:text-slate-900">
            {t.features.title}
          </h2>
          <p className="max-w-2xl mx-auto text-slate-400 light:text-slate-600 text-base sm:text-lg">
            {t.features.subtitle}
          </p>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-3 gap-8">
          <div className="glass p-8 rounded-3xl border border-white/10 space-y-4 hover:border-loomora-primary/50 transition-all">
            <div className="w-12 h-12 rounded-2xl bg-loomora-primary/20 flex items-center justify-center text-loomora-secondary">
              <Lock className="w-6 h-6" />
            </div>
            <h3 className="text-xl font-bold text-white light:text-slate-900">{t.features.localTitle}</h3>
            <p className="text-sm text-slate-400 light:text-slate-600 leading-relaxed">{t.features.localDesc}</p>
          </div>

          <div className="glass p-8 rounded-3xl border border-white/10 space-y-4 hover:border-loomora-primary/50 transition-all">
            <div className="w-12 h-12 rounded-2xl bg-loomora-primary/20 flex items-center justify-center text-loomora-secondary">
              <Sliders className="w-6 h-6" />
            </div>
            <h3 className="text-xl font-bold text-white light:text-slate-900">{t.features.editorTitle}</h3>
            <p className="text-sm text-slate-400 light:text-slate-600 leading-relaxed">{t.features.editorDesc}</p>
          </div>

          <div className="glass p-8 rounded-3xl border border-white/10 space-y-4 hover:border-loomora-primary/50 transition-all">
            <div className="w-12 h-12 rounded-2xl bg-loomora-primary/20 flex items-center justify-center text-loomora-secondary">
              <Sparkles className="w-6 h-6" />
            </div>
            <h3 className="text-xl font-bold text-white light:text-slate-900">{t.features.aiTitle}</h3>
            <p className="text-sm text-slate-400 light:text-slate-600 leading-relaxed">{t.features.aiDesc}</p>
          </div>
        </div>
      </section>

      {/* 4. WHY LOOMORA (PRIVACY & RELIABILITY) */}
      <section className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="bg-gradient-to-r from-loomora-primary/20 via-slate-900 to-slate-900 p-8 sm:p-12 rounded-3xl border border-loomora-primary/30 space-y-8">
          <div className="max-w-3xl space-y-4">
            <span className="text-xs font-bold uppercase tracking-wider text-loomora-secondary">Why Choose Loomora</span>
            <h2 className="text-3xl sm:text-4xl font-bold text-white">
              No Cloud Lock-In. No Paywalls for Your Own Recordings.
            </h2>
            <p className="text-slate-300 leading-relaxed text-base">
              Unlike generic cloud recording services that hide your audio behind subscription walls or silently train models on your personal notes, Loomora is engineered with strict local-first principles. Your audio belongs to you forever.
            </p>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6 pt-4">
            <div className="space-y-2">
              <CheckCircle className="w-5 h-5 text-loomora-secondary" />
              <h4 className="font-bold text-white text-base">Zero Login Required</h4>
              <p className="text-xs text-slate-400">Record and manage audio without creating an account.</p>
            </div>
            <div className="space-y-2">
              <CheckCircle className="w-5 h-5 text-loomora-secondary" />
              <h4 className="font-bold text-white text-base">Foreground Protection</h4>
              <p className="text-xs text-slate-400">Foreground service prevents system kills during long meetings.</p>
            </div>
            <div className="space-y-2">
              <CheckCircle className="w-5 h-5 text-loomora-secondary" />
              <h4 className="font-bold text-white text-base">Idempotent Accounting</h4>
              <p className="text-xs text-slate-400">Trial uses are consumed strictly after successful AI processing.</p>
            </div>
            <div className="space-y-2">
              <CheckCircle className="w-5 h-5 text-loomora-secondary" />
              <h4 className="font-bold text-white text-base">Path Traversal Safe</h4>
              <p className="text-xs text-slate-400">Strict canonical file bounds protect private app data.</p>
            </div>
          </div>
        </div>
      </section>

      {/* 5. PRICING & COMPARISON */}
      <section className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 space-y-12">
        <div className="text-center space-y-4">
          <h2 className="text-3xl sm:text-5xl font-bold text-white light:text-slate-900">{t.pricing.title}</h2>
          <p className="text-slate-400 light:text-slate-600 text-base">Transparent capabilities with no hidden clauses.</p>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-3 gap-8">
          {/* Free Plan */}
          <div className="glass p-8 rounded-3xl border border-white/10 flex flex-col justify-between">
            <div className="space-y-6">
              <div>
                <h3 className="text-xl font-bold text-white light:text-slate-900">{t.pricing.freePlan}</h3>
                <p className="text-xs text-slate-400 mt-1">{t.pricing.freeDesc}</p>
              </div>
              <div className="text-3xl font-extrabold text-white light:text-slate-900">$0 <span className="text-sm font-normal text-slate-400">/ forever</span></div>
              <ul className="space-y-3 text-xs text-slate-300">
                <li className="flex items-center space-x-2"><CheckCircle className="w-4 h-4 text-loomora-secondary" /><span>Unlimited Local Recording</span></li>
                <li className="flex items-center space-x-2"><CheckCircle className="w-4 h-4 text-loomora-secondary" /><span>Unlimited Local Playback &amp; Search</span></li>
                <li className="flex items-center space-x-2"><CheckCircle className="w-4 h-4 text-loomora-secondary" /><span>Non-Destructive Audio Editor</span></li>
                <li className="flex items-center space-x-2"><CheckCircle className="w-4 h-4 text-loomora-secondary" /><span>Dual English &amp; Vietnamese Support</span></li>
              </ul>
            </div>
            <Link href="/download" className="mt-8 block text-center py-3 rounded-full border border-white/20 text-white font-semibold text-xs hover:bg-white/10 transition-colors">
              Download Free App
            </Link>
          </div>

          {/* Trial Plan */}
          <div className="glass p-8 rounded-3xl border border-loomora-primary/40 flex flex-col justify-between relative">
            <div className="space-y-6">
              <div>
                <span className="text-[10px] font-bold uppercase tracking-wider bg-loomora-primary/30 text-loomora-container px-2.5 py-1 rounded-full">Included In App</span>
                <h3 className="text-xl font-bold text-white light:text-slate-900 mt-2">{t.pricing.trialPlan}</h3>
                <p className="text-xs text-slate-400 mt-1">{t.pricing.trialDesc}</p>
              </div>
              <div className="text-3xl font-extrabold text-white light:text-slate-900">3 Free Uses <span className="text-sm font-normal text-slate-400">/ auto-enabled</span></div>
              <ul className="space-y-3 text-xs text-slate-300">
                <li className="flex items-center space-x-2"><CheckCircle className="w-4 h-4 text-loomora-secondary" /><span>All Free Plan Features</span></li>
                <li className="flex items-center space-x-2"><CheckCircle className="w-4 h-4 text-loomora-secondary" /><span>3 Cloud AI Transcriptions</span></li>
                <li className="flex items-center space-x-2"><CheckCircle className="w-4 h-4 text-loomora-secondary" /><span>3 Smart AI Summary Extractions</span></li>
                <li className="flex items-center space-x-2"><CheckCircle className="w-4 h-4 text-loomora-secondary" /><span>Idempotent Use Accounting</span></li>
              </ul>
            </div>
            <Link href="/download" className="mt-8 block text-center py-3 rounded-full bg-loomora-primary/20 border border-loomora-primary/40 text-white font-semibold text-xs hover:bg-loomora-primary/30 transition-colors">
              Try In App
            </Link>
          </div>

          {/* Pro Plan */}
          <div className="glass p-8 rounded-3xl border-2 border-loomora-primary flex flex-col justify-between shadow-xl shadow-loomora-primary/20 relative">
            <div className="space-y-6">
              <div>
                <span className="text-[10px] font-bold uppercase tracking-wider bg-loomora-primary text-white px-2.5 py-1 rounded-full">Recommended</span>
                <h3 className="text-xl font-bold text-white light:text-slate-900 mt-2">{t.pricing.proPlan}</h3>
                <p className="text-xs text-slate-400 mt-1">{t.pricing.proDesc}</p>
              </div>
              <div className="text-3xl font-extrabold text-white light:text-slate-900">$4.99 <span className="text-sm font-normal text-slate-400">/ license key</span></div>
              <ul className="space-y-3 text-xs text-slate-300">
                <li className="flex items-center space-x-2"><CheckCircle className="w-4 h-4 text-loomora-secondary" /><span>Unlimited Cloud AI Transcripts</span></li>
                <li className="flex items-center space-x-2"><CheckCircle className="w-4 h-4 text-loomora-secondary" /><span>Unlimited Speaker Labels &amp; Action Items</span></li>
                <li className="flex items-center space-x-2"><CheckCircle className="w-4 h-4 text-loomora-secondary" /><span>Advanced Audio Export Formats</span></li>
                <li className="flex items-center space-x-2"><CheckCircle className="w-4 h-4 text-loomora-secondary" /><span>Priority Customer Support</span></li>
              </ul>
            </div>
            <Link href="/pro" className="mt-8 block text-center py-3 rounded-full bg-loomora-primary text-white font-semibold text-xs hover:bg-loomora-primary/90 transition-colors shadow-lg shadow-loomora-primary/30">
              {t.pricing.buyCta}
            </Link>
          </div>
        </div>
      </section>

      {/* 6. FAQ SECTION */}
      <section className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 space-y-8">
        <div className="text-center space-y-2">
          <h2 className="text-3xl font-bold text-white light:text-slate-900">Frequently Asked Questions</h2>
          <p className="text-sm text-slate-400">Honest answers regarding privacy, audio storage, and AI processing.</p>
        </div>

        <div className="space-y-4">
          <div className="glass p-6 rounded-2xl border border-white/10 space-y-2">
            <h4 className="font-bold text-white light:text-slate-900 flex items-center space-x-2">
              <HelpCircle className="w-4 h-4 text-loomora-secondary" />
              <span>Are my voice recordings uploaded to the cloud automatically?</span>
            </h4>
            <p className="text-xs text-slate-400 leading-relaxed pl-6">
              No. Loomora is strictly local-first. Your audio recordings remain stored on your device&apos;s internal storage. Audio is transmitted over HTTPS only when you explicitly tap the Transcribe button and confirm the cloud processing disclosure.
            </p>
          </div>

          <div className="glass p-6 rounded-2xl border border-white/10 space-y-2">
            <h4 className="font-bold text-white light:text-slate-900 flex items-center space-x-2">
              <HelpCircle className="w-4 h-4 text-loomora-secondary" />
              <span>What happens if my AI trial ends or an AI request fails?</span>
            </h4>
            <p className="text-xs text-slate-400 leading-relaxed pl-6">
              Trial uses are consumed only when an AI request succeeds. If an error occurs, your trial counter is preserved. Furthermore, trial expiration never locks your ability to record, play back, edit, or export local audio files.
            </p>
          </div>

          <div className="glass p-6 rounded-2xl border border-white/10 space-y-2">
            <h4 className="font-bold text-white light:text-slate-900 flex items-center space-x-2">
              <HelpCircle className="w-4 h-4 text-loomora-secondary" />
              <span>How does non-destructive editing work?</span>
            </h4>
            <p className="text-xs text-slate-400 leading-relaxed pl-6">
              Loomora saves edit actions (trims, section deletions, speech clarity filters) in an immutable recipe. When exporting, it creates a new edited audio file without altering your original raw recording.
            </p>
          </div>
        </div>
      </section>

      {/* 7. FINAL CTA BANNER */}
      <section className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="bg-gradient-to-r from-loomora-primary to-indigo-900 rounded-3xl p-8 sm:p-12 text-center text-white space-y-6 shadow-2xl">
          <h2 className="text-3xl sm:text-4xl font-extrabold">Ready to Upgrade Your Voice Notes?</h2>
          <p className="max-w-xl mx-auto text-slate-200 text-sm sm:text-base">
            Download Loomora for Android today and take complete ownership of your recorded conversations.
          </p>
          <div className="flex flex-col sm:flex-row items-center justify-center gap-4 pt-2">
            <Link
              href="/download"
              className="px-8 py-3.5 rounded-full bg-white text-loomora-primary font-bold text-sm hover:bg-slate-100 transition-colors shadow-lg"
            >
              Download Free Android APK
            </Link>
            <Link
              href="/pro"
              className="px-8 py-3.5 rounded-full border border-white/30 text-white font-bold text-sm hover:bg-white/10 transition-colors"
            >
              Activate Pro Key
            </Link>
          </div>
        </div>
      </section>
    </div>
  );
}
