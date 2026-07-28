import Link from 'next/link';

type PortalShellProps = {
  title: string;
  description: string;
  nav: Array<{ href: string; label: string }>;
  children: React.ReactNode;
};

export function PortalShell({ title, description, nav, children }: PortalShellProps) {
  return (
    <div className="mx-auto flex w-full max-w-7xl gap-6 px-4 py-8 sm:px-6 lg:px-8">
      <aside className="hidden w-64 shrink-0 lg:block">
        <nav className="sticky top-24 space-y-1 rounded-lg border border-slate-800 bg-slate-900/70 p-3">
          {nav.map((item) => (
            <Link
              key={item.href}
              href={item.href}
              className="block rounded-md px-3 py-2 text-sm text-slate-300 hover:bg-slate-800 hover:text-white"
            >
              {item.label}
            </Link>
          ))}
        </nav>
      </aside>
      <section className="min-w-0 flex-1 space-y-6">
        <div>
          <h1 className="text-3xl font-semibold tracking-tight text-white light:text-slate-950">{title}</h1>
          <p className="mt-2 max-w-3xl text-sm leading-6 text-slate-400 light:text-slate-600">{description}</p>
        </div>
        {children}
      </section>
    </div>
  );
}
