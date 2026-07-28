import 'server-only';
import { prisma } from '@/lib/db/prisma';
import { logAuditEvent } from '@/features/audit/audit-service';
import type { BlogPost, ContactLead } from '@prisma/client';

export async function createBlogPost(params: {
  slug: string;
  title: string;
  excerpt: string;
  body: string;
  status: 'DRAFT' | 'PUBLISHED';
  authorUserId: string;
}) {
  if (!process.env.DATABASE_URL) throw new Error('Database not configured.');
  const post = await prisma.blogPost.create({
    data: {
      slug: params.slug,
      title: params.title,
      excerpt: params.excerpt,
      body: params.body,
      status: params.status,
      authorUserId: params.authorUserId,
      publishedAt: params.status === 'PUBLISHED' ? new Date() : null,
    },
  });
  if (params.status === 'PUBLISHED') {
    await logAuditEvent({
      actorUserId: params.authorUserId,
      action: 'BLOG_PUBLISHED',
      entityType: 'BlogPost',
      entityId: post.id,
      metadata: { slug: params.slug },
    });
  }
  return post;
}

export async function updateBlogPost(params: {
  id: string;
  title?: string;
  excerpt?: string;
  body?: string;
  status?: 'DRAFT' | 'PUBLISHED';
  actorUserId: string;
}) {
  if (!process.env.DATABASE_URL) throw new Error('Database not configured.');
  const data: Record<string, unknown> = {};
  if (params.title !== undefined) data.title = params.title;
  if (params.excerpt !== undefined) data.excerpt = params.excerpt;
  if (params.body !== undefined) data.body = params.body;
  if (params.status !== undefined) {
    data.status = params.status;
    if (params.status === 'PUBLISHED') data.publishedAt = new Date();
  }

  const post = await prisma.blogPost.update({
    where: { id: params.id },
    data,
  });

  await logAuditEvent({
    actorUserId: params.actorUserId,
    action: 'BLOG_UPDATED',
    entityType: 'BlogPost',
    entityId: post.id,
    metadata: { status: params.status },
  });

  return post;
}

export async function listBlogPosts(params: {
  page?: number;
  pageSize?: number;
  status?: string;
  publishedOnly?: boolean;
}): Promise<{ posts: BlogPost[]; total: number }> {
  if (!process.env.DATABASE_URL) return { posts: [], total: 0 };
  const page = params.page ?? 1;
  const pageSize = Math.min(params.pageSize ?? 20, 100);
  const where: Record<string, unknown> = {};
  if (params.status) where.status = params.status;
  if (params.publishedOnly) where.status = 'PUBLISHED';

  const [posts, total] = await Promise.all([
    prisma.blogPost.findMany({
      where,
      orderBy: { createdAt: 'desc' },
      skip: (page - 1) * pageSize,
      take: pageSize,
    }),
    prisma.blogPost.count({ where }),
  ]);
  return { posts, total };
}

export async function getBlogPostBySlug(slug: string) {
  if (!process.env.DATABASE_URL) return null;
  return prisma.blogPost.findUnique({ where: { slug } });
}

export async function deleteBlogPost(id: string) {
  if (!process.env.DATABASE_URL) throw new Error('Database not configured.');
  return prisma.blogPost.delete({ where: { id } });
}

// ---------------------------------------------------------------------------
// Contact Leads
// ---------------------------------------------------------------------------

export async function createContactLead(params: {
  name: string;
  email: string;
  company?: string;
  topic: string;
  message: string;
  consent: boolean;
}) {
  if (!process.env.DATABASE_URL) return null;
  return prisma.contactLead.create({
    data: {
      name: params.name,
      email: params.email,
      company: params.company ?? null,
      topic: params.topic,
      message: params.message,
      consent: params.consent,
    },
  });
}

export async function listContactLeads(params: {
  page?: number;
  pageSize?: number;
}): Promise<{ leads: ContactLead[]; total: number }> {
  if (!process.env.DATABASE_URL) return { leads: [], total: 0 };
  const page = params.page ?? 1;
  const pageSize = Math.min(params.pageSize ?? 20, 100);
  const [leads, total] = await Promise.all([
    prisma.contactLead.findMany({
      orderBy: { createdAt: 'desc' },
      skip: (page - 1) * pageSize,
      take: pageSize,
    }),
    prisma.contactLead.count(),
  ]);
  return { leads, total };
}
