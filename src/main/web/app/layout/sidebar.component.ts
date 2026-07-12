import {
  Component,
  ChangeDetectionStrategy,
  inject,
  signal,
  HostListener,
  computed,
  OnInit,
} from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { CommonModule } from '@angular/common';
import { I18nService } from '@core/i18n';
import {
  SidebarGroupComponent,
} from '@/shared/components/layout/sidebar.component';
import { ZardSidebarMenuButtonDirective } from '@/shared/components/layout/sidebar-menu-button.directive';
import { SidebarService } from './sidebar.service';
import { SessionItemComponent } from './components/session-item/session-item.component';
import { LanguagePickerComponent } from './components/language-picker/language-picker.component';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';
import { SESSION_LIST } from './services/session-list.token';
import type { SidebarSession } from './sidebar-session.model';
import {
  isNavTabEnabled,
  MODULE_NAV_TABS,
  type ModuleNavTab,
} from '@core/config/module-nav.config';
import { FeatureFlagService } from '@core/services/feature-flag.service';

@Component({
  selector: 'app-sidebar',
  imports: [
    RouterLink,
    CommonModule,
    SidebarGroupComponent,
    ZardSidebarMenuButtonDirective,
    SessionItemComponent,
    LanguagePickerComponent,
  ],
  templateUrl: './sidebar.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SidebarComponent implements OnInit {
  private readonly router = inject(Router);
  private readonly sanitizer = inject(DomSanitizer);
  protected readonly i18n = inject(I18nService);
  readonly sidebar = inject(SidebarService);
  protected readonly sessionList = inject(SESSION_LIST);
  private readonly featureFlags = inject(FeatureFlagService);

  readonly collapsed = this.sidebar.collapsed;
  readonly recentsExpanded = signal(true);

  private readonly isMobile = signal(false);

  readonly displaySessions = computed<SidebarSession[]>(
    () => this.sessionList.sessions(),
  );

  readonly sidebarClasses = computed(() => {
    const mobile = this.isMobile();
    const collapsed = this.collapsed();
    const mobileOpen = this.sidebar.mobileOpen();

    const classes: string[] = [];

    if (mobile) {
      classes.push('w-[min(88vw,20rem)]');
    } else if (!collapsed) {
      classes.push('w-[240px]');
    }
    if (!mobile && collapsed) {
      classes.push('w-[64px]');
    }

    if (!mobile) {
      classes.push('translate-x-0');
    } else if (mobileOpen) {
      classes.push('translate-x-0');
    } else {
      classes.push('-translate-x-full');
    }

    return classes.join(' ');
  });

  readonly tabs = computed<ModuleNavTab[]>(() => MODULE_NAV_TABS.filter(
    tab => isNavTabEnabled(tab, this.featureFlags),
  ));

  constructor() {
    this.updateMobileState();
  }

  ngOnInit(): void {
    this.sessionList.initializeSessions();
  }

  private updateMobileState(): void {
    const mobile = window.innerWidth < 768;
    this.isMobile.set(mobile);
    if (mobile) {
      this.sidebar.collapsed.set(false);
    }
  }

  @HostListener('window:resize')
  onResize(): void {
    this.updateMobileState();
  }

  get t() {
    return this.i18n.t;
  }

  toggleCollapse(): void {
    if (!this.isMobile()) {
      this.collapsed.update(v => !v);
    }
  }

  onNavClick(): void {
    this.sidebar.close();
  }

  newChat(): void {
    this.sessionList.createSession();
    if (this.router.url !== '/chat') {
      void this.router.navigate(['/chat']);
    }
    this.sidebar.close();
  }

  onSessionSelect(sessionId: string): void {
    this.sessionList.selectSession(sessionId);
    if (this.router.url !== '/chat') {
      void this.router.navigate(['/chat']);
    }
    this.sidebar.close();
  }

  onSessionPin(): void {
    // Pin is client-only; server sessions use recents list for now
  }

  onSessionDelete(sessionId: string): void {
    this.sessionList.deleteSession(sessionId);
  }

  getIcon(key: string): SafeHtml {
    const icons: Record<string, string> = {
      rag: `<svg class="size-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M14.5 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V7.5L14.5 2z"/><polyline points="14,2 14,8 20,8"/><line x1="16" y1="13" x2="8" y2="13"/><line x1="16" y1="17" x2="8" y2="17"/><line x1="10" y1="9" x2="8" y2="9"/></svg>`,
      vision: `<svg class="size-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M2 12s3-7 10-7 10 7 10 7-3 7-10 7-10-7-10-7Z"/><circle cx="12" cy="12" r="3"/></svg>`,
      mcp: `<svg class="size-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 2v4"/><path d="M12 18v4"/><path d="m4.93 4.93 2.83 2.83"/><path d="m16.24 16.24 2.83 2.83"/><path d="M2 12h4"/><path d="M18 12h4"/><path d="m4.93 19.07 2.83-2.83"/><path d="m16.24 7.76 2.83-2.83"/><circle cx="12" cy="12" r="3"/></svg>`,
      eval: `<svg class="size-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 20V10"/><path d="M18 20V4"/><path d="M6 20v-4"/></svg>`,
      asr: `<svg class="size-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 2a3 3 0 0 0-3 3v7a3 3 0 0 0 6 0V5a3 3 0 0 0-3-3Z"/><path d="M19 10v2a7 7 0 0 1-14 0v-2"/><line x1="12" x2="12" y1="19" y2="22"/></svg>`,
      chat: `<svg class="size-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/></svg>`,
      generate: `<svg class="size-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect width="18" height="18" x="3" y="3" rx="2" ry="2"/><circle cx="9" cy="9" r="2"/><path d="m21 15-3.086-3.086a2 2 0 0 0-2.828 0L6 21"/><path d="M12 2v4M12 18v4M2 12h4M18 12h4"/></svg>`,
    };
    const iconSvg = icons[key] || `<svg class="size-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="3" width="18" height="18" rx="2"/></svg>`;
    return this.sanitizer.bypassSecurityTrustHtml(iconSvg);
  }

  @HostListener('document:pointerdown', ['$event'])
  onDocumentPointerDown(event: PointerEvent): void {
    const target = event.target as Element;
    const isOutsideSidebar =
      !target || typeof target.closest !== 'function' || !target.closest('[data-sidebar-panel]');

    if (this.sidebar.mobileOpen() && isOutsideSidebar) {
      this.sidebar.close();
    }
  }
}
