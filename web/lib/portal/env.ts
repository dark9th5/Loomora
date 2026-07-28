import { z } from 'zod';

const optionalUrl = z.string().url().optional().or(z.literal(''));

export const portalEnvSchema = z.object({
  DATABASE_URL: z.string().min(1).optional(),
  AUTH_SECRET: z.string().min(16).optional(),
  AUTH_GOOGLE_ID: z.string().min(1).optional(),
  AUTH_GOOGLE_SECRET: z.string().min(1).optional(),
  SUPER_ADMIN_EMAIL: z.string().email().default('giolanhluc@gmail.com'),
  APP_BASE_URL: optionalUrl,
  LICENSE_SIGNING_MODE: z.enum(['external', 'encrypted-env', 'offline-cli', 'disabled']).default('disabled'),
  LICENSE_SIGNING_KEY_ID: z.string().min(1).optional(),
  LICENSE_PUBLIC_KEY: z.string().min(1).optional(),
  LICENSE_PRIVATE_KEY_ENCRYPTED: z.string().min(1).optional(),
  LICENSE_PRIVATE_KEY_DECRYPTION_SECRET: z.string().min(1).optional(),
  BLOB_READ_WRITE_TOKEN: z.string().min(1).optional(),
  RATE_LIMIT_PROVIDER_URL: optionalUrl,
  RATE_LIMIT_PROVIDER_TOKEN: z.string().min(1).optional(),
  EMAIL_PROVIDER_API_KEY: z.string().min(1).optional(),
  SUPPORT_FROM_EMAIL: z.string().email().optional(),
});

export type PortalEnv = z.infer<typeof portalEnvSchema>;

export function readPortalEnv(): PortalEnv {
  return portalEnvSchema.parse(process.env);
}

export function assertProductionPortalEnv(env = readPortalEnv()) {
  const missing: string[] = [];
  if (!env.DATABASE_URL) missing.push('DATABASE_URL');
  if (!env.AUTH_SECRET) missing.push('AUTH_SECRET');
  if (!env.AUTH_GOOGLE_ID) missing.push('AUTH_GOOGLE_ID');
  if (!env.AUTH_GOOGLE_SECRET) missing.push('AUTH_GOOGLE_SECRET');
  if (!env.APP_BASE_URL) missing.push('APP_BASE_URL');
  if (env.LICENSE_SIGNING_MODE === 'encrypted-env') {
    if (!env.LICENSE_SIGNING_KEY_ID) missing.push('LICENSE_SIGNING_KEY_ID');
    if (!env.LICENSE_PUBLIC_KEY) missing.push('LICENSE_PUBLIC_KEY');
    if (!env.LICENSE_PRIVATE_KEY_ENCRYPTED) missing.push('LICENSE_PRIVATE_KEY_ENCRYPTED');
    if (!env.LICENSE_PRIVATE_KEY_DECRYPTION_SECRET) missing.push('LICENSE_PRIVATE_KEY_DECRYPTION_SECRET');
  }
  if (missing.length > 0) {
    throw new Error(`Missing required production portal environment variables: ${missing.join(', ')}`);
  }
}
