import React from 'react';

export default function TermsPage() {
  return (
    <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-16 space-y-8">
      <div className="space-y-4 border-b border-white/10 pb-8">
        <span className="text-xs font-bold uppercase tracking-wider text-loomora-secondary">Terms &amp; Conditions</span>
        <h1 className="text-4xl font-extrabold text-white light:text-slate-900">Terms of Service</h1>
        <p className="text-xs text-slate-400">Effective Date: July 26, 2026</p>
      </div>

      <div className="glass p-8 sm:p-12 rounded-3xl border border-white/10 space-y-6 text-sm text-slate-300 leading-relaxed">
        <h3 className="text-lg font-bold text-white">1. Acceptance of Terms</h3>
        <p>
          By downloading, installing, or using the Loomora application or website, you agree to comply with and be bound by these Terms of Service.
        </p>

        <h3 className="text-lg font-bold text-white">2. Permitted Use &amp; Consent Responsibilities</h3>
        <p>
          Loomora is designed for personal voice note-taking, lecture recording, and meeting summaries. You are solely responsible for ensuring that your voice recording activities comply with all applicable local, state, and federal wiretapping and audio consent laws.
        </p>

        <h3 className="text-lg font-bold text-white">3. Pro License &amp; Entitlements</h3>
        <p>
          Loomora Pro license keys grant access to premium capabilities (such as cloud AI transcription and smart summaries). Licenses are validated via backend contracts and cached securely for offline grace periods.
        </p>

        <h3 className="text-lg font-bold text-white">4. Limitation of Liability</h3>
        <p>
          Loomora is provided &quot;as is&quot; without warranties of any kind. Loomora is not liable for data loss resulting from device hardware failure, storage corruption, or user deletion actions.
        </p>
      </div>
    </div>
  );
}
