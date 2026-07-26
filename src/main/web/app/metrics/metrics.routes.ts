import { Routes } from '@angular/router';

export const METRICS_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () => import('./pages/metrics-overview.page').then(m => m.MetricsOverviewPage),
  },
  {
    path: ':domain',
    loadComponent: () => import('./pages/metrics-domain.page').then(m => m.MetricsDomainPage),
  },
];
