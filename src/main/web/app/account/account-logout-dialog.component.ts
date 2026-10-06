import { Component, inject } from '@angular/core';
import { AccountService } from './account.service';
import { I18nService } from '../i18n';
import { ZardButtonComponent } from '../ui/button';
import { Z_MODAL_DATA, ZardDialogRef } from '../ui/dialog';
import { textOr } from '../shared/presence';

export interface AccountLogoutDialogData {
  email: string | null;
  displayName: string;
}

@Component({
  selector: 'app-account-logout-dialog',
  imports: [ZardButtonComponent],
  template: `
    <div class="flex flex-col gap-5 px-1 pt-2 pb-1">
      <h3 class="text-center text-lg font-semibold tracking-tight text-foreground">
        {{ t().account.logoutDialogTitle }}
      </h3>

      <div
        class="
          flex items-center gap-3 rounded-xl border border-border bg-background
          p-3 text-left
        "
      >
        <span
          class="
            flex size-10 shrink-0 items-center justify-center rounded-full
            bg-[#007AFF] text-sm font-semibold text-white
          "
          aria-hidden="true"
        >
          {{ avatarLetter }}
        </span>
        <span class="min-w-0 flex-1">
          <span class="block truncate text-sm font-medium text-foreground">
            {{ data.displayName }}
          </span>
          @if (data.email !== null && data.email !== '') {
            <span class="block truncate text-xs text-muted-foreground">
              {{ data.email }}
            </span>
          }
        </span>
      </div>

      <div class="flex flex-col gap-2">
        <button
          type="button"
          z-button
          zSize="lg"
          class="
            h-11 w-full rounded-xl border-transparent bg-foreground text-sm
            font-medium text-background hover:bg-foreground/90
          "
          (click)="confirmLogout()"
        >
          {{ t().account.logoutConfirm }}
        </button>
        <button
          type="button"
          z-button
          zType="outline"
          zSize="lg"
          class="h-11 w-full rounded-xl text-sm font-medium"
          (click)="cancel()"
        >
          {{ t().account.logoutCancel }}
        </button>
      </div>
    </div>
  `,
})
export class AccountLogoutDialogComponent {
  readonly #account = inject(AccountService);
  readonly #dialogRef = inject(ZardDialogRef);
  readonly #i18n = inject(I18nService);
  readonly data = inject<AccountLogoutDialogData>(Z_MODAL_DATA);

  get t() {
    return this.#i18n.t;
  }

  readonly avatarLetter = (
    textOr(this.data.displayName.trim(), textOr(this.data.email, 'G'))
  ).charAt(0).toUpperCase();

  /** Closes the dialog and signs out. */
  confirmLogout(): void {
    this.#dialogRef.close();
    this.#account.logout();
  }

  /** Closes the dialog without signing out. */
  cancel(): void {
    this.#dialogRef.close();
  }
}
