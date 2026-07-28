'use client';

import React, { useState } from 'react';
import { useRouter } from 'next/navigation';
import { PortalShell } from '@/components/PortalShell';
import { Send, CheckCircle2 } from 'lucide-react';

const nav = [
  { href: '/account', label: 'Dashboard' },
  { href: '/account/support', label: 'Support tickets' },
  { href: '/account/support/new', label: 'New ticket' },
];

const topics = ['General', 'Licensing', 'Technical', 'Billing', 'Feature Request', 'Bug Report'];

export default function NewTicketPage() {
  const router = useRouter();
  const [formData, setFormData] = useState({ subject: '', topic: topics[0], message: '' });
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setSubmitting(true);
    setError(null);
    try {
      const res = await fetch('/api/support/tickets', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(formData),
      });
      const data = await res.json();
      if (!res.ok) {
        setError(data.error ?? 'Failed to create ticket.');
        return;
      }
      setSuccess(true);
    } catch {
      setError('Network error. Please try again.');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <PortalShell title="Create Support Ticket" description="Describe your issue and we will respond as soon as possible." nav={nav}>
      {success ? (
        <div className="rounded-2xl border border-emerald-700/50 bg-emerald-950/20 p-8 text-center space-y-3">
          <CheckCircle2 className="h-8 w-8 text-emerald-400 mx-auto" />
          <h3 className="text-lg font-semibold text-white">Ticket Created</h3>
          <p className="text-sm text-slate-300">We have received your support request and will respond shortly.</p>
          <button onClick={() => router.push('/account/support')} className="rounded-lg bg-loomora-primary px-4 py-2 text-sm font-semibold text-white hover:bg-loomora-primary/90 transition-colors">
            View My Tickets
          </button>
        </div>
      ) : (
        <form onSubmit={handleSubmit} className="space-y-5 max-w-2xl">
          {error && <div className="rounded-lg border border-red-800/50 bg-red-950/30 p-3 text-sm text-red-300">{error}</div>}

          <div>
            <label className="block text-xs font-semibold text-slate-300 mb-1">Topic</label>
            <select
              value={formData.topic}
              onChange={(e) => setFormData({ ...formData, topic: e.target.value })}
              className="w-full rounded-lg border border-slate-700 bg-slate-800 px-3 py-2 text-sm text-white focus:border-loomora-primary focus:outline-none"
            >
              {topics.map((t) => <option key={t} value={t}>{t}</option>)}
            </select>
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-300 mb-1">Subject</label>
            <input
              type="text"
              required
              minLength={4}
              maxLength={180}
              value={formData.subject}
              onChange={(e) => setFormData({ ...formData, subject: e.target.value })}
              placeholder="Brief description of your issue"
              className="w-full rounded-lg border border-slate-700 bg-slate-800 px-3 py-2 text-sm text-white placeholder-slate-500 focus:border-loomora-primary focus:outline-none"
            />
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-300 mb-1">Message</label>
            <textarea
              required
              minLength={10}
              maxLength={4000}
              rows={6}
              value={formData.message}
              onChange={(e) => setFormData({ ...formData, message: e.target.value })}
              placeholder="Describe your issue in detail..."
              className="w-full rounded-lg border border-slate-700 bg-slate-800 px-3 py-2 text-sm text-white placeholder-slate-500 focus:border-loomora-primary focus:outline-none"
            />
          </div>

          <button
            type="submit"
            disabled={submitting}
            className="rounded-lg bg-loomora-primary px-6 py-2.5 text-sm font-semibold text-white hover:bg-loomora-primary/90 transition-colors disabled:opacity-50 flex items-center gap-2"
          >
            <Send className="h-4 w-4" />
            {submitting ? 'Submitting...' : 'Submit Ticket'}
          </button>
        </form>
      )}
    </PortalShell>
  );
}
