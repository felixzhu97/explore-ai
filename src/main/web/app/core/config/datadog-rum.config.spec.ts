import { describe, expect, it, vi, beforeEach, afterEach } from 'vitest';
import { datadogRum } from '@datadog/browser-rum';

vi.mock('@datadog/browser-rum', () => ({
  datadogRum: {
    init: vi.fn(),
    addError: vi.fn(),
  },
}));

const envState = vi.hoisted(() => ({
  datadog: {
    applicationId: '',
    clientToken: '',
    site: 'us5.datadoghq.com',
    service: 'explore-ai-web',
    env: 'development',
    version: '0.0.1',
  },
  apiBaseUrl: '/api',
}));

vi.mock('../../../environments/environment', () => ({
  environment: envState,
}));

describe('datadog-rum.config', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.resetModules();
    envState.datadog.applicationId = '';
    envState.datadog.clientToken = '';
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('should skip init when credentials missing', async () => {
    const { initDatadogRum } = await import('./datadog-rum.config');
    initDatadogRum();
    expect(datadogRum.init).not.toHaveBeenCalled();
  });

  it('should init rum when credentials present', async () => {
    envState.datadog.applicationId = 'app-id';
    envState.datadog.clientToken = 'client-token';

    const { initDatadogRum } = await import('./datadog-rum.config');
    initDatadogRum();

    expect(datadogRum.init).toHaveBeenCalledWith(expect.objectContaining({
      applicationId: 'app-id',
      clientToken: 'client-token',
    }));
  });

  it('should not init twice', async () => {
    envState.datadog.applicationId = 'app-id';
    envState.datadog.clientToken = 'client-token';

    const { initDatadogRum } = await import('./datadog-rum.config');
    initDatadogRum();
    initDatadogRum();

    expect(datadogRum.init).toHaveBeenCalledTimes(1);
  });

  it('should log error without rum when not initialized', async () => {
    const consoleSpy = vi.spyOn(console, 'error').mockImplementation(() => undefined);
    const { DatadogErrorHandler } = await import('./datadog-rum.config');
    const handler = new DatadogErrorHandler();

    handler.handleError(new Error('boom'));

    expect(consoleSpy).toHaveBeenCalled();
    expect(datadogRum.addError).not.toHaveBeenCalled();
  });

  it('should forward error to rum when initialized', async () => {
    envState.datadog.applicationId = 'app-id';
    envState.datadog.clientToken = 'client-token';
    vi.spyOn(console, 'error').mockImplementation(() => undefined);

    const { initDatadogRum, DatadogErrorHandler } = await import('./datadog-rum.config');
    initDatadogRum();
    new DatadogErrorHandler().handleError(new Error('tracked'));

    expect(datadogRum.addError).toHaveBeenCalledWith(expect.any(Error));
  });
});
