import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { DomSanitizer } from '@angular/platform-browser';
import { AccountService } from '../core/account.service';
import { I18nService } from '../core/i18n';
import { ZardButtonComponent } from '../shared/components/button';
import { ZardDialogRef } from '../shared/components/dialog';

@Component({
  selector: 'app-account-login-dialog',
  imports: [ZardButtonComponent],
  template: `
    <div class="flex flex-col gap-5 px-1 pt-2 pb-1 text-center">
      <div class="flex flex-col gap-2">
        <h3 class="text-xl font-semibold tracking-tight text-foreground">
          {{ t().account.loginDialogTitle }}
        </h3>
        <p class="text-sm text-muted-foreground">
          {{ t().account.loginDialogDescription }}
        </p>
      </div>

      <div class="flex flex-col gap-3">
        @if (showGoogle()) {
          <button
            type="button"
            z-button
            zType="outline"
            zSize="lg"
            class="h-11 w-full justify-center gap-3 rounded-xl border-border bg-background text-sm font-medium"
            (click)="continueWith('google')"
          >
            <span class="inline-flex size-5 shrink-0" [innerHTML]="googleIcon"></span>
            <span>{{ t().account.continueWithGoogle }}</span>
          </button>
        }
        @if (showGithub()) {
          <button
            type="button"
            z-button
            zType="outline"
            zSize="lg"
            class="h-11 w-full justify-center gap-3 rounded-xl border-border bg-background text-sm font-medium"
            (click)="continueWith('github')"
          >
            <span class="inline-flex size-5 shrink-0" [innerHTML]="githubIcon"></span>
            <span>{{ t().account.continueWithGithub }}</span>
          </button>
        }
      </div>
    </div>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AccountLoginDialogComponent {
  private readonly account = inject(AccountService);
  private readonly dialogRef = inject(ZardDialogRef);
  private readonly i18n = inject(I18nService);
  private readonly sanitizer = inject(DomSanitizer);

  readonly showGoogle = computed(() => this.account.loginProviders().includes('google'));
  readonly showGithub = computed(() => this.account.loginProviders().includes('github'));

  readonly googleIcon = this.sanitizer.bypassSecurityTrustHtml(
    `<svg viewBox="0 0 24 24" class="size-5" aria-hidden="true">
      <path fill="#4285F4" d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z"/>
      <path fill="#34A853" d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"/>
      <path fill="#FBBC05" d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z"/>
      <path fill="#EA4335" d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z"/>
    </svg>`,
  );

  readonly githubIcon = this.sanitizer.bypassSecurityTrustHtml(
    `<svg viewBox="0 0 24 24" class="size-5" aria-hidden="true" fill="currentColor"><path d="M12 .5C5.73.5.75 5.48.75 11.76c0 4.97 3.22 9.18 7.69 10.66.56.1.77-.24.77-.54 0-.27-.01-1.16-.02-2.1-3.13.68-3.79-1.33-3.79-1.33-.51-1.3-1.25-1.65-1.25-1.65-.1-.82.62-.82.62-.82 1.13.08 1.72 1.16 1.72 1.16 1.11 1.9 2.91 1.35 3.62 1.03.11-.81.43-1.35.78-1.66-2.5-.28-5.13-1.25-5.13-5.56 0-1.23.44-2.23 1.16-3.02-.12-.28-.5-1.42.11-2.96 0 0 .95-.3 3.11 1.15a10.7 10.7 0 0 1 2.83-.38c.96 0 1.93.13 2.83.38 2.16-1.45 3.11-1.15 3.11-1.15.61 1.54.23 2.68.11 2.96.72.79 1.16 1.79 1.16 3.02 0 4.32-2.64 5.27-5.15 5.55.44.38.83 1.12.83 2.26 0 1.63-.01 2.95-.01 3.35 0 .3.2.65.78.54 4.46-1.49 7.67-5.7 7.67-10.66C23.25 5.48 18.27.5 12 .5z"/></svg>`,
  );

  get t() {
    return this.i18n.t;
  }

  continueWith(provider: 'google' | 'github'): void {
    this.dialogRef.close();
    this.account.startOAuthLogin(provider);
  }
}
