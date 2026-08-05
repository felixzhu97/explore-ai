import { describe, expect, it, beforeEach } from 'vitest';
import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { AgentsService } from './agents.service';
import { API_BASE_URL } from '../core/api.constants';

describe('AgentsService', () => {
  let service: AgentsService;
  let httpMock: HttpTestingController;
  const libraryBase = `${API_BASE_URL}/pipelines/agents/library`;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), AgentsService],
    });
    service = TestBed.inject(AgentsService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  it('should_list_library_from_api', () => {
    service.listLibrary().subscribe((agents) => {
      expect(agents).toHaveLength(1);
      expect(agents[0].typeKey).toBe('researcher');
    });

    const req = httpMock.expectOne(libraryBase);
    expect(req.request.method).toBe('GET');
    req.flush([
      {
        id: '1',
        typeKey: 'researcher',
        name: 'Researcher',
        description: '',
        systemPrompt: 'You research.',
        toolKeys: ['web'],
        enabled: true,
      },
    ]);
  });

  it('should_create_saved_agent', () => {
    const body = {
      typeKey: 'custom',
      name: 'Custom',
      description: 'd',
      systemPrompt: 'prompt',
      toolKeys: ['web'],
    };
    service.create(body).subscribe((agent) => {
      expect(agent.id).toBe('42');
    });

    const req = httpMock.expectOne(libraryBase);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(body);
    req.flush({ id: '42', ...body, enabled: true });
  });

  it('should_set_enabled_via_patch', () => {
    service.setEnabled('42', false).subscribe((agent) => {
      expect(agent.enabled).toBe(false);
    });

    const req = httpMock.expectOne(`${libraryBase}/42/enabled`);
    expect(req.request.method).toBe('PATCH');
    expect(req.request.body).toEqual({ enabled: false });
    req.flush({
      id: '42',
      typeKey: 'custom',
      name: 'Custom',
      description: '',
      systemPrompt: 'p',
      toolKeys: [],
      enabled: false,
    });
  });
});
