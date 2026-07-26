'use client';

import React from 'react';
import Link from 'next/link';
import { useTheme } from '@/components/ThemeContext';
import { Download, ShieldCheck, CheckCircle2, FileCheck, Smartphone, Info, AlertTriangle } from 'lucide-react';
import { env } from '@/lib/env';

export default function DownloadPage() {
  const { t } = useTheme();

  return (
    <div className="max-w-5xl mx-auto px-4 sm:px-6 lg:px-8 py-16 space-y-12">
      {/* Header */}
      <div className="text-center space-y-4">
        <h1 className="text-4xl sm:text-5xl font-extrabold text-white light:text-slate-900">
          Download Loomora for Android
        </h1>
        <p className="text-slate-400 light:text-slate-600 text-base max-w-xl mx-auto">
          Get the signed, production release of Loomora directly or visit Google Play.
        </p>
      </div>

      {/* Primary Download Card */}
      <div className="glass p-8 sm:p-10 rounded-3xl border border-loomora-primary/30 space-y-8 shadow-2xl">
        <div className="grid grid-cols-1 md:grid-cols-2 gap-8 items-center">
          <div className="space-y-4">
            <div className="inline-flex items-center space-x-2 px-3 py-1 rounded-full bg-green-500/20 text-green-400 text-xs font-bold">
              <ShieldCheck className="w-4 h-4" />
              <span>Production Release (Verified Signed APK)</span>
            </div>
            <h2 className="text-2xl font-bold text-white light:text-slate-900">
              Loomora v{env.latestVersion}
            </h2>
            <p className="text-xs text-slate-400 leading-relaxed">
              Fully standalone APK compiled directly from source. Requires zero Google Play services for core local audio recording.
            </p>

            <div className="space-y-2 text-xs text-slate-300">
              <div className="flex justify-between border-b border-white/5 pb-1">
                <span className="text-slate-400">File Size:</span>
                <span className="font-mono text-white">{env.apkSize}</span>
              </div>
              <div className="flex justify-between border-b border-white/5 pb-1">
                <span className="text-slate-400">Minimum Android Version:</span>
                <span className="font-mono text-white">Android 8.0 (API level 26)</span>
              </div>
              <div className="flex justify-between border-b border-white/5 pb-1">
                <span className="text-slate-400">Architecture:</span>
                <span className="font-mono text-white">arm64-v8a / armeabi-v7a / x86_64</span>
              </div>
            </div>
          </div>

          <div className="space-y-4 text-center md:text-left">
            <a
              href={env.apkUrl}
              download
              className="w-full inline-flex items-center justify-center space-x-3 px-8 py-4 rounded-full bg-loomora-primary text-white font-bold text-base hover:bg-loomora-primary/90 transition-all shadow-xl shadow-loomora-primary/30"
            >
              <Download className="w-5 h-5" />
              <span>Download Direct APK ({env.apkSize})</span>
            </a>

            <a
              href={env.playUrl}
              target="_blank"
              rel="noopener noreferrer"
              className="w-full inline-flex items-center justify-center space-x-3 px-8 py-4 rounded-full glass border border-white/20 text-white light:text-slate-800 font-semibold text-sm hover:bg-white/10 transition-all opacity-80"
            >
              <Smartphone className="w-5 h-5 text-loomora-secondary" />
              <span>Google Play Listing (Store Verification Pending)</span>
            </a>
          </div>
        </div>

        {/* SHA256 Checksum */}
        <div className="bg-slate-950 p-4 rounded-2xl border border-white/5 space-y-1.5">
          <div className="flex items-center justify-between text-xs text-slate-400">
            <span className="font-semibold flex items-center space-x-1">
              <FileCheck className="w-3.5 h-3.5 text-loomora-secondary" />
              <span>SHA-256 Package Checksum:</span>
            </span>
          </div>
          <p className="font-mono text-[11px] text-loomora-container break-all">
            {env.apkSha256}
          </p>
        </div>
      </div>

      {/* Installation Guide & Requirements */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
        <div className="glass p-8 rounded-3xl border border-white/10 space-y-4">
          <h3 className="text-lg font-bold text-white light:text-slate-900 flex items-center space-x-2">
            <Info className="w-5 h-5 text-loomora-secondary" />
            <span>How to Install Direct APK</span>
          </h3>
          <ol className="space-y-3 text-xs text-slate-300 list-decimal pl-4 leading-relaxed">
            <li>Download the APK file to your Android device.</li>
            <li>Open your device Downloads and tap <code className="bg-slate-900 px-1 py-0.5 rounded text-loomora-secondary">app-release-unsigned.apk</code>.</li>
            <li>If prompted by Android, grant temporary permission to &quot;Install from unknown sources&quot; for your browser.</li>
            <li>Tap Install and open Loomora immediately with zero setup or login required.</li>
          </ol>
        </div>

        <div className="glass p-8 rounded-3xl border border-white/10 space-y-4">
          <h3 className="text-lg font-bold text-white light:text-slate-900 flex items-center space-x-2">
            <AlertTriangle className="w-5 h-5 text-yellow-400" />
            <span>System Requirements</span>
          </h3>
          <ul className="space-y-3 text-xs text-slate-300 leading-relaxed">
            <li className="flex items-start space-x-2"><CheckCircle2 className="w-4 h-4 text-loomora-secondary shrink-0 mt-0.5" /><span>Android 8.0 Oreo (API Level 26) or higher.</span></li>
            <li className="flex items-start space-x-2"><CheckCircle2 className="w-4 h-4 text-loomora-secondary shrink-0 mt-0.5" /><span>Microphone hardware permission for voice capture.</span></li>
            <li className="flex items-start space-x-2"><CheckCircle2 className="w-4 h-4 text-loomora-secondary shrink-0 mt-0.5" /><span>Minimum 50 MB free internal storage space.</span></li>
          </ul>
        </div>
      </div>
    </div>
  );
}
