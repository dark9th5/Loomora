import Link from 'next/link';
import { PortalShell } from '@/components/PortalShell';
import { Badge, statusToBadgeVariant } from '@/components/Badge';
import { EmptyState } from '@/components/EmptyState';
import { requireSession } from '@/lib/portal/authz';
import { listTicketsByCustomer, type CustomerTicket } from '@/features/support/support-service';
import { MessageSquare, Plus } from 'lucide-react';

const nav = [
  { href: '/account', label: 'Dashboard' },
  { href: '/account/licenses', label: 'My licenses' },
  { href: '/account/orders', label: 'Orders' },
  { href: '/account/support', label: 'Support tickets' },
];

export default async function SupportPage() {
  const session = await requireSession();
  const userId = session.user?.id;
  const tickets: CustomerTicket[] = userId && process.env.DATABASE_URL ? await listTicketsByCustomer(userId) : [];

  return (
    <PortalShell title="Support Tickets" description="View your support requests. Only tickets owned by your account are shown." nav={nav}>
      <div className="flex justify-end">
        <Link href="/account/support/new" className="rounded-lg bg-loomora-primary px-4 py-2 text-sm font-semibold text-white hover:bg-loomora-primary/90 transition-colors flex items-center gap-2">
          <Plus className="h-4 w-4" /> New Ticket
        </Link>
      </div>

      {tickets.length === 0 ? (
        <EmptyState
          icon={MessageSquare}
          title="No support tickets"
          description="Need help? Create a new support ticket and we'll get back to you."
        />
      ) : (
        <div className="space-y-3">
          {tickets.map((ticket) => (
            <div key={ticket.id} className="rounded-xl border border-slate-800 bg-slate-900/70 p-4 hover:border-slate-700 transition-colors">
              <div className="flex items-start justify-between gap-4">
                <div className="min-w-0 flex-1">
                  <h3 className="text-sm font-semibold text-white truncate">{ticket.subject}</h3>
                  <p className="text-xs text-slate-400 mt-0.5">{ticket.topic} · {new Date(ticket.createdAt).toLocaleDateString()}</p>
                  {ticket.messages[0] && (
                    <p className="text-xs text-slate-500 mt-1 truncate">{ticket.messages[0].body}</p>
                  )}
                </div>
                <Badge variant={statusToBadgeVariant(ticket.status)} label={ticket.status} />
              </div>
            </div>
          ))}
        </div>
      )}
    </PortalShell>
  );
}
