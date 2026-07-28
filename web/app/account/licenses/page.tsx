import Link from 'next/link';
import { PortalShell } from '@/components/PortalShell';
import { Badge, statusToBadgeVariant } from '@/components/Badge';
import { EmptyState } from '@/components/EmptyState';
import { requireSession } from '@/lib/portal/authz';
import { getLicensesByCustomer } from '@/features/licenses/license-service';
import { FileKey, Download } from 'lucide-react';

const nav = [
  { href: '/account', label: 'Dashboard' },
  { href: '/account/licenses', label: 'My licenses' },
  { href: '/account/orders', label: 'Orders' },
  { href: '/account/support', label: 'Support tickets' },
];

export default async function LicensesPage() {
  const session = await requireSession();
  const userId = session.user?.id;
  const licenses = userId && process.env.DATABASE_URL ? await getLicensesByCustomer(userId) : [];

  return (
    <PortalShell title="My Licenses" description="Only licenses owned by your account are shown. Download your signed .license file to import into the Android app." nav={nav}>
      {licenses.length === 0 ? (
        <EmptyState
          icon={FileKey}
          title="No licenses yet"
          description="Purchase a Pro license to unlock offline transcription, smart insights, and more."
          action={
            <Link href="/pricing" className="rounded-lg bg-loomora-primary px-4 py-2 text-sm font-semibold text-white hover:bg-loomora-primary/90 transition-colors">
              View Pricing
            </Link>
          }
        />
      ) : (
        <div className="space-y-4">
          {licenses.map((license) => (
            <div key={license.id} className="rounded-xl border border-slate-800 bg-slate-900/70 p-5">
              <div className="flex items-center justify-between mb-3">
                <div className="flex items-center gap-3">
                  <FileKey className="h-5 w-5 text-loomora-secondary" />
                  <div>
                    <h3 className="text-sm font-semibold text-white">
                      {license.edition?.product?.name} — {license.edition?.name}
                    </h3>
                    <p className="text-xs text-slate-400 font-mono">{license.id}</p>
                  </div>
                </div>
                <Badge variant={statusToBadgeVariant(license.status)} label={license.status} />
              </div>

              <div className="grid grid-cols-2 sm:grid-cols-4 gap-4 text-xs text-slate-300 mb-3">
                <div>
                  <span className="text-slate-500">Capabilities</span>
                  <p className="mt-0.5">{license.capabilities.map((c) => c.capability.key).join(', ') || 'N/A'}</p>
                </div>
                <div>
                  <span className="text-slate-500">Revision</span>
                  <p className="mt-0.5">v{license.currentRevision}</p>
                </div>
                <div>
                  <span className="text-slate-500">Device Binding</span>
                  <p className="mt-0.5">{license.deviceBinding?.label ?? 'None'}</p>
                </div>
                <div>
                  <span className="text-slate-500">Created</span>
                  <p className="mt-0.5">{new Date(license.createdAt).toLocaleDateString()}</p>
                </div>
              </div>

              {license.status === 'ACTIVE' && (
                <Link
                  href={`/api/account/licenses/${license.id}/download`}
                  className="inline-flex items-center gap-1.5 rounded-lg bg-loomora-primary/20 border border-loomora-primary/30 px-3 py-1.5 text-xs font-semibold text-loomora-container hover:bg-loomora-primary/30 transition-colors"
                >
                  <Download className="h-3.5 w-3.5" /> Download .license file
                </Link>
              )}
            </div>
          ))}
        </div>
      )}
    </PortalShell>
  );
}
