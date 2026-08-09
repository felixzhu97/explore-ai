import { bootstrapApplication } from '@angular/platform-browser';
import { AppComponent } from './app/app.component';
import { appConfig } from './app/app.config';
import { initDatadogRum } from './app/core/config/datadog-rum.config';
import { loadRuntimeTranslations } from './app/core/i18n/load-runtime-translations';
import { hasAnalyticsConsent } from './app/privacy/privacy-consent.storage';

async function main(): Promise<void> {
  await loadRuntimeTranslations();

  if (hasAnalyticsConsent()) {
    initDatadogRum();
  }

  await bootstrapApplication(AppComponent, appConfig);
}

main().catch(err => console.error(err));
