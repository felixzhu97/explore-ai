import 'zone.js';
import 'zone.js/testing';
import { TestBed } from '@angular/core/testing';

// Only configure TestBed for Angular component/directive tests
// Pure unit tests (services, models, theme) don't need TestBed
try {
  TestBed.initTestEnvironment('jsdom' as any, 'jsdom' as any);
} catch {
  // Already initialized or not needed for unit tests
}

beforeEach(() => {
  try {
    TestBed.configureTestingModule({
      teardown: { destroyAfterEach: true }
    });
  } catch {
    // TestBed not available or already configured
  }
});
