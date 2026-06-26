import { Routes } from '@angular/router';
import { MainLayoutComponent } from './layout';

export const routes: Routes = [
  {
    path: '',
    component: MainLayoutComponent,
    children: [
      { path: '', redirectTo: 'ai-infra', pathMatch: 'full' },
      {
        path: 'ai-infra',
        loadComponent: () =>
          import('./panels/ai-infra-panel/ai-infra-panel.component').then(
            (m) => m.AIInfraPanelComponent
          ),
      },
      {
        path: 'rag',
        loadComponent: () =>
          import('./ai/rag/rag.page').then((m) => m.RagPageComponent),
      },
      {
        path: 'vision',
        loadComponent: () =>
          import('./ai/vision/vision.page').then(
            (m) => m.VisionPageComponent
          ),
      },
      {
        path: 'aihubs',
        loadComponent: () =>
          import('./ai/ai-hub/ai-hub.component').then((m) => m.AiHubComponent),
      },
    ],
  },
];
