'use client';

import React from 'react';
import Link from 'next/link';
import { useTheme } from './ThemeContext';
import { Mic, Shield, Lock, ExternalLink } from 'lucide-react';

export default function Footer() {
  const { t } = useTheme();

  return (
    <footer className="bg-slate-950 light:bg-slate-50 border-t border-white/10 light:border-slate-200 text-slate-400 light:text-slate-600 transition-colors">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-12">
        <div className="grid grid-cols-1 md:grid-cols-4 gap-8 mb-12">
          {/* Column 1: Brand Info */}
          <div className="space-y-4">
            <div className="flex items-center space-x-3">
              <div className="w-8 h-8 rounded-full bg-loomora-primary flex items-center justify-center text-white">
                <Mic className="w-4 h-4" />
              </div>
              <span className="text-lg font-bold text-white light:text-slate-900">Loomora</span>
            </div>
            <p className="text-sm leading-relaxed text-slate-400 light:text-slate-600">
              Smart Voice Recorder &amp; AI Notes for Android. Local-first, private, and dependable.
            </p>
            <div className="flex items-center space-x-2 text-xs text-loomora-secondary font-medium">
              <Shield className="w-4 h-4" />
              <span>100% Local-First Guarantee</span>
            </div>
          </div>

          {/* Column 2: Product Links */}
          <div className="space-y-3">
            <h4 className="text-sm font-semibold text-white light:text-slate-900 uppercase tracking-wider">Product</h4>
            <ul className="space-y-2 text-sm">
              <li><Link href="/features" className="hover:text-white light:hover:text-slate-900 transition-colors">Features Overview</Link></li>
              <li><Link href="/pricing" className="hover:text-white light:hover:text-slate-900 transition-colors">Free vs Pro Pricing</Link></li>
              <li><Link href="/download" className="hover:text-white light:hover:text-slate-900 transition-colors">Download Android APK</Link></li>
              <li><Link href="/pro" className="hover:text-white light:hover:text-slate-900 transition-colors">Activate Pro Key</Link></li>
            </ul>
          </div>

          {/* Column 3: Resources & Blog */}
          <div className="space-y-3">
            <h4 className="text-sm font-semibold text-white light:text-slate-900 uppercase tracking-wider">Resources</h4>
            <ul className="space-y-2 text-sm">
              <li><Link href="/blog" className="hover:text-white light:hover:text-slate-900 transition-colors">Engineering &amp; Product Blog</Link></li>
              <li><Link href="/contact" className="hover:text-white light:hover:text-slate-900 transition-colors">Contact Support</Link></li>
              <li><Link href="/rss.xml" className="hover:text-white light:hover:text-slate-900 transition-colors inline-flex items-center space-x-1"><span>RSS Feed</span> <ExternalLink className="w-3 h-3" /></Link></li>
            </ul>
          </div>

          {/* Column 4: Legal & Privacy */}
          <div className="space-y-3">
            <h4 className="text-sm font-semibold text-white light:text-slate-900 uppercase tracking-wider">Legal &amp; Privacy</h4>
            <ul className="space-y-2 text-sm">
              <li><Link href="/privacy" className="hover:text-white light:hover:text-slate-900 transition-colors">{t.footer.privacy}</Link></li>
              <li><Link href="/terms" className="hover:text-white light:hover:text-slate-900 transition-colors">{t.footer.terms}</Link></li>
              <li><Link href="/data-deletion" className="hover:text-white light:hover:text-slate-900 transition-colors">{t.footer.deletion}</Link></li>
            </ul>
          </div>
        </div>

        <div className="pt-8 border-t border-white/5 light:border-slate-200 flex flex-col sm:flex-row items-center justify-between text-xs text-slate-500">
          <p>&copy; {new Date().getFullYear()} Loomora Audio. {t.footer.rights}</p>
          <div className="flex items-center space-x-4 mt-4 sm:mt-0">
            <span className="flex items-center space-x-1">
              <Lock className="w-3.5 h-3.5" />
              <span>HTTPS Enforced</span>
            </span>
          </div>
        </div>
      </div>
    </footer>
  );
}
