import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { ChatService } from '../chat/chat.service';
import { API_BASE_URL } from './api.constants';
import { AccountService } from './account.service';
import { NotificationService } from './notification.service';

describe('AccountService', () => {
  let service: AccountService;
  let http: HttpTestingController;
  let notifications: NotificationService;
  let chat: { resetForOwnerChange: ReturnType<typeof vi.fn> };

  beforeEach(() => {
    chat = { resetForOwnerChange: vi.fn() };
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        AccountService,
        { provide: ChatService, useValue: chat },
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

  it('should load anonymous account when me succeeds', () => {
    service.load();
    http.expectOne(`${API_BASE_URL}/account/me`).flush({
      mode: 'anonymous',
      clientId: 'c1',
      userId: null,
      email: null,
      plan: 'free',
      loginAvailable: true,
      loginProviders: ['google', 'github'],
    });

    expect(service.account()?.mode).toBe('anonymous');
    expect(service.showLogin()).toBe(true);
    expect(service.loginProviders()).toEqual(['google', 'github']);
  });

  it('should notify success when login query is success', () => {
    service.consumeLoginReturn('?login=success', '/');
    http.expectOne(`${API_BASE_URL}/account/me`).flush({
      mode: 'authenticated',
      clientId: 'c1',
      userId: 'u1',
      email: 'a@b.com',
      plan: 'free',
      loginAvailable: true,
      loginProviders: ['google'],
    });

    expect(notifications.showSuccess).toHaveBeenCalled();
    expect(chat.resetForOwnerChange).toHaveBeenCalled();
  });

  it('should reset chat owner scope when logout succeeds', () => {
    service.logout();
    http.expectOne(`${API_BASE_URL}/account/logout`).flush(null, { status: 204, statusText: 'No Content' });
    http.expectOne(`${API_BASE_URL}/account/me`).flush({
      mode: 'anonymous',
      clientId: 'c1',
      userId: null,
      email: null,
      plan: 'free',
      loginAvailable: true,
      loginProviders: ['google'],
    });

    expect(chat.resetForOwnerChange).toHaveBeenCalled();
    expect(notifications.showSuccess).toHaveBeenCalled();
  });

  it('should assign github authorization when start oauth login github', () => {
    const assign = vi.fn();
    service.startOAuthLogin('github', assign);
    expect(assign).toHaveBeenCalledWith('/oauth2/authorization/github');
  });
});
