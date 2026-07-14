import { afterEach, describe, expect, it, vi } from 'vitest';

type DatadogEnvironment = {
  production: boolean;
  apiBaseUrl: string;
  wsUrl: string;
  launchDarklyClientSideId: string;
  datadog: {
    applicationId: string;
    clientToken: string;
    site: string;
    service: string;
    env: string;
    version: string;
  };
};

const CONFIGURED_ENVIRONMENT: DatadogEnvironment = {
  production: false,
  apiBaseUrl: 'https://api.example.test/api',
  wsUrl: 'ws://localhost:9000',
  launchDarklyClientSideId: '',
  datadog: {
    applicationId: 'rum-application-id',
    clientToken: 'rum-client-token',
    site: 'us5.datadoghq.com',
    service: 'explore-ai-web',
    env: 'test',
    version: '1.2.3',
  },
};

describe('datadog-rum.config', () => {
  afterEach(() => {
    vi.doUnmock('@datadog/browser-rum');
    vi.doUnmock('@env/environment');
    vi.restoreAllMocks();
    vi.resetModules();
  });

  it('should_skipRumInitialization_when_credentialsMissing', async () => {
    const { initDatadogRum, initSpy } = await loadConfig({
      datadog: {
        applicationId: '',
        clientToken: '',
      },
    });

    initDatadogRum();

    expect(initSpy).not.toHaveBeenCalled();
  });

  it('should_initializeRumWithTracingOptions_when_credentialsConfigured', async () => {
    const { environment, initDatadogRum, initSpy } = await loadConfig();

    initDatadogRum();

    expect(initSpy).toHaveBeenCalledOnce();
    expect(initSpy).toHaveBeenCalledWith({
      applicationId: environment.datadog.applicationId,
      clientToken: environment.datadog.clientToken,
      site: environment.datadog.site,
      service: environment.datadog.service,
      env: environment.datadog.env,
      version: environment.datadog.version,
      sessionSampleRate: 100,
      traceSampleRate: 100,
      trackUserInteractions: true,
      trackResources: true,
      allowedTracingUrls: [environment.apiBaseUrl],
    });
  });

  it('should_initializeRumOnlyOnce_when_calledRepeatedly', async () => {
    const { initDatadogRum, initSpy } = await loadConfig();

    initDatadogRum();
    initDatadogRum();

    expect(initSpy).toHaveBeenCalledOnce();
  });

  it('should_reportHandledErrors_when_rumInitialized', async () => {
    const { DatadogErrorHandler, addErrorSpy, initDatadogRum } = await loadConfig();
    const consoleErrorSpy = vi.spyOn(console, 'error').mockImplementation(() => undefined);
    const error = new Error('user-facing startup error');

    initDatadogRum();
    new DatadogErrorHandler().handleError(error);

    expect(consoleErrorSpy).toHaveBeenCalledWith(error);
    expect(addErrorSpy).toHaveBeenCalledWith(error);
  });

  it('should_notReportHandledErrors_when_rumNotInitialized', async () => {
    const { DatadogErrorHandler, addErrorSpy } = await loadConfig({
      datadog: {
        applicationId: '',
        clientToken: '',
      },
    });
    const consoleErrorSpy = vi.spyOn(console, 'error').mockImplementation(() => undefined);
    const error = new Error('local development error');

    new DatadogErrorHandler().handleError(error);

    expect(consoleErrorSpy).toHaveBeenCalledWith(error);
    expect(addErrorSpy).not.toHaveBeenCalled();
  });
});

async function loadConfig(overrides: PartialEnvironment = {}) {
  vi.resetModules();

  const initSpy = vi.fn();
  const addErrorSpy = vi.fn();
  const environment = mergeEnvironment(overrides);

  vi.doMock('@datadog/browser-rum', () => ({
    datadogRum: {
      init: initSpy,
      addError: addErrorSpy,
    },
  }));
  vi.doMock('@env/environment', () => ({ environment }));

  const config = await import('./datadog-rum.config');

  return {
    ...config,
    addErrorSpy,
    environment,
    initSpy,
  };
}

type PartialEnvironment = Omit<Partial<DatadogEnvironment>, 'datadog'> & {
  datadog?: Partial<DatadogEnvironment['datadog']>;
};

function mergeEnvironment(overrides: PartialEnvironment): DatadogEnvironment {
  return {
    ...CONFIGURED_ENVIRONMENT,
    ...overrides,
    datadog: {
      ...CONFIGURED_ENVIRONMENT.datadog,
      ...overrides.datadog,
    },
  };
}
