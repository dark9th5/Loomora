import 'server-only';
import { prisma } from '@/lib/db/prisma';
import { logAuditEvent } from '@/features/audit/audit-service';
import type { OrderStatus } from '@prisma/client';

// ---------------------------------------------------------------------------
// Customer-facing
// ---------------------------------------------------------------------------

export async function createOrder(params: {
  customerUserId: string;
  editionId: string;
  quantity: number;
}) {
  if (!process.env.DATABASE_URL) throw new Error('Database not configured.');

  const edition = await prisma.edition.findUnique({
    where: { id: params.editionId },
    select: { id: true, priceCents: true, currency: true },
  });
  if (!edition) throw new Error('Edition not found.');

  const order = await prisma.order.create({
    data: {
      customerUserId: params.customerUserId,
      status: 'PENDING_PAYMENT',
      currency: edition.currency,
      totalCents: edition.priceCents * params.quantity,
      items: {
        create: {
          editionId: params.editionId,
          quantity: params.quantity,
          unitCents: edition.priceCents,
        },
      },
    },
    include: { items: { include: { edition: true } } },
  });

  await logAuditEvent({
    actorUserId: params.customerUserId,
    action: 'ORDER_CREATED',
    entityType: 'Order',
    entityId: order.id,
    metadata: { editionId: params.editionId, totalCents: order.totalCents },
  });

  return order;
}

export async function listOrdersByCustomer(customerUserId: string) {
  if (!process.env.DATABASE_URL) return [];
  return prisma.order.findMany({
    where: { customerUserId },
    orderBy: { createdAt: 'desc' },
    include: { items: { include: { edition: { include: { product: true } } } } },
  });
}

export async function getOrderById(orderId: string) {
  if (!process.env.DATABASE_URL) return null;
  return prisma.order.findUnique({
    where: { id: orderId },
    include: {
      customer: { select: { id: true, name: true, email: true } },
      items: { include: { edition: { include: { product: true } } } },
      payments: true,
    },
  });
}

// ---------------------------------------------------------------------------
// Admin-facing
// ---------------------------------------------------------------------------

export async function listAllOrders(params: {
  page?: number;
  pageSize?: number;
  status?: OrderStatus;
}) {
  if (!process.env.DATABASE_URL) return { orders: [], total: 0 };
  const page = params.page ?? 1;
  const pageSize = Math.min(params.pageSize ?? 20, 100);
  const where: Record<string, unknown> = {};
  if (params.status) where.status = params.status;

  const [orders, total] = await Promise.all([
    prisma.order.findMany({
      where,
      orderBy: { createdAt: 'desc' },
      skip: (page - 1) * pageSize,
      take: pageSize,
      include: {
        customer: { select: { id: true, name: true, email: true } },
        items: { include: { edition: { include: { product: true } } } },
        payments: true,
      },
    }),
    prisma.order.count({ where }),
  ]);
  return { orders, total };
}

export async function confirmManualPayment(params: {
  orderId: string;
  actorUserId: string;
  reason: string;
  reference?: string;
}) {
  if (!process.env.DATABASE_URL) throw new Error('Database not configured.');
  if (!params.reason || params.reason.trim().length < 5) {
    throw new Error('A reason is required for manual payment confirmation.');
  }

  const order = await prisma.order.findUnique({
    where: { id: params.orderId },
    select: { status: true, totalCents: true },
  });
  if (!order) throw new Error('Order not found.');
  if (order.status !== 'PENDING_PAYMENT') {
    throw new Error(`Cannot confirm payment for order with status ${order.status}.`);
  }

  const result = await prisma.$transaction(async (tx) => {
    const updated = await tx.order.update({
      where: { id: params.orderId },
      data: { status: 'PAID_MANUALLY' },
    });

    const payment = await tx.paymentRecord.create({
      data: {
        orderId: params.orderId,
        status: 'PAID_MANUALLY',
        provider: 'manual',
        reference: params.reference ?? null,
        amountCents: order.totalCents,
        reason: params.reason,
        actorUserId: params.actorUserId,
      },
    });

    return { order: updated, payment };
  });

  await logAuditEvent({
    actorUserId: params.actorUserId,
    action: 'ORDER_PAYMENT_CONFIRMED',
    entityType: 'Order',
    entityId: params.orderId,
    metadata: { reason: params.reason, amountCents: order.totalCents },
  });

  return result;
}

export async function cancelOrder(orderId: string, actorUserId: string) {
  if (!process.env.DATABASE_URL) throw new Error('Database not configured.');
  const order = await prisma.order.update({
    where: { id: orderId },
    data: { status: 'CANCELLED' },
  });
  await logAuditEvent({
    actorUserId,
    action: 'ORDER_CANCELLED',
    entityType: 'Order',
    entityId: orderId,
  });
  return order;
}

export async function refundOrder(orderId: string, actorUserId: string, reason: string) {
  if (!process.env.DATABASE_URL) throw new Error('Database not configured.');
  const result = await prisma.$transaction(async (tx) => {
    const order = await tx.order.update({
      where: { id: orderId },
      data: { status: 'REFUNDED' },
    });

    const payment = await tx.paymentRecord.create({
      data: {
        orderId,
        status: 'REFUNDED',
        provider: 'manual',
        amountCents: order.totalCents,
        reason,
        actorUserId,
      },
    });

    return { order, payment };
  });

  await logAuditEvent({
    actorUserId,
    action: 'ORDER_REFUNDED',
    entityType: 'Order',
    entityId: orderId,
    metadata: { reason },
  });

  return result;
}

// ---------------------------------------------------------------------------
// Stats
// ---------------------------------------------------------------------------

export async function getOrderStats() {
  if (!process.env.DATABASE_URL) return { pending: 0, total: 0, revenue: 0 };
  const [pending, total, revenue] = await Promise.all([
    prisma.order.count({ where: { status: 'PENDING_PAYMENT' } }),
    prisma.order.count(),
    prisma.order.aggregate({
      where: { status: { in: ['PAID', 'PAID_MANUALLY'] } },
      _sum: { totalCents: true },
    }),
  ]);
  return { pending, total, revenue: revenue._sum.totalCents ?? 0 };
}
