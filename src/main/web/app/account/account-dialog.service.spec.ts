import { describe, it, expect, beforeEach, vi } from 'vitest';
import { TestBed } from '@angular/core/testing';
import { ZardDialogService } from '../shared/components/dialog';
import { AccountDialogService } from './account-dialog.service';
import { AccountLoginDialogComponent } from './account-login-dialog.component';
import { AccountLogoutDialogComponent } from './account-logout-dialog.component';

describe('AccountDialogService', () => {
  let service: AccountDialogService;
  let create: ReturnType<typeof vi.fn>;

  beforeEach(() => {
    create = vi.fn().mockReturnValue({ close: vi.fn() });
    TestBed.configureTestingModule({
      providers: [
        AccountDialogService,
        { provide: ZardDialogService, useValue: { create } },
      ],
    });
    service = TestBed.inject(AccountDialogService);
  });

  it('should open login dialog when open login called', () => {
    service.openLogin();

    expect(create).toHaveBeenCalledWith(
      expect.objectContaining({
        zContent: AccountLoginDialogComponent,
        zHideFooter: true,
        zMaskClosable: true,
      }),
    );
  });

  it('should open logout dialog when open logout called', () => {
    service.openLogout({ email: 'a@b.com', displayName: 'A' });

    expect(create).toHaveBeenCalledWith(
      expect.objectContaining({
        zContent: AccountLogoutDialogComponent,
        zData: { email: 'a@b.com', displayName: 'A' },
        zHideFooter: true,
      }),
    );
  });
});
