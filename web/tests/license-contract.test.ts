import crypto from 'crypto';
import { describe, expect, it } from 'vitest';
import {
  canonicalizeLicensePayload,
  licensePayloadHash,
  signLicensePayload,
  verifyLicenseEnvelope,
  type LicensePayload,
} from '@/lib/license/contract';

const payload: LicensePayload = {
  schemaVersion: 1,
  licenseId: 'lic_test',
  product: 'loomora',
  edition: 'pro',
  customerId: 'cus_test',
  capabilities: ['OFFLINE_TRANSCRIPTION', 'SMART_INSIGHTS'],
  issuedAt: '2026-07-28T00:00:00.000Z',
  notBefore: '2026-07-28T00:00:00.000Z',
  expiresAt: '2027-07-28T00:00:00.000Z',
  deviceBinding: null,
  licenseVersion: 1,
};

describe('license contract', () => {
  it('canonicalizes and hashes deterministically', () => {
    expect(canonicalizeLicensePayload(payload)).toBe(canonicalizeLicensePayload({ ...payload }));
    expect(licensePayloadHash(payload)).toHaveLength(64);
  });

  it('signs and rejects tampered payloads with Ed25519 test key', () => {
    const { publicKey, privateKey } = crypto.generateKeyPairSync('ed25519');
    const envelope = signLicensePayload(
      payload,
      'loomora-test',
      privateKey.export({ type: 'pkcs8', format: 'pem' }).toString()
    );
    const publicPem = publicKey.export({ type: 'spki', format: 'pem' }).toString();
    expect(verifyLicenseEnvelope(envelope, publicPem)).toBe(true);
    expect(verifyLicenseEnvelope({
      ...envelope,
      payload: { ...envelope.payload, edition: 'tampered' },
    }, publicPem)).toBe(false);
  });
});
