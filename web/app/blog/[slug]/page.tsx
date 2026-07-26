import React from 'react';
import { getPostBySlug, getAllPosts } from '@/lib/blog';
import { notFound } from 'next/navigation';
import Link from 'next/link';
import { ArrowLeft, Calendar, Clock, Tag, Download, ShieldCheck } from 'lucide-react';
import { env } from '@/lib/env';
import type { Metadata } from 'next';

export async function generateStaticParams() {
  const posts = getAllPosts();
  return posts.map((post) => ({
    slug: post.slug,
  }));
}

export async function generateMetadata({ params }: { params: { slug: string } }): Promise<Metadata> {
  const post = getPostBySlug(params.slug);
  if (!post) return {};

  return {
    title: `${post.title} — Loomora Blog`,
    description: post.excerpt,
    openGraph: {
      title: post.title,
      description: post.excerpt,
      type: 'article',
      publishedTime: post.date,
      authors: [post.author],
    },
  };
}

export default function BlogPostPage({ params }: { params: { slug: string } }) {
  const post = getPostBySlug(params.slug);

  if (!post) {
    notFound();
  }

  const jsonLd = {
    '@context': 'https://schema.org',
    '@type': 'BlogPosting',
    headline: post.title,
    description: post.excerpt,
    datePublished: post.date,
    author: {
      '@type': 'Organization',
      name: post.author,
    },
  };

  return (
    <article className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-16 space-y-8">
      <script
        type="application/ld+json"
        dangerouslySetInnerHTML={{ __html: JSON.stringify(jsonLd) }}
      />

      <Link href="/blog" className="inline-flex items-center space-x-2 text-xs font-semibold text-loomora-secondary hover:underline">
        <ArrowLeft className="w-4 h-4" />
        <span>Back to All Articles</span>
      </Link>

      <div className="space-y-4">
        <div className="flex items-center space-x-3 text-xs text-slate-400">
          <span className="bg-loomora-primary/20 text-loomora-container font-bold px-2.5 py-1 rounded-full uppercase">{post.category}</span>
          <span>•</span>
          <span className="flex items-center space-x-1"><Calendar className="w-3.5 h-3.5" /><span>{post.date}</span></span>
          <span>•</span>
          <span className="flex items-center space-x-1"><Clock className="w-3.5 h-3.5" /><span>{post.readTime}</span></span>
        </div>

        <h1 className="text-3xl sm:text-5xl font-extrabold text-white light:text-slate-900 leading-tight">
          {post.title}
        </h1>

        <p className="text-base text-slate-300 light:text-slate-600 font-medium italic border-l-2 border-loomora-secondary pl-4 py-1">
          {post.excerpt}
        </p>
      </div>

      <div className="glass p-8 sm:p-12 rounded-3xl border border-white/10 prose prose-invert max-w-none text-slate-300 space-y-6 leading-relaxed text-sm">
        {post.content.split('\n\n').map((paragraph, idx) => {
          if (paragraph.startsWith('### ')) {
            return <h3 key={idx} className="text-xl font-bold text-white pt-4">{paragraph.replace('### ', '')}</h3>;
          }
          if (paragraph.startsWith('- ')) {
            return (
              <ul key={idx} className="list-disc pl-5 space-y-1">
                {paragraph.split('\n').map((item, iIndex) => (
                  <li key={iIndex}>{item.replace('- ', '')}</li>
                ))}
              </ul>
            );
          }
          return <p key={idx}>{paragraph}</p>;
        })}
      </div>

      {/* Download CTA Banner */}
      <div className="glass p-8 rounded-3xl border border-loomora-primary/40 space-y-4 shadow-xl text-center sm:text-left sm:flex sm:items-center sm:justify-between sm:space-y-0 sm:space-x-6">
        <div className="space-y-2">
          <div className="inline-flex items-center space-x-1.5 px-3 py-1 rounded-full bg-green-500/20 text-green-400 text-xs font-bold">
            <ShieldCheck className="w-4 h-4" />
            <span>Verified Android Release</span>
          </div>
          <h3 className="text-xl font-bold text-white light:text-slate-900">
            Try Loomora on Android Today
          </h3>
          <p className="text-xs text-slate-400 max-w-md">
            Local-first audio recording, non-destructive editing, and AI meeting notes with total privacy.
          </p>
        </div>
        <a
          href={env.apkUrl}
          download
          className="inline-flex items-center justify-center space-x-2 px-6 py-3.5 rounded-full bg-loomora-primary text-white font-bold text-xs hover:bg-loomora-primary/90 transition-all shadow-lg shrink-0"
        >
          <Download className="w-4 h-4" />
          <span>Tải APK ({env.apkSize})</span>
        </a>
      </div>

      {/* Tags */}
      <div className="flex items-center space-x-2 pt-4">
        <Tag className="w-4 h-4 text-slate-400" />
        <div className="flex flex-wrap gap-2">
          {post.tags.map((tag) => (
            <span key={tag} className="text-xs bg-slate-900 text-slate-400 px-3 py-1 rounded-lg border border-white/5">
              #{tag}
            </span>
          ))}
        </div>
      </div>
    </article>
  );
}
