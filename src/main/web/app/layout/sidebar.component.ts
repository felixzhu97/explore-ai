import {
  Component,
  inject,
  signal,
  computed,
  type OnInit,
} from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { NavigationEnd, Router, RouterLink } from '@angular/router';
import { filter, map, startWith } from 'rxjs';
import { I18nService } from '../i18n';
import { ZardSidebarGroupComponent } from '../ui/layout/sidebar-group.component';
import { ZardSidebarMenuButtonDirective } from '../ui/layout/sidebar-menu-button.directive';
import { SidebarService } from './sidebar.service';
import { SessionItemComponent } from './session-item.component';
import { SidebarUserMenuComponent } from './sidebar-user-menu.component';
import { SidebarMoreMenuComponent } from './sidebar-more-menu.component';
import { DomSanitizer, type SafeHtml } from '@angular/platform-browser';
import { SESSION_LIST, type SidebarSession } from './session-list.token';
import {
  isNavTabEnabled,
  MODULE_NAV_TABS,
  listMoreNavSections,
  listPrimaryNavTabs,
  type ModuleNavTab,
} from './module-nav.config';
import { FeatureFlagService } from '../feature-flags/feature-flag.service';

@Component({
  selector: 'app-sidebar',
  imports: [
    RouterLink,
    ZardSidebarGroupComponent,
    ZardSidebarMenuButtonDirective,
    SessionItemComponent,
    SidebarUserMenuComponent,
    SidebarMoreMenuComponent,
  ],
  templateUrl: './sidebar.component.html',
  host: {
    '(window:resize)': 'onResize()',
    '(document:pointerdown)': 'onDocumentPointerDown($event)',
  },
})
export class AppSidebarComponent implements OnInit {
  readonly #sanitizer = inject(DomSanitizer);
  readonly #router = inject(Router);
  readonly #featureFlags = inject(FeatureFlagService);
  protected readonly i18n = inject(I18nService);
  readonly sidebar = inject(SidebarService);
  protected readonly sessionList = inject(SESSION_LIST);

  readonly isCollapsed = this.sidebar.isCollapsed;
  readonly #isMobile = signal(false);

  readonly sidebarClasses = computed(() => {
    const isMobile = this.#isMobile();
    const isCollapsed = this.isCollapsed();
    const isMobileOpen = this.sidebar.isMobileOpen();

    const classes: string[] = [];

    if (isMobile) {
      classes.push('w-[min(88vw,20rem)]');
    } else if (!isCollapsed) {
      classes.push('w-[240px]');
    }
    if (!isMobile && isCollapsed) {
      classes.push('w-[64px]');
    }

    if (!isMobile) {
      classes.push('translate-x-0');
    } else if (isMobileOpen) {
      classes.push('translate-x-0');
    } else {
      classes.push('-translate-x-full');
    }

    return classes.join(' ');
  });

  get t() {
    return this.i18n.t;
  }

  readonly tabs = computed<ModuleNavTab[]>(() => MODULE_NAV_TABS.filter(
    tab => isNavTabEnabled(tab, this.#featureFlags),
  ));

  readonly primaryTabs = computed(() => listPrimaryNavTabs(this.tabs()));
  readonly moreSections = computed(() => listMoreNavSections(this.tabs()));
  readonly navIconFn = (key: string): SafeHtml => this.getIcon(key);

  readonly displaySessions = computed<SidebarSession[]>(
    () => this.sessionList.sessions(),
  );

  readonly pinnedSessions = computed<SidebarSession[]>(() => {
    return compareByNewest(this.displaySessions().filter(session => session.pinned));
  });

  readonly isPinnedExpanded = signal(true);

  readonly recentSessions = computed<SidebarSession[]>(() => {
    return compareByNewest(this.displaySessions().filter(session => !session.pinned));
  });

  readonly isRecentsExpanded = signal(true);

  readonly #currentUrl = toSignal(
    this.#router.events.pipe(
      filter((event): event is NavigationEnd => event instanceof NavigationEnd),
      map(() => this.#router.url),
      startWith(this.#router.url),
    ),
    { initialValue: this.#router.url },
  );

  constructor() {
    this.#updateMobileState();
  }

  ngOnInit(): void {
    this.sessionList.initializeSessions();
  }

  /** Collapses or expands the sidebar on desktop. */
  toggleCollapse(): void {
    if (!this.#isMobile()) {
      this.isCollapsed.update(v => !v);
    }
  }

  /** Starts a new chat and closes the mobile sidebar. */
  startNewChat(): void {
    this.sessionList.createSession();
    this.sidebar.closeSidebar();
  }

  /** Tells whether the path is the current page. */
  isNavActive(path: string): boolean {
    const [url = ''] = this.#currentUrl().split('?');
    return url === path || url.startsWith(`${path}/`);
  }

  /** Closes the mobile sidebar after navigation. */
  onNavClick(): void {
    this.sidebar.closeSidebar();
  }

  /** Returns the icon of a module. */
  getIcon(key: string): SafeHtml {
    const icons: Record<string, string> = {
      rag: `<svg class="size-3.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M14.5 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V7.5L14.5 2z"/><polyline points="14,2 14,8 20,8"/><line x1="16" y1="13" x2="8" y2="13"/><line x1="16" y1="17" x2="8" y2="17"/><line x1="10" y1="9" x2="8" y2="9"/></svg>`,
      vision: `<svg class="size-3.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M2 12s3-7 10-7 10 7 10 7-3 7-10 7-10-7-10-7Z"/><circle cx="12" cy="12" r="3"/></svg>`,
      mcp: `<svg class="size-3.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 2v4"/><path d="M12 18v4"/><path d="m4.93 4.93 2.83 2.83"/><path d="m16.24 16.24 2.83 2.83"/><path d="M2 12h4"/><path d="M18 12h4"/><path d="m4.93 19.07 2.83-2.83"/><path d="m16.24 7.76 2.83-2.83"/><circle cx="12" cy="12" r="3"/></svg>`,
      eval: `<svg class="size-3.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 20V10"/><path d="M18 20V4"/><path d="M6 20v-4"/></svg>`,
      speechToText: `<svg class="size-3.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 2a3 3 0 0 0-3 3v7a3 3 0 0 0 6 0V5a3 3 0 0 0-3-3Z"/><path d="M19 10v2a7 7 0 0 1-14 0v-2"/><line x1="12" x2="12" y1="19" y2="22"/></svg>`,
      agents: `<svg class="size-3.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 8V4H8"/><rect width="16" height="12" x="4" y="8" rx="2"/><path d="M2 14h2"/><path d="M20 14h2"/><path d="M15 13v2"/><path d="M9 13v2"/></svg>`,
      pipelines: `<svg class="size-3.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect width="8" height="8" x="3" y="3" rx="2"/><path d="M7 11v4a2 2 0 0 0 2 2h4"/><rect width="8" height="8" x="13" y="13" rx="2"/></svg>`,
      automations: `<svg class="size-3.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/></svg>`,
      skills: `<svg class="size-3.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M14.7 6.3a1 1 0 0 0 0 1.4l1.6 1.6a1 1 0 0 0 1.4 0l3.77-3.77a6 6 0 0 1-7.94 7.94l-6.91 6.91a2.12 2.12 0 0 1-3-3l6.91-6.91a6 6 0 0 1 7.94-7.94l-3.76 3.76z"/></svg>`,
      chat: `<svg class="size-3.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/></svg>`,
      metrics: `<svg class="size-3.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M3 3v16a2 2 0 0 0 2 2h16"/><path d="M18 17V9"/><path d="M13 17V5"/><path d="M8 17v-3"/></svg>`,
      generate: `<svg class="size-3.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect width="18" height="18" x="3" y="3" rx="2" ry="2"/><circle cx="9" cy="9" r="2"/><path d="m21 15-3.086-3.086a2 2 0 0 0-2.828 0L6 21"/><path d="M12 2v4M12 18v4M2 12h4M18 12h4"/></svg>`,
    };
    const iconSvg = icons[key] ?? `<svg class="size-3.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="3" width="18" height="18" rx="2"/></svg>`;
    return this.#sanitizer.bypassSecurityTrustHtml(iconSvg);
  }

  /** Opens a session and closes the mobile sidebar. */
  onSessionSelect(sessionId: string): void {
    this.sessionList.selectSession(sessionId);
    this.sidebar.closeSidebar();
  }

  /** Pins or unpins a session. */
  onSessionPin(sessionId: string): void {
    this.sessionList.togglePin(sessionId);
  }

  /** Deletes a session. */
  onSessionDelete(sessionId: string): void {
    this.sessionList.deleteSession(sessionId);
  }

  /** Updates the mobile layout on resize. */
  onResize(): void {
    this.#updateMobileState();
  }

  /** Closes the mobile sidebar on a click outside it. */
  onDocumentPointerDown(event: PointerEvent): void {
    const isOutsideSidebar = (event.target as Element).closest('[data-sidebar-panel]') === null;

    if (this.sidebar.isMobileOpen() && isOutsideSidebar) {
      this.sidebar.closeSidebar();
    }
  }

  #updateMobileState(): void {
    const mobile = window.innerWidth < 768;
    this.#isMobile.set(mobile);
    if (mobile) {
      this.sidebar.isCollapsed.set(false);
    }
  }
}

function compareByNewest(sessions: SidebarSession[]): SidebarSession[] {
  return [...sessions].sort((a, b) => b.timestamp.compareTo(a.timestamp));
}
