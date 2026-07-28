import { describe, expect, it } from 'vitest';
import { capabilitySchema } from '@/lib/license/contract';

// These capability names are rejected because they are runtime names, not in the allowed enum
const FORBIDDEN = ['LITERT_LM_PRO', 'LLAMA_CPP_PRO', 'GGUF_ACCESS'];
// These are the actual allowed product capability names from the enum
const VALID = ['OFFLINE_TRANSCRIPTION', 'SMART_INSIGHTS', 'AUDIO_EDITOR', 'CORE_RECORDING'];

describe('capability validation', () => {
  it('rejects forbidden runtime capability names', () => {
    for (const name of FORBIDDEN) {
      const result = capabilitySchema.safeParse(name);
      expect(result.success).toBe(false);
    }
  });

  it('accepts valid product capability names', () => {
    for (const name of VALID) {
      const result = capabilitySchema.safeParse(name);
      expect(result.success).toBe(true);
    }
  });

  it('rejects unknown capability names not in the enum', () => {
    const result = capabilitySchema.safeParse('ADVANCED_EXPORT');
    expect(result.success).toBe(false);
  });

  it('rejects empty capability names', () => {
    const result = capabilitySchema.safeParse('');
    expect(result.success).toBe(false);
  });

  it('rejects capability names with only whitespace', () => {
    const result = capabilitySchema.safeParse('   ');
    expect(result.success).toBe(false);
  });
});
