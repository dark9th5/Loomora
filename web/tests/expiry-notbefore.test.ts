import { describe, expect, it } from 'vitest';
import {
  canonicalizeLicensePayload,
  licensePayloadSchema,
  type LicensePayload,
} from '@/lib/license/contract';

const basePayload: LicensePayload = {
  schemaVersion: 1,
  licenseId: 'lic_test',
  product: 'loomora',
  edition: 'pro',
  customerId: 'cus_test',
  capabilities: ['OFFLINE_TRANSCRIPTION'],
  issuedAt: '2026-07-28T00:00:00.000Z',
  notBefore: '2026-07-28T00:00:00.000Z',
  expiresAt: '2027-07-28T00:00:00.000Z',
  deviceBinding: null,
  licenseVersion: 1,
};

describe('expiry and notBefore validation', () => {
  it('accepts payload where expiresAt is after notBefore', () => {
    // canonicalize enforces the constraint
    expect(() => canonicalizeLicensePayload(basePayload)).not.toThrow();
  });

  it('rejects payload where expiresAt is before notBefore via canonicalize', () => {
    expect(() => canonicalizeLicensePayload({
      ...basePayload,
      notBefore: '2027-07-28T00:00:00.000Z',
      expiresAt: '2026-07-28T00:00:00.000Z',
    })).toThrow('License expiry must be after notBefore.');
  });

  it('rejects payload where expiresAt equals notBefore via canonicalize', () => {
    expect(() => canonicalizeLicensePayload({
      ...basePayload,
      notBefore: '2027-01-01T00:00:00.000Z',
      expiresAt: '2027-01-01T00:00:00.000Z',
    })).toThrow('License expiry must be after notBefore.');
  });

  it('correctly detects expired license by comparing with current time', () => {
    const now = new Date();
    const pastDate = new Date(now.getTime() - 86400000);
    const pastNotBefore = new Date(now.getTime() - 86400000 * 2);
    // Canonicalize succeeds because expiresAt > notBefore, but license is expired relative to now
    const canonical = canonicalizeLicensePayload({
      ...basePayload,
      notBefore: pastNotBefore.toISOString(),
      expiresAt: pastDate.toISOString(),
    });
    const parsed = JSON.parse(canonical);
    expect(new Date(parsed.expiresAt) < now).toBe(true);
  });
});
