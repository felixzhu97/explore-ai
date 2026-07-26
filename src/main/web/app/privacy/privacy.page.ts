import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { ZardSwitchComponent } from '../shared/components/switch/switch.component';
import { NotificationService } from '../core/notification.service';
import { ChatService } from '../chat/chat.service';
import { PrivacyApiService } from './privacy-api.service';
import { PrivacyConsentService } from './privacy-consent.service';
import { I18nService } from '../core/i18n';

@Component({
  selector: 'app-privacy-page',
  imports: [FormsModule, RouterLink, ZardSwitchComponent],
  template: `
    <div class="mx-auto flex w-full max-w-3xl flex-col gap-8">
      <header>
        <p class="text-xs tracking-[0.16em] text-muted-foreground uppercase">ExploreAI</p>
        <h1 class="mt-1 text-2xl font-semibold tracking-tight">{{ t().privacy.title }}</h1>
        <p class="mt-1 text-sm text-muted-foreground">{{ t().privacy.subtitle }}</p>
      </header>

      <section class="rounded-xl border border-black/8 bg-background p-5">
        <h2 class="text-base font-semibold">{{ t().privacy.noticeHeading }}</h2>
        <div class="mt-3 space-y-3 text-sm leading-relaxed text-muted-foreground">
          <p>{{ t().privacy.noticeIdentity }}</p>
          <p>{{ t().privacy.noticeChat }}</p>
          <p>{{ t().privacy.noticeRetention }}</p>
        </div>
      </section>

      <section class="rounded-xl border border-black/8 bg-background p-5">
        <h2 class="text-base font-semibold">{{ t().privacy.processorsHeading }}</h2>
        <ul class="mt-3 space-y-2 text-sm text-muted-foreground">
          @for (item of processors; track item.name) {
            <li>
              <span class="font-medium text-foreground">{{ item.name }}</span>
              — {{ item.purpose }}
            </li>
          }
        </ul>
      </section>

      <section class="rounded-xl border border-black/8 bg-background p-5">
        <h2 class="text-base font-semibold">{{ t().privacy.analyticsHeading }}</h2>
        <p class="mt-2 text-sm text-muted-foreground">{{ t().privacy.analyticsHelp }}</p>
        <div class="mt-4 flex items-center justify-between gap-4">
          <span class="text-sm">{{ t().privacy.analyticsLabel }}</span>
          <z-switch
            [ngModel]="consent.consent().analytics"
            (ngModelChange)="onAnalyticsToggle($event)"
          />
        </div>
      </section>

      <section class="rounded-xl border border-black/8 bg-background p-5">
        <h2 class="text-base font-semibold">{{ t().privacy.controlsHeading }}</h2>
        <p class="mt-2 text-sm text-muted-foreground">{{ t().privacy.controlsHelp }}</p>
        <div class="mt-4 flex flex-wrap gap-3">
          <button
            type="button"
            class="rounded-md border border-black/10 px-4 py-2 text-sm font-medium hover:bg-black/4 disabled:opacity-50"
            [disabled]="busy()"
            (click)="eraseSessions()"
          >
            {{ t().privacy.eraseButton }}
          </button>
          <button
            type="button"
            class="rounded-md border border-black/10 px-4 py-2 text-sm font-medium hover:bg-black/4 disabled:opacity-50"
            [disabled]="busy()"
            (click)="resetIdentity()"
          >
            {{ t().privacy.resetButton }}
          </button>
        </div>
      </section>

      <p class="text-xs text-muted-foreground">
        <a routerLink="/chat" class="underline underline-offset-2">{{ t().privacy.backToChat }}</a>
      </p>
    </div>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
  // Own vertical scroll: main layout uses overflow-hidden (same as metrics).
  host: {
    class: 'flex flex-1 min-h-0 w-full flex-col overflow-y-auto bg-surface px-4 py-6',
  },
})
export class PrivacyPage {
  private readonly api = inject(PrivacyApiService);
  private readonly chat = inject(ChatService);
  private readonly notify = inject(NotificationService);
  private readonly i18n = inject(I18nService);
  protected readonly consent = inject(PrivacyConsentService);

  readonly t = this.i18n.t;
  readonly busy = signal(false);

  readonly processors = [
    { name: 'DeepSeek', purpose: 'Large language model inference for chat' },
    { name: 'OpenAI', purpose: 'Optional image generation and text-to-speech' },
    { name: 'Serper', purpose: 'Web search tool results' },
    { name: 'LaunchDarkly', purpose: 'Feature flags (only with analytics consent)' },
    { name: 'Datadog', purpose: 'Optional RUM / APM (only with analytics consent)' },
  ];

  onAnalyticsToggle(enabled: boolean): void {
    if (enabled) {
      this.consent.acceptAnalytics();
    } else {
      this.consent.rejectAnalytics();
    }
  }

  eraseSessions(): void {
    if (!confirm(this.t().privacy.eraseConfirm)) {
      return;
    }
    this.busy.set(true);
    this.api.eraseAllSessions().subscribe({
      next: () => {
        this.chat.sessions.set([]);
        this.chat.activeSessionId.set(null);
        this.chat.messages.set([]);
        this.notify.showSuccess(this.t().privacy.eraseSuccess);
        this.busy.set(false);
      },
      error: () => {
        this.notify.showError(this.t().privacy.eraseFailed);
        this.busy.set(false);
      },
    });
  }

  resetIdentity(): void {
    if (!confirm(this.t().privacy.resetConfirm)) {
      return;
    }
    this.busy.set(true);
    this.api.resetIdentity().subscribe({
      next: () => {
        this.chat.sessions.set([]);
        this.chat.activeSessionId.set(null);
        this.chat.messages.set([]);
        this.chat.loadSessions();
        this.notify.showSuccess(this.t().privacy.resetSuccess);
        this.busy.set(false);
      },
      error: () => {
        this.notify.showError(this.t().privacy.resetFailed);
        this.busy.set(false);
      },
    });
  }
}
