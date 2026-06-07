import { Page, Locator, ConsoleMessage } from '@playwright/test';

export interface DiffOptions {
  threshold?: number;
  ignoreColors?: boolean;
  ignoreAlpha?: boolean;
  ignoreRectangles?: boolean;
}

export interface DiffResult {
  pixelDiffRatio: number;
  passed: boolean;
  message: string;
}

export interface ScreenshotMetadata {
  width: number;
  height: number;
  timestamp: number;
}

/**
 * Compare two screenshots and return the pixel difference ratio.
 * A ratio of 0 means identical, 1 means completely different.
 */
export async function compareScreenshots(
  screenshot1: Buffer,
  screenshot2: Buffer
): Promise<DiffResult> {
  const { diffImages } = await import('playwright-core');

  try {
    const result = await diffImages(screenshot1, screenshot2, {
      threshold: 0.1,
    });

    const diffRatio = result.pixelDiffRatio ?? 0;
    return {
      pixelDiffRatio: diffRatio,
      passed: diffRatio < 0.001,
      message: `Pixel diff ratio: ${(diffRatio * 100).toFixed(3)}%`,
    };
  } catch {
    return {
      pixelDiffRatio: 1,
      passed: false,
      message: 'Failed to compare screenshots',
    };
  }
}

/**
 * Capture a screenshot and return metadata about the element/page.
 */
export async function captureScreenshot(
  page: Page,
  selector?: string,
  options?: { fullPage?: boolean }
): Promise<{ buffer: Buffer; metadata: ScreenshotMetadata }> {
  const buffer = selector
    ? await page.locator(selector).screenshot(options)
    : await page.screenshot(options);

  const metadata: ScreenshotMetadata = {
    width: page.viewportSize()?.width ?? 0,
    height: page.viewportSize()?.height ?? 0,
    timestamp: Date.now(),
  };

  return { buffer, metadata };
}

/**
 * Compare computed styles between React and Angular versions.
 */
export async function compareStyles(
  reactPage: Page,
  angularPage: Page,
  selector: string,
  styleProperties: string[]
): Promise<Map<string, { react: string; angular: string; match: boolean }>> {
  const results = new Map<string, { react: string; angular: string; match: boolean }>();

  for (const prop of styleProperties) {
    const [reactValue, angularValue] = await Promise.all([
      reactPage.locator(selector).evaluate(
        (el, p) => getComputedStyle(el).getPropertyValue(p),
        prop
      ),
      angularPage.locator(selector).evaluate(
        (el, p) => getComputedStyle(el).getPropertyValue(p),
        prop
      ),
    ]);

    results.set(prop, {
      react: reactValue.trim(),
      angular: angularValue.trim(),
      match: reactValue.trim() === angularValue.trim(),
    });
  }

  return results;
}

/**
 * Compare DOM structure between React and Angular versions.
 */
export async function compareDOMStructure(
  reactPage: Page,
  angularPage: Page,
  selector: string
): Promise<{
  elementCounts: Map<string, number>;
  tagCounts: Map<string, number>;
  match: boolean;
}> {
  const queryElements = async (page: Page, sel: string) => {
    return page.evaluate(
      (s) => {
        const elements = document.querySelectorAll(s);
        const tagCounts: Record<string, number> = {};
        elements.forEach((el) => {
          const tag = el.tagName.toLowerCase();
          tagCounts[tag] = (tagCounts[tag] || 0) + 1;
        });
        return {
          total: elements.length,
          tags: tagCounts,
        };
      },
      sel
    );
  };

  const [reactResult, angularResult] = await Promise.all([
    queryElements(reactPage, selector),
    queryElements(angularPage, selector),
  ]);

  const elementCounts = new Map<string, number>();
  elementCounts.set('react', reactResult.total);
  elementCounts.set('angular', angularResult.total);

  const tagCounts = new Map<string, number>();
  for (const [tag, count] of Object.entries(reactResult.tags)) {
    tagCounts.set(`${tag}_react`, count);
  }
  for (const [tag, count] of Object.entries(angularResult.tags)) {
    tagCounts.set(`${tag}_angular`, count);
  }

  return {
    elementCounts,
    tagCounts,
    match: reactResult.total === angularResult.total,
  };
}

/**
 * Compare accessibility attributes between React and Angular versions.
 */
export async function compareAccessibility(
  reactPage: Page,
  angularPage: Page,
  selector: string
): Promise<{
  ariaAttributes: Map<string, { react: string | null; angular: string | null; match: boolean }>;
  roles: Map<string, { react: string | null; angular: string | null; match: boolean }>;
}> {
  const ariaAttributes = new Map<
    string,
    { react: string | null; angular: string | null; match: boolean }
  >();
  const roles = new Map<
    string,
    { react: string | null; angular: string | null; match: boolean }
  >();

  const ariaProps = [
    'aria-selected',
    'aria-checked',
    'aria-disabled',
    'aria-expanded',
    'aria-hidden',
    'aria-label',
    'aria-labelledby',
    'aria-describedby',
    'aria-live',
    'aria-relevant',
  ];

  const getAriaAttributes = async (page: Page, sel: string) => {
    return page.evaluate(
      (props) => {
        const el = document.querySelector(sel);
        if (!el) return {};
        const result: Record<string, string | null> = {};
        for (const prop of props) {
          result[prop] = el.getAttribute(prop);
        }
        return result;
      },
      ariaProps
    );
  };

  const [reactAria, angularAria] = await Promise.all([
    getAriaAttributes(reactPage, selector),
    getAriaAttributes(angularPage, selector),
  ]);

  for (const prop of ariaProps) {
    ariaAttributes.set(prop, {
      react: reactAria[prop],
      angular: angularAria[prop],
      match: reactAria[prop] === angularAria[prop],
    });
  }

  const [reactRole, angularRole] = await Promise.all([
    reactPage.locator(selector).evaluate((el) => el.getAttribute('role')),
    angularPage.locator(selector).evaluate((el) => el.getAttribute('role')),
  ]);

  roles.set('role', {
    react: reactRole,
    angular: angularRole,
    match: reactRole === angularRole,
  });

  return { ariaAttributes, roles };
}

/**
 * Wait for animations to complete before taking screenshots.
 */
export async function waitForAnimations(page: Page, timeout = 1000): Promise<void> {
  await page.evaluate(
    async (ms) => {
      await new Promise((resolve) => setTimeout(resolve, ms));
    },
    timeout
  );

  await page.waitForFunction(
    () =>
      document.querySelectorAll('*').length > 0 &&
      !document.querySelectorAll('*').forEach((el) => {
        const style = getComputedStyle(el);
        if (
          style.animationName !== 'none' ||
          style.transitionProperty !== 'all'
        ) {
          return;
        }
      }),
    { timeout: 5000 }
  ).catch(() => {});
}

/**
 * Extract CSS color values for comparison.
 */
export async function getCSSColor(
  page: Page,
  selector: string,
  property: string
): Promise<{ rgb: string; hex: string } | null> {
  return page.evaluate(
    (s, p) => {
      const el = document.querySelector(s);
      if (!el) return null;
      const style = getComputedStyle(el);
      const color = style.getPropertyValue(p);
      const rgb = style.getPropertyValue(p);

      let hex = rgb;
      if (rgb.startsWith('rgb')) {
        const match = rgb.match(/rgb\((\d+),\s*(\d+),\s*(\d+)\)/);
        if (match) {
          hex = `#${parseInt(match[1])
            .toString(16)
            .padStart(2, '0')}${parseInt(match[2])
            .toString(16)
            .padStart(2, '0')}${parseInt(match[3])
            .toString(16)
            .padStart(2, '0')}`;
        }
      }

      return { rgb: rgb.trim(), hex: hex.trim() };
    },
    selector,
    property
  );
}

/**
 * Measure element dimensions and spacing.
 */
export async function measureElement(
  page: Page,
  selector: string
): Promise<{
  width: number;
  height: number;
  paddingTop: number;
  paddingRight: number;
  paddingBottom: number;
  paddingLeft: number;
  marginTop: number;
  marginRight: number;
  marginBottom: number;
  marginLeft: number;
  borderTopWidth: number;
  borderRightWidth: number;
  borderBottomWidth: number;
  borderLeftWidth: number;
} | null> {
  return page.evaluate(
    (s) => {
      const el = document.querySelector(s);
      if (!el) return null;
      const style = getComputedStyle(el);
      const rect = el.getBoundingClientRect();
      return {
        width: rect.width,
        height: rect.height,
        paddingTop: parseFloat(style.paddingTop),
        paddingRight: parseFloat(style.paddingRight),
        paddingBottom: parseFloat(style.paddingBottom),
        paddingLeft: parseFloat(style.paddingLeft),
        marginTop: parseFloat(style.marginTop),
        marginRight: parseFloat(style.marginRight),
        marginBottom: parseFloat(style.marginBottom),
        marginLeft: parseFloat(style.marginLeft),
        borderTopWidth: parseFloat(style.borderTopWidth),
        borderRightWidth: parseFloat(style.borderRightWidth),
        borderBottomWidth: parseFloat(style.borderBottomWidth),
        borderLeftWidth: parseFloat(style.borderLeftWidth),
      };
    },
    selector
  );
}

/**
 * Monitor console for errors and warnings.
 */
export async function captureConsoleMessages(
  page: Page
): Promise<{ errors: string[]; warnings: string[] }> {
  const errors: string[] = [];
  const warnings: string[] = [];

  page.on('console', (msg: ConsoleMessage) => {
    if (msg.type() === 'error') {
      errors.push(msg.text());
    } else if (msg.type() === 'warning') {
      warnings.push(msg.text());
    }
  });

  return { errors, warnings };
}

/**
 * Generate a unique test ID for screenshots.
 */
export function generateTestId(prefix: string): string {
  return `${prefix}_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`;
}
