import { bootstrapApplication } from '@angular/platform-browser';
import { AppComponent } from './app/app.component';
import { appConfig } from './app/app.config';
import { initDatadogRum } from './app/core/config/datadog-rum.config';
import { hasAnalyticsConsent } from './app/privacy/privacy-consent.storage';

if (hasAnalyticsConsent()) {
  initDatadogRum();
}

bootstrapApplication(AppComponent, appConfig).catch(err => console.error(err));
