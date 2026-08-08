import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { AccountService } from '../core/account.service';
import { I18nService } from '../core/i18n';
import { ZardButtonComponent } from '../shared/components/button';
import { Z_MODAL_DATA, ZardDialogRef } from '../shared/components/dialog';

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
          @if (data.email) {
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
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AccountLogoutDialogComponent {
  readonly data = inject<AccountLogoutDialogData>(Z_MODAL_DATA);
  private readonly account = inject(AccountService);
  private readonly dialogRef = inject(ZardDialogRef);
  private readonly i18n = inject(I18nService);

  readonly avatarLetter = (
    this.data.displayName?.trim() || this.data.email || 'G'
  ).charAt(0).toUpperCase();

  get t() {
    return this.i18n.t;
  }

  confirmLogout(): void {
    this.dialogRef.close();
    this.account.logout();
  }

  cancel(): void {
    this.dialogRef.close();
  }
}
