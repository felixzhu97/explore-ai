import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { I18nService } from '../core/i18n';
import { PrivacyConsentService } from './privacy-consent.service';

@Component({
  selector: 'app-consent-banner',
  imports: [RouterLink],
  template: `
    @if (consent.needsDecision()) {
      <div
        class="fixed inset-x-0 bottom-0 z-100 border-t border-black/10 bg-background/95 p-4 backdrop-blur-sm"
        role="dialog"
        aria-labelledby="consent-title"
      >
        <div class="mx-auto flex max-w-3xl flex-col gap-3 md:flex-row md:items-end md:justify-between">
          <div class="min-w-0 flex-1">
            <h2 id="consent-title" class="text-sm font-semibold">{{ t().privacy.consentTitle }}</h2>
            <p class="mt-1 text-sm text-muted-foreground">
              {{ t().privacy.consentBody }}
              <a routerLink="/privacy" class="underline underline-offset-2">
                {{ t().privacy.consentLearnMore }}
              </a>
            </p>
          </div>
          <div class="flex shrink-0 gap-2">
            <button
              type="button"
              class="rounded-md border border-black/10 px-3 py-2 text-sm hover:bg-black/4"
              (click)="consent.rejectAnalytics()"
            >
              {{ t().privacy.consentReject }}
            </button>
            <button
              type="button"
              class="rounded-md bg-foreground px-3 py-2 text-sm font-medium text-background"
              (click)="consent.acceptAnalytics()"
            >
              {{ t().privacy.consentAccept }}
            </button>
          </div>
        </div>
      </div>
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ConsentBannerComponent {
  private readonly i18n = inject(I18nService);
  protected readonly consent = inject(PrivacyConsentService);
  readonly t = this.i18n.t;
}
