import { NextResponse, type NextRequest } from 'next/server';
import { requireAdmin } from '@/lib/portal/authz';
import { listAuditLogs } from '@/features/audit/audit-service';

export async function GET(request: NextRequest) {
  await requireAdmin();
  const searchParams = request.nextUrl.searchParams;
  const page = parseInt(searchParams.get('page') ?? '1', 10);
  const pageSize = parseInt(searchParams.get('pageSize') ?? '50', 10);
  const action = searchParams.get('action') ?? undefined;
  const entityType = searchParams.get('entityType') ?? undefined;
  const actorUserId = searchParams.get('actorUserId') ?? undefined;

  const result = await listAuditLogs({ page, pageSize, action, entityType, actorUserId });
  return NextResponse.json(result);
}
