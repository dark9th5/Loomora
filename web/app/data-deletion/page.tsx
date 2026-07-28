import React from 'react';
import Link from 'next/link';

export default function DataDeletionPage() {
  return (
    <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-16 space-y-8">
      <div className="space-y-4 border-b border-white/10 pb-8">
        <span className="text-xs font-bold uppercase tracking-wider text-loomora-secondary">User Data Rights</span>
        <h1 className="text-4xl font-extrabold text-white light:text-slate-900">Data Deletion Instructions</h1>
        <p className="text-xs text-slate-400">How to remove all local and cloud data completely.</p>
      </div>

      <div className="glass p-8 sm:p-12 rounded-3xl border border-white/10 space-y-6 text-sm text-slate-300 leading-relaxed">
        <h3 className="text-lg font-bold text-white">1. Local Audio &amp; Database Deletion</h3>
        <p>
          Because Loomora stores all recordings locally on your device, deleting an item in the app immediately and permanently purges the audio file from your device storage.
        </p>
        <ol className="list-decimal pl-5 space-y-2 text-xs text-slate-400">
          <li>Open Loomora and navigate to <strong>Library</strong>.</li>
          <li>Tap the options menu on any recording and select <strong>Delete Recording</strong>.</li>
          <li>Confirm deletion to permanently remove the file.</li>
        </ol>

        <h3 className="text-lg font-bold text-white">2. Complete App Data Reset</h3>
        <p>
          To erase all Loomora data (recordings, settings, preferences) in a single action:
        </p>
        <ol className="list-decimal pl-5 space-y-2 text-xs text-slate-400">
          <li>Open Android <strong>Settings → Apps → Loomora</strong>.</li>
          <li>Select <strong>Storage &amp; Cache</strong>.</li>
          <li>Tap <strong>Clear Storage</strong> (or Clear Data).</li>
        </ol>

        <h3 className="text-lg font-bold text-white">3. Offline AI Processing Records</h3>
        <p>
          Current AI processing is local/offline after required models are installed. Removing app data deletes local settings, license state, trial records, transcripts, and insight revisions stored by the app. To request help reviewing support data you explicitly sent us, contact <Link href="/contact" className="text-loomora-secondary underline">Loomora Support</Link>.
        </p>
      </div>
    </div>
  );
}
