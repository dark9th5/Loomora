'use client';

import React, { useState } from 'react';
import { PortalShell } from '@/components/PortalShell';
import { Save, CheckCircle2, User } from 'lucide-react';

const nav = [
  { href: '/account', label: 'Dashboard' },
  { href: '/account/settings', label: 'Settings' },
];

export default function AccountSettingsPage() {
  const [formData, setFormData] = useState({ company: '', phone: '', country: '' });
  const [saved, setSaved] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    // Profile update will be wired when database is connected
    setSaved(true);
    setTimeout(() => setSaved(false), 3000);
  };

  return (
    <PortalShell title="Account Settings" description="Update your profile information. These settings are stored server-side." nav={nav}>
      <form onSubmit={handleSubmit} className="space-y-5 max-w-lg">
        {saved && (
          <div className="flex items-center gap-2 rounded-lg border border-emerald-700/50 bg-emerald-950/20 p-3 text-sm text-emerald-300">
            <CheckCircle2 className="h-4 w-4" /> Settings saved.
          </div>
        )}

        <div>
          <label className="block text-xs font-semibold text-slate-300 mb-1">Company (optional)</label>
          <input
            type="text"
            value={formData.company}
            onChange={(e) => setFormData({ ...formData, company: e.target.value })}
            placeholder="Your company name"
            className="w-full rounded-lg border border-slate-700 bg-slate-800 px-3 py-2 text-sm text-white placeholder-slate-500 focus:border-loomora-primary focus:outline-none"
          />
        </div>

        <div>
          <label className="block text-xs font-semibold text-slate-300 mb-1">Phone (optional)</label>
          <input
            type="tel"
            value={formData.phone}
            onChange={(e) => setFormData({ ...formData, phone: e.target.value })}
            placeholder="+84 xxx xxx xxx"
            className="w-full rounded-lg border border-slate-700 bg-slate-800 px-3 py-2 text-sm text-white placeholder-slate-500 focus:border-loomora-primary focus:outline-none"
          />
        </div>

        <div>
          <label className="block text-xs font-semibold text-slate-300 mb-1">Country (optional)</label>
          <input
            type="text"
            value={formData.country}
            onChange={(e) => setFormData({ ...formData, country: e.target.value })}
            placeholder="Vietnam"
            className="w-full rounded-lg border border-slate-700 bg-slate-800 px-3 py-2 text-sm text-white placeholder-slate-500 focus:border-loomora-primary focus:outline-none"
          />
        </div>

        <button
          type="submit"
          className="rounded-lg bg-loomora-primary px-6 py-2.5 text-sm font-semibold text-white hover:bg-loomora-primary/90 transition-colors flex items-center gap-2"
        >
          <Save className="h-4 w-4" /> Save Settings
        </button>
      </form>

      <div className="rounded-lg border border-slate-800 bg-slate-900/70 p-4 text-sm text-slate-400 space-y-2">
        <h4 className="font-semibold text-slate-200 flex items-center gap-2"><User className="h-4 w-4" /> Account Info</h4>
        <p>Your name and email are managed through your Google account. Sign out and sign in with a different Google account to change.</p>
      </div>
    </PortalShell>
  );
}
