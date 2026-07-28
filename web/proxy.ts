import { NextResponse } from 'next/server';
import { auth } from '@/auth';
import { canAccessAdmin, type Role } from '@/lib/portal/rbac';

export default auth((request) => {
  const pathname = request.nextUrl.pathname;
  const session = request.auth;
  if ((pathname.startsWith('/account') || pathname.startsWith('/admin')) && !session?.user) {
    return NextResponse.redirect(new URL('/sign-in', request.url));
  }
  if (pathname.startsWith('/admin')) {
    const role = (session?.user?.role ?? 'CUSTOMER') as Role;
    if (!canAccessAdmin(role)) {
      return NextResponse.redirect(new URL('/admin/unauthorized', request.url));
    }
  }
  return NextResponse.next();
});

export const config = {
  matcher: ['/account/:path*', '/admin/:path*'],
};
