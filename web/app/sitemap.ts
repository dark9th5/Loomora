import { MetadataRoute } from 'next';
import { env } from '@/lib/env';
import { getAllPosts } from '@/lib/blog';

export default function sitemap(): MetadataRoute.Sitemap {
  const baseUrl = env.siteUrl;

  const routes = [
    '',
    '/features',
    '/pricing',
    '/download',
    '/blog',
    '/contact',
    '/privacy',
    '/terms',
    '/data-deletion',
    '/pro',
  ].map((route) => ({
    url: `${baseUrl}${route}`,
    lastModified: new Date().toISOString(),
    changeFrequency: 'weekly' as const,
    priority: route === '' ? 1.0 : 0.8,
  }));

  const blogPosts = getAllPosts().map((post) => ({
    url: `${baseUrl}/blog/${post.slug}`,
    lastModified: post.date,
    changeFrequency: 'monthly' as const,
    priority: 0.6,
  }));

  return [...routes, ...blogPosts];
}
