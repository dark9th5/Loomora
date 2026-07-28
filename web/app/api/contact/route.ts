import { NextResponse } from 'next/server';
import { contactLeadSchema } from '@/lib/portal/validation';
import { checkRateLimit } from '@/lib/portal/rate-limit';
import { createContactLead } from '@/features/content/content-service';

export async function POST(request: Request) {
  const limiter = checkRateLimit(`contact:${request.headers.get('x-forwarded-for') ?? 'local'}`, 5, 60_000);
  if (!limiter.allowed) {
    return NextResponse.json({ error: 'Too many contact attempts. Please try again later.' }, { status: 429 });
  }

  const parsed = contactLeadSchema.safeParse(await request.json().catch(() => null));
  if (!parsed.success) {
    return NextResponse.json({ error: 'Invalid contact request.', details: parsed.error.flatten().fieldErrors }, { status: 400 });
  }

  const lead = await createContactLead(parsed.data);

  return NextResponse.json({
    status: 'accepted',
    persisted: !!lead,
    note: lead
      ? 'Contact lead saved. We will respond shortly.'
      : 'Lead accepted but database persistence requires DATABASE_URL configuration.',
  });
}
