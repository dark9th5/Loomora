import Link from 'next/link';
import { PortalShell } from '@/components/PortalShell';
import { Badge, statusToBadgeVariant } from '@/components/Badge';
import { EmptyState } from '@/components/EmptyState';
import { requireSession } from '@/lib/portal/authz';
import { listOrdersByCustomer } from '@/features/orders/order-service';
import { ShoppingCart } from 'lucide-react';

const nav = [
  { href: '/account', label: 'Dashboard' },
  { href: '/account/licenses', label: 'My licenses' },
  { href: '/account/orders', label: 'Orders' },
  { href: '/account/support', label: 'Support tickets' },
];

export default async function OrdersPage() {
  const session = await requireSession();
  const userId = session.user?.id;
  const orders = userId && process.env.DATABASE_URL ? await listOrdersByCustomer(userId) : [];

  return (
    <PortalShell title="My Orders" description="View your order history. Payment confirmation is handled manually by an admin — Pro is never granted automatically." nav={nav}>
      {orders.length === 0 ? (
        <EmptyState
          icon={ShoppingCart}
          title="No orders yet"
          description="Browse our pricing plans and place an order to get started."
          action={
            <Link href="/pricing" className="rounded-lg bg-loomora-primary px-4 py-2 text-sm font-semibold text-white hover:bg-loomora-primary/90 transition-colors">
              View Pricing
            </Link>
          }
        />
      ) : (
        <div className="overflow-x-auto rounded-lg border border-slate-800">
          <table className="w-full text-left text-sm">
            <thead className="bg-slate-900 text-slate-300 border-b border-slate-800">
              <tr>
                <th className="px-4 py-3 font-medium">Order ID</th>
                <th className="px-4 py-3 font-medium">Items</th>
                <th className="px-4 py-3 font-medium">Total</th>
                <th className="px-4 py-3 font-medium">Status</th>
                <th className="px-4 py-3 font-medium">Date</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-800">
              {orders.map((order) => (
                <tr key={order.id} className="bg-slate-950/60">
                  <td className="px-4 py-3 font-mono text-xs text-slate-200">{order.id.slice(0, 12)}…</td>
                  <td className="px-4 py-3 text-slate-300">
                    {order.items.map((item) => `${item.edition?.product?.name ?? ''} ${item.edition?.name ?? ''}`).join(', ')}
                  </td>
                  <td className="px-4 py-3 text-slate-200">${(order.totalCents / 100).toFixed(2)} {order.currency}</td>
                  <td className="px-4 py-3"><Badge variant={statusToBadgeVariant(order.status)} label={order.status} /></td>
                  <td className="px-4 py-3 text-slate-400 text-xs">{new Date(order.createdAt).toLocaleDateString()}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      <div className="rounded-lg border border-amber-700/50 bg-amber-950/20 p-4 text-xs text-amber-200">
        <strong>Manual payment flow:</strong> After placing an order, contact support with your payment details. An admin will confirm payment and issue your license separately. The buy button does not grant Pro automatically.
      </div>
    </PortalShell>
  );
}
