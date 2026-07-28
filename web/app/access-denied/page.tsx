import Link from 'next/link';

export default function AccessDeniedPage() {
  return (
    <div className="mx-auto max-w-2xl px-4 py-20">
      <h1 className="text-3xl font-semibold text-white">Access denied</h1>
      <p className="mt-3 text-slate-400">
        This area requires a verified Google account with the correct role. Customer accounts cannot access admin routes.
      </p>
      <Link href="/" className="mt-6 inline-block rounded-md bg-loomora-primary px-4 py-2 font-semibold text-white">
        Return home
      </Link>
    </div>
  );
}
