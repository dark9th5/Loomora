import { PortalShell } from '@/components/PortalShell';
import { Badge } from '@/components/Badge';
import { EmptyState } from '@/components/EmptyState';
import { requireAdmin } from '@/lib/portal/authz';
import { listBlogPosts } from '@/features/content/content-service';
import { FileText } from 'lucide-react';

const nav = [
  { href: '/admin', label: 'Dashboard' },
  { href: '/admin/content', label: 'Blog/Content' },
];

export default async function AdminContentPage() {
  await requireAdmin();
  const hasDb = !!process.env.DATABASE_URL;
  const { posts, total } = hasDb ? await listBlogPosts({ page: 1, pageSize: 50 }) : { posts: [], total: 0 };

  return (
    <PortalShell title="Blog & Content" description="Create, edit, and publish blog posts. Manage content through the admin API." nav={nav}>
      {posts.length === 0 ? (
        <EmptyState icon={FileText} title="No blog posts" description={hasDb ? 'Create posts via POST /api/admin/content with action: create.' : 'Database not connected.'} />
      ) : (
        <>
          <p className="text-xs text-slate-400">{total} total posts</p>
          <div className="overflow-x-auto rounded-lg border border-slate-800">
            <table className="w-full text-left text-sm">
              <thead className="bg-slate-900 text-slate-300 border-b border-slate-800">
                <tr>
                  <th className="px-4 py-3 font-medium">Title</th>
                  <th className="px-4 py-3 font-medium">Slug</th>
                  <th className="px-4 py-3 font-medium">Status</th>
                  <th className="px-4 py-3 font-medium">Published</th>
                  <th className="px-4 py-3 font-medium">Created</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-800">
                {posts.map((post) => (
                  <tr key={post.id} className="bg-slate-950/60 hover:bg-slate-900/80 transition-colors">
                    <td className="px-4 py-3 text-slate-200 max-w-[250px] truncate">{post.title}</td>
                    <td className="px-4 py-3 text-slate-400 text-xs font-mono">{post.slug}</td>
                    <td className="px-4 py-3"><Badge variant={post.status === 'PUBLISHED' ? 'active' : 'draft'} label={post.status} /></td>
                    <td className="px-4 py-3 text-slate-400 text-xs">{post.publishedAt ? new Date(post.publishedAt).toLocaleDateString() : '—'}</td>
                    <td className="px-4 py-3 text-slate-400 text-xs">{new Date(post.createdAt).toLocaleDateString()}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </>
      )}
    </PortalShell>
  );
}
