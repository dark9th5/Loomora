'use client';

import React, { useState } from 'react';
import { useTheme } from '@/components/ThemeContext';
import { Key, ShieldCheck, CheckCircle2, CreditCard, Mail } from 'lucide-react';
import { env } from '@/lib/env';

export default function BuyProPage() {
  const [email, setEmail] = useState('');
  const [requested, setRequested] = useState(false);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (email) {
      setRequested(true);
    }
  };

  return (
    <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-16 space-y-12">
      <div className="text-center space-y-4">
        <div className="inline-flex items-center space-x-2 px-3 py-1 rounded-full bg-loomora-primary/20 text-loomora-container text-xs font-bold">
          <ShieldCheck className="w-4 h-4 text-loomora-secondary" />
          <span>Loomora Pro Activation Handshake</span>
        </div>
        <h1 className="text-4xl sm:text-5xl font-extrabold text-white light:text-slate-900">
          Get Your Loomora Pro License Key
        </h1>
        <p className="text-slate-400 light:text-slate-600 text-base max-w-xl mx-auto">
          Purchase or request your 8+ character signed license key (e.g., <code className="bg-slate-900 px-1 py-0.5 rounded text-white">LM-PRO-XXXX</code>) to enter directly inside the Android application.
        </p>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
        {/* Manual Fulfillment Request Form */}
        <div className="glass p-8 rounded-3xl border border-white/10 space-y-6">
          <h3 className="text-xl font-bold text-white light:text-slate-900 flex items-center space-x-2">
            <Key className="w-5 h-5 text-loomora-secondary" />
            <span>License Key Issuance</span>
          </h3>
          <p className="text-xs text-slate-400 leading-relaxed">
            Enter your email address below to receive payment instructions or request an early-access Loomora Pro license code.
          </p>

          {requested ? (
            <div className="p-4 bg-green-500/10 border border-green-500/30 rounded-2xl space-y-2 text-xs text-green-300">
              <div className="flex items-center space-x-2 font-bold">
                <CheckCircle2 className="w-4 h-4 text-green-400" />
                <span>Request Received!</span>
              </div>
              <p>We have dispatched license key instructions to <strong className="text-white">{email}</strong>. Check your inbox to complete activation.</p>
            </div>
          ) : (
            <form onSubmit={handleSubmit} className="space-y-4">
              <div>
                <label className="block text-xs font-semibold text-slate-300 mb-1">Email Address</label>
                <input
                  type="email"
                  required
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  placeholder="name@example.com"
                  className="w-full px-4 py-3 rounded-xl bg-slate-900 border border-white/10 text-white placeholder-slate-500 text-sm focus:outline-none focus:border-loomora-primary"
                />
              </div>
              <button
                type="submit"
                className="w-full py-3.5 rounded-xl bg-loomora-primary text-white font-bold text-sm hover:bg-loomora-primary/90 transition-colors shadow-lg shadow-loomora-primary/20"
              >
                Request Pro License Key ($4.99)
              </button>
            </form>
          )}

          <p className="text-[11px] text-slate-500 text-center">
            Need help? Contact support directly at <a href={`mailto:${env.supportEmail}`} className="text-loomora-secondary underline">{env.supportEmail}</a>
          </p>
        </div>

        {/* How Activation Works */}
        <div className="glass p-8 rounded-3xl border border-white/10 space-y-6">
          <h3 className="text-xl font-bold text-white light:text-slate-900 flex items-center space-x-2">
            <CheckCircle2 className="w-5 h-5 text-loomora-secondary" />
            <span>How Activation Works</span>
          </h3>
          <ol className="space-y-4 text-xs text-slate-300 list-decimal pl-4 leading-relaxed">
            <li>Receive your unique license key via email (e.g. <code className="bg-slate-900 px-1 py-0.5 rounded text-white">LM-PRO-KEY-1234</code>).</li>
            <li>Open Loomora on your Android device and navigate to <strong>Settings → Pro &amp; Trial</strong>.</li>
            <li>Paste your key into the <strong>Activate Pro License Key</strong> field and tap Activate.</li>
            <li>Your app immediately unlocks unlimited cloud AI transcriptions and smart insights!</li>
          </ol>
        </div>
      </div>
    </div>
  );
}
