import { Routes } from '@angular/router';

export const ttsRoutes: Routes = [
  {
    path: '',
    loadComponent: () => import('./tts.page').then((m) => m.TtsPageComponent),
  },
];
