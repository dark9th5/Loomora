import { PortalShell } from '@/components/PortalShell';
import { Badge } from '@/components/Badge';
import { requireAdmin } from '@/lib/portal/authz';
import { getUserStats } from '@/features/auth/auth-service';
import { getOrderStats } from '@/features/orders/order-service';
import { getTicketStats } from '@/features/support/support-service';
import { Users, FileKey, ShoppingCart, MessageSquare, BarChart3, Activity, AlertTriangle } from 'lucide-react';

const nav = [
  { href: '/admin', label: 'Dashboard' },
  { href: '/admin/customers', label: 'Customers' },
  { href: '/admin/users', label: 'Users & Roles' },
  { href: '/admin/licenses', label: 'Licenses' },
  { href: '/admin/orders', label: 'Orders' },
  { href: '/admin/products', label: 'Products' },
  { href: '/admin/releases', label: 'App Releases' },
  { href: '/admin/support', label: 'Support' },
  { href: '/admin/contact-leads', label: 'Contact Leads' },
  { href: '/admin/content', label: 'Blog/Content' },
  { href: '/admin/audit', label: 'Audit Logs' },
  { href: '/admin/settings', label: 'Settings' },
];

export default async function AdminDashboardPage() {
  const session = await requireAdmin();
  const hasDb = !!process.env.DATABASE_URL;

  const [userStats, orderStats, ticketStats] = hasDb
    ? await Promise.all([getUserStats(), getOrderStats(), getTicketStats()])
    : [{ total: 0, newThisMonth: 0, superAdmins: 0 }, { pending: 0, total: 0, revenue: 0 }, { open: 0, inProgress: 0, total: 0 }];

  return (
    <PortalShell title="Admin Dashboard" description="Revenue and counts are from real order data only — no placeholder charts or fake revenue." nav={nav}>
      {/* Stat Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <StatCard icon={Users} label="Total Customers" value={String(userStats.total)} sub={`${userStats.newThisMonth} new this month`} />
        <StatCard icon={ShoppingCart} label="Pending Orders" value={String(orderStats.pending)} sub={`${orderStats.total} total orders`} />
        <StatCard icon={BarChart3} label="Revenue" value={`$${(orderStats.revenue / 100).toFixed(2)}`} sub="From confirmed payments only" />
        <StatCard icon={MessageSquare} label="Open Tickets" value={String(ticketStats.open)} sub={`${ticketStats.inProgress} in progress`} />
      </div>

      {/* Offline limitation notice */}
      <div className="rounded-lg border border-amber-700 bg-amber-950/40 p-4 text-sm text-amber-100 flex items-start gap-3">
        <AlertTriangle className="h-5 w-5 text-amber-400 shrink-0 mt-0.5" />
        <div>
          <p className="font-semibold">Offline License Limitation</p>
          <p className="text-xs text-amber-200 mt-1">Suspending a web record cannot instantly disable an already issued offline license on a disconnected Android device. Use expiry windows for offline control. Future online refresh/revocation is out of scope.</p>
        </div>
      </div>

      {/* Quick Status */}
      <div className="rounded-xl border border-slate-800 bg-slate-900/70 p-4">
        <h3 className="text-sm font-semibold text-white mb-3 flex items-center gap-2"><Activity className="h-4 w-4 text-loomora-secondary" /> System Status</h3>
        <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 text-xs">
          <div className="flex items-center gap-2">
            <Badge variant={hasDb ? 'active' : 'error'} label={hasDb ? '● Database' : '✕ No DB'} />
          </div>
          <div className="flex items-center gap-2">
            <Badge variant="info" label={`${userStats.superAdmins} Super Admin(s)`} />
          </div>
          <div className="flex items-center gap-2">
            <Badge variant="info" label={`Role: ${session.user?.role}`} />
          </div>
          <div className="flex items-center gap-2">
            <Badge variant="active" label="● Auth OK" />
          </div>
        </div>
      </div>

      <p className="text-xs text-slate-500">Signed in as {session.user?.email}; role {session.user?.role}.</p>
    </PortalShell>
  );
}

function StatCard({ icon: Icon, label, value, sub }: {
  icon: React.ComponentType<{ className?: string }>;
  label: string;
  value: string;
  sub: string;
}) {
  return (
    <div className="rounded-xl border border-slate-800 bg-slate-900/70 p-4">
      <Icon className="h-5 w-5 text-slate-500 mb-2" />
      <p className="text-xs text-slate-400">{label}</p>
      <p className="text-2xl font-bold text-white mt-0.5">{value}</p>
      <p className="text-xs text-slate-500 mt-1">{sub}</p>
    </div>
  );
}
