import { describe, it, expect, beforeEach, afterEach } from 'vitest';
import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { httpResource } from '@angular/common/http';
import { computed, ApplicationRef, Injector, runInInjectionContext } from '@angular/core';
import { API_BASE_URL } from '../core/api.constants';
import type { AgentInfo } from './pipelines.model';

describe('AgentsPage httpResource', () => {
  let http: HttpTestingController;
  let injector: Injector;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    http = TestBed.inject(HttpTestingController);
    injector = TestBed.inject(Injector);
  });

  afterEach(() => {
    http.match(() => true).forEach(req => req.flush([]));
    http.verify();
  });

  it('should load agents via http resource when mounted', async () => {
    const agents = runInInjectionContext(injector, () => {
      const resource = httpResource<AgentInfo[]>(() => `${API_BASE_URL}/pipelines/list`);
      const agentsSignal = computed(() => resource.hasValue() ? resource.value()! : [],
      );
      return { resource, agentsSignal };
    });

    TestBed.tick();
    const req = http.expectOne(`${API_BASE_URL}/pipelines/list`);
    req.flush([
      {
        type: 'supervisor',
        name: 'Supervisor',
        description: 'Routes tasks',
        healthy: true,
        supervisor: true,
      },
    ]);
    await TestBed.inject(ApplicationRef).whenStable();

    expect(agents.agentsSignal()).toHaveLength(1);
    expect(agents.agentsSignal()[0]?.type).toBe('supervisor');
  });

  it('should expose error when agents request fails', async () => {
    const resource = runInInjectionContext(injector, () => httpResource<AgentInfo[]>(() => `${API_BASE_URL}/pipelines/list`),
    );

    TestBed.tick();
    http.expectOne(`${API_BASE_URL}/pipelines/list`).error(new ProgressEvent('error'));
    await TestBed.inject(ApplicationRef).whenStable();

    expect(resource.error()).toBeTruthy();
    expect(resource.hasValue()).toBe(false);
  });
});
