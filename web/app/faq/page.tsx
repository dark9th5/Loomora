import { featureMatrix } from '@/lib/portal/sample-data';

export default function FaqPage() {
  return (
    <div className="mx-auto max-w-4xl px-4 py-12">
      <h1 className="text-3xl font-semibold text-white">FAQ</h1>
      <div className="mt-8 space-y-5">
        {[
          ['Does Loomora require an account?', 'No. The Android app keeps recording, playback and library features usable without an account or internet.'],
          ['Are AI features cloud based?', 'The current Android AI architecture runs on device after required local models are installed.'],
          ['Are summaries deep generative LLM output?', 'Release-available insights are evidence-linked and extractive. Deep generative summaries remain experimental.'],
          ['Can support identify speakers by real name?', 'No. Speaker labels are generic and manually renameable by the user.'],
        ].map(([q, a]) => (
          <section key={q} className="rounded-lg border border-slate-800 bg-slate-900 p-5">
            <h2 className="font-semibold text-white">{q}</h2>
            <p className="mt-2 text-sm text-slate-400">{a}</p>
          </section>
        ))}
      </div>
      <h2 className="mt-10 text-xl font-semibold text-white">Feature status</h2>
      <ul className="mt-4 space-y-2 text-sm text-slate-400">
        {featureMatrix.map(([name, status, note]) => (
          <li key={name}>{name}: {status}. {note}.</li>
        ))}
      </ul>
    </div>
  );
}
