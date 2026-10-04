import 'zone.js';
import 'zone.js/testing';
import '@angular/compiler';
import { BrowserTestingModule, platformBrowserTesting } from '@angular/platform-browser/testing';
import { getTestBed } from '@angular/core/testing';

// Initialize test environment
try {
  getTestBed()
    .initTestEnvironment(BrowserTestingModule, platformBrowserTesting(), {
      teardown: { destroyAfterEach: true },
    });
} catch {
  // Already initialized
}

// Mock URL global for JSDOM environment (not available in Node.js)
if (typeof globalThis.URL.createObjectURL === 'undefined') {
  Object.defineProperty(globalThis.URL, 'createObjectURL', {
    value: () => 'blob:mock-url',
    writable: true,
    configurable: true,
  });
}
if (typeof globalThis.URL.revokeObjectURL === 'undefined') {
  Object.defineProperty(globalThis.URL, 'revokeObjectURL', {
    value: () => { /* empty */ },
    writable: true,
    configurable: true,
  });
}

// Suppress JSDOM CSS parsing errors for SCSS nesting
const originalError = console.error;
console.error = (...args: unknown[]) => {
  if (
    args[0]
    && typeof args[0] === 'string'
    && args[0].includes('Could not parse CSS stylesheet')
  ) {
    return;
  }
  originalError.apply(console, args);
};
