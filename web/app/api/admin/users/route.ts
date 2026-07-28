import { NextResponse, type NextRequest } from 'next/server';
import { requireSuperAdmin } from '@/lib/portal/authz';
import { listUsers, changeUserRole, disableUser, enableUser } from '@/features/auth/auth-service';
import { z } from 'zod';

export async function GET(request: NextRequest) {
  await requireSuperAdmin();
  const searchParams = request.nextUrl.searchParams;
  const page = parseInt(searchParams.get('page') ?? '1', 10);
  const pageSize = parseInt(searchParams.get('pageSize') ?? '20', 10);
  const search = searchParams.get('search') ?? undefined;
  const role = searchParams.get('role') as 'CUSTOMER' | 'SUPPORT' | 'ADMIN' | 'SUPER_ADMIN' | undefined;

  const result = await listUsers({ page, pageSize, search, role });
  return NextResponse.json(result);
}

const roleChangeSchema = z.object({
  targetUserId: z.string().min(1),
  newRole: z.enum(['CUSTOMER', 'SUPPORT', 'ADMIN', 'SUPER_ADMIN']),
});

const userActionSchema = z.object({
  userId: z.string().min(1),
  action: z.enum(['disable', 'enable']),
});

export async function PATCH(request: Request) {
  const session = await requireSuperAdmin();
  const actorUserId = session.user?.id;
  if (!actorUserId) return NextResponse.json({ error: 'Unauthorized' }, { status: 401 });

  const body = await request.json().catch(() => null);

  // Role change
  const roleChange = roleChangeSchema.safeParse(body);
  if (roleChange.success) {
    try {
      const user = await changeUserRole({
        targetUserId: roleChange.data.targetUserId,
        newRole: roleChange.data.newRole,
        actorUserId,
      });
      return NextResponse.json({ user, note: 'Role updated with audit log.' });
    } catch (error) {
      return NextResponse.json({ error: error instanceof Error ? error.message : 'Failed.' }, { status: 400 });
    }
  }

  // User disable/enable
  const userAction = userActionSchema.safeParse(body);
  if (userAction.success) {
    try {
      const user = userAction.data.action === 'disable'
        ? await disableUser(userAction.data.userId, actorUserId)
        : await enableUser(userAction.data.userId, actorUserId);
      return NextResponse.json({ user });
    } catch (error) {
      return NextResponse.json({ error: error instanceof Error ? error.message : 'Failed.' }, { status: 400 });
    }
  }

  return NextResponse.json({ error: 'Invalid request.' }, { status: 400 });
}
