export const env = {
  apkUrl: process.env.NEXT_PUBLIC_APK_URL || '/api/download',
  playUrl: process.env.NEXT_PUBLIC_PLAY_URL || 'https://play.google.com/store/apps/details?id=com.loomora',
  latestVersion: process.env.NEXT_PUBLIC_LATEST_VERSION || '1.0.2',
  apkSize: process.env.NEXT_PUBLIC_APK_SIZE || '180.4 MiB',
  apkSha256: process.env.NEXT_PUBLIC_APK_SHA256 || 'fcaae770f9e39128a01d00d7852e3b2df6d415e3d292f24c8806a425399cc3a3',
  contactEmail: process.env.NEXT_PUBLIC_CONTACT_EMAIL || 'contact@loomora.app',
  supportEmail: process.env.NEXT_PUBLIC_SUPPORT_EMAIL || 'support@loomora.app',
  siteUrl: process.env.NEXT_PUBLIC_SITE_URL || 'https://loomora.app',
};
