import { NextResponse } from 'next/server';
import { getAllPosts } from '@/lib/blog';
import { env } from '@/lib/env';

export async function GET() {
  const posts = getAllPosts();

  const itemsXml = posts
    .map(
      (post) => `
    <item>
      <title><![CDATA[${post.title}]]></title>
      <link>${env.siteUrl}/blog/${post.slug}</link>
      <guid>${env.siteUrl}/blog/${post.slug}</guid>
      <pubDate>${new Date(post.date).toUTCString()}</pubDate>
      <description><![CDATA[${post.excerpt}]]></description>
    </item>`
    )
    .join('');

  const rssXml = `<?xml version="1.0" encoding="UTF-8"?>
<rss version="2.0">
  <channel>
    <title>Loomora Blog</title>
    <link>${env.siteUrl}</link>
    <description>Articles on voice recording, local-first privacy, and AI notes.</description>
    <language>en-us</language>
    ${itemsXml}
  </channel>
</rss>`;

  return new NextResponse(rssXml, {
    headers: {
      'Content-Type': 'application/xml',
    },
  });
}
