import { PortalShell } from '@/components/PortalShell';
import { Badge, statusToBadgeVariant } from '@/components/Badge';
import { EmptyState } from '@/components/EmptyState';
import { requireAdmin } from '@/lib/portal/authz';
import { listAllOrders } from '@/features/orders/order-service';
import { ShoppingCart } from 'lucide-react';

const nav = [
  { href: '/admin', label: 'Dashboard' },
  { href: '/admin/orders', label: 'Orders' },
  { href: '/admin/licenses', label: 'Licenses' },
  { href: '/admin/audit', label: 'Audit Logs' },
];

export default async function AdminOrdersPage() {
  await requireAdmin();
  const hasDb = !!process.env.DATABASE_URL;
  const { orders, total } = hasDb ? await listAllOrders({ page: 1, pageSize: 50 }) : { orders: [], total: 0 };

  return (
    <PortalShell title="Orders" description="Manual payment confirmation requires an admin actor, reason, and audit log. The buy button does not auto-grant Pro." nav={nav}>
      {orders.length === 0 ? (
        <EmptyState icon={ShoppingCart} title="No orders yet" description={hasDb ? 'Customer orders will appear after they place orders through the portal.' : 'Database is not connected.'} />
      ) : (
        <>
          <p className="text-xs text-slate-400">{total} total orders</p>
          <div className="overflow-x-auto rounded-lg border border-slate-800">
            <table className="w-full text-left text-sm">
              <thead className="bg-slate-900 text-slate-300 border-b border-slate-800">
                <tr>
                  <th className="px-4 py-3 font-medium">Order ID</th>
                  <th className="px-4 py-3 font-medium">Customer</th>
                  <th className="px-4 py-3 font-medium">Items</th>
                  <th className="px-4 py-3 font-medium">Total</th>
                  <th className="px-4 py-3 font-medium">Status</th>
                  <th className="px-4 py-3 font-medium">Payments</th>
                  <th className="px-4 py-3 font-medium">Date</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-800">
                {orders.map((order) => (
                  <tr key={order.id} className="bg-slate-950/60 hover:bg-slate-900/80 transition-colors">
                    <td className="px-4 py-3 font-mono text-xs text-slate-200">{order.id.slice(0, 12)}…</td>
                    <td className="px-4 py-3 text-slate-300 text-xs">{order.customer?.name ?? order.customer?.email ?? '—'}</td>
                    <td className="px-4 py-3 text-slate-300 text-xs">{order.items?.map((i) => `${i.edition?.product?.name ?? ''} ${i.edition?.name ?? ''}`).join(', ')}</td>
                    <td className="px-4 py-3 text-slate-200 text-xs">${(order.totalCents / 100).toFixed(2)} {order.currency}</td>
                    <td className="px-4 py-3"><Badge variant={statusToBadgeVariant(order.status)} label={order.status} /></td>
                    <td className="px-4 py-3 text-slate-400 text-xs">{order.payments?.length ?? 0} record(s)</td>
                    <td className="px-4 py-3 text-slate-400 text-xs">{new Date(order.createdAt).toLocaleDateString()}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </>
      )}

      <div className="rounded-lg border border-slate-800 bg-slate-900/70 p-4 text-xs text-slate-400 space-y-1">
        <p><strong className="text-slate-200">Order Statuses:</strong> DRAFT, PENDING_PAYMENT, PAID_MANUALLY, PAID, CANCELLED, REFUNDED</p>
        <p>Payment confirmation is done via the <code className="bg-slate-800 px-1 py-0.5 rounded text-white">POST /api/admin/orders/[orderId]/confirm</code> endpoint requiring a reason and creating an audit log.</p>
      </div>
    </PortalShell>
  );
}
