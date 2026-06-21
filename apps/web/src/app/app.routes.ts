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
          import('./features/panels/ai-infra-panel/ai-infra-panel.component').then(
            (m) => m.AIInfraPanelComponent
          ),
      },
      {
        path: 'rag',
        loadComponent: () =>
          import('./features/ai/rag/rag.page').then((m) => m.RagPageComponent),
      },
      {
        path: 'vision',
        loadComponent: () =>
          import('./features/ai/vision-panel/vision-panel.component').then(
            (m) => m.VisionPanelComponent
          ),
      },
      {
        path: 'aihubs',
        loadComponent: () =>
          import('./features/ai/ai-hub/ai-hub.component').then((m) => m.AiHubComponent),
      },
    ],
  },
];
