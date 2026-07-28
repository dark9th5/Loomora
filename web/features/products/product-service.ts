import 'server-only';
import { prisma } from '@/lib/db/prisma';
import { logAuditEvent } from '@/features/audit/audit-service';
import type { Prisma } from '@prisma/client';

export type ProductRow = Prisma.ProductGetPayload<{
  include: { editions: { include: { capabilities: { include: { capability: true } } } } };
}>;

export type EditionRow = Prisma.EditionGetPayload<{
  include: { product: true; capabilities: { include: { capability: true } } };
}>;

export type CapabilityRow = Prisma.CapabilityGetPayload<Record<string, never>>;

// ---------------------------------------------------------------------------
// Products
// ---------------------------------------------------------------------------

export async function listProducts(): Promise<ProductRow[]> {
  if (!process.env.DATABASE_URL) return [];
  return prisma.product.findMany({
    include: { editions: { include: { capabilities: { include: { capability: true } } } } },
    orderBy: { createdAt: 'desc' },
  });
}

export async function createProduct(params: {
  slug: string;
  name: string;
  description: string;
  actorUserId: string;
}) {
  if (!process.env.DATABASE_URL) throw new Error('Database not configured.');
  const product = await prisma.product.create({
    data: { slug: params.slug, name: params.name, description: params.description },
  });
  await logAuditEvent({
    actorUserId: params.actorUserId,
    action: 'PRODUCT_CREATED',
    entityType: 'Product',
    entityId: product.id,
    metadata: { slug: params.slug },
  });
  return product;
}

// ---------------------------------------------------------------------------
// Editions
// ---------------------------------------------------------------------------

export async function createEdition(params: {
  productId: string;
  slug: string;
  name: string;
  priceCents: number;
  currency?: string;
  capabilityIds: string[];
  actorUserId: string;
}) {
  if (!process.env.DATABASE_URL) throw new Error('Database not configured.');
  const edition = await prisma.edition.create({
    data: {
      productId: params.productId,
      slug: params.slug,
      name: params.name,
      priceCents: params.priceCents,
      currency: params.currency ?? 'USD',
      capabilities: {
        create: params.capabilityIds.map((id) => ({ capabilityId: id })),
      },
    },
    include: { capabilities: { include: { capability: true } } },
  });
  await logAuditEvent({
    actorUserId: params.actorUserId,
    action: 'EDITION_CREATED',
    entityType: 'Edition',
    entityId: edition.id,
    metadata: { slug: params.slug, priceCents: params.priceCents },
  });
  return edition;
}

export async function listEditions(productId?: string): Promise<EditionRow[]> {
  if (!process.env.DATABASE_URL) return [];
  const where = productId ? { productId } : {};
  return prisma.edition.findMany({
    where,
    include: {
      product: true,
      capabilities: { include: { capability: true } },
    },
  });
}

// ---------------------------------------------------------------------------
// Capabilities
// ---------------------------------------------------------------------------

const FORBIDDEN_CAPABILITY_KEYS = ['LITERT_LM_PRO', 'LLAMA_CPP_PRO', 'GGUF_ACCESS'];

export async function createCapability(params: {
  key: string;
  name: string;
  description: string;
  actorUserId: string;
}) {
  if (!process.env.DATABASE_URL) throw new Error('Database not configured.');
  if (FORBIDDEN_CAPABILITY_KEYS.includes(params.key)) {
    throw new Error(`Capability key "${params.key}" is forbidden. Use product capability names instead of runtime names.`);
  }
  const capability = await prisma.capability.create({
    data: { key: params.key, name: params.name, description: params.description },
  });
  await logAuditEvent({
    actorUserId: params.actorUserId,
    action: 'CAPABILITY_CREATED',
    entityType: 'Capability',
    entityId: capability.id,
    metadata: { key: params.key },
  });
  return capability;
}

export async function listCapabilities(): Promise<CapabilityRow[]> {
  if (!process.env.DATABASE_URL) return [];
  return prisma.capability.findMany({ orderBy: { key: 'asc' } });
}
