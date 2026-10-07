import { describe, expect, it, beforeEach } from 'vitest';
import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { AgentsService, type CustomAgentResponse } from './agents.service';
import { API_BASE_URL } from '../http/api.constants';

const STAMPS = { createdAt: '2026-01-01T00:00:00Z', updatedAt: '2026-01-02T00:00:00Z' };

describe('AgentsService', () => {
  let service: AgentsService;
  let httpMock: HttpTestingController;
  const customAgentsBase = `${API_BASE_URL}/pipelines/agents`;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), AgentsService],
    });
    service = TestBed.inject(AgentsService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  it('should list custom agents from api', () => {
    service.listCustomAgents().subscribe((agents) => {
      expect(agents).toHaveLength(1);
      expect(agents[0]?.agentType).toBe('researcher');
    });

    const request = httpMock.expectOne(customAgentsBase);
    expect(request.request.method).toBe('GET');
    const response: CustomAgentResponse[] = [
      {
        id: '1',
        agentType: 'researcher',
        name: 'Researcher',
        description: '',
        systemPrompt: 'You research.',
        tools: ['web'],
        enabled: true,
        ...STAMPS,
      },
    ];
    request.flush(response);
  });

  it('should create custom agent', () => {
    const body = {
      agentType: 'custom',
      name: 'Custom',
      description: 'd',
      systemPrompt: 'prompt',
      tools: ['web'],
    };
    service.createAgent(body).subscribe((agent) => {
      expect(agent.id).toBe('42');
    });

    const request = httpMock.expectOne(customAgentsBase);
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual(body);
    const response: CustomAgentResponse = { id: '42', ...body, enabled: true, ...STAMPS };
    request.flush(response);
  });

  it('should set enabled via patch', () => {
    service.setEnabled('42', false).subscribe((agent) => {
      expect(agent.enabled).toBe(false);
    });

    const request = httpMock.expectOne(`${customAgentsBase}/42/enabled`);
    expect(request.request.method).toBe('PATCH');
    expect(request.request.body).toEqual({ enabled: false });
    const response: CustomAgentResponse = {
      id: '42',
      agentType: 'custom',
      name: 'Custom',
      description: '',
      systemPrompt: 'p',
      tools: [],
      enabled: false,
      ...STAMPS,
    };
    request.flush(response);
  });
});
