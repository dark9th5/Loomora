export const env = {
  apkUrl: process.env.NEXT_PUBLIC_APK_URL || '/api/download',
  playUrl: process.env.NEXT_PUBLIC_PLAY_URL || 'https://play.google.com/store/apps/details?id=com.loomora',
  latestVersion: process.env.NEXT_PUBLIC_LATEST_VERSION || '1.0.0',
  apkSize: process.env.NEXT_PUBLIC_APK_SIZE || '14.2 MB',
  apkSha256: process.env.NEXT_PUBLIC_APK_SHA256 || 'e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855',
  contactEmail: process.env.NEXT_PUBLIC_CONTACT_EMAIL || 'contact@loomora.app',
  supportEmail: process.env.NEXT_PUBLIC_SUPPORT_EMAIL || 'support@loomora.app',
  siteUrl: process.env.NEXT_PUBLIC_SITE_URL || 'https://loomora.app',
};
