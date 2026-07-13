import { Injectable } from '@angular/core';
import { datadogRum } from '@datadog/browser-rum';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class DatadogRumService {
  private enabled = false;

  initialize(): void {
    const config = environment.datadog;
    if (!config.applicationId || !config.clientToken) {
      return;
    }

    datadogRum.init({
      applicationId: config.applicationId,
      clientToken: config.clientToken,
      site: config.site,
      service: config.service,
      env: config.env,
      version: config.version,
      sessionSampleRate: 100,
      sessionReplaySampleRate: 20,
      trackUserInteractions: true,
      trackResources: true,
      trackLongTasks: true,
      defaultPrivacyLevel: 'mask-user-input',
      allowedTracingUrls: [environment.apiBaseUrl],
    });

    datadogRum.startSessionReplayRecording();
    this.enabled = true;
  }

  addError(error: unknown, context?: Record<string, unknown>): void {
    if (!this.enabled) {
      return;
    }

    datadogRum.addError(error, context);
  }
}
