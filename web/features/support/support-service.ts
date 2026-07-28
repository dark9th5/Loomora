import 'server-only';
import { prisma } from '@/lib/db/prisma';
import { logAuditEvent } from '@/features/audit/audit-service';
import type { TicketStatus } from '@prisma/client';

export async function createTicket(params: {
  customerUserId: string | null;
  email: string;
  topic: string;
  subject: string;
  message: string;
}) {
  if (!process.env.DATABASE_URL) throw new Error('Database not configured.');

  const ticket = await prisma.supportTicket.create({
    data: {
      customerUserId: params.customerUserId,
      email: params.email,
      topic: params.topic,
      subject: params.subject,
      status: 'OPEN',
      messages: {
        create: {
          authorUserId: params.customerUserId,
          body: params.message,
        },
      },
    },
    include: { messages: true },
  });

  return ticket;
}

export async function addMessage(params: {
  ticketId: string;
  authorUserId: string | null;
  body: string;
}) {
  if (!process.env.DATABASE_URL) throw new Error('Database not configured.');
  return prisma.supportMessage.create({
    data: {
      ticketId: params.ticketId,
      authorUserId: params.authorUserId,
      body: params.body,
    },
  });
}

export async function updateTicketStatus(params: {
  ticketId: string;
  status: TicketStatus;
  actorUserId: string;
}) {
  if (!process.env.DATABASE_URL) throw new Error('Database not configured.');
  const ticket = await prisma.supportTicket.update({
    where: { id: params.ticketId },
    data: { status: params.status },
  });
  await logAuditEvent({
    actorUserId: params.actorUserId,
    action: 'TICKET_STATUS_CHANGED',
    entityType: 'SupportTicket',
    entityId: params.ticketId,
    metadata: { newStatus: params.status },
  });
  return ticket;
}

export async function listTicketsByCustomer(customerUserId: string) {
  if (!process.env.DATABASE_URL) return [];
  return prisma.supportTicket.findMany({
    where: { customerUserId },
    orderBy: { updatedAt: 'desc' },
    include: { messages: { orderBy: { createdAt: 'desc' }, take: 1 } },
  });
}

export async function getTicketById(ticketId: string) {
  if (!process.env.DATABASE_URL) return null;
  return prisma.supportTicket.findUnique({
    where: { id: ticketId },
    include: {
      customer: { select: { id: true, name: true, email: true } },
      messages: { orderBy: { createdAt: 'asc' } },
    },
  });
}

export async function listAllTickets(params: {
  page?: number;
  pageSize?: number;
  status?: TicketStatus;
}) {
  if (!process.env.DATABASE_URL) return { tickets: [], total: 0 };
  const page = params.page ?? 1;
  const pageSize = Math.min(params.pageSize ?? 20, 100);
  const where: Record<string, unknown> = {};
  if (params.status) where.status = params.status;

  const [tickets, total] = await Promise.all([
    prisma.supportTicket.findMany({
      where,
      orderBy: { updatedAt: 'desc' },
      skip: (page - 1) * pageSize,
      take: pageSize,
      include: {
        customer: { select: { id: true, name: true, email: true } },
        messages: { orderBy: { createdAt: 'desc' }, take: 1 },
      },
    }),
    prisma.supportTicket.count({ where }),
  ]);
  return { tickets, total };
}

export async function getTicketStats() {
  if (!process.env.DATABASE_URL) return { open: 0, inProgress: 0, total: 0 };
  const [open, inProgress, total] = await Promise.all([
    prisma.supportTicket.count({ where: { status: 'OPEN' } }),
    prisma.supportTicket.count({ where: { status: 'IN_PROGRESS' } }),
    prisma.supportTicket.count(),
  ]);
  return { open, inProgress, total };
}
