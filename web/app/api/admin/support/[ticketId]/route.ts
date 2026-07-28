import { NextResponse } from 'next/server';
import { requireAdmin } from '@/lib/portal/authz';
import { getTicketById, updateTicketStatus, addMessage } from '@/features/support/support-service';
import { z } from 'zod';

const statusSchema = z.object({
  status: z.enum(['OPEN', 'IN_PROGRESS', 'WAITING_CUSTOMER', 'RESOLVED', 'CLOSED']),
});

const replySchema = z.object({
  body: z.string().min(1).max(4000),
});

export async function GET(_: Request, { params }: { params: Promise<{ ticketId: string }> }) {
  await requireAdmin();
  const { ticketId } = await params;
  const ticket = await getTicketById(ticketId);
  if (!ticket) return NextResponse.json({ error: 'Ticket not found.' }, { status: 404 });
  return NextResponse.json({ ticket });
}

export async function PATCH(request: Request, { params }: { params: Promise<{ ticketId: string }> }) {
  const session = await requireAdmin();
  const actorUserId = session.user?.id;
  if (!actorUserId) return NextResponse.json({ error: 'Unauthorized' }, { status: 401 });

  const { ticketId } = await params;
  const parsed = statusSchema.safeParse(await request.json().catch(() => null));
  if (!parsed.success) {
    return NextResponse.json({ error: 'Invalid status.' }, { status: 400 });
  }

  const ticket = await updateTicketStatus({ ticketId, status: parsed.data.status, actorUserId });
  return NextResponse.json({ ticket });
}

export async function POST(request: Request, { params }: { params: Promise<{ ticketId: string }> }) {
  const session = await requireAdmin();
  const actorUserId = session.user?.id;
  if (!actorUserId) return NextResponse.json({ error: 'Unauthorized' }, { status: 401 });

  const { ticketId } = await params;
  const parsed = replySchema.safeParse(await request.json().catch(() => null));
  if (!parsed.success) {
    return NextResponse.json({ error: 'Invalid reply.' }, { status: 400 });
  }

  const message = await addMessage({ ticketId, authorUserId: actorUserId, body: parsed.data.body });
  return NextResponse.json({ message }, { status: 201 });
}
