import { type Routes } from '@angular/router';

export const METRICS_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () => import('./metrics-overview.page').then(m => m.MetricsOverviewPageComponent),
  },
  {
    path: ':capability',
    loadComponent: () => import('./metrics-capability.page').then(m => m.MetricsCapabilityPageComponent),
  },
];
