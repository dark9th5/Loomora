import 'server-only';
import { prisma } from '@/lib/db/prisma';
import { logAuditEvent } from '@/features/audit/audit-service';
import type { ReleaseChannel, ReleaseStatus } from '@prisma/client';

export async function publishRelease(params: {
  actorUserId: string;
  versionName: string;
  versionCode: number;
  channel: ReleaseChannel;
  releaseNotes: string;
  minimumAndroid: string;
  supportedAbis: string;
  checksumSha256: string;
  fileSizeBytes: bigint;
  artifactStorageRef: string;
}) {
  if (!process.env.DATABASE_URL) throw new Error('Database not configured.');

  const release = await prisma.appRelease.create({
    data: {
      versionName: params.versionName,
      versionCode: params.versionCode,
      channel: params.channel,
      status: 'PUBLISHED',
      releaseNotes: params.releaseNotes,
      minimumAndroid: params.minimumAndroid,
      supportedAbis: params.supportedAbis,
      checksumSha256: params.checksumSha256,
      fileSizeBytes: params.fileSizeBytes,
      artifactStorageRef: params.artifactStorageRef,
      publishedAt: new Date(),
    },
  });

  await logAuditEvent({
    actorUserId: params.actorUserId,
    action: 'RELEASE_PUBLISHED',
    entityType: 'AppRelease',
    entityId: release.id,
    metadata: { versionName: params.versionName, channel: params.channel },
  });

  return release;
}

export async function retireRelease(releaseId: string, actorUserId: string) {
  if (!process.env.DATABASE_URL) throw new Error('Database not configured.');
  const release = await prisma.appRelease.update({
    where: { id: releaseId },
    data: { status: 'RETIRED' },
  });
  await logAuditEvent({
    actorUserId,
    action: 'RELEASE_RETIRED',
    entityType: 'AppRelease',
    entityId: releaseId,
  });
  return release;
}

export async function getPublishedReleases(channel?: ReleaseChannel) {
  if (!process.env.DATABASE_URL) return [];
  const where: { status: ReleaseStatus; channel?: ReleaseChannel } = { status: 'PUBLISHED' };
  if (channel) where.channel = channel;
  return prisma.appRelease.findMany({
    where,
    orderBy: { publishedAt: 'desc' },
  });
}

export async function getLatestStableRelease() {
  if (!process.env.DATABASE_URL) return null;
  return prisma.appRelease.findFirst({
    where: { status: 'PUBLISHED', channel: 'STABLE' },
    orderBy: { versionCode: 'desc' },
  });
}

export async function listAllReleases(params: {
  page?: number;
  pageSize?: number;
}) {
  if (!process.env.DATABASE_URL) return { releases: [], total: 0 };
  const page = params.page ?? 1;
  const pageSize = Math.min(params.pageSize ?? 20, 100);

  const [releases, total] = await Promise.all([
    prisma.appRelease.findMany({
      orderBy: { createdAt: 'desc' },
      skip: (page - 1) * pageSize,
      take: pageSize,
    }),
    prisma.appRelease.count(),
  ]);
  return { releases, total };
}
