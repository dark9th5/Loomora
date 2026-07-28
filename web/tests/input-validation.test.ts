import { describe, expect, it } from 'vitest';
import { z } from 'zod';
import {
  contactLeadSchema,
  supportTicketSchema,
  createOrderSchema,
} from '@/lib/portal/validation';

describe('input validation schemas', () => {
  describe('contactLeadSchema', () => {
    it('accepts valid contact data', () => {
      const result = contactLeadSchema.safeParse({
        name: 'Jane Doe',
        email: 'jane@example.com',
        company: 'Acme Corp',
        topic: 'General Query',
        message: 'Hello, I have a question about licensing.',
        consent: true,
      });
      expect(result.success).toBe(true);
    });

    it('rejects missing consent', () => {
      const result = contactLeadSchema.safeParse({
        name: 'Jane Doe',
        email: 'jane@example.com',
        topic: 'General Query',
        message: 'Hello, I have a question.',
        consent: false,
      });
      expect(result.success).toBe(false);
    });

    it('rejects invalid email', () => {
      const result = contactLeadSchema.safeParse({
        name: 'Jane',
        email: 'not-an-email',
        topic: 'Help',
        message: 'Need help with something important.',
        consent: true,
      });
      expect(result.success).toBe(false);
    });

    it('rejects short message', () => {
      const result = contactLeadSchema.safeParse({
        name: 'Jane',
        email: 'jane@example.com',
        topic: 'Help',
        message: 'Hi',
        consent: true,
      });
      expect(result.success).toBe(false);
    });
  });

  describe('supportTicketSchema', () => {
    it('accepts valid ticket', () => {
      const result = supportTicketSchema.safeParse({
        subject: 'Cannot import license',
        topic: 'Technical',
        message: 'I tried to import the license file but it failed.',
      });
      expect(result.success).toBe(true);
    });

    it('rejects short subject', () => {
      const result = supportTicketSchema.safeParse({
        subject: 'Hi',
        topic: 'Technical',
        message: 'Some long enough message here.',
      });
      expect(result.success).toBe(false);
    });
  });

  describe('createOrderSchema', () => {
    it('accepts valid order with default quantity', () => {
      const result = createOrderSchema.safeParse({ editionSlug: 'pro' });
      expect(result.success).toBe(true);
      if (result.success) expect(result.data.quantity).toBe(1);
    });

    it('rejects quantity above 50', () => {
      const result = createOrderSchema.safeParse({ editionSlug: 'pro', quantity: 100 });
      expect(result.success).toBe(false);
    });

    it('rejects empty editionSlug', () => {
      const result = createOrderSchema.safeParse({ editionSlug: '' });
      expect(result.success).toBe(false);
    });
  });
});
