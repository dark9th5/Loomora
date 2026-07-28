import { NextResponse } from 'next/server';
import { requireAdmin } from '@/lib/portal/authz';
import { confirmManualPayment } from '@/features/orders/order-service';
import { z } from 'zod';

const confirmSchema = z.object({
  reason: z.string().min(5, 'Reason must be at least 5 characters.'),
  reference: z.string().optional(),
});

export async function POST(request: Request, { params }: { params: Promise<{ orderId: string }> }) {
  const session = await requireAdmin();
  const actorUserId = session.user?.id;
  if (!actorUserId) return NextResponse.json({ error: 'Unauthorized' }, { status: 401 });

  const { orderId } = await params;
  const parsed = confirmSchema.safeParse(await request.json().catch(() => null));
  if (!parsed.success) {
    return NextResponse.json({ error: 'Invalid request.', details: parsed.error.flatten().fieldErrors }, { status: 400 });
  }

  try {
    const result = await confirmManualPayment({
      orderId,
      actorUserId,
      reason: parsed.data.reason,
      reference: parsed.data.reference,
    });
    return NextResponse.json({
      orderId,
      status: result.order.status,
      paymentId: result.payment.id,
      note: 'Payment confirmed manually with audit log. License issuance is a separate admin action.',
    });
  } catch (error) {
    return NextResponse.json({ error: error instanceof Error ? error.message : 'Failed.' }, { status: 400 });
  }
}
