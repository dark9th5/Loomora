import { redirect } from 'next/navigation';
import { auth } from '@/auth';
import { canAccessAdmin, type Role } from '@/lib/portal/rbac';

export async function requireSession() {
  const session = await auth();
  if (!session?.user?.id) {
    redirect('/sign-in');
  }
  if (!session.user.email) {
    redirect('/access-denied');
  }
  return session;
}

export async function requireAdmin() {
  const session = await requireSession();
  const role = (session.user?.role ?? 'CUSTOMER') as Role;
  if (!canAccessAdmin(role)) {
    redirect('/access-denied');
  }
  return session;
}

export async function requireSuperAdmin() {
  const session = await requireSession();
  if (session.user?.role !== 'SUPER_ADMIN') {
    redirect('/access-denied');
  }
  return session;
}
