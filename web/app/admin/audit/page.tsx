import { PortalShell } from '@/components/PortalShell';
import { EmptyState } from '@/components/EmptyState';
import { requireAdmin } from '@/lib/portal/authz';
import { listAuditLogs } from '@/features/audit/audit-service';
import { ScrollText } from 'lucide-react';

const nav = [
  { href: '/admin', label: 'Dashboard' },
  { href: '/admin/audit', label: 'Audit Logs' },
];

export default async function AdminAuditPage() {
  await requireAdmin();
  const hasDb = !!process.env.DATABASE_URL;
  const { logs, total } = hasDb ? await listAuditLogs({ page: 1, pageSize: 100 }) : { logs: [], total: 0 };

  return (
    <PortalShell title="Audit Logs" description="Immutable record of all sensitive actions: role changes, license issuance, payment confirmations, and more." nav={nav}>
      {logs.length === 0 ? (
        <EmptyState icon={ScrollText} title="No audit logs" description={hasDb ? 'Audit entries are created automatically when sensitive actions occur.' : 'Database is not connected.'} />
      ) : (
        <>
          <p className="text-xs text-slate-400">{total} total audit entries</p>
          <div className="overflow-x-auto rounded-lg border border-slate-800">
            <table className="w-full text-left text-sm">
              <thead className="bg-slate-900 text-slate-300 border-b border-slate-800">
                <tr>
                  <th className="px-4 py-3 font-medium">Timestamp</th>
                  <th className="px-4 py-3 font-medium">Actor</th>
                  <th className="px-4 py-3 font-medium">Action</th>
                  <th className="px-4 py-3 font-medium">Entity</th>
                  <th className="px-4 py-3 font-medium">Entity ID</th>
                  <th className="px-4 py-3 font-medium">Details</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-800">
                {logs.map((log) => (
                  <tr key={log.id} className="bg-slate-950/60">
                    <td className="px-4 py-3 text-slate-400 text-xs whitespace-nowrap">{new Date(log.createdAt).toLocaleString()}</td>
                    <td className="px-4 py-3 text-slate-300 text-xs">{log.actor?.name ?? log.actor?.email ?? log.actorUserId ?? 'System'}</td>
                    <td className="px-4 py-3 text-slate-200 text-xs font-mono">{log.action}</td>
                    <td className="px-4 py-3 text-slate-400 text-xs">{log.entityType}</td>
                    <td className="px-4 py-3 text-slate-400 text-xs font-mono">{log.entityId ? `${log.entityId.slice(0, 12)}…` : '—'}</td>
                    <td className="px-4 py-3 text-slate-500 text-xs max-w-[200px] truncate">{log.metadata ? JSON.stringify(log.metadata) : '—'}</td>
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
