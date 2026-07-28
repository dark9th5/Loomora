export default function ChangelogPage() {
  return (
    <div className="mx-auto max-w-4xl px-4 py-12">
      <h1 className="text-3xl font-semibold text-white">Changelog and release notes</h1>
      <div className="mt-6 rounded-lg border border-slate-800 bg-slate-900 p-5">
        <h2 className="font-semibold text-white">Internal test channel</h2>
        <p className="mt-2 text-sm text-slate-400">
          The latest Android release audit is internal-test quality until production signing and signed release smoke testing are completed.
        </p>
      </div>
    </div>
  );
}
