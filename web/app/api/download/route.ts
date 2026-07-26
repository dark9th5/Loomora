import { NextResponse } from 'next/server';
import fs from 'fs';
import path from 'path';

export async function GET() {
  const customApkUrl = process.env.NEXT_PUBLIC_APK_URL;

  // If a custom external URL is provided (e.g. GitHub Releases / S3 bucket / CDN), redirect to it
  if (customApkUrl && customApkUrl.startsWith('http')) {
    return NextResponse.redirect(customApkUrl, { status: 307 });
  }

  // Path to static APK asset in public/downloads/
  const filePath = path.join(process.cwd(), 'public', 'downloads', 'app-release-unsigned.apk');

  if (fs.existsSync(filePath)) {
    const fileBuffer = fs.readFileSync(filePath);
    return new NextResponse(fileBuffer, {
      status: 200,
      headers: {
        'Content-Type': 'application/vnd.android.package-archive',
        'Content-Disposition': 'attachment; filename="app-release-unsigned.apk"',
        'Content-Length': fileBuffer.length.toString(),
        'Cache-Control': 'public, max-age=3600, must-revalidate',
      },
    });
  }

  // Fallback: If static file is missing, return friendly JSON error instead of blank 404 HTML
  return NextResponse.json(
    {
      error: 'APK download file is temporarily unavailable.',
      message: 'Please check back shortly or download directly via Google Play Store.',
      playUrl: process.env.NEXT_PUBLIC_PLAY_URL || 'https://play.google.com/store/apps/details?id=com.loomora',
    },
    { status: 404 }
  );
}
