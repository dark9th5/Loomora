export default function UnauthorizedAdminPage() {
  return (
    <div className="mx-auto max-w-2xl px-4 py-20">
      <h1 className="text-3xl font-semibold text-white">Unauthorized admin state</h1>
      <p className="mt-3 text-slate-400">
        Your verified Google account is signed in, but it does not have an admin role. Customer accounts cannot access admin data or actions.
      </p>
    </div>
  );
}
