import { Injectable, inject } from '@angular/core';
import { ZardDialogService } from '../shared/components/dialog';
import { AccountLoginDialogComponent } from './account-login-dialog.component';
import {
  AccountLogoutDialogComponent,
  type AccountLogoutDialogData,
} from './account-logout-dialog.component';

@Injectable({ providedIn: 'root' })
export class AccountDialogService {
  private readonly dialog = inject(ZardDialogService);

  openLogin(): void {
    this.dialog.create({
      zContent: AccountLoginDialogComponent,
      zHideFooter: true,
      zWidth: '24rem',
      zMaskClosable: true,
      zCustomClasses: 'rounded-2xl border border-border bg-background p-5 shadow-xl',
    });
  }

  openLogout(data: AccountLogoutDialogData): void {
    this.dialog.create({
      zContent: AccountLogoutDialogComponent,
      zData: data,
      zHideFooter: true,
      zWidth: '24rem',
      zMaskClosable: true,
      zCustomClasses: 'rounded-2xl border border-border bg-background p-5 shadow-xl',
    });
  }
}
