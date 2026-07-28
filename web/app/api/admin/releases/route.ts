import { NextResponse, type NextRequest } from 'next/server';
import { requireAdmin } from '@/lib/portal/authz';
import { listAllReleases, publishRelease, retireRelease } from '@/features/downloads/download-service';
import { z } from 'zod';

export async function GET(request: NextRequest) {
  await requireAdmin();
  const searchParams = request.nextUrl.searchParams;
  const page = parseInt(searchParams.get('page') ?? '1', 10);
  const pageSize = parseInt(searchParams.get('pageSize') ?? '20', 10);

  const result = await listAllReleases({ page, pageSize });
  return NextResponse.json(result);
}

const publishSchema = z.object({
  versionName: z.string().min(1),
  versionCode: z.number().int().positive(),
  channel: z.enum(['INTERNAL', 'BETA', 'STABLE']),
  releaseNotes: z.string().min(10),
  minimumAndroid: z.string().min(1),
  supportedAbis: z.string().min(1),
  checksumSha256: z.string().length(64),
  fileSizeBytes: z.number().int().positive(),
  artifactStorageRef: z.string().min(1),
});

export async function POST(request: Request) {
  const session = await requireAdmin();
  const actorUserId = session.user?.id;
  if (!actorUserId) return NextResponse.json({ error: 'Unauthorized' }, { status: 401 });

  const parsed = publishSchema.safeParse(await request.json().catch(() => null));
  if (!parsed.success) {
    return NextResponse.json({ error: 'Invalid release data.', details: parsed.error.flatten().fieldErrors }, { status: 400 });
  }

  const release = await publishRelease({
    actorUserId,
    ...parsed.data,
    fileSizeBytes: BigInt(parsed.data.fileSizeBytes),
  });

  return NextResponse.json({ release: { ...release, fileSizeBytes: release.fileSizeBytes.toString() } }, { status: 201 });
}

const retireSchema = z.object({ releaseId: z.string().min(1) });

export async function PATCH(request: Request) {
  const session = await requireAdmin();
  const actorUserId = session.user?.id;
  if (!actorUserId) return NextResponse.json({ error: 'Unauthorized' }, { status: 401 });

  const parsed = retireSchema.safeParse(await request.json().catch(() => null));
  if (!parsed.success) {
    return NextResponse.json({ error: 'Invalid request.' }, { status: 400 });
  }

  const release = await retireRelease(parsed.data.releaseId, actorUserId);
  return NextResponse.json({ release: { ...release, fileSizeBytes: release.fileSizeBytes.toString() } });
}
