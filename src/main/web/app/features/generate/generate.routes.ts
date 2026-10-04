import { Routes } from '@angular/router';
import { GeneratePageComponent } from './pages/generate.page';

export const GENERATE_ROUTES: Routes = [
  {
    path: '',
    component: GeneratePageComponent,
    children: [
      { path: '', redirectTo: 'image', pathMatch: 'full' },
      {
        path: 'image',
        loadComponent: () => import('./image/pages/image.page').then(m => m.ImagePageComponent),
      },
      {
        path: 'tts',
        loadComponent: () => import('./tts/pages/tts.page').then(m => m.TtsPageComponent),
      },
    ],
  },
];
