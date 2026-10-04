import { bootstrapApplication } from '@angular/platform-browser';
import { AppComponent } from './app/app.component';
import { appConfig } from './app/app.config';
import { initDatadogRum } from './app/privacy/datadog-rum.config';
import { migrateLegacyStorageKeys } from './app/storage-keys';
import { hasAnalyticsConsent } from './app/privacy/privacy-consent.storage';

migrateLegacyStorageKeys();

if (hasAnalyticsConsent()) {
  initDatadogRum();
}

bootstrapApplication(AppComponent, appConfig).catch(error => console.error(error));
