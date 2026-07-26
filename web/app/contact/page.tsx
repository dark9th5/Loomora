'use client';

import React, { useState } from 'react';
import { Mail, MessageSquare, Send, CheckCircle2 } from 'lucide-react';
import { env } from '@/lib/env';

export default function ContactPage() {
  const [submitted, setSubmitted] = useState(false);
  const [formData, setFormData] = useState({ name: '', email: '', subject: 'General Query', message: '' });

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (formData.email && formData.message) {
      setSubmitted(true);
    }
  };

  return (
    <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-16 space-y-12">
      <div className="text-center space-y-4">
        <h1 className="text-4xl sm:text-5xl font-extrabold text-white light:text-slate-900">
          Contact Loomora Support
        </h1>
        <p className="text-slate-400 light:text-slate-600 text-base max-w-xl mx-auto">
          Have questions about Pro licensing, Android installation, or local data privacy? We are here to help.
        </p>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
        <div className="glass p-8 rounded-3xl border border-white/10 space-y-6">
          <h3 className="text-xl font-bold text-white light:text-slate-900 flex items-center space-x-2">
            <Mail className="w-5 h-5 text-loomora-secondary" />
            <span>Direct Email Support</span>
          </h3>
          <p className="text-xs text-slate-400 leading-relaxed">
            Send us an email directly at any time. We aim to respond to all technical queries within 24 hours.
          </p>

          <div className="space-y-3 text-xs">
            <div className="bg-slate-900 p-3.5 rounded-xl border border-white/5 flex justify-between items-center">
              <span className="text-slate-400">General Contact:</span>
              <a href={`mailto:${env.contactEmail}`} className="font-mono text-loomora-secondary hover:underline">{env.contactEmail}</a>
            </div>
            <div className="bg-slate-900 p-3.5 rounded-xl border border-white/5 flex justify-between items-center">
              <span className="text-slate-400">Technical Support:</span>
              <a href={`mailto:${env.supportEmail}`} className="font-mono text-loomora-secondary hover:underline">{env.supportEmail}</a>
            </div>
          </div>
        </div>

        <div className="glass p-8 rounded-3xl border border-white/10 space-y-6">
          <h3 className="text-xl font-bold text-white light:text-slate-900 flex items-center space-x-2">
            <MessageSquare className="w-5 h-5 text-loomora-secondary" />
            <span>Send a Message</span>
          </h3>

          {submitted ? (
            <div className="p-6 bg-green-500/10 border border-green-500/30 rounded-2xl space-y-2 text-xs text-green-300 text-center">
              <CheckCircle2 className="w-6 h-6 text-green-400 mx-auto" />
              <h4 className="font-bold text-sm text-white">Message Sent!</h4>
              <p>Thank you for contacting us. We will respond to <strong className="text-white">{formData.email}</strong> shortly.</p>
            </div>
          ) : (
            <form onSubmit={handleSubmit} className="space-y-4">
              <div>
                <label className="block text-xs font-semibold text-slate-300 mb-1">Your Name</label>
                <input
                  type="text"
                  required
                  value={formData.name}
                  onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                  placeholder="Jane Doe"
                  className="w-full px-4 py-2.5 rounded-xl bg-slate-900 border border-white/10 text-white placeholder-slate-500 text-xs focus:outline-none focus:border-loomora-primary"
                />
              </div>
              <div>
                <label className="block text-xs font-semibold text-slate-300 mb-1">Email Address</label>
                <input
                  type="email"
                  required
                  value={formData.email}
                  onChange={(e) => setFormData({ ...formData, email: e.target.value })}
                  placeholder="jane@example.com"
                  className="w-full px-4 py-2.5 rounded-xl bg-slate-900 border border-white/10 text-white placeholder-slate-500 text-xs focus:outline-none focus:border-loomora-primary"
                />
              </div>
              <div>
                <label className="block text-xs font-semibold text-slate-300 mb-1">Message</label>
                <textarea
                  required
                  rows={4}
                  value={formData.message}
                  onChange={(e) => setFormData({ ...formData, message: e.target.value })}
                  placeholder="Describe your inquiry..."
                  className="w-full px-4 py-2.5 rounded-xl bg-slate-900 border border-white/10 text-white placeholder-slate-500 text-xs focus:outline-none focus:border-loomora-primary"
                />
              </div>
              <button
                type="submit"
                className="w-full py-3 rounded-xl bg-loomora-primary text-white font-bold text-xs hover:bg-loomora-primary/90 transition-colors shadow-lg shadow-loomora-primary/20 flex items-center justify-center space-x-2"
              >
                <Send className="w-4 h-4" />
                <span>Submit Message</span>
              </button>
            </form>
          )}
        </div>
      </div>
    </div>
  );
}
