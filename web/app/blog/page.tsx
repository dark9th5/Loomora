'use client';

import React, { useState } from 'react';
import Link from 'next/link';
import { getAllPosts, getCategories } from '@/lib/blog';
import { Search, Tag, Calendar, Clock, ArrowRight } from 'lucide-react';

export default function BlogPage() {
  const posts = getAllPosts();
  const categories = getCategories();
  const [selectedCategory, setSelectedCategory] = useState<string | null>(null);
  const [searchQuery, setSearchQuery] = useState('');

  const filteredPosts = posts.filter((post) => {
    const matchesCategory = selectedCategory ? post.category === selectedCategory : true;
    const matchesSearch =
      post.title.toLowerCase().includes(searchQuery.toLowerCase()) ||
      post.excerpt.toLowerCase().includes(searchQuery.toLowerCase());
    return matchesCategory && matchesSearch;
  });

  return (
    <div className="max-w-6xl mx-auto px-4 sm:px-6 lg:px-8 py-16 space-y-12">
      <div className="text-center space-y-4 max-w-2xl mx-auto">
        <h1 className="text-4xl sm:text-5xl font-extrabold text-white light:text-slate-900">
          Loomora Blog &amp; Insights
        </h1>
        <p className="text-slate-400 light:text-slate-600 text-base">
          Articles on audio engineering, local-first privacy, AI note-taking, and mobile reliability.
        </p>
      </div>

      {/* Search & Category Filter Controls */}
      <div className="flex flex-col sm:flex-row items-center justify-between gap-4 glass p-4 rounded-2xl border border-white/10">
        <div className="relative w-full sm:w-72">
          <Search className="w-4 h-4 absolute left-3 top-3.5 text-slate-400" />
          <input
            type="text"
            placeholder="Search articles..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="w-full pl-9 pr-4 py-2.5 rounded-xl bg-slate-900 border border-white/10 text-white placeholder-slate-500 text-xs focus:outline-none focus:border-loomora-primary"
          />
        </div>

        <div className="flex items-center space-x-2 overflow-x-auto w-full sm:w-auto pb-2 sm:pb-0">
          <button
            onClick={() => setSelectedCategory(null)}
            className={`px-3 py-1.5 rounded-lg text-xs font-semibold whitespace-nowrap transition-colors ${
              selectedCategory === null
                ? 'bg-loomora-primary text-white'
                : 'bg-slate-900 text-slate-400 hover:text-white'
            }`}
          >
            All Categories
          </button>
          {categories.map((cat) => (
            <button
              key={cat}
              onClick={() => setSelectedCategory(cat)}
              className={`px-3 py-1.5 rounded-lg text-xs font-semibold whitespace-nowrap transition-colors ${
                selectedCategory === cat
                  ? 'bg-loomora-primary text-white'
                  : 'bg-slate-900 text-slate-400 hover:text-white'
              }`}
            >
              {cat}
            </button>
          ))}
        </div>
      </div>

      {/* Post Grid */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-8">
        {filteredPosts.map((post) => (
          <article
            key={post.slug}
            className="glass rounded-3xl p-6 border border-white/10 flex flex-col justify-between hover:border-loomora-primary/50 transition-all space-y-4"
          >
            <div className="space-y-3">
              <div className="flex items-center justify-between text-[11px] text-slate-400">
                <span className="font-bold text-loomora-secondary uppercase">{post.category}</span>
                <span className="flex items-center space-x-1"><Clock className="w-3 h-3" /><span>{post.readTime}</span></span>
              </div>
              <h2 className="text-xl font-bold text-white light:text-slate-900 leading-snug">
                <Link href={`/blog/${post.slug}`} className="hover:text-loomora-container transition-colors">
                  {post.title}
                </Link>
              </h2>
              <p className="text-xs text-slate-400 leading-relaxed line-clamp-3">
                {post.excerpt}
              </p>
            </div>

            <div className="pt-4 border-t border-white/5 flex items-center justify-between">
              <span className="text-[11px] text-slate-500">{post.date}</span>
              <Link
                href={`/blog/${post.slug}`}
                className="inline-flex items-center space-x-1 text-xs font-semibold text-loomora-secondary hover:underline"
              >
                <span>Read Article</span>
                <ArrowRight className="w-3.5 h-3.5" />
              </Link>
            </div>
          </article>
        ))}
      </div>
    </div>
  );
}
