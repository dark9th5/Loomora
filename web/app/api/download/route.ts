import { existsSync } from 'node:fs';
import { join } from 'node:path';
import { NextResponse, type NextRequest } from 'next/server';

export const runtime = 'nodejs';

function getConfiguredApkUrl() {
  const apkUrl = process.env.NEXT_PUBLIC_APK_URL;
  return apkUrl && /^https?:\/\//i.test(apkUrl) ? apkUrl : null;
}

export async function GET(request: NextRequest) {
  const configuredApkUrl = getConfiguredApkUrl();
  if (configuredApkUrl) {
    return NextResponse.redirect(configuredApkUrl, { status: 307 });
  }

  const localApkPath = join(process.cwd(), 'public', 'downloads', 'app-release.apk');
  if (!existsSync(localApkPath)) {
    return NextResponse.json(
      {
        error: 'APK artifact is not configured.',
        message: 'Set NEXT_PUBLIC_APK_URL to a public APK artifact URL such as GitHub Releases, S3, or a CDN.',
      },
      { status: 503 },
    );
  }

  const requestUrl = new URL(request.url);
  const downloadUrl = new URL('/downloads/app-release.apk', requestUrl.origin);

  return NextResponse.redirect(downloadUrl.toString(), { status: 307 });
}
