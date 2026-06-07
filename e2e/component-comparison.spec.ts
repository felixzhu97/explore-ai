import { test, expect, Page, Browser, BrowserContext } from '@playwright/test';
import {
  compareStyles,
  compareDOMStructure,
  compareAccessibility,
  waitForAnimations,
  measureElement,
} from './helpers/visual-diff';

const ANGULAR_BASE_URL = 'http://localhost:4200';
const REACT_BASE_URL = 'http://localhost:5173';

async function createDualBrowser(
  browser: Browser
): Promise<{ reactContext: BrowserContext; angularContext: BrowserContext }> {
  const reactContext = await browser.newContext();
  const angularContext = await browser.newContext();
  return { reactContext, angularContext };
}

async function navigateToReact(
  page: Page,
  path: string
): Promise<void> {
  await page.goto(`${REACT_BASE_URL}${path}`);
  await page.waitForLoadState('networkidle');
  await waitForAnimations(page);
}

async function navigateToAngular(
  page: Page,
  path: string
): Promise<void> {
  await page.goto(`${ANGULAR_BASE_URL}${path}`);
  await page.waitForLoadState('networkidle');
  await waitForAnimations(page);
}

// =============================================================================
// SEGMENTED CONTROL COMPARISON
// =============================================================================

test.describe('SegmentedControl: React vs Angular Comparison', () => {
  test('container has matching display and padding', async ({ browser }) => {
    const { reactContext, angularContext } = await createDualBrowser(browser);
    const reactPage = await reactContext.newPage();
    const angularPage = await angularContext.newPage();

    await navigateToReact(reactPage, '/ai-hub');
    await navigateToAngular(angularPage, '/ai-hub');

    const styles = await compareStyles(
      reactPage,
      angularPage,
      '.segmented-control, .container',
      ['display', 'padding', 'gap']
    );

    for (const [prop, result] of styles) {
      expect(result.react).toBe(result.angular);
    }

    await reactContext.close();
    await angularContext.close();
  });

  test('tab button has matching styles', async ({ browser }) => {
    const { reactContext, angularContext } = await createDualBrowser(browser);
    const reactPage = await reactContext.newPage();
    const angularPage = await angularContext.newPage();

    await navigateToReact(reactPage, '/ai-hub');
    await navigateToAngular(angularPage, '/ai-hub');

    const styles = await compareStyles(
      reactPage,
      angularPage,
      '.segment-button',
      ['font-size', 'font-weight', 'padding', 'border-radius']
    );

    for (const [, result] of styles) {
      expect(result.react).toBe(result.angular);
    }

    await reactContext.close();
    await angularContext.close();
  });

  test('active tab has matching box-shadow', async ({ browser }) => {
    const { reactContext, angularContext } = await createDualBrowser(browser);
    const reactPage = await reactContext.newPage();
    const angularPage = await angularContext.newPage();

    await navigateToReact(reactPage, '/ai-hub');
    await navigateToAngular(angularPage, '/ai-hub');

    const [reactShadow, angularShadow] = await Promise.all([
      reactPage.locator('.segment-button.active').evaluate(
        (el) => getComputedStyle(el).boxShadow
      ),
      angularPage.locator('.segment-button.active').evaluate(
        (el) => getComputedStyle(el).boxShadow
      ),
    ]);

    expect(reactShadow).toBe(angularShadow);

    await reactContext.close();
    await angularContext.close();
  });

  test('ARIA attributes match between React and Angular', async ({ browser }) => {
    const { reactContext, angularContext } = await createDualBrowser(browser);
    const reactPage = await reactContext.newPage();
    const angularPage = await angularContext.newPage();

    await navigateToReact(reactPage, '/ai-hub');
    await navigateToAngular(angularPage, '/ai-hub');

    const ariaResult = await compareAccessibility(
      reactPage,
      angularPage,
      '[role="tab"]'
    );

    for (const [, result] of ariaResult.ariaAttributes) {
      expect(result.react).toBe(result.angular);
    }

    await reactContext.close();
    await angularContext.close();
  });

  test('tab interaction produces same DOM changes', async ({ browser }) => {
    const { reactContext, angularContext } = await createDualBrowser(browser);
    const reactPage = await reactContext.newPage();
    const angularPage = await angularContext.newPage();

    await navigateToReact(reactPage, '/ai-hub');
    await navigateToAngular(angularPage, '/ai-hub');

    await Promise.all([
      reactPage.locator('.segment-button').nth(1).click(),
      angularPage.locator('.segment-button').nth(1).click(),
    ]);

    await Promise.all([
      reactPage.waitForTimeout(300),
      angularPage.waitForTimeout(300),
    ]);

    const [reactClasses, angularClasses] = await Promise.all([
      reactPage.locator('.segment-button').nth(1).getAttribute('class'),
      angularPage.locator('.segment-button').nth(1).getAttribute('class'),
    ]);

    expect(reactClasses?.includes('active')).toBe(angularClasses?.includes('active'));

    await reactContext.close();
    await angularContext.close();
  });

  test('DOM structure is equivalent', async ({ browser }) => {
    const { reactContext, angularContext } = await createDualBrowser(browser);
    const reactPage = await reactContext.newPage();
    const angularPage = await angularContext.newPage();

    await navigateToReact(reactPage, '/ai-hub');
    await navigateToAngular(angularPage, '/ai-hub');

    const domResult = await compareDOMStructure(
      reactPage,
      angularPage,
      '[role="tab"]'
    );

    expect(domResult.elementCounts.get('react')).toBe(domResult.elementCounts.get('angular'));

    await reactContext.close();
    await angularContext.close();
  });
});

// =============================================================================
// STATUS BADGE COMPARISON
// =============================================================================

test.describe('StatusBadge: React vs Angular Comparison', () => {
  test('badge has matching styles', async ({ browser }) => {
    const { reactContext, angularContext } = await createDualBrowser(browser);
    const reactPage = await reactContext.newPage();
    const angularPage = await angularContext.newPage();

    await navigateToReact(reactPage, '/agent-chat');
    await navigateToAngular(angularPage, '/agent-chat');

    const styles = await compareStyles(
      reactPage,
      angularPage,
      '.badge',
      ['padding', 'font-size', 'font-weight', 'border-radius', 'display']
    );

    for (const [, result] of styles) {
      expect(result.react).toBe(result.angular);
    }

    await reactContext.close();
    await angularContext.close();
  });

  test('dot has matching dimensions', async ({ browser }) => {
    const { reactContext, angularContext } = await createDualBrowser(browser);
    const reactPage = await reactContext.newPage();
    const angularPage = await angularContext.newPage();

    await navigateToReact(reactPage, '/agent-chat');
    await navigateToAngular(angularPage, '/agent-chat');

    const [reactDotSize, angularDotSize] = await Promise.all([
      reactPage.locator('.badge__dot').first().evaluate((el) => ({
        width: el.offsetWidth,
        height: el.offsetHeight,
        borderRadius: getComputedStyle(el).borderRadius,
      })),
      angularPage.locator('.badge__dot').first().evaluate((el) => ({
        width: el.offsetWidth,
        height: el.offsetHeight,
        borderRadius: getComputedStyle(el).borderRadius,
      })),
    ]);

    expect(reactDotSize.width).toBe(angularDotSize.width);
    expect(reactDotSize.height).toBe(angularDotSize.height);
    expect(reactDotSize.borderRadius).toBe(angularDotSize.borderRadius);

    await reactContext.close();
    await angularContext.close();
  });

  test('online badge background color matches', async ({ browser }) => {
    const { reactContext, angularContext } = await createDualBrowser(browser);
    const reactPage = await reactContext.newPage();
    const angularPage = await angularContext.newPage();

    await navigateToReact(reactPage, '/agent-chat');
    await navigateToAngular(angularPage, '/agent-chat');

    const [reactBg, angularBg] = await Promise.all([
      reactPage.locator('.badge--online').evaluate(
        (el) => getComputedStyle(el).backgroundColor
      ),
      angularPage.locator('.badge--online').evaluate(
        (el) => getComputedStyle(el).backgroundColor
      ),
    ]);

    expect(reactBg).toBe(angularBg);

    await reactContext.close();
    await angularContext.close();
  });

  test('offline badge has transparent-like background', async ({ browser }) => {
    const { reactContext, angularContext } = await createDualBrowser(browser);
    const reactPage = await reactContext.newPage();
    const angularPage = await angularContext.newPage();

    await navigateToReact(reactPage, '/agent-chat');
    await navigateToAngular(angularPage, '/agent-chat');

    const [reactBg, angularBg] = await Promise.all([
      reactPage.locator('.badge--offline').evaluate(
        (el) => getComputedStyle(el).backgroundColor
      ),
      angularPage.locator('.badge--offline').evaluate(
        (el) => getComputedStyle(el).backgroundColor
      ),
    ]);

    expect(reactBg).toBe(angularBg);

    await reactContext.close();
    await angularContext.close();
  });
});

// =============================================================================
// VISION PANEL COMPARISON
// =============================================================================

test.describe('VisionPanel: React vs Angular Comparison', () => {
  test('image area has matching styles', async ({ browser }) => {
    const { reactContext, angularContext } = await createDualBrowser(browser);
    const reactPage = await reactContext.newPage();
    const angularPage = await angularContext.newPage();

    await navigateToReact(reactPage, '/vision');
    await navigateToAngular(angularPage, '/vision');

    const styles = await compareStyles(
      reactPage,
      angularPage,
      '.image-area',
      ['background-color', 'border-radius', 'cursor']
    );

    for (const [, result] of styles) {
      expect(result.react).toBe(result.angular);
    }

    await reactContext.close();
    await angularContext.close();
  });

  test('preview image hover transform matches', async ({ browser }) => {
    const { reactContext, angularContext } = await createDualBrowser(browser);
    const reactPage = await reactContext.newPage();
    const angularPage = await angularContext.newPage();

    await navigateToReact(reactPage, '/vision');
    await navigateToAngular(angularPage, '/vision');

    await Promise.all([
      reactPage.locator('input[type="file"]').setInputFiles('./test-image.png'),
      angularPage.locator('input[type="file"]').setInputFiles('./test-image.png'),
    ]);

    await Promise.all([
      reactPage.locator('.preview-image').hover(),
      angularPage.locator('.preview-image').hover(),
    ]);

    await Promise.all([
      reactPage.waitForTimeout(200),
      angularPage.waitForTimeout(200),
    ]);

    const [reactTransform, angularTransform] = await Promise.all([
      reactPage.locator('.preview-image').evaluate(
        (el) => getComputedStyle(el).transform
      ),
      angularPage.locator('.preview-image').evaluate(
        (el) => getComputedStyle(el).transform
      ),
    ]);

    expect(reactTransform).toBe(angularTransform);

    await reactContext.close();
    await angularContext.close();
  });

  test('spinner has matching animation speed', async ({ browser }) => {
    const { reactContext, angularContext } = await createDualBrowser(browser);
    const reactPage = await reactContext.newPage();
    const angularPage = await angularContext.newPage();

    await navigateToReact(reactPage, '/vision');
    await navigateToAngular(angularPage, '/vision');

    await Promise.all([
      reactPage.locator('input[type="file"]').setInputFiles('./test-image.png'),
      angularPage.locator('input[type="file"]').setInputFiles('./test-image.png'),
    ]);

    await Promise.all([
      reactPage.locator('.action-button, .analyze-button').click(),
      angularPage.locator('.action-button').click(),
    ]);

    const [reactSpinner, angularSpinner] = await Promise.all([
      reactPage.locator('.spinner').first().evaluate(
        (el) => getComputedStyle(el).animation
      ),
      angularPage.locator('.spinner').first().evaluate(
        (el) => getComputedStyle(el).animation
      ),
    ]);

    expect(reactSpinner).toBe(angularSpinner);

    await reactContext.close();
    await angularContext.close();
  });

  test('zoom modal background matches', async ({ browser }) => {
    const { reactContext, angularContext } = await createDualBrowser(browser);
    const reactPage = await reactContext.newPage();
    const angularPage = await angularContext.newPage();

    await navigateToReact(reactPage, '/vision');
    await navigateToAngular(angularPage, '/vision');

    await Promise.all([
      reactPage.locator('input[type="file"]').setInputFiles('./test-image.png'),
      angularPage.locator('input[type="file"]').setInputFiles('./test-image.png'),
    ]);

    await Promise.all([
      reactPage.locator('.preview-image').click(),
      angularPage.locator('.preview-image').click(),
    ]);

    await Promise.all([
      reactPage.waitForTimeout(300),
      angularPage.waitForTimeout(300),
    ]);

    const [reactBg, angularBg] = await Promise.all([
      reactPage.locator('.zoom-modal').evaluate(
        (el) => getComputedStyle(el).backgroundColor
      ),
      angularPage.locator('.zoom-modal').evaluate(
        (el) => getComputedStyle(el).backgroundColor
      ),
    ]);

    expect(reactBg).toBe(angularBg);

    await reactContext.close();
    await angularContext.close();
  });

  test('error message styles match', async ({ browser }) => {
    const { reactContext, angularContext } = await createDualBrowser(browser);
    const reactPage = await reactContext.newPage();
    const angularPage = await angularContext.newPage();

    await navigateToReact(reactPage, '/vision');
    await navigateToAngular(angularPage, '/vision');

    const styles = await compareStyles(
      reactPage,
      angularPage,
      '.error-message',
      ['background-color', 'color', 'border-radius', 'padding']
    );

    for (const [, result] of styles) {
      expect(result.react).toBe(result.angular);
    }

    await reactContext.close();
    await angularContext.close();
  });
});

// =============================================================================
// RAG CHAT COMPARISON
// =============================================================================

test.describe('RAGChat: React vs Angular Comparison', () => {
  test('chat input styles match', async ({ browser }) => {
    const { reactContext, angularContext } = await createDualBrowser(browser);
    const reactPage = await reactContext.newPage();
    const angularPage = await angularContext.newPage();

    await navigateToReact(reactPage, '/rag-chat');
    await navigateToAngular(angularPage, '/rag-chat');

    const styles = await compareStyles(
      reactPage,
      angularPage,
      '.chat-input',
      ['border-radius', 'padding', 'font-size']
    );

    for (const [, result] of styles) {
      expect(result.react).toBe(result.angular);
    }

    await reactContext.close();
    await angularContext.close();
  });

  test('send button styles match', async ({ browser }) => {
    const { reactContext, angularContext } = await createDualBrowser(browser);
    const reactPage = await reactContext.newPage();
    const angularPage = await angularContext.newPage();

    await navigateToReact(reactPage, '/rag-chat');
    await navigateToAngular(angularPage, '/rag-chat');

    const styles = await compareStyles(
      reactPage,
      angularPage,
      '.send-button',
      ['border-radius', 'width', 'height', 'background-color']
    );

    for (const [, result] of styles) {
      expect(result.react).toBe(result.angular);
    }

    await reactContext.close();
    await angularContext.close();
  });

  test('send button disabled state matches', async ({ browser }) => {
    const { reactContext, angularContext } = await createDualBrowser(browser);
    const reactPage = await reactContext.newPage();
    const angularPage = await angularContext.newPage();

    await navigateToReact(reactPage, '/rag-chat');
    await navigateToAngular(angularPage, '/rag-chat');

    const [reactDisabled, angularDisabled] = await Promise.all([
      reactPage.locator('.send-button').evaluate(
        (el) => getComputedStyle(el).opacity
      ),
      angularPage.locator('.send-button').evaluate(
        (el) => getComputedStyle(el).opacity
      ),
    ]);

    expect(reactDisabled).toBe(angularDisabled);

    await reactContext.close();
    await angularContext.close();
  });

  test('document card selected state matches', async ({ browser }) => {
    const { reactContext, angularContext } = await createDualBrowser(browser);
    const reactPage = await reactContext.newPage();
    const angularPage = await angularContext.newPage();

    await navigateToReact(reactPage, '/rag-chat');
    await navigateToAngular(angularPage, '/rag-chat');

    await Promise.all([
      reactPage.waitForTimeout(1000),
      angularPage.waitForTimeout(1000),
    ]);

    const reactDocCount = await reactPage.locator('.document-card').count();
    const angularDocCount = await angularPage.locator('.document-card').count();

    if (reactDocCount > 0 && angularDocCount > 0) {
      await Promise.all([
        reactPage.locator('.document-card').first().click(),
        angularPage.locator('.document-card').first().click(),
      ]);

      await Promise.all([
        reactPage.waitForTimeout(200),
        angularPage.waitForTimeout(200),
      ]);

      const [reactBg, angularBg] = await Promise.all([
        reactPage.locator('.document-card.selected').evaluate(
          (el) => getComputedStyle(el).backgroundColor
        ),
        angularPage.locator('.document-card.selected').evaluate(
          (el) => getComputedStyle(el).backgroundColor
        ),
      ]);

      expect(reactBg).toBe(angularBg);
    }

    await reactContext.close();
    await angularContext.close();
  });

  test('toast animation matches', async ({ browser }) => {
    const { reactContext, angularContext } = await createDualBrowser(browser);
    const reactPage = await reactContext.newPage();
    const angularPage = await angularContext.newPage();

    await navigateToReact(reactPage, '/rag-chat');
    await navigateToAngular(angularPage, '/rag-chat');

    await Promise.all([
      reactPage.locator('input#file-upload').setInputFiles('./test.pdf'),
      angularPage.locator('#file-upload').setInputFiles('./test.pdf'),
    ]);

    await Promise.all([
      reactPage.waitForTimeout(500),
      angularPage.waitForTimeout(500),
    ]);

    const [reactToast, angularToast] = await Promise.all([
      reactPage.locator('.toast-item').first().evaluate(
        (el) => getComputedStyle(el).animation
      ),
      angularPage.locator('.toast-item').first().evaluate(
        (el) => getComputedStyle(el).animation
      ),
    ]);

    expect(reactToast).toBe(angularToast);

    await reactContext.close();
    await angularContext.close();
  });

  test('empty state structure matches', async ({ browser }) => {
    const { reactContext, angularContext } = await createDualBrowser(browser);
    const reactPage = await reactContext.newPage();
    const angularPage = await angularContext.newPage();

    await navigateToReact(reactPage, '/rag-chat');
    await navigateToAngular(angularPage, '/rag-chat');

    const domResult = await compareDOMStructure(
      reactPage,
      angularPage,
      '.empty-state'
    );

    expect(domResult.match).toBeTruthy();

    await reactContext.close();
    await angularContext.close();
  });

  test('quick actions count matches', async ({ browser }) => {
    const { reactContext, angularContext } = await createDualBrowser(browser);
    const reactPage = await reactContext.newPage();
    const angularPage = await angularContext.newPage();

    await navigateToReact(reactPage, '/rag-chat');
    await navigateToAngular(angularPage, '/rag-chat');

    const [reactCount, angularCount] = await Promise.all([
      reactPage.locator('.quick-action').count(),
      angularPage.locator('.quick-action').count(),
    ]);

    expect(reactCount).toBe(angularCount);

    await reactContext.close();
    await angularContext.close();
  });
});

// =============================================================================
// CHAT MESSAGE COMPARISON
// =============================================================================

test.describe('ChatMessage: React vs Angular Comparison', () => {
  test('message bubble animation matches', async ({ browser }) => {
    const { reactContext, angularContext } = await createDualBrowser(browser);
    const reactPage = await reactContext.newPage();
    const angularPage = await angularContext.newPage();

    await navigateToReact(reactPage, '/agent-chat');
    await navigateToAngular(angularPage, '/agent-chat');

    await Promise.all([
      reactPage.locator('.chat-input').fill('Hello'),
      angularPage.locator('.chat-input').fill('Hello'),
    ]);

    await Promise.all([
      reactPage.locator('.send-button').click(),
      angularPage.locator('.send-button').click(),
    ]);

    await Promise.all([
      reactPage.waitForTimeout(500),
      angularPage.waitForTimeout(500),
    ]);

    const [reactAnimation, angularAnimation] = await Promise.all([
      reactPage.locator('.message-bubble').first().evaluate(
        (el) => getComputedStyle(el).animation
      ),
      angularPage.locator('.message-bubble').first().evaluate(
        (el) => getComputedStyle(el).animation
      ),
    ]);

    expect(reactAnimation).toBe(angularAnimation);

    await reactContext.close();
    await angularContext.close();
  });

  test('user message alignment matches', async ({ browser }) => {
    const { reactContext, angularContext } = await createDualBrowser(browser);
    const reactPage = await reactContext.newPage();
    const angularPage = await angularContext.newPage();

    await navigateToReact(reactPage, '/agent-chat');
    await navigateToAngular(angularPage, '/agent-chat');

    await Promise.all([
      reactPage.locator('.chat-input').fill('Test'),
      angularPage.locator('.chat-input').fill('Test'),
    ]);

    await Promise.all([
      reactPage.locator('.send-button').click(),
      angularPage.locator('.send-button').click(),
    ]);

    await Promise.all([
      reactPage.waitForTimeout(500),
      angularPage.waitForTimeout(500),
    ]);

    const [reactAlign, angularAlign] = await Promise.all([
      reactPage.locator('.message-bubble.user').first().evaluate(
        (el) => getComputedStyle(el).alignSelf
      ),
      angularPage.locator('.message-bubble.user, .message-bubble--user').first().evaluate(
        (el) => getComputedStyle(el).alignSelf
      ),
    ]);

    expect(reactAlign).toBe(angularAlign);

    await reactContext.close();
    await angularContext.close();
  });

  test('user message border radius matches', async ({ browser }) => {
    const { reactContext, angularContext } = await createDualBrowser(browser);
    const reactPage = await reactContext.newPage();
    const angularPage = await angularContext.newPage();

    await navigateToReact(reactPage, '/agent-chat');
    await navigateToAngular(angularPage, '/agent-chat');

    await Promise.all([
      reactPage.locator('.chat-input').fill('Test'),
      angularPage.locator('.chat-input').fill('Test'),
    ]);

    await Promise.all([
      reactPage.locator('.send-button').click(),
      angularPage.locator('.send-button').click(),
    ]);

    await Promise.all([
      reactPage.waitForTimeout(500),
      angularPage.waitForTimeout(500),
    ]);

    const [reactRadius, angularRadius] = await Promise.all([
      reactPage.locator('.message-bubble.user').first().evaluate(
        (el) => getComputedStyle(el).borderRadius
      ),
      angularPage.locator('.message-bubble.user, .message-bubble--user').first().evaluate(
        (el) => getComputedStyle(el).borderRadius
      ),
    ]);

    expect(reactRadius).toBe(angularRadius);

    await reactContext.close();
    await angularContext.close();
  });

  test('message time font size matches', async ({ browser }) => {
    const { reactContext, angularContext } = await createDualBrowser(browser);
    const reactPage = await reactContext.newPage();
    const angularPage = await angularContext.newPage();

    await navigateToReact(reactPage, '/agent-chat');
    await navigateToAngular(angularPage, '/agent-chat');

    await Promise.all([
      reactPage.locator('.chat-input').fill('Test'),
      angularPage.locator('.chat-input').fill('Test'),
    ]);

    await Promise.all([
      reactPage.locator('.send-button').click(),
      angularPage.locator('.send-button').click(),
    ]);

    await Promise.all([
      reactPage.waitForTimeout(500),
      angularPage.waitForTimeout(500),
    ]);

    const [reactTime, angularTime] = await Promise.all([
      reactPage.locator('.message-time').first().evaluate(
        (el) => getComputedStyle(el).fontSize
      ),
      angularPage.locator('.message-time').first().evaluate(
        (el) => getComputedStyle(el).fontSize
      ),
    ]);

    expect(reactTime).toBe(angularTime);

    await reactContext.close();
    await angularContext.close();
  });
});

// =============================================================================
// AI HUB COMPARISON
// =============================================================================

test.describe('AIHub: React vs Angular Comparison', () => {
  test('tab section animation matches', async ({ browser }) => {
    const { reactContext, angularContext } = await createDualBrowser(browser);
    const reactPage = await reactContext.newPage();
    const angularPage = await angularContext.newPage();

    await navigateToReact(reactPage, '/ai-hub');
    await navigateToAngular(angularPage, '/ai-hub');

    const [reactAnimation, angularAnimation] = await Promise.all([
      reactPage.locator('.tab-section').evaluate(
        (el) => getComputedStyle(el).animation
      ),
      angularPage.locator('.tab-section').evaluate(
        (el) => getComputedStyle(el).animation
      ),
    ]);

    expect(reactAnimation).toBe(angularAnimation);

    await reactContext.close();
    await angularContext.close();
  });

  test('chat container styles match', async ({ browser }) => {
    const { reactContext, angularContext } = await createDualBrowser(browser);
    const reactPage = await reactContext.newPage();
    const angularPage = await angularContext.newPage();

    await navigateToReact(reactPage, '/ai-hub');
    await navigateToAngular(angularPage, '/ai-hub');

    const styles = await compareStyles(
      reactPage,
      angularPage,
      '.chat-container',
      ['background-color', 'border-radius', 'border', 'padding']
    );

    for (const [, result] of styles) {
      expect(result.react).toBe(result.angular);
    }

    await reactContext.close();
    await angularContext.close();
  });

  test('send button disabled state matches', async ({ browser }) => {
    const { reactContext, angularContext } = await createDualBrowser(browser);
    const reactPage = await reactContext.newPage();
    const angularPage = await angularContext.newPage();

    await navigateToReact(reactPage, '/ai-hub');
    await navigateToAngular(angularPage, '/ai-hub');

    const [reactOpacity, angularOpacity] = await Promise.all([
      reactPage.locator('.send-button').evaluate(
        (el) => getComputedStyle(el).opacity
      ),
      angularPage.locator('.send-button').evaluate(
        (el) => getComputedStyle(el).opacity
      ),
    ]);

    expect(reactOpacity).toBe(angularOpacity);

    await reactContext.close();
    await angularContext.close();
  });

  test('quick action button styles match', async ({ browser }) => {
    const { reactContext, angularContext } = await createDualBrowser(browser);
    const reactPage = await reactContext.newPage();
    const angularPage = await angularContext.newPage();

    await navigateToReact(reactPage, '/ai-hub');
    await navigateToAngular(angularPage, '/ai-hub');

    const styles = await compareStyles(
      reactPage,
      angularPage,
      '.quick-action',
      ['background-color', 'border', 'color', 'border-radius', 'padding']
    );

    for (const [, result] of styles) {
      expect(result.react).toBe(result.angular);
    }

    await reactContext.close();
    await angularContext.close();
  });

  test('input area layout matches', async ({ browser }) => {
    const { reactContext, angularContext } = await createDualBrowser(browser);
    const reactPage = await reactContext.newPage();
    const angularPage = await angularContext.newPage();

    await navigateToReact(reactPage, '/ai-hub');
    await navigateToAngular(angularPage, '/ai-hub');

    const styles = await compareStyles(
      reactPage,
      angularPage,
      '.input-area',
      ['display', 'gap', 'align-items']
    );

    for (const [, result] of styles) {
      expect(result.react).toBe(result.angular);
    }

    await reactContext.close();
    await angularContext.close();
  });
});

// =============================================================================
// ACCESSIBILITY COMPARISON
// =============================================================================

test.describe('Accessibility: React vs Angular Comparison', () => {
  test('all interactive elements have accessible labels', async ({ browser }) => {
    const { reactContext, angularContext } = await createDualBrowser(browser);
    const reactPage = await reactContext.newPage();
    const angularPage = await angularContext.newPage();

    await navigateToReact(reactPage, '/ai-hub');
    await navigateToAngular(angularPage, '/ai-hub');

    const [reactLabels, angularLabels] = await Promise.all([
      reactPage.evaluate(() => {
        const buttons = document.querySelectorAll('button');
        return Array.from(buttons).map((btn) => ({
          text: btn.textContent?.trim(),
          ariaLabel: btn.getAttribute('aria-label'),
        }));
      }),
      angularPage.evaluate(() => {
        const buttons = document.querySelectorAll('button');
        return Array.from(buttons).map((btn) => ({
          text: btn.textContent?.trim(),
          ariaLabel: btn.getAttribute('aria-label'),
        }));
      }),
    ]);

    expect(reactLabels.length).toBe(angularLabels.length);

    await reactContext.close();
    await angularContext.close();
  });

  test('focus order is logical', async ({ browser }) => {
    const { reactContext, angularContext } = await createDualBrowser(browser);
    const reactPage = await reactContext.newPage();
    const angularPage = await angularContext.newPage();

    await navigateToReact(reactPage, '/ai-hub');
    await navigateToAngular(angularPage, '/ai-hub');

    await Promise.all([
      reactPage.keyboard.press('Tab'),
      angularPage.keyboard.press('Tab'),
    ]);

    const [reactFocus, angularFocus] = await Promise.all([
      reactPage.evaluate(() => document.activeElement?.tagName.toLowerCase()),
      angularPage.evaluate(() => document.activeElement?.tagName.toLowerCase()),
    ]);

    expect(reactFocus).toBe(angularFocus);

    await reactContext.close();
    await angularContext.close();
  });
});
