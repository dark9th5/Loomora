import { PortalShell } from '@/components/PortalShell';
import { Badge, statusToBadgeVariant } from '@/components/Badge';
import { EmptyState } from '@/components/EmptyState';
import { requireAdmin } from '@/lib/portal/authz';
import { listAllTickets, type AdminTicketRow } from '@/features/support/support-service';
import { MessageSquare } from 'lucide-react';

const nav = [
  { href: '/admin', label: 'Dashboard' },
  { href: '/admin/support', label: 'Support' },
];

export default async function AdminSupportPage() {
  await requireAdmin();
  const hasDb = !!process.env.DATABASE_URL;
  const { tickets, total }: { tickets: AdminTicketRow[]; total: number } = hasDb ? await listAllTickets({ page: 1, pageSize: 50 }) : { tickets: [], total: 0 };

  return (
    <PortalShell title="Support Tickets" description="View and manage all customer support tickets. Reply and update statuses via the API." nav={nav}>
      {tickets.length === 0 ? (
        <EmptyState icon={MessageSquare} title="No support tickets" description={hasDb ? 'Customer tickets will appear after they submit through the portal.' : 'Database is not connected.'} />
      ) : (
        <>
          <p className="text-xs text-slate-400">{total} total tickets</p>
          <div className="overflow-x-auto rounded-lg border border-slate-800">
            <table className="w-full text-left text-sm">
              <thead className="bg-slate-900 text-slate-300 border-b border-slate-800">
                <tr>
                  <th className="px-4 py-3 font-medium">Subject</th>
                  <th className="px-4 py-3 font-medium">Customer</th>
                  <th className="px-4 py-3 font-medium">Topic</th>
                  <th className="px-4 py-3 font-medium">Status</th>
                  <th className="px-4 py-3 font-medium">Last Message</th>
                  <th className="px-4 py-3 font-medium">Updated</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-800">
                {tickets.map((ticket) => (
                  <tr key={ticket.id} className="bg-slate-950/60 hover:bg-slate-900/80 transition-colors">
                    <td className="px-4 py-3 text-slate-200 max-w-[200px] truncate">{ticket.subject}</td>
                    <td className="px-4 py-3 text-slate-300 text-xs">{ticket.customer?.name ?? ticket.email}</td>
                    <td className="px-4 py-3 text-slate-400 text-xs">{ticket.topic}</td>
                    <td className="px-4 py-3"><Badge variant={statusToBadgeVariant(ticket.status)} label={ticket.status} /></td>
                    <td className="px-4 py-3 text-slate-400 text-xs max-w-[200px] truncate">{ticket.messages?.[0]?.body ?? '—'}</td>
                    <td className="px-4 py-3 text-slate-400 text-xs">{new Date(ticket.updatedAt).toLocaleDateString()}</td>
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
