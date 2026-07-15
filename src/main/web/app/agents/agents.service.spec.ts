import { describe, expect, it, beforeEach } from 'vitest';
import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { AgentsService } from './agents.service';
import { API_BASE_URL } from '@core/services/api.constants';

describe('AgentsService', () => {
  let service: AgentsService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), AgentsService],
    });
    service = TestBed.inject(AgentsService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  it('should_list_agents_from_api', () => {
    const mockAgents = [
      {
        type: 'supervisor',
        name: 'Supervisor',
        description: 'coords',
        healthy: true,
        supervisor: true,
      },
    ];

    service.listAgents().subscribe((agents) => {
      expect(agents).toHaveLength(1);
      expect(agents[0].type).toBe('supervisor');
    });

    const req = httpMock.expectOne(`${API_BASE_URL}/agents/list`);
    expect(req.request.method).toBe('GET');
    req.flush(mockAgents);
  });

  it('should_get_agent_health', () => {
    service.getHealth('k8s').subscribe((health) => {
      expect(health.status).toBe('UP');
    });

    const req = httpMock.expectOne(`${API_BASE_URL}/agents/k8s/health`);
    req.flush({ type: 'k8s', healthy: true, status: 'UP' });
  });
});
