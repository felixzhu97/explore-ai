import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
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

      <button
        type="button"
        z-button
        zType="outline"
        zSize="lg"
        class="h-11 w-full justify-center gap-3 rounded-xl border-border bg-background text-sm font-medium"
        (click)="continueWithGoogle()"
      >
        <span class="inline-flex size-5 shrink-0" [innerHTML]="googleIcon"></span>
        <span>{{ t().account.continueWithGoogle }}</span>
      </button>
    </div>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AccountLoginDialogComponent {
  private readonly account = inject(AccountService);
  private readonly dialogRef = inject(ZardDialogRef);
  private readonly i18n = inject(I18nService);
  private readonly sanitizer = inject(DomSanitizer);

  readonly googleIcon = this.sanitizer.bypassSecurityTrustHtml(
    `<svg viewBox="0 0 24 24" class="size-5" aria-hidden="true">
      <path fill="#4285F4" d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z"/>
      <path fill="#34A853" d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"/>
      <path fill="#FBBC05" d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z"/>
      <path fill="#EA4335" d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z"/>
    </svg>`,
  );

  get t() {
    return this.i18n.t;
  }

  continueWithGoogle(): void {
    this.dialogRef.close();
    this.account.startGoogleLogin();
  }
}
