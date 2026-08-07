import { Routes } from '@angular/router';
import { MainLayoutComponent } from './layout';
import { FEATURE_FLAG_KEYS } from './core/config/feature-flag-keys';
import { moduleEnabledGuard } from './core/guards/module-enabled.guard';

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
        canActivate: [moduleEnabledGuard(FEATURE_FLAG_KEYS.MODULE_VISION)],
        loadComponent: () => import('./vision/vision.page').then(m => m.VisionPageComponent),
      },
      {
        path: 'mcp',
        canActivate: [moduleEnabledGuard(FEATURE_FLAG_KEYS.MODULE_MCP)],
        loadComponent: () => import('./mcp/mcp.page').then(m => m.McpPageComponent),
      },
      {
        path: 'eval',
        canActivate: [moduleEnabledGuard(FEATURE_FLAG_KEYS.MODULE_EVAL)],
        loadComponent: () => import('./eval/eval.page').then(m => m.EvalPageComponent),
      },
      {
        path: 'asr',
        canActivate: [moduleEnabledGuard(FEATURE_FLAG_KEYS.MODULE_AUDIO_ASR)],
        loadComponent: () => import('./asr/asr.page').then(m => m.AsrPageComponent),
      },
      {
        path: 'pipelines',
        canActivate: [moduleEnabledGuard(FEATURE_FLAG_KEYS.MODULE_PIPELINES)],
        loadComponent: () => import('./pipelines/pipelines.page').then(m => m.PipelinesPageComponent),
      },
      {
        path: 'automations',
        canActivate: [moduleEnabledGuard(FEATURE_FLAG_KEYS.MODULE_AUTOMATIONS)],
        loadComponent: () => import('./automations/automations.page').then(m => m.AutomationsPageComponent),
      },
      {
        path: 'agents',
        canActivate: [moduleEnabledGuard(FEATURE_FLAG_KEYS.MODULE_PIPELINES)],
        loadComponent: () => import('./agents/agents.page').then(m => m.AgentsPageComponent),
      },

      {
        path: 'skills',
        canActivate: [moduleEnabledGuard(FEATURE_FLAG_KEYS.MODULE_SKILLS)],
        loadComponent: () => import('./skills/skills.page').then(m => m.SkillsPageComponent),
      },
      {
        path: 'chat',
        loadComponent: () => import('./chat/chat.page').then(m => m.ChatPage),
      },
      {
        path: 'metrics',
        loadChildren: () => import('./metrics/metrics.routes').then(m => m.METRICS_ROUTES),
      },
      {
        path: 'privacy',
        loadComponent: () => import('./privacy/privacy.page').then(m => m.PrivacyPage),
      },
      {
        path: 'legal',
        loadComponent: () => import('./legal/legal.page').then(m => m.LegalPage),
      },
      {
        path: 'legal/:doc',
        loadComponent: () => import('./legal/legal.page').then(m => m.LegalPage),
      },
      {
        path: 'generate',
        loadComponent: () => import('./generate/generate.page').then(m => m.GeneratePage),
        children: [
          { path: '', redirectTo: 'image', pathMatch: 'full' },
          {
            path: 'image',
            loadComponent: () => import('./generate/image/image.page').then(m => m.ImagePage),
          },
          {
            path: 'tts',
            loadComponent: () => import('./generate/tts/tts.page').then(m => m.TtsPage),
          },
        ],
      },
    ],
  },
];
