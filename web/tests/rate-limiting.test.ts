import { describe, expect, it } from 'vitest';
import { checkRateLimit } from '@/lib/portal/rate-limit';

describe('rate limiting', () => {
  it('allows requests within the limit', () => {
    const key = 'test-allow-' + Date.now();
    const limit = 3;
    const windowMs = 60_000;

    expect(checkRateLimit(key, limit, windowMs).allowed).toBe(true);
    expect(checkRateLimit(key, limit, windowMs).allowed).toBe(true);
    expect(checkRateLimit(key, limit, windowMs).allowed).toBe(true);
  });

  it('blocks requests after the limit is exceeded', () => {
    const key = 'test-block-' + Date.now();
    const limit = 2;
    const windowMs = 60_000;

    expect(checkRateLimit(key, limit, windowMs).allowed).toBe(true);
    expect(checkRateLimit(key, limit, windowMs).allowed).toBe(true);
    expect(checkRateLimit(key, limit, windowMs).allowed).toBe(false);
    expect(checkRateLimit(key, limit, windowMs).remaining).toBe(0);
  });

  it('resets after the time window expires', () => {
    const key = 'test-reset-' + Date.now();
    const limit = 1;
    const windowMs = 100;
    const now = Date.now();

    expect(checkRateLimit(key, limit, windowMs, now).allowed).toBe(true);
    expect(checkRateLimit(key, limit, windowMs, now).allowed).toBe(false);

    // After window expires
    expect(checkRateLimit(key, limit, windowMs, now + windowMs + 1).allowed).toBe(true);
  });

  it('tracks remaining count correctly', () => {
    const key = 'test-remaining-' + Date.now();
    const limit = 5;
    const windowMs = 60_000;

    const r1 = checkRateLimit(key, limit, windowMs);
    expect(r1.remaining).toBe(4);

    const r2 = checkRateLimit(key, limit, windowMs);
    expect(r2.remaining).toBe(3);
  });

  it('uses separate buckets for different keys', () => {
    const key1 = 'test-separate-1-' + Date.now();
    const key2 = 'test-separate-2-' + Date.now();
    const limit = 1;
    const windowMs = 60_000;

    checkRateLimit(key1, limit, windowMs);
    expect(checkRateLimit(key1, limit, windowMs).allowed).toBe(false);
    // key2 is unaffected
    expect(checkRateLimit(key2, limit, windowMs).allowed).toBe(true);
  });
});
