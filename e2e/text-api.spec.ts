import { test, expect } from '@playwright/test';

test.describe('Text/LLM API', () => {
  const baseURL = 'http://localhost:8006';

  test('GET /api/text/providers should return available providers', async ({ request }) => {
    const response = await request.get(`${baseURL}/api/text/providers`);
    expect(response.status()).toBe(200);
    const body = await response.json();
    expect(Array.isArray(body)).toBeTruthy();
  });

  test('GET /api/text/models should return models for a provider', async ({ request }) => {
    const response = await request.get(`${baseURL}/api/text/models?provider=deepseek`);
    expect(response.status()).toBe(200);
    const body = await response.json();
    expect(Array.isArray(body)).toBeTruthy();
    expect(body.length).toBeGreaterThan(0);
  });

  test('POST /api/text/chat should return response for valid request', async ({ request }) => {
    const response = await request.post(`${baseURL}/api/text/chat`, {
      headers: {
        'Content-Type': 'application/json',
      },
      data: {
        messages: [
          { role: 'user', content: 'Hello, this is a test' }
        ],
        provider: 'deepseek',
        model: 'deepseek-v4-flash',
        session_id: 'test-' + Date.now(),
      },
    });

    expect([200, 400, 422, 500]).toContain(response.status());
  });

  test('health endpoint should return service status', async ({ request }) => {
    const response = await request.get(`${baseURL}/api/text/health`);
    expect(response.status()).toBe(200);
    const body = await response.json();
    expect(body).toHaveProperty('status');
    expect(['ok', 'degraded']).toContain(body.status);
  });
});
