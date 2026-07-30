'use client';

import React from 'react';
import Link from 'next/link';
import { useTheme } from '@/components/ThemeContext';
import { Mic, Sliders, ShieldCheck, Sparkles, Activity, FileText, Lock, Cpu, Globe } from 'lucide-react';

export default function FeaturesPage() {
  const { t } = useTheme();

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-16 space-y-20">
      <div className="text-center space-y-4 max-w-3xl mx-auto">
        <h1 className="text-4xl sm:text-5xl font-extrabold text-white light:text-slate-900">
          Engineered for High-Stakes Audio
        </h1>
        <p className="text-slate-400 light:text-slate-600 text-base sm:text-lg">
          Loomora combines local-first reliability, non-destructive editing, and secure provider-neutral AI notes.
        </p>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-12 items-center">
        <div className="space-y-6">
          <div className="w-12 h-12 rounded-2xl bg-loomora-primary/20 flex items-center justify-center text-loomora-secondary">
            <Lock className="w-6 h-6" />
          </div>
          <h2 className="text-3xl font-bold text-white light:text-slate-900">100% Local-First Architecture</h2>
          <p className="text-slate-300 light:text-slate-600 text-sm leading-relaxed">
            Core recording, playback, and file management require zero internet connection and zero login credentials. Your audio notes remain stored directly in Android internal storage guarded by strict canonical path safety.
          </p>
          <ul className="space-y-2 text-xs text-slate-300">
            <li className="flex items-center space-x-2"><ShieldCheck className="w-4 h-4 text-loomora-secondary" /><span>No login or user tracking required</span></li>
            <li className="flex items-center space-x-2"><ShieldCheck className="w-4 h-4 text-loomora-secondary" /><span>Path Traversal safe file deletion</span></li>
          </ul>
        </div>
        <div className="glass p-8 rounded-3xl border border-white/10 space-y-4">
          <div className="flex items-center space-x-3 text-loomora-secondary text-sm font-semibold">
            <Cpu className="w-5 h-5" />
            <span>Foreground Service Execution</span>
          </div>
          <p className="text-xs text-slate-400 leading-relaxed">
            Active microphone recording is managed by an Android Foreground Service (<code className="bg-slate-950 px-1 py-0.5 rounded text-white">foregroundServiceType=&quot;microphone&quot;</code>) with persistent notifications. Long AI job progress notification hardening remains tracked separately.
          </p>
        </div>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-12 items-center">
        <div className="glass p-8 rounded-3xl border border-white/10 space-y-4 order-2 md:order-1">
          <div className="flex items-center space-x-3 text-loomora-secondary text-sm font-semibold">
            <Activity className="w-5 h-5" />
            <span>Non-Destructive Export Pipeline</span>
          </div>
          <p className="text-xs text-slate-400 leading-relaxed">
            Edits are stored in an immutable <code className="bg-slate-950 px-1 py-0.5 rounded text-white">EditRecipe</code> model. Exporting generates a new <code className="bg-slate-950 px-1 py-0.5 rounded text-white">_edited.m4a</code> audio file while preserving your original raw recording completely untouched.
          </p>
        </div>
        <div className="space-y-6 order-1 md:order-2">
          <div className="w-12 h-12 rounded-2xl bg-loomora-primary/20 flex items-center justify-center text-loomora-secondary">
            <Sliders className="w-6 h-6" />
          </div>
          <h2 className="text-3xl font-bold text-white light:text-slate-900">Non-Destructive Audio Editor</h2>
          <p className="text-slate-300 light:text-slate-600 text-sm leading-relaxed">
            Trim specific ranges and delete selected sections while preserving the original source file. Optional offline RNNoise filtering keeps the original recording and prepares a clearer copy for transcription.
          </p>
        </div>
      </div>

      <div className="text-center pt-8">
        <Link
          href="/download"
          className="inline-flex items-center space-x-2 px-8 py-4 rounded-full bg-loomora-primary text-white font-bold text-base hover:bg-loomora-primary/90 transition-all shadow-xl shadow-loomora-primary/30"
        >
          <Mic className="w-5 h-5" />
          <span>Get Loomora for Android</span>
        </Link>
      </div>
    </div>
  );
}
