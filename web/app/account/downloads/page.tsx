import Link from 'next/link';
import { PortalShell } from '@/components/PortalShell';
import { EmptyState } from '@/components/EmptyState';
import { Badge } from '@/components/Badge';
import { requireSession } from '@/lib/portal/authz';
import { getPublishedReleases } from '@/features/downloads/download-service';
import { Download, Shield, Smartphone, Package } from 'lucide-react';

const nav = [
  { href: '/account', label: 'Dashboard' },
  { href: '/account/downloads', label: 'Downloads' },
];

export default async function DownloadsPage() {
  const session = await requireSession();
  const releases = process.env.DATABASE_URL ? await getPublishedReleases() : [];

  return (
    <PortalShell title="Downloads" description="Download the latest Loomora Android APK. Verify checksum and file size before sideloading." nav={nav}>
      {releases.length === 0 ? (
        <div className="space-y-4">
          <EmptyState
            icon={Package}
            title="No published releases in database"
            description="Published releases will appear here once an admin publishes an APK through the admin portal."
          />
          <div className="rounded-xl border border-slate-800 bg-slate-900/70 p-5 space-y-3">
            <h3 className="text-sm font-semibold text-white flex items-center gap-2">
              <Download className="h-4 w-4 text-loomora-secondary" /> Direct Download
            </h3>
            <p className="text-xs text-slate-400">Use the public download page for the latest APK.</p>
            <Link href="/download" className="inline-flex items-center gap-2 rounded-lg bg-loomora-primary px-4 py-2 text-sm font-semibold text-white hover:bg-loomora-primary/90 transition-colors">
              <Download className="h-4 w-4" /> Go to Download Page
            </Link>
          </div>
        </div>
      ) : (
        <div className="space-y-4">
          {releases.map((release) => (
            <div key={release.id} className="rounded-xl border border-slate-800 bg-slate-900/70 p-5">
              <div className="flex items-start justify-between mb-3">
                <div>
                  <h3 className="text-sm font-semibold text-white flex items-center gap-2">
                    <Smartphone className="h-4 w-4 text-loomora-secondary" />
                    Loomora v{release.versionName}
                  </h3>
                  <p className="text-xs text-slate-400 mt-0.5">
                    Build {release.versionCode} · {release.channel} · Android {release.minimumAndroid}+
                  </p>
                </div>
                <Badge variant="active" label={release.channel} />
              </div>

              <p className="text-xs text-slate-300 mb-3">{release.releaseNotes}</p>

              <div className="grid grid-cols-2 sm:grid-cols-3 gap-3 text-xs text-slate-400 mb-3">
                <div>
                  <span className="text-slate-500">Size</span>
                  <p className="mt-0.5">{(Number(release.fileSizeBytes) / 1024 / 1024).toFixed(1)} MB</p>
                </div>
                <div>
                  <span className="text-slate-500">ABIs</span>
                  <p className="mt-0.5">{release.supportedAbis}</p>
                </div>
                <div className="col-span-2 sm:col-span-1">
                  <span className="text-slate-500">SHA-256</span>
                  <p className="mt-0.5 font-mono text-[10px] break-all">{release.checksumSha256}</p>
                </div>
              </div>

              <Link
                href="/download"
                className="inline-flex items-center gap-2 rounded-lg bg-loomora-primary px-4 py-2 text-xs font-semibold text-white hover:bg-loomora-primary/90 transition-colors"
              >
                <Download className="h-3.5 w-3.5" /> Download APK
              </Link>
            </div>
          ))}
        </div>
      )}

      <div className="rounded-lg border border-amber-700/50 bg-amber-950/20 p-4 text-xs text-amber-200 space-y-1">
        <p className="font-semibold flex items-center gap-1.5"><Shield className="h-3.5 w-3.5" /> Android Sideloading</p>
        <p>APK files downloaded outside Google Play require enabling &quot;Install Unknown Apps&quot; in Android Settings. Android will warn about unverified sources — this is expected for sideloaded APKs. Always verify the SHA-256 checksum matches.</p>
      </div>
    </PortalShell>
  );
}
