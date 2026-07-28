import crypto from 'crypto';
import { z } from 'zod';

export const capabilitySchema = z.enum([
  'CORE_RECORDING',
  'AUDIO_EDITOR',
  'OFFLINE_TRANSCRIPTION',
  'SPEAKER_DIARIZATION',
  'SMART_INSIGHTS',
  'LLM_ENHANCED_INSIGHTS',
  'MODEL_PACK_STANDARD',
  'MODEL_PACK_ADVANCED',
]);

export const licensePayloadSchema = z.object({
  schemaVersion: z.literal(1),
  licenseId: z.string().min(4),
  product: z.literal('loomora'),
  edition: z.string().min(1),
  customerId: z.string().min(1),
  capabilities: z.array(capabilitySchema).min(1),
  issuedAt: z.string().datetime(),
  notBefore: z.string().datetime(),
  expiresAt: z.string().datetime(),
  deviceBinding: z.string().min(1).nullable(),
  licenseVersion: z.number().int().positive(),
});

export const signedLicenseEnvelopeSchema = z.object({
  payload: licensePayloadSchema,
  signatureAlgorithm: z.literal('Ed25519'),
  keyId: z.string().min(1),
  signature: z.string().min(1),
});

export type LicensePayload = z.infer<typeof licensePayloadSchema>;
export type SignedLicenseEnvelope = z.infer<typeof signedLicenseEnvelopeSchema>;

function sortJson(value: unknown): unknown {
  if (Array.isArray(value)) return value.map(sortJson);
  if (value && typeof value === 'object') {
    return Object.keys(value)
      .sort()
      .reduce<Record<string, unknown>>((acc, key) => {
        acc[key] = sortJson((value as Record<string, unknown>)[key]);
        return acc;
      }, {});
  }
  return value;
}

export function canonicalizeLicensePayload(payload: LicensePayload) {
  const parsed = licensePayloadSchema.parse(payload);
  const notBefore = Date.parse(parsed.notBefore);
  const expiresAt = Date.parse(parsed.expiresAt);
  if (Number.isNaN(notBefore) || Number.isNaN(expiresAt) || expiresAt <= notBefore) {
    throw new Error('License expiry must be after notBefore.');
  }
  return JSON.stringify(sortJson(parsed));
}

export function licensePayloadHash(payload: LicensePayload) {
  return crypto.createHash('sha256').update(canonicalizeLicensePayload(payload)).digest('hex');
}

export function signLicensePayload(payload: LicensePayload, keyId: string, privateKeyPem: string): SignedLicenseEnvelope {
  const canonicalPayload = canonicalizeLicensePayload(payload);
  const signature = crypto.sign(null, Buffer.from(canonicalPayload), privateKeyPem).toString('base64');
  return signedLicenseEnvelopeSchema.parse({
    payload,
    signatureAlgorithm: 'Ed25519',
    keyId,
    signature,
  });
}

export function verifyLicenseEnvelope(envelope: SignedLicenseEnvelope, publicKeyPem: string) {
  const parsed = signedLicenseEnvelopeSchema.parse(envelope);
  return crypto.verify(
    null,
    Buffer.from(canonicalizeLicensePayload(parsed.payload)),
    publicKeyPem,
    Buffer.from(parsed.signature, 'base64')
  );
}
