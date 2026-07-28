import { requireSession } from '@/lib/portal/authz';

export default async function FirstLoginOnboardingPage() {
  await requireSession();
  return (
    <div className="mx-auto max-w-3xl px-4 py-12">
      <h1 className="text-3xl font-semibold text-white">Welcome to Loomora</h1>
      <p className="mt-3 text-slate-400">
        Add optional profile details for invoices, support, and business inquiries. Core Android recording never requires this account.
      </p>
      <form className="mt-6 space-y-4 rounded-lg border border-slate-800 bg-slate-900 p-5">
        <input className="w-full rounded-md border border-slate-700 bg-slate-950 px-3 py-2" placeholder="Company" />
        <input className="w-full rounded-md border border-slate-700 bg-slate-950 px-3 py-2" placeholder="Country" />
        <button type="button" className="rounded-md bg-loomora-primary px-4 py-2 text-sm font-semibold text-white">
          Save profile draft
        </button>
      </form>
    </div>
  );
}
