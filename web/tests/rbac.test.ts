import { describe, expect, it } from 'vitest';
import {
  assertCanAccessOwnedResource,
  assertFinalSuperAdminProtected,
  canAccessAdmin,
  canManageRoles,
  normalizeEmail,
  roleForVerifiedEmail,
} from '@/lib/portal/rbac';

describe('portal RBAC', () => {
  it('normalizes email before bootstrap comparison', () => {
    expect(normalizeEmail('  GioLanhLuc@Gmail.com ')).toBe('giolanhluc@gmail.com');
  });

  it('assigns bootstrap super admin only for verified exact normalized email', () => {
    expect(roleForVerifiedEmail('GIOLANHLUC@gmail.com', true, 'giolanhluc@gmail.com')).toBe('SUPER_ADMIN');
    expect(roleForVerifiedEmail('someone@example.com', true, 'giolanhluc@gmail.com')).toBe('CUSTOMER');
    expect(() => roleForVerifiedEmail('giolanhluc@gmail.com', false, 'giolanhluc@gmail.com')).toThrow();
  });

  it('guards admin access and final super admin demotion', () => {
    expect(canAccessAdmin('CUSTOMER')).toBe(false);
    expect(canAccessAdmin('ADMIN')).toBe(true);
    expect(() => assertFinalSuperAdminProtected(1, 'SUPER_ADMIN', 'ADMIN')).toThrow();
    expect(() => assertFinalSuperAdminProtected(2, 'SUPER_ADMIN', 'ADMIN')).not.toThrow();
  });

  it('SUPPORT role cannot access admin portal', () => {
    expect(canAccessAdmin('SUPPORT')).toBe(false);
  });

  it('SUPER_ADMIN can access admin portal', () => {
    expect(canAccessAdmin('SUPER_ADMIN')).toBe(true);
  });

  it('only SUPER_ADMIN can manage roles', () => {
    expect(canManageRoles('SUPER_ADMIN')).toBe(true);
    expect(canManageRoles('ADMIN')).toBe(false);
    expect(canManageRoles('SUPPORT')).toBe(false);
    expect(canManageRoles('CUSTOMER')).toBe(false);
  });

  it('assertCanAccessOwnedResource allows same user', () => {
    expect(() => assertCanAccessOwnedResource('user-1', 'user-1')).not.toThrow();
  });

  it('assertCanAccessOwnedResource blocks different user (IDOR protection)', () => {
    expect(() => assertCanAccessOwnedResource('user-1', 'user-2')).toThrow('Forbidden');
  });

  it('protects final super admin from demotion to CUSTOMER', () => {
    expect(() => assertFinalSuperAdminProtected(1, 'SUPER_ADMIN', 'CUSTOMER')).toThrow();
  });

  it('allows demotion when multiple super admins exist', () => {
    expect(() => assertFinalSuperAdminProtected(3, 'SUPER_ADMIN', 'ADMIN')).not.toThrow();
  });
});
