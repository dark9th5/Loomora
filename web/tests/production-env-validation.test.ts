import { describe, expect, it } from 'vitest';
import { assertProductionPortalEnv, type PortalEnv } from '@/lib/portal/env';

function makeEnv(overrides: Partial<PortalEnv> = {}): PortalEnv {
  return {
    DATABASE_URL: 'postgresql://localhost/test',
    AUTH_SECRET: 'a-very-secure-secret-key-1234',
    AUTH_GOOGLE_ID: 'google-id',
    AUTH_GOOGLE_SECRET: 'google-secret',
    SUPER_ADMIN_EMAIL: 'giolanhluc@gmail.com',
    APP_BASE_URL: 'https://loomora.app',
    LICENSE_SIGNING_MODE: 'disabled',
    ...overrides,
  };
}

describe('production environment validation', () => {
  it('passes with all required variables set', () => {
    expect(() => assertProductionPortalEnv(makeEnv())).not.toThrow();
  });

  it('fails when DATABASE_URL is missing', () => {
    expect(() => assertProductionPortalEnv(makeEnv({ DATABASE_URL: undefined }))).toThrow('DATABASE_URL');
  });

  it('fails when AUTH_SECRET is missing', () => {
    expect(() => assertProductionPortalEnv(makeEnv({ AUTH_SECRET: undefined }))).toThrow('AUTH_SECRET');
  });

  it('fails when AUTH_GOOGLE_ID is missing', () => {
    expect(() => assertProductionPortalEnv(makeEnv({ AUTH_GOOGLE_ID: undefined }))).toThrow('AUTH_GOOGLE_ID');
  });

  it('fails when AUTH_GOOGLE_SECRET is missing', () => {
    expect(() => assertProductionPortalEnv(makeEnv({ AUTH_GOOGLE_SECRET: undefined }))).toThrow('AUTH_GOOGLE_SECRET');
  });

  it('fails when APP_BASE_URL is missing', () => {
    expect(() => assertProductionPortalEnv(makeEnv({ APP_BASE_URL: undefined }))).toThrow('APP_BASE_URL');
  });

  it('requires signing-related keys when mode is encrypted-env', () => {
    expect(() => assertProductionPortalEnv(makeEnv({
      LICENSE_SIGNING_MODE: 'encrypted-env',
    }))).toThrow('LICENSE_SIGNING_KEY_ID');
  });

  it('passes encrypted-env with all signing keys', () => {
    expect(() => assertProductionPortalEnv(makeEnv({
      LICENSE_SIGNING_MODE: 'encrypted-env',
      LICENSE_SIGNING_KEY_ID: 'key-1',
      LICENSE_PUBLIC_KEY: 'pub-key',
      LICENSE_PRIVATE_KEY_ENCRYPTED: 'enc-priv',
      LICENSE_PRIVATE_KEY_DECRYPTION_SECRET: 'dec-secret',
    }))).not.toThrow();
  });

  it('reports multiple missing variables', () => {
    try {
      assertProductionPortalEnv(makeEnv({ DATABASE_URL: undefined, AUTH_SECRET: undefined }));
      expect.unreachable();
    } catch (error) {
      const message = (error as Error).message;
      expect(message).toContain('DATABASE_URL');
      expect(message).toContain('AUTH_SECRET');
    }
  });
});
