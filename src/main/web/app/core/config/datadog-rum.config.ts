import { ErrorHandler } from '@angular/core';
import { datadogRum } from '@datadog/browser-rum';
import { environment } from '@env/environment';

let rumInitialized = false;

export function initDatadogRum(): void {
  const { applicationId, clientToken, site, service, env, version } = environment.datadog;
  if (!applicationId || !clientToken) {
    return;
  }

  datadogRum.init({
    applicationId,
    clientToken,
    site,
    service,
    env,
    version,
    sessionSampleRate: 100,
    traceSampleRate: 100,
    trackUserInteractions: true,
    trackResources: true,
    allowedTracingUrls: [environment.apiBaseUrl],
  });
  rumInitialized = true;
}

export class DatadogErrorHandler implements ErrorHandler {
  handleError(error: unknown): void {
    console.error(error);
    if (rumInitialized) {
      datadogRum.addError(error);
    }
  }
}
