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
    let withCredentials = false;

    interceptor(req, (outgoing) => {
      withCredentials = outgoing.withCredentials;
      return of(new HttpResponse({ status: 200 }));
    }).subscribe();

    expect(withCredentials).toBe(true);
  });
});
