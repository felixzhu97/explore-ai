import { describe, it, expect, beforeEach, afterEach } from 'vitest';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { API_BASE_URL } from '../../../core/api.constants';
import { SidebarUserMenuComponent } from './sidebar-user-menu.component';
import { I18nService } from '../../../core/i18n';

describe('SidebarUserMenuComponent', () => {
  let fixture: ComponentFixture<SidebarUserMenuComponent>;
  let http: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SidebarUserMenuComponent],
      providers: [provideHttpClient(), provideHttpClientTesting(), I18nService],
    }).compileComponents();

    fixture = TestBed.createComponent(SidebarUserMenuComponent);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    http.match(() => true).forEach(req => req.flush(null));
    http.verify();
  });

  it('should_load_account_via_httpResource_when_mounted', async () => {
    fixture.detectChanges();
    const req = http.expectOne(`${API_BASE_URL}/account/me`);
    req.flush({
      mode: 'anonymous',
      clientId: 'c1',
      userId: null,
      email: 'user@example.com',
      plan: 'free',
    });
    await fixture.whenStable();
    fixture.detectChanges();

    expect(fixture.componentInstance.account()?.email).toBe('user@example.com');
    expect(fixture.componentInstance.displayName()).toBe('user@example.com');
  });

  it('should_showGuest_when_accountRequestFails', async () => {
    fixture.detectChanges();
    http.expectOne(`${API_BASE_URL}/account/me`).error(new ProgressEvent('error'));
    await fixture.whenStable();
    fixture.detectChanges();

    expect(fixture.componentInstance.account()).toBeNull();
    expect(fixture.componentInstance.displayName()).toBe(
      TestBed.inject(I18nService).t().account.guest,
    );
  });
});
