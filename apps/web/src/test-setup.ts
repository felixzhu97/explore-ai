import 'zone.js';
import 'zone.js/testing';
import {
  BrowserDynamicTestingModule,
  platformBrowserDynamicTesting,
} from '@angular/platform-browser-dynamic/testing';
import { getTestBed } from '@angular/core/testing';

// Initialize test environment
try {
  getTestBed().initTestEnvironment(BrowserDynamicTestingModule, platformBrowserDynamicTesting(), {
    teardown: { destroyAfterEach: true },
  });
} catch {
  // Already initialized
}

// Suppress JSDOM CSS parsing errors for SCSS nesting
const originalError = console.error;
console.error = (...args: unknown[]) => {
  if (
    args[0] &&
    typeof args[0] === 'string' &&
    args[0].includes('Could not parse CSS stylesheet')
  ) {
    return;
  }
  originalError.apply(console, args);
};
