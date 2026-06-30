import { Routes } from '@angular/router';

export const aiHubRoutes: Routes = [
  {
    path: '',
    loadComponent: () => import('./hub-dashboard/hub-dashboard.component').then(m => m.HubDashboardComponent),
  },
  {
    path: 'chat',
    loadComponent: () => import('./chat/chat.component').then(m => m.ChatTabComponent),
  },
  {
    path: 'image',
    loadComponent: () => import('./image/image.component').then(m => m.ImageComponent),
  },
  {
    path: 'tts',
    loadComponent: () => import('./tts/tts.component').then(m => m.TtsPageComponent),
  },
];
