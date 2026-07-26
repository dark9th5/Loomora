import { NextResponse, type NextRequest } from 'next/server';

export async function GET(request: NextRequest) {
  const customApkUrl = process.env.NEXT_PUBLIC_APK_URL;

  // If a custom external URL is provided (e.g. GitHub Releases / S3 bucket / CDN), redirect to it
  if (customApkUrl && customApkUrl.startsWith('http')) {
    return NextResponse.redirect(customApkUrl, { status: 307 });
  }

  // Redirect to the static CDN asset at /downloads/app-release-unsigned.apk
  // This bypasses Vercel Serverless Function 4.5MB payload limits and streams via Vercel Edge CDN
  const requestUrl = new URL(request.url);
  const downloadUrl = new URL('/downloads/app-release-unsigned.apk', requestUrl.origin);

  return NextResponse.redirect(downloadUrl.toString(), { status: 307 });
}
