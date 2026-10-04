import { type Routes } from '@angular/router';
import { GeneratePageComponent } from './generate.page';

export const GENERATE_ROUTES: Routes = [
  {
    path: '',
    component: GeneratePageComponent,
    children: [
      { path: '', redirectTo: 'image', pathMatch: 'full' },
      {
        path: 'image',
        loadComponent: () => import('../image/image.page').then(m => m.ImagePageComponent),
      },
      {
        path: 'tts',
        loadComponent: () => import('../tts/tts.page').then(m => m.TtsPageComponent),
      },
    ],
  },
];
