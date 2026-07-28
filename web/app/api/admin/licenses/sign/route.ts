import { NextResponse } from 'next/server';
import { requireAdmin } from '@/lib/portal/authz';
import { readPortalEnv } from '@/lib/portal/env';
import { licenseDraftSchema } from '@/lib/portal/validation';
import { licensePayloadHash } from '@/lib/license/contract';
import { signWithConfiguredServerKey } from '@/lib/license/server-signer';

export async function POST(request: Request) {
  await requireAdmin();
  const env = readPortalEnv();
  const parsed = licenseDraftSchema.safeParse(await request.json().catch(() => null));
  if (!parsed.success) {
    return NextResponse.json({ error: 'Invalid license payload.' }, { status: 400 });
  }
  if (env.LICENSE_SIGNING_MODE !== 'encrypted-env') {
    return NextResponse.json({
      status: 'unsigned',
      payloadHash: licensePayloadHash(parsed.data),
      note: 'Secure server signing is not configured. Export canonical payload for external offline signing.',
    }, { status: 503 });
  }
  const envelope = signWithConfiguredServerKey(parsed.data);
  return NextResponse.json({
    status: 'signed',
    payloadHash: licensePayloadHash(parsed.data),
    envelope,
  });
}
