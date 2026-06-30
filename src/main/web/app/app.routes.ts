import { Routes } from '@angular/router';
import { MainLayoutComponent } from './layout';

export const routes: Routes = [
  {
    path: '',
    component: MainLayoutComponent,
    children: [
      { path: '', redirectTo: 'rag', pathMatch: 'full' },
      {
        path: 'rag',
        loadComponent: () => import('./rag/rag.page').then(m => m.RagPageComponent),
      },
      {
        path: 'vision',
        loadComponent: () => import('./vision/vision.page').then(
          m => m.VisionPageComponent,
        ),
      },
      {
        path: 'ai-hubs',
        loadComponent: () => import('./ai-hub/ai-hub.page').then(m => m.AiHubPage),
        children: [
          {
            path: '',
            loadComponent: () => import('./ai-hub/hub-dashboard/hub-dashboard.component').then(m => m.HubDashboardComponent),
          },
          {
            path: 'chat',
            loadComponent: () => import('./ai-hub/chat/chat.component').then(m => m.ChatTabComponent),
          },
          {
            path: 'image',
            loadComponent: () => import('./ai-hub/image/image.component').then(m => m.ImageComponent),
          },
          {
            path: 'tts',
            loadComponent: () => import('./ai-hub/tts/tts.component').then(m => m.TtsPageComponent),
          },
        ],
      },
    ],
  },
];
