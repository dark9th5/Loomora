export default function ChangelogPage() {
  return (
    <div className="mx-auto max-w-4xl px-4 py-12">
      <h1 className="text-3xl font-semibold text-white">Changelog and release notes</h1>
      <div className="mt-6 space-y-6">
        <div className="rounded-lg border border-slate-800 bg-slate-900 p-5">
          <h2 className="font-semibold text-white">Loomora 1.0.3 - Unified Code Fix & Live AI Upgrade</h2>
          <p className="mt-2 text-sm text-slate-400">
            Applied unified transcription segmenter, speaker fusion display rows, intelligent Vietnamese/English transcription model routing, AudioCaptureSpec standardization, and live caption &amp; translation coordinator foundations.
          </p>
        </div>
        <div className="rounded-lg border border-slate-800 bg-slate-900 p-5">
          <h2 className="font-semibold text-white">Loomora 1.0.2 - Task Management Upgrade</h2>
          <p className="mt-2 text-sm text-slate-400">
            Integrated Task Management feature, database migration 8 to 9, deterministic task ID deduplication based on evidence segment IDs, and Room schema validation.
          </p>
        </div>
        <div className="rounded-lg border border-slate-800 bg-slate-900 p-5">
          <h2 className="font-semibold text-white">Loomora 1.0.1 - Internal test channel</h2>
          <p className="mt-2 text-sm text-slate-400">
            Added selectable MIC, VOICE_RECOGNITION, and CAMCORDER sources plus offline RNNoise filtering at Off, Light, and Strong levels.
          </p>
        </div>
      </div>
    </div>
  );
}
