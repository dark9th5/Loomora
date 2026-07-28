export const env = {
  apkUrl: process.env.NEXT_PUBLIC_APK_URL || '/api/download',
  playUrl: process.env.NEXT_PUBLIC_PLAY_URL || 'https://play.google.com/store/apps/details?id=com.loomora',
  latestVersion: process.env.NEXT_PUBLIC_LATEST_VERSION || '1.0.0',
  apkSize: process.env.NEXT_PUBLIC_APK_SIZE || '179.2 MiB',
  apkSha256: process.env.NEXT_PUBLIC_APK_SHA256 || '26d2e3aa7e2e34f20ec8742c969d730b009705ae4219dd0b40070d2a1cc619d3',
  contactEmail: process.env.NEXT_PUBLIC_CONTACT_EMAIL || 'contact@loomora.app',
  supportEmail: process.env.NEXT_PUBLIC_SUPPORT_EMAIL || 'support@loomora.app',
  siteUrl: process.env.NEXT_PUBLIC_SITE_URL || 'https://loomora.app',
};
