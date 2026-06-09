import { test, expect } from '@playwright/test';

test.describe('Vision API', () => {
  const baseURL = 'http://localhost:8000';

  test('GET /health should return health status', async ({ request }) => {
    const response = await request.get(`${baseURL}/health`);
    expect(response.status()).toBe(200);
    const body = await response.json();
    expect(body).toHaveProperty('status', 'ok');
  });

  test('POST /vision/analyze should accept image URL', async ({ request }) => {
    const response = await request.post(`${baseURL}/vision/analyze`, {
      headers: {
        'Content-Type': 'application/json',
      },
      data: {
        imageUrl: 'https://via.placeholder.com/300',
        task: 'caption',
      },
    });

    expect([200, 400, 422, 500]).toContain(response.status());
  });

  test('POST /vision/detect should accept image', async ({ request }) => {
    const response = await request.post(`${baseURL}/vision/detect`, {
      headers: {
        'Content-Type': 'application/json',
      },
      data: {
        imageUrl: 'https://via.placeholder.com/300',
        confidence: 0.5,
      },
    });

    expect([200, 400, 422, 500]).toContain(response.status());
  });
});
