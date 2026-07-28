import { PortalShell } from '@/components/PortalShell';
import { Badge, statusToBadgeVariant } from '@/components/Badge';
import { EmptyState } from '@/components/EmptyState';
import { StatusTable } from '@/components/StatusTable';
import { requireAdmin } from '@/lib/portal/authz';
import { listAllLicenses, type AdminLicenseRow } from '@/features/licenses/license-service';
import { FileKey } from 'lucide-react';

type AdminLicenseCapability = AdminLicenseRow['capabilities'][number];

const nav = [
  { href: '/admin', label: 'Dashboard' },
  { href: '/admin/licenses', label: 'Licenses' },
  { href: '/admin/orders', label: 'Orders' },
  { href: '/admin/audit', label: 'Audit Logs' },
];

export default async function AdminLicensesPage() {
  await requireAdmin();
  const hasDb = !!process.env.DATABASE_URL;
  const { licenses, total }: { licenses: AdminLicenseRow[]; total: number } = hasDb ? await listAllLicenses({ page: 1, pageSize: 50 }) : { licenses: [], total: 0 };

  return (
    <PortalShell title="License Management" description="Issue, reissue, renew, suspend, and inspect immutable license revisions. Customer endpoints cannot call the signer." nav={nav}>
      {/* License rules */}
      <StatusTable rows={[
        { label: 'Signing mode', value: 'Server-side only', note: 'Customer endpoints cannot call the signer.' },
        { label: 'Capabilities', value: 'Product names only', note: 'Runtime names such as LITERT_LM_PRO are forbidden.' },
        { label: 'Immutable revisions', value: 'Required', note: 'Reissue creates a new signed payload and hash.' },
      ]} />

      {/* License table */}
      {licenses.length === 0 ? (
        <EmptyState icon={FileKey} title="No licenses issued" description={hasDb ? 'Issue a license after confirming a customer order payment.' : 'Database is not connected.'} />
      ) : (
        <>
          <p className="text-xs text-slate-400">{total} total licenses</p>
          <div className="overflow-x-auto rounded-lg border border-slate-800">
            <table className="w-full text-left text-sm">
              <thead className="bg-slate-900 text-slate-300 border-b border-slate-800">
                <tr>
                  <th className="px-4 py-3 font-medium">License ID</th>
                  <th className="px-4 py-3 font-medium">Customer</th>
                  <th className="px-4 py-3 font-medium">Edition</th>
                  <th className="px-4 py-3 font-medium">Capabilities</th>
                  <th className="px-4 py-3 font-medium">Status</th>
                  <th className="px-4 py-3 font-medium">Revision</th>
                  <th className="px-4 py-3 font-medium">Created</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-800">
                {licenses.map((license) => (
                  <tr key={license.id} className="bg-slate-950/60 hover:bg-slate-900/80 transition-colors">
                    <td className="px-4 py-3 font-mono text-xs text-slate-200">{license.id.slice(0, 16)}…</td>
                    <td className="px-4 py-3 text-slate-300 text-xs">{license.customer?.name ?? license.customer?.email ?? '—'}</td>
                    <td className="px-4 py-3 text-slate-300 text-xs">{license.edition?.product?.name} {license.edition?.name}</td>
                    <td className="px-4 py-3 text-slate-400 text-xs">{license.capabilities?.map((c: AdminLicenseCapability) => c.capability.key).join(', ') || '—'}</td>
                    <td className="px-4 py-3"><Badge variant={statusToBadgeVariant(license.status)} label={license.status} /></td>
                    <td className="px-4 py-3 text-slate-300 text-xs">v{license.currentRevision}</td>
                    <td className="px-4 py-3 text-slate-400 text-xs">{new Date(license.createdAt).toLocaleDateString()}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </>
      )}

      <div className="rounded-lg border border-amber-700/50 bg-amber-950/20 p-4 text-xs text-amber-200">
        <strong>Offline limitation:</strong> Suspending a web record cannot instantly disable an already issued offline license on a disconnected Android device. Use expiry for offline control.
      </div>
    </PortalShell>
  );
}
