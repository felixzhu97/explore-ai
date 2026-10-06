import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
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
      providers: [McpService, provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(McpService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  it('should fetch health', () => {
    service.getHealth().subscribe((response) => {
      expect(response.status).toBe('UP');
    });

    const request = httpMock.expectOne('/api/mcp/health');
    expect(request.request.method).toBe('GET');
    request.flush({
      status: 'UP',
      server: 'explore-ai-mcp-server',
      version: '1.0.0',
      protocol: 'MCP 1.0',
    });
  });

  it('should list tools', () => {
    service.listTools().subscribe((tools) => {
      expect(tools).toHaveLength(1);
      expect(tools[0]?.name).toBe('get_weather');
    });

    const request = httpMock.expectOne('/api/mcp/client/tools');
    expect(request.request.method).toBe('GET');
    request.flush([{ name: 'get_weather', description: 'Weather lookup' }]);
  });
});
