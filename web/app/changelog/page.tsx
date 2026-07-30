export default function ChangelogPage() {
  return (
    <div className="mx-auto max-w-4xl px-4 py-12">
      <h1 className="text-3xl font-semibold text-white">Changelog and release notes</h1>
      <div className="mt-6 rounded-lg border border-slate-800 bg-slate-900 p-5">
        <h2 className="font-semibold text-white">Loomora 1.0.1 - Internal test channel</h2>
        <p className="mt-2 text-sm text-slate-400">
          Added selectable MIC, VOICE_RECOGNITION, and CAMCORDER sources plus offline RNNoise filtering at Off, Light, and Strong levels. Loomora keeps the original recording and uses the filtered copy for clearer transcription.
        </p>
        <p className="mt-2 text-sm text-slate-400">
          This APK remains internal-test quality and is signed with the Android debug certificate until production signing and Play Store verification are completed.
        </p>
      </div>
    </div>
  );
}
