import { describe, expect, it } from 'vitest';
import { normalizeEmail } from '@/lib/portal/rbac';

describe('email normalization', () => {
  it('lowercases and trims leading/trailing whitespace', () => {
    expect(normalizeEmail('  GioLanhLuc@Gmail.com ')).toBe('giolanhluc@gmail.com');
  });

  it('handles all-uppercase emails', () => {
    expect(normalizeEmail('ADMIN@EXAMPLE.COM')).toBe('admin@example.com');
  });

  it('handles mixed-case with internal spaces preserved', () => {
    // Only trims, does not remove internal whitespace (email spec)
    expect(normalizeEmail('Test@Example.com')).toBe('test@example.com');
  });

  it('handles empty string', () => {
    expect(normalizeEmail('')).toBe('');
    expect(normalizeEmail('   ')).toBe('');
  });

  it('handles special characters in local part', () => {
    expect(normalizeEmail('User+Tag@Domain.COM')).toBe('user+tag@domain.com');
  });

  it('handles unicode normalization at lowercase level', () => {
    expect(normalizeEmail('HÙNG@gmail.com')).toBe('hùng@gmail.com');
  });
});
