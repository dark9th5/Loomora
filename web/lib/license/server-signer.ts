import 'server-only';
import crypto from 'crypto';
import { readPortalEnv } from '@/lib/portal/env';
import { signLicensePayload, type LicensePayload } from '@/lib/license/contract';

type EncryptedPrivateKey = {
  version: 1;
  algorithm: 'aes-256-gcm';
  iv: string;
  tag: string;
  ciphertext: string;
};

function decryptPrivateKey(encryptedJson: string, secret: string) {
  const encrypted = JSON.parse(encryptedJson) as EncryptedPrivateKey;
  if (encrypted.version !== 1 || encrypted.algorithm !== 'aes-256-gcm') {
    throw new Error('Unsupported encrypted private key format.');
  }
  const key = crypto.createHash('sha256').update(secret).digest();
  const decipher = crypto.createDecipheriv('aes-256-gcm', key, Buffer.from(encrypted.iv, 'base64'));
  decipher.setAuthTag(Buffer.from(encrypted.tag, 'base64'));
  const plaintext = Buffer.concat([
    decipher.update(Buffer.from(encrypted.ciphertext, 'base64')),
    decipher.final(),
  ]);
  return plaintext.toString('utf8');
}

export function signWithConfiguredServerKey(payload: LicensePayload) {
  const env = readPortalEnv();
  if (env.LICENSE_SIGNING_MODE !== 'encrypted-env') {
    throw new Error('Server signing is not configured.');
  }
  if (!env.LICENSE_SIGNING_KEY_ID || !env.LICENSE_PRIVATE_KEY_ENCRYPTED || !env.LICENSE_PRIVATE_KEY_DECRYPTION_SECRET) {
    throw new Error('Server signing environment is incomplete.');
  }
  const privateKeyPem = decryptPrivateKey(
    env.LICENSE_PRIVATE_KEY_ENCRYPTED,
    env.LICENSE_PRIVATE_KEY_DECRYPTION_SECRET
  );
  return signLicensePayload(payload, env.LICENSE_SIGNING_KEY_ID, privateKeyPem);
}
