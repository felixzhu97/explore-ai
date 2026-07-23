import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { API_BASE_URL } from '../../../core/api.constants';

export interface ToolCatalogEntryDto {
  name: string;
  description: string;
  source: 'LOCAL' | 'MCP' | string;
}

@Injectable({ providedIn: 'root' })
export class ToolsCatalogService {
  private readonly http = inject(HttpClient);

  listCatalog(): Observable<ToolCatalogEntryDto[]> {
    return this.http
      .get<ToolCatalogEntryDto[]>(`${API_BASE_URL}/tools/catalog`)
      .pipe(map(entries => entries ?? []));
  }
}
