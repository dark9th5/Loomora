import 'server-only';
import { prisma } from '@/lib/db/prisma';
import type { Prisma } from '@prisma/client';

export type CustomerRow = Prisma.UserGetPayload<{
  include: { customerProfile: true };
}>;

export async function getCustomerProfile(userId: string) {
  if (!process.env.DATABASE_URL) return null;
  return prisma.customerProfile.findUnique({ where: { userId } });
}

export async function upsertCustomerProfile(userId: string, data: {
  company?: string;
  phone?: string;
  country?: string;
}) {
  if (!process.env.DATABASE_URL) return null;
  return prisma.customerProfile.upsert({
    where: { userId },
    create: { userId, ...data },
    update: data,
  });
}

export async function listCustomers(params: {
  page?: number;
  pageSize?: number;
  search?: string;
}): Promise<{ customers: CustomerRow[]; total: number }> {
  if (!process.env.DATABASE_URL) return { customers: [], total: 0 };
  const page = params.page ?? 1;
  const pageSize = Math.min(params.pageSize ?? 20, 100);
  const where = params.search
    ? {
        OR: [
          { name: { contains: params.search, mode: 'insensitive' as const } },
          { email: { contains: params.search, mode: 'insensitive' as const } },
        ],
      }
    : {};

  const [customers, total] = await Promise.all([
    prisma.user.findMany({
      where,
      orderBy: { createdAt: 'desc' },
      skip: (page - 1) * pageSize,
      take: pageSize,
      include: { customerProfile: true },
    }),
    prisma.user.count({ where }),
  ]);
  return { customers, total };
}

export async function getCustomerById(userId: string) {
  if (!process.env.DATABASE_URL) return null;
  return prisma.user.findUnique({
    where: { id: userId },
    include: {
      customerProfile: true,
      licenses: { include: { edition: { include: { product: true } }, capabilities: { include: { capability: true } } } },
      orders: { orderBy: { createdAt: 'desc' }, take: 5 },
      supportTickets: { orderBy: { createdAt: 'desc' }, take: 5 },
    },
  });
}
