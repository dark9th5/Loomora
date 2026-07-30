import { existsSync } from 'node:fs';
import { join } from 'node:path';
import { NextResponse, type NextRequest } from 'next/server';

export const runtime = 'nodejs';

function getConfiguredApkUrl() {
  const apkUrl = process.env.NEXT_PUBLIC_APK_URL;
  if (apkUrl && /^https?:\/\//i.test(apkUrl) && !apkUrl.includes('android-v1.0.1') && !apkUrl.includes('android-v1.0.2')) {
    return apkUrl;
  }
  return 'https://github.com/dark9th5/Loomora/releases/download/android-v1.0.3/app-release.apk';
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
        error: 'APK artifact is not found.',
        message: 'The requested APK file could not be located.',
      },
      { status: 404 },
    );
  }

  const requestUrl = new URL(request.url);
  const downloadUrl = new URL('/downloads/app-release.apk', requestUrl.origin);

  return NextResponse.redirect(downloadUrl.toString(), { status: 302 });
}
