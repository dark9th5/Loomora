import 'server-only';
import { prisma } from '@/lib/db/prisma';
import { logAuditEvent } from '@/features/audit/audit-service';
import {
  canonicalizeLicensePayload,
  licensePayloadHash,
  signLicensePayload,
  type LicensePayload,
  type SignedLicenseEnvelope,
} from '@/lib/license/contract';
import type { LicenseRevision, Prisma } from '@prisma/client';

export type CustomerLicense = Prisma.LicenseGetPayload<{
  include: {
    edition: { include: { product: true } };
    capabilities: { include: { capability: true } };
    deviceBinding: true;
    revisions: true;
  };
}>;

export type AdminLicenseRow = Prisma.LicenseGetPayload<{
  include: {
    customer: { select: { id: true; name: true; email: true } };
    edition: { include: { product: true } };
    capabilities: { include: { capability: true } };
    deviceBinding: true;
  };
}>;

// ---------------------------------------------------------------------------
// Query
// ---------------------------------------------------------------------------

export async function getLicensesByCustomer(customerUserId: string): Promise<CustomerLicense[]> {
  if (!process.env.DATABASE_URL) return [];
  return prisma.license.findMany({
    where: { customerUserId },
    include: {
      edition: { include: { product: true } },
      capabilities: { include: { capability: true } },
      deviceBinding: true,
      revisions: { orderBy: { revision: 'desc' as const }, take: 1 },
    },
    orderBy: { createdAt: 'desc' },
  });
}

export async function getLicenseById(licenseId: string) {
  if (!process.env.DATABASE_URL) return null;
  return prisma.license.findUnique({
    where: { id: licenseId },
    include: {
      customer: { select: { id: true, name: true, email: true } },
      edition: { include: { product: true, capabilities: { include: { capability: true } } } },
      capabilities: { include: { capability: true } },
      deviceBinding: true,
      revisions: { orderBy: { revision: 'desc' as const } },
    },
  });
}

export async function getLicenseRevisions(licenseId: string): Promise<LicenseRevision[]> {
  if (!process.env.DATABASE_URL) return [];
  return prisma.licenseRevision.findMany({
    where: { licenseId },
    orderBy: { revision: 'desc' as const },
  });
}

export async function listAllLicenses(params: {
  page?: number;
  pageSize?: number;
  status?: string;
}): Promise<{ licenses: AdminLicenseRow[]; total: number }> {
  if (!process.env.DATABASE_URL) return { licenses: [], total: 0 };
  const page = params.page ?? 1;
  const pageSize = Math.min(params.pageSize ?? 20, 100);
  const where: Record<string, unknown> = {};
  if (params.status) where.status = params.status;

  const [licenses, total] = await Promise.all([
    prisma.license.findMany({
      where,
      orderBy: { createdAt: 'desc' },
      skip: (page - 1) * pageSize,
      take: pageSize,
      include: {
        customer: { select: { id: true, name: true, email: true } },
        edition: { include: { product: true } },
        capabilities: { include: { capability: true } },
        deviceBinding: true,
      },
    }),
    prisma.license.count({ where }),
  ]);
  return { licenses, total };
}

// ---------------------------------------------------------------------------
// License Issuance (transactional)
// ---------------------------------------------------------------------------

export async function issueLicense(params: {
  actorUserId: string;
  customerUserId: string;
  editionId: string;
  capabilityIds: string[];
  payload: LicensePayload;
  privateKeyPem: string | null;
  keyId: string;
}) {
  if (!process.env.DATABASE_URL) throw new Error('Database not configured.');

  const { actorUserId, customerUserId, editionId, capabilityIds, payload, privateKeyPem, keyId } = params;

  let signature = '';
  let envelope: SignedLicenseEnvelope | null = null;

  if (privateKeyPem) {
    envelope = signLicensePayload(payload, keyId, privateKeyPem);
    signature = envelope.signature;
  }

  const hash = licensePayloadHash(payload);
  const canonical = canonicalizeLicensePayload(payload);

  const result = await prisma.$transaction(async (tx) => {
    const license = await tx.license.create({
      data: {
        id: payload.licenseId,
        customerUserId,
        editionId,
        status: privateKeyPem ? 'ACTIVE' : 'UNSIGNED_DRAFT',
        currentRevision: 1,
        capabilities: {
          create: capabilityIds.map((capabilityId) => ({ capabilityId })),
        },
        deviceBinding: payload.deviceBinding
          ? { create: { digest: payload.deviceBinding, label: 'Primary Device' } }
          : undefined,
      },
    });

    const revision = await tx.licenseRevision.create({
      data: {
        licenseId: license.id,
        revision: 1,
        payloadJson: JSON.parse(canonical),
        signature,
        keyId,
        payloadHash: hash,
        actorUserId,
      },
    });

    return { license, revision, envelope };
  });

  await logAuditEvent({
    actorUserId,
    action: 'LICENSE_ISSUED',
    entityType: 'License',
    entityId: result.license.id,
    metadata: { editionId, capabilityIds, payloadHash: hash, signed: !!privateKeyPem },
  });

  return result;
}

// ---------------------------------------------------------------------------
// Reissue (creates a new immutable revision)
// ---------------------------------------------------------------------------

export async function reissueLicense(params: {
  actorUserId: string;
  licenseId: string;
  payload: LicensePayload;
  privateKeyPem: string | null;
  keyId: string;
}) {
  if (!process.env.DATABASE_URL) throw new Error('Database not configured.');

  const existing = await prisma.license.findUnique({
    where: { id: params.licenseId },
    select: { currentRevision: true },
  });
  if (!existing) throw new Error('License not found.');

  const nextRevision = existing.currentRevision + 1;
  const hash = licensePayloadHash(params.payload);
  const canonical = canonicalizeLicensePayload(params.payload);
  let signature = '';
  let envelope: SignedLicenseEnvelope | null = null;

  if (params.privateKeyPem) {
    envelope = signLicensePayload(
      { ...params.payload, licenseVersion: nextRevision },
      params.keyId,
      params.privateKeyPem,
    );
    signature = envelope.signature;
  }

  const result = await prisma.$transaction(async (tx) => {
    const revision = await tx.licenseRevision.create({
      data: {
        licenseId: params.licenseId,
        revision: nextRevision,
        payloadJson: JSON.parse(canonical),
        signature,
        keyId: params.keyId,
        payloadHash: hash,
        actorUserId: params.actorUserId,
      },
    });

    const license = await tx.license.update({
      where: { id: params.licenseId },
      data: { currentRevision: nextRevision },
    });

    return { license, revision, envelope };
  });

  await logAuditEvent({
    actorUserId: params.actorUserId,
    action: 'LICENSE_REISSUED',
    entityType: 'License',
    entityId: params.licenseId,
    metadata: { revision: nextRevision, payloadHash: hash },
  });

  return result;
}

// ---------------------------------------------------------------------------
// Status mutations
// ---------------------------------------------------------------------------

export async function suspendLicense(licenseId: string, actorUserId: string) {
  if (!process.env.DATABASE_URL) throw new Error('Database not configured.');
  const license = await prisma.license.update({
    where: { id: licenseId },
    data: { status: 'SUSPENDED' },
  });
  await logAuditEvent({
    actorUserId,
    action: 'LICENSE_SUSPENDED',
    entityType: 'License',
    entityId: licenseId,
  });
  return license;
}

export async function markLicenseExpired(licenseId: string, actorUserId: string) {
  if (!process.env.DATABASE_URL) throw new Error('Database not configured.');
  const license = await prisma.license.update({
    where: { id: licenseId },
    data: { status: 'EXPIRED' },
  });
  await logAuditEvent({
    actorUserId,
    action: 'LICENSE_EXPIRED',
    entityType: 'License',
    entityId: licenseId,
  });
  return license;
}

// ---------------------------------------------------------------------------
// Download envelope (ownership checked)
// ---------------------------------------------------------------------------

export async function downloadLicenseEnvelope(licenseId: string, requestingUserId: string) {
  if (!process.env.DATABASE_URL) throw new Error('Database not configured.');
  const license = await prisma.license.findUnique({
    where: { id: licenseId },
    include: {
      revisions: { orderBy: { revision: 'desc' as const }, take: 1 },
    },
  });
  if (!license) throw new Error('License not found.');
  if (license.customerUserId !== requestingUserId) throw new Error('Forbidden');
  if (!license.revisions[0]?.signature) throw new Error('License is not signed.');

  const revision = license.revisions[0];
  return {
    payload: revision.payloadJson,
    signatureAlgorithm: 'Ed25519' as const,
    keyId: revision.keyId,
    signature: revision.signature,
  };
}
