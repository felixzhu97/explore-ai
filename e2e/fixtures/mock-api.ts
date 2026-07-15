import type { Page, Route } from '@playwright/test';

const TEST_SESSION_ID = 'e2e-session-001';

const PROVIDERS = [
  {
    name: 'openai',
    displayName: 'DeepSeek',
    models: ['deepseek-v4-flash', 'deepseek-v4-pro'],
    status: 'available',
  },
];

const MODELS = {
  provider: 'openai',
  models: [
    { name: 'deepseek-v4-flash', provider: 'openai' },
    { name: 'deepseek-v4-pro', provider: 'openai' },
  ],
  count: 2,
};

const RAG_DOCUMENTS = {
  documents: [
    { id: 'doc-e2e-1', title: 'Product Guide.pdf' },
    { id: 'doc-e2e-2', title: 'API Reference.md' },
  ],
};

const RAG_SOURCES = [
  {
    text: 'The ng-zorro-x bubble component supports custom message and footer rendering templates.',
    score: 0.94,
    metadata: { source: 'Product Guide.pdf' },
  },
  {
    text: 'Welcome and Prompts components can be composed for empty chat states.',
    score: 0.87,
    metadata: { source: 'API Reference.md' },
  },
];

interface SessionMessage {
  id: string;
  role: 'user' | 'assistant';
  content: string;
  timestamp: string;
}

let sessionMessages: SessionMessage[] = [];

function fulfillJson(route: Route, body: unknown, status = 200): Promise<void> {
  return route.fulfill({
    status,
    contentType: 'application/json',
    body: JSON.stringify(body),
  });
}

function fulfillSse(route: Route, events: string[]): Promise<void> {
  const body = events.map(line => (line === '' ? '\n' : `${line}\n`)).join('');
  return route.fulfill({
    status: 200,
    contentType: 'text/event-stream',
    headers: { 'Cache-Control': 'no-cache' },
    body,
  });
}

/** Encode multi-line assistant text as one SSE event (spec: join data lines with \\n). */
export function encodeSseTextEvent(text: string): string[] {
  const dataLines = text.split('\n').map(line => `data: ${line}`);
  return [...dataLines, '', 'data: [DONE]', ''];
}

export async function setupCommonMocks(page: Page): Promise<void> {
  sessionMessages = [];

  await page.addInitScript(() => {
    localStorage.setItem('language', 'en');
  });

  await page.route('**/api/text/providers', route => fulfillJson(route, PROVIDERS));
  await page.route('**/api/text/models**', route => fulfillJson(route, MODELS));
  await page.route('**/api/sessions', async (route) => {
    if (route.request().method() === 'GET') {
      await fulfillJson(route, [
        {
          sessionId: TEST_SESSION_ID,
          title: 'E2E Chat',
          lastActivityAt: new Date('2026-01-01T00:00:00.000Z').toISOString(),
        },
      ]);
      return;
    }
    if (route.request().method() === 'POST') {
      await fulfillJson(route, {
        sessionId: TEST_SESSION_ID,
        title: 'New Chat',
        lastActivityAt: new Date('2026-01-01T00:00:00.000Z').toISOString(),
      });
    }
  });
  await page.route(`**/api/sessions/${TEST_SESSION_ID}/messages`, route => fulfillJson(route, sessionMessages),
  );
}

export async function setupChatStreamMock(
  page: Page,
  response = 'Here is a **markdown** answer with a list:\n\n- Item one\n- Item two',
): Promise<void> {
  await page.route('**/api/text/chat/stream', async (route) => {
    const request = route.request().postDataJSON() as {
      messages?: { role: string; content: string }[];
    };
    const userContent = request.messages?.[0]?.content ?? 'Hello';
    const timestamp = new Date('2026-01-01T00:00:00.000Z').toISOString();

    sessionMessages = [
      {
        id: 'user-e2e-1',
        role: 'user',
        content: userContent,
        timestamp,
      },
      {
        id: 'assistant-e2e-1',
        role: 'assistant',
        content: response,
        timestamp,
      },
    ];

    await fulfillSse(route, encodeSseTextEvent(response));
  });
}

export async function setupRagMocks(page: Page): Promise<void> {
  await page.route('**/api/rag/documents', route => fulfillJson(route, RAG_DOCUMENTS));
}

export async function setupRagStreamMock(
  page: Page,
  answer = 'Based on your documents, ng-zorro-x provides Bubble, Welcome, and Prompts for chat UIs.',
): Promise<void> {
  await page.route('**/api/rag/chat/stream', route => fulfillSse(route, [
    'event: sources',
    `data: ${JSON.stringify(RAG_SOURCES)}`,
    '',
    `data: ${answer}`,
    'data: [DONE]',
    '',
  ]),
  );
}

export async function prepareVisualPage(page: Page): Promise<void> {
  await page.addStyleTag({
    content: `
      *, *::before, *::after {
        animation-duration: 0s !important;
        animation-delay: 0s !important;
        transition-duration: 0s !important;
        transition-delay: 0s !important;
      }
    `,
  });
}

export async function gotoAppPage(page: Page, path: '/chat' | '/rag'): Promise<void> {
  await Promise.all([
    page.waitForResponse(
      response => response.url().includes('/api/sessions') && response.request().method() === 'GET',
    ),
    page.goto(path),
  ]);
  await page.locator('nx-sender textarea').waitFor({ state: 'visible' });
}

export async function waitForChatReady(page: Page): Promise<void> {
  await page.locator('nx-sender textarea').waitFor({ state: 'visible' });
  await page.waitForFunction(() => {
    const textarea = document.querySelector('nx-sender textarea') as HTMLTextAreaElement | null;
    return textarea !== null && !textarea.disabled;
  });
}

export async function sendChatMessage(page: Page, text: string): Promise<void> {
  const input = page.locator('nx-sender textarea');
  await input.click();
  await input.fill(text);
  await input.press('Enter');
}

export async function sendRagMessage(page: Page, text: string): Promise<void> {
  await sendChatMessage(page, text);
}

export async function waitForBubbleConversation(
  page: Page,
  userText: string,
): Promise<void> {
  await page.locator('nx-bubble-list').waitFor({ state: 'visible' });
  await page.locator('main').getByText(userText).waitFor({ state: 'visible' });
  await page.locator('app-markdown-content').first().waitFor({ state: 'visible' });
}
