import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { API_BASE_URL } from '../core/api.constants';
import type {
  InstallPluginRequest,
  PluginDefinition,
  PluginInstallation,
} from './plugins.model';

@Injectable({ providedIn: 'root' })
export class PluginsService {
  private readonly http = inject(HttpClient);
  private readonly base = `${API_BASE_URL}/plugins`;

  listCatalog(): Observable<PluginDefinition[]> {
    return this.http.get<PluginDefinition[]>(`${this.base}/catalog`);
  }

  listInstalled(): Observable<PluginInstallation[]> {
    return this.http.get<PluginInstallation[]>(`${this.base}/installed`);
  }

  install(body: InstallPluginRequest): Observable<PluginInstallation> {
    return this.http.post<PluginInstallation>(`${this.base}/install`, body);
  }

  setEnabled(id: string, enabled: boolean): Observable<PluginInstallation> {
    return this.http.patch<PluginInstallation>(`${this.base}/installed/${id}`, { enabled });
  }

  uninstall(id: string): Observable<void> {
    return this.http.delete<void>(`${this.base}/installed/${id}`);
  }

  listTools(id: string): Observable<string[]> {
    return this.http
      .get<{ tools: string[] }>(`${this.base}/installed/${id}/tools`)
      .pipe(map(res => res.tools ?? []));
  }
}
