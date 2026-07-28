import { NextResponse, type NextRequest } from 'next/server';
import { requireAdmin } from '@/lib/portal/authz';
import { listCustomers, getCustomerById } from '@/features/customers/customer-service';

export async function GET(request: NextRequest) {
  await requireAdmin();
  const searchParams = request.nextUrl.searchParams;
  const page = parseInt(searchParams.get('page') ?? '1', 10);
  const pageSize = parseInt(searchParams.get('pageSize') ?? '20', 10);
  const search = searchParams.get('search') ?? undefined;
  const id = searchParams.get('id');

  if (id) {
    const customer = await getCustomerById(id);
    if (!customer) return NextResponse.json({ error: 'Customer not found.' }, { status: 404 });
    return NextResponse.json({ customer });
  }

  const result = await listCustomers({ page, pageSize, search });
  return NextResponse.json(result);
}
