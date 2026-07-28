export const roles = ['CUSTOMER', 'SUPPORT', 'ADMIN', 'SUPER_ADMIN'] as const;
export type Role = (typeof roles)[number];

export function normalizeEmail(email: string) {
  return email.trim().toLowerCase();
}

export function roleForVerifiedEmail(email: string, verified: boolean, superAdminEmail: string): Role {
  if (!verified) {
    throw new Error('Google email must be verified.');
  }
  return normalizeEmail(email) === normalizeEmail(superAdminEmail) ? 'SUPER_ADMIN' : 'CUSTOMER';
}

export function canAccessAdmin(role: Role) {
  return role === 'ADMIN' || role === 'SUPER_ADMIN';
}

export function canManageRoles(role: Role) {
  return role === 'SUPER_ADMIN';
}

export function assertCanAccessOwnedResource(actorUserId: string, ownerUserId: string) {
  if (actorUserId !== ownerUserId) {
    throw new Error('Forbidden');
  }
}

export function assertFinalSuperAdminProtected(activeSuperAdminCount: number, targetRole: Role, nextRole: Role) {
  if (targetRole === 'SUPER_ADMIN' && nextRole !== 'SUPER_ADMIN' && activeSuperAdminCount <= 1) {
    throw new Error('Cannot remove or demote the final active Super Admin.');
  }
}
