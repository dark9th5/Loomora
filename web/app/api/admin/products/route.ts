import { NextResponse, type NextRequest } from 'next/server';
import { requireAdmin } from '@/lib/portal/authz';
import { listProducts, createProduct, createEdition, listCapabilities, createCapability } from '@/features/products/product-service';
import { z } from 'zod';

export async function GET(request: NextRequest) {
  await requireAdmin();
  const type = request.nextUrl.searchParams.get('type');

  if (type === 'capabilities') {
    const capabilities = await listCapabilities();
    return NextResponse.json({ capabilities });
  }

  const products = await listProducts();
  return NextResponse.json({ products });
}

const productSchema = z.object({
  type: z.literal('product'),
  slug: z.string().min(1).max(80),
  name: z.string().min(1).max(120),
  description: z.string().min(1).max(2000),
});

const editionSchema = z.object({
  type: z.literal('edition'),
  productId: z.string().min(1),
  slug: z.string().min(1).max(80),
  name: z.string().min(1).max(120),
  priceCents: z.number().int().min(0),
  currency: z.string().length(3).optional(),
  capabilityIds: z.array(z.string().min(1)),
});

const capabilitySchema = z.object({
  type: z.literal('capability'),
  key: z.string().min(1).max(80),
  name: z.string().min(1).max(120),
  description: z.string().min(1).max(2000),
});

export async function POST(request: Request) {
  const session = await requireAdmin();
  const actorUserId = session.user?.id;
  if (!actorUserId) return NextResponse.json({ error: 'Unauthorized' }, { status: 401 });

  const body = await request.json().catch(() => null);

  const product = productSchema.safeParse(body);
  if (product.success) {
    try {
      const result = await createProduct({ ...product.data, actorUserId });
      return NextResponse.json({ product: result }, { status: 201 });
    } catch (error) {
      return NextResponse.json({ error: error instanceof Error ? error.message : 'Failed.' }, { status: 400 });
    }
  }

  const edition = editionSchema.safeParse(body);
  if (edition.success) {
    try {
      const result = await createEdition({ ...edition.data, actorUserId });
      return NextResponse.json({ edition: result }, { status: 201 });
    } catch (error) {
      return NextResponse.json({ error: error instanceof Error ? error.message : 'Failed.' }, { status: 400 });
    }
  }

  const capability = capabilitySchema.safeParse(body);
  if (capability.success) {
    try {
      const result = await createCapability({ ...capability.data, actorUserId });
      return NextResponse.json({ capability: result }, { status: 201 });
    } catch (error) {
      return NextResponse.json({ error: error instanceof Error ? error.message : 'Failed.' }, { status: 400 });
    }
  }

  return NextResponse.json({ error: 'Invalid request. Provide type: product, edition, or capability.' }, { status: 400 });
}
