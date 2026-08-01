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

  it('should_listTools_with_serverName', () => {
    service.listTools().subscribe((tools) => {
      expect(tools).toHaveLength(1);
      expect(tools[0].name).toBe('get_weather');
      expect(tools[0].serverName).toBe('fetch');
    });

    const req = httpMock.expectOne('/api/mcp/client/tools');
    expect(req.request.method).toBe('GET');
    req.flush([{ name: 'get_weather', description: 'Weather lookup', serverName: 'fetch' }]);
  });

  it('should_listServers', () => {
    service.listServers().subscribe((servers) => {
      expect(servers).toHaveLength(1);
      expect(servers[0].name).toBe('fetch');
    });

    const req = httpMock.expectOne('/api/mcp/client/servers');
    req.flush([{
      name: 'fetch',
      toolCount: 1,
      resourceCount: 0,
      promptCount: 0,
      status: 'ACTIVE',
      capabilities: { tools: true, resources: false, prompts: false },
    }]);
  });

  it('should_listResources', () => {
    service.listResources().subscribe((resources) => {
      expect(resources[0].uri).toBe('config:///key');
    });

    const req = httpMock.expectOne('/api/mcp/client/resources');
    req.flush([{
      uri: 'config:///key',
      name: 'Configuration',
      description: 'Config',
      serverName: 'fetch',
    }]);
  });

  it('should_listPrompts', () => {
    service.listPrompts().subscribe((prompts) => {
      expect(prompts[0].name).toBe('greeting');
    });

    const req = httpMock.expectOne('/api/mcp/client/prompts');
    req.flush([{ name: 'greeting', description: 'Hi', serverName: 'fetch' }]);
  });
});
