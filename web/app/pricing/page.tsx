'use client';

import React from 'react';
import Link from 'next/link';
import { useTheme } from '@/components/ThemeContext';
import { CheckCircle, ShieldCheck, HelpCircle } from 'lucide-react';

export default function PricingPage() {
  const { t } = useTheme();

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-16 space-y-16">
      <div className="text-center space-y-4 max-w-3xl mx-auto">
        <h1 className="text-4xl sm:text-5xl font-extrabold text-white light:text-slate-900">
          Transparent, Capability-Based Plans
        </h1>
        <p className="text-slate-400 light:text-slate-600 text-base sm:text-lg">
          Local voice recording and playback remain free and offline. Pro maps to signed product capabilities, not runtime names or cloud processing promises.
        </p>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-3 gap-8">
        {/* Free Plan */}
        <div className="glass p-8 rounded-3xl border border-white/10 flex flex-col justify-between space-y-6">
          <div className="space-y-4">
            <h3 className="text-xl font-bold text-white light:text-slate-900">Loomora Free</h3>
            <p className="text-xs text-slate-400">Core local recorder and organizer.</p>
            <div className="text-3xl font-extrabold text-white">$0 <span className="text-sm font-normal text-slate-400">/ forever</span></div>
            <ul className="space-y-3 text-xs text-slate-300">
              <li className="flex items-center space-x-2"><CheckCircle className="w-4 h-4 text-loomora-secondary" /><span>Unlimited Local Recording</span></li>
              <li className="flex items-center space-x-2"><CheckCircle className="w-4 h-4 text-loomora-secondary" /><span>Media3 ExoPlayer Playback</span></li>
              <li className="flex items-center space-x-2"><CheckCircle className="w-4 h-4 text-loomora-secondary" /><span>Non-Destructive Audio Editor</span></li>
              <li className="flex items-center space-x-2"><CheckCircle className="w-4 h-4 text-loomora-secondary" /><span>English &amp; Vietnamese Localization</span></li>
            </ul>
          </div>
          <Link href="/download" className="block text-center py-3 rounded-full border border-white/20 text-white font-semibold text-xs hover:bg-white/10 transition-colors">
            Get Free Android App
          </Link>
        </div>

        {/* Trial Plan */}
        <div className="glass p-8 rounded-3xl border border-loomora-primary/40 flex flex-col justify-between space-y-6">
          <div className="space-y-4">
            <span className="text-[10px] font-bold uppercase tracking-wider bg-loomora-primary/30 text-loomora-container px-2.5 py-1 rounded-full">Included In App</span>
            <h3 className="text-xl font-bold text-white light:text-slate-900 mt-2">Loomora Trial</h3>
            <p className="text-xs text-slate-400">Successful-use premium capability testing.</p>
            <div className="text-3xl font-extrabold text-white">3 Free Uses</div>
            <ul className="space-y-3 text-xs text-slate-300">
              <li className="flex items-center space-x-2"><CheckCircle className="w-4 h-4 text-loomora-secondary" /><span>All Free Plan Capabilities</span></li>
              <li className="flex items-center space-x-2"><CheckCircle className="w-4 h-4 text-loomora-secondary" /><span>Eligible offline transcription jobs</span></li>
              <li className="flex items-center space-x-2"><CheckCircle className="w-4 h-4 text-loomora-secondary" /><span>Smart extractive insights with evidence IDs</span></li>
              <li className="flex items-center space-x-2"><CheckCircle className="w-4 h-4 text-loomora-secondary" /><span>Idempotent Trial Accounting</span></li>
            </ul>
          </div>
          <Link href="/download" className="block text-center py-3 rounded-full bg-loomora-primary/20 border border-loomora-primary/40 text-white font-semibold text-xs hover:bg-loomora-primary/30 transition-colors">
            Try In App
          </Link>
        </div>

        {/* Pro Plan */}
        <div className="glass p-8 rounded-3xl border-2 border-loomora-primary flex flex-col justify-between space-y-6 shadow-xl shadow-loomora-primary/20">
          <div className="space-y-4">
            <span className="text-[10px] font-bold uppercase tracking-wider bg-loomora-primary text-white px-2.5 py-1 rounded-full">Pro License</span>
            <h3 className="text-xl font-bold text-white light:text-slate-900 mt-2">Loomora Pro</h3>
            <p className="text-xs text-slate-400">Unlimited enhancement &amp; AI insights.</p>
            <div className="text-3xl font-extrabold text-white">$4.99 <span className="text-sm font-normal text-slate-400">/ license key</span></div>
            <ul className="space-y-3 text-xs text-slate-300">
              <li className="flex items-center space-x-2"><CheckCircle className="w-4 h-4 text-loomora-secondary" /><span>Offline transcription capability</span></li>
              <li className="flex items-center space-x-2"><CheckCircle className="w-4 h-4 text-loomora-secondary" /><span>Beta generic speaker labels when a compatible model/device is available</span></li>
              <li className="flex items-center space-x-2"><CheckCircle className="w-4 h-4 text-loomora-secondary" /><span>Smart AI Summaries &amp; Action Items</span></li>
              <li className="flex items-center space-x-2"><CheckCircle className="w-4 h-4 text-loomora-secondary" /><span>Advanced Non-Destructive Exports</span></li>
              <li className="flex items-center space-x-2"><CheckCircle className="w-4 h-4 text-loomora-secondary" /><span>Offline Grace &amp; Priority Support</span></li>
            </ul>
          </div>
          <Link href="/pro" className="block text-center py-3 rounded-full bg-loomora-primary text-white font-semibold text-xs hover:bg-loomora-primary/90 transition-colors shadow-lg shadow-loomora-primary/30">
            Activate License Key
          </Link>
        </div>
      </div>
    </div>
  );
}
