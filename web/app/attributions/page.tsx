export default function AttributionsPage() {
  return (
    <div className="mx-auto max-w-4xl px-4 py-12">
      <h1 className="text-3xl font-semibold text-white">Model and third-party attributions</h1>
      <p className="mt-3 text-slate-400">
        Loomora uses open-source Android, web, and local AI components. Large speech model packs are imported separately and must preserve their upstream license notices.
      </p>
      <ul className="mt-8 space-y-3 text-sm text-slate-300">
        <li>sherpa-onnx for offline speech recognition and diarization runtime integration.</li>
        <li>Media3 for Android audio playback/edit/export pipeline.</li>
        <li>RNNoise by Xiph.Org for offline neural-network noise suppression (BSD 3-Clause).</li>
        <li>Next.js, React, Tailwind CSS, Auth.js, Prisma, and Zod for this portal.</li>
      </ul>
    </div>
  );
}
