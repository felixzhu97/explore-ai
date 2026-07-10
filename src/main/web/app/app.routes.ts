import { Routes } from '@angular/router';
import { MainLayoutComponent } from './layout';

export const routes: Routes = [
  {
    path: '',
    component: MainLayoutComponent,
    children: [
      { path: '', redirectTo: 'chat', pathMatch: 'full' },
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
        path: 'chat',
        loadComponent: () => import('./ai-hub/chat/chat.component').then(m => m.ChatTabComponent),
      },
      {
        path: 'generate',
        loadComponent: () => import('./generate/generate.page').then(m => m.GeneratePage),
        children: [
          { path: '', redirectTo: 'image', pathMatch: 'full' },
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
