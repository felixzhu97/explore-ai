import { Routes } from '@angular/router';
import { chatRouteMatcher } from './chat.route-matcher';

/** Spread into the shell children: a top-level matcher keeps the page mounted. */
export const CHAT_ROUTES: Routes = [
  {
    matcher: chatRouteMatcher,
    loadComponent: () => import('./pages/chat.page').then(m => m.ChatPageComponent),
  },
];
