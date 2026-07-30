import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { NotificationService } from '../core/notification.service';
import { ChatService } from '../chat/chat.service';
import { PrivacyApiService } from './privacy-api.service';
import { I18nService } from '../core/i18n';
import { PRIVACY_PAGE_COPY } from './privacy.page.copy';
import { PrivacyPreferencesFormComponent } from './privacy-preferences-form.component';

@Component({
  selector: 'app-privacy-page',
  imports: [RouterLink, PrivacyPreferencesFormComponent],
  template: `
    <div class="mx-auto flex w-full max-w-3xl flex-col gap-8">
      <header>
        <p class="text-xs tracking-[0.16em] text-muted-foreground uppercase">ExploreAI</p>
        <h1 class="mt-1 text-2xl font-semibold tracking-tight">{{ copy().title }}</h1>
        <p class="mt-1 text-sm text-muted-foreground">{{ copy().subtitle }}</p>
      </header>

      <section class="rounded-xl border border-black/8 bg-background p-5">
        <h2 class="text-base font-semibold">{{ copy().noticeHeading }}</h2>
        <div class="mt-3 space-y-3 text-sm leading-relaxed text-muted-foreground">
          <p>{{ copy().noticeIdentity }}</p>
          <p>{{ copy().noticeChat }}</p>
          <p>{{ copy().noticeRetention }}</p>
        </div>
      </section>

      <section class="rounded-xl border border-black/8 bg-background p-5">
        <h2 class="text-base font-semibold">{{ copy().processorsHeading }}</h2>
        <ul class="mt-3 space-y-2 text-sm text-muted-foreground">
          @for (item of processors; track item.name) {
            <li>
              <span class="font-medium text-foreground">{{ item.name }}</span>
              — {{ item.purpose }}
            </li>
          }
        </ul>
      </section>

      <app-privacy-preferences-form [copy]="copy()" />

      <section class="rounded-xl border border-black/8 bg-background p-5">
        <h2 class="text-base font-semibold">{{ copy().controlsHeading }}</h2>
        <p class="mt-2 text-sm text-muted-foreground">{{ copy().controlsHelp }}</p>
        <div class="mt-4 flex flex-wrap gap-3">
          <button
            type="button"
            class="rounded-md border border-black/10 px-4 py-2 text-sm font-medium hover:bg-black/4 disabled:opacity-50"
            [disabled]="busy()"
            (click)="eraseSessions()"
          >
            {{ copy().eraseButton }}
          </button>
          <button
            type="button"
            class="rounded-md border border-black/10 px-4 py-2 text-sm font-medium hover:bg-black/4 disabled:opacity-50"
            [disabled]="busy()"
            (click)="resetIdentity()"
          >
            {{ copy().resetButton }}
          </button>
        </div>
      </section>

      <p class="flex flex-wrap gap-4 text-xs text-muted-foreground">
        <a routerLink="/chat" class="underline underline-offset-2">{{ copy().backToChat }}</a>
        <a routerLink="/legal" class="underline underline-offset-2">{{ copy().legalHub }}</a>
        <a routerLink="/legal/subprocessors" class="underline underline-offset-2">{{
          copy().subprocessorsLink
        }}</a>
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

  readonly copy = computed(() => PRIVACY_PAGE_COPY[this.i18n.language()]);
  readonly busy = signal(false);

  readonly processors = [
    { name: 'DeepSeek', purpose: 'Large language model inference for chat' },
    { name: 'OpenAI', purpose: 'Optional image generation and text-to-speech' },
    { name: 'Serper', purpose: 'Web search tool results' },
    { name: 'LaunchDarkly', purpose: 'Feature flags (only with analytics consent)' },
    { name: 'Datadog', purpose: 'Optional RUM / APM (only with analytics consent)' },
  ];

  eraseSessions(): void {
    if (!confirm(this.copy().eraseConfirm)) {
      return;
    }
    this.busy.set(true);
    this.api.eraseAllSessions().subscribe({
      next: () => {
        this.chat.sessions.set([]);
        this.chat.activeSessionId.set(null);
        this.chat.messages.set([]);
        this.notify.showSuccess(this.copy().eraseSuccess);
        this.busy.set(false);
      },
      error: () => {
        this.notify.showError(this.copy().eraseFailed);
        this.busy.set(false);
      },
    });
  }

  resetIdentity(): void {
    if (!confirm(this.copy().resetConfirm)) {
      return;
    }
    this.busy.set(true);
    this.api.resetIdentity().subscribe({
      next: () => {
        this.chat.sessions.set([]);
        this.chat.activeSessionId.set(null);
        this.chat.messages.set([]);
        this.chat.loadSessions();
        this.notify.showSuccess(this.copy().resetSuccess);
        this.busy.set(false);
      },
      error: () => {
        this.notify.showError(this.copy().resetFailed);
        this.busy.set(false);
      },
    });
  }
}
