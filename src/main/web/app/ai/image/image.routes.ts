import { Routes } from '@angular/router';

export const imageRoutes: Routes = [
  {
    path: '',
    loadComponent: () => import('./image.page').then((m) => m.ImagePageComponent),
  },
];
