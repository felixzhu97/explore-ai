import { test, expect } from '@playwright/test';

test.describe('Health Check API - Python Services', () => {
  const services = [
    { name: 'AI Agents', base: 'http://localhost:8003', path: '/health', key: 'status' },
    { name: 'Vision Service', base: 'http://localhost:8000', path: '/health', key: 'status' },
    { name: 'Text Service', base: 'http://localhost:8006', path: '/api/text/health', key: 'status' },
    { name: 'TTS Service', base: 'http://localhost:8013', path: '/tts/health', key: 'status' },
    { name: 'RAG Service', base: 'http://localhost:8010', path: '/health', key: 'status' },
    { name: 'Media Gen', base: 'http://localhost:8015', path: '/health', key: 'status' },
    { name: 'Express Server', base: 'http://localhost:3000', path: '/api/health', key: 'status' },
  ];

  for (const svc of services) {
    test(`${svc.name} should respond with 200`, async ({ request }) => {
      const response = await request.get(`${svc.base}${svc.path}`);
      expect(response.status(), `${svc.name} returned ${response.status()}`).toBe(200);
    });

    test(`${svc.name} should return valid JSON with status`, async ({ request }) => {
      const response = await request.get(`${svc.base}${svc.path}`);
      if (response.status() === 200) {
        const body = await response.json();
        expect(body).toHaveProperty(svc.key);
        console.log(`${svc.name}:`, body);
      }
    });
  }
});
