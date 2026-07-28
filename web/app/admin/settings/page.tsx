import { PortalShell } from '@/components/PortalShell';
import { StatusTable } from '@/components/StatusTable';
import { requireSuperAdmin } from '@/lib/portal/authz';
import { Shield, Settings } from 'lucide-react';

const nav = [
  { href: '/admin', label: 'Dashboard' },
  { href: '/admin/settings', label: 'Settings' },
];

export default async function AdminSettingsPage() {
  await requireSuperAdmin();

  return (
    <PortalShell title="System Settings" description="Super Admin only. Configure system-wide settings, license signing mode, and notification preferences." nav={nav}>
      <div className="rounded-lg border border-loomora-primary/30 bg-loomora-primary/10 p-4 text-xs text-loomora-container flex items-center gap-2">
        <Shield className="h-4 w-4" /> Settings changes require Super Admin role and create audit logs.
      </div>

      <StatusTable rows={[
        { label: 'License signing mode', value: 'encrypted-env', note: 'Private key in Vercel Sensitive Env Var. External KMS/HSM preferred for production.' },
        { label: 'Payment mode', value: 'Manual', note: 'Admin confirms payment manually. No auto-grant Pro.' },
        { label: 'Super Admin email', value: process.env.SUPER_ADMIN_EMAIL ?? 'giolanhluc@gmail.com', note: 'Bootstrap Super Admin. Cannot be removed.' },
        { label: 'Database', value: process.env.DATABASE_URL ? '● Connected' : '✕ Not configured', note: 'PostgreSQL from Vercel Marketplace or compatible.' },
        { label: 'Blob storage', value: process.env.BLOB_READ_WRITE_TOKEN ? '● Configured' : '✕ Not configured', note: 'For APK and license artifact storage.' },
        { label: 'Auth secret', value: process.env.AUTH_SECRET ? '● Set' : '✕ Missing', note: 'Required for session signing.' },
      ]} />

      <div className="rounded-xl border border-slate-800 bg-slate-900/70 p-4 space-y-2">
        <h3 className="text-sm font-semibold text-white flex items-center gap-2"><Settings className="h-4 w-4 text-loomora-secondary" /> Environment Variables</h3>
        <p className="text-xs text-slate-400">System settings are managed through Vercel environment variables. See DEPLOYMENT.md for the complete list.</p>
        <p className="text-xs text-slate-400">Custom settings can be stored in the SystemSetting table via <code className="bg-slate-800 px-1 py-0.5 rounded text-white">PUT /api/admin/settings</code>.</p>
      </div>
    </PortalShell>
  );
}
