import 'server-only';
import { prisma } from '@/lib/db/prisma';
import type { Prisma } from '@prisma/client';

export type AuditAction =
  | 'USER_ROLE_CHANGED'
  | 'LICENSE_ISSUED'
  | 'LICENSE_REISSUED'
  | 'LICENSE_RENEWED'
  | 'LICENSE_SUSPENDED'
  | 'LICENSE_EXPIRED'
  | 'ORDER_CREATED'
  | 'ORDER_PAYMENT_CONFIRMED'
  | 'ORDER_CANCELLED'
  | 'ORDER_REFUNDED'
  | 'RELEASE_PUBLISHED'
  | 'RELEASE_RETIRED'
  | 'TICKET_STATUS_CHANGED'
  | 'TICKET_REPLY'
  | 'PRODUCT_CREATED'
  | 'EDITION_CREATED'
  | 'CAPABILITY_CREATED'
  | 'BLOG_PUBLISHED'
  | 'BLOG_UPDATED'
  | 'SETTINGS_UPDATED'
  | 'USER_DISABLED'
  | 'USER_ENABLED';

export async function logAuditEvent(params: {
  actorUserId: string | null;
  action: AuditAction;
  entityType: string;
  entityId?: string | null;
  metadata?: Record<string, unknown>;
}) {
  if (!process.env.DATABASE_URL) return null;
  return prisma.auditLog.create({
    data: {
      actorUserId: params.actorUserId,
      action: params.action,
      entityType: params.entityType,
      entityId: params.entityId ?? null,
      metadata: (params.metadata as Prisma.InputJsonValue) ?? undefined,
    },
  });
}

export async function listAuditLogs(params: {
  page?: number;
  pageSize?: number;
  action?: string;
  entityType?: string;
  actorUserId?: string;
}) {
  if (!process.env.DATABASE_URL) return { logs: [], total: 0 };
  const page = params.page ?? 1;
  const pageSize = Math.min(params.pageSize ?? 50, 100);
  const where: Record<string, unknown> = {};
  if (params.action) where.action = params.action;
  if (params.entityType) where.entityType = params.entityType;
  if (params.actorUserId) where.actorUserId = params.actorUserId;

  const [logs, total] = await Promise.all([
    prisma.auditLog.findMany({
      where,
      orderBy: { createdAt: 'desc' },
      skip: (page - 1) * pageSize,
      take: pageSize,
      include: { actor: { select: { id: true, name: true, email: true } } },
    }),
    prisma.auditLog.count({ where }),
  ]);
  return { logs, total };
}
