import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { describe, expect, it, beforeEach, afterEach } from 'vitest';
import { McpService } from './mcp.service';

describe('McpService', () => {
  let service: McpService;
  let httpMock: HttpTestingController;

  afterEach(() => {
    httpMock.verify();
  });

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [McpService],
    });
    service = TestBed.inject(McpService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  it('should_fetchHealth', () => {
    service.getHealth().subscribe((response) => {
      expect(response.status).toBe('UP');
    });

    const req = httpMock.expectOne('/api/mcp/health');
    expect(req.request.method).toBe('GET');
    req.flush({
      status: 'UP',
      server: 'explore-ai-mcp-server',
      version: '1.0.0',
      protocol: 'MCP 1.0',
    });
  });

  it('should_listTools', () => {
    service.listTools().subscribe((tools) => {
      expect(tools).toHaveLength(1);
      expect(tools[0].name).toBe('get_weather');
    });

    const req = httpMock.expectOne('/api/mcp/client/tools');
    expect(req.request.method).toBe('GET');
    req.flush([{ name: 'get_weather', description: 'Weather lookup' }]);
  });
});
