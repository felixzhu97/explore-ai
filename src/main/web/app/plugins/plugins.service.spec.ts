import { describe, expect, it, beforeEach } from 'vitest';
import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { PluginsService } from './plugins.service';
import { API_BASE_URL } from '../core/api.constants';

describe('PluginsService', () => {
  let service: PluginsService;
  let httpMock: HttpTestingController;
  const base = `${API_BASE_URL}/plugins`;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), PluginsService],
    });
    service = TestBed.inject(PluginsService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  it('should request catalog when listCatalog called', () => {
    service.listCatalog().subscribe((catalog) => {
      expect(catalog).toHaveLength(1);
      expect(catalog[0].id).toBe('github');
    });

    const req = httpMock.expectOne(`${base}/catalog`);
    expect(req.request.method).toBe('GET');
    req.flush([{ id: 'github', name: 'GitHub' }]);
  });

  it('should post install body when install called', () => {
    service
      .install({ definitionId: 'custom', endpoint: 'https://example.com/mcp' })
      .subscribe((installed) => {
        expect(installed.id).toBe('1');
      });

    const req = httpMock.expectOne(`${base}/install`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({
      definitionId: 'custom',
      endpoint: 'https://example.com/mcp',
    });
    req.flush({ id: '1', definitionId: 'custom', enabled: true });
  });

  it('should map tools payload when listTools called', () => {
    service.listTools('inst-1').subscribe((tools) => {
      expect(tools).toEqual(['get_weather', 'search']);
    });

    const req = httpMock.expectOne(`${base}/installed/inst-1/tools`);
    expect(req.request.method).toBe('GET');
    req.flush({ tools: ['get_weather', 'search'] });
  });
});
