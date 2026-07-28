import { signIn } from '@/auth';

export default function SignInPage() {
  return (
    <div className="mx-auto max-w-md px-4 py-20">
      <div className="rounded-lg border border-slate-800 bg-slate-900 p-6">
        <h1 className="text-2xl font-semibold text-white">Sign in to Loomora</h1>
        <p className="mt-2 text-sm text-slate-400">
          Use Google sign-in for customer downloads, support, and administration. Verified Google email is required.
        </p>
        <form
          className="mt-6"
          action={async () => {
            'use server';
            await signIn('google', { redirectTo: '/account' });
          }}
        >
          <button className="w-full rounded-md bg-loomora-primary px-4 py-3 font-semibold text-white hover:bg-loomora-primary/90">
            Continue with Google
          </button>
        </form>
      </div>
    </div>
  );
}
