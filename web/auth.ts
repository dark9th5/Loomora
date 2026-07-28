import NextAuth from 'next-auth';
import Google from 'next-auth/providers/google';
import { PrismaAdapter } from '@auth/prisma-adapter';
import { prisma } from '@/lib/db/prisma';
import { normalizeEmail, roleForVerifiedEmail } from '@/lib/portal/rbac';
import { readPortalEnv } from '@/lib/portal/env';

const portalEnv = readPortalEnv();

export const { handlers, signIn, signOut, auth } = NextAuth({
  adapter: process.env.DATABASE_URL ? PrismaAdapter(prisma) : undefined,
  providers: [
    Google({
      clientId: portalEnv.AUTH_GOOGLE_ID,
      clientSecret: portalEnv.AUTH_GOOGLE_SECRET,
      allowDangerousEmailAccountLinking: false,
    }),
  ],
  session: {
    strategy: process.env.DATABASE_URL ? 'database' : 'jwt',
    maxAge: 60 * 60 * 24 * 14,
  },
  pages: {
    signIn: '/sign-in',
    error: '/access-denied',
  },
  callbacks: {
    async signIn({ profile }) {
      const email = profile?.email;
      const verified = profile?.email_verified === true;
      if (!email || !verified) return false;
      return true;
    },
    async jwt({ token, profile }) {
      if (profile?.email) {
        token.email = normalizeEmail(profile.email);
        token.role = roleForVerifiedEmail(profile.email, profile.email_verified === true, portalEnv.SUPER_ADMIN_EMAIL);
      }
      return token;
    },
    async session({ session, token, user }) {
      if (session.user) {
        session.user.email = normalizeEmail(session.user.email ?? token.email ?? '');
        session.user.id = user?.id ?? token.sub ?? '';
        session.user.role = (user as { role?: string } | undefined)?.role ?? (token.role as string | undefined) ?? 'CUSTOMER';
      }
      return session;
    },
  },
  events: {
    async signIn({ user }) {
      if (!user.email || !process.env.DATABASE_URL) return;
      const normalizedEmail = normalizeEmail(user.email);
      await prisma.user.update({
        where: { id: user.id },
        data: {
          normalizedEmail,
          role: roleForVerifiedEmail(normalizedEmail, true, portalEnv.SUPER_ADMIN_EMAIL),
          lastLoginAt: new Date(),
        },
      });
    },
  },
});
