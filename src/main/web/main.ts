import { bootstrapApplication } from '@angular/platform-browser';
import { AppComponent } from './app/app.component';
import { appConfig } from './app/app.config';
import { initDatadogRum } from './app/core/config/datadog-rum.config';
import { migrateLegacyStorageKeys } from './app/core/config/storage-keys';
import { hasAnalyticsConsent } from './app/features/privacy/services/privacy-consent.storage';

migrateLegacyStorageKeys();

if (hasAnalyticsConsent()) {
  initDatadogRum();
}

bootstrapApplication(AppComponent, appConfig).catch(error => console.error(error));
