import { NextResponse } from 'next/server';
import { requireSession } from '@/lib/portal/authz';
import { downloadLicenseEnvelope } from '@/features/licenses/license-service';

export async function GET(_: Request, { params }: { params: Promise<{ licenseId: string }> }) {
  const session = await requireSession();
  const userId = session.user?.id;
  if (!userId) return NextResponse.json({ error: 'Unauthorized' }, { status: 401 });

  const { licenseId } = await params;

  if (!process.env.DATABASE_URL) {
    return NextResponse.json({
      licenseId,
      status: 'unavailable',
      note: 'License download requires PostgreSQL ownership lookup and a signed envelope revision. No private storage URL is exposed.',
    }, { status: 404 });
  }

  try {
    const envelope = await downloadLicenseEnvelope(licenseId, userId);

    const json = JSON.stringify(envelope, null, 2);
    return new Response(json, {
      status: 200,
      headers: {
        'Content-Type': 'application/json',
        'Content-Disposition': `attachment; filename="${licenseId}.license"`,
        'Cache-Control': 'no-store',
      },
    });
  } catch (error) {
    const message = error instanceof Error ? error.message : 'Download failed.';
    const status = message === 'Forbidden' ? 403 : message === 'License not found.' ? 404 : 500;
    return NextResponse.json({ error: message }, { status });
  }
}
