export const env = {
  apkUrl: process.env.NEXT_PUBLIC_APK_URL || '/api/download',
  playUrl: process.env.NEXT_PUBLIC_PLAY_URL || 'https://play.google.com/store/apps/details?id=com.loomora',
  latestVersion: process.env.NEXT_PUBLIC_LATEST_VERSION || '1.0.0',
  apkSize: process.env.NEXT_PUBLIC_APK_SIZE || '179.2 MiB',
  apkSha256: process.env.NEXT_PUBLIC_APK_SHA256 || '29c6da7f4c5ab0628195e31231c1580671e530e7e5507a560fb07f8a1fd5a016',
  contactEmail: process.env.NEXT_PUBLIC_CONTACT_EMAIL || 'contact@loomora.app',
  supportEmail: process.env.NEXT_PUBLIC_SUPPORT_EMAIL || 'support@loomora.app',
  siteUrl: process.env.NEXT_PUBLIC_SITE_URL || 'https://loomora.app',
};
