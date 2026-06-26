import { Routes } from '@angular/router';

export const visionRoutes: Routes = [
  {
    path: '',
    loadComponent: () => import('./vision.page').then((m) => m.VisionPageComponent),
  },
];
