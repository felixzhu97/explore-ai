import { Routes } from '@angular/router';

export const ragRoutes: Routes = [
  {
    path: '',
    loadComponent: () => import('./rag.page').then((m) => m.RagPageComponent),
  },
];
