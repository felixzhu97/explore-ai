import { TestBed } from '@angular/core/testing';
import {
  HttpInterceptorFn,
  HttpRequest,
  HttpResponse,
} from '@angular/common/http';
import { of } from 'rxjs';
import { credentialsInterceptor } from './credentials.interceptor';

describe('credentialsInterceptor', () => {
  const interceptor: HttpInterceptorFn = (req, next) => {
    return TestBed.runInInjectionContext(() => {
      return credentialsInterceptor(req, next);
    });
  };

  beforeEach(() => {
    TestBed.configureTestingModule({});
  });

  it('should_setWithCredentials_whenRequestCloned', () => {
    const req = new HttpRequest('GET', '/api/sessions');
    let seen: HttpRequest<unknown> | null = null;

    interceptor(req, (outgoing) => {
      seen = outgoing;
      return of(new HttpResponse({ status: 200 }));
    }).subscribe();

    expect(seen?.withCredentials).toBe(true);
  });
});
