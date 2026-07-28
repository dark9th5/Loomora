import { z } from 'zod';
import { capabilitySchema, licensePayloadSchema } from '@/lib/license/contract';

export const contactLeadSchema = z.object({
  name: z.string().min(2).max(120),
  email: z.string().email(),
  company: z.string().max(120).optional().or(z.literal('')),
  topic: z.string().min(2).max(80),
  message: z.string().min(10).max(4000),
  consent: z.literal(true),
});

export const supportTicketSchema = z.object({
  subject: z.string().min(4).max(180),
  topic: z.string().min(2).max(80),
  message: z.string().min(10).max(4000),
});

export const createOrderSchema = z.object({
  editionSlug: z.string().min(1).max(80),
  quantity: z.number().int().positive().max(50).default(1),
});

export const licenseDraftSchema = licensePayloadSchema.extend({
  capabilities: z.array(capabilitySchema).min(1),
});
