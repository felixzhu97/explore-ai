import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { API_BASE_URL } from './api.constants';
import { AccountService } from './account.service';
import { NotificationService } from './notification.service';

describe('AccountService', () => {
  let service: AccountService;
  let http: HttpTestingController;
  let notifications: NotificationService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        AccountService,
        {
          provide: NotificationService,
          useValue: {
            showSuccess: vi.fn(),
            showError: vi.fn(),
          },
        },
      ],
    });
    service = TestBed.inject(AccountService);
    http = TestBed.inject(HttpTestingController);
    notifications = TestBed.inject(NotificationService);
    sessionStorage.clear();
  });

  afterEach(() => {
    http.verify();
    sessionStorage.clear();
  });

  it('should_loadAnonymousAccount_when_meSucceeds', () => {
    service.load();
    http.expectOne(`${API_BASE_URL}/account/me`).flush({
      mode: 'anonymous',
      clientId: 'c1',
      userId: null,
      email: null,
      plan: 'free',
      loginAvailable: true,
    });

    expect(service.account()?.mode).toBe('anonymous');
    expect(service.showLogin()).toBe(true);
  });

  it('should_notifySuccess_when_loginQueryIsSuccess', () => {
    service.consumeLoginReturn('?login=success', '/');
    http.expectOne(`${API_BASE_URL}/account/me`).flush({
      mode: 'authenticated',
      clientId: 'c1',
      userId: 'u1',
      email: 'a@b.com',
      plan: 'free',
      loginAvailable: true,
    });

    expect(notifications.showSuccess).toHaveBeenCalled();
  });
});
