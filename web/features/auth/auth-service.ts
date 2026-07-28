import 'server-only';
import { prisma } from '@/lib/db/prisma';
import { logAuditEvent } from '@/features/audit/audit-service';
import { normalizeEmail, roleForVerifiedEmail, assertFinalSuperAdminProtected, type Role } from '@/lib/portal/rbac';
import type { Prisma } from '@prisma/client';

export type AdminUserRow = Prisma.UserGetPayload<{
  select: {
    id: true;
    name: true;
    email: true;
    normalizedEmail: true;
    role: true;
    disabledAt: true;
    lastLoginAt: true;
    createdAt: true;
  };
}>;

export async function getSessionUser(userId: string) {
  if (!process.env.DATABASE_URL) return null;
  return prisma.user.findUnique({
    where: { id: userId },
    include: { customerProfile: true },
  });
}

export async function trackLastLogin(userId: string) {
  if (!process.env.DATABASE_URL) return;
  await prisma.user.update({
    where: { id: userId },
    data: { lastLoginAt: new Date() },
  });
}

export async function listUsers(params: {
  page?: number;
  pageSize?: number;
  search?: string;
  role?: Role;
}): Promise<{ users: AdminUserRow[]; total: number }> {
  if (!process.env.DATABASE_URL) return { users: [], total: 0 };
  const page = params.page ?? 1;
  const pageSize = Math.min(params.pageSize ?? 20, 100);
  const where: Record<string, unknown> = {};
  if (params.role) where.role = params.role;
  if (params.search) {
    where.OR = [
      { name: { contains: params.search, mode: 'insensitive' } },
      { email: { contains: params.search, mode: 'insensitive' } },
    ];
  }

  const [users, total] = await Promise.all([
    prisma.user.findMany({
      where,
      orderBy: { createdAt: 'desc' },
      skip: (page - 1) * pageSize,
      take: pageSize,
      select: {
        id: true,
        name: true,
        email: true,
        normalizedEmail: true,
        role: true,
        disabledAt: true,
        lastLoginAt: true,
        createdAt: true,
      },
    }),
    prisma.user.count({ where }),
  ]);
  return { users, total };
}

export async function changeUserRole(params: {
  targetUserId: string;
  newRole: Role;
  actorUserId: string;
}) {
  if (!process.env.DATABASE_URL) throw new Error('Database not configured.');

  const target = await prisma.user.findUnique({
    where: { id: params.targetUserId },
    select: { role: true },
  });
  if (!target) throw new Error('User not found.');

  // Protect final Super Admin
  if (target.role === 'SUPER_ADMIN' && params.newRole !== 'SUPER_ADMIN') {
    const count = await prisma.user.count({ where: { role: 'SUPER_ADMIN', disabledAt: null } });
    assertFinalSuperAdminProtected(count, target.role as Role, params.newRole);
  }

  const user = await prisma.user.update({
    where: { id: params.targetUserId },
    data: { role: params.newRole },
  });

  await logAuditEvent({
    actorUserId: params.actorUserId,
    action: 'USER_ROLE_CHANGED',
    entityType: 'User',
    entityId: params.targetUserId,
    metadata: { previousRole: target.role, newRole: params.newRole },
  });

  return user;
}

export async function disableUser(userId: string, actorUserId: string) {
  if (!process.env.DATABASE_URL) throw new Error('Database not configured.');
  const user = await prisma.user.update({
    where: { id: userId },
    data: { disabledAt: new Date() },
  });
  await logAuditEvent({
    actorUserId,
    action: 'USER_DISABLED',
    entityType: 'User',
    entityId: userId,
  });
  return user;
}

export async function enableUser(userId: string, actorUserId: string) {
  if (!process.env.DATABASE_URL) throw new Error('Database not configured.');
  const user = await prisma.user.update({
    where: { id: userId },
    data: { disabledAt: null },
  });
  await logAuditEvent({
    actorUserId,
    action: 'USER_ENABLED',
    entityType: 'User',
    entityId: userId,
  });
  return user;
}

export async function getUserStats() {
  if (!process.env.DATABASE_URL) return { total: 0, newThisMonth: 0, superAdmins: 0 };
  const now = new Date();
  const monthStart = new Date(now.getFullYear(), now.getMonth(), 1);
  const [total, newThisMonth, superAdmins] = await Promise.all([
    prisma.user.count(),
    prisma.user.count({ where: { createdAt: { gte: monthStart } } }),
    prisma.user.count({ where: { role: 'SUPER_ADMIN', disabledAt: null } }),
  ]);
  return { total, newThisMonth, superAdmins };
}
