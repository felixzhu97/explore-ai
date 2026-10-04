import { Routes } from '@angular/router';
import { MainLayoutComponent } from './layout';
import { FEATURE_FLAG_KEYS } from './feature-flags/feature-flag-keys';
import { moduleEnabledGuard } from './feature-flags/module-enabled.guard';
import { CHAT_ROUTES } from './chat/chat.routes';

export const routes: Routes = [
  {
    path: '',
    component: MainLayoutComponent,
    children: [
      { path: '', redirectTo: 'chat', pathMatch: 'full' },
      {
        path: 'rag',
        loadChildren: () => import('./rag/rag.routes').then(m => m.RAG_ROUTES),
      },
      {
        path: 'vision',
        canActivate: [moduleEnabledGuard(FEATURE_FLAG_KEYS.MODULE_VISION)],
        loadChildren: () => import('./vision/vision.routes').then(m => m.VISION_ROUTES),
      },
      {
        path: 'mcp',
        canActivate: [moduleEnabledGuard(FEATURE_FLAG_KEYS.MODULE_MCP)],
        loadChildren: () => import('./mcp/mcp.routes').then(m => m.MCP_ROUTES),
      },
      {
        path: 'eval',
        canActivate: [moduleEnabledGuard(FEATURE_FLAG_KEYS.MODULE_EVAL)],
        loadChildren: () => import('./eval/eval.routes').then(m => m.EVAL_ROUTES),
      },
      {
        path: 'speech-to-text',
        canActivate: [moduleEnabledGuard(FEATURE_FLAG_KEYS.MODULE_AUDIO_ASR)],
        loadChildren: () => import('./speech-to-text/speech-to-text.routes').then(m => m.SPEECH_TO_TEXT_ROUTES),
      },
      {
        path: 'pipelines',
        canActivate: [moduleEnabledGuard(FEATURE_FLAG_KEYS.MODULE_PIPELINES)],
        loadChildren: () => import('./pipelines/pipelines.routes').then(m => m.PIPELINES_ROUTES),
      },
      {
        path: 'automations',
        canActivate: [moduleEnabledGuard(FEATURE_FLAG_KEYS.MODULE_AUTOMATIONS)],
        loadChildren: () => import('./automations/automations.routes').then(m => m.AUTOMATIONS_ROUTES),
      },
      {
        path: 'agents',
        canActivate: [moduleEnabledGuard(FEATURE_FLAG_KEYS.MODULE_PIPELINES)],
        loadChildren: () => import('./agents/agents.routes').then(m => m.AGENTS_ROUTES),
      },
      {
        path: 'skills',
        canActivate: [moduleEnabledGuard(FEATURE_FLAG_KEYS.MODULE_SKILLS)],
        loadChildren: () => import('./skills/skills.routes').then(m => m.SKILLS_ROUTES),
      },
      ...CHAT_ROUTES,
      {
        path: 'metrics',
        loadChildren: () => import('./metrics/metrics.routes').then(m => m.METRICS_ROUTES),
      },
      {
        path: 'privacy',
        loadChildren: () => import('./privacy/privacy.routes').then(m => m.PRIVACY_ROUTES),
      },
      {
        path: 'policies',
        loadChildren: () => import('./policies/policies.routes').then(m => m.POLICIES_ROUTES),
      },
      { path: 'asr', redirectTo: 'speech-to-text', pathMatch: 'full' },
      { path: 'legal', redirectTo: 'policies', pathMatch: 'full' },
      { path: 'legal/terms', redirectTo: 'policies/terms-of-use', pathMatch: 'full' },
      { path: 'legal/privacy', redirectTo: 'policies/privacy-policy', pathMatch: 'full' },
      { path: 'legal/cookies', redirectTo: 'policies/cookie-policy', pathMatch: 'full' },
      { path: 'legal/subprocessors', redirectTo: 'policies/subprocessors', pathMatch: 'full' },
      { path: 'legal/:doc', redirectTo: 'policies', pathMatch: 'full' },
      {
        path: 'generate',
        loadChildren: () => import('./generate/generate.routes').then(m => m.GENERATE_ROUTES),
      },
    ],
  },
];
