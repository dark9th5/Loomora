import { PortalShell } from '@/components/PortalShell';
import { EmptyState } from '@/components/EmptyState';
import { requireAdmin } from '@/lib/portal/authz';
import { listContactLeads } from '@/features/content/content-service';
import { Mail } from 'lucide-react';

const nav = [
  { href: '/admin', label: 'Dashboard' },
  { href: '/admin/contact-leads', label: 'Contact Leads' },
];

export default async function AdminContactLeadsPage() {
  await requireAdmin();
  const hasDb = !!process.env.DATABASE_URL;
  const { leads, total } = hasDb ? await listContactLeads({ page: 1, pageSize: 50 }) : { leads: [], total: 0 };

  return (
    <PortalShell title="Contact Leads" description="Submitted contact form entries from the public website." nav={nav}>
      {leads.length === 0 ? (
        <EmptyState icon={Mail} title="No contact leads" description={hasDb ? 'Contact form submissions will appear here.' : 'Database not connected.'} />
      ) : (
        <>
          <p className="text-xs text-slate-400">{total} total leads</p>
          <div className="overflow-x-auto rounded-lg border border-slate-800">
            <table className="w-full text-left text-sm">
              <thead className="bg-slate-900 text-slate-300 border-b border-slate-800">
                <tr>
                  <th className="px-4 py-3 font-medium">Name</th>
                  <th className="px-4 py-3 font-medium">Email</th>
                  <th className="px-4 py-3 font-medium">Company</th>
                  <th className="px-4 py-3 font-medium">Topic</th>
                  <th className="px-4 py-3 font-medium">Message</th>
                  <th className="px-4 py-3 font-medium">Consent</th>
                  <th className="px-4 py-3 font-medium">Date</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-800">
                {leads.map((lead) => (
                  <tr key={lead.id} className="bg-slate-950/60">
                    <td className="px-4 py-3 text-slate-200 text-xs">{lead.name}</td>
                    <td className="px-4 py-3 text-slate-300 text-xs font-mono">{lead.email}</td>
                    <td className="px-4 py-3 text-slate-400 text-xs">{lead.company ?? '—'}</td>
                    <td className="px-4 py-3 text-slate-400 text-xs">{lead.topic}</td>
                    <td className="px-4 py-3 text-slate-400 text-xs max-w-[200px] truncate">{lead.message}</td>
                    <td className="px-4 py-3 text-slate-400 text-xs">{lead.consent ? '✓' : '✕'}</td>
                    <td className="px-4 py-3 text-slate-400 text-xs">{new Date(lead.createdAt).toLocaleDateString()}</td>
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
