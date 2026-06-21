import { Routes } from '@angular/router';

export const chatRoutes: Routes = [
  {
    path: '',
    loadComponent: () => import('./chat.page').then((m) => m.ChatPageComponent),
  },
];
