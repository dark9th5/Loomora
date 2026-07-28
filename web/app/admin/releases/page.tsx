import { PortalShell } from '@/components/PortalShell';
import { Badge, statusToBadgeVariant } from '@/components/Badge';
import { EmptyState } from '@/components/EmptyState';
import { requireAdmin } from '@/lib/portal/authz';
import { listAllReleases } from '@/features/downloads/download-service';
import { Package } from 'lucide-react';

const nav = [
  { href: '/admin', label: 'Dashboard' },
  { href: '/admin/releases', label: 'App Releases' },
];

export default async function AdminReleasesPage() {
  await requireAdmin();
  const hasDb = !!process.env.DATABASE_URL;
  const { releases, total } = hasDb ? await listAllReleases({ page: 1, pageSize: 50 }) : { releases: [], total: 0 };

  return (
    <PortalShell title="App Releases" description="Publish Android APK releases. Only published artifacts appear publicly. Never upload the Android signing key." nav={nav}>
      {releases.length === 0 ? (
        <EmptyState icon={Package} title="No releases" description={hasDb ? 'Publish your first Android release through the API.' : 'Database is not connected.'} />
      ) : (
        <>
          <p className="text-xs text-slate-400">{total} total releases</p>
          <div className="overflow-x-auto rounded-lg border border-slate-800">
            <table className="w-full text-left text-sm">
              <thead className="bg-slate-900 text-slate-300 border-b border-slate-800">
                <tr>
                  <th className="px-4 py-3 font-medium">Version</th>
                  <th className="px-4 py-3 font-medium">Code</th>
                  <th className="px-4 py-3 font-medium">Channel</th>
                  <th className="px-4 py-3 font-medium">Status</th>
                  <th className="px-4 py-3 font-medium">Size</th>
                  <th className="px-4 py-3 font-medium">Min Android</th>
                  <th className="px-4 py-3 font-medium">Published</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-800">
                {releases.map((release) => (
                  <tr key={release.id} className="bg-slate-950/60 hover:bg-slate-900/80 transition-colors">
                    <td className="px-4 py-3 text-slate-200 font-semibold">v{release.versionName}</td>
                    <td className="px-4 py-3 text-slate-300 text-xs">{release.versionCode}</td>
                    <td className="px-4 py-3"><Badge variant={release.channel === 'STABLE' ? 'active' : release.channel === 'BETA' ? 'pending' : 'draft'} label={release.channel} /></td>
                    <td className="px-4 py-3"><Badge variant={statusToBadgeVariant(release.status)} label={release.status} /></td>
                    <td className="px-4 py-3 text-slate-400 text-xs">{(Number(release.fileSizeBytes) / 1024 / 1024).toFixed(1)} MB</td>
                    <td className="px-4 py-3 text-slate-400 text-xs">{release.minimumAndroid}</td>
                    <td className="px-4 py-3 text-slate-400 text-xs">{release.publishedAt ? new Date(release.publishedAt).toLocaleDateString() : '—'}</td>
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
