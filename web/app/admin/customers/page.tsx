import { PortalShell } from '@/components/PortalShell';
import { Badge, statusToBadgeVariant } from '@/components/Badge';
import { EmptyState } from '@/components/EmptyState';
import { requireAdmin } from '@/lib/portal/authz';
import { listCustomers } from '@/features/customers/customer-service';
import { Users } from 'lucide-react';

const nav = [
  { href: '/admin', label: 'Dashboard' },
  { href: '/admin/customers', label: 'Customers' },
  { href: '/admin/users', label: 'Users & Roles' },
];

export default async function AdminCustomersPage() {
  await requireAdmin();
  const hasDb = !!process.env.DATABASE_URL;
  const { customers, total } = hasDb ? await listCustomers({ page: 1, pageSize: 50 }) : { customers: [], total: 0 };

  return (
    <PortalShell title="Customers" description="Search, filter, and view customer records. Customer data is scoped server-side." nav={nav}>
      {customers.length === 0 ? (
        <EmptyState icon={Users} title="No customers yet" description={hasDb ? 'Customers will appear after they sign in with Google.' : 'Database is not connected. Configure DATABASE_URL.'} />
      ) : (
        <>
          <p className="text-xs text-slate-400">{total} total customers</p>
          <div className="overflow-x-auto rounded-lg border border-slate-800">
            <table className="w-full text-left text-sm">
              <thead className="bg-slate-900 text-slate-300 border-b border-slate-800">
                <tr>
                  <th className="px-4 py-3 font-medium">Name</th>
                  <th className="px-4 py-3 font-medium">Email</th>
                  <th className="px-4 py-3 font-medium">Role</th>
                  <th className="px-4 py-3 font-medium">Company</th>
                  <th className="px-4 py-3 font-medium">Last Login</th>
                  <th className="px-4 py-3 font-medium">Joined</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-800">
                {customers.map((customer) => (
                  <tr key={customer.id} className="bg-slate-950/60 hover:bg-slate-900/80 transition-colors">
                    <td className="px-4 py-3 text-slate-200">{customer.name ?? '—'}</td>
                    <td className="px-4 py-3 text-slate-300 text-xs font-mono">{customer.email}</td>
                    <td className="px-4 py-3"><Badge variant={customer.role === 'SUPER_ADMIN' ? 'warning' : customer.role === 'ADMIN' ? 'info' : 'draft'} label={customer.role} /></td>
                    <td className="px-4 py-3 text-slate-400 text-xs">{customer.customerProfile?.company ?? '—'}</td>
                    <td className="px-4 py-3 text-slate-400 text-xs">{customer.lastLoginAt ? new Date(customer.lastLoginAt).toLocaleDateString() : '—'}</td>
                    <td className="px-4 py-3 text-slate-400 text-xs">{new Date(customer.createdAt).toLocaleDateString()}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </>
      )}
    </PortalShell>
  );
}
