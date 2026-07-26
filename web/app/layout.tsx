import type { Metadata } from 'next';
import './globals.css';
import { ThemeProvider } from '@/components/ThemeContext';
import Navbar from '@/components/Navbar';
import Footer from '@/components/Footer';
import { env } from '@/lib/env';

export const metadata: Metadata = {
  metadataBase: new URL(env.siteUrl),
  title: 'Loomora — Smart Voice Recorder & AI Notes for Android',
  description: 'Record conversations, lectures, and voice notes cleanly. Turn spoken ideas into structured summaries, key points, and action items with 100% offline local privacy guarantee.',
  keywords: ['Android Voice Recorder', 'AI Notes', 'Local-First Audio', 'Voice Transcription', 'Speech Clarity', 'Meeting Recorder'],
  authors: [{ name: 'Loomora Team' }],
  openGraph: {
    title: 'Loomora — Smart Voice Recorder & AI Notes for Android',
    description: '100% Local-first Android voice recorder with smart AI summaries, non-destructive editing, and offline privacy.',
    url: env.siteUrl,
    siteName: 'Loomora',
    locale: 'en_US',
    type: 'website',
  },
  twitter: {
    card: 'summary_large_image',
    title: 'Loomora — Smart Voice Recorder & AI Notes',
    description: 'Record conversations cleanly on Android. Turn spoken ideas into structured summaries with absolute privacy.',
  },
  robots: {
    index: true,
    follow: true,
  },
};

export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  const jsonLd = {
    '@context': 'https://schema.org',
    '@type': 'SoftwareApplication',
    name: 'Loomora',
    operatingSystem: 'Android 8.0+',
    applicationCategory: 'ProductivityApplication',
    offers: {
      '@type': 'Offer',
      price: '0.00',
      priceCurrency: 'USD',
    },
    description: 'Smart Voice Recorder & AI Notes for Android with 100% local-first privacy.',
  };

  return (
    <html lang="en" className="dark" style={{ colorScheme: 'dark' }}>
      <head>
        <script
          type="application/ld+json"
          dangerouslySetInnerHTML={{ __html: JSON.stringify(jsonLd) }}
        />
      </head>
      <body className="bg-slate-950 text-slate-100 min-h-screen flex flex-col antialiased selection:bg-loomora-primary selection:text-white transition-colors duration-200">
        <ThemeProvider>
          <Navbar />
          <main className="flex-grow">{children}</main>
          <Footer />
        </ThemeProvider>
      </body>
    </html>
  );
}
