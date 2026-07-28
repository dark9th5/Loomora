import React from 'react';
import { ShieldCheck } from 'lucide-react';

export default function PrivacyPage() {
  return (
    <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-16 space-y-8">
      <div className="space-y-4 border-b border-white/10 pb-8">
        <div className="flex items-center space-x-2 text-xs font-bold uppercase tracking-wider text-loomora-secondary">
          <ShieldCheck className="w-4 h-4" />
          <span>Privacy Guarantee</span>
        </div>
        <h1 className="text-4xl font-extrabold text-white light:text-slate-900">Privacy Policy</h1>
        <p className="text-xs text-slate-400">Effective Date: July 26, 2026</p>
      </div>

      <div className="glass p-8 sm:p-12 rounded-3xl border border-white/10 space-y-6 text-sm text-slate-300 leading-relaxed">
        <h3 className="text-lg font-bold text-white">1. Core Local-First Principle</h3>
        <p>
          Loomora is designed from the ground up as a local-first application. Your voice recordings, audio edits, tags, and persistent metadata are stored directly in your Android device&apos;s internal application storage.
        </p>

        <h3 className="text-lg font-bold text-white">2. Audio Data Processing</h3>
        <p>
          Audio files are recorded exclusively through your device&apos;s microphone upon explicit user initiation. Audio is not uploaded for AI processing in the current offline architecture. Transcription, diarization, and extractive insights run on device after required local models are installed.
        </p>

        <h3 className="text-lg font-bold text-white">3. Zero Data Sale or Ingestion</h3>
        <p>
          Loomora does not sell, rent, or monetize your personal voice data. We do not use your private recordings to train machine learning models.
        </p>

        <h3 className="text-lg font-bold text-white">4. User File Control &amp; Deletion</h3>
        <p>
          You retain 100% ownership of your audio files. Deleting a recording inside the Loomora app permanently removes the audio file from local storage using path-traversal guarded canonical file operations.
        </p>
      </div>
    </div>
  );
}
