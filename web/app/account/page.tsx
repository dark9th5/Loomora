import Link from 'next/link';
import { PortalShell } from '@/components/PortalShell';
import { Badge, statusToBadgeVariant } from '@/components/Badge';
import { EmptyState } from '@/components/EmptyState';
import { requireSession } from '@/lib/portal/authz';
import { getLicensesByCustomer } from '@/features/licenses/license-service';
import { listOrdersByCustomer } from '@/features/orders/order-service';
import { listTicketsByCustomer } from '@/features/support/support-service';
import { getLatestStableRelease } from '@/features/downloads/download-service';
import { FileKey, Package, Headphones, Download, ShoppingCart, MessageSquare, Shield } from 'lucide-react';

const nav = [
  { href: '/account', label: 'Dashboard' },
  { href: '/account/licenses', label: 'My licenses' },
  { href: '/account/orders', label: 'Orders' },
  { href: '/account/support', label: 'Support tickets' },
  { href: '/account/downloads', label: 'Downloads' },
  { href: '/account/settings', label: 'Settings' },
];

export default async function AccountDashboardPage() {
  const session = await requireSession();
  const userId = session.user?.id;

  const [licenses, orders, tickets, latestRelease] = userId && process.env.DATABASE_URL
    ? await Promise.all([
        getLicensesByCustomer(userId),
        listOrdersByCustomer(userId),
        listTicketsByCustomer(userId),
        getLatestStableRelease(),
      ])
    : [[], [], [], null];

  const activeLicense = licenses.find((l) => l.status === 'ACTIVE');
  const pendingOrders = orders.filter((o) => o.status === 'PENDING_PAYMENT');
  const openTickets = tickets.filter((t) => t.status === 'OPEN' || t.status === 'IN_PROGRESS');

  return (
    <PortalShell title="Customer Dashboard" description="Your licenses, orders, and support — all in one place." nav={nav}>
      {/* User info */}
      <div className="rounded-xl border border-slate-800 bg-slate-900/70 p-4 flex items-center justify-between">
        <div className="flex items-center gap-3">
          <div className="h-10 w-10 rounded-full bg-loomora-primary/20 flex items-center justify-center">
            <Shield className="h-5 w-5 text-loomora-secondary" />
          </div>
          <div>
            <p className="text-sm font-medium text-white">{session.user?.name ?? session.user?.email}</p>
            <p className="text-xs text-slate-400">Role: {session.user?.role ?? 'CUSTOMER'}</p>
          </div>
        </div>
      </div>

      {/* Dashboard Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <DashboardCard
          icon={FileKey}
          label="Active License"
          value={activeLicense ? activeLicense.edition?.name ?? 'Licensed' : 'None'}
          badge={activeLicense ? <Badge variant="active" label="Active" /> : <Badge variant="draft" label="No License" />}
          href="/account/licenses"
        />
        <DashboardCard
          icon={ShoppingCart}
          label="Pending Orders"
          value={String(pendingOrders.length)}
          badge={pendingOrders.length > 0 ? <Badge variant="pending" label={`${pendingOrders.length} pending`} /> : undefined}
          href="/account/orders"
        />
        <DashboardCard
          icon={MessageSquare}
          label="Open Tickets"
          value={String(openTickets.length)}
          badge={openTickets.length > 0 ? <Badge variant="pending" label={`${openTickets.length} open`} /> : undefined}
          href="/account/support"
        />
        <DashboardCard
          icon={Download}
          label="Latest Release"
          value={latestRelease?.versionName ?? 'N/A'}
          href="/account/downloads"
        />
      </div>

      {/* Active License Detail */}
      {activeLicense && (
        <div className="rounded-xl border border-emerald-800/50 bg-emerald-950/20 p-5 space-y-3">
          <div className="flex items-center justify-between">
            <h3 className="text-sm font-semibold text-emerald-200 flex items-center gap-2">
              <FileKey className="h-4 w-4" /> License: {activeLicense.edition?.product?.name} {activeLicense.edition?.name}
            </h3>
            <Badge variant="active" />
          </div>
          <div className="grid grid-cols-2 sm:grid-cols-4 gap-4 text-xs text-slate-300">
            <div>
              <span className="text-slate-500">License ID</span>
              <p className="font-mono mt-0.5">{activeLicense.id}</p>
            </div>
            <div>
              <span className="text-slate-500">Capabilities</span>
              <p className="mt-0.5">{activeLicense.capabilities.map((c) => c.capability.key).join(', ') || 'N/A'}</p>
            </div>
            <div>
              <span className="text-slate-500">Device Binding</span>
              <p className="mt-0.5">{activeLicense.deviceBinding?.label ?? 'None'}</p>
            </div>
            <div>
              <span className="text-slate-500">Revision</span>
              <p className="mt-0.5">v{activeLicense.currentRevision}</p>
            </div>
          </div>
          <Link href={`/api/account/licenses/${activeLicense.id}/download`} className="inline-flex items-center gap-1.5 text-xs font-semibold text-loomora-secondary hover:underline">
            <Download className="h-3.5 w-3.5" /> Download .license file
          </Link>
        </div>
      )}

      {/* Quick actions */}
      <div className="flex flex-wrap gap-3">
        <Link href="/account/downloads" className="rounded-lg border border-slate-700 px-4 py-2 text-sm text-slate-200 hover:bg-slate-800 transition-colors flex items-center gap-2">
          <Download className="h-4 w-4" /> Download Android App
        </Link>
        <Link href="/account/support/new" className="rounded-lg bg-loomora-primary px-4 py-2 text-sm font-semibold text-white hover:bg-loomora-primary/90 transition-colors flex items-center gap-2">
          <Headphones className="h-4 w-4" /> Create Support Ticket
        </Link>
        <Link href="/pricing" className="rounded-lg border border-loomora-primary/40 px-4 py-2 text-sm text-loomora-container hover:bg-loomora-primary/10 transition-colors flex items-center gap-2">
          <Package className="h-4 w-4" /> View Pricing
        </Link>
      </div>

      {!process.env.DATABASE_URL && (
        <div className="rounded-lg border border-amber-700 bg-amber-950/40 p-4 text-sm text-amber-100">
          Database is not connected. Dashboard data will appear after DATABASE_URL is configured and migrations are applied.
        </div>
      )}
    </PortalShell>
  );
}

function DashboardCard({ icon: Icon, label, value, badge, href }: {
  icon: React.ComponentType<{ className?: string }>;
  label: string;
  value: string;
  badge?: React.ReactNode;
  href: string;
}) {
  return (
    <Link href={href} className="rounded-xl border border-slate-800 bg-slate-900/70 p-4 hover:border-slate-700 transition-colors block">
      <div className="flex items-center justify-between mb-2">
        <Icon className="h-5 w-5 text-slate-500" />
        {badge}
      </div>
      <p className="text-xs text-slate-400">{label}</p>
      <p className="text-lg font-semibold text-white mt-0.5">{value}</p>
    </Link>
  );
}
