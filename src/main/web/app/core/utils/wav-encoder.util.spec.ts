import { describe, it, expect } from 'vitest';
import { encodeWav16kMono, encodeWav16kMonoBase64 } from './wav-encoder.util';

describe('wav-encoder.util', () => {
  it('should_encode_wav_with_16khz_mono_header_when_source_is_16k', () => {
    const samples = new Float32Array([0, 0.5, -0.5, 1, -1]);
    const buffer = encodeWav16kMono(samples, 16_000);
    const view = new DataView(buffer);

    expect(readAscii(view, 0, 4)).toBe('RIFF');
    expect(readAscii(view, 8, 4)).toBe('WAVE');
    expect(readAscii(view, 12, 4)).toBe('fmt ');
    expect(view.getUint16(20, true)).toBe(1);
    expect(view.getUint16(22, true)).toBe(1);
    expect(view.getUint32(24, true)).toBe(16_000);
    expect(view.getUint16(34, true)).toBe(16);
    expect(readAscii(view, 36, 4)).toBe('data');
    expect(view.getUint32(40, true)).toBe(samples.length * 2);
  });

  it('should_resample_to_16khz_when_source_rate_differs', () => {
    const samples = new Float32Array(48_000);
    const buffer = encodeWav16kMono(samples, 48_000);
    const view = new DataView(buffer);
    const dataBytes = view.getUint32(40, true);

    expect(view.getUint32(24, true)).toBe(16_000);
    expect(dataBytes).toBe(16_000 * 2);
  });

  it('should_return_base64_encoded_wav', () => {
    const samples = new Float32Array([0.25, -0.25]);
    const base64 = encodeWav16kMonoBase64(samples, 16_000);

    expect(base64.length).toBeGreaterThan(0);
    expect(() => atob(base64)).not.toThrow();
  });
});

function readAscii(view: DataView, offset: number, length: number): string {
  let text = '';
  for (let i = 0; i < length; i++) {
    text += String.fromCharCode(view.getUint8(offset + i));
  }
  return text;
}
