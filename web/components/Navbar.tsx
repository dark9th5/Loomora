'use client';

import React, { useState } from 'react';
import Link from 'next/link';
import { useTheme } from './ThemeContext';
import { Mic, Sun, Moon, Monitor, Globe, Menu, X, ShieldCheck } from 'lucide-react';

export default function Navbar() {
  const { theme, setTheme, lang, setLang, t } = useTheme();
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);

  return (
    <header className="sticky top-0 z-50 glass border-b border-white/10 dark:border-white/10 light:border-black/10 transition-colors">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 h-16 flex items-center justify-between">
        {/* Brand Logo */}
        <Link href="/" className="flex items-center space-x-3">
          <div className="w-9 h-9 rounded-full bg-loomora-primary flex items-center justify-center text-white shadow-lg shadow-loomora-primary/30">
            <Mic className="w-5 h-5" />
          </div>
          <span className="text-xl font-bold tracking-tight text-white light:text-slate-900">
            Loomora
          </span>
        </Link>

        {/* Desktop Nav Links */}
        <nav className="hidden md:flex items-center space-x-6">
          <Link href="/" className="text-sm font-medium text-slate-300 hover:text-white light:text-slate-600 light:hover:text-slate-900 transition-colors">
            {t.nav.home}
          </Link>
          <Link href="/features" className="text-sm font-medium text-slate-300 hover:text-white light:text-slate-600 light:hover:text-slate-900 transition-colors">
            {t.nav.features}
          </Link>
          <Link href="/pricing" className="text-sm font-medium text-slate-300 hover:text-white light:text-slate-600 light:hover:text-slate-900 transition-colors">
            {t.nav.pricing}
          </Link>
          <Link href="/download" className="text-sm font-medium text-slate-300 hover:text-white light:text-slate-600 light:hover:text-slate-900 transition-colors">
            {t.nav.download}
          </Link>
          <Link href="/blog" className="text-sm font-medium text-slate-300 hover:text-white light:text-slate-600 light:hover:text-slate-900 transition-colors">
            {t.nav.blog}
          </Link>
          <Link href="/contact" className="text-sm font-medium text-slate-300 hover:text-white light:text-slate-600 light:hover:text-slate-900 transition-colors">
            {t.nav.contact}
          </Link>
        </nav>

        {/* Controls & CTA */}
        <div className="hidden md:flex items-center space-x-4">
          {/* Language Selector */}
          <button
            onClick={() => setLang(lang === 'en' ? 'vi' : 'en')}
            className="flex items-center space-x-1.5 text-xs font-semibold px-2.5 py-1.5 rounded-lg border border-white/10 light:border-black/10 hover:bg-white/5 light:hover:bg-black/5 text-slate-300 light:text-slate-700 transition-colors"
            aria-label="Switch Language"
          >
            <Globe className="w-3.5 h-3.5" />
            <span>{lang.toUpperCase()}</span>
          </button>

          {/* Theme Switcher */}
          <div className="flex items-center bg-slate-900/50 light:bg-slate-100 p-1 rounded-lg border border-white/10 light:border-black/10">
            <button
              onClick={() => setTheme('light')}
              className={`p-1.5 rounded-md transition-colors ${theme === 'light' ? 'bg-loomora-primary text-white' : 'text-slate-400 hover:text-white light:text-slate-600'}`}
              aria-label="Light Theme"
            >
              <Sun className="w-3.5 h-3.5" />
            </button>
            <button
              onClick={() => setTheme('dark')}
              className={`p-1.5 rounded-md transition-colors ${theme === 'dark' ? 'bg-loomora-primary text-white' : 'text-slate-400 hover:text-white light:text-slate-600'}`}
              aria-label="Dark Theme"
            >
              <Moon className="w-3.5 h-3.5" />
            </button>
            <button
              onClick={() => setTheme('system')}
              className={`p-1.5 rounded-md transition-colors ${theme === 'system' ? 'bg-loomora-primary text-white' : 'text-slate-400 hover:text-white light:text-slate-600'}`}
              aria-label="System Theme"
            >
              <Monitor className="w-3.5 h-3.5" />
            </button>
          </div>

          {/* Buy Pro Button */}
          <Link
            href="/pro"
            className="inline-flex items-center space-x-1.5 text-xs font-semibold px-4 py-2 rounded-full bg-loomora-primary text-white hover:bg-loomora-primary/90 transition-all shadow-md shadow-loomora-primary/20"
          >
            <ShieldCheck className="w-4 h-4" />
            <span>{t.nav.buyPro}</span>
          </Link>
        </div>

        {/* Mobile Menu Toggle Button */}
        <div className="flex md:hidden items-center space-x-2">
          <button
            onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
            className="p-2 rounded-lg text-slate-300 light:text-slate-700 hover:bg-white/10 light:hover:bg-black/10"
            aria-label="Toggle Menu"
          >
            {mobileMenuOpen ? <X className="w-6 h-6" /> : <Menu className="w-6 h-6" />}
          </button>
        </div>
      </div>

      {/* Mobile Drawer Menu */}
      {mobileMenuOpen && (
        <div className="md:hidden glass border-b border-white/10 px-4 pt-2 pb-6 space-y-3">
          <Link
            href="/"
            onClick={() => setMobileMenuOpen(false)}
            className="block text-base font-medium text-slate-200 light:text-slate-800 py-2 border-b border-white/5"
          >
            {t.nav.home}
          </Link>
          <Link
            href="/features"
            onClick={() => setMobileMenuOpen(false)}
            className="block text-base font-medium text-slate-200 light:text-slate-800 py-2 border-b border-white/5"
          >
            {t.nav.features}
          </Link>
          <Link
            href="/pricing"
            onClick={() => setMobileMenuOpen(false)}
            className="block text-base font-medium text-slate-200 light:text-slate-800 py-2 border-b border-white/5"
          >
            {t.nav.pricing}
          </Link>
          <Link
            href="/download"
            onClick={() => setMobileMenuOpen(false)}
            className="block text-base font-medium text-slate-200 light:text-slate-800 py-2 border-b border-white/5"
          >
            {t.nav.download}
          </Link>
          <Link
            href="/blog"
            onClick={() => setMobileMenuOpen(false)}
            className="block text-base font-medium text-slate-200 light:text-slate-800 py-2 border-b border-white/5"
          >
            {t.nav.blog}
          </Link>
          <Link
            href="/contact"
            onClick={() => setMobileMenuOpen(false)}
            className="block text-base font-medium text-slate-200 light:text-slate-800 py-2 border-b border-white/5"
          >
            {t.nav.contact}
          </Link>

          <div className="pt-2 flex items-center justify-between">
            <button
              onClick={() => setLang(lang === 'en' ? 'vi' : 'en')}
              className="flex items-center space-x-1.5 text-xs font-semibold px-3 py-2 rounded-lg border border-white/10 text-slate-300"
            >
              <Globe className="w-4 h-4" />
              <span>Language: {lang.toUpperCase()}</span>
            </button>

            <Link
              href="/pro"
              onClick={() => setMobileMenuOpen(false)}
              className="inline-flex items-center space-x-1.5 text-xs font-semibold px-4 py-2 rounded-full bg-loomora-primary text-white"
            >
              <span>{t.nav.buyPro}</span>
            </Link>
          </div>
        </div>
      )}
    </header>
  );
}
