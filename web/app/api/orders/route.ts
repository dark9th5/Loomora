import { NextResponse } from 'next/server';
import { requireSession } from '@/lib/portal/authz';
import { checkRateLimit } from '@/lib/portal/rate-limit';
import { createOrderSchema } from '@/lib/portal/validation';
import { createOrder, listOrdersByCustomer } from '@/features/orders/order-service';
import { prisma } from '@/lib/db/prisma';

export async function POST(request: Request) {
  const session = await requireSession();
  const userId = session.user?.id;
  if (!userId) return NextResponse.json({ error: 'Unauthorized' }, { status: 401 });

  const limiter = checkRateLimit(`order:${userId}`, 10, 60_000);
  if (!limiter.allowed) {
    return NextResponse.json({ error: 'Too many order attempts.' }, { status: 429 });
  }

  const parsed = createOrderSchema.safeParse(await request.json().catch(() => null));
  if (!parsed.success) {
    return NextResponse.json({ error: 'Invalid order request.', details: parsed.error.flatten().fieldErrors }, { status: 400 });
  }

  if (!process.env.DATABASE_URL) {
    return NextResponse.json({
      status: 'PENDING_PAYMENT',
      paymentMode: 'manual',
      note: 'Manual payment order created only after database wiring. This response does not grant Pro.',
    });
  }

  // Resolve edition by slug
  const edition = await prisma.edition.findFirst({ where: { slug: parsed.data.editionSlug } });
  if (!edition) {
    return NextResponse.json({ error: 'Edition not found.' }, { status: 404 });
  }

  const order = await createOrder({
    customerUserId: userId,
    editionId: edition.id,
    quantity: parsed.data.quantity,
  });

  return NextResponse.json({
    orderId: order.id,
    status: order.status,
    totalCents: order.totalCents,
    currency: order.currency,
    paymentMode: 'manual',
    note: 'Order created with PENDING_PAYMENT. Contact support or wait for admin to confirm manual payment. This does not grant Pro automatically.',
  }, { status: 201 });
}

export async function GET() {
  const session = await requireSession();
  const userId = session.user?.id;
  if (!userId) return NextResponse.json({ error: 'Unauthorized' }, { status: 401 });

  const orders = await listOrdersByCustomer(userId);
  return NextResponse.json({ orders });
}
