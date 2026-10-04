import { Routes } from '@angular/router';

export const METRICS_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () => import('./pages/metrics-overview.page').then(m => m.MetricsOverviewPageComponent),
  },
  {
    path: ':domain',
    loadComponent: () => import('./pages/metrics-domain.page').then(m => m.MetricsDomainPageComponent),
  },
];
