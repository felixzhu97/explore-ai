import { Routes } from '@angular/router';
import { AIInfraPanelComponent } from '@features/panels/ai-infra-panel/ai-infra-panel.component';
import { RagChatComponent } from '@features/ai/rag-chat/rag-chat.component';
import { VisionPanelComponent } from '@features/ai/vision-panel/vision-panel.component';
import { AiHubComponent } from '@features/ai/ai-hub/ai-hub.component';

export const routes: Routes = [
  { path: '', redirectTo: 'ai-infra', pathMatch: 'full' },
  { path: 'ai-infra', component: AIInfraPanelComponent },
  { path: 'rag', component: RagChatComponent },
  { path: 'vision', component: VisionPanelComponent },
  { path: 'aihubs', component: AiHubComponent },
];
