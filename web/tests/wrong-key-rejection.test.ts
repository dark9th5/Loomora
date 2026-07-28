import crypto from 'crypto';
import { describe, expect, it } from 'vitest';
import {
  signLicensePayload,
  verifyLicenseEnvelope,
  type LicensePayload,
} from '@/lib/license/contract';

const payload: LicensePayload = {
  schemaVersion: 1,
  licenseId: 'lic_wrong_key_test',
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

describe('wrong key rejection', () => {
  it('rejects verification with a different public key', () => {
    // Key pair 1 — used for signing
    const kp1 = crypto.generateKeyPairSync('ed25519');
    // Key pair 2 — different, used for verification
    const kp2 = crypto.generateKeyPairSync('ed25519');

    const envelope = signLicensePayload(
      payload,
      'key-1',
      kp1.privateKey.export({ type: 'pkcs8', format: 'pem' }).toString(),
    );

    const wrongPub = kp2.publicKey.export({ type: 'spki', format: 'pem' }).toString();
    const correctPub = kp1.publicKey.export({ type: 'spki', format: 'pem' }).toString();

    expect(verifyLicenseEnvelope(envelope, wrongPub)).toBe(false);
    expect(verifyLicenseEnvelope(envelope, correctPub)).toBe(true);
  });

  it('rejects corrupted signature', () => {
    const kp = crypto.generateKeyPairSync('ed25519');
    const envelope = signLicensePayload(
      payload,
      'key-corrupt',
      kp.privateKey.export({ type: 'pkcs8', format: 'pem' }).toString(),
    );

    const publicPem = kp.publicKey.export({ type: 'spki', format: 'pem' }).toString();
    const corrupted = { ...envelope, signature: envelope.signature.slice(0, -4) + 'AAAA' };

    expect(verifyLicenseEnvelope(corrupted, publicPem)).toBe(false);
  });

  it('rejects empty signature via Zod schema validation', () => {
    const kp = crypto.generateKeyPairSync('ed25519');
    const envelope = signLicensePayload(
      payload,
      'key-empty',
      kp.privateKey.export({ type: 'pkcs8', format: 'pem' }).toString(),
    );

    const publicPem = kp.publicKey.export({ type: 'spki', format: 'pem' }).toString();
    const emptySig = { ...envelope, signature: '' };

    // verifyLicenseEnvelope parses through Zod first, which rejects empty signatures
    expect(() => verifyLicenseEnvelope(emptySig, publicPem)).toThrow();
  });
});
