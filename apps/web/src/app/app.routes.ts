import { Routes } from '@angular/router';

export const routes: Routes = [
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
      import('./features/ai/rag-chat/rag-chat.component').then((m) => m.RagChatComponent),
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
];
