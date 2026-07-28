import { PortalShell } from '@/components/PortalShell';
import { Badge } from '@/components/Badge';
import { EmptyState } from '@/components/EmptyState';
import { requireSuperAdmin } from '@/lib/portal/authz';
import { listUsers } from '@/features/auth/auth-service';
import { Users, Shield } from 'lucide-react';

const nav = [
  { href: '/admin', label: 'Dashboard' },
  { href: '/admin/users', label: 'Users & Roles' },
  { href: '/admin/audit', label: 'Audit Logs' },
];

export default async function AdminUsersPage() {
  const session = await requireSuperAdmin();
  const hasDb = !!process.env.DATABASE_URL;
  const { users, total } = hasDb ? await listUsers({ page: 1, pageSize: 50 }) : { users: [], total: 0 };

  return (
    <PortalShell title="Users & Roles" description="Super Admin only. Promote/demote users. The final active Super Admin cannot be removed or demoted." nav={nav}>
      <div className="rounded-lg border border-loomora-primary/30 bg-loomora-primary/10 p-4 text-xs text-loomora-container flex items-center gap-2">
        <Shield className="h-4 w-4" /> This page is restricted to Super Admins only. Role changes create audit logs.
      </div>

      {users.length === 0 ? (
        <EmptyState icon={Users} title="No users" description={hasDb ? 'Users appear after signing in with Google.' : 'Database is not connected.'} />
      ) : (
        <>
          <p className="text-xs text-slate-400">{total} total users</p>
          <div className="overflow-x-auto rounded-lg border border-slate-800">
            <table className="w-full text-left text-sm">
              <thead className="bg-slate-900 text-slate-300 border-b border-slate-800">
                <tr>
                  <th className="px-4 py-3 font-medium">Name</th>
                  <th className="px-4 py-3 font-medium">Email</th>
                  <th className="px-4 py-3 font-medium">Role</th>
                  <th className="px-4 py-3 font-medium">Status</th>
                  <th className="px-4 py-3 font-medium">Last Login</th>
                  <th className="px-4 py-3 font-medium">Joined</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-800">
                {users.map((user) => (
                  <tr key={user.id} className="bg-slate-950/60 hover:bg-slate-900/80 transition-colors">
                    <td className="px-4 py-3 text-slate-200">{user.name ?? '—'}</td>
                    <td className="px-4 py-3 text-slate-300 text-xs font-mono">{user.email}</td>
                    <td className="px-4 py-3">
                      <Badge
                        variant={user.role === 'SUPER_ADMIN' ? 'warning' : user.role === 'ADMIN' ? 'info' : user.role === 'SUPPORT' ? 'pending' : 'draft'}
                        label={user.role}
                      />
                    </td>
                    <td className="px-4 py-3">
                      <Badge variant={user.disabledAt ? 'suspended' : 'active'} label={user.disabledAt ? '⊘ Disabled' : '● Active'} />
                    </td>
                    <td className="px-4 py-3 text-slate-400 text-xs">{user.lastLoginAt ? new Date(user.lastLoginAt).toLocaleDateString() : '—'}</td>
                    <td className="px-4 py-3 text-slate-400 text-xs">{new Date(user.createdAt).toLocaleDateString()}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </>
      )}

      <div className="text-xs text-slate-500">
        Role changes are made via <code className="bg-slate-800 px-1 py-0.5 rounded text-white">PATCH /api/admin/users</code> with audit logging and final Super Admin protection.
      </div>
    </PortalShell>
  );
}
