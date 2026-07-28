import { NextResponse } from 'next/server';
import { requireSession } from '@/lib/portal/authz';
import { checkRateLimit } from '@/lib/portal/rate-limit';
import { supportTicketSchema } from '@/lib/portal/validation';
import { createTicket, listTicketsByCustomer } from '@/features/support/support-service';

export async function POST(request: Request) {
  const session = await requireSession();
  const userId = session.user?.id ?? null;
  const email = session.user?.email ?? '';

  const limiter = checkRateLimit(`support:${userId ?? 'anon'}`, 10, 60_000);
  if (!limiter.allowed) {
    return NextResponse.json({ error: 'Too many support requests.' }, { status: 429 });
  }

  const parsed = supportTicketSchema.safeParse(await request.json().catch(() => null));
  if (!parsed.success) {
    return NextResponse.json({ error: 'Invalid support ticket.', details: parsed.error.flatten().fieldErrors }, { status: 400 });
  }

  if (!process.env.DATABASE_URL) {
    return NextResponse.json({
      status: 'draft',
      note: 'Ticket persistence requires DATABASE_URL. The request did not send email or expose secrets.',
    });
  }

  const ticket = await createTicket({
    customerUserId: userId,
    email,
    topic: parsed.data.topic,
    subject: parsed.data.subject,
    message: parsed.data.message,
  });

  return NextResponse.json({
    ticketId: ticket.id,
    status: ticket.status,
    note: 'Support ticket created. An agent will review your request.',
  }, { status: 201 });
}

export async function GET() {
  const session = await requireSession();
  const userId = session.user?.id;
  if (!userId) return NextResponse.json({ error: 'Unauthorized' }, { status: 401 });

  const tickets = await listTicketsByCustomer(userId);
  return NextResponse.json({ tickets });
}
