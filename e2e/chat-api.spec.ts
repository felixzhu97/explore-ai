import { test, expect } from '@playwright/test';

test.describe('Chat API (AI Agents)', () => {
  const baseURL = 'http://localhost:8003';

  test('POST /api/agents/supervisor/invoke should return streaming response', async ({ request }) => {
    const response = await request.post(`${baseURL}/api/agents/supervisor/invoke`, {
      headers: {
        'Content-Type': 'application/json',
      },
      data: {
        messages: [
          { role: 'user', content: 'Hello, this is a test message' }
        ],
      },
    });

    expect(response.status()).toBe(200);
    const contentType = response.headers()['content-type'];
    expect(contentType).toContain('text/event-stream');
  });

  test('GET /health should return service status', async ({ request }) => {
    const response = await request.get(`${baseURL}/health`);
    expect(response.status()).toBe(200);
    const body = await response.json();
    expect(body).toHaveProperty('status', 'ok');
    expect(body).toHaveProperty('service', 'ai_agents');
  });

  test('GET /agents should list available agents', async ({ request }) => {
    const response = await request.get(`${baseURL}/agents`);
    expect([200, 503]).toContain(response.status());
    if (response.status() === 200) {
      const body = await response.json();
      expect(body).toHaveProperty('agents');
      expect(Array.isArray(body.agents)).toBeTruthy();
    }
  });
});
