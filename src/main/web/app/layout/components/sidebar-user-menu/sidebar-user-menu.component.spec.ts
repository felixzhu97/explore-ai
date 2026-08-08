import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { AccountDialogService } from '../../../account/account-dialog.service';
import { API_BASE_URL } from '../../../core/api.constants';
import { AccountService } from '../../../core/account.service';
import { SidebarUserMenuComponent } from './sidebar-user-menu.component';
import { I18nService } from '../../../core/i18n';

describe('SidebarUserMenuComponent', () => {
  let fixture: ComponentFixture<SidebarUserMenuComponent>;
  let http: HttpTestingController;
  let account: AccountService;
  let accountDialog: {
    openLogin: ReturnType<typeof vi.fn>;
    openLogout: ReturnType<typeof vi.fn>;
  };

  beforeEach(async () => {
    accountDialog = {
      openLogin: vi.fn(),
      openLogout: vi.fn(),
    };

    await TestBed.configureTestingModule({
      imports: [SidebarUserMenuComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        I18nService,
        AccountService,
        { provide: AccountDialogService, useValue: accountDialog },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(SidebarUserMenuComponent);
    http = TestBed.inject(HttpTestingController);
    account = TestBed.inject(AccountService);
  });

  afterEach(() => {
    http.match(() => true).forEach(req => req.flush(null));
    http.verify();
  });

  it('should_showEmail_when_accountLoaded', async () => {
    account.load();
    http.expectOne(`${API_BASE_URL}/account/me`).flush({
      mode: 'authenticated',
      clientId: 'c1',
      userId: 'u1',
      email: 'user@example.com',
      plan: 'free',
      loginAvailable: true,
      loginProviders: ['google'],
    });
    await fixture.whenStable();
    fixture.detectChanges();

    expect(fixture.componentInstance.account()?.email).toBe('user@example.com');
    expect(fixture.componentInstance.displayName()).toBe('user@example.com');
  });

  it('should_showSignedIn_when_authenticatedWithoutEmail', async () => {
    account.load();
    http.expectOne(`${API_BASE_URL}/account/me`).flush({
      mode: 'authenticated',
      clientId: 'c1',
      userId: 'u1',
      email: null,
      plan: 'free',
      loginAvailable: true,
      loginProviders: ['github'],
    });
    await fixture.whenStable();
    fixture.detectChanges();

    expect(fixture.componentInstance.displayName()).toBe(
      TestBed.inject(I18nService).t().account.signedIn,
    );
    expect(fixture.componentInstance.showLogout()).toBe(true);
    expect(fixture.componentInstance.showLogin()).toBe(false);
  });

  it('should_showGuest_when_accountRequestFails', async () => {
    account.load();
    http.expectOne(`${API_BASE_URL}/account/me`).error(new ProgressEvent('error'));
    await fixture.whenStable();
    fixture.detectChanges();

    expect(fixture.componentInstance.account()).toBeNull();
    expect(fixture.componentInstance.displayName()).toBe(
      TestBed.inject(I18nService).t().account.guest,
    );
  });

  it('should_showLogin_when_loginAvailableAndAnonymous', async () => {
    account.load();
    http.expectOne(`${API_BASE_URL}/account/me`).flush({
      mode: 'anonymous',
      clientId: 'c1',
      userId: null,
      email: null,
      plan: 'free',
      loginAvailable: true,
      loginProviders: ['google', 'github'],
    });
    await fixture.whenStable();
    fixture.detectChanges();

    expect(fixture.componentInstance.showLogin()).toBe(true);
    expect(fixture.componentInstance.showLogout()).toBe(false);
  });

  it('should_openLoginDialog_when_loginClicked', async () => {
    account.load();
    http.expectOne(`${API_BASE_URL}/account/me`).flush({
      mode: 'anonymous',
      clientId: 'c1',
      userId: null,
      email: null,
      plan: 'free',
      loginAvailable: true,
      loginProviders: ['github'],
    });
    fixture.detectChanges();

    fixture.componentInstance.onLogin();
    expect(accountDialog.openLogin).toHaveBeenCalled();
  });

  it('should_openLogoutDialog_when_logoutClicked', async () => {
    account.load();
    http.expectOne(`${API_BASE_URL}/account/me`).flush({
      mode: 'authenticated',
      clientId: 'c1',
      userId: 'u1',
      email: 'a@b.com',
      plan: 'free',
      loginAvailable: true,
      loginProviders: ['google', 'github'],
    });
    await fixture.whenStable();
    fixture.detectChanges();

    fixture.componentInstance.onLogout();
    expect(accountDialog.openLogout).toHaveBeenCalledWith({
      email: 'a@b.com',
      displayName: 'a@b.com',
    });
  });
});
