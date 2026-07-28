import { NextResponse } from 'next/server';
import { requireSuperAdmin } from '@/lib/portal/authz';
import { prisma } from '@/lib/db/prisma';
import { logAuditEvent } from '@/features/audit/audit-service';
import { z } from 'zod';
import type { Prisma } from '@prisma/client';

export async function GET() {
  await requireSuperAdmin();
  if (!process.env.DATABASE_URL) return NextResponse.json({ settings: {} });

  const settings = await prisma.systemSetting.findMany();
  const result: Record<string, unknown> = {};
  for (const s of settings) result[s.key] = s.value;
  return NextResponse.json({ settings: result });
}

const settingSchema = z.object({
  key: z.string().min(1).max(80),
  value: z.unknown(),
});

export async function PUT(request: Request) {
  const session = await requireSuperAdmin();
  const actorUserId = session.user?.id;
  if (!actorUserId) return NextResponse.json({ error: 'Unauthorized' }, { status: 401 });

  const parsed = settingSchema.safeParse(await request.json().catch(() => null));
  if (!parsed.success) {
    return NextResponse.json({ error: 'Invalid setting.' }, { status: 400 });
  }

  if (!process.env.DATABASE_URL) {
    return NextResponse.json({ error: 'Database not configured.' }, { status: 503 });
  }

  const jsonValue = parsed.data.value as Prisma.InputJsonValue;

  const setting = await prisma.systemSetting.upsert({
    where: { key: parsed.data.key },
    create: { key: parsed.data.key, value: jsonValue },
    update: { value: jsonValue },
  });

  await logAuditEvent({
    actorUserId,
    action: 'SETTINGS_UPDATED',
    entityType: 'SystemSetting',
    entityId: parsed.data.key,
    metadata: { value: parsed.data.value },
  });

  return NextResponse.json({ setting });
}
