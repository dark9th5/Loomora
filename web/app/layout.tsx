import type { Metadata } from 'next';
import './globals.css';
import { ThemeProvider } from '@/components/ThemeContext';
import Navbar from '@/components/Navbar';
import Footer from '@/components/Footer';
import { env } from '@/lib/env';

export const metadata: Metadata = {
  metadataBase: new URL(env.siteUrl),
  title: 'Loomora — Smart Voice Recorder & AI Notes for Android',
  description: 'Record conversations, lectures, and voice notes cleanly. After local models are installed, generate transcripts and evidence-linked extractive notes on device.',
  keywords: ['Android Voice Recorder', 'Local-First Audio', 'Offline Transcription', 'Extractive Meeting Notes', 'Meeting Recorder'],
  authors: [{ name: 'Loomora Team' }],
  openGraph: {
    title: 'Loomora — Smart Voice Recorder & AI Notes for Android',
    description: 'Local-first Android voice recorder with offline transcription, extractive insights, non-destructive editing, and privacy-first defaults.',
    url: env.siteUrl,
    siteName: 'Loomora',
    locale: 'en_US',
    type: 'website',
  },
  twitter: {
    card: 'summary_large_image',
    title: 'Loomora — Smart Voice Recorder & AI Notes',
    description: 'Record conversations cleanly on Android. Generate local transcripts and evidence-linked extractive notes after required models are installed.',
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
    description: 'Smart voice recorder for Android with local-first privacy, offline transcription, and extractive meeting notes.',
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
