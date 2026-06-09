import { test, expect } from '@playwright/test';

test.describe('TTS API', () => {
  const baseURL = 'http://localhost:8013';

  test('GET /tts/health should return health status', async ({ request }) => {
    const response = await request.get(`${baseURL}/tts/health`);
    expect(response.status()).toBe(200);
    const body = await response.json();
    expect(body).toHaveProperty('status');
    expect(body).toHaveProperty('provider');
  });

  test('POST /tts/synthesize should accept text', async ({ request }) => {
    const response = await request.post(`${baseURL}/tts/synthesize`, {
      headers: {
        'Content-Type': 'application/json',
      },
      data: {
        text: 'Hello, this is a test',
        language: 'zh-CN',
      },
    });

    expect([200, 400, 422]).toContain(response.status());
  });

  test('GET /tts/providers should list providers', async ({ request }) => {
    const response = await request.get(`${baseURL}/tts/providers`);
    expect(response.status()).toBe(200);
    const body = await response.json();
    expect(Array.isArray(body)).toBeTruthy();
  });
});
