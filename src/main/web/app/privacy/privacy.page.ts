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
    <div class="mx-auto w-full max-w-(--container-2xl) px-6 py-12 md:py-16">
      <header>
        <h1
          class="
            text-[2rem] leading-tight font-medium tracking-[-0.03em] text-[#0D0D0D]
            md:text-[2.5rem]
          "
        >
          {{ copy().title }}
        </h1>
        <p class="mt-4 text-[15px] leading-relaxed text-[#5C5C5C]">{{ copy().subtitle }}</p>
      </header>

      <div class="mt-12 space-y-10">
        <section>
          <h2 class="text-xl leading-snug font-medium tracking-[-0.02em] text-[#0D0D0D]">
            {{ copy().noticeHeading }}
          </h2>
          <p class="mt-3 text-[15px] leading-relaxed text-[#0D0D0D]/80">
            {{ copy().noticeIdentity }}
          </p>
          <p class="mt-3 text-[15px] leading-relaxed text-[#0D0D0D]/80">
            {{ copy().noticeChat }}
          </p>
          <p class="mt-3 text-[15px] leading-relaxed text-[#0D0D0D]/80">
            {{ copy().noticeRetention }}
          </p>
        </section>

        <section>
          <h2 class="text-xl leading-snug font-medium tracking-[-0.02em] text-[#0D0D0D]">
            {{ copy().processorsHeading }}
          </h2>
          <ul class="mt-3 list-disc space-y-2 pl-5 text-[15px] leading-relaxed text-[#0D0D0D]/80">
            @for (item of processors; track item.name) {
              <li>
                <span class="font-medium text-[#0D0D0D]">{{ item.name }}</span>
                — {{ item.purpose }}
              </li>
            }
          </ul>
        </section>

        <app-privacy-preferences-form [copy]="copy()" />

        <section>
          <h2 class="text-xl leading-snug font-medium tracking-[-0.02em] text-[#0D0D0D]">
            {{ copy().controlsHeading }}
          </h2>
          <p class="mt-3 text-[15px] leading-relaxed text-[#0D0D0D]/80">
            {{ copy().controlsHelp }}
          </p>
          <div class="mt-6 flex flex-wrap gap-3">
            <button
              type="button"
              class="
                rounded-full border border-black/12 px-4 py-2 text-[14px] font-medium
                text-[#0D0D0D] transition-opacity hover:opacity-70 disabled:opacity-40
              "
              [disabled]="busy()"
              (click)="eraseSessions()"
            >
              {{ copy().eraseButton }}
            </button>
            <button
              type="button"
              class="
                rounded-full border border-black/12 px-4 py-2 text-[14px] font-medium
                text-[#0D0D0D] transition-opacity hover:opacity-70 disabled:opacity-40
              "
              [disabled]="busy()"
              (click)="resetIdentity()"
            >
              {{ copy().resetButton }}
            </button>
          </div>
        </section>
      </div>

      <footer class="mt-14 flex flex-wrap gap-x-6 gap-y-2 border-t border-black/8 pt-6">
        <a
          routerLink="/chat"
          class="text-[13px] text-[#8F8F8F] no-underline underline-offset-4 hover:text-[#0D0D0D] hover:underline"
        >
          {{ copy().backToChat }}
        </a>
        <a
          routerLink="/policies"
          class="text-[13px] text-[#8F8F8F] no-underline underline-offset-4 hover:text-[#0D0D0D] hover:underline"
        >
          {{ copy().legalHub }}
        </a>
        <a
          routerLink="/policies/subprocessors"
          class="text-[13px] text-[#8F8F8F] no-underline underline-offset-4 hover:text-[#0D0D0D] hover:underline"
        >
          {{ copy().subprocessorsLink }}
        </a>
      </footer>
    </div>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: {
    class: 'flex min-h-0 w-full flex-1 flex-col overflow-y-auto bg-white',
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
