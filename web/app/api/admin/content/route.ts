import { NextResponse, type NextRequest } from 'next/server';
import { requireAdmin } from '@/lib/portal/authz';
import { listBlogPosts, createBlogPost, updateBlogPost, deleteBlogPost } from '@/features/content/content-service';
import { z } from 'zod';

export async function GET(request: NextRequest) {
  await requireAdmin();
  const searchParams = request.nextUrl.searchParams;
  const page = parseInt(searchParams.get('page') ?? '1', 10);
  const pageSize = parseInt(searchParams.get('pageSize') ?? '20', 10);
  const status = searchParams.get('status') ?? undefined;

  const result = await listBlogPosts({ page, pageSize, status });
  return NextResponse.json(result);
}

const createSchema = z.object({
  action: z.literal('create'),
  slug: z.string().min(1).max(200),
  title: z.string().min(1).max(200),
  excerpt: z.string().min(1).max(500),
  body: z.string().min(10),
  status: z.enum(['DRAFT', 'PUBLISHED']),
});

const updateSchema = z.object({
  action: z.literal('update'),
  id: z.string().min(1),
  title: z.string().min(1).max(200).optional(),
  excerpt: z.string().min(1).max(500).optional(),
  body: z.string().min(10).optional(),
  status: z.enum(['DRAFT', 'PUBLISHED']).optional(),
});

const deleteSchema = z.object({
  action: z.literal('delete'),
  id: z.string().min(1),
});

export async function POST(request: Request) {
  const session = await requireAdmin();
  const actorUserId = session.user?.id;
  if (!actorUserId) return NextResponse.json({ error: 'Unauthorized' }, { status: 401 });

  const body = await request.json().catch(() => null);

  const create = createSchema.safeParse(body);
  if (create.success) {
    const post = await createBlogPost({ ...create.data, authorUserId: actorUserId });
    return NextResponse.json({ post }, { status: 201 });
  }

  const update = updateSchema.safeParse(body);
  if (update.success) {
    const post = await updateBlogPost({ ...update.data, actorUserId });
    return NextResponse.json({ post });
  }

  const del = deleteSchema.safeParse(body);
  if (del.success) {
    await deleteBlogPost(del.data.id);
    return NextResponse.json({ deleted: true });
  }

  return NextResponse.json({ error: 'Invalid request. Provide action: create, update, or delete.' }, { status: 400 });
}
